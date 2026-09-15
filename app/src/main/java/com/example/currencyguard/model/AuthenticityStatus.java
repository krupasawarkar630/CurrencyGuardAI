package com.example.currencyguard.model;

/**
 * Enumeration representing the AI screening assessment outcomes.
 * Provides explicit Real vs Fake verdicts alongside confidence calibration.
 */
public enum AuthenticityStatus {
    LOW_RISK("Low Risk", "LOW RISK", "Detected features are largely consistent with expected banknote patterns."),
    SUSPICIOUS("Suspicious", "SUSPICIOUS NOTE", "Some expected security characteristics could not be confidently verified."),
    HIGH_RISK("High Risk", "HIGH RISK", "Multiple characteristics show significant inconsistencies or failed checks."),
    NOT_A_CURRENCY("Not a Currency Note", "NOT A CURRENCY NOTE", "No recognized banknote detected in this image."),
    UNABLE_TO_VERIFY("Unable to Verify", "UNABLE TO VERIFY", "Poor image quality or obstructed view.");

    // Legacy aliases for backwards compatibility
    public static final AuthenticityStatus LIKELY_GENUINE = LOW_RISK;
    public static final AuthenticityStatus LIKELY_FAKE = HIGH_RISK;

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
        return this == LOW_RISK;
    }

    public boolean isFake() {
        return this == HIGH_RISK;
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
        if (score >= 75.0) {
            return LOW_RISK;
        } else if (score >= 45.0) {
            return SUSPICIOUS;
        } else {
            return HIGH_RISK;
        }
    }
}

