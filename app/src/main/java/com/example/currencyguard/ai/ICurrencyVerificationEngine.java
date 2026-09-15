package com.example.currencyguard.ai;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.currencyguard.model.AnalysisResult;

/**
 * Common Verification Engine Contract for CurrencyGuard AI.
 * Shared directly between the Android application and the WhatsApp Verification Bot
 * to guarantee identical risk scoring, explainability, and audit trails.
 */
public interface ICurrencyVerificationEngine {

    interface VerificationCallback {
        void onSuccess(AnalysisResult result);
        void onError(String error);
    }

    /**
     * Perform single-side currency screening.
     */
    void verifyBanknote(Context context, Bitmap image, String side, VerificationCallback callback);

    /**
     * Perform full two-sided currency verification.
     */
    void verifyDualSided(Context context, Bitmap frontImage, Bitmap backImage, VerificationCallback callback);
}
