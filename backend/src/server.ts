import * as http from 'node:http';
import * as url from 'node:url';
import { getDb } from './db/db.ts';
import { importKmcDirectory } from './services/csvImporter.ts';
import { registerUser, loginUser, verifyToken } from './services/authService.ts';
import {
  createComplaint,
  toggleSupport,
  claimResolution,
  getComplaintsFeed
} from './services/complaintService.ts';
import { findNearbyDuplicates } from './services/duplicateDetector.ts';
import { dispatchComplaintEmail } from './services/emailService.ts';
import {
  handleEmailDeliveryConfirmed,
  handleEmailDeliveryFailed,
  approveVerifiedResolution,
  reopenComplaint,
  getMunicipalCorporationRankings
} from './services/rankingService.ts';
import { recordAuditLog } from './services/auditService.ts';
import { seedMajorAuthorities } from './services/authoritySeed.ts';

const PORT = parseInt(process.env.PORT || '8080', 10);

// Initialize DB and CSV directory
const db = getDb();
try {
  importKmcDirectory(undefined, db);
  seedMajorAuthorities(db);
  console.log('✅ KMC Authority, major Indian Municipal Corporations, and category routes initialized.');
} catch (err) {
  console.error('Error importing directory or seeding authorities on startup:', err);
}

// Ensure at least one default citizen and admin exist for testing
try {
  registerUser('Citizen Ramesh Patil', 'ramesh@kolhapur.org', 'Pass12345!', 'CITIZEN', '9822112233', db);
  registerUser('Municipal Admin Deshmukh', 'admin@kmc.gov.in', 'AdminSecret2026!', 'ADMIN', '02312540291', db);
} catch {
  // Already exists
}

function sendJson(res: http.ServerResponse, statusCode: number, data: any) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(JSON.stringify(data));
}

function sendHtml(res: http.ServerResponse, html: string) {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Access-Control-Allow-Origin': '*'
  });
  res.end(html);
}

function parseBody(req: http.IncomingMessage): Promise<any> {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 5 * 1024 * 1024) {
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      if (!body) return resolve({});
      try {
        resolve(JSON.parse(body));
      } catch (e) {
        reject(new Error('Invalid JSON'));
      }
    });
    req.on('error', reject);
  });
}

function extractUser(req: http.IncomingMessage) {
  const authHeader = req.headers['authorization'];
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null;
  const token = authHeader.substring(7);
  return verifyToken(token);
}

