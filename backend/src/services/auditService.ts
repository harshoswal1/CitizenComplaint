import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';

export function recordAuditLog(
  action: string,
  targetType: string,
  targetId: string,
  details: Record<string, any>,
  userId?: string,
  customDb?: DatabaseSync
): void {
  const db = customDb || getDb();
  const id = `audit_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
  const now = new Date().toISOString();
  
  const stmt = db.prepare(`
    INSERT INTO audit_logs (id, user_id, action, target_type, target_id, details_json, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);
  stmt.run(id, userId || null, action, targetType, targetId, JSON.stringify(details), now);
}
