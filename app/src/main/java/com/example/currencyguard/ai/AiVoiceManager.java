package com.example.currencyguard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.example.currencyguard.model.AnalysisResult;

import java.util.Locale;
import java.util.UUID;

/**
 * Centralized AI Voice & Text-to-Speech Engine for CurrencyGuard AI.
 * Provides natural speech feedback for:
 * 1. Screening verdicts & forensic breakdowns (Real / Fake / Suspicious / Non-Currency)
 * 2. Conversational AI Chat replies
 * 3. Live camera scanning guidance cues
 */
public class AiVoiceManager {

    private static final String TAG = "AiVoiceManager";
    private static final String PREFS_NAME = "currency_guard_voice_prefs";
    private static final String KEY_AUTO_SPEAK = "voice_auto_speak_results";
    private static final String KEY_VOICE_GUIDE = "voice_live_guidance";

    private static volatile AiVoiceManager instance;

    private TextToSpeech tts;
    private boolean isInitialized = false;
    private final Handler mainHandler;
    private SpeechListener currentListener;

    public interface SpeechListener {
        void onSpeechStarted();
        void onSpeechCompleted();
        void onSpeechError(String error);
    }

    private AiVoiceManager(Context context) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTts(context.getApplicationContext());
    }

    public static synchronized AiVoiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new AiVoiceManager(context);
        }
        return instance;
    }

    private void initTts(Context appContext) {
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = tts.setLanguage(Locale.US);
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setPitch(1.02f); // Slightly natural, friendly pitch
                tts.setSpeechRate(0.96f); // Clear, articulate cadence for accessibility

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        mainHandler.post(() -> {
                            if (currentListener != null) {
                                currentListener.onSpeechStarted();
                            }
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        mainHandler.post(() -> {
                            if (currentListener != null) {
                                currentListener.onSpeechCompleted();
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        mainHandler.post(() -> {
                            if (currentListener != null) {
                                currentListener.onSpeechError("Speech synthesis error");
                            }
                        });
                    }
                });
                isInitialized = true;
                Log.d(TAG, "AI Voice TextToSpeech initialized successfully.");
            } else {
                Log.w(TAG, "Failed to initialize TextToSpeech: " + status);
            }
        });
    }

    public void speakText(String text, SpeechListener listener) {
        if (text == null || text.trim().isEmpty()) return;
        this.currentListener = listener;

        if (!isInitialized || tts == null) {
            if (listener != null) {
                listener.onSpeechError("Voice engine initializing. Please try again.");
            }
            return;
        }

        stop();

        String utteranceId = UUID.randomUUID().toString();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void speakText(String text) {
        speakText(text, null);
    }

    public void speakAnalysisResult(Context context, AnalysisResult result, SpeechListener listener) {
        if (result == null) return;
        String speechContent = buildResultSpeechScript(result);
        speakText(speechContent, listener);
    }

    private String buildResultSpeechScript(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        if (!result.isCurrencyNote()) {
            sb.append("Screening Alert. The scanned image was rejected and is not recognized as a currency note. ");
            sb.append("Please place an official banknote flat on a clean surface and scan again.");
            return sb.toString();
        }

        String denom = result.getDenomination() != null ? result.getDenomination() : "banknote";
        String currency = result.getCurrency() != null ? result.getCurrency() : "";
        int confidence = (int) Math.round(result.getFinalConfidence());

        if (result.getStatus().isReal()) {
            sb.append("Screening Verdict: Real Currency. ");
            sb.append("Identified ").append(denom).append(" ").append(currency).append(" note ");
            sb.append("with an overall confidence of ").append(confidence).append(" percent. ");
            sb.append("Security thread, Mahatma Gandhi watermark, and micro-typography conform to genuine specifications.");
        } else if (result.getStatus().isFake()) {
            sb.append("Screening Warning: Counterfeit note detected. ");
            sb.append("The scanned ").append(denom).append(" note is flagged as Counterfeit or Fake ");
            sb.append("with ").append(confidence).append(" percent confidence. ");
            sb.append("Key security features including the security thread or microprint failed verification. ");
            sb.append("Do not accept or circulate this note.");
        } else if (result.getStatus().isSuspicious()) {
            sb.append("Screening Notice: Suspicious note. ");
            sb.append("The scanned ").append(denom).append(" note scored ").append(confidence).append(" percent confidence. ");
            sb.append("Surface anomalies, reflections, or pattern discrepancies were observed. ");
            sb.append("A secondary scan under neutral lighting or verification at a banking institution is advised.");
        } else {
            sb.append("Screening Complete. ").append(denom).append(" note screened with ").append(confidence).append(" percent confidence.");
        }

        return sb.toString();
    }

    public void stop() {
        if (tts != null && isInitialized) {
            try {
                tts.stop();
            } catch (Exception ignored) {}
        }
        if (currentListener != null) {
            currentListener.onSpeechCompleted();
        }
    }

    public boolean isSpeaking() {
        return tts != null && isInitialized && tts.isSpeaking();
    }

    public static boolean isAutoSpeakEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_SPEAK, false);
    }

    public static void setAutoSpeakEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply();
    }

    public static boolean isVoiceGuidanceEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VOICE_GUIDE, true);
    }

    public static void setVoiceGuidanceEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_VOICE_GUIDE, enabled).apply();
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
            isInitialized = false;
        }
    }
}
