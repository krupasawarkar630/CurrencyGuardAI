package com.example.currencyguard.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite domain model encapsulating all stage outcomes from the
 * multi-model AI ensemble screening pipeline.
 */
public class AnalysisResult {

    private String denomination;
    private String currency;
    private String orientation;
    private String side;

    private double imageQualityScore;
    private double visualScore;
    private double securityScore;
    private double ocrScore;
    private double geometryScore;
    private double anomalyScore;
    private double finalConfidence;

    private AuthenticityStatus status;
    private List<UncertaintyReason> uncertaintyReasons = new ArrayList<>();
    private String detectedOcrText;
    private String aiExplanation;
    private boolean isGeminiUsed;

    // Risk Assessment Model (0-100 Risk Score)
    private int riskScore = 15;
    private String riskLevel = "LOW_RISK";
    private String verificationId = "CG-2026-0000";
    private String serialNumber = "Unclear";
    private boolean isTwoSided = false;

    // Granular Security Audit Statuses ("CONSISTENT", "UNCLEAR", "SUSPICIOUS", "NOT_ANALYZED")
    private String securityThreadStatus = "CONSISTENT";
    private String watermarkStatus = "CONSISTENT";
    private String substrateStatus = "CONSISTENT";
    private String alignmentStatus = "CONSISTENT";
    private String securityThreadDetail = "Detected visual characteristics are consistent with expected security-thread region.";
    private String watermarkDetail = "Multi-tonal fiber watermark characteristics detected in expected window.";
    private String substrateDetail = "Substrate rag-paper texture and micro-fibers detected.";
    private String alignmentDetail = "Denomination typography and geometrical aspect ratio align with official standards.";

    // Real vs Fake explicit verdict and feature checklist
    private String verdictTitle = "LOW RISK";
    private String verdictSummary = "Detected features are largely consistent with expected banknote patterns.";
    private boolean isCurrencyNote = true;
    private String rejectionReason = null;
    private boolean isSecurityThreadReal = true;
    private boolean isWatermarkReal = true;
    private boolean isSubstrateReal = true;
    private boolean isPrintAlignmentReal = true;
    private List<String> realFeaturesPassed = new ArrayList<>();
    private List<String> fakeIndicatorsDetected = new ArrayList<>();

    // Component point contributions for explainability
    private double contributionVisual;
    private double contributionSecurity;
    private double contributionOcr;
    private double contributionGeometry;
    private double contributionQuality;

    public AnalysisResult() {
        this.denomination = "₹500";
        this.currency = "INR";
        this.orientation = "Landscape";
        this.side = "Front";
        this.status = AuthenticityStatus.LOW_RISK;
        this.verificationId = "CG-" + (100000 + (int)(Math.random() * 900000));
    }

    public String getDenomination() { return denomination; }
    public void setDenomination(String denomination) { this.denomination = denomination; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public double getImageQualityScore() { return imageQualityScore; }
    public void setImageQualityScore(double imageQualityScore) { this.imageQualityScore = imageQualityScore; }

    public double getVisualScore() { return visualScore; }
    public void setVisualScore(double visualScore) { this.visualScore = visualScore; }

    public double getSecurityScore() { return securityScore; }
    public void setSecurityScore(double securityScore) { this.securityScore = securityScore; }

    public double getOcrScore() { return ocrScore; }
    public void setOcrScore(double ocrScore) { this.ocrScore = ocrScore; }

    public double getGeometryScore() { return geometryScore; }
    public void setGeometryScore(double geometryScore) { this.geometryScore = geometryScore; }

    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }

    public double getFinalConfidence() { return finalConfidence; }
    public void setFinalConfidence(double finalConfidence) { this.finalConfidence = finalConfidence; }

    public AuthenticityStatus getStatus() { return status; }
    public void setStatus(AuthenticityStatus status) { this.status = status; }

    public List<UncertaintyReason> getUncertaintyReasons() { return uncertaintyReasons; }
    public void setUncertaintyReasons(List<UncertaintyReason> uncertaintyReasons) {
        this.uncertaintyReasons = uncertaintyReasons;
    }

