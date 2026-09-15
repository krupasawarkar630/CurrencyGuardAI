package com.example.currencyguard.whatsapp;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.currencyguard.ai.CurrencyVerificationEngine;
import com.example.currencyguard.ai.ICurrencyVerificationEngine;
import com.example.currencyguard.model.AnalysisResult;

/**
 * Webhook and API request handler for CurrencyGuard AI WhatsApp Bot.
 * Routes incoming images to the unified CurrencyVerificationEngine
 * and returns formatted markdown responses.
 */
public class WhatsAppWebhookHandler {

    public interface WhatsAppResponseCallback {
        void onResponseReady(String messageText, AnalysisResult result);
        void onError(String error);
    }

    /**
     * Process an incoming image from a simulated or real WhatsApp webhook.
     */
    public static void handleIncomingImage(Context context, Bitmap image, WhatsAppResponseCallback callback) {
        if (image == null) {
            callback.onError("No image received in webhook payload.");
            return;
        }

        CurrencyVerificationEngine.getInstance().verifyBanknote(
                context.getApplicationContext(),
                image,
                "Front",
                new ICurrencyVerificationEngine.VerificationCallback() {
                    @Override
                    public void onSuccess(AnalysisResult result) {
                        String formatted = WhatsAppMessageFormatter.formatWhatsAppResponse(result);
                        callback.onResponseReady(formatted, result);
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError("Engine verification error: " + error);
                    }
                }
        );
    }
}
