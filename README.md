# 🇮🇳 NagrikVoice — Citizen-Powered Civic Accountability Platform for India

NagrikVoice is a complete, production-oriented, native Android application built using Kotlin and Jetpack Compose paired with a secure backend service. It combines the engagement of an Instagram-style feed with the functionality of a civic complaint management and municipal accountability platform.

Initially focused on **Kolhapur, Maharashtra** using the verified Municipal Corporation contact dataset, its architecture supports addition of municipal corporations and councils throughout India.

---

## 🌟 Key Features

1. **One Problem = One Complaint Record (Rule 1)**:
   - Proactive duplicate detection uses GPS proximity (within 100m) and category matching.
   - When a citizen reports an already documented issue, they are guided to support the existing complaint instead of creating artificial duplicates.

2. **Option B Delivery Rule (Rule 2)**:
   - A municipal corporation's unresolved complaint count increases **ONLY** when a complaint email is confirmed as delivered by the mail service.
   - Merely queuing or sending an email does not alter the count.

3. **Failed Delivery & Idempotent Retry (Rule 3)**:
   - Failed email attempts are retained for retry and reuse the existing complaint ID without inflating counts.

4. **Community Support & Voting (Rule 4)**:
   - Citizens can support issues with a single vote to elevate community priority.
   - Votes never artificially multiply unresolved problem counts.

5. **Verified Resolutions Only (Rule 5)**:
   - Issues leave the unresolved count only after resolution evidence is independently approved.

6. **All Authorities Start at Zero (Rule 6)**:
   - Initial rankings start with zero unresolved complaints without fabricated historical numbers.

7. **In-App Camera & GPS Metadata**:
   - Camera interface directly captures authentic photographs with timestamp and GPS coordinate overlays.

8. **KMC Directory Integration**:
   - 49 contact records imported across 7 departments from `KMC_All_Departments_Complete_Directory.csv` with automatic routing rules.

---

## 📂 Repository Structure

```
civic-accountability-platform/
├── android/                             # Native Android Kotlin Project
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml          # Permissions & activity declarations
│   │   ├── java/org/nagrikvoice/app/
│   │   │   ├── MainActivity.kt          # Host Activity with Compose NavHost
│   │   │   ├── NagrikVoiceApp.kt        # Application class
│   │   │   ├── data/                    # Models, ApiClient, Repositories
│   │   │   ├── ui/
│   │   │   │   ├── components/          # ComplaintCard, DuplicateDialog, StatusBadge
│   │   │   │   ├── navigation/          # BottomNavBar, Screen routes
│   │   │   │   ├── screens/             # Onboarding, Feed, Report, Rankings, Detail, Profile
│   │   │   │   └── theme/               # Material 3 Civic Palette & Typography
│   │   └── res/                         # Strings, colors, themes, launcher icons
│   └── app/src/test/                    # CivicCoreRulesTest unit test suite
├── backend/                             # Secure Backend Engine
│   ├── data/
│   │   └── KMC_All_Departments_Complete_Directory.csv # Kolhapur directory dataset
│   ├── src/
│   │   ├── db/                          # SQLite schema with foreign keys & indexes
│   │   ├── services/                    # CSV Importer, Duplicate Detector, Email, Ranking
│   │   └── server.ts                    # REST API & Interactive Civic Dashboard
│   └── tests/
│       └── acceptance.test.ts           # 10 automated acceptance tests
├── docs/
│   ├── ARCHITECTURE.md                  # Comprehensive system architecture
│   ├── RULES_SPECIFICATION.md           # The 9 Core Rules specification
│   └── SETUP_GUIDE.md                   # Step-by-step local execution instructions
└── README.md
```

---

## 🚀 Quick Start

### 1. Start the Backend
```bash
cd backend
npm start
```
- API runs at `http://localhost:8080/api/v1/`
- Web Dashboard accessible at `http://localhost:8080/admin`

### 2. Run the Acceptance Tests
```bash
cd backend
npm test
```

### 3. Open Android App in Android Studio
- Open `android/` directory in Android Studio.
- Sync Gradle and click **Run** on an Android Emulator or device.