    public String getDetectedOcrText() { return detectedOcrText; }
    public void setDetectedOcrText(String detectedOcrText) { this.detectedOcrText = detectedOcrText; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public boolean isGeminiUsed() { return isGeminiUsed; }
    public void setGeminiUsed(boolean geminiUsed) { isGeminiUsed = geminiUsed; }

    public double getContributionVisual() { return contributionVisual; }
    public void setContributionVisual(double contributionVisual) { this.contributionVisual = contributionVisual; }

    public double getContributionSecurity() { return contributionSecurity; }
    public void setContributionSecurity(double contributionSecurity) { this.contributionSecurity = contributionSecurity; }

    public double getContributionOcr() { return contributionOcr; }
    public void setContributionOcr(double contributionOcr) { this.contributionOcr = contributionOcr; }

    public double getContributionGeometry() { return contributionGeometry; }
    public void setContributionGeometry(double contributionGeometry) { this.contributionGeometry = contributionGeometry; }

    public double getContributionQuality() { return contributionQuality; }
    public void setContributionQuality(double contributionQuality) { this.contributionQuality = contributionQuality; }

    public String getVerdictTitle() { return verdictTitle; }
    public void setVerdictTitle(String verdictTitle) { this.verdictTitle = verdictTitle; }

    public String getVerdictSummary() { return verdictSummary; }
    public void setVerdictSummary(String verdictSummary) { this.verdictSummary = verdictSummary; }

    public boolean isCurrencyNote() { return isCurrencyNote; }
    public void setCurrencyNote(boolean currencyNote) { isCurrencyNote = currencyNote; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public boolean isSecurityThreadReal() { return isSecurityThreadReal; }
    public void setSecurityThreadReal(boolean securityThreadReal) { isSecurityThreadReal = securityThreadReal; }

    public boolean isWatermarkReal() { return isWatermarkReal; }
    public void setWatermarkReal(boolean watermarkReal) { isWatermarkReal = watermarkReal; }

    public boolean isSubstrateReal() { return isSubstrateReal; }
    public void setSubstrateReal(boolean substrateReal) { isSubstrateReal = substrateReal; }

    public boolean isPrintAlignmentReal() { return isPrintAlignmentReal; }
    public void setPrintAlignmentReal(boolean printAlignmentReal) { isPrintAlignmentReal = printAlignmentReal; }

    public List<String> getRealFeaturesPassed() { return realFeaturesPassed; }
    public void setRealFeaturesPassed(List<String> realFeaturesPassed) { this.realFeaturesPassed = realFeaturesPassed; }

    public List<String> getFakeIndicatorsDetected() { return fakeIndicatorsDetected; }
    public void setFakeIndicatorsDetected(List<String> fakeIndicatorsDetected) { this.fakeIndicatorsDetected = fakeIndicatorsDetected; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getVerificationId() { return verificationId; }
    public void setVerificationId(String verificationId) { this.verificationId = verificationId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public boolean isTwoSided() { return isTwoSided; }
    public void setTwoSided(boolean twoSided) { isTwoSided = twoSided; }

    public String getSecurityThreadStatus() { return securityThreadStatus; }
    public void setSecurityThreadStatus(String securityThreadStatus) { this.securityThreadStatus = securityThreadStatus; }

    public String getWatermarkStatus() { return watermarkStatus; }
    public void setWatermarkStatus(String watermarkStatus) { this.watermarkStatus = watermarkStatus; }

    public String getSubstrateStatus() { return substrateStatus; }
    public void setSubstrateStatus(String substrateStatus) { this.substrateStatus = substrateStatus; }

    public String getAlignmentStatus() { return alignmentStatus; }
    public void setAlignmentStatus(String alignmentStatus) { this.alignmentStatus = alignmentStatus; }

    public String getSecurityThreadDetail() { return securityThreadDetail; }
    public void setSecurityThreadDetail(String securityThreadDetail) { this.securityThreadDetail = securityThreadDetail; }

    public String getWatermarkDetail() { return watermarkDetail; }
    public void setWatermarkDetail(String watermarkDetail) { this.watermarkDetail = watermarkDetail; }

    public String getSubstrateDetail() { return substrateDetail; }
    public void setSubstrateDetail(String substrateDetail) { this.substrateDetail = substrateDetail; }

    public String getAlignmentDetail() { return alignmentDetail; }
    public void setAlignmentDetail(String alignmentDetail) { this.alignmentDetail = alignmentDetail; }
}

