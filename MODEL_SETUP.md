# Machine Learning Model Setup Guide — CurrencyGuard AI

This document details the specifications, input/output tensors, dataset requirements, and procedure for integrating a custom-trained TensorFlow Lite currency classification model into **CurrencyGuard AI**.

---

## 1. Overview & Architecture

CurrencyGuard AI uses a modular architecture for AI-driven currency screening:
- **`CurrencyClassifier` (Java Interface)**: Abstract contract for image classification.
- **`DemoCurrencyClassifier` (Default / Fallback)**: Built-in deterministic mock inference engine clearly labeled for testing and demonstration when no `.tflite` model is present.
- **`TensorFlowCurrencyClassifier` (Production Implementation)**: On-device TensorFlow Lite interpreter engine.

---

## 2. Model Specifications

If training your own Convolutional Neural Network (e.g., MobileNetV3, EfficientNet-Lite, or custom ResNet):

| Property | Requirement |
| :--- | :--- |
| **Model Format** | TensorFlow Lite (`.tflite`) |
| **Input Shape** | `[1, 224, 224, 3]` (Batch Size 1, 224x224 Height x Width, 3 Color Channels RGB) |
| **Input Type** | `FLOAT32` (normalized between `0.0` and `1.0` or `UINT8` `0` to `255`) |
| **Output Shape** | `[1, 4]` (Softmax probability distribution over 4 classes) |
| **Supported Classes** | `Likely Genuine`, `Suspicious`, `Likely Fake`, `Unable to Verify` |
| **Max Model Size** | Recommended `< 25 MB` for fast mobile inference |

---

## 3. Label Mapping (`labels.txt`)

The labels file is located at `app/src/main/assets/labels.txt`:
```text
Likely Genuine
Suspicious
Likely Fake
Unable to Verify
```
Ensure the order of classes in your training pipeline matches the index ordering of `labels.txt`.

---

## 4. How to Install Your Trained Model

1. **Export `.tflite`**: Train your model and export as `currency_model.tflite`.
2. **Place in Assets**: Copy `currency_model.tflite` into:
   ```
   app/src/main/assets/currency_model.tflite
   ```
3. **Verify Configuration**:
   In `com.example.currencyguard.model.AppConfig.java`, verify that:
   ```java
   public static final String MODEL_FILE = "currency_model.tflite";
   public static final boolean USE_DEMO_CLASSIFIER_IF_MISSING = true;
   ```
4. **Build & Run**:
   `TensorFlowCurrencyClassifier.java` will automatically detect the file in assets, initialize the TFLite native interpreter, and perform real-time on-device inference.

---

## 5. Evaluation Metrics Reporting Standard

When publishing or submitting academic or hackathon results:
- **Never claim 100% detection accuracy.**
- Report standard metrics across balanced test sets:
  - **Accuracy**: $\frac{TP + TN}{TP + TN + FP + FN}$
  - **Precision**: $\frac{TP}{TP + FP}$
  - **Recall**: $\frac{TP}{TP + FN}$
  - **F1-Score**: $2 \times \frac{\text{Precision} \times \text{Recall}}{\text{Precision} + \text{Recall}}$
- Clearly state lighting, camera angle, and wear-and-tear conditions under which metrics were gathered.

---

## 6. Disclaimer

*CurrencyGuard AI provides screening assistance for educational and experimental purposes. It does not provide legal or forensic proof of currency genuineness.*
