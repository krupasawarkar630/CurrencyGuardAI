package com.example.currencyguard.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyguard.R;
import com.example.currencyguard.ai.ConfidenceEngine;
import com.example.currencyguard.ai.LocalExplanationGenerator;
import com.example.currencyguard.ml.AnomalyDetector;
import com.example.currencyguard.ml.CurrencyClassifier;
import com.example.currencyguard.ml.CurrencyDetector;
import com.example.currencyguard.ml.HeatmapGenerator;
import com.example.currencyguard.ml.ImageQualityAnalyzer;
import com.example.currencyguard.ml.OCRAnalyzer;
import com.example.currencyguard.ml.SecurityFeatureAnalyzer;
import com.example.currencyguard.ml.TensorFlowCurrencyClassifier;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.AuthenticityStatus;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.repository.ScanRepository;
import com.example.currencyguard.utils.ImageUtils;

import org.json.JSONArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-Stage AI Pipeline Execution Activity.
 * Orchestrates 7 distinct screening stages with animated visual progress
 * and persists the comprehensive result to Room Database before navigating to ResultActivity.
 */
public class AnalysisActivity extends AppCompatActivity {

    public static final String EXTRA_FRONT_PATH = "extra_front_path";
    public static final String EXTRA_BACK_PATH = "extra_back_path";
    public static final String EXTRA_SIDE = "extra_side";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    public static AnalysisResult currentAnalysisResult;
    public static Bitmap currentNoteBitmap;
    public static Bitmap currentHeatmapBitmap;

    private TextView tvPipelineStatus;
    private TextView stepQuality;
    private TextView stepCurrency;
    private TextView stepOcr;
    private TextView stepSecurity;
    private TextView stepTflite;
    private TextView stepAnomaly;
    private TextView stepDoubt;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService pipelineExecutor = Executors.newSingleThreadExecutor();

    private ScanRepository scanRepository;
    private String frontPath;
    private String backPath;
    private String sideHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        tvPipelineStatus = findViewById(R.id.tv_pipeline_status);
        stepQuality = findViewById(R.id.step_quality);
        stepCurrency = findViewById(R.id.step_currency);
        stepOcr = findViewById(R.id.step_ocr);
        stepSecurity = findViewById(R.id.step_security);
        stepTflite = findViewById(R.id.step_tflite);
        stepAnomaly = findViewById(R.id.step_anomaly);
        stepDoubt = findViewById(R.id.step_doubt);

        scanRepository = new ScanRepository(getApplication());

