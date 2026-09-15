package com.example.currencyguard.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.UncertaintyReason;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CurrencyGuard AI — Professional PDF Audit Passport & Screening Report Generator.
 * Creates an A4 digital verification passport documenting banknote risk assessment,
 * security feature audit, component contributions, and explainability notes.
 */
public class PdfReportGenerator {

    public static File generateReport(Context context, AnalysisResult result, String customId) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int y = 35;

        // 1. Professional Header Banner
        paint.setColor(Color.parseColor("#0F172A")); // Deep Trust Slate Navy
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(30, y, 565, y + 65, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText("CurrencyGuard AI — Verification & Risk Report", 45, y + 30, paint);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("Algorithmic Banknote Screening, Explainability & Audit Passport", 45, y + 48, paint);

        y += 85;

        // 2. Metadata Block
        String reportId = (customId != null && !customId.isEmpty()) ? customId : "CG-" + (System.currentTimeMillis() % 1000000);
        String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date());

        paint.setColor(Color.BLACK);
        paint.setTextSize(10f);
        paint.setFakeBoldText(true);
        canvas.drawText("Verification ID: " + reportId, 45, y, paint);
        canvas.drawText("Scan Timestamp: " + dateStr, 340, y, paint);

        y += 18;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(45, y, 550, y, paint);
        y += 20;

        // 3. Banknote Details & Risk Score Banner
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRect(45, y, 550, y + 70, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("BANKNOTE SPECIFICATIONS", 58, y + 20, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText(result.getDenomination() + " (" + result.getCurrency() + ")", 58, y + 44, paint);

        String serialNum = result.getSerialNumber() != null && !result.getSerialNumber().isEmpty() ? result.getSerialNumber() : "Unclear";
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("Serial: " + serialNum + "  |  " + (result.isTwoSided() ? "Dual-Sided Verified" : "Single-Side Screening"), 58, y + 60, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(9.5f);
        canvas.drawText("AI RISK ASSESSMENT", 340, y + 20, paint);

        int statusColor = Color.parseColor("#10B981");
        String riskText = "LOW RISK (Score: " + result.getRiskScore() + "/100)";
        if (result.getStatus() == com.example.currencyguard.model.AuthenticityStatus.NOT_A_CURRENCY) {
            statusColor = Color.parseColor("#EF4444");
            riskText = "NOT A CURRENCY NOTE";
        } else if ("HIGH_RISK".equalsIgnoreCase(result.getRiskLevel()) || result.getRiskScore() >= 70) {
            statusColor = Color.parseColor("#EF4444");
            riskText = "HIGH RISK (Score: " + result.getRiskScore() + "/100)";
        } else if ("SUSPICIOUS".equalsIgnoreCase(result.getRiskLevel()) || result.getRiskScore() >= 30) {
            statusColor = Color.parseColor("#F59E0B");
            riskText = "SUSPICIOUS (Score: " + result.getRiskScore() + "/100)";
        }

        paint.setColor(statusColor);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        canvas.drawText(riskText, 340, y + 44, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("Confidence: " + (int) result.getFinalConfidence() + "%", 340, y + 60, paint);

        y += 92;

        // 4. Security Feature Verification Audit
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Security Feature Verification Audit", 45, y, paint);
        y += 18;

        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        if (result.isCurrencyNote()) {
            canvas.drawText("• Security Thread: [" + result.getSecurityThreadStatus() + "] " + result.getSecurityThreadDetail(), 60, y, paint); y += 16;
            canvas.drawText("• Watermark Window: [" + result.getWatermarkStatus() + "] " + result.getWatermarkDetail(), 60, y, paint); y += 16;
            canvas.drawText("• Substrate & Texture: [" + result.getSubstrateStatus() + "] " + result.getSubstrateDetail(), 60, y, paint); y += 16;
            canvas.drawText("• Print & Geometry: [" + result.getAlignmentStatus() + "] " + result.getAlignmentDetail(), 60, y, paint); y += 22;
        } else {
            canvas.drawText("• Rejection Reason: " + result.getVerdictSummary(), 60, y, paint); y += 16;
            canvas.drawText("• Scanned image does not exhibit official currency layout, typography, or security marks.", 60, y, paint); y += 22;
        }

        // 5. Multi-Stage AI Component Analysis
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Multi-Stage AI Component Breakdown", 45, y, paint);
        y += 18;

        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("• Visual Model Similarity: " + (int) result.getVisualScore() + "% (Weight: 35%)", 60, y, paint); y += 15;
        canvas.drawText("• Security Region Features: " + (int) result.getSecurityScore() + "% (Weight: 25%)", 60, y, paint); y += 15;
        canvas.drawText("• OCR Text & Denomination: " + (int) result.getOcrScore() + "% (Weight: 15%)", 60, y, paint); y += 15;
        canvas.drawText("• Geometry & Layout Alignment: " + (int) result.getGeometryScore() + "% (Weight: 15%)", 60, y, paint); y += 15;
        canvas.drawText("• Input Image Quality Score: " + (int) result.getImageQualityScore() + "% (Weight: 10%)", 60, y, paint); y += 15;
        canvas.drawText("• Visual Anomaly Index: " + (int) result.getAnomalyScore() + " / 100", 60, y, paint); y += 22;

        // 6. AI Explanation Summary
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("AI Screening Summary & Rationale", 45, y, paint);
        y += 16;

        paint.setTextSize(9.5f);
        paint.setFakeBoldText(false);
        String explanation = result.getAiExplanation() != null ? result.getAiExplanation() : "Analysis completed successfully.";
        String[] words = explanation.split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (line.length() + w.length() > 80) {
                canvas.drawText(line.toString(), 60, y, paint);
                y += 14;
                line = new StringBuilder();
            }
            line.append(w).append(" ");
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), 60, y, paint);
            y += 22;
        }

        // 7. Uncertainty Factors (AI Doubt Meter)
        if (result.getUncertaintyReasons() != null && !result.getUncertaintyReasons().isEmpty()) {
            paint.setTextSize(12f);
            paint.setFakeBoldText(true);
            paint.setColor(Color.parseColor("#D97706"));
            canvas.drawText("Uncertainty Factors (AI Doubt Meter)", 45, y, paint);
            y += 16;

            paint.setTextSize(9.5f);
            paint.setFakeBoldText(false);
            paint.setColor(Color.BLACK);
            for (UncertaintyReason reason : result.getUncertaintyReasons()) {
                canvas.drawText("⚠ " + reason.getTitle() + ": " + reason.getDescription(), 60, y, paint);
                y += 14;
            }
            y += 16;
        }

        // 8. Mandatory Institutional Legal Disclaimer
        y = 750;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(45, y, 550, y, paint);
        y += 18;

        paint.setColor(Color.GRAY);
        paint.setTextSize(8.5f);
        paint.setFakeBoldText(false);
        canvas.drawText("LEGAL NOTICE & AUDIT DISCLAIMER:", 45, y, paint); y += 12;
        canvas.drawText("This report is an automated algorithmic screening assessment produced by CurrencyGuard AI.", 45, y, paint); y += 11;
        canvas.drawText("It does not constitute an official central bank guarantee or forensic legal authentication.", 45, y, paint);

        document.finishPage(page);

        // Save PDF to external reports folder
        File reportsDir = new File(context.getExternalFilesDir(null), "reports");
        if (!reportsDir.exists()) reportsDir.mkdirs();

        File pdfFile = new File(reportsDir, "CurrencyGuard_" + reportId + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            document.writeTo(fos);
        } catch (IOException e) {
            document.close();
            return null;
        }
        document.close();
        return pdfFile;
    }
}
