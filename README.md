# CurrencyGuard AI 🛡️💵

### AI-Assisted Fake Currency Detection & Authenticity Screening

CurrencyGuard AI is an Android application designed to assist users in screening currency notes for potential counterfeit anomalies using computer vision, machine learning, image analysis, and explainable AI techniques.

The application combines multiple analysis stages instead of relying on a single prediction. It evaluates image quality, denomination information, security-feature regions, geometry, texture, and visual anomalies to generate an overall screening assessment.

> **Important:** CurrencyGuard AI is a prototype/educational screening system. It does not replace official verification by banks, the Reserve Bank of India (RBI), or law-enforcement authorities.

---

## 🌟 Key Features

### 1. 🎯 AI Doubt Meter

CurrencyGuard AI includes an uncertainty-analysis layer that explains **why the system may have lower confidence** instead of showing only a single percentage.

Possible reasons include:

* `LOW_IMAGE_QUALITY` — Blur, glare, poor illumination, or unclear image
* `CONFLICTING_MODELS` — Different analysis stages produce inconsistent results
* `UNUSUAL_GEOMETRY` — Note dimensions or aspect ratio appear unusual
* `MISSING_SECURITY_FEATURES` — Important regions are unclear or difficult to analyze
* `OCR_MISMATCH` — Detected denomination information does not align with the screening result
* `HIGH_ANOMALY_SCORE` — Unusual color, texture, or visual patterns

This provides the user with an understandable **AI Doubt Meter** rather than hiding uncertainty.

---

### 2. 📈 Confidence Timeline

The application can track confidence scores across multiple captures of the same note.

The system calculates confidence variation using standard deviation:

```text
σ = √(1/N × Σ(xᵢ - μ)²)
```

The result can be presented as an **AI Consistency Score**.

Example:

```text
92% Consistency
Stable & Reliable
```

or:

```text
High Volatility: ±9.4%
Please retake under better lighting
```

This helps identify whether repeated scans are producing stable results.

---

### 3. 🧩 Multi-Stage Analysis Pipeline

CurrencyGuard AI combines multiple specialized analysis stages.

#### Analysis stages

1. **Visual Analysis**

   * TensorFlow Lite model
   * Visual feature analysis

2. **Security Feature Analysis**

   * Region-based analysis
   * Watermark/security-thread related regions

3. **OCR Consistency**

   * ML Kit Text Recognition
   * Denomination and visible text analysis

4. **Geometry Analysis**

   * Aspect ratio
   * Orientation
   * Rectangular proportion checks

5. **Image Quality Gate**

   * Blur detection
   * Lighting/quality checks
   * Prevents unreliable images from proceeding

6. **Texture & Visual Anomaly Analysis**

   * Edge-density analysis
   * Color distribution
   * Texture variation

The results are combined by the confidence engine to produce an overall screening assessment.

---

## 4. 🎨 AI Attention Heatmap

CurrencyGuard AI can visualize regions that influenced the screening analysis.

The heatmap uses an intuitive interpretation:

| Indicator | Meaning                            |
| --------- | ---------------------------------- |
| 🟢 Green  | Strong consistency                 |
| 🟡 Yellow | Moderate confidence / ambiguity    |
| 🔴 Red    | Potential anomaly / low confidence |

The purpose is to make the AI result more understandable to the user.

---

## 5. 🤖 AI Explanation Layer

CurrencyGuard AI supports an AI explanation layer that converts structured analysis results into a short, human-readable explanation.

### Optional Gemini Layer

When configured, Gemini can generate a concise explanation based on the structured findings.

### Local Fallback

If the application is offline or an AI API key is not configured, the application can use a local rule-based explanation system.

This allows the core screening workflow to remain usable without depending completely on a cloud AI service.

---

## 6. 📄 Currency Passport & PDF Report

CurrencyGuard AI can generate a digital screening report containing information such as:

* Unique Passport ID
* Date and time
* Front/back note thumbnails
* Overall screening score
* Individual analysis results
* Confidence information
* AI explanation
* Disclaimer

Example Passport ID format:

```text
CG-YYYY-XXXXXX
```

Reports are generated using Android's native `PdfDocument` API.

---

# 🏗️ System Architecture

