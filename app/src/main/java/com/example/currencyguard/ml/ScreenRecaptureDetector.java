package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Locale;

/**
 * AI Anti-Spoofing & Screen Recapture Detector.
 * Detects whether a banknote image was captured from a laptop, monitor, or mobile display,
 * or contains non-circulating digital toy/souvenir reproduction text (e.g., "Manoranjan Bank", "Coupon", "Points").
 */
public class ScreenRecaptureDetector {

    public static class ScreenReport {
        private final boolean isScreenRecapture;
        private final boolean isToyOrCoupon;
        private final double screenScore; // 0 to 100
        private final String primaryReason;

        public ScreenReport(boolean isScreenRecapture, boolean isToyOrCoupon, double screenScore, String primaryReason) {
            this.isScreenRecapture = isScreenRecapture;
            this.isToyOrCoupon = isToyOrCoupon;
            this.screenScore = screenScore;
            this.primaryReason = primaryReason;
        }

        public boolean isScreenRecapture() { return isScreenRecapture; }
        public boolean isToyOrCoupon() { return isToyOrCoupon; }
        public double getScreenScore() { return screenScore; }
        public String getPrimaryReason() { return primaryReason; }
    }

    public ScreenReport detect(Bitmap bitmap, String ocrText) {
        if (bitmap == null) {
            return new ScreenReport(false, false, 0.0, "No image to inspect");
        }

        String rawUpper = (ocrText != null) ? ocrText.toUpperCase(Locale.ROOT) : "";

        // 1. Text-based digital / toy reproduction detection
        boolean hasManoranjan = rawUpper.contains("MANORANJAN") || rawUpper.contains("MANORAMJAN") || rawUpper.contains("मनोरंजन");
        boolean hasCoupon = rawUpper.contains("COUPON") || rawUpper.contains("KOOPAN") || rawUpper.contains("कूपन");
        boolean hasPoints = rawUpper.contains("POINTS") || rawUpper.contains("POINT") || rawUpper.contains("पॉइंट्स");
        boolean hasFun = rawUpper.contains("FULL OF FUN") || rawUpper.contains("OF FUN");
        boolean hasChildrenBank = rawUpper.contains("CHILDREN BANK") || rawUpper.contains("BAL BANK") || rawUpper.contains("बाल बैंक");
        boolean hasToyOrFake = rawUpper.contains("TOY") || rawUpper.contains("SPECIMEN") || rawUpper.contains("REPLICA") ||
                               rawUpper.contains("CINEMA") || rawUpper.contains("CHURAN") || rawUpper.contains("DUMMY") ||
                               rawUpper.contains("NOT FOR CIRCULATION") || rawUpper.contains("NOT LEGAL TENDER");

        boolean isToyNote = hasManoranjan || hasCoupon || hasPoints || hasFun || hasChildrenBank || hasToyOrFake;

        if (isToyNote) {
            StringBuilder sb = new StringBuilder("Non-circulating reproduction markings detected: ");
            if (hasManoranjan) sb.append("'Manoranjan Bank' ");
            if (hasCoupon) sb.append("'Coupon' ");
            if (hasPoints) sb.append("'Points' ");
            if (hasFun) sb.append("'Full of Fun' ");
            if (hasChildrenBank) sb.append("'Children Bank' ");
            if (hasToyOrFake) sb.append("'Toy/Replica' ");
            sb.append("(Not official legal tender currency).");

            return new ScreenReport(true, true, 95.0, sb.toString().trim());
        }

        // 2. Optical & Physical Screen Recapture Analysis
        // (Detects LCD/OLED subpixel moire patterns, screen backlight emission, and lack of paper texture)
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int step = Math.max(2, Math.min(width, height) / 80);

        int sampleCount = 0;
        double periodicGridVariance = 0.0;
        double backlightBlueBias = 0.0;
        int flatColorPixels = 0;
        int elevatedBlackCount = 0; // LCD displays have elevated black level due to backlight glow

        // Scan central region of the image
        int startX = width / 6;
        int endX = width - startX;
        int startY = height / 6;
        int endY = height - startY;

        for (int y = startY + step; y < endY - step; y += step) {
            for (int x = startX + step; x < endX - step; x += step) {
                int p = bitmap.getPixel(x, y);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                sampleCount++;

                // Measure blue-shifted backlight bias typical of LCD/LED monitors (6500K-9300K white point)
                if (b > r + 15 && b > g + 10 && (r + g + b) > 400) {
                    backlightBlueBias += 1.0;
                }

                // Check LCD backlight floor in dark printing regions (LCD black is rarely pure black, usually rgb 25-50)
                int maxChannel = Math.max(r, Math.max(g, b));
                int minChannel = Math.min(r, Math.min(g, b));
                if (maxChannel < 60 && minChannel > 18) {
                    elevatedBlackCount++;
                }

                // Measure micro-grid alternating difference (Moire pattern & pixel rows)
                int pNextX = bitmap.getPixel(x + step, y);
                int rNext = Color.red(pNextX);
                int gNext = Color.green(pNextX);
                int bNext = Color.blue(pNextX);

                int diffX = Math.abs(r - rNext) + Math.abs(g - gNext) + Math.abs(b - bNext);
                if (diffX < 5) {
                    flatColorPixels++;
                } else if (diffX > 45 && diffX < 120) {
                    periodicGridVariance += 1.0;
                }
            }
        }

        if (sampleCount == 0) {
            return new ScreenReport(false, false, 0.0, "Insufficient sample pixels");
        }

        double gridRatio = periodicGridVariance / sampleCount;
        double blueBiasRatio = backlightBlueBias / sampleCount;
        double blackFloorRatio = (double) elevatedBlackCount / sampleCount;
        double flatRatio = (double) flatColorPixels / sampleCount;

        // Scoring algorithm for screen recapture probability
        double screenProbability = 10.0; // Baseline

        if (gridRatio > 0.28) {
            screenProbability += 30.0; // High frequency periodic grid (Moire fringes)
        }
        if (blueBiasRatio > 0.15) {
            screenProbability += 22.0; // Emissive cool monitor backlight
        }
        if (blackFloorRatio > 0.08) {
            screenProbability += 18.0; // LCD backlight bleed in dark regions
        }
        if (flatRatio > 0.35) {
            screenProbability += 15.0; // Flat digital graphics without cotton fibre micro-texture
        }

        boolean isScreen = screenProbability >= 52.0;
        String reason = isScreen ?
                "Digital display moire pattern and emissive screen backlight detected (photographed from laptop/monitor screen)." :
                "Normal physical reflection characteristics.";

        return new ScreenReport(isScreen, false, Math.min(95.0, screenProbability), reason);
    }
}
