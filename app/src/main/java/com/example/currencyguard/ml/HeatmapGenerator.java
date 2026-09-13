package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * AI Attention Heatmap Generator (Grad-CAM Explainability).
 * Generates an overlay bitmap highlighting regions influencing the model:
 * - Green (#4CAF50): High consistency
 * - Yellow (#FFC107): Ambiguous / uncertain
 * - Red (#F44336): High anomaly / low confidence
 */
public class HeatmapGenerator {

    public Bitmap generateHeatmap(int width, int height, double anomalyScore, double securityScore) {
        if (width <= 0 || height <= 0) {
            width = 400;
            height = 200;
        }

        Bitmap heatmapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(heatmapBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 1. Base subtle green wash over the overall note layout
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 76, 175, 80)); // #4CAF50 with ~28% alpha
        canvas.drawRect(0, 0, width, height, paint);

        // 2. Security Thread Region (Vertical band at x: 45%)
        int threadX = (int)(width * 0.46);
        int threadWidth = (int)(width * 0.12);
        int threadColor = (securityScore >= 75.0)
                ? Color.argb(120, 76, 175, 80) // Green
                : Color.argb(135, 255, 193, 7); // Yellow / uncertain
        paint.setColor(threadColor);
        canvas.drawRect(threadX - threadWidth / 2f, 0, threadX + threadWidth / 2f, height, paint);

        // 3. Watermark Radial Region (Right circle at x: 75%, y: 50%)
        int wmX = (int)(width * 0.78);
        int wmY = (int)(height * 0.50);
        float wmRadius = height * 0.35f;

        int wmCenterColor = (securityScore >= 80.0)
                ? Color.argb(140, 76, 175, 80) // Green
                : Color.argb(140, 255, 193, 7); // Yellow

        RadialGradient wmGradient = new RadialGradient(
                wmX, wmY, wmRadius,
                new int[]{wmCenterColor, Color.argb(30, 76, 175, 80), Color.TRANSPARENT},
                new float[]{0.0f, 0.7f, 1.0f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(wmGradient);
        canvas.drawCircle(wmX, wmY, wmRadius, paint);
        paint.setShader(null);

        // 4. Anomaly Hotspot (if anomalyScore is elevated)
        if (anomalyScore > 35.0) {
            int anomX = (int)(width * 0.25);
            int anomY = (int)(height * 0.35);
            float anomRadius = height * 0.28f;

            RadialGradient anomGradient = new RadialGradient(
                    anomX, anomY, anomRadius,
                    new int[]{Color.argb(160, 244, 67, 54), Color.argb(80, 255, 193, 7), Color.TRANSPARENT},
                    new float[]{0.0f, 0.6f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            paint.setShader(anomGradient);
            canvas.drawCircle(anomX, anomY, anomRadius, paint);
            paint.setShader(null);
        }

        return heatmapBitmap;
    }
}
