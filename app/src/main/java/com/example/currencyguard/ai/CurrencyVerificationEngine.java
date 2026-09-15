package com.example.currencyguard.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concrete implementation of ICurrencyVerificationEngine.
 * Implements the full ensemble verification pipeline for on-device Android
 * and headless WhatsApp Bot verification.
 */
public class CurrencyVerificationEngine implements ICurrencyVerificationEngine {

    private static volatile CurrencyVerificationEngine INSTANCE;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static CurrencyVerificationEngine getInstance() {
        if (INSTANCE == null) {
            synchronized (CurrencyVerificationEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CurrencyVerificationEngine();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void verifyBanknote(Context context, Bitmap image, String side, VerificationCallback callback) {
        verifyInternal(context, image, null, side, callback);
    }

    @Override
    public void verifyDualSided(Context context, Bitmap frontImage, Bitmap backImage, VerificationCallback callback) {
        verifyInternal(context, frontImage, backImage, "Both", callback);
    }

    public AnalysisResult verifySynchronous(Context context, Bitmap frontImage, Bitmap backImage, String side) {
        return executePipeline(context, frontImage, backImage, side);
    }

    private void verifyInternal(Context context, Bitmap frontImage, Bitmap backImage, String side, VerificationCallback callback) {
        executor.execute(() -> {
            try {
                AnalysisResult result = executePipeline(context, frontImage, backImage, side);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Verification failed"));
            }
        });
    }

    private AnalysisResult executePipeline(Context context, Bitmap frontBitmap, Bitmap backBitmap, String sideHint) {
        boolean isDualSided = (backBitmap != null) || "Both".equalsIgnoreCase(sideHint);
        Bitmap targetBitmap = frontBitmap;

        // Stage 1: Image Quality
        ImageQualityAnalyzer qualityAnalyzer = new ImageQualityAnalyzer();
        ImageQualityAnalyzer.QualityReport qualityReport = qualityAnalyzer.analyze(targetBitmap);

        // Stage 2: OCR Analysis
        OCRAnalyzer ocrAnalyzer = new OCRAnalyzer();
        OCRAnalyzer.OcrResult ocrResult = ocrAnalyzer.analyze(targetBitmap, "500");
        ocrAnalyzer.close();

        // Stage 3: Currency Detection Gate
        CurrencyDetector currencyDetector = new CurrencyDetector();
        CurrencyDetector.DetectionResult detection = currencyDetector.detect(targetBitmap, sideHint, ocrResult.getRawText());

        if (!detection.isCurrency()) {
            AnalysisResult rejectedResult = new AnalysisResult();
            rejectedResult.setCurrencyNote(false);
            rejectedResult.setRejectionReason(detection.getRejectionReason());
            rejectedResult.setStatus(AuthenticityStatus.NOT_A_CURRENCY);
            rejectedResult.setRiskLevel("UNVERIFIED");
            rejectedResult.setRiskScore(99);
            rejectedResult.setVerdictTitle("NOT A CURRENCY NOTE");
            rejectedResult.setVerdictSummary(detection.getRejectionReason());
            rejectedResult.setDenomination("None");
            rejectedResult.setCurrency("N/A");
            rejectedResult.setFinalConfidence(0.0);
            rejectedResult.setVerificationId("CG-REJECTED-" + (System.currentTimeMillis() % 10000));
            rejectedResult.setImageQualityScore(qualityReport.getScore());
            rejectedResult.setAiExplanation("The scanned image does not exhibit standard banknote characteristics or denomination markers.");
            return rejectedResult;
        }

        // Stage 4: Security Feature Analysis
        SecurityFeatureAnalyzer securityAnalyzer = new SecurityFeatureAnalyzer();
        SecurityFeatureAnalyzer.SecurityReport securityReport = securityAnalyzer.analyze(targetBitmap);

        // Stage 5: Classifier
        TensorFlowCurrencyClassifier classifier = new TensorFlowCurrencyClassifier(context);
        CurrencyClassifier.ClassificationResult classResult = classifier.classify(targetBitmap);
        classifier.close();

        // Stage 6: Anomaly Detection
        AnomalyDetector anomalyDetector = new AnomalyDetector();
        AnomalyDetector.AnomalyReport anomalyReport = anomalyDetector.detect(targetBitmap);

        // Anti-Spoofing & Screen Recapture Detector
        com.example.currencyguard.ml.ScreenRecaptureDetector screenDetector = new com.example.currencyguard.ml.ScreenRecaptureDetector();
        com.example.currencyguard.ml.ScreenRecaptureDetector.ScreenReport screenReport = screenDetector.detect(targetBitmap, ocrResult.getRawText());

        // Stage 7: Calibrated Confidence & Risk Scoring
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

        if (ocrResult.getSerialNumber() != null && !ocrResult.getSerialNumber().isEmpty()) {
            analysisResult.setSerialNumber(ocrResult.getSerialNumber());
        }

        // Fallback local explainability
        LocalExplanationGenerator localExplainer = new LocalExplanationGenerator();
        analysisResult.setAiExplanation(localExplainer.generateExplanation(analysisResult));

        return analysisResult;
    }
}
