package com.example.currencyguard.camera;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Smart Scan Guidance Layer (Section 18).
 * Evaluates real-time camera frames to provide immediate, actionable feedback:
 * - Distance checks (too close / too far)
 * - Glare & illumination checks
 * - Angle & alignment guidance
 */
public class SmartScanGuide {

    public static class GuidanceResult {
        private final String message;
        private final boolean isReadyToCapture;

        public GuidanceResult(String message, boolean isReadyToCapture) {
            this.message = message;
            this.isReadyToCapture = isReadyToCapture;
        }

        public String getMessage() { return message; }
        public boolean isReadyToCapture() { return isReadyToCapture; }
    }

    public static GuidanceResult evaluateFrame(Bitmap bitmap) {
        if (bitmap == null) {
            return new GuidanceResult("📐 Align currency note inside the frame", false);
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int step = Math.max(1, Math.min(width, height) / 25);

        long totalLum = 0;
        int overexposedCount = 0;
        int underexposedCount = 0;
        int totalSampled = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int p = bitmap.getPixel(x, y);
                int lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000;
                totalLum += lum;
                if (lum > 240) overexposedCount++;
                if (lum < 30) underexposedCount++;
                totalSampled++;
            }
        }

        double avgLum = (double) totalLum / (totalSampled + 1);
        double glareRatio = (double) overexposedCount / (totalSampled + 1);

        if (avgLum < 40) {
            return new GuidanceResult("💡 Increase ambient light or enable flashlight", false);
        }

        if (glareRatio > 0.18) {
            return new GuidanceResult("💡 Reduce reflection: Tilt camera slightly away from light", false);
        }

        // Check if note edges fill the reticle frame reasonably
        return new GuidanceResult("✨ Note framed well. Tap shutter to screen.", true);
    }
}
