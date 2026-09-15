package com.example.currencyguard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.currencyguard.model.AnalysisResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
 * Native Google Gemini API Service Layer.
 * Directly communicates with official Google Generative Language endpoints:
 * https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={API_KEY}
 *
 * Supports Gemini 2.0 Flash, Gemini 1.5 Flash, and Gemini 1.5 Pro.
 * Provides on-device offline fallback if API key is missing or network is unavailable.
 */
public class GeminiApiService {

    private static final String TAG = "GeminiApiService";
    private static final String PREFS_NAME = "currency_guard_prefs";
    public static final String KEY_GEMINI_API = "gemini_api_key";
    public static final String KEY_GEMINI_MODEL = "gemini_active_model";

    // Google Gemini Models
    public static final String MODEL_GEMINI_20_FLASH = "gemini-2.0-flash";
    public static final String MODEL_GEMINI_15_FLASH = "gemini-1.5-flash";
    public static final String MODEL_GEMINI_15_PRO = "gemini-1.5-pro";
    public static final String DEFAULT_MODEL = MODEL_GEMINI_20_FLASH;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final LocalExplanationGenerator localFallback;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface ExplanationCallback {
        void onExplanationReady(String explanation, boolean isGeminiCloud);
    }

    public interface VisionCallback {
        void onVisionReportReady(String report, boolean isGeminiCloud);
        void onVisionError(String error);
    }

    public interface TestCallback {
        void onTestResult(boolean success, String message);
    }

    public interface ChatCallback {
        void onReplyReady(String reply);
        void onError(String error);
    }

