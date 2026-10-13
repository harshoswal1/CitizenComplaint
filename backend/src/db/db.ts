import { DatabaseSync } from 'node:sqlite';
import * as path from 'node:path';
import * as fs from 'node:fs';

let instance: DatabaseSync | null = null;

export function getDb(dbPath?: string): DatabaseSync {
  if (instance && !dbPath) {
    return instance;
  }

  const resolvedPath = dbPath || process.env.DATABASE_PATH || path.resolve(import.meta.dirname, '../../data/civic_platform.db');
  
  // Ensure directory exists if not memory
  if (resolvedPath !== ':memory:') {
    const dir = path.dirname(resolvedPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  }

  const db = new DatabaseSync(resolvedPath);
  db.exec('PRAGMA foreign_keys = ON;');
  db.exec('PRAGMA journal_mode = WAL;');

  initSchema(db);

  if (!dbPath) {
    instance = db;
  }
  return db;
}

export function closeDb(): void {
  if (instance) {
    instance.close();
    instance = null;
  }
}

export function initSchema(db: DatabaseSync): void {
  db.exec(`
    CREATE TABLE IF NOT EXISTS authorities (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      code TEXT NOT NULL UNIQUE,
      state TEXT NOT NULL,
      district TEXT NOT NULL,
      type TEXT NOT NULL DEFAULT 'MUNICIPAL_CORPORATION',
      center_lat REAL,
      center_lng REAL,
      unresolved_count INTEGER NOT NULL DEFAULT 0,
      resolved_verified_count INTEGER NOT NULL DEFAULT 0,
      total_delivered_count INTEGER NOT NULL DEFAULT 0,
      failed_delivery_count INTEGER NOT NULL DEFAULT 0,
      pending_email_count INTEGER NOT NULL DEFAULT 0,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS departments (
      id TEXT PRIMARY KEY,
      authority_id TEXT NOT NULL REFERENCES authorities(id),
      name TEXT NOT NULL,
      code TEXT NOT NULL,
      primary_email TEXT,
      contact_number TEXT,
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS authority_contacts (
      id TEXT PRIMARY KEY,
      authority_id TEXT NOT NULL REFERENCES authorities(id),
      department_id TEXT REFERENCES departments(id),
      department_name TEXT NOT NULL,
      name TEXT NOT NULL,
      designation TEXT NOT NULL,
      contact_number TEXT,
      email TEXT,
      is_email_valid INTEGER NOT NULL DEFAULT 0,
      verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED',
      last_verified_at TEXT,
      source_file TEXT NOT NULL DEFAULT 'KMC_All_Departments_Complete_Directory.csv',
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS category_mappings (
      id TEXT PRIMARY KEY,
      authority_id TEXT NOT NULL REFERENCES authorities(id),
      category TEXT NOT NULL,
      subtype TEXT,
      department_id TEXT NOT NULL REFERENCES departments(id),
      department_name TEXT NOT NULL,
      default_official_email TEXT,
      routing_notes TEXT
    );

    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      salt TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'CITIZEN',
      phone TEXT,
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS complaints (
      id TEXT PRIMARY KEY,
      reporter_id TEXT NOT NULL REFERENCES users(id),
      title TEXT NOT NULL,
      description TEXT NOT NULL,
      category TEXT NOT NULL,
      subtype TEXT,
      photo_url TEXT NOT NULL,
      latitude REAL NOT NULL,
      longitude REAL NOT NULL,
      location_accuracy REAL,
      landmark TEXT,
      locality TEXT NOT NULL,
      administrative_area TEXT NOT NULL,
      authority_id TEXT NOT NULL REFERENCES authorities(id),
      department_id TEXT REFERENCES departments(id),
      email_status TEXT NOT NULL DEFAULT 'PENDING',
      resolution_status TEXT NOT NULL DEFAULT 'PENDING_EMAIL',
      support_count INTEGER NOT NULL DEFAULT 0,
      is_overdue INTEGER NOT NULL DEFAULT 0,
      submission_timestamp TEXT NOT NULL,
      camera_capture_timestamp TEXT,
      delivered_timestamp TEXT,
      resolved_timestamp TEXT,
      flagged_count INTEGER NOT NULL DEFAULT 0,
      is_hidden INTEGER NOT NULL DEFAULT 0,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_complaints_geo ON complaints(latitude, longitude);
    CREATE INDEX IF NOT EXISTS idx_complaints_authority ON complaints(authority_id);
    CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(resolution_status);
    CREATE INDEX IF NOT EXISTS idx_complaints_category ON complaints(category);

    CREATE TABLE IF NOT EXISTS votes (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id),
      complaint_id TEXT NOT NULL REFERENCES complaints(id),
      created_at TEXT NOT NULL,
      UNIQUE(user_id, complaint_id)
    );

    CREATE TABLE IF NOT EXISTS email_deliveries (
      id TEXT PRIMARY KEY,
      complaint_id TEXT NOT NULL REFERENCES complaints(id),
      attempt_number INTEGER NOT NULL,
      recipient_email TEXT NOT NULL,
      is_test_recipient INTEGER NOT NULL DEFAULT 1,
      subject TEXT NOT NULL,
      body TEXT NOT NULL,
      status TEXT NOT NULL,
      provider_reference TEXT,
      idempotency_key TEXT UNIQUE,
      error_message TEXT,
      created_at TEXT NOT NULL,
      sent_at TEXT,
      delivered_at TEXT
    );

    CREATE TABLE IF NOT EXISTS complaint_status_history (
      id TEXT PRIMARY KEY,
      complaint_id TEXT NOT NULL REFERENCES complaints(id),
      previous_status TEXT NOT NULL,
      new_status TEXT NOT NULL,
      changed_by_user_id TEXT,
      reason TEXT NOT NULL,
      timestamp TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS resolution_records (
      id TEXT PRIMARY KEY,
      complaint_id TEXT NOT NULL REFERENCES complaints(id),
      proposed_by_user_id TEXT NOT NULL REFERENCES users(id),
      evidence_description TEXT NOT NULL,
      evidence_photo_url TEXT,
      status TEXT NOT NULL DEFAULT 'PROPOSED',
      reviewed_by_user_id TEXT,
      review_notes TEXT,
      reviewed_at TEXT,
      created_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS audit_logs (
      id TEXT PRIMARY KEY,
      user_id TEXT,
      action TEXT NOT NULL,
      target_type TEXT NOT NULL,
      target_id TEXT NOT NULL,
      details_json TEXT,
      created_at TEXT NOT NULL
    );
  `);
}
