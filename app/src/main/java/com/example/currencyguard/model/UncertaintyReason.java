package com.example.currencyguard.model;

/**
 * Reasons detected by the AI Doubt Meter (UncertaintyAnalyzer)
 * to explicitly communicate why confidence is diminished.
 */
public enum UncertaintyReason {
    LOW_IMAGE_QUALITY(
        "Low Image Quality",
        "Image quality below threshold (glare, blur or uneven illumination detected).",
        "Retake under even lighting with note flat on surface."
    ),
    CONFLICTING_MODELS(
        "Conflicting Classifier Signals",
        "The primary neural network and the regional security models disagree.",
        "Take an additional scan to allow model ensemble calibration."
    ),
    UNUSUAL_GEOMETRY(
        "Unusual Geometry & Dimensions",
        "Note aspect ratio and corner alignment deviate from standard currency specifications.",
        "Ensure note is fully uncreased and edges are strictly within frame."
    ),
    MISSING_SECURITY_FEATURES(
        "Security Feature Region Unclear",
        "Watermark or security thread region appears unclear, obscured, or missing.",
        "Hold note flat with transmitted light if checking watermark."
    ),
    OCR_MISMATCH(
        "OCR Denomination Mismatch",
        "Printed character recognition does not correlate with the expected currency denomination.",
        "Ensure the denomination and serial numbers are not obstructed."
    ),
    HIGH_ANOMALY_SCORE(
        "High Visual Anomaly Score",
        "Texture distribution and color histogram variance exceed normal tolerance limits.",
        "Check note for stains, tears, or physical alterations."
    ),
    SINGLE_SIDE_ONLY(
        "Single Side Capture Only",
        "Only one face of the note was analyzed. Both front and back are recommended for full verification.",
        "Scan the reverse side for complete two-sided verification."
    );

    private final String title;
    private final String description;
    private final String recommendation;

    UncertaintyReason(String title, String description, String recommendation) {
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
