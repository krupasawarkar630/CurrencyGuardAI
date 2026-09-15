package com.example.currencyguard.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Educational AI Chat Assistant Service for "Ask CurrencyGuard AI".
 * Answers questions regarding currency security characteristics and AI confidence.
 * Preloaded with extensive offline knowledge; can optionally leverage Gemini if connected.
 */
public class ChatAssistantService {

    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    public interface ChatCallback {
        void onReply(String reply);
    }

    public ChatAssistantService() {
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void ask(Context context, String query, ChatCallback callback) {
        String queryLower = query.toLowerCase().trim();

        // Safety Guardrail: Block illicit inquiries
        if (queryLower.contains("make fake") || queryLower.contains("bypass") ||
            queryLower.contains("counterfeit template") || queryLower.contains("forge") ||
            queryLower.contains("print money")) {
            mainHandler.post(() -> callback.onReply("I cannot provide instructions for creating, modifying, or bypassing currency security features. I can only assist with educational verification concepts."));
            return;
        }

        // Check offline educational knowledge base first
        String offlineMatch = findOfflineAnswer(queryLower);
        String apiKey = GeminiExplanationService.getApiKey(context);

        if (apiKey == null || apiKey.trim().isEmpty() || offlineMatch != null) {
            mainHandler.post(() -> callback.onReply(offlineMatch != null ? offlineMatch : getDefaultEducationalReply()));
            return;
        }

        // Online Google Gemini query if key is configured
        new GeminiApiService().queryChat(context, query, new GeminiApiService.ChatCallback() {
            @Override
            public void onReplyReady(String reply) {
                mainHandler.post(() -> callback.onReply(reply));
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> callback.onReply(getDefaultEducationalReply()));
            }
        });
    }

    private String findOfflineAnswer(String query) {
        if (query.contains("low") || query.contains("why confidence")) {
            return "AI confidence is reduced when the AI Doubt Meter detects issues like glare or blur on the note surface, unclear security thread regions, an aspect ratio mismatch, or discrepancies between OCR text and the detected denomination.";
        }
        if (query.contains("watermark")) {
            return "A watermark is formed by varying paper fiber density during paper creation. In genuine Indian currency, the Mahatma Gandhi portrait and electrotype denomination appear seamlessly with multidirectional shading when held up against light.";
        }
        if (query.contains("heatmap")) {
            return "The AI Attention Heatmap highlights which regions guided the computer-vision model. Green areas indicate high consistency with genuine reference templates, yellow marks ambiguity, and red indicates an unexpected anomaly or visual artifact.";
        }
        if (query.contains("ocr")) {
            return "ML Kit OCR extracts printed characters including denomination numerals and 'Reserve Bank of India'. If the detected text fails to align with the visual denomination, confidence drops to prevent misclassification.";
        }
        if (query.contains("suspicious") || query.contains("what does suspicious mean")) {
            return "A 'Suspicious' result means the AI observed deviations—such as unusual color histograms, missing security thread contrast, or blur—that prevent confirming genuineness. We recommend taking a second scan under better lighting or consulting a bank.";
        }
        if (query.contains("retake")) {
            return "You should retake the scan if there are strong reflections, dark shadows, or if the note is folded or tilted. A flat note on a dark, non-reflective surface produces the most reliable screening.";
        }
        return null;
    }

    private String getDefaultEducationalReply() {
        return "CurrencyGuard AI screens notes by cross-referencing visual geometry, ML Kit character recognition, watermark/security thread regions, and texture anomaly models. Ensure the note is uncreased and evenly illuminated for the best evaluation.";
    }
}
