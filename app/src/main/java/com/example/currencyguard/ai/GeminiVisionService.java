package com.example.currencyguard.ai;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.currencyguard.model.AnalysisResult;

/**
 * Compatibility adapter routing vision forensics to OpenRouterVisionService.
 */
public class GeminiVisionService {

    private final OpenRouterVisionService visionService = new OpenRouterVisionService();

    public interface VisionCallback {
        void onVisionReportReady(String report, boolean isCloud);
        void onVisionError(String error);
    }

    public void analyzeImage(Context context, Bitmap bitmap, AnalysisResult currentResult, VisionCallback callback) {
        visionService.analyzeImage(context, bitmap, currentResult, new OpenRouterVisionService.VisionCallback() {
            @Override
            public void onVisionReportReady(String report, boolean isCloudOpenRouter) {
                callback.onVisionReportReady(report, isCloudOpenRouter);
            }

            @Override
            public void onVisionError(String error) {
                callback.onVisionError(error);
            }
        });
    }
}
