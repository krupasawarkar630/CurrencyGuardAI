package com.example.currencyguard.ai;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.currencyguard.model.AnalysisResult;

/**
 * Service routing multimodal vision forensics directly to Google Gemini API.
 */
public class GeminiVisionService {

    private final GeminiApiService geminiApiService = new GeminiApiService();

    public interface VisionCallback {
        void onVisionReportReady(String report, boolean isCloud);
        void onVisionError(String error);
    }

    public interface VisionAuditCallback {
        void onAuditSuccess(String detailedReport);
        void onAuditFailure(String errorMessage);
    }

    public void analyzeImage(Context context, Bitmap bitmap, AnalysisResult currentResult, VisionCallback callback) {
        geminiApiService.analyzeBanknoteVision(context, bitmap, currentResult, new GeminiApiService.VisionCallback() {
            @Override
            public void onVisionReportReady(String report, boolean isGeminiCloud) {
                if (callback != null) callback.onVisionReportReady(report, isGeminiCloud);
            }

            @Override
            public void onVisionError(String error) {
                if (callback != null) callback.onVisionError(error);
            }
        });
    }

    public void analyzeBanknoteVision(Context context, Bitmap bitmap, AnalysisResult currentResult, VisionAuditCallback callback) {
        analyzeImage(context, bitmap, currentResult, new VisionCallback() {
            @Override
            public void onVisionReportReady(String report, boolean isCloud) {
                if (callback != null) callback.onAuditSuccess(report);
            }

            @Override
            public void onVisionError(String error) {
                if (callback != null) callback.onAuditFailure(error);
            }
        });
    }
}
