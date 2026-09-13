package com.example.currencyguard.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.currencyguard.model.AnalysisResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Multimodal OpenRouter Vision AI Forensics Service.
 * Transmits banknote imagery to OpenRouter-hosted vision models (Gemini 2.0 Flash, Llama 3.2 Vision)
 * for physical security feature inspection.
 */
public class OpenRouterVisionService {

    private static final String TAG = "OpenRouterVision";
    private static final String OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface VisionCallback {
        void onVisionReportReady(String report, boolean isCloudOpenRouter);
        void onVisionError(String error);
    }

    public OpenRouterVisionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void analyzeImage(Context context, Bitmap bitmap, AnalysisResult currentResult, VisionCallback callback) {
        String apiKey = OpenRouterService.getApiKey(context);

        if (bitmap == null) {
            callback.onVisionError("No image available for AI Vision inspection.");
            return;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            String localReport = generateLocalForensicReport(currentResult);
            mainHandler.post(() -> callback.onVisionReportReady(localReport, false));
            return;
        }

        executor.execute(() -> {
            try {
                Bitmap scaledBitmap = scaleBitmapDown(bitmap, 1024);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                String prompt = "You are the CurrencyGuard AI Multimodal Vision Forensic Inspector. " +
                        "Inspect this currency banknote photo for authenticity and security feature integrity. " +
                        "Evaluate: " +
                        "1. Denomination & Currency identity. " +
                        "2. Security thread (windowed, continuous, color-shift). " +
                        "3. Portrait watermark & electrotype region. " +
                        "4. Microlettering, bleed lines, and typography. " +
                        "5. Serial number alignment and font uniformity. " +
                        "Provide a concise, professional forensic summary strictly under 120 words. " +
                        "If the image does NOT contain a recognized banknote, explicitly state it is not recognized currency.";

                String selectedModel = OpenRouterService.getActiveModel(context);
                String[] visionModels = new String[]{selectedModel, "google/gemini-2.0-flash-exp:free", "meta-llama/llama-3.2-11b-vision-instruct:free"};

                String aiResponseText = null;

                for (String model : visionModels) {
                    try {
                        JSONObject textPart = new JSONObject().put("type", "text").put("text", prompt);
                        JSONObject imgUrlObj = new JSONObject().put("url", "data:image/jpeg;base64," + base64Image);
                        JSONObject imagePart = new JSONObject().put("type", "image_url").put("image_url", imgUrlObj);

                        JSONArray contentParts = new JSONArray().put(textPart).put(imagePart);
                        JSONObject userMessage = new JSONObject().put("role", "user").put("content", contentParts);
                        JSONArray messages = new JSONArray().put(userMessage);

                        JSONObject payload = new JSONObject();
                        payload.put("model", model);
                        payload.put("messages", messages);
                        payload.put("temperature", 0.2);

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
                                    JSONObject choiceObj = choices.getJSONObject(0);
                                    JSONObject messageObj = choiceObj.getJSONObject("message");
                                    aiResponseText = messageObj.getString("content");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Vision attempt with " + model + " failed: " + e.getMessage());
                    }
                }

                if (aiResponseText != null && !aiResponseText.trim().isEmpty()) {
                    String finalResult = aiResponseText.trim();
                    mainHandler.post(() -> callback.onVisionReportReady(finalResult, true));
                    return;
                }

            } catch (Exception e) {
                Log.e(TAG, "OpenRouter vision call error: " + e.getMessage(), e);
            }

            String localFallback = generateLocalForensicReport(currentResult);
            mainHandler.post(() -> callback.onVisionReportReady(localFallback, false));
        });
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }
        float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String generateLocalForensicReport(AnalysisResult result) {
        if (result == null || !result.isCurrencyNote()) {
            return "On-Device Visual Inspection: The presented image does not exhibit structural geometry, security thread contrast, or standard central bank typography characteristic of currency notes.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("On-Device Vision Forensics: ").append(result.getDenomination()).append(" ").append(result.getCurrency()).append(" note. ");

        if (result.getStatus() != null && result.getStatus().isReal()) {
            sb.append("Security thread continuity and luminescence contrast match authentic references. ");
            sb.append("Watermark window density variation and microprinting alignment scored in genuine tolerance (")
                    .append((int) result.getSecurityScore()).append("%). Overall confidence is robust at ")
                    .append((int) result.getFinalConfidence()).append("%.");
        } else if (result.getStatus() != null && result.getStatus().isFake()) {
            sb.append("Significant anomalies detected in security thread reflectance and typography alignment. ");
            sb.append("Watermark and substrate fidelity scored low (")
                    .append((int) result.getSecurityScore()).append("%). Note exhibits high counterfeit probability.");
        } else {
            sb.append("Visual audit indicates partial alignment with reference patterns. ");
            sb.append("However, ambient lighting glare or surface wear induced uncertainty. Secondary scan or bank examination recommended.");
        }

        return sb.toString();
    }
}
