package com.example.currencyguard.utils;

import android.content.Context;
import android.content.Intent;

/**
 * Utility for global application sharing and screening report dissemination.
 */
public class ShareUtils {

    /**
     * Shares the CurrencyGuard AI application with a rich message.
     */
    public static void shareApp(Context context) {
        String message = "🛡️ Check out CurrencyGuard AI — The Smart Banknote Authenticity & Currency Companion!\n\n" +
                "Key Features:\n" +
                "• 🟢 REAL vs 🔴 FAKE Banknote Detection\n" +
                "• ⚠️ AI Doubt Meter (Transparent Uncertainty Analysis)\n" +
                "• 📈 Multi-Scan Confidence Timeline Chart\n" +
                "• 💱 Multi-Country Live Currency Converter (INR to USD, EUR, GBP, AED, JPY, etc.)\n" +
                "• 🔒 100% On-Device ML Execution & Cloud Firebase Sync\n\n" +
                "Download CurrencyGuard AI to safeguard your currency transactions today!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CurrencyGuard AI — Banknote Protection");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        context.startActivity(Intent.createChooser(shareIntent, "Share CurrencyGuard AI"));
    }

    /**
     * Shares an individual screening result with details.
     */
    public static void shareScreeningResult(Context context, String denomination, String currency, String status, double confidence) {
        String message = "🛡️ CurrencyGuard AI Screening Report\n\n" +
                "Banknote: " + denomination + " " + currency + "\n" +
                "Verdict: " + status + "\n" +
                "AI Confidence: " + (int) confidence + "%\n\n" +
                "Screened with CurrencyGuard AI — AI-Assisted Fake Currency Screening.";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Currency Screening Result — " + denomination);
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        context.startActivity(Intent.createChooser(shareIntent, "Share Screening Result"));
    }
}