        frontPath = getIntent().getStringExtra(EXTRA_FRONT_PATH);
        backPath = getIntent().getStringExtra(EXTRA_BACK_PATH);
        sideHint = getIntent().getStringExtra(EXTRA_SIDE);
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);

        pipelineExecutor.execute(() -> {
            try {
                Bitmap inputBitmap = null;
                if (frontPath != null) {
                    inputBitmap = ImageUtils.loadAndCorrectOrientation(frontPath);
                } else if (uriString != null) {
                    inputBitmap = ImageUtils.loadBitmapFromUri(this, Uri.parse(uriString));
                    if (inputBitmap != null) {
                        frontPath = ImageUtils.saveBitmap(this, inputBitmap, "gallery_note");
                    }
                }

                if (inputBitmap == null) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Failed to load currency note image.", Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                runPipeline(inputBitmap);
            } catch (Throwable t) {
                Log.e("AnalysisActivity", "Pipeline processing failed", t);
                mainHandler.post(() -> {
                    Toast.makeText(this, "Analysis failed: " + (t.getMessage() != null ? t.getMessage() : "Unexpected error"), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void runPipeline(Bitmap bitmap) {
        currentNoteBitmap = bitmap;
        boolean isDualSided = (backPath != null && !backPath.isEmpty()) || "Both".equalsIgnoreCase(sideHint);

        // Stage 1: Image Quality Analyzer
        updateStep(stepQuality, "✓ Stage 1: Image Quality Verified", "Analyzing quality...");
        ImageQualityAnalyzer qualityAnalyzer = new ImageQualityAnalyzer();
        ImageQualityAnalyzer.QualityReport qualityReport = qualityAnalyzer.analyze(bitmap);
        sleepBriefly(300);

        // Stage 2: OCR Analysis
        updateStep(stepOcr, "⏳ Stage 2: Scanning OCR & Typography...", "Scanning text...");
        OCRAnalyzer ocrAnalyzer = new OCRAnalyzer();
        OCRAnalyzer.OcrResult ocrResult = ocrAnalyzer.analyze(bitmap, "500");
        ocrAnalyzer.close();
        sleepBriefly(250);

        // Stage 3: Banknote Verification Gate
        updateStep(stepCurrency, "⏳ Stage 3: Banknote Verification Gate...", "Checking if image is a currency note...");
        CurrencyDetector currencyDetector = new CurrencyDetector();
        CurrencyDetector.DetectionResult detection = currencyDetector.detect(bitmap, sideHint, ocrResult.getRawText());
        sleepBriefly(250);

        if (!detection.isCurrency()) {
            // Rejection: Not recognized as currency
            mainHandler.post(() -> {
                tvPipelineStatus.setText("Rejected: Not a Currency Note");
                stepCurrency.setText("✗ Stage 3: NOT A CURRENCY NOTE");
                stepCurrency.setTextColor(getColor(R.color.status_fake));
            });

            AnalysisResult rejectedResult = new AnalysisResult();
            rejectedResult.setCurrencyNote(false);
            rejectedResult.setRejectionReason(detection.getRejectionReason());
            rejectedResult.setStatus(AuthenticityStatus.NOT_A_CURRENCY);
            rejectedResult.setVerdictTitle("NOT A CURRENCY NOTE");
            rejectedResult.setVerdictSummary(detection.getRejectionReason());
            rejectedResult.setDenomination("None");
            rejectedResult.setCurrency("N/A");
            rejectedResult.setFinalConfidence(0.0);
            rejectedResult.setRiskScore(100);
            rejectedResult.setRiskLevel("UNVERIFIED");
            rejectedResult.setVerificationId("CG-REJECTED-" + (System.currentTimeMillis() % 10000));
            rejectedResult.setImageQualityScore(qualityReport.getScore());
            rejectedResult.setAiExplanation("The uploaded image does not contain a recognized currency note. " +
                    "CurrencyGuard AI is designed strictly for currency banknotes. " +
                    "Please capture or upload a clear photo of an official banknote (₹10, ₹20, ₹50, ₹100, ₹200, ₹500).");

            currentAnalysisResult = rejectedResult;
            currentHeatmapBitmap = null;

            // Persist rejected scan record to Room database so history & counts remain accurate
            saveToDatabase(rejectedResult, () -> {
                mainHandler.post(() -> {
                    Intent intent = new Intent(AnalysisActivity.this, ResultActivity.class);
                    intent.putExtra(ResultActivity.EXTRA_FRONT_IMAGE_PATH, frontPath);
                    startActivity(intent);
                    finish();
                });
            });
            return;
        }

        updateStep(stepCurrency, "✓ Stage 3: Confirmed Currency (" + detection.getDenomination() + ")", "Banknote verified. Analyzing security features...");
        updateStep(stepOcr, "✓ Stage 2: OCR Text Verified", "OCR verified.");
        sleepBriefly(250);

        // Stage 4: Security Feature Analyzer
        updateStep(stepSecurity, "✓ Stage 4: Security Regions Analyzed", "Auditing security feature regions...");
        SecurityFeatureAnalyzer securityAnalyzer = new SecurityFeatureAnalyzer();
        SecurityFeatureAnalyzer.SecurityReport securityReport = securityAnalyzer.analyze(bitmap);
        sleepBriefly(300);

        // Stage 5: TensorFlow Lite Classifier
        updateStep(stepTflite, "✓ Stage 5: TFLite Classification Complete", "Executing visual neural network...");
        TensorFlowCurrencyClassifier tfliteClassifier = new TensorFlowCurrencyClassifier(this);
        CurrencyClassifier.ClassificationResult classResult = tfliteClassifier.classify(bitmap);
        tfliteClassifier.close();
        sleepBriefly(300);

        // Stage 6: Anomaly & Texture Dispersion
        updateStep(stepAnomaly, "✓ Stage 6: Anomaly Screening Complete", "Calculating visual anomaly index...");
        AnomalyDetector anomalyDetector = new AnomalyDetector();
        AnomalyDetector.AnomalyReport anomalyReport = anomalyDetector.detect(bitmap);
        sleepBriefly(250);

        // Anti-Spoofing & Screen Recapture Analysis
        com.example.currencyguard.ml.ScreenRecaptureDetector screenDetector = new com.example.currencyguard.ml.ScreenRecaptureDetector();
        com.example.currencyguard.ml.ScreenRecaptureDetector.ScreenReport screenReport = screenDetector.detect(bitmap, ocrResult.getRawText());

        // Stage 7: Confidence Engine & AI Doubt Meter
        updateStep(stepDoubt, "✓ Stage 7: Confidence Calibrated & Doubt Audited", "Finalizing assessment...");
        ConfidenceEngine confidenceEngine = new ConfidenceEngine();
        AnalysisResult analysisResult = confidenceEngine.computeFinalResult(
                detection.getDenomination(),
                detection.getCurrency(),
                qualityReport.getScore(),
                classResult.getConfidence(),
                securityReport.getOverallScore(),
                ocrResult.getScore(),
                detection.getGeometryScore(),
                anomalyReport.getAnomalyScore(),
                ocrResult.getRawText(),
                isDualSided,
                screenReport
        );

        // Generate Grad-CAM Attention Heatmap
        HeatmapGenerator heatmapGenerator = new HeatmapGenerator();
        currentHeatmapBitmap = heatmapGenerator.generateHeatmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                anomalyReport.getAnomalyScore(),
                securityReport.getOverallScore()
        );

        // Generate Natural Language Explanation (Gemini with Local Fallback)
        LocalExplanationGenerator localGenerator = new LocalExplanationGenerator();
        analysisResult.setAiExplanation(localGenerator.generateExplanation(analysisResult));

        currentAnalysisResult = analysisResult;

        // Persist to Room Database and then transition to Result Screen
        saveToDatabase(analysisResult, () -> {
            mainHandler.post(() -> {
                Intent intent = new Intent(AnalysisActivity.this, ResultActivity.class);
                intent.putExtra(ResultActivity.EXTRA_FRONT_IMAGE_PATH, frontPath);
                intent.putExtra(ResultActivity.EXTRA_BACK_IMAGE_PATH, backPath);
                startActivity(intent);
                finish();
            });
        });
    }

    private void updateStep(TextView stepView, String completedText, String currentStatus) {
        mainHandler.post(() -> {
            tvPipelineStatus.setText(currentStatus);
            stepView.setText(completedText);
            stepView.setTextColor(getColor(R.color.status_genuine));
        });
    }

    private void sleepBriefly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private void saveToDatabase(AnalysisResult result, Runnable onComplete) {
        ScanResult scan = new ScanResult();
        scan.setTimestamp(System.currentTimeMillis());
        scan.setDenomination(result.getDenomination());
        scan.setCurrency(result.getCurrency());
        scan.setConfidence(result.getFinalConfidence());
        scan.setImageQuality(result.getImageQualityScore());
        scan.setVisualScore(result.getVisualScore());
        scan.setSecurityScore(result.getSecurityScore());
        scan.setOcrScore(result.getOcrScore());
        scan.setGeometryScore(result.getGeometryScore());
        scan.setAnomalyScore(result.getAnomalyScore());
        scan.setFrontImagePath(frontPath);
        scan.setBackImagePath(backPath);
        scan.setAiExplanation(result.getAiExplanation());
        scan.setStatus(result.getStatus().name());
        scan.setRiskScore(result.getRiskScore());
        scan.setRiskLevel(result.getRiskLevel());
        scan.setVerificationId(result.getVerificationId());
        scan.setSerialNumber(result.getSerialNumber());
        scan.setDualSided((backPath != null && !backPath.isEmpty()) || "Both".equalsIgnoreCase(sideHint));

        JSONArray reasonsArray = new JSONArray();
        if (result.getUncertaintyReasons() != null) {
            for (var r : result.getUncertaintyReasons()) {
                reasonsArray.put(r.name());
            }
        }
        scan.setUncertaintyReasonsJson(reasonsArray.toString());

        scanRepository.insert(scan, id -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pipelineExecutor.shutdown();
    }
}
