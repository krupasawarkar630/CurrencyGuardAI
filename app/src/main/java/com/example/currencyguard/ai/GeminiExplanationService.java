package com.example.currencyguard.ai;

import android.content.Context;

import com.example.currencyguard.model.AnalysisResult;

/**
 * Service routing explanation requests directly to Google Gemini API.
 */
public class GeminiExplanationService {

    private final GeminiApiService geminiApiService = new GeminiApiService();

    public interface ExplanationCallback {
        void onExplanationReady(String explanation, boolean isCloud);
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }

    public static void saveApiKey(Context context, String apiKey) {
        GeminiApiService.saveApiKey(context, apiKey);
    }

    public static String getApiKey(Context context) {
        return GeminiApiService.getApiKey(context);
    }

    public static void setActiveModel(Context context, String model) {
        GeminiApiService.setActiveModel(context, model);
    }

    public static String getActiveModel(Context context) {
        return GeminiApiService.getActiveModel(context);
    }

    public void explain(Context context, AnalysisResult result, ExplanationCallback callback) {
        geminiApiService.explain(context, result, callback::onExplanationReady);
    }

    public void testApiKey(Context context, String apiKey, TestCallback callback) {
        geminiApiService.testApiKey(context, apiKey, callback::onTestResult);
    }
}
