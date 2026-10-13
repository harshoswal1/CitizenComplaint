# 🏛️ NagrikVoice Platform Architecture

NagrikVoice is a native Android-driven civic accountability system for India designed to transform citizen complaint reporting into transparent municipal performance tracking.

---

## 1. System Overview

```
 ┌────────────────────────────────────────────────────────┐
 │            Native Android App (Kotlin)                 │
 │  - Jetpack Compose + Material 3 UI                    │
 │  - In-App CameraX (Authenticity metadata capture)      │
 │  - GPS Location & Hierarchical Manual Selection        │
 │  - Instagram-style Civic Feed & Duplicate Dialog       │
 │  - Municipal Corporation Leaderboard                   │
 └──────────────────────────┬─────────────────────────────┘
                            │ REST API (JSON)
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │           NagrikVoice Backend Service                  │
 │  - Node.js runtime with native SQLite database         │
 │  - Centralized Email Dispatch & Provider Webhooks      │
 │  - Option B Ranking Engine (Delivered - Resolved)      │
 │  - Geo-proximity Duplicate Detection (Haversine)       │
 │  - Immutable Audit Trail & Status History              │
 │  - Admin Dashboard & Directory Management              │
 └──────────────────────────┬─────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
 ┌──────────────────────┐       ┌──────────────────────┐
 │  Email Dispatcher    │       │  Directory Importer  │
 │  - Configurable Test │       │  - KMC 49 Contacts   │
 │    Recipient         │       │  - 7 Departments     │
 │  - Official Routing  │       │  - Pan-India Model   │
 └──────────────────────┘       └──────────────────────┘
```

---

## 2. Native Android Application Structure

- **Framework**: Kotlin 2.0, Android SDK 34 (minSdk 26).
- **Presentation**: Jetpack Compose with Material 3 design system, custom typography, and Indian tricolor palette accents.
- **Architecture**: MVVM with unidirectional data flow (StateFlow), clean separation of data/domain/UI layers.
- **Networking**: Retrofit 2 + OkHttp 4 with Bearer auth token interceptor and logging.
- **Camera Capture**: In-app camera interface enforcing direct photo capture to preserve authentic timestamps and coordinate metadata.
- **Location Management**:
  - *Current Location*: Captured via device GPS for reporting verified issues.
  - *Manual Location*: Hierarchical selection (State -> District -> Locality) for browsing feeds when GPS is unavailable or denied.
- **Navigation**: Navigation Compose with bottom navigation bar (Home, Explore, Report, Rankings, Profile) and deep linking.

---

## 3. Backend Data Model

### Key Entities
1. **Authorities**: Municipal corporations across India (`code`, `unresolved_count`, `resolved_verified_count`, `total_delivered_count`, `failed_delivery_count`).
2. **Departments**: Specific administrative units (e.g. City Engineer / PWD, Health Department, Electricity & Street Lights).
3. **Authority Contacts**: Directory parsed from CSV (`contact_number`, `email`, `verification_status`).
4. **Category Mappings**: Configurable routing rules matching categories (Roads, Water, Waste, Electricity, Health, Tax, Disaster) to official department emails.
5. **Complaints**: Primary issue records (`id` e.g. `KMC-2026-0001`, `email_status`, `resolution_status`, `support_count`, `is_overdue`).
6. **Email Deliveries**: Record of each delivery attempt, recipient address, provider acknowledgement token, and delivery status.
7. **Resolution Records**: Proposed resolution claims, repair descriptions, and administrative review decisions.
8. **Audit Logs**: Immutable log of every system state transition.

---

## 4. Kolhapur Municipal Corporation Dataset

The initial contact directory was imported directly from `KMC_All_Departments_Complete_Directory.csv`:
- **Total Contact Records**: 49
- **Departments**: 7 (General Administration, City Engineer / PWD, Health Department, Property Tax Division, Computer Department, Electricity & Street Lights, Disaster Management)
- **Verified Usable Emails**: 29
- **Helplines / Unlisted**: 20 (Preserved with contact numbers like 1912, 101, 1800-123-2267)
- **Baseline Count**: Kolhapur initialized strictly at **0 unresolved complaints**.
