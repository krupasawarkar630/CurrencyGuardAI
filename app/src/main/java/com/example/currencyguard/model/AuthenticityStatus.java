package com.example.currencyguard.model;

/**
 * Enumeration representing the AI screening assessment outcomes.
 * Provides explicit Real vs Fake verdicts alongside confidence calibration.
 */
public enum AuthenticityStatus {
    LIKELY_GENUINE("Likely Genuine", "REAL CURRENCY", "Authentic Banknote Screening Passed"),
    SUSPICIOUS("Suspicious", "SUSPICIOUS NOTE", "Inconclusive / Mixed Security Signals"),
    LIKELY_FAKE("Likely Fake", "FAKE CURRENCY", "High Counterfeit Risk Detected"),
    NOT_A_CURRENCY("Not a Currency Note", "NOT A CURRENCY NOTE", "No recognized banknote detected in this image"),
    UNABLE_TO_VERIFY("Unable to Verify", "UNABLE TO VERIFY", "Poor Image Quality / Obstructed View");

    private final String displayName;
    private final String verdictTitle;
    private final String verdictDescription;

    AuthenticityStatus(String displayName, String verdictTitle, String verdictDescription) {
        this.displayName = displayName;
        this.verdictTitle = verdictTitle;
        this.verdictDescription = verdictDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getVerdictTitle() {
        return verdictTitle;
    }

    public String getVerdictDescription() {
        return verdictDescription;
    }

    public boolean isReal() {
        return this == LIKELY_GENUINE;
    }

    public boolean isFake() {
        return this == LIKELY_FAKE;
    }

    public boolean isSuspicious() {
        return this == SUSPICIOUS;
    }

    public boolean isNotCurrency() {
        return this == NOT_A_CURRENCY;
    }

    public static AuthenticityStatus fromConfidence(double score, boolean hasCriticalFailure) {
        if (hasCriticalFailure) {
            return UNABLE_TO_VERIFY;
        }
        if (score >= 80.0) {
            return LIKELY_GENUINE;
        } else if (score >= 55.0) {
            return SUSPICIOUS;
        } else {
            return LIKELY_FAKE;
        }
    }
}

