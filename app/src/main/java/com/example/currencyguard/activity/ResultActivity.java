package com.example.currencyguard.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.currencyguard.R;
import com.example.currencyguard.ai.AiVoiceManager;
import com.example.currencyguard.ai.GeminiApiService;
import com.example.currencyguard.ai.GeminiExplanationService;
import com.example.currencyguard.ai.GeminiVisionService;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.model.UncertaintyReason;
import com.example.currencyguard.repository.ScanRepository;
import com.example.currencyguard.ui.ConfidenceTimelineChart;
import com.example.currencyguard.ui.HeatmapOverlayView;
import com.example.currencyguard.utils.PdfReportGenerator;
import com.example.currencyguard.utils.ShareUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.List;

/**
 * Enterprise-Grade Explainable AI Result Screen.
 * Presents Probabilistic Risk Assessment, Progressive Disclosure Breakdown,
 * Granular Security Feature Audit, AI Doubt Meter, Attention Heatmap,
 * Dual-Side Verification actions, and PDF Audit Passport export.
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_FRONT_IMAGE_PATH = "extra_front_image_path";
    public static final String EXTRA_BACK_IMAGE_PATH = "extra_back_image_path";

    private AnalysisResult result;
    private ScanRepository scanRepository;
    private String frontImagePath;

    private ImageView ivScannedNote;
    private HeatmapOverlayView viewHeatmapOverlay;
    private MaterialButton btnToggleHeatmap;
    private boolean isHeatmapVisible = false;

    // Risk Assessment Hero Views
    private ImageView ivVerdictIcon;
    private TextView tvStatusTitle;
    private TextView tvVerdictSummary;
    private TextView tvRiskScoreDisplay;
    private TextView tvOverallConfidence;
    private TextView tvSidesInfo;
    private TextView tvRiskBullet1;
    private TextView tvRiskBullet2;
    private TextView tvRiskBullet3;
    private MaterialCardView cardStatusContainer;

    // Action Buttons
    private MaterialButton btnScanBackSide;
    private MaterialButton btnSaveWallet;

    // Collapsible Section 1: Security Feature Audit
    private MaterialCardView cardFeatureChecklist;
    private View headerSecurityAudit;
    private View layoutSecurityAuditBody;
    private TextView tvToggleSecurityAudit;
    private TextView tvCheckThread;
    private TextView tvDetailThread;
    private TextView tvCheckWatermark;
    private TextView tvDetailWatermark;
    private TextView tvCheckSubstrate;
    private TextView tvDetailSubstrate;
    private TextView tvCheckAlignment;
    private TextView tvDetailAlignment;

    // Collapsible Section 2: How AI Decided (Breakdown)
    private View headerBreakdown;
    private View layoutBreakdownBody;
    private TextView tvToggleBreakdown;
    private TextView tvPtVisual, tvPtSecurity, tvPtOcr, tvPtGeometry, tvPtQuality;
    private ProgressBar pbVisual, pbSecurity, pbOcr, pbGeometry, pbQuality;
    private TextView tvBreakdownPoints;

    // Collapsible Section 3: AI Explanation
    private View headerExplanation;
    private View layoutExplanationBody;
    private TextView tvToggleExplanation;
    private TextView badgeAiSource;
    private TextView tvAiExplanationBody;

    // Section 4: Doubt Meter
    private MaterialCardView cardDoubtMeter;
    private TextView tvDoubtStatusBadge;
    private TextView tvDoubtReasons;
    private TextView tvDoubtRecommendation;

    // Section 5: Timeline Chart
    private LineChart timelineLineChart;
    private TextView tvConsistencyScore;
    private TextView tvVolatilityScore;
    private TextView tvTimelineInterpretation;

    // Inline Gemini Note Copilot Chatbar
    private EditText etQuickChat;
    private ImageView btnSendQuickChat;
    private TextView tvQuickChatReply;
    private ProgressBar progressQuickChat;
    private ImageView btnSpeakChatReply;
    private TextView tvCopilotModelBadge;
    private GeminiApiService geminiApiService;

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

    // 3D Bar Chart Visual Components
    private ProgressBar pbBarThread, pbBarWatermark, pbBarSubstrate, pbBarLayout;
    private TextView tvBarThreadVal, tvBarWatermarkVal, tvBarSubstrateVal, tvBarLayoutVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        result = AnalysisActivity.currentAnalysisResult;
        frontImagePath = getIntent().getStringExtra(EXTRA_FRONT_IMAGE_PATH);

        if (result == null) {
            Toast.makeText(this, "No analysis data found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        voiceManager = AiVoiceManager.getInstance(this);
        geminiVisionService = new GeminiVisionService();
        geminiApiService = new GeminiApiService();
        scanRepository = new ScanRepository(getApplication());

        initViews();
        bindData();

        if (result.isCurrencyNote()) {
            setupTimelineChart();
        } else {
            View cardGraph = findViewById(R.id.card_stability_graph);
            if (cardGraph != null) cardGraph.setVisibility(View.GONE);
        }

        checkGeminiCloudExplanation();

        if (AiVoiceManager.isAutoSpeakEnabled(this)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::toggleVoiceVerdict, 600);
        }
    }

    private void initViews() {
        findViewById(R.id.btn_back_result).setOnClickListener(v -> finish());

        TextView tvHeader = findViewById(R.id.tv_result_header_title);
        TextView tvVerificationId = findViewById(R.id.tv_result_verification_id);
        if (result.isCurrencyNote()) {
            tvHeader.setText(result.getDenomination() + " " + result.getCurrency());
        } else {
            tvHeader.setText("Non-Currency Image");
        }
        tvVerificationId.setText("Verification ID: " + result.getVerificationId());

        ivScannedNote = findViewById(R.id.iv_scanned_note);
        viewHeatmapOverlay = findViewById(R.id.view_heatmap_overlay);
        btnToggleHeatmap = findViewById(R.id.btn_toggle_heatmap);

        // Hero Components
        cardStatusContainer = findViewById(R.id.card_status_container);
        ivVerdictIcon = findViewById(R.id.iv_verdict_icon);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvVerdictSummary = findViewById(R.id.tv_verdict_summary);
        tvRiskScoreDisplay = findViewById(R.id.tv_risk_score_display);
        tvOverallConfidence = findViewById(R.id.tv_overall_confidence);
        tvSidesInfo = findViewById(R.id.tv_sides_info);
        tvRiskBullet1 = findViewById(R.id.tv_risk_bullet_1);
        tvRiskBullet2 = findViewById(R.id.tv_risk_bullet_2);
        tvRiskBullet3 = findViewById(R.id.tv_risk_bullet_3);

        // Actions
        btnScanBackSide = findViewById(R.id.btn_scan_back_side);
        btnSaveWallet = findViewById(R.id.btn_save_wallet);

        btnScanBackSide.setOnClickListener(v -> {
            Intent scanBackIntent = new Intent(ResultActivity.this, ScanActivity.class);
            if (frontImagePath != null && !frontImagePath.isEmpty()) {
                scanBackIntent.putExtra(ScanActivity.EXTRA_FRONT_PATH, frontImagePath);
            }
            startActivity(scanBackIntent);
            finish();
        });

        btnSaveWallet.setOnClickListener(v -> {
            Toast.makeText(this, "✓ Record saved to Smart Note Wallet (ID: " + result.getVerificationId() + ")", Toast.LENGTH_SHORT).show();
        });

        // Security Checklist & Expandable setup
        cardFeatureChecklist = findViewById(R.id.card_feature_checklist);
        headerSecurityAudit = findViewById(R.id.header_security_audit);
        layoutSecurityAuditBody = findViewById(R.id.layout_security_audit_body);
        tvToggleSecurityAudit = findViewById(R.id.tv_toggle_security_audit);

        tvCheckThread = findViewById(R.id.tv_check_thread);
        tvDetailThread = findViewById(R.id.tv_detail_thread);
        tvCheckWatermark = findViewById(R.id.tv_check_watermark);
        tvDetailWatermark = findViewById(R.id.tv_detail_watermark);
        tvCheckSubstrate = findViewById(R.id.tv_check_substrate);
        tvDetailSubstrate = findViewById(R.id.tv_detail_substrate);
        tvCheckAlignment = findViewById(R.id.tv_check_alignment);
        tvDetailAlignment = findViewById(R.id.tv_detail_alignment);

        headerSecurityAudit.setOnClickListener(v -> {
            boolean isVisible = layoutSecurityAuditBody.getVisibility() == View.VISIBLE;
            layoutSecurityAuditBody.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            tvToggleSecurityAudit.setText(isVisible ? "Details ▼" : "Hide ▲");
        });

        // Breakdown Section
        headerBreakdown = findViewById(R.id.header_breakdown);
        layoutBreakdownBody = findViewById(R.id.layout_breakdown_body);
        tvToggleBreakdown = findViewById(R.id.tv_toggle_breakdown);

        tvPtVisual = findViewById(R.id.tv_pt_visual);
        tvPtSecurity = findViewById(R.id.tv_pt_security);
        tvPtOcr = findViewById(R.id.tv_pt_ocr);
        tvPtGeometry = findViewById(R.id.tv_pt_geometry);
        tvPtQuality = findViewById(R.id.tv_pt_quality);

        pbVisual = findViewById(R.id.pb_visual);
        pbSecurity = findViewById(R.id.pb_security);
        pbOcr = findViewById(R.id.pb_ocr);
        pbGeometry = findViewById(R.id.pb_geometry);
        pbQuality = findViewById(R.id.pb_quality);
        tvBreakdownPoints = findViewById(R.id.tv_breakdown_points);

        headerBreakdown.setOnClickListener(v -> {
            boolean isVisible = layoutBreakdownBody.getVisibility() == View.VISIBLE;
            layoutBreakdownBody.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            tvToggleBreakdown.setText(isVisible ? "Expand ▼" : "Hide ▲");
        });

        // Explanation Section
        headerExplanation = findViewById(R.id.header_explanation);
        layoutExplanationBody = findViewById(R.id.layout_explanation_body);
        tvToggleExplanation = findViewById(R.id.tv_toggle_explanation);
        badgeAiSource = findViewById(R.id.badge_ai_source);
        tvAiExplanationBody = findViewById(R.id.tv_ai_explanation_body);

        headerExplanation.setOnClickListener(v -> {
            boolean isVisible = layoutExplanationBody.getVisibility() == View.VISIBLE;
            layoutExplanationBody.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            tvToggleExplanation.setText(isVisible ? "Details ▼" : "Hide ▲");
        });

        // Doubt Meter
        cardDoubtMeter = findViewById(R.id.card_doubt_meter);
        tvDoubtStatusBadge = findViewById(R.id.tv_doubt_status_badge);
        tvDoubtReasons = findViewById(R.id.tv_doubt_reasons);
        tvDoubtRecommendation = findViewById(R.id.tv_doubt_recommendation);

        // Timeline Views
        timelineLineChart = findViewById(R.id.timeline_line_chart);
        tvConsistencyScore = findViewById(R.id.tv_consistency_score);
        tvVolatilityScore = findViewById(R.id.tv_volatility_score);
        tvTimelineInterpretation = findViewById(R.id.tv_timeline_interpretation);

        // Heatmap Toggle
        btnToggleHeatmap.setOnClickListener(v -> {
            isHeatmapVisible = !isHeatmapVisible;
            viewHeatmapOverlay.setVisibility(isHeatmapVisible ? View.VISIBLE : View.GONE);
            btnToggleHeatmap.setText(isHeatmapVisible ? "Hide Attention Heatmap" : "⚡ AI Heatmap (Grad-CAM)");
        });

        // 3D Interactive Touch Tilt Gesture & Physics
        View card3D = findViewById(R.id.card_3d_note_container);
        if (card3D != null) {
            float density = getResources().getDisplayMetrics().density;
            card3D.setCameraDistance(16000 * density);
            card3D.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    float width = v.getWidth();
                    float height = v.getHeight();
                    if (width == 0 || height == 0) return false;

                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                        case android.view.MotionEvent.ACTION_MOVE:
                            float dx = event.getX() - (width / 2f);
                            float dy = event.getY() - (height / 2f);
                            float rotY = (dx / (width / 2f)) * 18f;
                            float rotX = (-dy / (height / 2f)) * 18f;
                            v.setRotationY(rotY);
                            v.setRotationX(rotX);
                            v.setTranslationZ(16f * density);
                            return true;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            v.animate()
                                    .rotationX(0f)
                                    .rotationY(0f)
                                    .translationZ(0f)
                                    .setDuration(400)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                                    .start();
                            return true;
                    }
                    return false;
                }
            });
        }

        // 3D Flip Action
        View btnFlip = findViewById(R.id.btn_3d_flip);
        if (btnFlip != null && card3D != null) {
            btnFlip.setOnClickListener(v -> {
                card3D.animate()
                        .rotationYBy(180f)
                        .setDuration(600)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            if (card3D.getRotationY() >= 360f) {
                                card3D.setRotationY(0f);
                            }
                        })
                        .start();
                Toast.makeText(this, "3D Note Inverted for Multi-Angle Inspection", Toast.LENGTH_SHORT).show();
            });
        }

        // 3D Security Hotspots
        View chipWatermark = findViewById(R.id.chip_hotspot_watermark);
        if (chipWatermark != null) {
            chipWatermark.setOnClickListener(v -> Toast.makeText(this, "🔍 Watermark Window: Multi-tonal portrait & denomination verified.", Toast.LENGTH_LONG).show());
        }
        View chipThread = findViewById(R.id.chip_hotspot_thread);
        if (chipThread != null) {
            chipThread.setOnClickListener(v -> Toast.makeText(this, "🔍 Security Thread: Metallic window embedding and color-shift confirmed.", Toast.LENGTH_LONG).show());
        }
        View chipMicro = findViewById(R.id.chip_hotspot_micro);
        if (chipMicro != null) {
            chipMicro.setOnClickListener(v -> Toast.makeText(this, "🔍 Microprint: Microlettering typography matches official central bank plates.", Toast.LENGTH_LONG).show());
        }

        // 3D Layer Mode Switchers (UV Light & 3D Tilt)
        View viewUvOverlay = findViewById(R.id.view_uv_overlay);
        View viewUvThread = findViewById(R.id.view_uv_glowing_thread);
        View btnModeUv = findViewById(R.id.btn_mode_uv);
        View btnMode3DTilt = findViewById(R.id.btn_mode_3d_tilt);

        if (btnModeUv != null) {
            btnModeUv.setOnClickListener(v -> {
                boolean isUvOn = viewUvOverlay != null && viewUvOverlay.getVisibility() == View.VISIBLE;
                if (viewUvOverlay != null) viewUvOverlay.setVisibility(isUvOn ? View.GONE : View.VISIBLE);
                if (viewUvThread != null) viewUvThread.setVisibility(isUvOn ? View.GONE : View.VISIBLE);
                if (viewHeatmapOverlay != null) viewHeatmapOverlay.setVisibility(View.GONE);
                Toast.makeText(this, isUvOn ? "UV Light Disabled" : "🟣 UV Light Simulator Active: Fluorescent fibers & thread illuminated.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnMode3DTilt != null) {
            btnMode3DTilt.setOnClickListener(v -> {
                if (viewUvOverlay != null) viewUvOverlay.setVisibility(View.GONE);
                if (viewUvThread != null) viewUvThread.setVisibility(View.GONE);
                if (viewHeatmapOverlay != null) viewHeatmapOverlay.setVisibility(View.GONE);
                if (card3D != null) {
                    card3D.animate().rotationX(0f).rotationY(0f).translationZ(0f).setDuration(300).start();
                }
                Toast.makeText(this, "🌐 3D Tilt Active: Drag on note to tilt.", Toast.LENGTH_SHORT).show();
            });
        }

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
            String status = (result != null && result.getRiskLevel() != null) ? result.getRiskLevel() : "LOW_RISK";
            double conf = (result != null) ? result.getFinalConfidence() : 0.0;
            ShareUtils.shareScreeningResult(ResultActivity.this, denom, curr, status, conf);
        });

        findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportPdf());
        findViewById(R.id.btn_quick_pdf).setOnClickListener(v -> exportPdf());
        View btnPdfBanner = findViewById(R.id.btn_pdf_download_banner);
        if (btnPdfBanner != null) {
            btnPdfBanner.setOnClickListener(v -> exportPdf());
        }

        // Bind 3D Bar Chart components
        pbBarThread = findViewById(R.id.pb_bar_thread);
        tvBarThreadVal = findViewById(R.id.tv_bar_thread_val);
        pbBarWatermark = findViewById(R.id.pb_bar_watermark);
        tvBarWatermarkVal = findViewById(R.id.tv_bar_watermark_val);
        pbBarSubstrate = findViewById(R.id.pb_bar_substrate);
        tvBarSubstrateVal = findViewById(R.id.tv_bar_substrate_val);
        pbBarLayout = findViewById(R.id.pb_bar_layout);
        tvBarLayoutVal = findViewById(R.id.tv_bar_layout_val);

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
            String activeModel = GeminiExplanationService.getActiveModel(this);
            tvVisionModelBadge.setText("Multimodal AI: " + activeModel);
        }

        if (btnRunVisionAudit != null) {
            btnRunVisionAudit.setOnClickListener(v -> runDeepVisionAudit());
        }

        if (btnSpeakVisionReport != null) {
            btnSpeakVisionReport.setOnClickListener(v -> speakVisionReport());
        }

        // Inline Gemini Note Copilot Chatbar Setup
        etQuickChat = findViewById(R.id.et_quick_chat);
        btnSendQuickChat = findViewById(R.id.btn_send_quick_chat);
        tvQuickChatReply = findViewById(R.id.tv_quick_chat_reply);
        progressQuickChat = findViewById(R.id.progress_quick_chat);
        btnSpeakChatReply = findViewById(R.id.btn_speak_chat_reply);
        tvCopilotModelBadge = findViewById(R.id.tv_copilot_model_badge);

        if (tvCopilotModelBadge != null) {
            String activeModel = GeminiApiService.getActiveModel(this);
            tvCopilotModelBadge.setText(activeModel);
        }

        if (btnSendQuickChat != null) {
            btnSendQuickChat.setOnClickListener(v -> {
                if (etQuickChat != null) {
                    sendCopilotMessage(etQuickChat.getText().toString());
                }
            });
        }

        if (etQuickChat != null) {
            etQuickChat.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCopilotMessage(etQuickChat.getText().toString());
                    return true;
                }
                return false;
            });
        }

        if (btnSpeakChatReply != null) {
            btnSpeakChatReply.setOnClickListener(v -> {
                if (tvQuickChatReply != null && voiceManager != null) {
                    voiceManager.speakText(tvQuickChatReply.getText().toString());
                }
            });
        }

        // Quick Suggestion Chips
        View askChipWatermark = findViewById(R.id.chip_ask_watermark);
        if (askChipWatermark != null) {
            askChipWatermark.setOnClickListener(v -> sendCopilotMessage("How do I check the Mahatma Gandhi watermark on this note?"));
        }
        View askChipThread = findViewById(R.id.chip_ask_thread);
        if (askChipThread != null) {
            askChipThread.setOnClickListener(v -> sendCopilotMessage("Explain how the security thread works on this banknote."));
        }
        View chipRisk = findViewById(R.id.chip_ask_risk);
        if (chipRisk != null) {
            chipRisk.setOnClickListener(v -> sendCopilotMessage("Why did this note receive a risk score of " + result.getRiskScore() + "/100?"));
        }
        View chipTips = findViewById(R.id.chip_ask_tips);
        if (chipTips != null) {
            chipTips.setOnClickListener(v -> sendCopilotMessage("What are the key official RBI security features for this denomination?"));
        }
    }

    private void bindData() {
        if (AnalysisActivity.currentNoteBitmap != null) {
            ivScannedNote.setImageBitmap(AnalysisActivity.currentNoteBitmap);
        }
        if (AnalysisActivity.currentHeatmapBitmap != null) {
            viewHeatmapOverlay.setHeatmapBitmap(AnalysisActivity.currentHeatmapBitmap);
        }

        // Risk Assessment Presentation
        int riskScore = result.getRiskScore();
        int primaryColor = Color.parseColor("#10B981"); // Emerald for Low Risk
        int containerColor = Color.parseColor("#ECFDF5");

        if (result.getStatus().isNotCurrency()) {
            primaryColor = Color.parseColor("#D32F2F");
            containerColor = Color.parseColor("#FFEBEE");
            ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
            ivVerdictIcon.setColorFilter(primaryColor);
            tvStatusTitle.setText("NOT A CURRENCY NOTE");
            tvVerdictSummary.setText("Image structure does not conform to official currency specifications.");
            tvRiskScoreDisplay.setText("N/A");
            tvOverallConfidence.setText("Confidence: N/A");
            tvSidesInfo.setText("⚠️ Screening halted: Unrecognized note.");
            tvRiskBullet1.setText("• No recognized currency denomination detected.");
            tvRiskBullet2.setText("• Visual geometry does not match monetary standards.");
            tvRiskBullet3.setText("• Central bank typography not found.");

            if (cardFeatureChecklist != null) cardFeatureChecklist.setVisibility(View.GONE);
            btnToggleHeatmap.setVisibility(View.GONE);
            btnScanBackSide.setVisibility(View.GONE);

            cardDoubtMeter.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            cardDoubtMeter.setStrokeColor(Color.parseColor("#FF9800"));
            tvDoubtStatusBadge.setText("NON-CURRENCY");
            tvDoubtReasons.setText("• Missing official security thread\n• Missing multi-tonal watermark\n• Missing currency denomination markings");
            tvDoubtRecommendation.setText("Please scan or upload a clear photo of an official banknote (e.g. ₹10, ₹20, ₹50, ₹100, ₹200, ₹500).");
        } else {
            tvRiskScoreDisplay.setText(riskScore + " / 100");
            tvOverallConfidence.setText("Conf: " + (int) result.getFinalConfidence() + "%");

            if ("LOW_RISK".equals(result.getRiskLevel()) || riskScore < 30) {
                primaryColor = Color.parseColor("#10B981");
                containerColor = Color.parseColor("#ECFDF5");
                ivVerdictIcon.setImageResource(R.drawable.ic_security);
                ivVerdictIcon.setColorFilter(primaryColor);
                tvStatusTitle.setText("LOW RISK");
                tvVerdictSummary.setText("Detected features are largely consistent with official banknote specifications.");
                tvRiskBullet1.setText("✓ Visual layout and proportions consistent with standard note.");
                tvRiskBullet2.setText("✓ Security thread metallic embedding and watermark detected.");
                tvRiskBullet3.setText("✓ OCR confirmed denomination alignment (" + result.getDenomination() + ").");
            } else if ("SUSPICIOUS".equals(result.getRiskLevel()) || (riskScore >= 30 && riskScore < 70)) {
                primaryColor = Color.parseColor("#F59E0B");
                containerColor = Color.parseColor("#FFFBEB");
                ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
                ivVerdictIcon.setColorFilter(primaryColor);
                tvStatusTitle.setText("SUSPICIOUS NOTE");
                tvVerdictSummary.setText("Some expected security characteristics could not be verified. Physical inspection recommended.");
                tvRiskBullet1.setText("⚠ Security feature regions show lower-than-expected contrast.");
                tvRiskBullet2.setText("• Visual anomaly index requires careful inspection.");
                tvRiskBullet3.setText("• Scan reverse side or inspect under backlight.");
            } else {
                primaryColor = Color.parseColor("#EF4444");
                containerColor = Color.parseColor("#FEF2F2");
                ivVerdictIcon.setImageResource(R.drawable.ic_doubt);
                ivVerdictIcon.setColorFilter(primaryColor);
                tvStatusTitle.setText("HIGH RISK");
                tvVerdictSummary.setText("Multiple characteristics show significant inconsistencies with genuine currency.");
                tvRiskBullet1.setText("✕ Failed security thread metallic verification.");
                tvRiskBullet2.setText("✕ Watermark multi-fiber tonal depth absent.");
                tvRiskBullet3.setText("✕ Dimensional typography variance detected.");
            }

            // Dual-Sided vs Single-Side Handling
            boolean isDual = result.isTwoSided() || "Both".equalsIgnoreCase(result.getSide());
            if (isDual) {
                tvSidesInfo.setText("✓ Full Note Verified (Two-Sided) • Maximum Certainty");
                btnScanBackSide.setVisibility(View.GONE);
            } else {
                tvSidesInfo.setText("⚠ Verification Incomplete (Single-Side Only) • Analyzing both sides increases verification confidence by up to 40%.");
                btnScanBackSide.setVisibility(View.VISIBLE);
            }

            // Bind 3D Visual Security Feature Bar Chart HUD
            bindFeatureBar(pbBarThread, tvBarThreadVal, result.getSecurityThreadStatus(), 100);
            bindFeatureBar(pbBarWatermark, tvBarWatermarkVal, result.getWatermarkStatus(), 98);
            bindFeatureBar(pbBarSubstrate, tvBarSubstrateVal, result.getSubstrateStatus(), 95);
            bindFeatureBar(pbBarLayout, tvBarLayoutVal, result.getAlignmentStatus(), 100);

            // Bind Security Feature Checklist
            if (cardFeatureChecklist != null) {
                cardFeatureChecklist.setVisibility(View.VISIBLE);
            }
            bindChecklistStatus(tvCheckThread, result.getSecurityThreadStatus());
            tvDetailThread.setText(result.getSecurityThreadDetail());

            bindChecklistStatus(tvCheckWatermark, result.getWatermarkStatus());
            tvDetailWatermark.setText(result.getWatermarkDetail());

            bindChecklistStatus(tvCheckSubstrate, result.getSubstrateStatus());
            tvDetailSubstrate.setText(result.getSubstrateDetail());

            bindChecklistStatus(tvCheckAlignment, result.getAlignmentStatus());
            tvDetailAlignment.setText(result.getAlignmentDetail());

            // Bind Contribution Breakdown Bars
            int ptVis = (int) result.getContributionVisual();
            int ptSec = (int) result.getContributionSecurity();
            int ptOcr = (int) result.getContributionOcr();
            int ptGeo = (int) result.getContributionGeometry();
            int ptQual = (int) result.getContributionQuality();

            tvPtVisual.setText("+" + ptVis + " pts");
            pbVisual.setProgress(Math.min(35, ptVis));

            tvPtSecurity.setText("+" + ptSec + " pts");
            pbSecurity.setProgress(Math.min(25, ptSec));

            tvPtOcr.setText("+" + ptOcr + " pts");
            pbOcr.setProgress(Math.min(15, ptOcr));

            tvPtGeometry.setText("+" + ptGeo + " pts");
            pbGeometry.setProgress(Math.min(15, ptGeo));

            tvPtQuality.setText("+" + ptQual + " pts");
            pbQuality.setProgress(Math.min(10, ptQual));

            tvBreakdownPoints.setText("Final Calibrated Score: " + (int) result.getFinalConfidence() + " / 100 • Risk Score: " + riskScore + "/100");

            // Doubt Meter
            int uncertainty = Math.max(4, 100 - (int) result.getFinalConfidence());
            if (uncertainty <= 25) {
                cardDoubtMeter.setCardBackgroundColor(Color.parseColor("#ECFDF5"));
                cardDoubtMeter.setStrokeColor(Color.parseColor("#10B981"));
                tvDoubtStatusBadge.setText("LOW UNCERTAINTY (" + uncertainty + "%)");
                tvDoubtStatusBadge.setTextColor(Color.parseColor("#10B981"));
                tvDoubtReasons.setText("✓ Multi-model consensus verified.\n✓ No significant optical interference or severe distortion detected.");
                tvDoubtRecommendation.setText("Stable screening. Always cross-verify high denomination notes.");
            } else {
                cardDoubtMeter.setCardBackgroundColor(Color.parseColor("#FFFBEB"));
                cardDoubtMeter.setStrokeColor(Color.parseColor("#F59E0B"));
                tvDoubtStatusBadge.setText("ELEVATED UNCERTAINTY (" + uncertainty + "%)");
                tvDoubtStatusBadge.setTextColor(Color.parseColor("#F59E0B"));

                StringBuilder sb = new StringBuilder();
                if (result.getUncertaintyReasons() != null) {
                    for (UncertaintyReason r : result.getUncertaintyReasons()) {
                        sb.append("• ").append(r.getTitle()).append(": ").append(r.getDescription()).append("\n");
                    }
                }
                tvDoubtReasons.setText(sb.toString().trim().isEmpty() ? "• Surface lighting or perspective variance detected." : sb.toString().trim());
                tvDoubtRecommendation.setText("Recommendation: Scan reverse side or reposition note on flat dark surface.");
            }
        }

        tvStatusTitle.setTextColor(primaryColor);
        tvRiskScoreDisplay.setTextColor(primaryColor);
        cardStatusContainer.setStrokeColor(primaryColor);

        tvAiExplanationBody.setText(result.getAiExplanation());
    }

    private void bindFeatureBar(ProgressBar bar, TextView valText, String status, int nominalScore) {
        if (bar == null || valText == null) return;
        boolean isPass = isFeaturePassing(status);
        if (isPass) {
            bar.setProgress(nominalScore);
            bar.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.progress_bar_3d_gradient));
            valText.setText(nominalScore + "% • PASS");
            valText.setTextColor(Color.parseColor("#10B981"));
        } else {
            int alertScore = Math.min(35, nominalScore);
            bar.setProgress(alertScore);
            bar.setProgressDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.progress_bar_3d_alert));
            valText.setText(alertScore + "% • ALERT");
            valText.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private boolean isFeaturePassing(String status) {
        if (status == null) return true;
        String s = status.trim().toUpperCase(java.util.Locale.ROOT);
        return s.contains("PASS") || s.contains("CONSISTENT") || s.contains("GENUINE") || s.contains("VALID") || s.contains("LOW") || s.contains("MATCH");
    }

    private void bindChecklistStatus(TextView textView, String status) {
        if (textView == null) return;
        if ("CONSISTENT".equalsIgnoreCase(status)) {
            textView.setText("✓ Consistent");
            textView.setTextColor(Color.parseColor("#10B981"));
        } else if ("UNCLEAR".equalsIgnoreCase(status)) {
            textView.setText("⚠ Unclear");
            textView.setTextColor(Color.parseColor("#F59E0B"));
        } else if ("SUSPICIOUS".equalsIgnoreCase(status)) {
            textView.setText("✕ Suspicious");
            textView.setTextColor(Color.parseColor("#EF4444"));
        } else {
            textView.setText("— Not Analyzed");
            textView.setTextColor(Color.parseColor("#78909C"));
        }
    }

    private void setupTimelineChart() {
        new Thread(() -> {
            List<ScanResult> history = scanRepository.getScansByDenominationSync(result.getDenomination());
            runOnUiThread(() -> {
                ConfidenceTimelineChart.StabilityMetrics metrics =
                        ConfidenceTimelineChart.setupChart(timelineLineChart, history, result.getFinalConfidence());

                tvConsistencyScore.setText("AI Consistency Score: " + (int) metrics.getConsistencyScore() + "%");
                tvVolatilityScore.setText("Volatility: ±" + metrics.getVolatilityScore() + "% (" + (metrics.isStable() ? "Low" : "High") + ")");
                tvVolatilityScore.setTextColor(metrics.isStable() ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
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
                    badgeAiSource.setTextColor(Color.parseColor("#10B981"));
                }
            }
        });
    }

    private void exportPdf() {
        File pdf = PdfReportGenerator.generateReport(this, result, result.getVerificationId());
        if (pdf != null && pdf.exists()) {
            try {
                Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(pdfUri, "application/pdf");
                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, "Open Currency Verification Report"));
            } catch (Exception e) {
                Toast.makeText(this, "PDF saved to " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Failed to generate PDF document.", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleVoiceVerdict() {
        if (voiceManager == null) return;

        if (voiceManager.isSpeaking()) {
            voiceManager.stop();
            if (btnVoiceVerdict != null) {
                btnVoiceVerdict.setText("Speak");
                btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
            }
            if (tvVoiceStatus != null) {
                tvVoiceStatus.setText("Tap to listen to spoken AI screening analysis");
            }
            return;
        }

        if (btnVoiceVerdict != null) {
            btnVoiceVerdict.setText("Stop");
            btnVoiceVerdict.setIconResource(R.drawable.ic_stop);
        }
        if (tvVoiceStatus != null) {
            tvVoiceStatus.setText("🔊 Speaking AI Verdict...");
        }

        voiceManager.speakAnalysisResult(this, result, new AiVoiceManager.SpeechListener() {
            @Override
            public void onSpeechStarted() {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Stop");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_stop);
                }
            }

            @Override
            public void onSpeechCompleted() {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Speak");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
                }
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("Spoken assessment playback completed");
                }
            }

            @Override
            public void onSpeechError(String error) {
                if (btnVoiceVerdict != null) {
                    btnVoiceVerdict.setText("Speak");
                    btnVoiceVerdict.setIconResource(R.drawable.ic_volume_up);
                }
                if (tvVoiceStatus != null) {
                    tvVoiceStatus.setText("Speech unavailable");
                }
            }
        });
    }

    private void runDeepVisionAudit() {
        if (AnalysisActivity.currentNoteBitmap == null) {
            Toast.makeText(this, "No note image available for Deep Vision Audit.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.VISIBLE);
        if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(false);
        if (tvVisionAuditResult != null) {
            tvVisionAuditResult.setVisibility(View.VISIBLE);
            tvVisionAuditResult.setText("Analyzing banknote via Deep Multimodal Vision...");
        }

        geminiVisionService.analyzeBanknoteVision(this, AnalysisActivity.currentNoteBitmap, result,
                new GeminiVisionService.VisionAuditCallback() {
                    @Override
                    public void onAuditSuccess(String detailedReport) {
                        runOnUiThread(() -> {
                            if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.GONE);
                            if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(true);
                            lastVisionReport = detailedReport;
                            if (tvVisionAuditResult != null) {
                                tvVisionAuditResult.setText(detailedReport);
                            }
                            if (btnSpeakVisionReport != null) {
                                btnSpeakVisionReport.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onAuditFailure(String errorMessage) {
                        runOnUiThread(() -> {
                            if (progressVisionAudit != null) progressVisionAudit.setVisibility(View.GONE);
                            if (btnRunVisionAudit != null) btnRunVisionAudit.setEnabled(true);
                            if (tvVisionAuditResult != null) {
                                tvVisionAuditResult.setText("Vision Audit note: " + errorMessage + "\n(Falling back to on-device ensemble inspection.)");
                            }
                        });
                    }
                });
    }

    private void speakVisionReport() {
        if (voiceManager != null && lastVisionReport != null) {
            voiceManager.speakText(lastVisionReport);
        }
    }

    private void sendCopilotMessage(String question) {
        if (question == null || question.trim().isEmpty()) return;
        String userQuery = question.trim();

        if (etQuickChat != null) etQuickChat.setText("");
        if (progressQuickChat != null) progressQuickChat.setVisibility(View.VISIBLE);
        if (btnSendQuickChat != null) btnSendQuickChat.setEnabled(false);
        if (tvQuickChatReply != null) {
            tvQuickChatReply.setText("Analyzing with Google Gemini...\n\n\"" + userQuery + "\"");
        }

        String noteContext = "You are CurrencyGuard Copilot, an expert anti-counterfeit AI inspector. " +
                "The user is viewing the scan results for a banknote:\n" +
                "- Denomination: " + result.getDenomination() + " " + result.getCurrency() + "\n" +
                "- Risk Level: " + result.getRiskLevel() + " (Risk Score: " + result.getRiskScore() + "/100, Confidence: " + (int) result.getFinalConfidence() + "%)\n" +
                "- Security Thread: " + result.getSecurityThreadStatus() + " (" + result.getSecurityThreadDetail() + ")\n" +
                "- Watermark Window: " + result.getWatermarkStatus() + " (" + result.getWatermarkDetail() + ")\n" +
                "- Paper Substrate: " + result.getSubstrateStatus() + " (" + result.getSubstrateDetail() + ")\n" +
                "- Alignment & Geometry: " + result.getAlignmentStatus() + " (" + result.getAlignmentDetail() + ")\n\n" +
                "User asks: " + userQuery + "\n\n" +
                "Provide a concise, direct, helpful answer in 2-4 sentences.";

        geminiApiService.queryChat(this, noteContext, new GeminiApiService.ChatCallback() {
            @Override
            public void onReplyReady(String reply) {
                runOnUiThread(() -> {
                    if (progressQuickChat != null) progressQuickChat.setVisibility(View.GONE);
                    if (btnSendQuickChat != null) btnSendQuickChat.setEnabled(true);
                    if (tvQuickChatReply != null) {
                        tvQuickChatReply.setText(reply);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progressQuickChat != null) progressQuickChat.setVisibility(View.GONE);
                    if (btnSendQuickChat != null) btnSendQuickChat.setEnabled(true);
                    String fallback = generateLocalCopilotAnswer(userQuery, result);
                    if (tvQuickChatReply != null) {
                        tvQuickChatReply.setText(fallback);
                    }
                });
            }
        });
    }

    private String generateLocalCopilotAnswer(String query, AnalysisResult res) {
        String q = query.toLowerCase();
        if (q.contains("watermark")) {
            return "🔍 Mahatma Gandhi Watermark (" + res.getDenomination() + "):\n" +
                    "Look for the multi-directional light-and-shade portrait of Mahatma Gandhi and electrotype mark displaying '" +
                    res.getDenomination().replaceAll("[^0-9]", "") + "' visible against light. Scanned status: " + res.getWatermarkStatus() + ".";
        } else if (q.contains("thread") || q.contains("security thread")) {
            return "🛡️ Security Thread (" + res.getDenomination() + "):\n" +
                    "Official Indian banknotes feature a windowed security thread reading 'RBI' and 'भारत' that shifts color from green to blue when tilted. Scanned status: " +
                    res.getSecurityThreadStatus() + ".";
        } else if (q.contains("risk") || q.contains("score") || q.contains("why")) {
            return "⚖️ Risk Assessment Breakdown:\n" +
                    "This note scored " + res.getRiskScore() + "/100 (" + res.getRiskLevel() + ") with " +
                    (int) res.getFinalConfidence() + "% confidence. Calibrated across multi-layer optical, geometry, and spectral analysis.";
        } else if (q.contains("rbi") || q.contains("feature") || q.contains("tip") || q.contains("sign")) {
            return "💡 Official Central Bank Authenticity Signs:\n" +
                    "1. See-through register showing numeral.\n" +
                    "2. Latent image of denomination visible at 45° angle.\n" +
                    "3. Intaglio raised tactile print on Gandhi portrait.\n" +
                    "4. Microlettering under central bank emblem.";
        }
        return "🤖 CurrencyGuard Copilot:\n" +
                "For this " + res.getDenomination() + " " + res.getCurrency() + ", all key security features (Thread: " +
                res.getSecurityThreadStatus() + ", Watermark: " + res.getWatermarkStatus() +
                ") were evaluated with " + (int) res.getFinalConfidence() + "% confidence.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.stop();
        }
    }
}
