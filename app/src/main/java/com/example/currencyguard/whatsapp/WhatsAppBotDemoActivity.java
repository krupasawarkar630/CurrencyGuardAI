package com.example.currencyguard.whatsapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.AnalysisActivity;
import com.example.currencyguard.activity.HistoryDetailActivity;
import com.example.currencyguard.activity.MainActivity;
import com.example.currencyguard.model.AnalysisResult;
import com.example.currencyguard.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Interactive WhatsApp Bot Demonstration Activity.
 * Allows hackathon juries, clients, and auditors to test sending a banknote photo
 * and experience the exact automated WhatsApp screening response in real-time
 * using the unified CurrencyGuard verification engine.
 */
public class WhatsAppBotDemoActivity extends AppCompatActivity {

    private ImageView ivSentNote;
    private View cardUserMessage;
    private View cardBotMessage;
    private View layoutTyping;
    private TextView tvBotResponse;
    private TextView tvUserTime;
    private TextView tvBotTime;
    private AnalysisResult lastResult;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whatsapp_bot_demo);

        ivSentNote = findViewById(R.id.iv_sent_note);
        cardUserMessage = findViewById(R.id.card_user_message);
        cardBotMessage = findViewById(R.id.card_bot_message);
        layoutTyping = findViewById(R.id.layout_typing);
        tvBotResponse = findViewById(R.id.tv_bot_response);
        tvUserTime = findViewById(R.id.tv_user_time);
        tvBotTime = findViewById(R.id.tv_bot_time);

        findViewById(R.id.btn_back_whatsapp).setOnClickListener(v -> finish());

        findViewById(R.id.btn_pick_test_image).setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        findViewById(R.id.btn_open_full_audit).setOnClickListener(v -> {
            Intent intent = new Intent(WhatsAppBotDemoActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void processSelectedImage(Uri uri) {
        Bitmap bitmap = ImageUtils.loadBitmapFromUri(this, uri);
        if (bitmap == null) {
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentTime = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        ivSentNote.setImageBitmap(bitmap);
        tvUserTime.setText(currentTime + " ✓✓");
        cardUserMessage.setVisibility(View.VISIBLE);
        cardBotMessage.setVisibility(View.GONE);
        layoutTyping.setVisibility(View.VISIBLE);

        // Simulate real-world network round-trip delay then run unified engine
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            WhatsAppWebhookHandler.handleIncomingImage(this, bitmap, new WhatsAppWebhookHandler.WhatsAppResponseCallback() {
                @Override
                public void onResponseReady(String messageText, AnalysisResult result) {
                    runOnUiThread(() -> {
                        lastResult = result;
                        layoutTyping.setVisibility(View.GONE);
                        tvBotResponse.setText(messageText);
                        tvBotTime.setText(new SimpleDateFormat("h:mm a", Locale.US).format(new Date()));
                        cardBotMessage.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        layoutTyping.setVisibility(View.GONE);
                        tvBotResponse.setText("❌ Verification error: " + error);
                        cardBotMessage.setVisibility(View.VISIBLE);
                    });
                }
            });
        }, 1200);
    }
}
