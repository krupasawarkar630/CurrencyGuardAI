package com.example.currencyguard.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.currencyguard.R;
import com.example.currencyguard.ai.AiVoiceManager;
import com.example.currencyguard.ai.GeminiExplanationService;
import com.example.currencyguard.ai.GeminiVisionService;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.model.UncertaintyReason;
import com.example.currencyguard.repository.ScanRepository;
import com.example.currencyguard.ui.ConfidenceTimelineChart;
import com.example.currencyguard.ui.HeatmapOverlayView;
import com.example.currencyguard.utils.PdfReportGenerator;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

/**
 * Flagship Explainable AI Result Screen.
 * Presents Authenticity Assessment, AI Doubt Meter, Confidence Timeline Chart,
 * Grad-CAM Attention Heatmap overlay, and PDF Passport export.
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_FRONT_IMAGE_PATH = "extra_front_image_path";
    public static final String EXTRA_BACK_IMAGE_PATH = "extra_back_image_path";

    private AnalysisResult result;
    private ScanRepository scanRepository;

    private ImageView ivScannedNote;
    private HeatmapOverlayView viewHeatmapOverlay;
    private MaterialButton btnToggleHeatmap;
    private boolean isHeatmapVisible = false;

    // Real vs Fake Hero Views
    private ImageView ivVerdictIcon;
    private TextView tvStatusTitle;
    private TextView tvVerdictSummary;
    private TextView tvOverallConfidence;
    private TextView tvSidesInfo;
    private MaterialCardView cardStatusContainer;

    // Real vs Fake Feature Checklist Views
    private MaterialCardView cardFeatureChecklist;
    private TextView tvCheckThread;
    private TextView tvCheckWatermark;
    private TextView tvCheckSubstrate;
    private TextView tvCheckAlignment;

    // Feature 1: Doubt Meter
    private MaterialCardView cardDoubtMeter;
    private TextView tvDoubtReasons;
    private TextView tvDoubtRecommendation;

    // Feature 2: Confidence Timeline
    private LineChart timelineLineChart;
    private TextView tvConsistencyScore;
    private TextView tvVolatilityScore;
    private TextView tvTimelineInterpretation;

    private TextView tvBreakdownPoints;
    private TextView badgeAiSource;
    private TextView tvAiExplanationBody;

    // AI Voice & Deep Vision Components
    private MaterialButton btnVoiceVerdict;
    private TextView tvVoiceStatus;
    private ImageView ivVoiceIcon;
    private AiVoiceManager voiceManager;

    private MaterialButton btnRunVisionAudit;
    private ProgressBar progressVisionAudit;
    private TextView tvVisionAuditResult;
    private MaterialButton btnSpeakVisionReport;
    private TextView tvVisionModelBadge;
    private GeminiVisionService geminiVisionService;
    private String lastVisionReport = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        result = AnalysisActivity.currentAnalysisResult;
        if (result == null) {
            Toast.makeText(this, "No analysis data found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        voiceManager = AiVoiceManager.getInstance(this);
        geminiVisionService = new GeminiVisionService();
        scanRepository = new ScanRepository(getApplication());
        initViews();
        bindData();
        if (result.isCurrencyNote()) {
            setupTimelineChart();
        } else {
            timelineLineChart.setVisibility(View.GONE);
            findViewById(R.id.tv_consistency_score).setVisibility(View.GONE);
            findViewById(R.id.tv_volatility_score).setVisibility(View.GONE);
            findViewById(R.id.tv_timeline_interpretation).setVisibility(View.GONE);
        }
        checkGeminiCloudExplanation();

        // Check if Auto-Speak Result is enabled in settings
        if (AiVoiceManager.isAutoSpeakEnabled(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::toggleVoiceVerdict, 600);
        }
    }

    private void initViews() {
        findViewById(R.id.btn_back_result).setOnClickListener(v -> finish());

        TextView tvHeader = findViewById(R.id.tv_result_header_title);
        if (result.isCurrencyNote()) {
            tvHeader.setText(result.getDenomination() + " " + result.getCurrency());
        } else {
            tvHeader.setText("Non-Currency Image");
        }

        ivScannedNote = findViewById(R.id.iv_scanned_note);
        viewHeatmapOverlay = findViewById(R.id.view_heatmap_overlay);
        btnToggleHeatmap = findViewById(R.id.btn_toggle_heatmap);

        // Real vs Fake Hero components
        cardStatusContainer = findViewById(R.id.card_status_container);
        ivVerdictIcon = findViewById(R.id.iv_verdict_icon);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvVerdictSummary = findViewById(R.id.tv_verdict_summary);
        tvOverallConfidence = findViewById(R.id.tv_overall_confidence);
        tvSidesInfo = findViewById(R.id.tv_sides_info);

        // Real vs Fake Checklist components
        cardFeatureChecklist = findViewById(R.id.card_feature_checklist);
        tvCheckThread = findViewById(R.id.tv_check_thread);
        tvCheckWatermark = findViewById(R.id.tv_check_watermark);
        tvCheckSubstrate = findViewById(R.id.tv_check_substrate);
        tvCheckAlignment = findViewById(R.id.tv_check_alignment);

        cardDoubtMeter = findViewById(R.id.card_doubt_meter);
        tvDoubtReasons = findViewById(R.id.tv_doubt_reasons);
        tvDoubtRecommendation = findViewById(R.id.tv_doubt_recommendation);

        timelineLineChart = findViewById(R.id.timeline_line_chart);
        tvConsistencyScore = findViewById(R.id.tv_consistency_score);
        tvVolatilityScore = findViewById(R.id.tv_volatility_score);
        tvTimelineInterpretation = findViewById(R.id.tv_timeline_interpretation);

        tvBreakdownPoints = findViewById(R.id.tv_breakdown_points);
        badgeAiSource = findViewById(R.id.badge_ai_source);
        tvAiExplanationBody = findViewById(R.id.tv_ai_explanation_body);

        // Heatmap Toggle
        btnToggleHeatmap.setOnClickListener(v -> {
            isHeatmapVisible = !isHeatmapVisible;
            viewHeatmapOverlay.setVisibility(isHeatmapVisible ? View.VISIBLE : View.GONE);
            btnToggleHeatmap.setText(isHeatmapVisible ? "Hide Attention Heatmap" : "Show AI Attention Heatmap (Grad-CAM)");
        });

        // Action Buttons
        findViewById(R.id.btn_convert_result).setOnClickListener(v -> {
            double parsedAmount = 200.0;
            if (result != null && result.getDenomination() != null) {
                String cleanNum = result.getDenomination().replaceAll("[^0-9]", "");
                if (!cleanNum.isEmpty()) {
                    try {
                        parsedAmount = Double.parseDouble(cleanNum);
                    } catch (Exception ignored) {}
                }
            }
            Intent convertIntent = new Intent(ResultActivity.this, CurrencyConverterActivity.class);
            convertIntent.putExtra(CurrencyConverterActivity.EXTRA_AMOUNT, parsedAmount);
            startActivity(convertIntent);
        });

        findViewById(R.id.btn_share_result).setOnClickListener(v -> {
            String denom = (result != null && result.getDenomination() != null) ? result.getDenomination() : "Note";
            String curr = (result != null && result.getCurrency() != null) ? result.getCurrency() : "INR";
            String status = (result != null && result.getVerdictTitle() != null) ? result.getVerdictTitle() : "Screened";
            double conf = (result != null) ? result.getFinalConfidence() : 0.0;
            com.example.currencyguard.utils.ShareUtils.shareScreeningResult(ResultActivity.this, denom, curr, status, conf);
        });

        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.btn_quick_pdf).setOnClickListener(v -> exportPdf());

        findViewById(R.id.btn_ask_currencyguard).setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, ChatAssistantActivity.class));
        });

        findViewById(R.id.btn_scan_another).setOnClickListener(v -> {
            startActivity(new Intent(ResultActivity.this, ScanActivity.class));
            finish();
        });

        // AI Voice components
        btnVoiceVerdict = findViewById(R.id.btn_voice_verdict);
        tvVoiceStatus = findViewById(R.id.tv_voice_status);
        ivVoiceIcon = findViewById(R.id.iv_voice_icon);
        if (btnVoiceVerdict != null) {
            btnVoiceVerdict.setOnClickListener(v -> toggleVoiceVerdict());
        }

        // Deep Multimodal AI Vision Audit components
        btnRunVisionAudit = findViewById(R.id.btn_run_vision_audit);
        progressVisionAudit = findViewById(R.id.progress_vision_audit);
        tvVisionAuditResult = findViewById(R.id.tv_vision_audit_result);
        btnSpeakVisionReport = findViewById(R.id.btn_speak_vision_report);
        tvVisionModelBadge = findViewById(R.id.tv_vision_model_badge);

        if (tvVisionModelBadge != null) {
            String activeModel = com.example.currencyguard.ai.GeminiExplanationService.getActiveModel(this);
            tvVisionModelBadge.setText("Multimodal AI: " + activeModel);
        }

        if (btnRunVisionAudit != null) {
            btnRunVisionAudit.setOnClickListener(v -> runDeepVisionAudit());
        }

        if (btnSpeakVisionReport != null) {
            btnSpeakVisionReport.setOnClickListener(v -> speakVisionReport());
        }
    }

    private void bindData() {
        // Scanned Note & Heatmap
        if (AnalysisActivity.currentNoteBitmap != null) {
            ivScannedNote.setImageBitmap(AnalysisActivity.currentNoteBitmap);
        }
        if (AnalysisActivity.currentHeatmapBitmap != null) {
            viewHeatmapOverlay.setHeatmapBitmap(AnalysisActivity.currentHeatmapBitmap);
        }

        // Direct Real vs Fake Verdict Presentation
        String verdictTitle = result.getVerdictTitle();
        tvStatusTitle.setText(verdictTitle);
        tvVerdictSummary.setText(result.getVerdictSummary());

        int primaryColor = Color.parseColor("#00C853"); // Green default for genuine
        int containerColor = Color.parseColor("#E8F5E9");

        if (result.getStatus().isNotCurrency()) {
            primaryColor = Color.parseColor("#D32F2F"); // Strong alert red
            containerColor = Color.parseColor("#FFEBEE");
            ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
            ivVerdictIcon.setColorFilter(primaryColor);
            tvOverallConfidence.setText("N/A");
            tvSidesInfo.setText("⚠️ Screening blocked: Scanned image is not a recognized banknote.");

            // Hide banknote specific checklist
            if (cardFeatureChecklist != null) {
                cardFeatureChecklist.setVisibility(View.GONE);
            }
            btnToggleHeatmap.setVisibility(View.GONE);

            // Rejection reason card
            cardDoubtMeter.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            cardDoubtMeter.setStrokeColor(Color.parseColor("#FF9800"));
            tvDoubtReasons.setText("• No recognized banknote denomination markings found.\n• Visual structure and geometry do not match currency standards.\n• Central bank typography not detected.");
            tvDoubtRecommendation.setText("Please scan or upload a clear photo of an official banknote (e.g. ₹10, ₹20, ₹50, ₹100, ₹200, ₹500).");

            tvBreakdownPoints.setText("• Image Detection: Not a Currency Note\n• Counterfeit Screening: Halted\n• Reason: " + result.getVerdictSummary());
        } else {
            tvOverallConfidence.setText((int) result.getFinalConfidence() + "%");

            if (result.getStatus().isReal()) {
                primaryColor = Color.parseColor("#00C853");
                containerColor = Color.parseColor("#E8F5E9");
                ivVerdictIcon.setImageResource(R.drawable.ic_security);
                ivVerdictIcon.setColorFilter(primaryColor);
            } else if (result.getStatus().isFake()) {
                primaryColor = Color.parseColor("#F44336"); // Red
                containerColor = Color.parseColor("#FFEBEE");
                ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
                ivVerdictIcon.setColorFilter(primaryColor);
            } else if (result.getStatus().isSuspicious()) {
                primaryColor = Color.parseColor("#FFB300"); // Amber
                containerColor = Color.parseColor("#FFF8E1");
                ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
                ivVerdictIcon.setColorFilter(primaryColor);
            } else {
                primaryColor = Color.parseColor("#78909C");
                containerColor = Color.parseColor("#ECEFF1");
                ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
                ivVerdictIcon.setColorFilter(primaryColor);
            }

            // Bind Real vs Fake Feature Checklist Items
            if (cardFeatureChecklist != null) {
                cardFeatureChecklist.setVisibility(View.VISIBLE);
            }
            bindChecklistItem(tvCheckThread, result.isSecurityThreadReal());
            bindChecklistItem(tvCheckWatermark, result.isWatermarkReal());
            bindChecklistItem(tvCheckSubstrate, result.isSubstrateReal());
            bindChecklistItem(tvCheckAlignment, result.isPrintAlignmentReal());

            // Sides verification text
            if ("Both".equalsIgnoreCase(result.getSide())) {
                tvSidesInfo.setText("Front & Back Dual-Sided Scan Verified (High Reliability)");
            } else {
                tvSidesInfo.setText("⚠ Partial AI analysis — Only front side scanned. Back side scan recommended.");
            }
        }

        tvStatusTitle.setTextColor(primaryColor);
        tvOverallConfidence.setTextColor(primaryColor);
        cardStatusContainer.setCardBackgroundColor(containerColor);
        cardStatusContainer.setStrokeColor(primaryColor);

        // Feature 1: AI Doubt Meter
        if (result.getUncertaintyReasons().isEmpty()) {
            cardDoubtMeter.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            cardDoubtMeter.setStrokeColor(Color.parseColor("#00C853"));
            tvDoubtReasons.setText("✓ No significant uncertainty factors detected across models.\n✓ Security features, OCR, and geometry are aligned.");
            tvDoubtRecommendation.setText("Stable screening. Always cross-verify high denomination notes.");
            tvDoubtRecommendation.setTextColor(Color.parseColor("#00C853"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (UncertaintyReason r : result.getUncertaintyReasons()) {
                sb.append("• ").append(r.getTitle()).append(": ").append(r.getDescription()).append("\n");
            }
            tvDoubtReasons.setText(sb.toString().trim());

            String primaryRec = result.getUncertaintyReasons().get(0).getRecommendation();
            tvDoubtRecommendation.setText("Recommendation: \"" + primaryRec + "\"");
        }

        // Contribution Breakdown
        StringBuilder points = new StringBuilder();
        points.append("• Visual Similarity:       +").append((int) result.getContributionVisual()).append(" points\n");
        points.append("• Security Features:      +").append((int) result.getContributionSecurity()).append(" points\n");
        points.append("• OCR Consistency:        +").append((int) result.getContributionOcr()).append(" points\n");
        points.append("• Geometry & Layout:      +").append((int) result.getContributionGeometry()).append(" points\n");
        points.append("• Input Image Quality:    +").append((int) result.getContributionQuality()).append(" points\n");
        points.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        points.append("Final Calibrated Score:   ").append((int) result.getFinalConfidence()).append(" / 100");
        tvBreakdownPoints.setText(points.toString());

        // Initial Explanation
        tvAiExplanationBody.setText(result.getAiExplanation());
    }

    // Feature 2: Confidence Timeline Chart setup
    private void setupTimelineChart() {
        new Thread(() -> {
            List<ScanResult> history = scanRepository.getScansByDenominationSync(result.getDenomination());
            runOnUiThread(() -> {
                ConfidenceTimelineChart.StabilityMetrics metrics =
                        ConfidenceTimelineChart.setupChart(timelineLineChart, history, result.getFinalConfidence());

                tvConsistencyScore.setText("AI Consistency Score: " + (int) metrics.getConsistencyScore() + "%");
                tvVolatilityScore.setText("Volatility: ±" + metrics.getVolatilityScore() + "% (" + (metrics.isStable() ? "Low" : "High") + ")");
                tvVolatilityScore.setTextColor(metrics.isStable() ? Color.parseColor("#00C853") : Color.parseColor("#F44336"));
                tvTimelineInterpretation.setText("Interpretation: " + metrics.getInterpretation());
            });
        }).start();
    }

    private void checkGeminiCloudExplanation() {
        GeminiExplanationService geminiService = new GeminiExplanationService();
        geminiService.explain(this, result, (explanation, isGeminiCloud) -> {
            if (!isFinishing() && !isDestroyed()) {
                tvAiExplanationBody.setText(explanation);
                if (isGeminiCloud) {
                    badgeAiSource.setText("🌐 OpenRouter AI");
                    badgeAiSource.setTextColor(Color.parseColor("#1976D2"));
                } else {
                    badgeAiSource.setText("🔒 On-Device");
                    badgeAiSource.setTextColor(Color.parseColor("#388E3C"));
                }
            }
        });
    }

    private void exportPdf() {
        File pdf = PdfReportGenerator.generateReport(this, result, "CG-" + (System.currentTimeMillis() % 1000000));
        if (pdf != null && pdf.exists()) {
            try {
                Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(pdfUri, "application/pdf");
                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, "Open Currency Passport Report"));
            } catch (Exception e) {
                Toast.makeText(this, "PDF saved to " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Failed to generate PDF document.", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindChecklistItem(TextView textView, boolean isReal) {
        if (textView == null) return;
        if (isReal) {
            textView.setText("REAL ✓");
            textView.setTextColor(Color.parseColor("#00C853"));
        } else {
            textView.setText("FAKE ✗");
            textView.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void toggleVoiceVerdict() {
        if (voiceManager == null) return;

        if (voiceManager.isSpeaking()) {
            voiceManager.stop();
            if (btnVoiceVerdict != null) {
                btnVoiceVerdict.setText("Speak Verdict");
                btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
            }
            if (tvVoiceStatus != null) {
                tvVoiceStatus.setText("Tap to listen to spoken AI screening analysis");
            }
            return;
        }

        if (btnVoiceVerdict != null) {
            btnVoiceVerdict.setText("Stop Voice");
            btnVoiceVerdict.setIconResource(R.drawable.ic_stop);
        }
        if (tvVoiceStatus != null) {
            tvVoiceStatus.setText("🔊 Speaking AI Verdict...");
        }

        voiceManager.speakAnalysisResult(this, result, new com.example.currencyguard.ai.AiVoiceManager.SpeechListener() {
            @Override
            public void onSpeechStarted() {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Stop Voice");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_stop);
                }
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("🔊 Speaking AI Verdict...");
                }
            }

            @Override
            public void onSpeechCompleted() {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Speak Verdict");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
                }
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("Completed. Tap to replay analysis.");
                }
            }

            @Override
            public void onSpeechError(String error) {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Speak Verdict");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
                }
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("Speech unavailable: " + error);
                }
            }
        });
    }

    private void runDeepVisionAudit() {
        if (AnalysisActivity.currentNoteBitmap == null) {
            Toast.makeText(this, "No image bitmap available for deep vision analysis.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.VISIBLE);
        if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(false);
        if (tvVisionAuditResult != null) tvVisionAuditResult.setVisibility(View.GONE);
        if (btnSpeakVisionReport != null) btnSpeakVisionReport.setVisibility(View.GONE);

        geminiVisionService.analyzeImage(this, AnalysisActivity.currentNoteBitmap, result, new com.example.currencyguard.ai.GeminiVisionService.VisionCallback() {
            @Override
            public void onVisionReportReady(String report, boolean isCloudGemini) {
                if (isFinishing() || isDestroyed()) return;
                if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.GONE);
                if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(true);
                if (tvVisionAuditResult != null) {
                    tvVisionAuditResult.setText(report);
                    tvVisionAuditResult.setVisibility(View.VISIBLE);
                }
                if (btnSpeakVisionReport != null) {
                    btnSpeakVisionReport.setVisibility(View.VISIBLE);
                }
                lastVisionReport = report;

                if (tvVisionModelBadge != null) {
                    if (isCloudGemini) {
                        tvVisionModelBadge.setText("🌐 Live OpenRouter Forensics");
                        tvVisionModelBadge.setTextColor(Color.parseColor("#1976D2"));
                    } else {
                        tvVisionModelBadge.setText("🔒 On-Device Forensic Synthesis");
                        tvVisionModelBadge.setTextColor(Color.parseColor("#388E3C"));
                    }
                }

                if (com.example.currencyguard.ai.AiVoiceManager.isAutoSpeakEnabled(ResultActivity.this)) {
                    voiceManager.speakText("Deep AI Vision audit complete: " + report);
                }
            }

            @Override
            public void onVisionError(String error) {
                if (isFinishing() || isDestroyed()) return;
                if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.GONE);
                if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(true);
                Toast.makeText(ResultActivity.this, "Vision audit: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void speakVisionReport() {
        if (lastVisionReport == null || voiceManager == null) return;
        if (voiceManager.isSpeaking()) {
            voiceManager.stop();
            if (btnSpeakVisionReport != null) {
                btnSpeakVisionReport.setText("🔊 Speak AI Vision Report");
            }
        } else {
            if (btnSpeakVisionReport != null) {
                btnSpeakVisionReport.setText("⏹ Stop Voice");
            }
            voiceManager.speakText(lastVisionReport, new com.example.currencyguard.ai.AiVoiceManager.SpeechListener() {
                @Override
                public void onSpeechStarted() {
                    if (btnSpeakVisionReport != null) {
                        btnSpeakVisionReport.setText("⏹ Stop Voice");
                    }
                }

                @Override
                public void onSpeechCompleted() {
                    if (btnSpeakVisionReport != null) {
                        btnSpeakVisionReport.setText("🔊 Speak AI Vision Report");
                    }
                }

                @Override
                public void onSpeechError(String error) {
                    if (btnSpeakVisionReport != null) {
                        btnSpeakVisionReport.setText("🔊 Speak AI Vision Report");
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceManager != null) {
            voiceManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.stop();
        }
    }
}

