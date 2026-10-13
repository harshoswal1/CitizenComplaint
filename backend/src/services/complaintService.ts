import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';
import type { Complaint, ResolutionStatus } from '../models/types.ts';
import { findNearbyDuplicates } from './duplicateDetector.ts';
import { dispatchComplaintEmail } from './emailService.ts';
import { recordAuditLog } from './auditService.ts';

export interface CreateComplaintInput {
  reporterId: string;
  title: string;
  description: string;
  category: string;
  subtype?: string;
  photoUrl: string;
  latitude: number;
  longitude: number;
  locationAccuracy?: number;
  landmark?: string;
  locality: string;
  administrativeArea?: string;
  authorityId?: string;
  simulateEmailDelivery?: boolean;
  deliveryOutcome?: 'DELIVERED' | 'FAILED';
  cameraCaptureTimestamp?: string;
}

export function generateComplaintId(authorityCode: string, customDb?: DatabaseSync): string {
  const db = customDb || getDb();
  const year = new Date().getFullYear();
  const prefix = `${authorityCode}-${year}-`;
  
  const countRow = db.prepare(`
    SELECT COUNT(*) as count FROM complaints WHERE id LIKE ?
  `).get(`${prefix}%`) as any;

  const nextSeq = (countRow?.count || 0) + 1;
  return `${prefix}${String(nextSeq).padStart(4, '0')}`;
}

export function createComplaint(
  input: CreateComplaintInput,
  customDb?: DatabaseSync
): { complaint: Complaint; duplicateWarning?: boolean } {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  // 1. Resolve Authority (Default to KMC if not supplied)
  const authorityId = input.authorityId || 'auth_kmc_kolhapur';
  const authority = db.prepare('SELECT * FROM authorities WHERE id = ?').get(authorityId) as any;
  if (!authority) {
    throw new Error(`Authority ${authorityId} not found`);
  }

  // 2. Generate unique reference ID
  const complaintId = generateComplaintId(authority.code, db);

  // 3. Resolve department routing
  const mapping = db.prepare(`
    SELECT department_id FROM category_mappings WHERE authority_id = ? AND category = ?
  `).get(authorityId, input.category) as any;
  const departmentId = mapping?.department_id || null;

  // 4. Initial statuses (Option B requirement)
  // Email is PENDING; Resolution status is PENDING_EMAIL
  const emailStatus = 'PENDING';
  const resolutionStatus: ResolutionStatus = 'PENDING_EMAIL';

  db.prepare(`
    INSERT INTO complaints (
      id, reporter_id, title, description, category, subtype, photo_url,
      latitude, longitude, location_accuracy, landmark, locality, administrative_area,
      authority_id, department_id, email_status, resolution_status, support_count,
      is_overdue, submission_timestamp, camera_capture_timestamp, created_at, updated_at
    ) VALUES (
      ?, ?, ?, ?, ?, ?, ?,
      ?, ?, ?, ?, ?, ?,
      ?, ?, ?, ?, 0,
      0, ?, ?, ?, ?
    )
  `).run(
    complaintId,
    input.reporterId,
    input.title,
    input.description,
    input.category,
    input.subtype || null,
    input.photoUrl,
    input.latitude,
    input.longitude,
    input.locationAccuracy || null,
    input.landmark || null,
    input.locality,
    input.administrativeArea || `${authority.district}, ${authority.state}`,
    authorityId,
    departmentId,
    emailStatus,
    resolutionStatus,
    now,
    input.cameraCaptureTimestamp || now,
    now,
    now
  );

  // Update authority pending email count
  db.prepare(`
    UPDATE authorities SET pending_email_count = pending_email_count + 1 WHERE id = ?
  `).run(authorityId);

  // 5. Initial status history
  const historyId = `hist_${Date.now()}_init`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, changed_by_user_id, reason, timestamp)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    'NONE',
    'PENDING_EMAIL',
    input.reporterId,
    'Citizen created civic complaint. Awaiting email dispatch and confirmed delivery.',
    now
  );

  recordAuditLog('COMPLAINT_CREATED', 'complaint', complaintId, { input }, input.reporterId, db);

  // 6. Centralized email dispatch
  dispatchComplaintEmail({
    complaintId,
    simulateDelivery: input.simulateEmailDelivery,
    deliveryOutcome: input.deliveryOutcome,
    customDb: db
  });

  const created = db.prepare('SELECT * FROM complaints WHERE id = ?').get(complaintId) as Complaint;
  return { complaint: created };
}

