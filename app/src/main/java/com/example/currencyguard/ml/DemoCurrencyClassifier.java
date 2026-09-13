package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Deterministic Mock/Demo Currency Classifier.
 * Clearly identified as DEMO/MOCK inference. Never claims genuine forensic AI accuracy.
 * Serves as default out-of-the-box engine so the app runs smoothly without
 * requiring a pre-trained model file.
 */
public class DemoCurrencyClassifier implements CurrencyClassifier {

    public static final String DEMO_TAG = "[DEMO/MOCK CLASSIFIER]";

    @Override
    public ClassificationResult classify(Bitmap bitmap) {
        if (bitmap == null) {
            return new ClassificationResult("Unable to Verify", 0.0, new float[]{0f, 0f, 0f, 1f}, true);
        }

        // Deterministic heuristic sampling across the 224x224 bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long totalLum = 0;
        long totalGreen = 0;
        long totalRed = 0;
        int sampleStep = Math.max(1, width / 20);
        int sampledPixels = 0;

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                totalLum += (r * 299L + g * 587L + b * 114L) / 1000L;
                totalGreen += g;
                totalRed += r;
                sampledPixels++;
            }
        }

        double avgLum = (double) totalLum / sampledPixels;
        double greenRatio = (double) totalGreen / (totalRed + 1);

        // Derive deterministic probabilities based on realistic visual attributes
        float genuineProb = 0.82f;
        float suspiciousProb = 0.12f;
        float fakeProb = 0.04f;
        float unableProb = 0.02f;

        if (avgLum < 45 || avgLum > 215) {
            // Extreme dark or overexposed
            genuineProb = 0.30f;
            suspiciousProb = 0.35f;
            fakeProb = 0.10f;
            unableProb = 0.25f;
        } else if (greenRatio < 0.65 || greenRatio > 1.45) {
            // Uncharacteristic color balance
            genuineProb = 0.45f;
            suspiciousProb = 0.40f;
            fakeProb = 0.12f;
            unableProb = 0.03f;
        }

        String predictedClass = "Likely Genuine";
        double confidence = genuineProb * 100.0;

        if (suspiciousProb > genuineProb && suspiciousProb > fakeProb) {
            predictedClass = "Suspicious";
            confidence = suspiciousProb * 100.0;
        } else if (fakeProb > genuineProb && fakeProb > suspiciousProb) {
            predictedClass = "Likely Fake";
            confidence = fakeProb * 100.0;
        } else if (unableProb > 0.20f) {
            predictedClass = "Unable to Verify";
            confidence = unableProb * 100.0;
        }

        return new ClassificationResult(
                predictedClass,
                Math.round(confidence * 10.0) / 10.0,
                new float[]{genuineProb, suspiciousProb, fakeProb, unableProb},
                true // Clearly marked as DEMO
        );
    }

    @Override
    public void close() {
        // No native resources to release
    }
}
