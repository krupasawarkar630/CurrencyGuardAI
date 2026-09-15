package com.example.currencyguard.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.currencyguard.R;
import com.example.currencyguard.ai.AiVoiceManager;
import com.example.currencyguard.camera.CameraXHelper;
import com.example.currencyguard.camera.SmartScanGuide;
import com.example.currencyguard.utils.ImageUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;

/**
 * CameraX scanning viewfinder activity with live SmartScanGuide overlay,
 * AI Voice live guidance cues, torch toggle, gallery import, and 1-tap capture analysis flow.
 */
public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 101;
    public static final String EXTRA_FRONT_PATH = "extra_front_path";
    public static final String EXTRA_SHOW_GUIDE = "extra_show_guide";

    private PreviewView previewView;
    private TextView tvSmartGuide;
    private TextView tvSideBadge;
    private MaterialButton btnFlipSide;
    private FloatingActionButton btnShutter;
    private ImageView btnFlash;
    private ImageView btnVoiceGuideToggle;

    private CameraXHelper cameraXHelper;
    private AiVoiceManager voiceManager;
    private boolean isTorchOn = false;
    private boolean isFrontSide = true;
    private boolean isDualSideMode = false;
    private boolean isVoiceGuideEnabled = true;
    private String frontImagePath = null;

    private String lastSpokenGuidance = "";
    private long lastGuidanceSpokenTime = 0;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    if (voiceManager != null) {
                        voiceManager.stop();
                    }
                    Intent intent = new Intent(ScanActivity.this, AnalysisActivity.class);
                    intent.putExtra(AnalysisActivity.EXTRA_IMAGE_URI, uri.toString());
                    intent.putExtra(AnalysisActivity.EXTRA_SIDE, "Front");
                    startActivity(intent);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        previewView = findViewById(R.id.preview_view);
        tvSmartGuide = findViewById(R.id.tv_smart_guide);
        tvSideBadge = findViewById(R.id.tv_side_badge);
        btnFlipSide = findViewById(R.id.btn_flip_side);
        btnShutter = findViewById(R.id.btn_shutter);
        btnFlash = findViewById(R.id.btn_flash_toggle);
        btnVoiceGuideToggle = findViewById(R.id.btn_voice_guide_toggle);

        voiceManager = AiVoiceManager.getInstance(this);
        isVoiceGuideEnabled = AiVoiceManager.isVoiceGuidanceEnabled(this);
        updateVoiceGuideButton();

        // Check if opened directly to scan the back side
        String passedFrontPath = getIntent().getStringExtra(EXTRA_FRONT_PATH);
        if (passedFrontPath != null && !passedFrontPath.isEmpty()) {
            frontImagePath = passedFrontPath;
            isDualSideMode = true;
            isFrontSide = false;
            updateSideUI();
            Toast.makeText(this, "Scanning back side. Align banknote within guide.", Toast.LENGTH_LONG).show();
            if (isVoiceGuideEnabled && voiceManager != null) {
                voiceManager.speakText("Front captured. Now align the back side of the banknote.");
            }
        }

        findViewById(R.id.btn_close_scan).setOnClickListener(v -> finish());

        btnFlash.setOnClickListener(v -> {
            isTorchOn = !isTorchOn;
            cameraXHelper.toggleFlash();
            btnFlash.setImageResource(isTorchOn ? R.drawable.ic_doubt : R.drawable.ic_security);
        });

        if (btnVoiceGuideToggle != null) {
            btnVoiceGuideToggle.setOnClickListener(v -> {
                isVoiceGuideEnabled = !isVoiceGuideEnabled;
                AiVoiceManager.setVoiceGuidanceEnabled(this, isVoiceGuideEnabled);
                updateVoiceGuideButton();
                if (!isVoiceGuideEnabled && voiceManager != null) {
                    voiceManager.stop();
                }
                Toast.makeText(this, isVoiceGuideEnabled ? "Voice Guide: Enabled" : "Voice Guide: Muted", Toast.LENGTH_SHORT).show();
            });
        }

        btnFlipSide.setOnClickListener(v -> {
            isDualSideMode = true;
            isFrontSide = !isFrontSide;
            updateSideUI();
        });

        findViewById(R.id.btn_gallery_pick).setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });

        btnShutter.setOnClickListener(v -> captureImage());

        cameraXHelper = new CameraXHelper();
        checkPermissionsAndStartCamera();
    }

    private void updateVoiceGuideButton() {
        if (btnVoiceGuideToggle != null) {
            btnVoiceGuideToggle.setImageResource(isVoiceGuideEnabled ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        }
    }

    private void updateSideUI() {
        if (isFrontSide) {
            tvSideBadge.setText("Front Side");
            btnFlipSide.setText("Scan Dual Side");
        } else {
            tvSideBadge.setText("Back Side (Dual-Side Verification)");
            btnFlipSide.setText("Back Side Mode");
        }
    }

    private void checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraXHelper.startCamera(this, this, previewView);
            startPeriodicGuidanceCheck();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    private void startPeriodicGuidanceCheck() {
        previewView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    try {
                        Bitmap previewBitmap = previewView.getBitmap();
                        if (previewBitmap != null) {
                            SmartScanGuide.GuidanceResult guidance = SmartScanGuide.evaluateFrame(previewBitmap);
                            String msg = guidance.getMessage();
                            tvSmartGuide.setText(msg);

                            // Speak guidance advice periodically
                            if (isVoiceGuideEnabled && voiceManager != null && msg != null) {
                                long now = System.currentTimeMillis();
                                if (now - lastGuidanceSpokenTime > 4500 && !msg.equals(lastSpokenGuidance)) {
                                    lastSpokenGuidance = msg;
                                    lastGuidanceSpokenTime = now;
                                    voiceManager.speakText(msg);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    previewView.postDelayed(this, 1200);
                }
            }
        }, 1000);
    }

    private void captureImage() {
        try {
            File photoFile = ImageUtils.createTempImageFile(this);
            btnShutter.setEnabled(false);

            if (isVoiceGuideEnabled && voiceManager != null) {
                voiceManager.speakText("Capturing note...");
            }

            cameraXHelper.takePhoto(this, photoFile, new CameraXHelper.CaptureCallback() {
                @Override
                public void onImageCaptured(File file) {
                    runOnUiThread(() -> {
                        btnShutter.setEnabled(true);
                        if (isDualSideMode && isFrontSide && frontImagePath == null) {
                            frontImagePath = file.getAbsolutePath();
                            Toast.makeText(ScanActivity.this, "Front side captured! Now align back side and tap Shutter.", Toast.LENGTH_LONG).show();
                            isFrontSide = false;
                            updateSideUI();
                            if (isVoiceGuideEnabled && voiceManager != null) {
                                voiceManager.speakText("Front captured. Now flip the note to the back side.");
                            }
                        } else {
                            // Immediately proceed to analysis!
                            if (isVoiceGuideEnabled && voiceManager != null) {
                                voiceManager.speakText("Analyzing currency note...");
                            }
                            Intent intent = new Intent(ScanActivity.this, AnalysisActivity.class);
                            intent.putExtra(AnalysisActivity.EXTRA_FRONT_PATH, frontImagePath != null ? frontImagePath : file.getAbsolutePath());
                            if (frontImagePath != null && !frontImagePath.equals(file.getAbsolutePath())) {
                                intent.putExtra(AnalysisActivity.EXTRA_BACK_PATH, file.getAbsolutePath());
                                intent.putExtra(AnalysisActivity.EXTRA_SIDE, "Both");
                            } else {
                                intent.putExtra(AnalysisActivity.EXTRA_SIDE, "Front");
                            }
                            startActivity(intent);
                            finish();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnShutter.setEnabled(true);
                        Toast.makeText(ScanActivity.this, "Capture failed: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            btnShutter.setEnabled(true);
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraXHelper.startCamera(this, this, previewView);
                startPeriodicGuidanceCheck();
            } else {
                Toast.makeText(this, "Camera permission is required for banknote screening.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
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
        cameraXHelper.shutdown();
    }
}