```text
                 ┌─────────────────────────┐
                 │ CameraX / Gallery Input │
                 └────────────┬────────────┘
                              │
                              ▼
                 ┌─────────────────────────┐
                 │   Image Quality Gate    │
                 └────────────┬────────────┘
                              │
                    Quality < Threshold
                              │
                              ▼
                    ┌─────────────────┐
                    │  Retake Image   │
                    └─────────────────┘

                              │
                              ▼
             ┌────────────────────────────────┐
             │ Currency & Denomination Check  │
             └───────────────┬────────────────┘
                             │
             ┌───────────────┴────────────────┐
             ▼                                ▼
   ┌──────────────────┐             ┌──────────────────┐
   │ TensorFlow Lite  │             │ ML Kit OCR       │
   │ Visual Analysis  │             │ Text Analysis    │
   └────────┬─────────┘             └────────┬─────────┘
            │                                │
            ▼                                ▼
   ┌──────────────────┐             ┌──────────────────┐
   │ Security Feature │             │ Geometry &       │
   │ Analysis         │             │ Anomaly Analysis │
   └────────┬─────────┘             └────────┬─────────┘
            │                                │
            └──────────────┬─────────────────┘
                           ▼
              ┌────────────────────────┐
              │   AI Doubt Meter       │
              │ Uncertainty Analyzer   │
              └────────────┬───────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │   Confidence Engine    │
              │   & Calibration        │
              └────────────┬───────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
     ┌──────────────────┐      ┌────────────────────┐
     │ Heatmap / Visual │      │ AI Explanation     │
     │ Analysis         │      │ Layer              │
     └────────┬─────────┘      └─────────┬──────────┘
              │                          │
              └────────────┬─────────────┘
                           ▼
              ┌────────────────────────┐
              │ Screening Result       │
              │ + Confidence Timeline  │
              └────────────┬───────────┘
                           │
                           ▼
                 ┌──────────────────┐
                 │ PDF Report /     │
                 │ Currency Passport│
                 └──────────────────┘
```

---

# 📱 Technology Stack

## Android

* **Java**
* **XML**
* Android Studio
* Material Design 3
* CameraX

## Machine Learning & AI

* TensorFlow Lite
* Google ML Kit
* Computer Vision
* Image Quality Analysis
* Visual Anomaly Detection
* Optional Gemini AI explanation layer

## Local Storage

* Room Database

## Visualization

* MPAndroidChart
* AI confidence timeline
* Heatmap visualization

## Report Generation

* Android `PdfDocument`

---

# 📂 Project Architecture

The project follows a modular Android application structure.

```text
CurrencyGuardAI/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── ...
│   │       │
│   │       ├── res/
│   │       │   ├── drawable/
│   │       │   ├── layout/
│   │       │   ├── mipmap/
│   │       │   ├── values/
│   │       │   └── xml/
│   │       │
│   │       └── AndroidManifest.xml
│   │
│   └── build.gradle
│
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

Important analysis components include classes such as:

```text
UncertaintyAnalyzer.java
ConfidenceEngine.java
ConfidenceTimelineChart.java
LocalExplanationGenerator.java
```

---

# 🚀 Getting Started

## Prerequisites

Install the following:

* Android Studio Iguana 2023.2.1 or newer
* JDK 17
* Android SDK
* Android device or emulator
* Minimum Android API: 24
* Target Android API: 36

A physical Android device is recommended when testing camera-based functionality.

---

# 🔧 Installation

### 1. Clone the repository

```bash
git clone https://github.com/krupasawarkar630/CurrencyGuardAI.git
```

### 2. Open the project

Open the cloned project in Android Studio.

### 3. Allow Gradle Sync

Wait for Android Studio to download and synchronize the required dependencies.

### 4. Connect an Android device

Enable:

```text
Developer Options
        ↓
