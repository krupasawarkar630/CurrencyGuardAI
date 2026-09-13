# Firebase Setup & Manual Connection Guide

**CurrencyGuard AI** is preconfigured with Firebase Authentication and Cloud Firestore support. All the code and Gradle integrations are built-in. Follow these simple steps to connect your Firebase project:

---

## Step 1: Create or Open a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select your existing project) and name it `CurrencyGuardAI`.
3. Disable or enable Google Analytics according to your preference and click **Create Project**.

---

## Step 2: Register Android App
1. In Project Overview, click the **Android** icon (🤖) to add an Android app.
2. Enter the **Android package name**:
   ```
   com.example.currencyguard
   ```
3. Enter App nickname: `CurrencyGuard AI`.
4. (Optional) Debug signing certificate SHA-1 can be added later.
5. Click **Register app**.

---

## Step 3: Download `google-services.json`
1. Download the generated `google-services.json` file.
2. Place this file into the `app/` folder of the project:
   ```
   CurrencyGuardAI/
   └── app/
       ├── google-services.json  <-- Place file here!
       ├── build.gradle.kts
       └── src/
   ```
3. Once placed, Gradle automatically applies the `com.google.gms.google-services` plugin on your next build!

---

## Step 4: Enable Authentication & Firestore
1. In the Firebase Console left menu, navigate to **Build > Authentication**:
   - Click **Get Started**.
   - Under the **Sign-in method** tab, select **Email/Password**.
   - Toggle **Enable** and click **Save**.
2. In the Firebase Console left menu, navigate to **Build > Firestore Database**:
   - Click **Create database**.
   - Choose a location (e.g. `nam5 (us-central)` or `asia-south1`).
   - Start in **Test mode** (or Production mode with user authentication rules).
   - Click **Create**.

---

## Step 5: How It Works Out of the Box
- If `google-services.json` has **not** been added yet, CurrencyGuard AI uses a **secure offline/local authentication fallback** so you can develop, test, and run the app immediately without crashes!
- The moment you place `google-services.json` into `app/`, CurrencyGuard AI automatically detects Firebase and begins authenticating real users and syncing scan records to Cloud Firestore under `users/{uid}/scans/{scanId}`!
