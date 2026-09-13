package com.example.currencyguard.ml;

import com.example.currencyguard.model.AppConfig;
import com.example.currencyguard.model.UncertaintyReason;

import java.util.ArrayList;
import java.util.List;

/**
 * UNIQUE FEATURE 1: AI DOUBT METER (UncertaintyAnalyzer)
 * Explicit Uncertainty Communication Engine.
 *
 * Rather than masking uncertainty behind a vague percentage, this analyzer
 * audits every individual stage of the multi-model ensemble and identifies
 * explicit causal factors diminishing model confidence.
 */
public class UncertaintyAnalyzer {

    public static class DoubtReport {
        private final List<UncertaintyReason> reasons;
        private final double uncertaintyPenalty;
        private final String primaryRecommendation;
        private final boolean isCertain;

        public DoubtReport(List<UncertaintyReason> reasons, double uncertaintyPenalty, String primaryRecommendation) {
            this.reasons = reasons;
            this.uncertaintyPenalty = uncertaintyPenalty;
            this.primaryRecommendation = primaryRecommendation;
            this.isCertain = reasons.isEmpty();
        }

        public List<UncertaintyReason> getReasons() { return reasons; }
        public double getUncertaintyPenalty() { return uncertaintyPenalty; }
        public String getPrimaryRecommendation() { return primaryRecommendation; }
        public boolean isCertain() { return isCertain; }

        public String getFormattedExplanation() {
            if (reasons.isEmpty()) {
                return "AI analysis exhibits high stability with no significant uncertainty factors detected.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("⚠️ AI is Uncertain due to specific factors:\n");
            for (UncertaintyReason r : reasons) {
                sb.append("• ").append(r.getTitle()).append(": ").append(r.getDescription()).append("\n");
            }
            sb.append("\nRecommendation:\n\"").append(primaryRecommendation).append("\"");
            return sb.toString();
        }
    }

    public DoubtReport evaluate(
            double imageQuality,
            double visualScore,
            double securityScore,
            double ocrScore,
            double geometryScore,
            double anomalyScore,
            boolean isDualSided) {

        List<UncertaintyReason> detectedReasons = new ArrayList<>();
        double penalty = 0.0;

        // 1. Low Image Quality Check
        if (imageQuality < AppConfig.DOUBT_QUALITY_THRESHOLD) {
            detectedReasons.add(UncertaintyReason.LOW_IMAGE_QUALITY);
            penalty += 4.0;
        }

        // 2. Conflicting Models Check (Large variance between visual classifier and security features)
        if (Math.abs(visualScore - securityScore) > 22.0) {
            detectedReasons.add(UncertaintyReason.CONFLICTING_MODELS);
            penalty += 5.0;
        }

        // 3. Unusual Geometry Check
        if (geometryScore < AppConfig.DOUBT_GEOMETRY_THRESHOLD) {
            detectedReasons.add(UncertaintyReason.UNUSUAL_GEOMETRY);
            penalty += 3.5;
        }

        // 4. Missing / Unclear Security Features Check
        if (securityScore < AppConfig.DOUBT_SECURITY_THRESHOLD) {
            detectedReasons.add(UncertaintyReason.MISSING_SECURITY_FEATURES);
            penalty += 5.0;
        }

        // 5. OCR Mismatch Check
        if (ocrScore < AppConfig.DOUBT_OCR_THRESHOLD) {
            detectedReasons.add(UncertaintyReason.OCR_MISMATCH);
            penalty += 4.0;
        }

        // 6. High Anomaly Score Check
        if (anomalyScore > AppConfig.DOUBT_ANOMALY_THRESHOLD) {
            detectedReasons.add(UncertaintyReason.HIGH_ANOMALY_SCORE);
            penalty += 4.5;
        }

        // 7. Single Side Capture Only
        if (!isDualSided) {
            detectedReasons.add(UncertaintyReason.SINGLE_SIDE_ONLY);
            penalty += 3.0;
        }

        // Determine primary actionable recommendation
        String recommendation = "Capture under even, diffused lighting with note completely flat.";
        if (detectedReasons.contains(UncertaintyReason.LOW_IMAGE_QUALITY)) {
            recommendation = "Retake under better lighting with note flat on surface to avoid glare and blur.";
        } else if (detectedReasons.contains(UncertaintyReason.MISSING_SECURITY_FEATURES)) {
            recommendation = "Ensure the watermark and windowed security thread are illuminated and free of reflections.";
        } else if (detectedReasons.contains(UncertaintyReason.UNUSUAL_GEOMETRY)) {
            recommendation = "Flatten note corners and ensure the entire rectangular border is visible.";
        } else if (detectedReasons.contains(UncertaintyReason.SINGLE_SIDE_ONLY)) {
            recommendation = "Scan both front and back sides of the note for higher confidence screening.";
        }

        return new DoubtReport(detectedReasons, penalty, recommendation);
    }
}
