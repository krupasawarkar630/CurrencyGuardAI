package com.example.currencyguard.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Database entity representing an individual currency screening record.
 * Stores all multi-stage metrics, confidence scores, image file paths,
 * AI explanation, and uncertainty reasons for history and timeline analytics.
 */
@Entity(tableName = "scan_results")
public class ScanResult {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long timestamp;
    private String denomination;
    private String currency;
    private double confidence;
    private double imageQuality;

    private double visualScore;
    private double securityScore;
    private double ocrScore;
    private double geometryScore;
    private double anomalyScore;

    private String frontImagePath;
    private String backImagePath;
    private String aiExplanation;
    private String status; // LIKELY_GENUINE, SUSPICIOUS, LIKELY_FAKE, UNABLE_TO_VERIFY
    private String uncertaintyReasonsJson;
    private boolean isSynced;

    // Smart Wallet & Risk Fields
    private String serialNumber = "Unclear";
    private int riskScore = 15;
    private String riskLevel = "LOW_RISK";
    private String verificationId = "CG-2026-0000";
    private boolean isDualSided = false;

    public ScanResult() {
        this.timestamp = System.currentTimeMillis();
        this.currency = "INR";
        this.isSynced = false;
        this.verificationId = "CG-" + (100000 + (int)(Math.random() * 900000));
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDenomination() { return denomination; }
    public void setDenomination(String denomination) { this.denomination = denomination; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getImageQuality() { return imageQuality; }
    public void setImageQuality(double imageQuality) { this.imageQuality = imageQuality; }

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

    public String getFrontImagePath() { return frontImagePath; }
    public void setFrontImagePath(String frontImagePath) { this.frontImagePath = frontImagePath; }

    public String getBackImagePath() { return backImagePath; }
    public void setBackImagePath(String backImagePath) { this.backImagePath = backImagePath; }

    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUncertaintyReasonsJson() { return uncertaintyReasonsJson; }
    public void setUncertaintyReasonsJson(String uncertaintyReasonsJson) {
        this.uncertaintyReasonsJson = uncertaintyReasonsJson;
    }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getVerificationId() { return verificationId; }
    public void setVerificationId(String verificationId) { this.verificationId = verificationId; }

    public boolean isDualSided() { return isDualSided; }
    public void setDualSided(boolean dualSided) { isDualSided = dualSided; }
}
