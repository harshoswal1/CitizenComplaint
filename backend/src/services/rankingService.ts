import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';
import { recordAuditLog } from './auditService.ts';
import type { CorporationRanking } from '../models/types.ts';

export interface DeliveryTransitionResult {
  complaintId: string;
  previousEmailStatus: string;
  newEmailStatus: string;
  previousResolutionStatus: string;
  newResolutionStatus: string;
  rankingUpdated: boolean;
  unresolvedCount: number;
}

/**
 * Handles Option B: Delivery confirmation.
 * Increments unresolved count ONLY if previously NOT delivered.
 * Guarantees idempotency on duplicate notifications.
 */
export function handleEmailDeliveryConfirmed(
  complaintId: string,
  providerReference?: string,
  idempotencyKey?: string,
  customDb?: DatabaseSync
): DeliveryTransitionResult {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  // Load complaint
  const complaint = db.prepare(`
    SELECT * FROM complaints WHERE id = ?
  `).get(complaintId) as any;

  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  // Idempotency check: If already DELIVERED, do NOT double-increment!
  if (complaint.email_status === 'DELIVERED') {
    const auth = db.prepare('SELECT unresolved_count FROM authorities WHERE id = ?').get(complaint.authority_id) as any;
    return {
      complaintId,
      previousEmailStatus: 'DELIVERED',
      newEmailStatus: 'DELIVERED',
      previousResolutionStatus: complaint.resolution_status,
      newResolutionStatus: complaint.resolution_status,
      rankingUpdated: false,
      unresolvedCount: auth?.unresolved_count || 0
    };
  }

  const prevEmailStatus = complaint.email_status;
  const prevResolutionStatus = complaint.resolution_status;
  const newResolutionStatus = 'UNRESOLVED';

  // 1. Update complaint record
  db.prepare(`
    UPDATE complaints
    SET email_status = 'DELIVERED',
        resolution_status = ?,
        delivered_timestamp = ?,
        updated_at = ?
    WHERE id = ?
  `).run(newResolutionStatus, now, now, complaintId);

  // 2. Update latest email delivery attempt if present
  db.prepare(`
    UPDATE email_deliveries
    SET status = 'DELIVERED',
        provider_reference = COALESCE(?, provider_reference),
        delivered_at = ?
    WHERE complaint_id = ? AND status != 'DELIVERED'
  `).run(providerReference || null, now, complaintId);

  // 3. Atomically update authority statistics
  // Decrement pending/failed counts and increment total_delivered & unresolved_count
  db.prepare(`
    UPDATE authorities
    SET unresolved_count = unresolved_count + 1,
        total_delivered_count = total_delivered_count + 1,
        pending_email_count = CASE WHEN pending_email_count > 0 THEN pending_email_count - 1 ELSE 0 END,
        updated_at = ?
    WHERE id = ?
  `).run(now, complaint.authority_id);

  // 4. Record status history
  const historyId = `hist_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, reason, timestamp)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    prevResolutionStatus,
    newResolutionStatus,
    `Email delivery confirmed by provider (Ref: ${providerReference || 'N/A'}). Complaint now eligible for unresolved ranking under Option B.`,
    now
  );

  // 5. Audit log
  recordAuditLog(
    'EMAIL_DELIVERY_CONFIRMED',
    'complaint',
    complaintId,
    {
      authorityId: complaint.authority_id,
      previousEmailStatus: prevEmailStatus,
      newEmailStatus: 'DELIVERED',
      rankingIncremented: true,
      providerReference,
      idempotencyKey
    },
    undefined,
    db
  );

  const auth = db.prepare('SELECT unresolved_count FROM authorities WHERE id = ?').get(complaint.authority_id) as any;

  return {
    complaintId,
    previousEmailStatus: prevEmailStatus,
    newEmailStatus: 'DELIVERED',
    previousResolutionStatus: prevResolutionStatus,
    newResolutionStatus,
    rankingUpdated: true,
    unresolvedCount: auth?.unresolved_count || 0
  };
}

/**
 * Handles email delivery failure.
 * Does NOT increase unresolved count. Keeps complaint record for retry.
 */
export function handleEmailDeliveryFailed(
  complaintId: string,
  errorMessage: string,
  providerReference?: string,
  customDb?: DatabaseSync
): void {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const complaint = db.prepare(`SELECT * FROM complaints WHERE id = ?`).get(complaintId) as any;
  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  // Update complaint
  db.prepare(`
    UPDATE complaints
    SET email_status = 'FAILED',
        resolution_status = 'DELIVERY_FAILED',
        updated_at = ?
    WHERE id = ?
  `).run(now, complaintId);

  // Update delivery record
  db.prepare(`
    UPDATE email_deliveries
    SET status = 'FAILED',
        error_message = ?,
        provider_reference = COALESCE(?, provider_reference)
    WHERE complaint_id = ?
  `).run(errorMessage, providerReference || null, complaintId);

  // Update authority failed count (unresolved_count is NOT touched)
  db.prepare(`
    UPDATE authorities
    SET failed_delivery_count = failed_delivery_count + 1,
        pending_email_count = CASE WHEN pending_email_count > 0 THEN pending_email_count - 1 ELSE 0 END,
        updated_at = ?
    WHERE id = ?
  `).run(now, complaint.authority_id);

  // Record status history
  const historyId = `hist_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, reason, timestamp)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    complaint.resolution_status,
    'DELIVERY_FAILED',
    `Email delivery failed: ${errorMessage}. Complaint NOT counted towards unresolved rankings until successful delivery.`,
    now
  );

  recordAuditLog(
    'EMAIL_DELIVERY_FAILED',
    'complaint',
    complaintId,
    {
      authorityId: complaint.authority_id,
      errorMessage,
      rankingIncremented: false
    },
    undefined,
    db
  );
}

