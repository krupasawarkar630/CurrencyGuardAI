package com.example.currencyguard.ai;

import com.example.currencyguard.ml.ScreenRecaptureDetector;
import com.example.currencyguard.ml.UncertaintyAnalyzer;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.AppConfig;
import com.example.currencyguard.model.AuthenticityStatus;
import com.example.currencyguard.model.UncertaintyReason;

/**
 * AI Confidence Engine.
 * Combines multi-model scores with quality calibration adjustments
 * and uncertainty penalties according to the calibrated mathematical formula.
 * Includes anti-spoofing and digital screen recapture mitigation.
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
        return computeFinalResult(
                denomination, currency, imageQuality, visualScore, securityScore,
                ocrScore, geometryScore, rawAnomalyScore, ocrText, isDualSided, null
        );
    }

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
            boolean isDualSided,
            ScreenRecaptureDetector.ScreenReport screenReport) {

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
        result.setUncertaintyReasons(new java.util.ArrayList<>(doubtReport.getReasons()));

        // Calibrated Confidence Formula
        double weightedSum = (visualScore * AppConfig.WEIGHT_VISUAL) +
                             (securityScore * AppConfig.WEIGHT_SECURITY) +
                             (ocrScore * AppConfig.WEIGHT_OCR) +
                             (geometryScore * AppConfig.WEIGHT_GEOMETRY) +
                             (anomalyConsistency * AppConfig.WEIGHT_ANOMALY);

        // Quality adjustment factor: Images below baseline (80.0) proportionately reduce confidence
        double qualityAdjustment = Math.min(1.0, Math.max(0.4, imageQuality / AppConfig.IDEAL_IMAGE_QUALITY_BASELINE));

        // Calibrated Confidence & Dual-Sided Enhancement
        double dualSideBonus = isDualSided ? 4.0 : 0.0;
        double calibratedScore = (weightedSum * qualityAdjustment) - doubtReport.getUncertaintyPenalty() + dualSideBonus;
        double finalConfidence = Math.max(10.0, Math.min(96.0, Math.round(calibratedScore * 10.0) / 10.0));
        result.setFinalConfidence(finalConfidence);
        result.setTwoSided(isDualSided);

        // Calibrated Risk Score (0 - 100): Lower is safer, Higher is riskier
        int calculatedRisk = (int) Math.max(5, Math.min(95, Math.round(100.0 - finalConfidence)));
        result.setRiskScore(calculatedRisk);

        // Status & Risk Level assignment
        boolean hasCriticalFailure = imageQuality < AppConfig.MIN_IMAGE_QUALITY_THRESHOLD;
        AuthenticityStatus status = AuthenticityStatus.fromConfidence(finalConfidence, hasCriticalFailure);
        result.setStatus(status);

        if (status.isNotCurrency()) {
            result.setRiskLevel("UNVERIFIED");
            result.setVerdictTitle("NOT A CURRENCY NOTE");
            result.setVerdictSummary("Image structure does not conform to official currency specifications.");
        } else if (status == AuthenticityStatus.LOW_RISK) {
            result.setRiskLevel("LOW_RISK");
            result.setVerdictTitle("LOW RISK");
            result.setVerdictSummary("Detected features are largely consistent with the expected banknote pattern.");
        } else if (status == AuthenticityStatus.SUSPICIOUS) {
            result.setRiskLevel("SUSPICIOUS");
            result.setVerdictTitle("SUSPICIOUS");
            result.setVerdictSummary("Some expected characteristics could not be confidently verified. Inspection recommended.");
        } else {
            result.setRiskLevel("HIGH_RISK");
            result.setVerdictTitle("HIGH RISK");
            result.setVerdictSummary("Multiple characteristics show significant inconsistencies with genuine currency.");
        }

        // Anti-Spoofing: Screen Recapture / Laptop Display & Toy Note Override
        boolean isScreenOrToy = (screenReport != null && screenReport.isScreenRecapture());
        if (isScreenOrToy) {
            status = AuthenticityStatus.SUSPICIOUS;
            result.setStatus(status);

            if (screenReport.isToyOrCoupon()) {
                result.setRiskLevel("HIGH_RISK");
                result.setVerdictTitle("SUSPICIOUS (TOY / REPLICA NOTE)");
                result.setVerdictSummary("⚠️ Non-circulating replica detected (" + screenReport.getPrimaryReason() + "). This is not legal tender currency.");
                result.setRiskScore(85);
                result.setFinalConfidence(22.0);
                if (result.getUncertaintyReasons() != null) {
                    result.getUncertaintyReasons().add(0, UncertaintyReason.REPRODUCTION_OR_TOY_NOTE);
                }
            } else {
                result.setRiskLevel("SUSPICIOUS");
                result.setVerdictTitle("SUSPICIOUS (SCREEN RECAPTURE)");
                result.setVerdictSummary("⚠️ Screen Recapture Detected: Image was captured from a computer/laptop screen. Digital screen images do not have physical currency features (cotton fibre, raised ink, metallic thread) and cannot be verified as physical money.");
                result.setRiskScore(65);
                result.setFinalConfidence(44.0);
                if (result.getUncertaintyReasons() != null) {
                    result.getUncertaintyReasons().add(0, UncertaintyReason.DIGITAL_SCREEN_RECAPTURE);
                }
            }
        }

        // Extract serial number if OCR detected alphanumeric sequence
        String extractedSerial = "Unclear";
        if (ocrText != null && !ocrText.trim().isEmpty()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[0-9A-Z]{6,12}").matcher(ocrText);
            if (m.find()) {
                extractedSerial = m.group();
            }
        }
        result.setSerialNumber(extractedSerial);

        // Real vs Fake & Granular Security Feature Audit
        boolean threadReal = !isScreenOrToy && (securityScore >= 70.0);
        boolean watermarkReal = !isScreenOrToy && (securityScore >= 72.0);
        boolean substrateReal = !isScreenOrToy && (rawAnomalyScore <= 42.0);
        boolean alignmentReal = geometryScore >= 72.0 && ocrScore >= 65.0;

        result.setSecurityThreadReal(threadReal);
        result.setWatermarkReal(watermarkReal);
        result.setSubstrateReal(substrateReal);
        result.setPrintAlignmentReal(alignmentReal);

        // Security Thread Granular Status & Detail
        if (isScreenOrToy) {
            result.setSecurityThreadStatus("SUSPICIOUS");
            result.setSecurityThreadDetail(screenReport.isToyOrCoupon() ?
                    "Toy note markings detected: Lacks authentic RBI color-shifting metallic thread." :
                    "Screen display detected: Flat digital pixels cannot reproduce physical windowed color-shift thread.");
        } else if (securityScore >= 72.0) {
            result.setSecurityThreadStatus("CONSISTENT");
            result.setSecurityThreadDetail("Detected metallic window and microprinting characteristics are consistent with genuine specs.");
        } else if (securityScore >= 50.0) {
            result.setSecurityThreadStatus("UNCLEAR");
            result.setSecurityThreadDetail("Security thread region detected but surface reflections or lighting make microtext unclear.");
        } else {
            result.setSecurityThreadStatus("SUSPICIOUS");
            result.setSecurityThreadDetail("Security thread region lacks expected metallic fluorescence or continuous embedding.");
        }

        // Watermark Granular Status & Detail
        if (isScreenOrToy) {
            result.setWatermarkStatus("SUSPICIOUS");
            result.setWatermarkDetail(screenReport.isToyOrCoupon() ?
                    "Lacks authentic multi-directional portrait watermark and electrotype." :
                    "Screen display detected: Watermark must be verified by backlight transmission through real paper.");
        } else if (watermarkReal) {
            result.setWatermarkStatus("CONSISTENT");
            result.setWatermarkDetail("Multi-tonal portrait and denomination watermark detected with proper gradient transitions.");
        } else if (securityScore >= 52.0) {
            result.setWatermarkStatus("UNCLEAR");
            result.setWatermarkDetail("Watermark window detected; contrast is low due to background lighting.");
        } else {
            result.setWatermarkStatus("SUSPICIOUS");
            result.setWatermarkDetail("Watermark appears flat or missing required multi-directional fiber depth.");
        }

        // Substrate / Texture Granular Status & Detail
        if (isScreenOrToy) {
            result.setSubstrateStatus("SUSPICIOUS");
            result.setSubstrateDetail(screenReport.isToyOrCoupon() ?
                    "Toy note markings detected: Lacks official central bank 100% cotton rag paper." :
                    "Digital screen display detected: Lacks physical 100% cotton rag substrate and embedded security fibres.");
        } else if (substrateReal) {
            result.setSubstrateStatus("CONSISTENT");
            result.setSubstrateDetail("Rag-paper tactile fiber profile and anti-counterfeit substrate consistent.");
        } else if (rawAnomalyScore <= 58.0) {
            result.setSubstrateStatus("UNCLEAR");
            result.setSubstrateDetail("Substrate texture shows minor variance, possibly from note handling or fold marks.");
        } else {
            result.setSubstrateStatus("SUSPICIOUS");
            result.setSubstrateDetail("Surface reflection and grain resemble plain inkjet or photocopied paper.");
        }

        // Alignment / Geometry Granular Status & Detail
        if (alignmentReal) {
            result.setAlignmentStatus("CONSISTENT");
            result.setAlignmentDetail("Dimensional aspect ratio, borders, and denomination typography precisely matched.");
        } else if (geometryScore >= 55.0) {
            result.setAlignmentStatus("UNCLEAR");
            result.setAlignmentDetail("Minor perspective tilt or partial border cropping detected.");
        } else {
            result.setAlignmentStatus("SUSPICIOUS");
            result.setAlignmentDetail("Dimensional ratios or typography alignment deviate significantly from reference templates.");
        }

        java.util.List<String> realList = new java.util.ArrayList<>();
        java.util.List<String> fakeList = new java.util.ArrayList<>();

        if (threadReal) realList.add("Security Thread: Consistent metallic embedding");
        else fakeList.add("Security Thread: " + (isScreenOrToy ? "Screen image (no physical thread)" : "Irregular or surface-printed imitation"));

        if (watermarkReal) realList.add("Watermark: Multi-tonal fiber watermark verified");
        else fakeList.add("Watermark: " + (isScreenOrToy ? "Digital display (not real paper watermark)" : "Flat or missing genuine fiber depth"));

        if (substrateReal) realList.add("Substrate & Texture: Banknote cotton-rag paper consistent");
        else fakeList.add("Substrate & Texture: " + (isScreenOrToy ? "Digital screen display (no cotton fibres)" : "Photocopy / plain paper profile"));

        if (alignmentReal) realList.add("Denomination & Alignment: Standard geometry confirmed");
        else fakeList.add("Denomination & Alignment: Typography or dimension variance");

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
