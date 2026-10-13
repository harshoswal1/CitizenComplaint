# 📜 Civic Accountability Platform — Core Rules Specification

This document details the nine immutable core rules governing the **NagrikVoice** civic platform, ensuring mathematical consistency, preventing duplicate complaints, and enforcing strict municipal accountability.

---

### Rule 1: One Real-World Problem, One Primary Complaint
- **Principle**: Multiple citizens observing the same pothole, garbage mound, or streetlight failure must never inflate the count of primary complaints.
- **Implementation**:
  - Before a complaint is stored, the platform performs a geographic duplicate search (`haversineDistanceMeters <= 100m`) combined with category/subtype matching.
  - If a match is found, the application presents the existing complaint photograph, locality, and description: *"Is this the same physical problem?"*
  - If confirmed, the user is redirected to support the existing complaint record.
  - No new primary record is created, no additional email is sent, and the corporation's unresolved count remains unchanged.

---

### Rule 2: Option B — Email Delivery Determines Ranking Eligibility
- **Principle**: A municipal corporation is accountable only for complaints that have been confirmed as delivered to their grievance channels.
- **Implementation**:
  - Creating a complaint puts `email_status = PENDING` and `resolution_status = PENDING_EMAIL`.
  - While email is `PENDING`, `QUEUED`, `SENDING`, or `FAILED`, the municipal corporation's unresolved count remains untouched.
  - Only when the email service returns delivery confirmation (`DELIVERED`) does the unresolved complaint count increment by 1.
  - **Idempotency**: Duplicate delivery webhooks for an already delivered complaint are discarded and will never double-increment counts.

---

### Rule 3: Failed Delivery Requires Retry
- **Principle**: If email delivery fails (e.g. SMTP bounce, mail server error), the issue must not disappear, nor should it inflate municipal counts.
- **Implementation**:
  - Failed deliveries transition complaint to `DELIVERY_FAILED`.
  - The citizen or administrator is given an in-app "Retry Email" option.
  - The retry strictly reuses the existing complaint ID (`attempt_number = attempt_number + 1`).
  - No new complaint record is created.

---

### Rule 4: Votes Do Not Increase Complaint Counts
- **Principle**: Community votes ("Support") measure citizen concern, not the quantity of physical defects.
- **Implementation**:
  - Authenticated citizens can cast exactly 1 vote per complaint.
  - Pressing support again withdraws the vote.
  - Community votes increase feed visibility and escalation priority, but **never** increment the corporation's unresolved complaint count.

---

### Rule 5: Verified Resolution Decreases the Unresolved Count
- **Principle**: A civic issue leaves the unresolved total only after an independent verification process approves the resolution evidence.
- **Implementation**:
  - When a citizen or authority claims a fix, status becomes `RESOLUTION_CLAIMED`.
  - A claimed fix is **still counted as unresolved** until an administrator or community reviewer approves the evidence.
  - Upon approval (`RESOLVED_VERIFIED`), the unresolved count decrements by 1 and verified resolved increments by 1.
  - If the problem reoccurs, an authorized reopen (`REOPENED`) restores `resolution_status = UNRESOLVED` and increments the unresolved count back.

---

### Rule 6: Every Corporation Starts at Zero
- **Principle**: No fabricated historical data or arbitrary initial rankings are permitted.
- **Implementation**:
  - When Kolhapur Municipal Corporation (or any newly imported authority) is created, its initial `unresolved_count = 0` and `resolved_verified_count = 0`.
  - All metrics are computed dynamically from confirmed, verified complaint records.

---

### Rule 7: Test Delivery Is Not Official Delivery
- **Principle**: During development and testing, emails routed to the test recipient must never be misrepresented as delivered to government authorities.
- **Implementation**:
  - In testing environments, emails carry a prominent disclaimer: `[TESTING ENVIRONMENT NOTICE]: This email was dispatched to test recipient. It has NOT been delivered to the real government authority yet.`
  - The email delivery record explicitly marks `is_test_recipient = 1`.

---

### Rule 8: Ranking Changes Must Be Auditable
- **Principle**: Every change in ranking metrics must be traceable to a specific event.
- **Implementation**:
  - An append-only immutable `audit_logs` table logs every `COMPLAINT_CREATED`, `EMAIL_DELIVERY_CONFIRMED`, `EMAIL_DELIVERY_FAILED`, and `RESOLUTION_VERIFIED_APPROVED`.
  - Status history retains full timestamped records of who changed the state and why.

---

### Rule 9: Dynamic Pan-India Expansion
- **Principle**: The platform must not hardcode Kolhapur in its data model.
- **Implementation**:
  - Authorities, departments, contacts, and category mappings are fully relational and configurable.
  - Any municipal corporation, council, or panchayat across India can be added dynamically via CSV import or admin APIs without code alterations.
