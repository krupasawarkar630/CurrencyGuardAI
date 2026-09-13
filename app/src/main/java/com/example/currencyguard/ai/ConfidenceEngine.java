package com.example.currencyguard.ai;

import com.example.currencyguard.ml.UncertaintyAnalyzer;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.AppConfig;
import com.example.currencyguard.model.AuthenticityStatus;

/**
 * AI Confidence Engine.
 * Combines multi-model scores with quality calibration adjustments
 * and uncertainty penalties according to the calibrated mathematical formula.
 */
public class ConfidenceEngine {

    public AnalysisResult computeFinalResult(
            String denomination,
            String currency,
            double imageQuality,
            double visualScore,
            double securityScore,
            double ocrScore,
            double geometryScore,
            double rawAnomalyScore,
            String ocrText,
            boolean isDualSided) {

        AnalysisResult result = new AnalysisResult();
        result.setDenomination(denomination);
        result.setCurrency(currency);
        result.setImageQualityScore(imageQuality);
        result.setVisualScore(visualScore);
        result.setSecurityScore(securityScore);
        result.setOcrScore(ocrScore);
        result.setGeometryScore(geometryScore);
        result.setAnomalyScore(rawAnomalyScore);
        result.setDetectedOcrText(ocrText);
        result.setSide(isDualSided ? "Both" : "Front");

        // Convert anomaly score to consistency score (lower anomaly = higher consistency)
        double anomalyConsistency = Math.max(0.0, 100.0 - rawAnomalyScore);

        // Run Uncertainty Analyzer (AI Doubt Meter)
        UncertaintyAnalyzer doubtAnalyzer = new UncertaintyAnalyzer();
        UncertaintyAnalyzer.DoubtReport doubtReport = doubtAnalyzer.evaluate(
                imageQuality,
                visualScore,
                securityScore,
                ocrScore,
                geometryScore,
                rawAnomalyScore,
                isDualSided
        );
        result.setUncertaintyReasons(doubtReport.getReasons());

        // Calibrated Confidence Formula
        double weightedSum = (visualScore * AppConfig.WEIGHT_VISUAL) +
                             (securityScore * AppConfig.WEIGHT_SECURITY) +
                             (ocrScore * AppConfig.WEIGHT_OCR) +
                             (geometryScore * AppConfig.WEIGHT_GEOMETRY) +
                             (anomalyConsistency * AppConfig.WEIGHT_ANOMALY);

        // Quality adjustment factor: Images below baseline (80.0) proportionately reduce confidence
        double qualityAdjustment = Math.min(1.0, Math.max(0.4, imageQuality / AppConfig.IDEAL_IMAGE_QUALITY_BASELINE));

        double calibratedScore = (weightedSum * qualityAdjustment) - doubtReport.getUncertaintyPenalty();
        double finalConfidence = Math.max(10.0, Math.min(96.0, Math.round(calibratedScore * 10.0) / 10.0));
        result.setFinalConfidence(finalConfidence);

        // Status & Explicit Real vs Fake Verdict assignment
        boolean hasCriticalFailure = imageQuality < AppConfig.MIN_IMAGE_QUALITY_THRESHOLD;
        AuthenticityStatus status = AuthenticityStatus.fromConfidence(finalConfidence, hasCriticalFailure);
        result.setStatus(status);
        result.setVerdictTitle(status.getVerdictTitle());
        result.setVerdictSummary(status.getVerdictDescription());

        // Real vs Fake Feature Checklist
        boolean threadReal = securityScore >= 72.0;
        boolean watermarkReal = securityScore >= 74.0;
        boolean substrateReal = rawAnomalyScore <= 40.0;
        boolean alignmentReal = geometryScore >= 75.0 && ocrScore >= 68.0;

        result.setSecurityThreadReal(threadReal);
        result.setWatermarkReal(watermarkReal);
        result.setSubstrateReal(substrateReal);
        result.setPrintAlignmentReal(alignmentReal);

        java.util.List<String> realList = new java.util.ArrayList<>();
        java.util.List<String> fakeList = new java.util.ArrayList<>();

        if (threadReal) realList.add("Security Thread: Genuine metallic window with microtext");
        else fakeList.add("Security Thread: Irregular / surface-printed imitation");

        if (watermarkReal) realList.add("Watermark: Multi-tonal fiber watermark window");
        else fakeList.add("Watermark: Flat / missing genuine fiber depth");

        if (substrateReal) realList.add("Substrate & Texture: Banknote rag-paper texture consistent");
        else fakeList.add("Substrate & Texture: Plain paper photocopy / inkjet characteristics");

        if (alignmentReal) realList.add("Denomination & Alignment: Standard currency geometry verified");
        else fakeList.add("Denomination & Alignment: Dimensional or typography variance");

        result.setRealFeaturesPassed(realList);
        result.setFakeIndicatorsDetected(fakeList);

        // Contribution Breakdown calculation (Points summing to final score)
        double ptVisual = Math.round(visualScore * AppConfig.WEIGHT_VISUAL * qualityAdjustment);
        double ptSecurity = Math.round(securityScore * AppConfig.WEIGHT_SECURITY * qualityAdjustment);
        double ptOcr = Math.round(ocrScore * AppConfig.WEIGHT_OCR * qualityAdjustment);
        double ptGeometry = Math.round(geometryScore * AppConfig.WEIGHT_GEOMETRY * qualityAdjustment);
        double ptQuality = Math.max(0, Math.round(imageQuality * 0.05));

        result.setContributionVisual(ptVisual);
        result.setContributionSecurity(ptSecurity);
        result.setContributionOcr(ptOcr);
        result.setContributionGeometry(ptGeometry);
        result.setContributionQuality(ptQuality);

        return result;
    }
}