USB Debugging
```

Alternatively, use an Android emulator.

### 5. Build the application

In Android Studio:

```text
Build → Make Project
```

### 6. Run the application

Click:

```text
Run ▶
```

and select your connected device/emulator.

---

# 🧪 Demo Mode

CurrencyGuard AI supports a demonstration workflow so that the application can be tested without requiring a custom trained neural network.

Demo mode can be used to demonstrate:

* Note scanning
* Image-quality analysis
* Confidence scoring
* AI Doubt Meter
* Security-feature analysis
* OCR workflow
* Anomaly analysis
* Confidence timeline
* Heatmap visualization
* AI explanation
* PDF report generation

> Demo results should not be interpreted as real-world counterfeit certification.

---

# 🔐 Privacy & Security

CurrencyGuard AI is designed with a privacy-first approach.

The application aims to minimize unnecessary transmission of currency images and analysis data.

Where possible, analysis can be performed locally on the device using:

* TensorFlow Lite
* ML Kit
* Local image-processing algorithms
* Local rule-based explanations
* Room Database

The optional Gemini explanation layer may require sending structured analysis information to an external AI service depending on the configured implementation.

Users should review the configured privacy and API policies before deploying the application for real-world use.

---

# 🧠 Explainable AI Approach

Instead of presenting only:

```text
Authentic: 87%
```

CurrencyGuard AI aims to provide additional context:

```text
Screening Confidence: 87%

Why confidence is reduced:
• Image quality is moderate
• OCR result is partially unclear
• Security-feature region requires another capture

Recommendation:
Retake the note under even lighting.
```

This makes the result easier to understand and encourages human verification when uncertainty is high.

---

# ⚠️ Responsible AI

CurrencyGuard AI is intended as an **assistance and screening tool**, not an official currency authentication system.

The application:

* Does not replace bank verification
* Does not replace RBI verification
* Does not replace law-enforcement verification
* Does not guarantee that a note is genuine or counterfeit
* Should not be used as the sole basis for financial or legal decisions

A screening result should always be treated as an indication that may require further verification.

---

# 📊 Example Screening Flow

```text
Capture Note
     ↓
Check Image Quality
     ↓
Detect Denomination
     ↓
Analyze Visual Features
     ↓
Analyze Security Regions
     ↓
Run OCR Consistency Check
     ↓
Check Geometry
     ↓
Analyze Texture & Anomalies
     ↓
Calculate Confidence
     ↓
Generate Doubt Meter
     ↓
Generate Explanation
     ↓
Display Result
     ↓
Export PDF Report
```

---

# 🎯 Project Goals

CurrencyGuard AI aims to demonstrate how AI-assisted computer vision can be combined with explainability and uncertainty communication for currency screening.

The major goals are:

1. Reduce dependence on a single AI prediction.
2. Communicate uncertainty clearly.
3. Combine multiple visual and analytical signals.
4. Provide explainable screening results.
5. Encourage users to retake poor-quality images.
6. Maintain a privacy-conscious architecture.
7. Provide a digital screening report.
8. Demonstrate responsible AI usage.

---

# 🔮 Future Improvements

Possible future improvements include:

* Larger real-world currency datasets
* More denominations and currencies
* Improved counterfeit-specific datasets
* Advanced object detection
* Improved security-feature localization
* More robust OCR preprocessing
* Federated learning
* Model calibration using real validation data
* Better offline AI models
* Government/banking verification integration
* Advanced document/report analytics
* Improved accessibility for visually impaired users

---

# 📌 Limitations

The effectiveness of counterfeit screening depends on:

* Image quality
* Camera quality
* Lighting conditions
* Note orientation
* Training/validation data
* Model accuracy
* Availability and quality of visible security features

A prototype model or demo dataset cannot establish real-world counterfeit-detection accuracy without proper field validation.

Therefore, the application should not make unsupported claims of guaranteed detection accuracy.

---

# 👩‍💻 Development

CurrencyGuard AI is developed as an Android application using Java and XML.

The project focuses on:

```text
Android Development
        +
Computer Vision
        +
Machine Learning
        +
Explainable AI
        +
Responsible AI
```

---

# 📜 Disclaimer

**IMPORTANT DISCLAIMER**

CurrencyGuard AI provides an automated visual screening assessment for educational, research, and prototype demonstration purposes.

It is **not an official currency authentication system** and is not a substitute for verification by banks, the Reserve Bank of India (RBI), or law-enforcement authorities.

The application's output should not be considered a definitive declaration that a currency note is genuine or counterfeit.

---

# ⭐ Project

**Project Name:** CurrencyGuard AI

**Category:** AI-Assisted Currency Screening

**Platform:** Android

**Language:** Java

**UI:** XML + Material Design 3

**AI/ML:** TensorFlow Lite + ML Kit + Optional Gemini

---

## 📄 License

This project is intended for educational, research, and hackathon/prototype purposes.

Add an appropriate open-source license before distributing the project publicly.
