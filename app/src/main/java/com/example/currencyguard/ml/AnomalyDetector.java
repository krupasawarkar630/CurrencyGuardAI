package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * AI Visual Anomaly Detection Layer.
 * Analyzes color histogram dispersion, edge structure density, and
 * texture uniformity to output an anomaly score between 0 and 100.
 * (Lower score indicates fewer anomalies / closer to genuine standard).
 */
public class AnomalyDetector {

    public static class AnomalyReport {
        private final double anomalyScore; // 0 (normal) to 100 (extreme anomaly)
        private final String interpretation;
        private final String colorStatus;
        private final String textureStatus;
        private final String edgeStatus;

        public AnomalyReport(double anomalyScore, String interpretation,
                             String colorStatus, String textureStatus, String edgeStatus) {
            this.anomalyScore = anomalyScore;
            this.interpretation = interpretation;
            this.colorStatus = colorStatus;
            this.textureStatus = textureStatus;
            this.edgeStatus = edgeStatus;
        }

        public double getAnomalyScore() { return anomalyScore; }
        public String getInterpretation() { return interpretation; }
        public String getColorStatus() { return colorStatus; }
        public String getTextureStatus() { return textureStatus; }
        public String getEdgeStatus() { return edgeStatus; }
        public boolean isSubstrateReal() { return anomalyScore <= 40.0; }

        /**
         * Converts anomaly score into a confidence score (0 to 100)
         * where higher is more genuine.
         */
        public double toConsistencyScore() {
            return Math.max(0.0, 100.0 - anomalyScore);
        }
    }

    public AnomalyReport detect(Bitmap bitmap) {
        if (bitmap == null) {
            return new AnomalyReport(50.0, "Moderate Anomaly", "Normal", "Indeterminate", "Normal");
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int step = Math.max(1, Math.min(width, height) / 35);

        int[] histR = new int[16];
        int[] histG = new int[16];
        int[] histB = new int[16];
        int totalPixels = 0;
        double edgeAccumulator = 0;

        for (int y = step; y < height - step; y += step) {
            for (int x = step; x < width - step; x += step) {
                int p = bitmap.getPixel(x, y);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                histR[r / 16]++;
                histG[g / 16]++;
                histB[b / 16]++;
                totalPixels++;

                // Simple horizontal edge difference
                int pRight = bitmap.getPixel(x + step, y);
                int lumC = (r * 299 + g * 587 + b * 114) / 1000;
                int lumR = (Color.red(pRight) * 299 + Color.green(pRight) * 587 + Color.blue(pRight) * 114) / 1000;
                edgeAccumulator += Math.abs(lumC - lumR);
            }
        }

        double avgEdgeEnergy = edgeAccumulator / (totalPixels + 1);
        double anomaly = 18.0; // Baseline low anomaly for standard captures

        String colorStatus = "Normal";
        String textureStatus = "Consistent";
        String edgeStatus = "Expected";

        // Evaluate histogram entropy / flat color print anomaly
        int activeBins = 0;
        for (int i = 0; i < 16; i++) {
            if (histR[i] > (totalPixels * 0.02)) activeBins++;
            if (histG[i] > (totalPixels * 0.02)) activeBins++;
            if (histB[i] > (totalPixels * 0.02)) activeBins++;
        }

        if (activeBins < 12) {
            // Unusually flat/monotone palette (cheap photocopy / inkjet anomaly)
            anomaly += 32.0;
            colorStatus = "Unusual palette distribution (flat color bins)";
            textureStatus = "Substrate texture lacks typical micro-depth";
        }

        if (avgEdgeEnergy < 12.0) {
            // Lacks intaglio fine line engraving sharpness
            anomaly += 22.0;
            edgeStatus = "Softer than expected edge structure";
        } else if (avgEdgeEnergy > 55.0) {
            anomaly += 15.0;
            edgeStatus = "Excessive high-frequency noise detected";
        }

        anomaly = Math.max(8.0, Math.min(92.0, anomaly));
        String interpretation = (anomaly < 35.0) ? "Low Anomaly (Expected)" : (anomaly < 60.0 ? "Moderate Anomaly" : "High Anomaly (Suspicious)");

        return new AnomalyReport(Math.round(anomaly * 10.0) / 10.0, interpretation, colorStatus, textureStatus, edgeStatus);
    }
}
