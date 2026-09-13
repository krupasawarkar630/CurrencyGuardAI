package com.example.currencyguard.model;

/**
 * Global configuration parameters, weights, and thresholds for CurrencyGuard AI.
 * Weights are configurable as specified in Section 3 & 12 of the requirements.
 */
public final class AppConfig {

    private AppConfig() {
        // Prevent instantiation
    }

    // AI Ensemble Weights (Must sum to 1.0)
    public static final double WEIGHT_VISUAL = 0.35;
    public static final double WEIGHT_SECURITY = 0.25;
    public static final double WEIGHT_OCR = 0.15;
    public static final double WEIGHT_GEOMETRY = 0.15;
    public static final double WEIGHT_ANOMALY = 0.10;

    // Quality gate thresholds
    public static final double MIN_IMAGE_QUALITY_THRESHOLD = 50.0; // Image blocked below 50%
    public static final double IDEAL_IMAGE_QUALITY_BASELINE = 80.0;

    // AI Doubt / Uncertainty Thresholds
    public static final double DOUBT_QUALITY_THRESHOLD = 70.0;
    public static final double DOUBT_SECURITY_THRESHOLD = 65.0;
    public static final double DOUBT_GEOMETRY_THRESHOLD = 70.0;
    public static final double DOUBT_OCR_THRESHOLD = 65.0;
    public static final double DOUBT_ANOMALY_THRESHOLD = 40.0; // Higher anomaly = more doubt

    // Model configuration
    public static final String MODEL_ASSET_PATH = "currency_model.tflite";
    public static final String LABELS_ASSET_PATH = "labels.txt";
    public static final int MODEL_INPUT_SIZE = 224;
    public static final boolean USE_DEMO_CLASSIFIER_IF_MISSING = true;

    // Volatility thresholds for Confidence Timeline
    public static final double HIGH_VOLATILITY_THRESHOLD = 8.0; // Standard deviation > 8% is volatile
}
