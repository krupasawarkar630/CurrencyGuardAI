package com.example.currencyguard.ml;

import android.graphics.Bitmap;

/**
 * Interface defining the modular contract for Currency Classifiers.
 * Enables drop-in replacement between Demo and trained TensorFlow Lite models
 * without requiring changes to the UI layer.
 */
public interface CurrencyClassifier {

    class ClassificationResult {
        private final String predictedClass;
        private final double confidence; // 0.0 to 100.0
        private final float[] probabilities;
        private final boolean isDemo;

        public ClassificationResult(String predictedClass, double confidence, float[] probabilities, boolean isDemo) {
            this.predictedClass = predictedClass;
            this.confidence = confidence;
            this.probabilities = probabilities;
            this.isDemo = isDemo;
        }

        public String getPredictedClass() { return predictedClass; }
        public double getConfidence() { return confidence; }
        public float[] getProbabilities() { return probabilities; }
        public boolean isDemo() { return isDemo; }
    }

    ClassificationResult classify(Bitmap bitmap);
    void close();
}
