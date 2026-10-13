export type EmailStatus = 
  | 'PENDING' 
  | 'QUEUED' 
  | 'SENDING' 
  | 'SENT' 
  | 'DELIVERED' 
  | 'FAILED' 
  | 'BOUNCED';

export type ResolutionStatus = 
  | 'PENDING_EMAIL' 
  | 'DELIVERY_FAILED' 
  | 'UNRESOLVED' 
  | 'RESOLUTION_CLAIMED' 
  | 'RESOLUTION_UNDER_REVIEW' 
  | 'RESOLVED_VERIFIED' 
  | 'REOPENED' 
  | 'INVALIDATED';

export type UserRole = 'CITIZEN' | 'ADMIN';

export interface User {
  id: string;
  name: string;
  email: string;
  password_hash: string;
  salt: string;
  role: UserRole;
  phone?: string;
  created_at: string;
}

export interface Authority {
  id: string;
  name: string;
  code: string;
  state: string;
  district: string;
  type: string;
  center_lat?: number;
  center_lng?: number;
  unresolved_count: number;
  resolved_verified_count: number;
  total_delivered_count: number;
  failed_delivery_count: number;
  pending_email_count: number;
  created_at: string;
  updated_at: string;
}

export interface Department {
  id: string;
  authority_id: string;
  name: string;
  code: string;
  primary_email?: string;
  contact_number?: string;
  created_at: string;
}

export interface AuthorityContact {
  id: string;
  authority_id: string;
  department_id?: string;
  department_name: string;
  name: string;
  designation: string;
  contact_number?: string;
  email?: string;
  is_email_valid: number;
  verification_status: 'UNVERIFIED' | 'VERIFIED' | 'INVALID';
  last_verified_at?: string;
  source_file: string;
  created_at: string;
}

export interface CategoryMapping {
  id: string;
  authority_id: string;
  category: string;
  subtype?: string;
  department_id: string;
  department_name: string;
  default_official_email?: string;
  routing_notes?: string;
}

export interface Complaint {
  id: string;
  reporter_id: string;
  title: string;
  description: string;
  category: string;
  subtype?: string;
  photo_url: string;
  latitude: number;
  longitude: number;
  location_accuracy?: number;
  landmark?: string;
  locality: string;
  administrative_area: string;
  authority_id: string;
  department_id?: string;
  email_status: EmailStatus;
  resolution_status: ResolutionStatus;
  support_count: number;
  is_overdue: number;
  submission_timestamp: string;
  camera_capture_timestamp?: string;
  delivered_timestamp?: string;
  resolved_timestamp?: string;
  flagged_count: number;
  is_hidden: number;
  created_at: string;
  updated_at: string;
}

export interface EmailDelivery {
  id: string;
  complaint_id: string;
  attempt_number: number;
  recipient_email: string;
  is_test_recipient: number;
  subject: string;
  body: string;
  status: EmailStatus;
  provider_reference?: string;
  idempotency_key?: string;
  error_message?: string;
  created_at: string;
  sent_at?: string;
  delivered_at?: string;
}

export interface StatusHistory {
  id: string;
  complaint_id: string;
  previous_status: ResolutionStatus;
  new_status: ResolutionStatus;
  changed_by_user_id?: string;
  reason: string;
  timestamp: string;
}

export interface ResolutionRecord {
  id: string;
  complaint_id: string;
  proposed_by_user_id: string;
  evidence_description: string;
  evidence_photo_url?: string;
  status: 'PROPOSED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
  reviewed_by_user_id?: string;
  review_notes?: string;
  reviewed_at?: string;
  created_at: string;
}

export interface AuditLog {
  id: string;
  user_id?: string;
  action: string;
  target_type: string;
  target_id: string;
  details_json?: string;
  created_at: string;
}

export interface CorporationRanking {
  authority_id: string;
  name: string;
  code: string;
  state: string;
  district: string;
  unresolved_count: number;
  resolved_verified_count: number;
  awaiting_email_count: number;
  failed_delivery_count: number;
  overdue_count: number;
  total_supports: number;
  resolution_rate_percent: number;
  avg_resolution_hours?: number;
}
