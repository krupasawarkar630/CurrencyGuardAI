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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Currency Passport View Activity.
 * Displays saved historical screening records with full metric breakdowns
 * and digital PDF export capabilities.
 */
public class HistoryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_ID = "extra_scan_id";
    private ScanRepository scanRepository;
    private ScanResult scanResult;

    private TextView tvPassportId;
    private TextView tvPassportDate;
    private TextView tvPassportDenom;
    private TextView tvPassportStatus;
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
        tvPassportDate = findViewById(R.id.tv_passport_date);
        tvPassportDenom = findViewById(R.id.tv_passport_denom);
        tvPassportStatus = findViewById(R.id.tv_passport_status);
        tvPassportMetrics = findViewById(R.id.tv_passport_metrics);
        tvPassportExplanation = findViewById(R.id.tv_passport_explanation);
        ivFront = findViewById(R.id.iv_passport_front);
        ivBack = findViewById(R.id.iv_passport_back);

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
        String passportId = "CG-" + (scanResult.getId() * 1000 + 1234);
        tvPassportId.setText("Passport ID: " + passportId);

        String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date(scanResult.getTimestamp()));
        tvPassportDate.setText("Scan Date: " + dateStr);

        tvPassportDenom.setText(scanResult.getDenomination() + " (" + scanResult.getCurrency() + ")");

        String status = scanResult.getStatus() != null ? scanResult.getStatus() : "LIKELY_GENUINE";
        int color = Color.parseColor("#00C853");
        String displayName = "VERDICT: REAL CURRENCY";

        if (status.contains("NOT_A_CURRENCY") || status.contains("NOT_CURRENCY")) {
            color = Color.parseColor("#D32F2F");
            displayName = "VERDICT: NOT A CURRENCY NOTE";
        } else if (status.contains("SUSPICIOUS")) {
            color = Color.parseColor("#FFB300");
            displayName = "VERDICT: SUSPICIOUS NOTE";
        } else if (status.contains("FAKE")) {
            color = Color.parseColor("#F44336");
            displayName = "VERDICT: FAKE CURRENCY";
        } else if (status.contains("UNABLE")) {
            color = Color.parseColor("#78909C");
            displayName = "VERDICT: UNABLE TO VERIFY";
        }

        if (status.contains("NOT_A_CURRENCY") || status.contains("NOT_CURRENCY")) {
            tvPassportStatus.setText(displayName);
        } else {
            tvPassportStatus.setText(displayName + " (" + (int) scanResult.getConfidence() + "%)");
        }
        tvPassportStatus.setTextColor(color);

        // Metrics breakdown
        StringBuilder metrics = new StringBuilder();
        metrics.append("• Visual Model Score:   ").append((int) scanResult.getVisualScore()).append("%\n");
        metrics.append("• Security Feature Regions: ").append((int) scanResult.getSecurityScore()).append("%\n");
        metrics.append("• OCR Consistency:      ").append((int) scanResult.getOcrScore()).append("%\n");
        metrics.append("• Geometry Alignment:   ").append((int) scanResult.getGeometryScore()).append("%\n");
        metrics.append("• Input Image Quality:  ").append((int) scanResult.getImageQuality()).append("%\n");
        metrics.append("• Visual Anomaly Score: ").append((int) scanResult.getAnomalyScore()).append(" / 100");
        tvPassportMetrics.setText(metrics.toString());

        tvPassportExplanation.setText(scanResult.getAiExplanation() != null ? scanResult.getAiExplanation() : "All primary anti-counterfeit characteristics verified.");

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
        AnalysisResult analysisResult = engine.computeFinalResult(
                scanResult.getDenomination(),
                scanResult.getCurrency(),
                scanResult.getImageQuality(),
                scanResult.getVisualScore(),
                scanResult.getSecurityScore(),
                scanResult.getOcrScore(),
                scanResult.getGeometryScore(),
                scanResult.getAnomalyScore(),
                scanResult.getDenomination(),
                scanResult.getBackImagePath() != null
        );
        analysisResult.setAiExplanation(scanResult.getAiExplanation());

        String passportId = "CG-" + (scanResult.getId() * 1000 + 1234);
        File pdf = PdfReportGenerator.generateReport(this, analysisResult, passportId);
        if (pdf != null && pdf.exists()) {
            try {
                Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(pdfUri, "application/pdf");
                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, "Open Passport Report"));
            } catch (Exception e) {
                Toast.makeText(this, "PDF saved to " + pdf.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
