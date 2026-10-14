import * as fs from 'node:fs';
import * as path from 'node:path';
import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface AuthoritySummary {
  id: string;
  name: string;
  code: string;
  city: string;
  district: string;
  type: string;
  lat: number;
  lng: number;
}

// Center Coordinates for all 32 cities/districts in Maharashtra dataset
const CITY_COORDINATES: Record<string, { lat: number; lng: number }> = {
  'Ahilyanagar (Ahmednagar)': { lat: 19.0952, lng: 74.7496 },
  'Akola': { lat: 20.7002, lng: 77.0082 },
  'Amravati': { lat: 20.9374, lng: 77.7796 },
  'Baramati': { lat: 18.1517, lng: 74.5771 },
  'Beed': { lat: 18.9891, lng: 75.7601 },
  'Bhayandar': { lat: 19.2952, lng: 72.8544 },
  'Bhiwandi': { lat: 19.2967, lng: 73.0631 },
  'Chandrapur': { lat: 19.9615, lng: 79.2961 },
  'Chhatrapati Sambhajinagar': { lat: 19.8762, lng: 75.3433 },
  'Dhule': { lat: 20.9042, lng: 74.7749 },
  'Gondia': { lat: 21.4598, lng: 80.1961 },
  'Jalgaon': { lat: 21.0077, lng: 75.5626 },
  'Jalna': { lat: 19.8347, lng: 75.8816 },
  'Kalyan': { lat: 19.2403, lng: 73.1305 },
  'Kolhapur': { lat: 16.7050, lng: 74.2433 },
  'Latur': { lat: 18.4088, lng: 76.5604 },
  'Malegaon': { lat: 20.5579, lng: 74.5089 },
  'Mumbai': { lat: 19.0760, lng: 72.8777 },
  'Nanded': { lat: 19.1383, lng: 77.3210 },
  'Nashik': { lat: 19.9975, lng: 73.7898 },
  'Panvel': { lat: 18.9894, lng: 73.1175 },
  'Parbhani': { lat: 19.2644, lng: 76.7767 },
  'Pimpri': { lat: 18.6298, lng: 73.7997 },
  'Pune': { lat: 18.5204, lng: 73.8567 },
  'Sangli': { lat: 16.8524, lng: 74.5815 },
  'Satara': { lat: 17.6805, lng: 73.9997 },
  'Solapur': { lat: 17.6599, lng: 75.9064 },
  'Thane': { lat: 19.2183, lng: 72.9781 },
  'Ulhasnagar': { lat: 19.2183, lng: 73.1557 },
  'Vasai': { lat: 19.3919, lng: 72.8397 },
  'Wardha': { lat: 20.7453, lng: 78.6022 },
  'Yavatmal': { lat: 20.3888, lng: 78.1204 }
};