export function toggleSupport(
  complaintId: string,
  userId: string,
  customDb?: DatabaseSync
): { supported: boolean; supportCount: number } {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const complaint = db.prepare('SELECT id, support_count FROM complaints WHERE id = ?').get(complaintId) as any;
  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  const existingVote = db.prepare('SELECT id FROM votes WHERE user_id = ? AND complaint_id = ?').get(userId, complaintId);

  if (existingVote) {
    // Withdraw support
    db.prepare('DELETE FROM votes WHERE user_id = ? AND complaint_id = ?').run(userId, complaintId);
    db.prepare(`
      UPDATE complaints
      SET support_count = CASE WHEN support_count > 0 THEN support_count - 1 ELSE 0 END,
          updated_at = ?
      WHERE id = ?
    `).run(now, complaintId);

    const updated = db.prepare('SELECT support_count FROM complaints WHERE id = ?').get(complaintId) as any;
    return { supported: false, supportCount: updated.support_count };
  } else {
    // Add support
    const voteId = `vote_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
    db.prepare('INSERT INTO votes (id, user_id, complaint_id, created_at) VALUES (?, ?, ?, ?)').run(
      voteId,
      userId,
      complaintId,
      now
    );

    db.prepare(`
      UPDATE complaints
      SET support_count = support_count + 1,
          updated_at = ?
      WHERE id = ?
    `).run(now, complaintId);

    const updated = db.prepare('SELECT support_count FROM complaints WHERE id = ?').get(complaintId) as any;
    return { supported: true, supportCount: updated.support_count };
  }
}

export function claimResolution(
  complaintId: string,
  userId: string,
  evidenceDescription: string,
  evidencePhotoUrl?: string,
  customDb?: DatabaseSync
): { resolutionId: string } {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const complaint = db.prepare('SELECT * FROM complaints WHERE id = ?').get(complaintId) as any;
  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  const resolutionId = `res_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;

  db.prepare(`
    INSERT INTO resolution_records (
      id, complaint_id, proposed_by_user_id, evidence_description, evidence_photo_url, status, created_at
    ) VALUES (?, ?, ?, ?, ?, 'PROPOSED', ?)
  `).run(resolutionId, complaintId, userId, evidenceDescription, evidencePhotoUrl || null, now);

  db.prepare(`
    UPDATE complaints
    SET resolution_status = 'RESOLUTION_CLAIMED',
        updated_at = ?
    WHERE id = ?
  `).run(now, complaintId);

  const historyId = `hist_${Date.now()}_res_claim`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, changed_by_user_id, reason, timestamp)
    VALUES (?, ?, ?, 'RESOLUTION_CLAIMED', ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    complaint.resolution_status,
    userId,
    `Citizen claimed problem is resolved: "${evidenceDescription}". Under verification review.`,
    now
  );

  return { resolutionId };
}

export interface FeedQueryOptions {
  filter?: 'nearby' | 'latest' | 'most_supported' | 'oldest_unresolved' | 'most_overdue';
  category?: string;
  authorityId?: string;
  status?: string;
  latitude?: number;
  longitude?: number;
  userId?: string;
  limit?: number;
  offset?: number;
}

export function getComplaintsFeed(
  options: FeedQueryOptions,
  customDb?: DatabaseSync
): { complaints: any[]; total: number } {
  const db = customDb || getDb();
  const limit = options.limit || 20;
  const offset = options.offset || 0;

  // SLA overdue evaluation: unresolved > 7 days (604800000 ms)
  const overdueCutoff = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
  db.prepare(`
    UPDATE complaints
    SET is_overdue = 1
    WHERE resolution_status = 'UNRESOLVED'
      AND submission_timestamp < ?
      AND is_overdue = 0
  `).run(overdueCutoff);

  const whereClauses: string[] = ['c.is_hidden = 0'];
  const params: any[] = [];

  if (options.category) {
    whereClauses.push('c.category = ?');
    params.push(options.category);
  }

  if (options.authorityId) {
    whereClauses.push('c.authority_id = ?');
    params.push(options.authorityId);
  }

  if (options.status) {
    whereClauses.push('c.resolution_status = ?');
    params.push(options.status);
  }

  let orderBy = 'c.created_at DESC';
  if (options.filter === 'most_supported') {
    orderBy = 'c.support_count DESC, c.created_at DESC';
  } else if (options.filter === 'oldest_unresolved') {
    whereClauses.push("c.resolution_status = 'UNRESOLVED'");
    orderBy = 'c.created_at ASC';
  } else if (options.filter === 'most_overdue') {
    whereClauses.push('c.is_overdue = 1');
    orderBy = 'c.created_at ASC';
  }

  const whereSql = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : '';

  const countRow = db.prepare(`SELECT COUNT(*) as count FROM complaints c ${whereSql}`).get(...params) as any;
  const total = countRow?.count || 0;

  const query = `
    SELECT 
      c.*,
      a.name as authority_name,
      a.code as authority_code,
      d.name as department_name,
      (SELECT COUNT(*) FROM votes v WHERE v.complaint_id = c.id AND v.user_id = ?) as user_has_supported
    FROM complaints c
    LEFT JOIN authorities a ON a.id = c.authority_id
    LEFT JOIN departments d ON d.id = c.department_id
    ${whereSql}
    ORDER BY ${orderBy}
    LIMIT ? OFFSET ?
  `;

  const rows = db.prepare(query).all(options.userId || '', ...params, limit, offset) as any[];

  // If nearby requested with coords, compute live distance
  if (options.filter === 'nearby' && options.latitude !== undefined && options.longitude !== undefined) {
    const userLat = options.latitude;
    const userLng = options.longitude;
    const degToRad = (d: number) => (d * Math.PI) / 180;
    
    rows.forEach(r => {
      const dLat = degToRad(r.latitude - userLat);
      const dLon = degToRad(r.longitude - userLng);
      const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(degToRad(userLat)) * Math.cos(degToRad(r.latitude)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
      r.distance_meters = Math.round(6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    });

    rows.sort((a, b) => (a.distance_meters || 0) - (b.distance_meters || 0));
  }

  return { complaints: rows, total };
}
