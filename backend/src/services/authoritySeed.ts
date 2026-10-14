import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';

export interface MunicipalCorpSeed {
  id: string;
  name: string;
  code: string;
  state: string;
  district: string;
  lat: number;
  lng: number;
  email: string;
}

export const MAJOR_MUNICIPAL_CORPORATIONS: MunicipalCorpSeed[] = [
  {
    id: 'auth_kmc_kolhapur',
    name: 'Kolhapur Municipal Corporation (KMC)',
    code: 'KMC',
    state: 'Maharashtra',
    district: 'Kolhapur',
    lat: 16.7050,
    lng: 74.2433,
    email: 'prokmc29@gmail.com'
  },
  {
    id: 'auth_bmc_mumbai',
    name: 'Brihanmumbai Municipal Corporation (BMC)',
    code: 'BMC',
    state: 'Maharashtra',
    district: 'Mumbai',
    lat: 19.0760,
    lng: 72.8777,
    email: 'mc@mcgm.gov.in'
  },
  {
    id: 'auth_pmc_pune',
    name: 'Pune Municipal Corporation (PMC)',
    code: 'PMC',
    state: 'Maharashtra',
    district: 'Pune',
    lat: 18.5204,
    lng: 73.8567,
    email: 'commissioner@punecorporation.org'
  },
  {
    id: 'auth_mcd_delhi',
    name: 'Municipal Corporation of Delhi (MCD)',
    code: 'MCD',
    state: 'Delhi',
    district: 'Delhi',
    lat: 28.6139,
    lng: 77.2090,
    email: 'commissioner@mcd.nic.in'
  },
  {
    id: 'auth_bbmp_bengaluru',
    name: 'Bruhat Bengaluru Mahanagara Palike (BBMP)',
    code: 'BBMP',
    state: 'Karnataka',
    district: 'Bengaluru',
    lat: 12.9716,
    lng: 77.5946,
    email: 'comm@bbmp.gov.in'
  },
  {
    id: 'auth_gcc_chennai',
    name: 'Greater Chennai Corporation (GCC)',
    code: 'GCC',
    state: 'Tamil Nadu',
    district: 'Chennai',
    lat: 13.0827,
    lng: 80.2707,
    email: 'commissioner@chennaicorporation.gov.in'
  },
  {
    id: 'auth_kmc_kolkata',
    name: 'Kolkata Municipal Corporation (KMC)',
    code: 'KolkataMC',
    state: 'West Bengal',
    district: 'Kolkata',
    lat: 22.5726,
    lng: 88.3639,
    email: 'mc@kmcgov.in'
  },
  {
    id: 'auth_ghmc_hyderabad',
    name: 'Greater Hyderabad Municipal Corporation (GHMC)',
    code: 'GHMC',
    state: 'Telangana',
    district: 'Hyderabad',
    lat: 17.3850,
    lng: 78.4867,
    email: 'commissioner-ghmc@gov.in'
  },
  {
    id: 'auth_amc_ahmedabad',
    name: 'Ahmedabad Municipal Corporation (AMC)',
    code: 'AMC',
    state: 'Gujarat',
    district: 'Ahmedabad',
    lat: 23.0225,
    lng: 72.5714,
    email: 'comm@ahmedabadcity.gov.in'
  },
  {
    id: 'auth_jmc_jaipur',
    name: 'Jaipur Municipal Corporation (JMC)',
    code: 'JMC',
    state: 'Rajasthan',
    district: 'Jaipur',
    lat: 26.9124,
    lng: 75.7873,
    email: 'comm.jaipur@rajasthan.gov.in'
  },
  {
    id: 'auth_smc_surat',
    name: 'Surat Municipal Corporation (SMC)',
    code: 'SMC',
    state: 'Gujarat',
    district: 'Surat',
    lat: 21.1702,
    lng: 72.8311,
    email: 'commissioner@suratmunicipal.gov.in'
  },
  {
    id: 'auth_nmc_nagpur',
    name: 'Nagpur Municipal Corporation (NMC)',
    code: 'NMC',
    state: 'Maharashtra',
    district: 'Nagpur',
    lat: 21.1458,
    lng: 79.0882,
    email: 'mconagpur@gmail.com'
  },
  {
    id: 'auth_lmc_lucknow',
    name: 'Lucknow Municipal Corporation (LMC)',
    code: 'LMC',
    state: 'Uttar Pradesh',
    district: 'Lucknow',
    lat: 26.8467,
    lng: 80.9462,
    email: 'nnlko@nic.in'
  },
  {
    id: 'auth_pmc_patna',
    name: 'Patna Municipal Corporation (PMC)',
    code: 'PatnaMC',
    state: 'Bihar',
    district: 'Patna',
    lat: 25.5941,
    lng: 85.1376,
    email: 'patnamunicipalcorporation@gmail.com'
  },
  {
    id: 'auth_imc_indore',
    name: 'Indore Municipal Corporation (IMC)',
    code: 'IMC',
    state: 'Madhya Pradesh',
    district: 'Indore',
    lat: 22.7196,
    lng: 75.8577,
    email: 'commissioner@imcindore.org'
  },
  {
    id: 'auth_bmc_bhopal',
    name: 'Bhopal Municipal Corporation (BMC)',
    code: 'BhopalMC',
    state: 'Madhya Pradesh',
    district: 'Bhopal',
    lat: 23.2599,
    lng: 77.4126,
    email: 'commissioner@bmconline.gov.in'
  },
  {
    id: 'auth_tmc_thiruvananthapuram',
    name: 'Thiruvananthapuram Municipal Corporation',
    code: 'TMC',
    state: 'Kerala',
    district: 'Thiruvananthapuram',
    lat: 8.5241,
    lng: 76.9366,
    email: 'secretary@corporationoftrivandrum.in'
  },
  {
    id: 'auth_cmc_coimbatore',
    name: 'Coimbatore City Municipal Corporation (CCMC)',
    code: 'CCMC',
    state: 'Tamil Nadu',
    district: 'Coimbatore',
    lat: 11.0168,
    lng: 76.9558,
    email: 'commr.coimbatore@tn.gov.in'
  },
  {
    id: 'auth_nmc_nashik',
    name: 'Nashik Municipal Corporation (NMC)',
    code: 'NashikMC',
    state: 'Maharashtra',
    district: 'Nashik',
    lat: 19.9975,
    lng: 73.7898,
    email: 'commissioner@nmc.gov.in'
  },
  {
    id: 'auth_vmc_vadodara',
    name: 'Vadodara Municipal Corporation (VMC)',
    code: 'VMC',
    state: 'Gujarat',
    district: 'Vadodara',
    lat: 22.3072,
    lng: 73.1812,
    email: 'commissioner@vmc.gov.in'
  }
];

