import test from 'node:test';
import * as assert from 'node:assert';
import { DatabaseSync } from 'node:sqlite';
import { getDb, initSchema } from '../src/db/db.ts';
import { importKmcDirectory } from '../src/services/csvImporter.ts';
import { registerUser } from '../src/services/authService.ts';
import {
  createComplaint,
  toggleSupport,
  claimResolution,
  getComplaintsFeed
} from '../src/services/complaintService.ts';
import { findNearbyDuplicates, haversineDistanceMeters } from '../src/services/duplicateDetector.ts';
import {
  handleEmailDeliveryConfirmed,
  handleEmailDeliveryFailed,
  approveVerifiedResolution,
  reopenComplaint,
  getMunicipalCorporationRankings
} from '../src/services/rankingService.ts';
import { dispatchComplaintEmail } from '../src/services/emailService.ts';

test('CIVIC ACCOUNTABILITY PLATFORM - FULL ACCEPTANCE SUITE', async (t) => {
  // Create clean in-memory database for testing
  const db = new DatabaseSync(':memory:');
  initSchema(db);

  let citizenAId: string;
  let citizenBId: string;
  let adminId: string;

  await t.test('Phase 1 & 2: KMC Directory CSV Import & Zero Initial Baseline (Rule 6)', () => {
    const importRes = importKmcDirectory(undefined, db);
    assert.strictEqual(importRes.authorityId, 'auth_kmc_kolhapur');
    assert.strictEqual(importRes.totalContacts, 49, 'Must import all 49 contacts from CSV');
    assert.strictEqual(importRes.validEmailsCount, 29, 'Must identify 29 valid emails');
    assert.strictEqual(importRes.invalidOrNAEmailsCount, 20, 'Must identify 20 N/A or helpline contacts');
    assert.strictEqual(importRes.departmentsCount, 7, 'Must parse 7 distinct departments');

    // Rule 6: Initial unresolved count must strictly start at zero
    const kmc = db.prepare('SELECT unresolved_count, resolved_verified_count FROM authorities WHERE code = ?').get('KMC') as any;
    assert.strictEqual(kmc.unresolved_count, 0, 'KMC must start at 0 unresolved complaints');
    assert.strictEqual(kmc.resolved_verified_count, 0, 'KMC must start at 0 verified resolved complaints');
  });

  await t.test('Authentication & Users Setup', () => {
    const citizenA = registerUser('Citizen Ramesh', 'ramesh@kolhapur.org', 'Pass12345!', 'CITIZEN', '9822001122', db);
    citizenAId = citizenA.user.id;
    assert.ok(citizenA.token);

    const citizenB = registerUser('Citizen Priya', 'priya@kolhapur.org', 'Pass12345!', 'CITIZEN', '9822003344', db);
    citizenBId = citizenB.user.id;

    const admin = registerUser('Admin Deshmukh', 'admin@kolhapur.gov.in', 'AdminSecret2026!', 'ADMIN', '02312540291', db);
    adminId = admin.user.id;
  });

  let complaint1Id: string;

  await t.test('Phase 3 & 4: Complaint Creation & Option B Delivery Rule (Rule 2)', () => {
    // Citizen A reports a severe pothole in Tarabai Park, Kolhapur
    // Lat: 16.7112, Lng: 74.2385
    const createRes = createComplaint({
      reporterId: citizenAId,
      title: 'Dangerous deep pothole on Main Road near Circuit House',
      description: 'Pothole spanning ~1.5 meters width causing vehicle damage and waterlogging.',
      category: 'ROADS_AND_INFRASTRUCTURE',
      subtype: 'Potholes',
      photoUrl: 'https://cdn.nagrikvoice.org/photos/tarabai_pothole_01.jpg',
      latitude: 16.7112,
      longitude: 74.2385,
      locality: 'Tarabai Park',
      administrativeArea: 'Kolhapur Municipal Corporation',
      simulateEmailDelivery: false // Keep pending to verify Rule 2 Option B!
    }, db);

    complaint1Id = createRes.complaint.id;
    assert.ok(complaint1Id.startsWith('KMC-'), 'Complaint ID must have authority prefix');

    // Rule 2 Check: Prior to delivery confirmation, unresolved count must still be 0!
    let rankings = getMunicipalCorporationRankings(db);
    let kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 0, 'Rule 2: Unresolved count must NOT increment while email is pending');
    assert.strictEqual(kmcRank?.awaiting_email_count, 1, 'Must show 1 complaint awaiting delivery');

    // Now simulate email delivery confirmed callback from email service
    const confirmRes = handleEmailDeliveryConfirmed(complaint1Id, 'prov_smtp_ack_98472', 'idemp_key_1', db);
    assert.strictEqual(confirmRes.newEmailStatus, 'DELIVERED');
    assert.strictEqual(confirmRes.newResolutionStatus, 'UNRESOLVED');
    assert.strictEqual(confirmRes.rankingUpdated, true);

    // Rule 2 Check: Now that email is delivered, unresolved count MUST be exactly 1
    rankings = getMunicipalCorporationRankings(db);
    kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Rule 2: Confirmed delivery must increment unresolved count to 1');
    assert.strictEqual(kmcRank?.awaiting_email_count, 0);

    // Idempotency check: Provider sends duplicate delivery callback
    const duplicateDeliveryRes = handleEmailDeliveryConfirmed(complaint1Id, 'prov_smtp_ack_98472', 'idemp_key_1', db);
    assert.strictEqual(duplicateDeliveryRes.rankingUpdated, false, 'Duplicate delivery callback must not re-increment ranking');

    rankings = getMunicipalCorporationRankings(db);
    kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Unresolved count must remain 1 upon duplicate callback');
  });

  await t.test('Phase 5: Duplicate Complaint Prevention (Rule 1)', () => {
    // Citizen B encounters the same pothole 25 meters away (16.7114, 74.2386)
    // Runs duplicate detection
    const duplicates = findNearbyDuplicates(
      16.7114,
      74.2386,
      'ROADS_AND_INFRASTRUCTURE',
      'Potholes',
      100, // 100 meters radius
      db
    );

    assert.strictEqual(duplicates.length, 1, 'Duplicate detector must surface the existing nearby pothole complaint');
    assert.strictEqual(duplicates[0].id, complaint1Id);
    assert.ok(duplicates[0].distance_meters < 30, 'Distance must be ~25 meters');

    // Rule 1: Citizen B confirms it is the same problem and supports existing complaint instead
    const supportRes = toggleSupport(complaint1Id, citizenBId, db);
    assert.strictEqual(supportRes.supported, true);
    assert.strictEqual(supportRes.supportCount, 1);

    // Verify no new primary complaint record created
    const totalComplaints = db.prepare('SELECT COUNT(*) as count FROM complaints').get() as any;
    assert.strictEqual(totalComplaints.count, 1, 'Rule 1: Only 1 primary complaint record must exist');

    // Rule 4: Votes do not increment unresolved complaints count
    const rankings = getMunicipalCorporationRankings(db);
    const kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Rule 4: Citizen support/votes must NOT increment unresolved count');
    assert.strictEqual(kmcRank?.total_supports, 1, 'Community support count must reflect 1 vote');
  });

  await t.test('Phase 6: Voting Idempotency & Support Withdrawal (Rule 4)', () => {
    // Citizen B tries to support again (should toggle/withdraw)
    const withdrawRes = toggleSupport(complaint1Id, citizenBId, db);
    assert.strictEqual(withdrawRes.supported, false);
    assert.strictEqual(withdrawRes.supportCount, 0);

    let rankings = getMunicipalCorporationRankings(db);
    let kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Unresolved count must still be 1');

    // Re-add support
    toggleSupport(complaint1Id, citizenBId, db);
    const c = db.prepare('SELECT support_count FROM complaints WHERE id = ?').get(complaint1Id) as any;
    assert.strictEqual(c.support_count, 1);
  });

  let complaint2Id: string;

  await t.test('Phase 4 (continued): Email Delivery Failure & Retry Mechanism (Rule 3)', () => {
    // Citizen A reports a garbage overflow in Rajarampuri
    const c2 = createComplaint({
      reporterId: citizenAId,
      title: 'Uncollected garbage mound near Rajarampuri 5th lane',
      description: 'Garbage bins overflowing onto road for 4 days.',
      category: 'WASTE_MANAGEMENT',
      subtype: 'Garbage accumulation',
      photoUrl: 'https://cdn.nagrikvoice.org/photos/rajarampuri_garbage.jpg',
      latitude: 16.6920,
      longitude: 74.2410,
      locality: 'Rajarampuri',
      simulateEmailDelivery: true,
      deliveryOutcome: 'FAILED' // Simulate failure
    }, db);

    complaint2Id = c2.complaint.id;

    // Rule 3: Failed email must NOT increase unresolved count
    let rankings = getMunicipalCorporationRankings(db);
    let kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Failed email must NOT increment unresolved count');
    assert.strictEqual(kmcRank?.failed_delivery_count, 1, 'Failed delivery count must be 1');

    // Retry email using existing complaint ID
    const retryRes = dispatchComplaintEmail({
      complaintId: complaint2Id,
      isRetry: true,
      simulateDelivery: true,
      deliveryOutcome: 'DELIVERED', // Retry succeeds
      customDb: db
    });

    assert.strictEqual(retryRes.complaintId, complaint2Id, 'Rule 3: Retry must reuse existing complaint ID');
    assert.strictEqual(retryRes.attemptNumber, 2, 'Attempt number must be 2');

    // Verify unresolved count now increments by 1 (1 + 1 = 2)
    rankings = getMunicipalCorporationRankings(db);
    kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 2, 'Rule 2 & 3: Upon successful retry delivery, unresolved count becomes 2');
  });

  await t.test('Phase 8: Complaint Resolution & Verification (Rule 5)', () => {
    // Municipal corporation patches the Tarabai Park pothole
    // Citizen A claims resolution
    const claimRes = claimResolution(
      complaint1Id,
      citizenAId,
      'Road repair crew asphalted and flattened the pothole this morning.',
      'https://cdn.nagrikvoice.org/photos/tarabai_pothole_repaired.jpg',
      db
    );

    // Complaint enters RESOLUTION_CLAIMED; unresolved count must NOT decrement yet!
    let rankings = getMunicipalCorporationRankings(db);
    let kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 2, 'Unverified resolution claim must NOT decrement unresolved count');

    // Admin verifies evidence and approves resolution
    approveVerifiedResolution(
      complaint1Id,
      claimRes.resolutionId,
      adminId,
      'Verified with on-ground PWD inspection report.',
      db
    );

    // Rule 5: Now verified resolution decreases unresolved count from 2 to 1!
    rankings = getMunicipalCorporationRankings(db);
    kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 1, 'Rule 5: Approved resolution must decrement unresolved count to 1');
    assert.strictEqual(kmcRank?.resolved_verified_count, 1, 'Verified resolved count must be 1');
    assert.strictEqual(kmcRank?.resolution_rate_percent, 50, 'Resolution rate must be 50% (1 resolved out of 2 total)');

    // Reopen testing: Suppose road patch crumbles 2 days later
    reopenComplaint(complaint1Id, citizenBId, 'Patch washed away during rain, hole is back', db);

    rankings = getMunicipalCorporationRankings(db);
    kmcRank = rankings.find(r => r.code === 'KMC');
    assert.strictEqual(kmcRank?.unresolved_count, 2, 'Reopened complaint must restore unresolved count to 2');
    assert.strictEqual(kmcRank?.resolved_verified_count, 0, 'Verified resolved count must return to 0');
  });

  await t.test('Phase 9: Pan-India Expansion Support (Rule 9)', () => {
    // Add Pune Municipal Corporation (PMC) dynamically via data model
    const now = new Date().toISOString();
    db.prepare(`
      INSERT INTO authorities (
        id, name, code, state, district, type, center_lat, center_lng,
        unresolved_count, resolved_verified_count, total_delivered_count,
        failed_delivery_count, pending_email_count, created_at, updated_at
      ) VALUES (
        'auth_pmc_pune', 'Pune Municipal Corporation', 'PMC', 'Maharashtra', 'Pune',
        'MUNICIPAL_CORPORATION', 18.5204, 73.8567, 0, 0, 0, 0, 0, ?, ?
      )
    `).run(now, now);

    const rankings = getMunicipalCorporationRankings(db);
    assert.strictEqual(rankings.length, 2, 'Rankings must now include both KMC and PMC');
    const pmcRank = rankings.find(r => r.code === 'PMC');
    assert.strictEqual(pmcRank?.unresolved_count, 0, 'New authority PMC must start at zero');
  });

  await t.test('Audit Logs Integrity (Rule 8)', () => {
    const auditLogs = db.prepare('SELECT * FROM audit_logs').all() as any[];
    assert.ok(auditLogs.length >= 5, 'Every lifecycle transition must be auditable');
    const actions = auditLogs.map(l => l.action);
    assert.ok(actions.includes('COMPLAINT_CREATED'));
    assert.ok(actions.includes('EMAIL_DELIVERY_CONFIRMED'));
    assert.ok(actions.includes('EMAIL_DELIVERY_FAILED'));
    assert.ok(actions.includes('RESOLUTION_VERIFIED_APPROVED'));
  });
});
