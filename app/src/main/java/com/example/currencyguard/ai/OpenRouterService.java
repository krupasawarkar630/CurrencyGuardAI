package com.example.currencyguard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.model.UncertaintyReason;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenRouter AI Service Layer.
 * Communicates with OpenRouter (https://openrouter.ai/api/v1/chat/completions)
 * supporting multiple models (Gemini, Llama 3.3, DeepSeek) through an OpenAI-compatible API.
 * Automatically falls back to LocalExplanationGenerator if offline or key is missing.
 */
public class OpenRouterService {

    private static final String TAG = "OpenRouterService";
    private static final String PREFS_NAME = "currency_guard_prefs";
    public static final String KEY_OPENROUTER_API = "openrouter_api_key";
    public static final String KEY_OPENROUTER_MODEL = "openrouter_active_model";

    // OpenRouter popular models (including top free models)
    public static final String MODEL_GEMINI_FREE = "google/gemini-2.0-flash-exp:free";
    public static final String MODEL_LLAMA_FREE = "meta-llama/llama-3.3-70b-instruct:free";
    public static final String MODEL_DEEPSEEK = "deepseek/deepseek-chat";
    public static final String DEFAULT_MODEL = MODEL_GEMINI_FREE;

    private static final String OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final LocalExplanationGenerator localFallback;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface ExplanationCallback {
        void onExplanationReady(String explanation, boolean isOpenRouterCloud);
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }

    public OpenRouterService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
        this.localFallback = new LocalExplanationGenerator();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static void saveApiKey(Context context, String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_OPENROUTER_API, apiKey != null ? apiKey.trim() : "").apply();
    }

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Look up OpenRouter key first, fallback to previous key storage if present
        String key = prefs.getString(KEY_OPENROUTER_API, "");
        if (key.isEmpty()) {
            key = prefs.getString("legacy_openrouter_key", "");
        }
        return key;
    }

    public static void setActiveModel(Context context, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_OPENROUTER_MODEL, model != null ? model.trim() : DEFAULT_MODEL).apply();
    }

    public static String getActiveModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_OPENROUTER_MODEL, DEFAULT_MODEL);
    }

    public void explain(Context context, AnalysisResult result, ExplanationCallback callback) {
        String apiKey = getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            // No API key configured: Deliver on-device explanation
            String localText = localFallback.generateExplanation(result);
            mainHandler.post(() -> callback.onExplanationReady(localText, false));
            return;
        }

        executor.execute(() -> {
            String selectedModel = getActiveModel(context);
            String[] modelsToTry = new String[]{selectedModel, MODEL_GEMINI_FREE, MODEL_LLAMA_FREE};

            String systemPrompt = "You are an explanation assistant for an educational currency screening application. " +
                    "Explain the provided computer-vision analysis in simple language. " +
                    "Do not claim official authentication. Do not invent security features. " +
                    "If uncertainty reasons exist, explain them clearly. Keep response strictly under 95 words.";

            String userPrompt = buildPrompt(result);

            for (String model : modelsToTry) {
                try {
                    JSONObject systemMsg = new JSONObject().put("role", "system").put("content", systemPrompt);
                    JSONObject userMsg = new JSONObject().put("role", "user").put("content", userPrompt);
                    JSONArray messages = new JSONArray().put(systemMsg).put(userMsg);

                    JSONObject payload = new JSONObject();
                    payload.put("model", model);
                    payload.put("messages", messages);
                    payload.put("temperature", 0.3);

                    RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA);
                    Request request = new Request.Builder()
                            .url(OPENROUTER_ENDPOINT)
                            .header("Authorization", "Bearer " + apiKey)
                            .header("HTTP-Referer", "https://currencyguard.ai")
                            .header("X-Title", "CurrencyGuard AI")
                            .post(body)
                            .build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            JSONObject jsonResp = new JSONObject(response.body().string());
                            JSONArray choices = jsonResp.optJSONArray("choices");
                            if (choices != null && choices.length() > 0) {
                                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                                String outputText = message.getString("content");
                                mainHandler.post(() -> callback.onExplanationReady(outputText.trim(), true));
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "OpenRouter attempt with " + model + " failed: " + e.getMessage());
                }
            }

            // Fallback to local generator
            String localText = localFallback.generateExplanation(result);
            mainHandler.post(() -> callback.onExplanationReady(localText, false));
        });
    }

    public void testApiKey(Context context, String apiKey, TestCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onTestResult(false, "OpenRouter API key is empty.");
            return;
        }

        executor.execute(() -> {
            try {
                String model = getActiveModel(context);

                JSONObject userMsg = new JSONObject().put("role", "user").put("content", "Ping test. Reply with 'OK'.");
                JSONArray messages = new JSONArray().put(userMsg);

                JSONObject payload = new JSONObject();
                payload.put("model", model);
                payload.put("messages", messages);

                RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA);
                Request request = new Request.Builder()
                        .url(OPENROUTER_ENDPOINT)
                        .header("Authorization", "Bearer " + apiKey.trim())
                        .header("HTTP-Referer", "https://currencyguard.ai")
                        .header("X-Title", "CurrencyGuard AI")
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        mainHandler.post(() -> callback.onTestResult(true, "OpenRouter connected successfully (" + model + ")"));
                    } else {
                        int code = response.code();
                        mainHandler.post(() -> callback.onTestResult(false, "OpenRouter returned HTTP " + code + ". Please check your key."));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onTestResult(false, "Network error: " + e.getMessage()));
            }
        });
    }

    private String buildPrompt(AnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analysis Data:\n");
        sb.append("- Denomination: ").append(r.getDenomination()).append("\n");
        sb.append("- Overall Confidence: ").append(r.getFinalConfidence()).append("%\n");
        sb.append("- Image Quality: ").append(r.getImageQualityScore()).append("%\n");
        sb.append("- Visual Classifier Score: ").append(r.getVisualScore()).append("%\n");
        sb.append("- Security Region Score: ").append(r.getSecurityScore()).append("%\n");
        sb.append("- OCR Text Detected: \"").append(r.getDetectedOcrText()).append("\"\n");
        sb.append("- Geometry Score: ").append(r.getGeometryScore()).append("%\n");
        sb.append("- Anomaly Score: ").append(r.getAnomalyScore()).append("/100\n");

        if (r.getUncertaintyReasons() != null && !r.getUncertaintyReasons().isEmpty()) {
            sb.append("- Uncertainty Factors:\n");
            for (UncertaintyReason reason : r.getUncertaintyReasons()) {
                sb.append("  * ").append(reason.getDescription()).append("\n");
            }
        }
        return sb.toString();
    }
}