/**
 * Handles verified resolution approval.
 * Decrements unresolved count by 1 and increments resolved_verified_count by 1.
 */
export function approveVerifiedResolution(
  complaintId: string,
  resolutionRecordId: string,
  reviewedByUserId: string,
  reviewNotes: string,
  customDb?: DatabaseSync
): void {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const complaint = db.prepare(`SELECT * FROM complaints WHERE id = ?`).get(complaintId) as any;
  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  if (complaint.resolution_status === 'RESOLVED_VERIFIED') {
    return; // Already resolved
  }

  // Must have been delivered to have been counted in unresolved
  const wasCounted = complaint.email_status === 'DELIVERED';

  // 1. Update resolution record
  db.prepare(`
    UPDATE resolution_records
    SET status = 'APPROVED',
        reviewed_by_user_id = ?,
        review_notes = ?,
        reviewed_at = ?
    WHERE id = ?
  `).run(reviewedByUserId, reviewNotes, now, resolutionRecordId);

  // 2. Update complaint
  db.prepare(`
    UPDATE complaints
    SET resolution_status = 'RESOLVED_VERIFIED',
        resolved_timestamp = ?,
        updated_at = ?
    WHERE id = ?
  `).run(now, now, complaintId);

  // 3. Update authority counts if was previously unresolved
  if (wasCounted) {
    db.prepare(`
      UPDATE authorities
      SET unresolved_count = CASE WHEN unresolved_count > 0 THEN unresolved_count - 1 ELSE 0 END,
          resolved_verified_count = resolved_verified_count + 1,
          updated_at = ?
      WHERE id = ?
    `).run(now, complaint.authority_id);
  }

  // 4. Status history
  const historyId = `hist_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, changed_by_user_id, reason, timestamp)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    complaint.resolution_status,
    'RESOLVED_VERIFIED',
    reviewedByUserId,
    `Resolution verified & approved: ${reviewNotes}. Decremented authority unresolved count.`,
    now
  );

  recordAuditLog(
    'RESOLUTION_VERIFIED_APPROVED',
    'complaint',
    complaintId,
    {
      authorityId: complaint.authority_id,
      resolutionRecordId,
      reviewedByUserId,
      reviewNotes,
      unresolvedDecremented: wasCounted
    },
    reviewedByUserId,
    db
  );
}

/**
 * Reopens a previously resolved complaint.
 * Restores status to UNRESOLVED and increments unresolved count by 1.
 */
export function reopenComplaint(
  complaintId: string,
  reopenedByUserId: string,
  reason: string,
  customDb?: DatabaseSync
): void {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const complaint = db.prepare(`SELECT * FROM complaints WHERE id = ?`).get(complaintId) as any;
  if (!complaint) {
    throw new Error(`Complaint ${complaintId} not found`);
  }

  if (complaint.resolution_status !== 'RESOLVED_VERIFIED') {
    throw new Error(`Cannot reopen complaint that is not in RESOLVED_VERIFIED status (current: ${complaint.resolution_status})`);
  }

  // 1. Update complaint
  db.prepare(`
    UPDATE complaints
    SET resolution_status = 'UNRESOLVED',
        resolved_timestamp = NULL,
        updated_at = ?
    WHERE id = ?
  `).run(now, complaintId);

  // 2. Adjust authority stats
  db.prepare(`
    UPDATE authorities
    SET unresolved_count = unresolved_count + 1,
        resolved_verified_count = CASE WHEN resolved_verified_count > 0 THEN resolved_verified_count - 1 ELSE 0 END,
        updated_at = ?
    WHERE id = ?
  `).run(now, complaint.authority_id);

  // 3. Status history
  const historyId = `hist_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  db.prepare(`
    INSERT INTO complaint_status_history (id, complaint_id, previous_status, new_status, changed_by_user_id, reason, timestamp)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `).run(
    historyId,
    complaintId,
    'RESOLVED_VERIFIED',
    'UNRESOLVED',
    reopenedByUserId,
    `Complaint reopened: ${reason}. Restored to active unresolved count.`,
    now
  );

  recordAuditLog(
    'COMPLAINT_REOPENED',
    'complaint',
    complaintId,
    {
      authorityId: complaint.authority_id,
      reopenedByUserId,
      reason
    },
    reopenedByUserId,
    db
  );
}

