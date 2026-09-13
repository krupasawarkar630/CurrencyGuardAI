package com.example.currencyguard.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.model.UncertaintyReason;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FEATURE 16: AI REPORT / PDF EXPORT
 * Generates an authenticity screening report PDF using the native Android PdfDocument API.
 */
public class PdfReportGenerator {

    public static File generateReport(Context context, AnalysisResult result, String customId) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int y = 40;

        // 1. Header Banner
        paint.setColor(Color.parseColor("#1976D2"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(30, y, 565, y + 60, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("CurrencyGuard AI — Authenticity Report", 45, y + 36, paint);

        y += 85;

        // 2. Metadata
        String reportId = (customId != null && !customId.isEmpty()) ? customId : "CG-" + System.currentTimeMillis() % 1000000;
        String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(new Date());

        paint.setColor(Color.BLACK);
        paint.setTextSize(11f);
        paint.setFakeBoldText(false);
        canvas.drawText("Report ID: " + reportId, 45, y, paint);
        canvas.drawText("Date: " + dateStr, 350, y, paint);

        y += 20;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(45, y, 550, y, paint);
        y += 25;

        // 3. Overall Result Banner
        paint.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(45, y, 550, y + 60, paint);

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(10f);
        canvas.drawText("CURRENCY & DENOMINATION", 60, y + 20, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText(result.getDenomination() + " (" + result.getCurrency() + ")", 60, y + 45, paint);

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        canvas.drawText("SCREENING VERDICT", 350, y + 20, paint);

        int statusColor = (result.getStatus() == com.example.currencyguard.model.AuthenticityStatus.NOT_A_CURRENCY) ? Color.parseColor("#D32F2F") :
                ((result.getFinalConfidence() >= 80) ? Color.parseColor("#00C853") :
                (result.getFinalConfidence() >= 55 ? Color.parseColor("#FFB300") : Color.parseColor("#F44336")));
        paint.setColor(statusColor);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        String verdictStr = (result.getStatus() == com.example.currencyguard.model.AuthenticityStatus.NOT_A_CURRENCY)
                ? "NOT A CURRENCY NOTE"
                : result.getVerdictTitle() + " (" + (int)result.getFinalConfidence() + "%)";
        canvas.drawText(verdictStr, 350, y + 45, paint);

        y += 85;

        // 4. Real vs Fake Feature Checklist
        paint.setColor(Color.BLACK);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        canvas.drawText(result.isCurrencyNote() ? "Real vs Fake Security Feature Audit" : "Banknote Detection Diagnostics", 45, y, paint);
        y += 18;

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        if (result.isCurrencyNote()) {
            String threadCheck = result.isSecurityThreadReal() ? "REAL [PASS]" : "FAKE [FAIL - Low contrast / printed]";
            String watermarkCheck = result.isWatermarkReal() ? "REAL [PASS]" : "FAKE [FAIL - Lacks fiber graduation]";
            String substrateCheck = result.isSubstrateReal() ? "REAL [PASS]" : "FAKE [FAIL - Photocopy / plain paper]";
            String alignmentCheck = result.isPrintAlignmentReal() ? "REAL [PASS]" : "FAKE [FAIL - Dimension discrepancy]";

            canvas.drawText("• Security Thread: " + threadCheck, 60, y, paint); y += 15;
            canvas.drawText("• Watermark Window: " + watermarkCheck, 60, y, paint); y += 15;
            canvas.drawText("• Substrate & Texture: " + substrateCheck, 60, y, paint); y += 15;
            canvas.drawText("• Print & Geometry: " + alignmentCheck, 60, y, paint); y += 22;
        } else {
            canvas.drawText("• Rejection Reason: " + result.getVerdictSummary(), 60, y, paint); y += 15;
            canvas.drawText("• Image does not exhibit official banknote dimensions, typography, or substrate markers.", 60, y, paint); y += 22;
        }

        // 5. Detailed Component Analysis
        paint.setColor(Color.BLACK);
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        canvas.drawText("Multi-Stage AI Component Analysis", 45, y, paint);
        y += 18;

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        canvas.drawText("• Image Quality Gate: " + (int)result.getImageQualityScore() + "%", 60, y, paint); y += 15;
        canvas.drawText("• Visual Classifier Model: " + (int)result.getVisualScore() + "%", 60, y, paint); y += 15;
        canvas.drawText("• Security Region Features: " + (int)result.getSecurityScore() + "%", 60, y, paint); y += 15;
        canvas.drawText("• OCR Text Consistency: " + (int)result.getOcrScore() + "%", 60, y, paint); y += 15;
        canvas.drawText("• Geometry & Layout Alignment: " + (int)result.getGeometryScore() + "%", 60, y, paint); y += 15;
        canvas.drawText("• Visual Anomaly Index: " + (int)result.getAnomalyScore() + " / 100", 60, y, paint); y += 22;

        // 5. AI Explanation
        paint.setTextSize(13f);
        paint.setFakeBoldText(true);
        canvas.drawText("AI Explanation", 45, y, paint);
        y += 16;

        paint.setTextSize(10f);
        paint.setFakeBoldText(false);
        String explanation = result.getAiExplanation() != null ? result.getAiExplanation() : "Analysis completed successfully.";
        // Draw wrapped explanation lines
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
            y += 24;
        }

        // 6. Uncertainty Reasons (Feature 1 Doubt Meter)
        if (!result.getUncertaintyReasons().isEmpty()) {
            paint.setTextSize(13f);
            paint.setFakeBoldText(true);
            paint.setColor(Color.parseColor("#D32F2F"));
            canvas.drawText("Uncertainty Factors (AI Doubt Meter)", 45, y, paint);
            y += 16;

            paint.setTextSize(10f);
            paint.setFakeBoldText(false);
            paint.setColor(Color.BLACK);
            for (UncertaintyReason reason : result.getUncertaintyReasons()) {
                canvas.drawText("⚠ " + reason.getTitle() + ": " + reason.getDescription(), 60, y, paint);
                y += 14;
            }
            y += 16;
        }

        // 7. Mandatory Legal Disclaimer
        y = 750;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(45, y, 550, y, paint);
        y += 20;

        paint.setColor(Color.GRAY);
        paint.setTextSize(9f);
        paint.setFakeBoldText(false);
        canvas.drawText("DISCLAIMER: This is an AI-assisted screening assessment and is NOT official currency authentication.", 45, y, paint);
        canvas.drawText("CurrencyGuard AI prototype. Do not use as forensic or bank proof.", 45, y + 14, paint);

        document.finishPage(page);

        // Save PDF to cache/external reports folder
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
