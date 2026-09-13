package com.example.currencyguard.activity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.adapter.ChatAdapter;
import com.example.currencyguard.ai.AiVoiceManager;
import com.example.currencyguard.ai.ChatAssistantService;
import com.example.currencyguard.model.ChatMessage;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Educational Conversational Assistant Activity ("Ask CurrencyGuard AI")
 * with Voice-in (Speech-to-Text) and Voice-out (Text-to-Speech) capabilities.
 */
public class ChatAssistantActivity extends AppCompatActivity {

    private ChatAdapter chatAdapter;
    private RecyclerView rvMessages;
    private EditText etInput;
    private ImageView btnVoiceToggleChat;
    private ImageView btnMicInput;

    private ChatAssistantService chatService;
    private AiVoiceManager voiceManager;
    private boolean isVoiceSpeechEnabled = true;

    private final ActivityResultLauncher<Intent> speechRecognitionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenQuery = matches.get(0);
                        etInput.setText(spokenQuery);
                        sendMessage(spokenQuery);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        findViewById(R.id.btn_back_chat).setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rv_chat_messages);
        etInput = findViewById(R.id.et_chat_input);
        ImageView btnSend = findViewById(R.id.btn_send_chat);
        btnVoiceToggleChat = findViewById(R.id.btn_voice_toggle_chat);
        btnMicInput = findViewById(R.id.btn_mic_input);

        chatService = new ChatAssistantService();
        voiceManager = AiVoiceManager.getInstance(this);
        chatAdapter = new ChatAdapter();

        chatAdapter.setOnSpeakClickListener(text -> {
            if (voiceManager != null) {
                if (voiceManager.isSpeaking()) {
                    voiceManager.stop();
                } else {
                    voiceManager.speakText(text);
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);

        // Initial welcome message
        chatAdapter.addMessage(new ChatMessage(
                "Hello! I am CurrencyGuard AI Assistant. You can ask me by typing or speaking about currency security features, why AI confidence was reduced, or what our heatmap indicates.",
                false
        ));

        btnSend.setOnClickListener(v -> sendMessage(etInput.getText().toString()));

        // Speech-to-text mic input button
        if (btnMicInput != null) {
            btnMicInput.setOnClickListener(v -> startVoiceRecognition());
        }

        // Voice output toggle button in top bar
        if (btnVoiceToggleChat != null) {
            updateVoiceToggleButton();
            btnVoiceToggleChat.setOnClickListener(v -> {
                isVoiceSpeechEnabled = !isVoiceSpeechEnabled;
                updateVoiceToggleButton();
                if (!isVoiceSpeechEnabled && voiceManager != null) {
                    voiceManager.stop();
                }
                Toast.makeText(this, isVoiceSpeechEnabled ? "AI Voice Response: Enabled" : "AI Voice Response: Muted", Toast.LENGTH_SHORT).show();
            });
        }

        // Suggested Chips
        findViewById(R.id.chip_why_low).setOnClickListener(v -> sendMessage("Why is my confidence score low?"));
        findViewById(R.id.chip_watermark).setOnClickListener(v -> sendMessage("What is a watermark and how to verify it?"));
        findViewById(R.id.chip_heatmap).setOnClickListener(v -> sendMessage("Explain what the AI attention heatmap means."));
        findViewById(R.id.chip_ocr).setOnClickListener(v -> sendMessage("How does OCR help in fake note screening?"));
    }

    private void updateVoiceToggleButton() {
        if (btnVoiceToggleChat != null) {
            btnVoiceToggleChat.setImageResource(isVoiceSpeechEnabled ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        }
    }

    private void startVoiceRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask CurrencyGuard AI a question...");
            speechRecognitionLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        String cleanText = text.trim();
        chatAdapter.addMessage(new ChatMessage(cleanText, true));
        etInput.setText("");
        rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

        chatService.ask(this, cleanText, reply -> {
            chatAdapter.addMessage(new ChatMessage(reply, false));
            rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

            // Automatically speak response if voice output is enabled
            if (isVoiceSpeechEnabled && voiceManager != null) {
                voiceManager.speakText(reply);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceManager != null) {
            voiceManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.stop();
        }
    }
}