/**
 * Calculates corporation rankings using verified data.
 * Adheres strictly to: Unresolved = Delivered complaints - Verified resolutions.
 */
export function getMunicipalCorporationRankings(customDb?: DatabaseSync): CorporationRanking[] {
  const db = customDb || getDb();

  const authorities = db.prepare(`
    SELECT * FROM authorities ORDER BY unresolved_count DESC, total_delivered_count DESC
  `).all() as any[];

  return authorities.map(auth => {
    // Dynamic counts from complaints to ensure perfect auditable reconciliation
    const stats = db.prepare(`
      SELECT 
        COUNT(CASE WHEN email_status = 'DELIVERED' AND resolution_status NOT IN ('RESOLVED_VERIFIED', 'INVALIDATED') THEN 1 END) as live_unresolved,
        COUNT(CASE WHEN resolution_status = 'RESOLVED_VERIFIED' THEN 1 END) as live_resolved,
        COUNT(CASE WHEN email_status IN ('PENDING', 'QUEUED', 'SENDING', 'SENT') THEN 1 END) as live_awaiting,
        COUNT(CASE WHEN email_status = 'FAILED' THEN 1 END) as live_failed,
        COUNT(CASE WHEN is_overdue = 1 AND resolution_status NOT IN ('RESOLVED_VERIFIED', 'INVALIDATED') THEN 1 END) as live_overdue,
        COALESCE(SUM(support_count), 0) as total_supports
      FROM complaints
      WHERE authority_id = ? AND is_hidden = 0
    `).get(auth.id) as any;

    const totalDelivered = (stats?.live_unresolved || 0) + (stats?.live_resolved || 0);
    const resolutionRate = totalDelivered > 0
      ? Math.round(((stats?.live_resolved || 0) / totalDelivered) * 1000) / 10
      : 0.0;

    return {
      authority_id: auth.id,
      name: auth.name,
      code: auth.code,
      state: auth.state,
      district: auth.district,
      unresolved_count: stats?.live_unresolved || 0,
      resolved_verified_count: stats?.live_resolved || 0,
      awaiting_email_count: stats?.live_awaiting || 0,
      failed_delivery_count: stats?.live_failed || 0,
      overdue_count: stats?.live_overdue || 0,
      total_supports: stats?.total_supports || 0,
      resolution_rate_percent: resolutionRate
    };
  });
}
