import * as crypto from 'node:crypto';
import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';
import type { User, UserRole } from '../models/types.ts';

const JWT_SECRET = process.env.JWT_SECRET || 'nagrikvoice_super_secure_secret_key_2026';

export function hashPassword(password: string): { hash: string; salt: string } {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return { hash, salt };
}

export function verifyPassword(password: string, hash: string, salt: string): boolean {
  const testHash = crypto.scryptSync(password, salt, 64).toString('hex');
  return crypto.timingSafeEqual(Buffer.from(hash, 'hex'), Buffer.from(testHash, 'hex'));
}

export function generateToken(payload: { userId: string; email: string; role: UserRole }): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const exp = Date.now() + 30 * 24 * 60 * 60 * 1000; // 30 days
  const body = Buffer.from(JSON.stringify({ ...payload, exp })).toString('base64url');
  const signature = crypto
    .createHmac('sha256', JWT_SECRET)
    .update(`${header}.${body}`)
    .digest('base64url');
  return `${header}.${body}.${signature}`;
}

export function verifyToken(token: string): { userId: string; email: string; role: UserRole } | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const [header, body, signature] = parts;
    const expectedSig = crypto
      .createHmac('sha256', JWT_SECRET)
      .update(`${header}.${body}`)
      .digest('base64url');
    if (signature !== expectedSig) return null;
    const payload = JSON.parse(Buffer.from(body, 'base64url').toString('utf-8'));
    if (payload.exp && Date.now() > payload.exp) return null;
    return { userId: payload.userId, email: payload.email, role: payload.role };
  } catch {
    return null;
  }
}

export function registerUser(
  name: string,
  email: string,
  password: string,
  role: UserRole = 'CITIZEN',
  phone?: string,
  customDb?: DatabaseSync
): { user: Omit<User, 'password_hash' | 'salt'>; token: string } {
  const db = customDb || getDb();
  const normalizedEmail = email.toLowerCase().trim();

  const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(normalizedEmail);
  if (existing) {
    throw new Error('User with this email already exists');
  }

  const { hash, salt } = hashPassword(password);
  const userId = `usr_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
  const now = new Date().toISOString();

  db.prepare(`
    INSERT INTO users (id, name, email, password_hash, salt, role, phone, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).run(userId, name, normalizedEmail, hash, salt, role, phone || null, now);

  const token = generateToken({ userId, email: normalizedEmail, role });
  return {
    user: {
      id: userId,
      name,
      email: normalizedEmail,
      role,
      phone,
      created_at: now
    },
    token
  };
}

export function loginUser(
  email: string,
  password: string,
  customDb?: DatabaseSync
): { user: Omit<User, 'password_hash' | 'salt'>; token: string } {
  const db = customDb || getDb();
  const normalizedEmail = email.toLowerCase().trim();

  const user = db.prepare('SELECT * FROM users WHERE email = ?').get(normalizedEmail) as User | undefined;
  if (!user) {
    throw new Error('Invalid email or password');
  }

  const isValid = verifyPassword(password, user.password_hash, user.salt);
  if (!isValid) {
    throw new Error('Invalid email or password');
  }

  const token = generateToken({ userId: user.id, email: user.email, role: user.role });
  return {
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      phone: user.phone,
      created_at: user.created_at
    },
    token
  };
}