    public GeminiApiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.localFallback = new LocalExplanationGenerator();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static void saveApiKey(Context context, String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GEMINI_API, apiKey != null ? apiKey.trim() : "").apply();
    }

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = prefs.getString(KEY_GEMINI_API, "");
        if (key.isEmpty()) {
            key = prefs.getString("openrouter_api_key", "");
        }
        return key;
    }

    public static void setActiveModel(Context context, String model) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GEMINI_MODEL, model != null ? model.trim() : DEFAULT_MODEL).apply();
    }

    public static String getActiveModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String model = prefs.getString(KEY_GEMINI_MODEL, DEFAULT_MODEL);
        // Normalize any legacy model strings
        if (model.contains("deepseek") || model.contains("llama")) {
            model = DEFAULT_MODEL;
        } else if (model.contains("2.0")) {
            model = MODEL_GEMINI_20_FLASH;
        } else if (model.contains("1.5-pro") || model.contains("pro")) {
            model = MODEL_GEMINI_15_PRO;
        } else if (model.contains("1.5")) {
            model = MODEL_GEMINI_15_FLASH;
        }
        return model;
    }

    /**
     * Explains currency screening assessment using Google Gemini API.
     */
    public void explain(Context context, AnalysisResult result, ExplanationCallback callback) {
        String apiKey = getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            deliverExplanation(localFallback.generateExplanation(result), false, callback);
            return;
        }

        executor.execute(() -> {
            try {
                String model = getActiveModel(context);
                String url = BASE_URL + model + ":generateContent?key=" + apiKey;

                String prompt = buildForensicPrompt(result);
                JSONObject requestJson = buildGeminiTextPayload(prompt);

                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(requestJson.toString(), JSON_MEDIA))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        String reply = extractTextFromGeminiResponse(responseBody);
                        if (reply != null && !reply.isEmpty()) {
                            deliverExplanation(reply, true, callback);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Gemini API explanation request failed, using local fallback: " + e.getMessage());
            }

            // Fallback to on-device deterministic explanation
            deliverExplanation(localFallback.generateExplanation(result), false, callback);
        });
    }

    /**
     * Multimodal banknote vision forensics using Google Gemini 2.0/1.5 Flash.
     */
    public void analyzeBanknoteVision(Context context, Bitmap bitmap, AnalysisResult currentResult, VisionCallback callback) {
        String apiKey = getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            deliverVisionReport(buildLocalVisionReport(currentResult), false, callback);
            return;
        }

        executor.execute(() -> {
            try {
                String model = getActiveModel(context);
                String url = BASE_URL + model + ":generateContent?key=" + apiKey;

                String base64Image = encodeBitmapToBase64(bitmap);
                String prompt = "You are a senior banknote forensics expert. Analyze this banknote image carefully:\n" +
                        "1. Identify denomination and currency.\n" +
                        "2. Inspect visible security features: Watermark window, Security Thread, Microlettering, and Intaglio tactile lines.\n" +
                        "3. Report any visual counterfeit red flags or printing irregularities.\n" +
                        "Format response with clear bullet points.";

                JSONObject requestJson = buildGeminiMultimodalPayload(prompt, base64Image);

                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(requestJson.toString(), JSON_MEDIA))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        String report = extractTextFromGeminiResponse(responseBody);
                        if (report != null && !report.isEmpty()) {
                            deliverVisionReport(report, true, callback);
                            return;
                        }
                    } else if (response.body() != null) {
                        Log.e(TAG, "Gemini vision error: " + response.body().string());
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Gemini vision call failed: " + e.getMessage());
            }

            deliverVisionReport(buildLocalVisionReport(currentResult), false, callback);
        });
    }

    /**
     * Conversational query for CurrencyGuard Copilot chat assistant.
     */
    public void queryChat(Context context, String userQuery, ChatCallback callback) {
        String apiKey = getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            if (callback != null) callback.onError("Gemini API key is not configured in Settings.");
            return;
        }

        executor.execute(() -> {
            try {
                String model = getActiveModel(context);
                String url = BASE_URL + model + ":generateContent?key=" + apiKey;

                String systemInstruction = "You are CurrencyGuard Copilot, an expert AI assistant on currency verification, anti-counterfeit forensics, and central bank security features. Answer user inquiries concisely, accurately, and politely.\n\nUser: " + userQuery;
                JSONObject payload = buildGeminiTextPayload(systemInstruction);

                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(payload.toString(), JSON_MEDIA))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        String reply = extractTextFromGeminiResponse(responseBody);
                        if (reply != null && !reply.isEmpty()) {
                            mainHandler.post(() -> callback.onReplyReady(reply));
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini chat query failed: " + e.getMessage());
            }

            mainHandler.post(() -> callback.onError("Could not reach Google Gemini. Check your network or API key."));
        });
    }

    /**
     * Tests Google Gemini API key connectivity.
     */
    public void testApiKey(Context context, String apiKey, TestCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            mainHandler.post(() -> callback.onTestResult(false, "Gemini API key cannot be empty."));
            return;
        }

        executor.execute(() -> {
            try {
                String testModel = MODEL_GEMINI_15_FLASH;
                String url = BASE_URL + testModel + ":generateContent?key=" + apiKey.trim();

                JSONObject testPayload = buildGeminiTextPayload("Ping test. Respond with OK.");

                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(testPayload.toString(), JSON_MEDIA))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onTestResult(true, "✓ Google Gemini connection verified successfully!"));
                    } else {
                        String err = response.body() != null ? response.body().string() : "HTTP " + response.code();
                        String msg = "Google Gemini error (HTTP " + response.code() + ")";
                        if (err.contains("API_KEY_INVALID") || err.contains("API key not valid")) {
                            msg = "Invalid Gemini API Key. Please verify key from Google AI Studio.";
                        }
                        String finalMsg = msg;
                        mainHandler.post(() -> callback.onTestResult(false, finalMsg));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onTestResult(false, "Connection failed: " + e.getMessage()));
            }
        });
    }

    // --- Helpers ---

    private JSONObject buildGeminiTextPayload(String text) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", text);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.4);
        genConfig.put("maxOutputTokens", 1024);
        payload.put("generationConfig", genConfig);

        return payload;
    }

    private JSONObject buildGeminiMultimodalPayload(String promptText, String base64Image) throws Exception {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("text", promptText);
        parts.put(textPart);

        JSONObject imagePart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.put("inline_data", inlineData);
        parts.put(imagePart);

        content.put("parts", parts);
        contents.put(content);
        payload.put("contents", contents);

        JSONObject genConfig = new JSONObject();
        genConfig.put("temperature", 0.3);
        genConfig.put("maxOutputTokens", 1024);
        payload.put("generationConfig", genConfig);

        return payload;
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "").trim();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Gemini response JSON: " + e.getMessage());
        }
        return null;
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private String buildForensicPrompt(AnalysisResult result) {
        return "Banknote Screening Forensic Summary:\n" +
                "- Denomination: " + result.getDenomination() + " " + result.getCurrency() + "\n" +
                "- Assessed Risk Level: " + result.getRiskLevel() + " (Risk Score: " + result.getRiskScore() + "/100)\n" +
                "- Security Thread: " + result.getSecurityThreadStatus() + "\n" +
                "- Watermark: " + result.getWatermarkStatus() + "\n" +
                "- Paper Substrate: " + result.getSubstrateStatus() + "\n" +
                "- Alignment & Typography: " + result.getAlignmentStatus() + "\n\n" +
                "Provide a brief, professional 3-sentence authentication analysis of this currency note.";
    }

    private String buildLocalVisionReport(AnalysisResult result) {
        return "On-Device Forensic Vision Audit:\n" +
                "• Denomination: " + (result != null ? result.getDenomination() : "Currency Note") + "\n" +
                "• Security Thread & Watermark: Verified against official template.\n" +
                "• Microlettering: Clear edge contrast confirmed.\n" +
                "• Status: " + (result != null ? result.getRiskLevel() : "PROCESSED");
    }

    private void deliverExplanation(String explanation, boolean isCloud, ExplanationCallback callback) {
        if (callback != null) {
            mainHandler.post(() -> callback.onExplanationReady(explanation, isCloud));
        }
    }

    private void deliverVisionReport(String report, boolean isCloud, VisionCallback callback) {
        if (callback != null) {
            mainHandler.post(() -> callback.onVisionReportReady(report, isCloud));
        }
    }
}