export const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url || '/', true);
  const pathname = parsedUrl.pathname || '/';
  const method = req.method || 'GET';
  const query = parsedUrl.query;

  // CORS preflight
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    });
    return res.end();
  }

  try {
    // ----------------------------------------------------
    // AUTHENTICATION ROUTES
    // ----------------------------------------------------
    if (pathname === '/api/v1/auth/register' && method === 'POST') {
      const body = await parseBody(req);
      if (!body.name || !body.email || !body.password) {
        return sendJson(res, 400, { error: 'Name, email, and password are required' });
      }
      const result = registerUser(body.name, body.email, body.password, body.role || 'CITIZEN', body.phone);
      return sendJson(res, 201, result);
    }

    if (pathname === '/api/v1/auth/login' && method === 'POST') {
      const body = await parseBody(req);
      if (!body.email || !body.password) {
        return sendJson(res, 400, { error: 'Email and password are required' });
      }
      const result = loginUser(body.email, body.password);
      return sendJson(res, 200, result);
    }

    if (pathname === '/api/v1/auth/me' && method === 'GET') {
      const authUser = extractUser(req);
      if (!authUser) return sendJson(res, 401, { error: 'Unauthorized' });
      const user = db.prepare('SELECT id, name, email, role, phone, created_at FROM users WHERE id = ?').get(authUser.userId);
      return sendJson(res, 200, { user });
    }

    // ----------------------------------------------------
    // DUPLICATE DETECTION ROUTE (Rule 1)
    // ----------------------------------------------------
    if (pathname === '/api/v1/complaints/check-duplicate' && method === 'GET') {
      const lat = parseFloat(query.latitude as string);
      const lng = parseFloat(query.longitude as string);
      const category = query.category as string;
      const subtype = query.subtype as string | undefined;
      const radius = parseFloat((query.radius as string) || '100');

      if (isNaN(lat) || isNaN(lng) || !category) {
        return sendJson(res, 400, { error: 'latitude, longitude, and category are required' });
      }

      const duplicates = findNearbyDuplicates(lat, lng, category, subtype, radius);
      return sendJson(res, 200, {
        has_duplicates: duplicates.length > 0,
        duplicates_count: duplicates.length,
        candidates: duplicates
      });
    }

    // ----------------------------------------------------
    // COMPLAINTS FEED & SUBMISSION
    // ----------------------------------------------------
    if (pathname === '/api/v1/complaints' && method === 'GET') {
      const authUser = extractUser(req);
      const feed = getComplaintsFeed({
        filter: query.filter as any,
        category: query.category as string,
        authorityId: query.authority_id as string,
        status: query.status as string,
        latitude: query.latitude ? parseFloat(query.latitude as string) : undefined,
        longitude: query.longitude ? parseFloat(query.longitude as string) : undefined,
        userId: authUser?.userId,
        limit: query.limit ? parseInt(query.limit as string, 10) : 20,
        offset: query.offset ? parseInt(query.offset as string, 10) : 0
      });
      return sendJson(res, 200, feed);
    }

    if (pathname === '/api/v1/complaints' && method === 'POST') {
      const authUser = extractUser(req);
      const body = await parseBody(req);
      const reporterId = authUser?.userId || body.reporter_id;

      if (!reporterId) {
        return sendJson(res, 401, { error: 'Authentication required to report complaints' });
      }

      if (!body.title || !body.description || !body.category || body.latitude === undefined || body.longitude === undefined) {
        return sendJson(res, 400, { error: 'title, description, category, latitude, longitude are required' });
      }

      const result = createComplaint({
        reporterId,
        title: body.title,
        description: body.description,
        category: body.category,
        subtype: body.subtype,
        photoUrl: body.photo_url || 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800',
        latitude: parseFloat(body.latitude),
        longitude: parseFloat(body.longitude),
        locationAccuracy: body.location_accuracy ? parseFloat(body.location_accuracy) : undefined,
        landmark: body.landmark,
        locality: body.locality || 'Kolhapur Urban Area',
        administrativeArea: body.administrative_area || 'Kolhapur Municipal Corporation',
        authorityId: body.authority_id || 'auth_kmc_kolhapur',
        simulateEmailDelivery: body.simulate_delivery === true || body.simulate_delivery === 'true',
        deliveryOutcome: body.delivery_outcome || 'DELIVERED',
        cameraCaptureTimestamp: body.camera_capture_timestamp
      });

      return sendJson(res, 201, result);
    }

    // Specific complaint detail: /api/v1/complaints/:id
    const complaintDetailMatch = pathname.match(/^\/api\/v1\/complaints\/([a-zA-Z0-9_\-]+)$/);
    if (complaintDetailMatch && method === 'GET') {
      const id = complaintDetailMatch[1];
      const complaint = db.prepare(`
        SELECT c.*, a.name as authority_name, d.name as department_name
        FROM complaints c
        LEFT JOIN authorities a ON a.id = c.authority_id
        LEFT JOIN departments d ON d.id = c.department_id
        WHERE c.id = ?
      `).get(id) as any;

      if (!complaint) return sendJson(res, 404, { error: 'Complaint not found' });

      const history = db.prepare('SELECT * FROM complaint_status_history WHERE complaint_id = ? ORDER BY timestamp DESC').all(id);
      const resolutions = db.prepare('SELECT * FROM resolution_records WHERE complaint_id = ? ORDER BY created_at DESC').all(id);
      const deliveries = db.prepare('SELECT * FROM email_deliveries WHERE complaint_id = ? ORDER BY attempt_number DESC').all(id);

      return sendJson(res, 200, {
        complaint,
        history,
        resolutions,
        deliveries
      });
    }

    // Support toggle: /api/v1/complaints/:id/support
    const supportMatch = pathname.match(/^\/api\/v1\/complaints\/([a-zA-Z0-9_\-]+)\/support$/);
    if (supportMatch && method === 'POST') {
      const authUser = extractUser(req);
      const body = await parseBody(req);
      const userId = authUser?.userId || body.user_id;

      if (!userId) return sendJson(res, 401, { error: 'Authentication required to support issues' });
      const id = supportMatch[1];

      const result = toggleSupport(id, userId);
      return sendJson(res, 200, result);
    }

    // Retry email: /api/v1/complaints/:id/retry-email (Rule 3)
    const retryMatch = pathname.match(/^\/api\/v1\/complaints\/([a-zA-Z0-9_\-]+)\/retry-email$/);
    if (retryMatch && method === 'POST') {
      const id = retryMatch[1];
      const body = await parseBody(req);

      const result = dispatchComplaintEmail({
        complaintId: id,
        isRetry: true,
        simulateDelivery: body.simulate_delivery !== false,
        deliveryOutcome: body.delivery_outcome || 'DELIVERED'
      });
      return sendJson(res, 200, result);
    }

    // Propose resolution: /api/v1/complaints/:id/claim-resolution
    const claimMatch = pathname.match(/^\/api\/v1\/complaints\/([a-zA-Z0-9_\-]+)\/claim-resolution$/);
    if (claimMatch && method === 'POST') {
      const authUser = extractUser(req);
      const body = await parseBody(req);
      const userId = authUser?.userId || body.user_id;
      if (!userId) return sendJson(res, 401, { error: 'Authentication required' });
      if (!body.evidence_description) return sendJson(res, 400, { error: 'evidence_description is required' });

      const id = claimMatch[1];
      const result = claimResolution(id, userId, body.evidence_description, body.evidence_photo_url);
      return sendJson(res, 200, result);
    }

    // Report abuse: /api/v1/complaints/:id/report-abuse
    const abuseMatch = pathname.match(/^\/api\/v1\/complaints\/([a-zA-Z0-9_\-]+)\/report-abuse$/);
    if (abuseMatch && method === 'POST') {
      const id = abuseMatch[1];
      const body = await parseBody(req);
      db.prepare('UPDATE complaints SET flagged_count = flagged_count + 1 WHERE id = ?').run(id);
      recordAuditLog('COMPLAINT_FLAGGED_ABUSE', 'complaint', id, { reason: body.reason || 'Citizen flagged' });
      return sendJson(res, 200, { success: true, message: 'Complaint flagged for moderator review' });
    }

    // ----------------------------------------------------
    // MUNICIPAL CORPORATION RANKINGS (Section 11)
    // ----------------------------------------------------
    if (pathname === '/api/v1/rankings' && method === 'GET') {
      const rankings = getMunicipalCorporationRankings();
      return sendJson(res, 200, {
        ranking_policy: 'OPTION_B_VERIFIED_DELIVERY',
        formula: 'Unresolved Count = Confirmed Delivered Complaints - Verified Resolved Complaints',
        rankings
      });
    }

    // Authorities list
    if (pathname === '/api/v1/authorities' && method === 'GET') {
      const authorities = db.prepare('SELECT * FROM authorities').all();
      return sendJson(res, 200, { authorities });
    }

    // ----------------------------------------------------
    // EMAIL WEBHOOK / SIMULATOR (Option B Engine)
    // ----------------------------------------------------
    if (pathname === '/api/v1/email/webhook' && method === 'POST') {
      const body = await parseBody(req);
      if (!body.complaint_id || !body.status) {
        return sendJson(res, 400, { error: 'complaint_id and status (DELIVERED | FAILED) are required' });
      }

      if (body.status === 'DELIVERED') {
        const result = handleEmailDeliveryConfirmed(body.complaint_id, body.provider_reference, body.idempotency_key);
        return sendJson(res, 200, { success: true, result });
      } else if (body.status === 'FAILED' || body.status === 'BOUNCED') {
        handleEmailDeliveryFailed(body.complaint_id, body.error_message || 'Simulated mailserver failure', body.provider_reference);
        return sendJson(res, 200, { success: true, message: 'Complaint delivery marked as failed; ready for retry' });
      } else {
        return sendJson(res, 400, { error: 'Unsupported webhook status' });
      }
    }

    // User profile
    const profileMatch = pathname.match(/^\/api\/v1\/users\/([a-zA-Z0-9_\-]+)\/profile$/);
    if (profileMatch && method === 'GET') {
      const userId = profileMatch[1];
      const user = db.prepare('SELECT id, name, email, role, phone, created_at FROM users WHERE id = ?').get(userId);
      if (!user) return sendJson(res, 404, { error: 'User not found' });

      const submitted = db.prepare('SELECT * FROM complaints WHERE reporter_id = ? ORDER BY created_at DESC').all(userId);
      const supported = db.prepare(`
        SELECT c.* FROM complaints c
        JOIN votes v ON v.complaint_id = c.id
        WHERE v.user_id = ?
        ORDER BY v.created_at DESC
      `).all(userId);

      return sendJson(res, 200, { user, submitted, supported });
    }

    // ----------------------------------------------------
    // ADMIN ENDPOINTS & DASHBOARD
    // ----------------------------------------------------
    if (pathname === '/api/v1/admin/resolution-review' && method === 'POST') {
      const authUser = extractUser(req);
      const body = await parseBody(req);
      if (!body.complaint_id || !body.resolution_id || !body.action) {
        return sendJson(res, 400, { error: 'complaint_id, resolution_id, and action (APPROVE | REJECT) required' });
      }

      const adminId = authUser?.userId || body.admin_id || 'usr_admin_default';

      if (body.action === 'APPROVE') {
        approveVerifiedResolution(body.complaint_id, body.resolution_id, adminId, body.review_notes || 'Approved after verification');
        return sendJson(res, 200, { success: true, message: 'Resolution approved and unresolved count decremented' });
      } else {
        db.prepare('UPDATE resolution_records SET status = ?, review_notes = ?, reviewed_by_user_id = ? WHERE id = ?').run(
          'REJECTED',
          body.review_notes || 'Evidence insufficient',
          adminId,
          body.resolution_id
        );
        db.prepare('UPDATE complaints SET resolution_status = ? WHERE id = ?').run('UNRESOLVED', body.complaint_id);
        return sendJson(res, 200, { success: true, message: 'Resolution claim rejected; issue remains unresolved' });
      }
    }

    if (pathname === '/api/v1/admin/reopen' && method === 'POST') {
      const body = await parseBody(req);
      if (!body.complaint_id) return sendJson(res, 400, { error: 'complaint_id is required' });
      reopenComplaint(body.complaint_id, body.admin_id || 'usr_admin', body.reason || 'Problem re-occurred');
      return sendJson(res, 200, { success: true, message: 'Complaint reopened and restored to unresolved rankings' });
    }

    if (pathname === '/api/v1/admin/audit-logs' && method === 'GET') {
      const logs = db.prepare('SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 50').all();
      return sendJson(res, 200, { logs });
    }

    // ----------------------------------------------------
    // INTERACTIVE WEB DASHBOARD (HTML)
    // ----------------------------------------------------
    if (pathname === '/' || pathname === '/admin') {
      const rankings = getMunicipalCorporationRankings();
      const kmc = rankings.find(r => r.code === 'KMC') || rankings[0];
      const recentComplaints = db.prepare(`
        SELECT c.*, a.name as authority_name
        FROM complaints c
        JOIN authorities a ON a.id = c.authority_id
        ORDER BY c.created_at DESC LIMIT 10
      `).all() as any[];
      const auditLogs = db.prepare('SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 8').all() as any[];

      const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>NagrikVoice — Civic Accountability Platform</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
  <style>
    body { background: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; color: #1e293b; }
    .navbar { background: #0f172a; }
    .metric-card { border-radius: 12px; background: white; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); border-left: 4px solid #3b82f6; }
    .metric-card.unresolved { border-left-color: #ef4444; }
    .metric-card.resolved { border-left-color: #10b981; }
    .metric-card.awaiting { border-left-color: #f59e0b; }
    .badge-status { font-size: 0.8rem; padding: 6px 12px; border-radius: 20px; }
    .card-complaint { border-radius: 16px; overflow: hidden; border: none; box-shadow: 0 4px 12px rgba(0,0,0,0.06); margin-bottom: 20px; background: white; }
    .complaint-img { height: 200px; width: 100%; object-fit: cover; }
    .btn-action { border-radius: 8px; font-weight: 500; font-size: 0.85rem; }
  </style>
</head>
<body>
  <nav class="navbar navbar-dark px-4 py-3">
    <div class="container-fluid d-flex justify-content-between">
      <span class="navbar-brand fw-bold fs-4">🇮🇳 NagrikVoice <span class="fs-6 text-secondary fw-normal">| Citizen-Powered Civic Accountability</span></span>
      <span class="badge bg-primary px-3 py-2">Option B Delivery Rule Active</span>
    </div>
  </nav>

  <div class="container my-4">
    <!-- Notice Banner -->
    <div class="alert alert-info border-0 shadow-sm d-flex align-items-center mb-4">
      <div class="fs-2 me-3">🏛️</div>
      <div>
        <strong>Kolhapur Municipal Corporation (KMC) Accountability Dashboard</strong>
        <p class="mb-0 text-muted small">Real-time civic complaint metrics adhering strictly to Rule 2: Unresolved count increments <em>only</em> upon confirmed official email delivery.</p>
      </div>
    </div>

    <!-- Live Metrics Grid -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <div class="metric-card unresolved">
          <div class="text-muted small text-uppercase fw-semibold">Unresolved Complaints</div>
          <div class="fs-2 fw-bold text-danger">${kmc?.unresolved_count ?? 0}</div>
          <small class="text-muted">Delivered & awaiting verified fix</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="metric-card resolved">
          <div class="text-muted small text-uppercase fw-semibold">Verified Resolved</div>
          <div class="fs-2 fw-bold text-success">${kmc?.resolved_verified_count ?? 0}</div>
          <small class="text-muted">Approved with resolution evidence</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="metric-card awaiting">
          <div class="text-muted small text-uppercase fw-semibold">Awaiting Delivery</div>
          <div class="fs-2 fw-bold text-warning">${kmc?.awaiting_email_count ?? 0}</div>
          <small class="text-muted">In dispatch queue</small>
        </div>
      </div>
      <div class="col-md-3">
        <div class="metric-card">
          <div class="text-muted small text-uppercase fw-semibold">Community Support</div>
          <div class="fs-2 fw-bold text-primary">${kmc?.total_supports ?? 0}</div>
          <small class="text-muted">Citizen votes recorded</small>
        </div>
      </div>
    </div>

    <!-- Interactive Simulation & Actions -->
    <div class="card p-3 mb-4 shadow-sm border-0">
      <h6 class="fw-bold mb-2">⚡ Interactive Test & Demonstration Bar</h6>
      <div class="d-flex flex-wrap gap-2">
        <button class="btn btn-outline-primary btn-sm btn-action" onclick="createSampleComplaint('delivered')">+ Report New Pothole (Delivered)</button>
        <button class="btn btn-outline-danger btn-sm btn-action" onclick="createSampleComplaint('failed')">+ Report New Complaint (Failed Delivery)</button>
        <button class="btn btn-outline-secondary btn-sm btn-action" onclick="window.location.reload()">🔄 Refresh Dashboard</button>
      </div>
    </div>

    <div class="row g-4">
      <!-- Recent Complaints Feed -->
      <div class="col-lg-8">
        <h5 class="fw-bold mb-3">📸 Live Instagram-Style Civic Feed</h5>
        ${recentComplaints.length === 0 ? '<p class="text-muted">No complaints reported yet. Click the button above to simulate a submission!</p>' : ''}
        ${recentComplaints.map(c => `
          <div class="card-complaint">
            <img src="${c.photo_url}" class="complaint-img" alt="Civic Issue Photo">
            <div class="p-3">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="badge bg-dark">${c.category.replace(/_/g, ' ')}</span>
                <span class="badge ${c.resolution_status === 'UNRESOLVED' ? 'bg-danger' : (c.resolution_status === 'RESOLVED_VERIFIED' ? 'bg-success' : 'bg-warning text-dark')} badge-status">
                  ${c.resolution_status}
                </span>
              </div>
              <h6 class="fw-bold mb-1">${c.title}</h6>
              <p class="text-muted small mb-2">${c.description}</p>
              <div class="text-muted small mb-3">
                📍 <strong>${c.locality}</strong> (${c.latitude.toFixed(4)}, ${c.longitude.toFixed(4)}) &bull; 
                🏛️ ${c.authority_name} &bull; 
                ✉️ Email: <strong>${c.email_status}</strong>
              </div>
              <div class="d-flex justify-content-between align-items-center border-top pt-2">
                <button class="btn btn-sm btn-outline-danger btn-action" onclick="supportComplaint('${c.id}')">❤️ Support (${c.support_count})</button>
                <div class="d-flex gap-2">
                  ${c.email_status === 'FAILED' ? `<button class="btn btn-sm btn-warning btn-action" onclick="retryEmail('${c.id}')">🔄 Retry Email</button>` : ''}
                  ${c.resolution_status === 'UNRESOLVED' ? `<button class="btn btn-sm btn-success btn-action" onclick="claimFix('${c.id}')">✅ Propose Fix</button>` : ''}
                  ${c.resolution_status === 'RESOLUTION_CLAIMED' ? `<button class="btn btn-sm btn-primary btn-action" onclick="verifyFix('${c.id}')">🔍 Admin Verify Fix</button>` : ''}
                  ${c.resolution_status === 'RESOLVED_VERIFIED' ? `<button class="btn btn-sm btn-outline-dark btn-action" onclick="reopenIssue('${c.id}')">⚠️ Reopen Issue</button>` : ''}
                </div>
              </div>
            </div>
          </div>
        `).join('')}
      </div>

      <!-- Audit Log & Directory Summary -->
      <div class="col-lg-4">
        <h5 class="fw-bold mb-3">📜 Immutable Audit Logs (Rule 8)</h5>
        <div class="card border-0 shadow-sm p-3 mb-4">
          <ul class="list-group list-group-flush small">
            ${auditLogs.map(l => `
              <li class="list-group-item px-0">
                <div class="fw-bold text-primary">${l.action}</div>
                <div class="text-muted">${l.target_type}: ${l.target_id}</div>
                <div class="text-muted" style="font-size: 0.75rem;">${new Date(l.created_at).toLocaleTimeString()}</div>
              </li>
            `).join('')}
          </ul>
        </div>

        <h5 class="fw-bold mb-3">📱 Android App Local Endpoint</h5>
        <div class="card border-0 shadow-sm p-3 bg-light">
          <p class="small text-muted mb-2">Connect your native Android application to this backend:</p>
          <code class="p-2 bg-white rounded border d-block text-break">http://10.0.2.2:${PORT}/api/v1</code>
          <small class="text-muted mt-2 d-block">Use <code>10.0.2.2</code> in the Android Emulator to connect to your host Mac server.</small>
        </div>
      </div>
    </div>
  </div>

  <script>
    async function createSampleComplaint(outcome) {
      const res = await fetch('/api/v1/complaints', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reporter_id: 'usr_patil',
          title: 'Deep crater pothole near Rankala Lake gate',
          description: 'Rainwater filled crater causing severe road bottleneck.',
          category: 'ROADS_AND_INFRASTRUCTURE',
          subtype: 'Potholes',
          latitude: 16.6945,
          longitude: 74.2185,
          locality: 'Rankala Lake',
          simulate_delivery: true,
          delivery_outcome: outcome === 'delivered' ? 'DELIVERED' : 'FAILED',
          photo_url: 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800'
        })
      });
      if (res.ok) window.location.reload();
      else alert('Error creating sample complaint');
    }

    async function supportComplaint(id) {
      const res = await fetch('/api/v1/complaints/' + id + '/support', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user_id: 'usr_patil' })
      });
      if (res.ok) window.location.reload();
    }

    async function retryEmail(id) {
      const res = await fetch('/api/v1/complaints/' + id + '/retry-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ simulate_delivery: true, delivery_outcome: 'DELIVERED' })
      });
      if (res.ok) window.location.reload();
    }

    async function claimFix(id) {
      const desc = prompt('Enter resolution description and proof:', 'PWD road contractor patched and asphalted the section.');
      if (!desc) return;
      const res = await fetch('/api/v1/complaints/' + id + '/claim-resolution', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user_id: 'usr_patil', evidence_description: desc })
      });
      if (res.ok) window.location.reload();
    }

    async function verifyFix(id) {
      // Find resolution id
      const detailRes = await fetch('/api/v1/complaints/' + id);
      const data = await detailRes.json();
      const resRecord = data.resolutions && data.resolutions[0];
      if (!resRecord) return alert('No resolution claim found');

      const res = await fetch('/api/v1/admin/resolution-review', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          complaint_id: id,
          resolution_id: resRecord.id,
          action: 'APPROVE',
          review_notes: 'Verified by PWD site inspection.'
        })
      });
      if (res.ok) window.location.reload();
    }

    async function reopenIssue(id) {
      const reason = prompt('Reason for reopening issue:', 'Asphalt layer eroded during rain; pothole has reappeared.');
      if (!reason) return;
      const res = await fetch('/api/v1/admin/reopen', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ complaint_id: id, reason })
      });
      if (res.ok) window.location.reload();
    }
  </script>
</body>
</html>`;
      return sendHtml(res, html);
    }

    // 404
    return sendJson(res, 404, { error: 'Not Found' });
  } catch (error: any) {
    console.error('API Error:', error);
    return sendJson(res, 500, { error: error?.message || 'Internal Server Error' });
  }
});

// Start listening if run directly
if (process.env.NODE_ENV !== 'test_runner') {
  server.listen(PORT, () => {
    console.log(`🚀 NagrikVoice Civic Platform Backend running at http://localhost:${PORT}`);
    console.log(`📊 Admin & Civic Dashboard accessible at http://localhost:${PORT}/admin`);
  });
}
