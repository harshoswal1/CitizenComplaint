import * as fs from 'node:fs';
import * as path from 'node:path';
import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface ImportResult {
  authorityId: string;
  totalContacts: number;
  validEmailsCount: number;
  invalidOrNAEmailsCount: number;
  departmentsCount: number;
  categoriesConfigured: number;
}

export function importKmcDirectory(csvFilePath?: string, customDb?: DatabaseSync): ImportResult {
  const db = customDb || getDb();
  const filePath = csvFilePath || path.resolve(import.meta.dirname, '../../data/KMC_All_Departments_Complete_Directory.csv');

  if (!fs.existsSync(filePath)) {
    throw new Error(`CSV file not found at ${filePath}`);
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const lines = content.split(/\r?\n/).filter(line => line.trim().length > 0);
  
  if (lines.length < 2) {
    throw new Error('CSV file is empty or missing data rows');
  }

  const headerLine = lines[0];
  const headers = headerLine.split(',').map(h => h.trim());
  const deptIdx = headers.indexOf('Department');
  const nameIdx = headers.indexOf('Name');
  const desigIdx = headers.indexOf('Designation');
  const phoneIdx = headers.indexOf('Contact_Number');
  const emailIdx = headers.indexOf('Email');

  if (deptIdx === -1 || nameIdx === -1 || desigIdx === -1 || emailIdx === -1) {
    throw new Error(`Invalid CSV header format: ${headerLine}`);
  }

  // 1. Ensure Kolhapur Municipal Corporation Authority exists starting at 0 counts
  const now = new Date().toISOString();
  const kmcId = 'auth_kmc_kolhapur';

  db.exec(`
    INSERT INTO authorities (
      id, name, code, state, district, type, center_lat, center_lng,
      unresolved_count, resolved_verified_count, total_delivered_count,
      failed_delivery_count, pending_email_count, created_at, updated_at
    ) VALUES (
      '${kmcId}', 'Kolhapur Municipal Corporation', 'KMC', 'Maharashtra', 'Kolhapur',
      'MUNICIPAL_CORPORATION', 16.7050, 74.2433, 0, 0, 0, 0, 0, '${now}', '${now}'
    ) ON CONFLICT(id) DO NOTHING;
  `);

  // Track unique departments
  const departmentMap = new Map<string, string>(); // name -> id
  let validEmailsCount = 0;
  let invalidOrNAEmailsCount = 0;
  let totalContacts = 0;

  for (let i = 1; i < lines.length; i++) {
    const rawLine = lines[i].trim();
    if (!rawLine) continue;

    // Handle CSV comma splitting safely
    const parts: string[] = [];
    let current = '';
    let inQuotes = false;
    for (let c = 0; c < rawLine.length; c++) {
      const char = rawLine[c];
      if (char === '"') {
        inQuotes = !inQuotes;
      } else if (char === ',' && !inQuotes) {
        parts.push(current.trim());
        current = '';
      } else {
        current += char;
      }
    }
    parts.push(current.trim());

    if (parts.length < 5) continue;

    const deptName = parts[deptIdx];
    const contactName = parts[nameIdx];
    const designation = parts[desigIdx];
    const contactNumber = parts[phoneIdx] || '';
    const rawEmail = parts[emailIdx] || '';

    // Check / create Department
    let deptId = departmentMap.get(deptName);
    if (!deptId) {
      deptId = `dept_${deptName.toLowerCase().replace(/[^a-z0-9]/g, '_')}`;
      departmentMap.set(deptName, deptId);

      const deptCode = deptName.substring(0, 4).toUpperCase();
      const stmtDept = db.prepare(`
        INSERT INTO departments (id, authority_id, name, code, contact_number, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET contact_number = excluded.contact_number;
      `);
      stmtDept.run(deptId, kmcId, deptName, deptCode, contactNumber, now);
    }

    // Email validation
    const isEmailValid = rawEmail !== 'N/A' && EMAIL_REGEX.test(rawEmail);
    if (isEmailValid) {
      validEmailsCount++;
    } else {
      invalidOrNAEmailsCount++;
    }

    const verificationStatus = isEmailValid ? 'VERIFIED' : 'UNVERIFIED';
    const contactId = `contact_kmc_${i}`;

    const stmtContact = db.prepare(`
      INSERT INTO authority_contacts (
        id, authority_id, department_id, department_name, name, designation,
        contact_number, email, is_email_valid, verification_status, last_verified_at,
        source_file, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        contact_number = excluded.contact_number,
        email = excluded.email,
        is_email_valid = excluded.is_email_valid,
        verification_status = excluded.verification_status;
    `);

    stmtContact.run(
      contactId,
      kmcId,
      deptId,
      deptName,
      contactName,
      designation,
      contactNumber,
      rawEmail === 'N/A' ? null : rawEmail,
      isEmailValid ? 1 : 0,
      verificationStatus,
      isEmailValid ? now : null,
      'KMC_All_Departments_Complete_Directory.csv',
      now
    );

    totalContacts++;
  }

  // Configure issue category to department routing mappings
  const categoryConfigs = [
    {
      cat: 'ROADS_AND_INFRASTRUCTURE',
      deptName: 'City Engineer / PWD',
      email: 'cityengineerkmc@rediffmail.com',
      notes: 'Potholes, damaged roads, broken footpaths, road signage'
    },
    {
      cat: 'WATER_AND_DRAINAGE',
      deptName: 'City Engineer / PWD',
      email: 'dyce1kmc@gmail.com',
      notes: 'Water supply, pipeline leakage, drainage overflow, waterlogging'
    },
    {
      cat: 'WASTE_MANAGEMENT',
      deptName: 'Health Department',
      email: 'kmchealthofficer14@gmail.com',
      notes: 'Garbage accumulation, irregular collection, illegal dumping'
    },
    {
      cat: 'ELECTRICITY_AND_LIGHTING',
      deptName: 'Electricity & Street Lights',
      email: 'prokmc29@gmail.com', // Routed to commissioner desk with helpline tag since EESL is tollfree
      notes: 'Broken streetlights, dangling wires, public lighting faults. Helpline: 1800-123-2267'
    },
    {
      cat: 'PUBLIC_HEALTH_AND_SANITATION',
      deptName: 'Health Department',
      email: 'kmchealthofficer14@gmail.com',
      notes: 'Public sanitation, disease vectors, unsanitary public spaces'
    },
    {
      cat: 'PROPERTY_TAX',
      deptName: 'Property Tax Division',
      email: 'propertytaxmain@gmail.com',
      notes: 'Assessment queries, municipal tax billing issues'
    },
    {
      cat: 'DISASTER_AND_SAFETY',
      deptName: 'Disaster Management',
      email: 'prokmc29@gmail.com',
      notes: 'Flooding hazards, wall collapses, emergency civic hazards. Control room: 0231-2541188'
    },
    {
      cat: 'OTHER_CIVIC_ISSUES',
      deptName: 'General Administration',
      email: 'prokmc29@gmail.com',
      notes: 'General civic complaints requiring administrative direction'
    }
  ];

  let categoriesConfigured = 0;
  for (const cfg of categoryConfigs) {
    const deptId = departmentMap.get(cfg.deptName) || `dept_${cfg.deptName.toLowerCase().replace(/[^a-z0-9]/g, '_')}`;
    const mapId = `map_kmc_${cfg.cat.toLowerCase()}`;
    const stmtMap = db.prepare(`
      INSERT INTO category_mappings (
        id, authority_id, category, department_id, department_name, default_official_email, routing_notes
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        default_official_email = excluded.default_official_email,
        routing_notes = excluded.routing_notes;
    `);
    stmtMap.run(mapId, kmcId, cfg.cat, deptId, cfg.deptName, cfg.email, cfg.notes);
    categoriesConfigured++;
  }

  return {
    authorityId: kmcId,
    totalContacts,
    validEmailsCount,
    invalidOrNAEmailsCount,
    departmentsCount: departmentMap.size,
    categoriesConfigured
  };
}
