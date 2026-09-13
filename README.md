# 💵 CurrencyGuard AI — Real-Time Banknote Counterfeit Detection & AI Forensics

[![Android Build](https://img.shields.io/badge/Android-SDK%2036-brightgreen?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Java 21 / 17](https://img.shields.io/badge/Language-Java%2017%2F21-orange?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Gradle 8.6](https://img.shields.io/badge/Gradle-8.6-blue?style=for-the-badge&logo=gradle)](https://gradle.org/)
[![TensorFlow Lite](https://img.shields.io/badge/ML-TensorFlow%20Lite%202.14-ff69b4?style=for-the-badge&logo=tensorflow)](https://www.tensorflow.org/lite)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Auth%20%26%20Firestore-yellow?style=for-the-badge&logo=firebase)](https://firebase.google.com/)

**CurrencyGuard AI** is an advanced, production-grade Android application engineered for real-time banknote authenticity verification, physical security feature inspection, and explainable AI currency analytics. Powered by on-device **TensorFlow Lite**, **ML Kit OCR**, and cloud-based **Gemini 2.0 Flash / OpenRouter Vision** multimodal AI models, CurrencyGuard AI delivers instant, transparent, and actionable currency intelligence.

---

## 🌟 Key Features

### 1. 📸 Smart Live CameraX Scanner
- **Auto-Guidance Reticle**: Real-time bounding guide (`SmartScanGuide`) ensuring optimal banknote distance, orientation, and framing.
- **Real-Time Image Quality Analysis**: Detects motion blur, low lighting, glare, and cropping before sending frames to the AI pipeline (`ImageQualityAnalyzer`).
- **Interactive Flash & Zoom Control**: Seamless touch-to-focus and toggle torch controls for dark environments.

### 2. 🧠 Multi-Stage AI & ML Engine
- **On-Device TensorFlow Lite Classifier**: Rapid initial classification (`Likely Genuine`, `Suspicious`, `Likely Fake`, `Unable to Verify`).
- **ML Kit Text Recognition (v2 OCR)**: Extracts and verifies banknote serial numbers, central bank signatures, and denomination numbers (`OCRAnalyzer`).
- **Security Feature & Anomaly Detection**: Analyzes watermarks, security threads, color-shift inks, and microlettering alignment (`SecurityFeatureAnalyzer`, `AnomalyDetector`).
- **Uncertainty Engine**: Calculates confidence scores and breaks down risk factors when physical features are ambiguous (`UncertaintyAnalyzer`).

### 3. 🔬 Cloud Multimodal Vision Forensics (OpenRouter & Gemini)
- **Multimodal AI Deep Inspection**: Sends high-resolution banknote captures to vision models like **Gemini 2.0 Flash** and **Llama 3.2 Vision** via **OpenRouter API** (`OpenRouterVisionService`, `GeminiVisionService`).
- **Local Fallback Engine**: Provides full offline analytical reports (`LocalExplanationGenerator`) if no internet or API key is available.

### 4. 📊 Explainable AI (XAI) & Dynamic Visualizations
- **Heatmap Overlay (`HeatmapOverlayView`)**: Visualizes exact image regions contributing to authenticity or anomaly flags using Class Activation Mapping (CAM) concepts.
- **Confidence Timeline Chart (`ConfidenceTimelineChart`)**: Interactive breakdown of security feature confidence metrics using MPAndroidChart.
- **PDF Forensic Reports**: Generates formal PDF authenticity certificates (`PdfReportGenerator`) with complete breakdown, timestamps, and image snapshots.

### 5. 💬 Ask CurrencyGuard AI Chatbot & Voice Assistant
- **Context-Aware Educational Chat**: Instant answers on security features, counterfeit identification tips, and currency policies (`ChatAssistantActivity`, `ChatAssistantService`).
- **Text-to-Speech & Voice Input**: Hands-free voice assistant functionality powered by `Android TextToSpeech` (`AiVoiceManager`).

### 6. 💱 Multi-Country Currency Converter
- **Live Currency Exchange**: Converts rates across major world currencies with an interactive interface (`CurrencyConverterActivity`).

### 7. 🔐 User Gate & Data Management
- **Firebase Authentication**: Secure user login, registration, and session gate (`AuthActivity`, `FirebaseAuthManager`).
- **Room Database & Cloud Sync**: Persistent local storage of scan history (`AppDatabase`, `ScanResultDao`) synced seamlessly with Firebase Firestore (`FirebaseSyncManager`).
- **Secure File Sharing**: Android `FileProvider` integration for secure PDF and image sharing (`ShareUtils`).

---

## 🏗️ Project Architecture & Tech Stack

```text
CurrencyGuardAI
├── app
│   ├── src/main/java/com/example/currencyguard/
│   │   ├── activity/        # App Screen Host Activities (Main, Scan, Analysis, Result, Chat, Auth, Converter)
│   │   ├── adapter/         # RecyclerView Adapters (Scan History, Chat Messages, Learn Cards)
│   │   ├── ai/              # AI Services (OpenRouter, Gemini, Voice Manager, Confidence Engine)
│   │   ├── camera/          # CameraX Helpers & Real-Time Alignment Guide
│   │   ├── database/        # Room Database (AppDatabase, ScanResultDao)
│   │   ├── firebase/        # Firebase Authentication & Cloud Firestore Sync Manager
│   │   ├── fragment/        # Dashboard Views (Home, History, Learn, Profile)
│   │   ├── ml/              # Machine Learning Pipeline (TFLite, OCR, Anomaly, Heatmaps)
│   │   ├── model/           # Data Models (ScanResult, AnalysisResult, ChatMessage)
│   │   ├── repository/      # Repository Pattern (ScanRepository)
│   │   ├── ui/              # Custom Dynamic Views (Heatmap Overlay, Confidence Chart)
│   │   └── utils/           # Helper Utilities (PDF Generator, Settings, Image Processing)
│   └── src/main/res/        # Material 3 Layouts, Drawables, Styles, and Localization (AR, DE, ES, FR, HI)
```

### Technical Specifications

| Layer | Technology |
|---|---|
| **Language** | Java 17 / Java 21 (Android Studio JBR) |
| **Minimum SDK** | Android 7.0 (API Level 24) |
| **Target / Compile SDK** | Android 14 / 15 (API Level 36) |
| **Camera Framework** | AndroidX CameraX `1.3.4` |
| **Machine Learning** | TensorFlow Lite `2.14.0`, TFLite Support `0.4.4`, ML Kit Text Recognition `19.0.1` |
| **UI Components** | Material Design 3, ConstraintLayout, MPAndroidChart `v3.1.0` |
| **Networking** | OkHttp `4.12.0` |
| **Persistence** | Room Database `2.6.1` & Firebase Cloud Firestore |
| **Authentication** | Firebase Auth BoM `33.7.0` |

---

## 🛠️ Prerequisites & Setup Guide

### 1. Requirements
- **Android Studio**: Jellyfish (2024.1+) or Ladybug (2024.2+) recommended.
- **JDK**: JDK 17 or JDK 21 (bundled Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr`).
- **Android SDK**: API Level 36 installed.

### 2. Environment Configuration

#### `local.properties`
Create or update `local.properties` in the project root to specify your local Android SDK location:
```properties
sdk.dir=D:\\Movies
# OR default location:
# sdk.dir=C:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

#### `gradle.properties`
Ensure `gradle.properties` includes the proper JDK 21 path and network flags:
```properties
# Project-wide Gradle settings.
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djava.net.preferIPv4Stack=true -Dhttp.keepAlive=false
android.useAndroidX=true
android.enableJetifier=true
android.nonTransitiveRClass=true
android.suppressUnsupportedCompileSdk=36
```

---

## 🚀 Building & Running

### Command Line (Gradle Wrapper)
To assemble the debug APK directly from terminal:
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Android Studio
1. Open Android Studio and select **Open Project** -> Choose `CurrencyGuardAI`.
2. Allow Gradle Sync to finish.
3. Connect an Android device (or launch an Emulator running API 24+).
4. Click **Run 'app'** (`Shift + F10`).

---

## 🔑 Firebase & API Key Setup (Optional)

1. **Firebase Integration**:
   - Place your `google-services.json` file inside the `app/` directory (`CurrencyGuardAI/app/google-services.json`).
   - Enable **Email/Password** or **Google Sign-In** under your Firebase Console -> Authentication.

2. **OpenRouter / Gemini Vision Key**:
   - Launch the app -> Navigate to **Profile / Settings**.
   - Enter your OpenRouter API key (`sk-or-v1-...`) to unlock cloud multimodal AI vision inspection.
   - *Note: If no API key is provided, CurrencyGuard AI automatically operates in 100% offline local AI fallback mode.*

---

## 🔒 Security & Privacy

- **Zero Secret Exposure**: API keys are saved locally in private `SharedPreferences` and are never hardcoded into source control.
- **Privacy First**: Banknote scans are processed on-device by default. Cloud vision requests are only dispatched when explicitly authorized by the user.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/krupasawarkar630/CurrencyGuardAI/issues).

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AwesomeFeature`)
3. Commit your Changes (`git commit -m 'Add some AwesomeFeature'`)
4. Push to the Branch (`git push origin feature/AwesomeFeature`)
5. Open a Pull Request