export function seedMajorAuthorities(customDb?: DatabaseSync): void {
  const db = customDb || getDb();
  const now = new Date().toISOString();

  const standardCategories = [
    { cat: 'ROADS_AND_INFRASTRUCTURE', dept: 'Public Works & Roads', email: 'pwd@' },
    { cat: 'WATER_AND_DRAINAGE', dept: 'Water Supply & Sewerage', email: 'water@' },
    { cat: 'WASTE_MANAGEMENT', dept: 'Solid Waste Management', email: 'sanitation@' },
    { cat: 'ELECTRICITY_AND_LIGHTING', dept: 'Electrical & Street Lighting', email: 'lighting@' },
    { cat: 'PUBLIC_HEALTH_AND_SANITATION', dept: 'Public Health Department', email: 'health@' },
    { cat: 'PROPERTY_TAX', dept: 'Revenue & Assessment', email: 'tax@' },
    { cat: 'DISASTER_AND_SAFETY', dept: 'Disaster Management Cell', email: 'disaster@' },
    { cat: 'OTHER_CIVIC_ISSUES', dept: 'General Administration', email: 'admin@' }
  ];

  for (const corp of MAJOR_MUNICIPAL_CORPORATIONS) {
    db.prepare(`
      INSERT INTO authorities (
        id, name, code, state, district, type, center_lat, center_lng,
        unresolved_count, resolved_verified_count, total_delivered_count,
        failed_delivery_count, pending_email_count, created_at, updated_at
      ) VALUES (
        ?, ?, ?, ?, ?, 'MUNICIPAL_CORPORATION', ?, ?,
        0, 0, 0, 0, 0, ?, ?
      ) ON CONFLICT(id) DO UPDATE SET
        name = excluded.name,
        code = excluded.code,
        state = excluded.state,
        district = excluded.district,
        center_lat = excluded.center_lat,
        center_lng = excluded.center_lng;
    `).run(corp.id, corp.name, corp.code, corp.state, corp.district, corp.lat, corp.lng, now, now);

    // Create default department & category mappings for non-KMC authorities
    if (corp.id !== 'auth_kmc_kolhapur') {
      for (const item of standardCategories) {
        const deptId = `dept_${corp.code.toLowerCase()}_${item.cat.toLowerCase()}`;
        db.prepare(`
          INSERT INTO departments (id, authority_id, name, code, primary_email, created_at)
          VALUES (?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO NOTHING;
        `).run(deptId, corp.id, item.dept, corp.code.substring(0, 4), corp.email, now);

        const mapId = `map_${corp.code.toLowerCase()}_${item.cat.toLowerCase()}`;
        db.prepare(`
          INSERT INTO category_mappings (
            id, authority_id, category, department_id, department_name, default_official_email, routing_notes
          ) VALUES (?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET
            default_official_email = excluded.default_official_email;
        `).run(mapId, corp.id, item.cat, deptId, item.dept, corp.email, `${corp.name} official civic routing`);
      }
    }
  }
}
