# 🛠️ NagrikVoice Local Setup & Execution Guide

This guide explains how to run the **NagrikVoice** backend service, execute test suites, and launch the native Android application.

---

## 1. Prerequisites

- **Node.js**: v20 or newer (uses native SQLite and TypeScript stripping).
- **Android Studio**: Ladybug / Jellyfish or newer with Android SDK 34.
- **Java**: JDK 17 or JDK 21.

---

## 2. Running the Backend & Web Dashboard

### Step 1: Start the Backend Service
From the repository root:
```bash
cd backend
npm start
```

The server initializes the database, parses `KMC_All_Departments_Complete_Directory.csv`, seeds the Kolhapur directory starting at zero unresolved count, and listens on port `8080`:
```
✅ KMC Authority and 49 contacts initialized.
🚀 NagrikVoice Civic Platform Backend running at http://localhost:8080
📊 Admin & Civic Dashboard accessible at http://localhost:8080/admin
```

### Step 2: Open the Civic Dashboard
Open your browser and navigate to:
```
http://localhost:8080/admin
```
Here you can:
- View live real-time KMC accountability metrics.
- Inspect complaints and their Option B email delivery statuses.
- Use 1-click test simulation buttons to trigger delivered vs failed email callbacks.
- Review resolution claims and approve/reject fixes.
- Inspect immutable audit logs.

### Step 3: Run the Backend Acceptance Test Suite
Execute the 10 acceptance tests verifying all 9 Core Rules:
```bash
cd backend
npm test
```

---

## 3. Running the Native Android Application

### Step 1: Open in Android Studio
1. Launch Android Studio.
2. Select **Open** and select the directory:
   `/Users/Apple/.gemini/antigravity/scratch/civic-accountability-platform/android`
3. Allow Gradle to sync dependencies.

### Step 2: Configure Backend Connection
The Android app is preconfigured to communicate with the host machine via:
```
http://10.0.2.2:8080/api/v1/
```
*(10.0.2.2 is the standard loopback alias in Android Emulator for the host Mac's localhost)*

If running on a physical Android device connected via Wi-Fi:
Change `ApiClient.baseUrl` in `org/nagrikvoice/app/data/api/ApiClient.kt` to your Mac's local network IP (e.g. `http://192.168.1.5:8080/api/v1/`).

### Step 3: Run on Device or Emulator
1. Select an Android Virtual Device (API 30 - 34) in Android Studio.
2. Click **Run** (`Shift + F10`).
3. The app will launch with:
   - Onboarding carousel and location permissions.
   - Instagram-like civic feed.
   - Report screen with in-app camera capture and duplicate detector.
   - Municipal rankings screen.
   - Detailed complaint timeline.

---

## 4. Configuration & Environment Variables

| Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8080` | Port for backend REST API and Admin UI |
| `TEST_EMAIL_RECIPIENT` | `test-civic-officer@kolhapur.gov.local` | Configurable test Gmail address for testing complaint delivery |
| `NODE_ENV` | `development` | Set to `production` to route to verified official municipal emails |
| `DATABASE_PATH` | `./data/civic_platform.db` | Location of SQLite database file |
| `JWT_SECRET` | Secure key | Secret used for citizen session tokens |
