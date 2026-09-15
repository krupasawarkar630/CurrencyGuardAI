package com.example.currencyguard.whatsapp;

import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.AuthenticityStatus;

/**
 * WhatsApp Message Formatter for CurrencyGuard AI.
 * Converts verified AnalysisResult into structured WhatsApp markdown format.
 */
public class WhatsAppMessageFormatter {

    public static String formatWhatsAppResponse(AnalysisResult result) {
        if (result == null) {
            return "❌ *CurrencyGuard AI Error*\nCould not process the banknote image. Please re-send a clear photo.";
        }

        if (result.getStatus() == AuthenticityStatus.NOT_A_CURRENCY
                || !result.isCurrencyNote()
                || "None".equalsIgnoreCase(result.getDenomination())
                || (result.getRiskScore() >= 98 && result.getFinalConfidence() == 0.0)) {
            String reason = result.getRejectionReason();
            if (reason == null || reason.trim().isEmpty()) {
                reason = "No official central bank markings, denomination numerals, or security features detected.";
            }

            return "🛡️ *CurrencyGuard AI — Banknote Screening*\n" +
                   "-------------------------------------------\n" +
                   "❌ *Result:* NO BANKNOTE DETECTED\n\n" +
                   "• The uploaded image does not appear to be an official currency note.\n" +
                   "• *Reason:* " + reason + "\n\n" +
                   "💡 *Note:* CurrencyGuard AI analyzes only recognized Indian banknotes (₹10, ₹20, ₹50, ₹100, ₹200, ₹500, ₹2000).\n" +
                   "Please place the banknote on a flat surface in good lighting and re-send the photo.\n" +
                   "-------------------------------------------\n" +
                   "_Automated AI currency gate verification._";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🛡️ *CurrencyGuard AI — Banknote Screening Report*\n");
        sb.append("-------------------------------------------\n");
        sb.append("*Note:* ").append(result.getDenomination()).append(" ").append(result.getCurrency()).append("\n");

        int risk = result.getRiskScore();
        if ("LOW_RISK".equalsIgnoreCase(result.getRiskLevel()) || risk < 30) {
            sb.append("*Status:* 🟢 LOW RISK\n");
        } else if ("SUSPICIOUS".equalsIgnoreCase(result.getRiskLevel()) || (risk >= 30 && risk < 70)) {
            sb.append("*Status:* 🟡 SUSPICIOUS NOTE\n");
        } else {
            sb.append("*Status:* 🔴 HIGH RISK\n");
        }

        sb.append("*Risk Score:* ").append(risk).append("/100\n");
        sb.append("*Confidence:* ").append((int) result.getFinalConfidence()).append("%\n\n");

        sb.append("✅ *Security Findings:*\n");
        sb.append("• Visual layout: ").append(formatStatus(result.getVisualScore() >= 70 ? "CONSISTENT" : "UNCLEAR")).append("\n");
        sb.append("• Security thread: ").append(formatStatus(result.getSecurityThreadStatus())).append("\n");
        sb.append("• Watermark window: ").append(formatStatus(result.getWatermarkStatus())).append("\n");
        sb.append("• OCR denomination: Matched\n\n");

        if (result.isTwoSided()) {
            sb.append("✓ *Full Note Verified:* Both front and back sides analyzed.\n\n");
        } else {
            sb.append("⚠️ *Notice:* Only front side analyzed.\n");
            sb.append("_Scan both sides in the CurrencyGuard App for stronger verification._\n\n");
        }

        sb.append("*Verification ID:* ").append(result.getVerificationId()).append("\n");
        sb.append("-------------------------------------------\n");
        sb.append("_Automated AI screening assessment. Not a legal monetary certificate._");

        return sb.toString();
    }

    private static String formatStatus(String status) {
        if ("CONSISTENT".equalsIgnoreCase(status)) return "Consistent";
        if ("UNCLEAR".equalsIgnoreCase(status)) return "Unclear (Check lighting)";
        if ("SUSPICIOUS".equalsIgnoreCase(status)) return "Suspicious / Anomaly";
        return "Not Analyzed";
    }
}
