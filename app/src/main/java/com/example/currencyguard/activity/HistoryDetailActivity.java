package com.example.currencyguard.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.currencyguard.R;
import com.example.currencyguard.ai.ConfidenceEngine;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.repository.ScanRepository;
import com.example.currencyguard.utils.ImageUtils;
import com.example.currencyguard.utils.PdfReportGenerator;
import com.example.currencyguard.utils.ShareUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Currency Verification & Audit Passport Record View Activity.
 * Displays saved historical screening records with full metric breakdowns,
 * risk scores, dual-sided status, re-scan actions, and PDF export.
 */
public class HistoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_ID = "extra_scan_id";
    private ScanRepository scanRepository;
    private ScanResult scanResult;

    private TextView tvPassportId;
    private TextView tvPassportSidesBadge;
    private TextView tvPassportDate;
    private TextView tvPassportDenom;
    private TextView tvPassportSerial;
    private TextView tvPassportStatus;
    private TextView tvPassportRiskScore;
    private TextView tvPassportMetrics;
    private TextView tvPassportExplanation;
    private ImageView ivFront;
    private ImageView ivBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        long scanId = getIntent().getLongExtra(EXTRA_SCAN_ID, -1);
        if (scanId == -1) {
            Toast.makeText(this, "Invalid record ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scanRepository = new ScanRepository(getApplication());

        findViewById(R.id.btn_back_passport).setOnClickListener(v -> finish());
        findViewById(R.id.btn_export_passport_pdf).setOnClickListener(v -> exportPdf());

        tvPassportId = findViewById(R.id.tv_passport_id);
        tvPassportSidesBadge = findViewById(R.id.tv_passport_sides_badge);
        tvPassportDate = findViewById(R.id.tv_passport_date);
        tvPassportDenom = findViewById(R.id.tv_passport_denom);
        tvPassportSerial = findViewById(R.id.tv_passport_serial);
        tvPassportStatus = findViewById(R.id.tv_passport_status);
        tvPassportRiskScore = findViewById(R.id.tv_passport_risk_score);
        tvPassportMetrics = findViewById(R.id.tv_passport_metrics);
        tvPassportExplanation = findViewById(R.id.tv_passport_explanation);
        ivFront = findViewById(R.id.iv_passport_front);
        ivBack = findViewById(R.id.iv_passport_back);

        findViewById(R.id.btn_passport_rescan).setOnClickListener(v -> {
            Intent intent = new Intent(HistoryDetailActivity.this, ScanActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_passport_share).setOnClickListener(v -> {
            if (scanResult != null) {
                String denom = scanResult.getDenomination() != null ? scanResult.getDenomination() : "Banknote";
                String curr = scanResult.getCurrency() != null ? scanResult.getCurrency() : "INR";
                String status = scanResult.getRiskLevel() != null ? scanResult.getRiskLevel() : "LOW_RISK";
                ShareUtils.shareScreeningResult(HistoryDetailActivity.this, denom, curr, status, scanResult.getConfidence());
            }
        });

        loadRecord(scanId);
    }

    private void loadRecord(long id) {
        new Thread(() -> {
            scanResult = scanRepository.getScanByIdSync(id);
            if (scanResult == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Record not found.", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            runOnUiThread(this::bindRecordData);
        }).start();
    }

    private void bindRecordData() {
        String verificationId = scanResult.getVerificationId();
        if (verificationId == null || verificationId.isEmpty()) {
            verificationId = "CG-" + (scanResult.getId() * 1000 + 1234);
        }
        tvPassportId.setText("Verification ID: " + verificationId);

        String dateStr = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US).format(new Date(scanResult.getTimestamp()));
        tvPassportDate.setText("Scan Date: " + dateStr);

        tvPassportDenom.setText(scanResult.getDenomination() + " (" + scanResult.getCurrency() + ")");

        // Serial Number
        String serial = scanResult.getSerialNumber();
        if (serial != null && !serial.isEmpty() && !"Unclear".equalsIgnoreCase(serial)) {
            tvPassportSerial.setText("Serial Number: " + serial);
        } else {
            tvPassportSerial.setText("Serial Number: Not Detected");
        }

        // Dual-Sided vs Single-Side Tag
        boolean isDual = scanResult.isDualSided() || (scanResult.getBackImagePath() != null && !scanResult.getBackImagePath().isEmpty());
        tvPassportSidesBadge.setText(isDual ? "Dual-Sided Verified" : "Single-Side Screening");

        int risk = scanResult.getRiskScore() > 0 ? scanResult.getRiskScore() : (int) Math.max(5, Math.round(100.0 - scanResult.getConfidence()));
        tvPassportRiskScore.setText("Risk: " + risk + "/100 • Conf: " + (int) scanResult.getConfidence() + "%");

        String status = scanResult.getStatus() != null ? scanResult.getStatus() : "LOW_RISK";
        int color = Color.parseColor("#00C853");
        String displayName = "LOW RISK";

        if (status.contains("NOT_A_CURRENCY") || status.contains("NOT_CURRENCY")) {
            color = Color.parseColor("#D32F2F");
            displayName = "NOT A CURRENCY NOTE";
            tvPassportRiskScore.setText("Screening Halted");
        } else if (status.contains("SUSPICIOUS") || (risk >= 30 && risk < 70)) {
            color = Color.parseColor("#FFB300");
            displayName = "SUSPICIOUS";
        } else if (status.contains("HIGH_RISK") || status.contains("FAKE") || risk >= 70) {
            color = Color.parseColor("#F44336");
            displayName = "HIGH RISK";
        } else {
            color = Color.parseColor("#00C853");
            displayName = "LOW RISK";
        }

        tvPassportStatus.setText(displayName);
        tvPassportStatus.setTextColor(color);

        // Metrics breakdown
        StringBuilder metrics = new StringBuilder();
        metrics.append("• Visual Similarity Score:   ").append((int) scanResult.getVisualScore()).append("%\n");
        metrics.append("• Security Feature Regions:  ").append((int) scanResult.getSecurityScore()).append("%\n");
        metrics.append("• OCR Text Consistency:     ").append((int) scanResult.getOcrScore()).append("%\n");
        metrics.append("• Geometry & Proportions:    ").append((int) scanResult.getGeometryScore()).append("%\n");
        metrics.append("• Input Image Quality:       ").append((int) scanResult.getImageQuality()).append("%\n");
        metrics.append("• Visual Anomaly Index:      ").append((int) scanResult.getAnomalyScore()).append(" / 100");
        tvPassportMetrics.setText(metrics.toString());

        tvPassportExplanation.setText(scanResult.getAiExplanation() != null ? scanResult.getAiExplanation() : "All primary security characteristics consistent with currency pattern.");

        // Images
        if (scanResult.getFrontImagePath() != null) {
            Bitmap frontBm = ImageUtils.loadAndCorrectOrientation(scanResult.getFrontImagePath());
            if (frontBm != null) ivFront.setImageBitmap(frontBm);
        }
        if (scanResult.getBackImagePath() != null) {
            Bitmap backBm = ImageUtils.loadAndCorrectOrientation(scanResult.getBackImagePath());
            if (backBm != null) ivBack.setImageBitmap(backBm);
        }
    }

    private void exportPdf() {
        if (scanResult == null) return;

        ConfidenceEngine engine = new ConfidenceEngine();
        boolean isDual = scanResult.isDualSided() || (scanResult.getBackImagePath() != null && !scanResult.getBackImagePath().isEmpty());
        AnalysisResult analysisResult = engine.computeFinalResult(
                scanResult.getDenomination(),
                scanResult.getCurrency(),
                scanResult.getImageQuality(),
                scanResult.getVisualScore(),
                scanResult.getSecurityScore(),
                scanResult.getOcrScore(),
                scanResult.getGeometryScore(),
                scanResult.getAnomalyScore(),
                scanResult.getSerialNumber(),
                isDual
        );
        analysisResult.setAiExplanation(scanResult.getAiExplanation());
        if (scanResult.getVerificationId() != null && !scanResult.getVerificationId().isEmpty()) {
            analysisResult.setVerificationId(scanResult.getVerificationId());
        }

        File pdf = PdfReportGenerator.generateReport(this, analysisResult, analysisResult.getVerificationId());
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
            Toast.makeText(this, "Failed to generate PDF.", Toast.LENGTH_SHORT).show();
        }
    }
}
