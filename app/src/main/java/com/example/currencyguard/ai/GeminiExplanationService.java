package com.example.currencyguard.ai;

import android.content.Context;

import com.example.currencyguard.model.AnalysisResult;

/**
 * Compatibility adapter routing explanation requests to OpenRouterService.
 */
public class GeminiExplanationService {

    private final OpenRouterService openRouterService = new OpenRouterService();

    public interface ExplanationCallback {
        void onExplanationReady(String explanation, boolean isCloud);
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }

    public static void saveApiKey(Context context, String apiKey) {
        OpenRouterService.saveApiKey(context, apiKey);
    }

    public static String getApiKey(Context context) {
        return OpenRouterService.getApiKey(context);
    }

    public static void setActiveModel(Context context, String model) {
        OpenRouterService.setActiveModel(context, model);
    }

    public static String getActiveModel(Context context) {
        return OpenRouterService.getActiveModel(context);
    }

    public void explain(Context context, AnalysisResult result, ExplanationCallback callback) {
        openRouterService.explain(context, result, callback::onExplanationReady);
    }

    public void testApiKey(Context context, String apiKey, TestCallback callback) {
        openRouterService.testApiKey(context, apiKey, callback::onTestResult);
    }
}