export function importMaharashtraDirectory(csvFilePath?: string, customDb?: DatabaseSync): {
  authoritiesCount: number;
  contactsCount: number;
  departmentsCount: number;
  authorities: AuthoritySummary[];
} {
  const db = customDb || getDb();
  const filePath = csvFilePath || path.resolve(import.meta.dirname, '../../data/Maharashtra_All_Authorities_Directory.csv');

  if (!fs.existsSync(filePath)) {
    throw new Error(`CSV file not found at ${filePath}`);
  }

  const content = fs.readFileSync(filePath, 'utf-8');
  const lines = content.split(/\r?\n/).filter(line => line.trim().length > 0);
  if (lines.length < 2) {
    throw new Error('CSV file has no data rows');
  }

  const header = lines[0].split(',').map(h => h.trim());
  const cityIdx = header.indexOf('City / Urban Area');
  const authIdx = header.indexOf('Governing Authority Name');
  const typeIdx = header.indexOf('Governance Type');
  const distIdx = header.indexOf('District');
  const deptIdx = header.indexOf('Department');
  const desigIdx = header.indexOf('Designation');
  const phoneIdx = header.indexOf('Contact_Number');
  const emailIdx = header.indexOf('Email');

  const now = new Date().toISOString();

  // Helper to make clean authority id
  function makeAuthorityId(cityName: string, authName: string): string {
    const cleanCity = cityName.split(' ')[0].toLowerCase().replace(/[^a-z0-9]/g, '');
    if (cleanCity === 'kolhapur') return 'auth_kmc_kolhapur';
    return `auth_${cleanCity}`;
  }

  // Helper to extract unique code (e.g. PMC_PUNE, PMC_PANVEL, BMC_MUMBAI)
  function extractCode(authName: string, cityName: string): string {
    const cleanCity = cityName.split(' ')[0].toUpperCase().replace(/[^A-Z0-9]/g, '');
    const match = authName.match(/\(([A-Za-z0-9]+)\)/);
    if (match) return `${match[1].toUpperCase()}_${cleanCity}`;
    return `${cleanCity}_MC`;
  }

  const authorityMap = new Map<string, AuthoritySummary>();
  const departmentMap = new Map<string, string>(); // authId + "_" + deptName -> deptId
  const authorityEmailsByDept = new Map<string, Map<string, string>>(); // authId -> (deptCategory -> email)

  let contactsCount = 0;

  for (let i = 1; i < lines.length; i++) {
    const rawLine = lines[i].trim();
    if (!rawLine) continue;

    // Parse CSV line with quotes safely
    const parts: string[] = [];
    let cur = '';
    let inQuotes = false;
    for (let c = 0; c < rawLine.length; c++) {
      const ch = rawLine[c];
      if (ch === '"') {
        inQuotes = !inQuotes;
      } else if (ch === ',' && !inQuotes) {
        parts.push(cur.trim());
        cur = '';
      } else {
        cur += ch;
      }
    }
    parts.push(cur.trim());

    if (parts.length < 8) continue;

    const city = parts[cityIdx];
    const authName = parts[authIdx];
    const govType = parts[typeIdx];
    const district = parts[distIdx];
    const deptName = parts[deptIdx];
    const designation = parts[desigIdx];
    const contactNumber = parts[phoneIdx] === 'N/A' ? '' : parts[phoneIdx];
    const rawEmailField = parts[emailIdx] === 'N/A' ? '' : parts[emailIdx];

    // Pick first valid email if multiple are listed
    let email = '';
    if (rawEmailField) {
      const splitEmails = rawEmailField.split(',').map(e => e.trim());
      email = splitEmails.find(e => EMAIL_REGEX.test(e)) || '';
    }

    const authId = makeAuthorityId(city, authName);
    const code = extractCode(authName, city);
    const coords = CITY_COORDINATES[city] || { lat: 18.5204, lng: 73.8567 };

    // 1. Ensure Authority exists
    if (!authorityMap.has(authId)) {
      const summary: AuthoritySummary = {
        id: authId,
        name: authName,
        code,
        city,
        district,
        type: govType.toUpperCase().includes('COUNCIL') ? 'MUNICIPAL_COUNCIL' : 'MUNICIPAL_CORPORATION',
        lat: coords.lat,
        lng: coords.lng
      };
      authorityMap.set(authId, summary);

      db.prepare(`
        INSERT INTO authorities (
          id, name, code, state, district, type, center_lat, center_lng,
          unresolved_count, resolved_verified_count, total_delivered_count,
          failed_delivery_count, pending_email_count, created_at, updated_at
        ) VALUES (
          ?, ?, ?, 'Maharashtra', ?, ?, ?, ?,
          0, 0, 0, 0, 0, ?, ?
        ) ON CONFLICT(id) DO UPDATE SET
          name = excluded.name,
          code = excluded.code,
          district = excluded.district,
          type = excluded.type,
          center_lat = excluded.center_lat,
          center_lng = excluded.center_lng;
      `).run(authId, authName, code, district, summary.type, coords.lat, coords.lng, now, now);
    }

    // 2. Ensure Department exists
    const deptKey = `${authId}_${deptName}`;
    let deptId = departmentMap.get(deptKey);
    if (!deptId) {
      deptId = `dept_${authId.replace('auth_', '')}_${deptName.toLowerCase().replace(/[^a-z0-9]/g, '_')}`.substring(0, 50);
      departmentMap.set(deptKey, deptId);

      const deptCode = deptName.substring(0, 4).toUpperCase();
      db.prepare(`
        INSERT INTO departments (id, authority_id, name, code, primary_email, contact_number, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          contact_number = COALESCE(NULLIF(excluded.contact_number, ''), departments.contact_number),
          primary_email = COALESCE(NULLIF(excluded.primary_email, ''), departments.primary_email);
      `).run(deptId, authId, deptName, deptCode, email || null, contactNumber || null, now);
    }

    // Record email for routing
    if (email) {
      if (!authorityEmailsByDept.has(authId)) {
        authorityEmailsByDept.set(authId, new Map());
      }
      const deptEmails = authorityEmailsByDept.get(authId)!;
      if (!deptEmails.has(deptName)) {
        deptEmails.set(deptName, email);
      }
    }

    // 3. Insert Authority Contact
    const contactId = `contact_mh_${i}`;
    const isEmailValid = email ? 1 : 0;
    const verificationStatus = isEmailValid ? 'VERIFIED' : 'UNVERIFIED';

    db.prepare(`
      INSERT INTO authority_contacts (
        id, authority_id, department_id, department_name, name, designation,
        contact_number, email, is_email_valid, verification_status, last_verified_at,
        source_file, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Maharashtra_All_Authorities_Directory.csv', ?)
      ON CONFLICT(id) DO UPDATE SET
        contact_number = excluded.contact_number,
        email = excluded.email,
        is_email_valid = excluded.is_email_valid,
        verification_status = excluded.verification_status;
    `).run(
      contactId,
      authId,
      deptId,
      deptName,
      designation,
      designation,
      contactNumber || null,
      email || null,
      isEmailValid,
      verificationStatus,
      isEmailValid ? now : null,
      now
    );

    contactsCount++;
  }

  // 4. Configure category routing for all authorities
  for (const [authId, auth] of authorityMap.entries()) {
    const deptEmails = authorityEmailsByDept.get(authId) || new Map<string, string>();

    const pwdEmail = deptEmails.get('Public Works Department (PWD)') || '';
    const waterEmail = deptEmails.get('Water Department') || pwdEmail;
    const electricityEmail = deptEmails.get('Electricity Department (MSEDCL)') || '';
    const healthEmail = deptEmails.get('Healthcare / District Health Office') || '';
    const taxEmail = deptEmails.get('Property Tax Department') || '';
    const disasterEmail = deptEmails.get('Disaster Management') || '';

    // Fallback general email
    const generalEmail = taxEmail || pwdEmail || waterEmail || healthEmail || disasterEmail || 'commissioner@nagrikvoice.org';

    const categoryMappings = [
      { cat: 'ROADS_AND_INFRASTRUCTURE', dept: 'Public Works Department (PWD)', email: pwdEmail || generalEmail, notes: 'Roads, potholes, footpaths, bridges' },
      { cat: 'WATER_AND_DRAINAGE', dept: 'Water Department', email: waterEmail || generalEmail, notes: 'Water supply, drainage leaks, pipe bursts' },
      { cat: 'WASTE_MANAGEMENT', dept: 'Healthcare / District Health Office', email: healthEmail || generalEmail, notes: 'Solid waste management, garbage accumulation' },
      { cat: 'ELECTRICITY_AND_LIGHTING', dept: 'Electricity Department (MSEDCL)', email: electricityEmail || generalEmail, notes: 'Street lights, electricity poles, power faults' },
      { cat: 'PUBLIC_HEALTH_AND_SANITATION', dept: 'Healthcare / District Health Office', email: healthEmail || generalEmail, notes: 'Public health, pest control, sanitation' },
      { cat: 'PROPERTY_TAX', dept: 'Property Tax Department', email: taxEmail || generalEmail, notes: 'Property tax assessment and billing' },
      { cat: 'DISASTER_AND_SAFETY', dept: 'Disaster Management', email: disasterEmail || generalEmail, notes: 'Emergency hazard, flooding, wall collapse' },
      { cat: 'OTHER_CIVIC_ISSUES', dept: 'General Administration', email: generalEmail, notes: 'General administrative civic issue' }
    ];

    for (const mapping of categoryMappings) {
      const deptKey = `${authId}_${mapping.dept}`;
      let deptId = departmentMap.get(deptKey);
      if (!deptId) {
        deptId = `dept_${authId.replace('auth_', '')}_${mapping.dept.toLowerCase().replace(/[^a-z0-9]/g, '_')}`.substring(0, 50);
        db.prepare(`
          INSERT INTO departments (id, authority_id, name, code, primary_email, created_at)
          VALUES (?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO NOTHING;
        `).run(deptId, authId, mapping.dept, mapping.dept.substring(0, 4).toUpperCase(), mapping.email || null, now);
        departmentMap.set(deptKey, deptId);
      }

      const mapId = `map_${authId.replace('auth_', '')}_${mapping.cat.toLowerCase()}`;

      db.prepare(`
        INSERT INTO category_mappings (
          id, authority_id, category, department_id, department_name, default_official_email, routing_notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
          default_official_email = excluded.default_official_email,
          routing_notes = excluded.routing_notes;
      `).run(mapId, authId, mapping.cat, deptId, mapping.dept, mapping.email, mapping.notes);
    }
  }

  const allAuthorities = Array.from(authorityMap.values());

  return {
    authoritiesCount: authorityMap.size,
    contactsCount,
    departmentsCount: departmentMap.size,
    authorities: allAuthorities
  };
}
