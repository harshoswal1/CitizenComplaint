import { DatabaseSync } from 'node:sqlite';
import { getDb } from '../db/db.ts';

export interface DuplicateCandidate {
  id: string;
  title: string;
  description: string;
  photo_url: string;
  category: string;
  subtype?: string;
  locality: string;
  latitude: number;
  longitude: number;
  distance_meters: number;
  resolution_status: string;
  email_status: string;
  support_count: number;
  created_at: string;
}

const EARTH_RADIUS_METERS = 6371000;

export function haversineDistanceMeters(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) *
      Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return EARTH_RADIUS_METERS * c;
}

export function findNearbyDuplicates(
  latitude: number,
  longitude: number,
  category: string,
  subtype?: string,
  radiusMeters: number = 100,
  customDb?: DatabaseSync
): DuplicateCandidate[] {
  const db = customDb || getDb();

  // Bounding box pre-filter for index optimization
  // 1 degree lat is ~111,320 meters
  const latDelta = radiusMeters / 111320;
  const lonDelta = radiusMeters / (111320 * Math.cos((latitude * Math.PI) / 180));

  const minLat = latitude - latDelta;
  const maxLat = latitude + latDelta;
  const minLon = longitude - lonDelta;
  const maxLon = longitude + lonDelta;

  // Search active complaints within bounding box and matching category
  const stmt = db.prepare(`
    SELECT id, title, description, photo_url, category, subtype, locality,
           latitude, longitude, resolution_status, email_status, support_count, created_at
    FROM complaints
    WHERE category = ?
      AND resolution_status IN ('PENDING_EMAIL', 'DELIVERY_FAILED', 'UNRESOLVED', 'RESOLUTION_CLAIMED', 'RESOLUTION_UNDER_REVIEW')
      AND is_hidden = 0
      AND latitude BETWEEN ? AND ?
      AND longitude BETWEEN ? AND ?
  `);

  const candidates = stmt.all(category, minLat, maxLat, minLon, maxLon) as any[];
  const matches: DuplicateCandidate[] = [];

  for (const c of candidates) {
    const dist = haversineDistanceMeters(latitude, longitude, c.latitude, c.longitude);
    if (dist <= radiusMeters) {
      matches.push({
        id: c.id,
        title: c.title,
        description: c.description,
        photo_url: c.photo_url,
        category: c.category,
        subtype: c.subtype,
        locality: c.locality,
        latitude: c.latitude,
        longitude: c.longitude,
        distance_meters: Math.round(dist * 10) / 10,
        resolution_status: c.resolution_status,
        email_status: c.email_status,
        support_count: c.support_count,
        created_at: c.created_at
      });
    }
  }

  // Sort by closest distance
  matches.sort((a, b) => a.distance_meters - b.distance_meters);
  return matches;
}
