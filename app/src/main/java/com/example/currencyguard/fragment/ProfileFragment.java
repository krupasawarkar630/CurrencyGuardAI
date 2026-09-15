package com.example.currencyguard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.PrivacyPolicyActivity;
import com.example.currencyguard.ai.GeminiExplanationService;
import com.example.currencyguard.ml.TensorFlowCurrencyClassifier;
import com.example.currencyguard.repository.ScanRepository;
import com.example.currencyguard.utils.SettingsManager;

/**
 * Settings & System Fragment.
 * Handles:
 * 1. Appearance & Theme (Dark, Light, System Default)
 * 2. In-App Language selection (English, Spanish, Hindi, French, German, Arabic)
 * 3. Privacy Policy & Security guarantee
 * 4. AI Engine configuration & Gemini API key
 * 5. Data management & Clear history
 */
public class ProfileFragment extends Fragment {

    private ScanRepository scanRepository;
    private EditText etGeminiKey;
    private TextView tvActiveModel;
    private TextView tvCurrentLanguage;
    private RadioGroup rgTheme;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        scanRepository = new ScanRepository(requireActivity().getApplication());

        etGeminiKey = view.findViewById(R.id.et_gemini_key);
        tvActiveModel = view.findViewById(R.id.tv_active_model);
        tvCurrentLanguage = view.findViewById(R.id.tv_current_language);
        rgTheme = view.findViewById(R.id.rg_theme);

        setupUserProfile(view);
        setupThemeSelector();
        setupLanguageSelector(view);
        setupPrivacyPolicyButton(view);
        setupAiVoiceSettings(view);
        setupAiModelAndKey(view);
        setupClearHistoryButton(view);

        return view;
    }

    private void setupUserProfile(View view) {
        TextView tvProfileName = view.findViewById(R.id.tv_profile_name);
        TextView tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        TextView tvFirebaseStatus = view.findViewById(R.id.tv_profile_firebase_status);

        com.example.currencyguard.firebase.FirebaseAuthManager.UserProfile profile =
                com.example.currencyguard.firebase.FirebaseAuthManager.getUserProfile(requireContext());

        if (profile != null) {
            tvProfileName.setText(profile.getName());
            tvProfileEmail.setText(profile.getEmail());
        }

        if (com.example.currencyguard.firebase.FirebaseAuthManager.isFirebaseAvailable(requireContext())) {
            tvFirebaseStatus.setText("☁️ Firebase Cloud Sync Active");
            tvFirebaseStatus.setTextColor(getResources().getColor(R.color.status_genuine, null));
        } else {
            tvFirebaseStatus.setText("🔒 Secured Local Session (Firebase Ready)");
            tvFirebaseStatus.setTextColor(getResources().getColor(R.color.primary, null));
        }

        view.findViewById(R.id.btn_share_app_profile).setOnClickListener(v -> {
            com.example.currencyguard.utils.ShareUtils.shareApp(requireContext());
        });

        view.findViewById(R.id.btn_sign_out).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out of CurrencyGuard AI?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        com.example.currencyguard.firebase.FirebaseAuthManager.logout(requireContext());
                        Toast.makeText(getContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireActivity(), com.example.currencyguard.activity.AuthActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /**
     * Sets up Theme options: Light, Dark, System Default.
     */
    private void setupThemeSelector() {
        int currentTheme = SettingsManager.getThemeMode(requireContext());
        if (currentTheme == SettingsManager.THEME_LIGHT) {
            rgTheme.check(R.id.rb_theme_light);
        } else if (currentTheme == SettingsManager.THEME_DARK) {
            rgTheme.check(R.id.rb_theme_dark);
        } else {
            rgTheme.check(R.id.rb_theme_system);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedTheme;
            if (checkedId == R.id.rb_theme_light) {
                selectedTheme = SettingsManager.THEME_LIGHT;
            } else if (checkedId == R.id.rb_theme_dark) {
                selectedTheme = SettingsManager.THEME_DARK;
            } else {
                selectedTheme = SettingsManager.THEME_SYSTEM;
            }

            if (selectedTheme != SettingsManager.getThemeMode(requireContext())) {
                SettingsManager.setThemeMode(requireContext(), selectedTheme);
                Toast.makeText(getContext(), getString(R.string.theme_changed_toast), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up Language switcher with interactive SingleChoice dialog.
     */
    private void setupLanguageSelector(View view) {
        updateLanguageDisplay();

        View.OnClickListener langClick = v -> {
            int currentIndex = SettingsManager.getLanguageIndex(requireContext());
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.select_language))
                    .setSingleChoiceItems(SettingsManager.SUPPORTED_LANG_NAMES, currentIndex, (dialog, which) -> {
                        String selectedCode = SettingsManager.SUPPORTED_LANG_CODES[which];
                        SettingsManager.setLanguage(requireContext(), selectedCode);
                        dialog.dismiss();

                        Toast.makeText(getContext(), getString(R.string.language_changed_toast), Toast.LENGTH_SHORT).show();

                        // Recreate host activity to immediately reload all localized strings and resources
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        };

        view.findViewById(R.id.btn_change_language).setOnClickListener(langClick);
        tvCurrentLanguage.setOnClickListener(langClick);
    }

    private void updateLanguageDisplay() {
        int index = SettingsManager.getLanguageIndex(requireContext());
        if (index >= 0 && index < SettingsManager.SUPPORTED_LANG_NAMES.length) {
            tvCurrentLanguage.setText("🌐 " + SettingsManager.SUPPORTED_LANG_NAMES[index]);
        } else {
            tvCurrentLanguage.setText("🌐 English");
        }
    }

    /**
     * Opens the full-screen Privacy & Security Policy Activity.
     */
    private void setupPrivacyPolicyButton(View view) {
        view.findViewById(R.id.btn_view_privacy).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PrivacyPolicyActivity.class);
            startActivity(intent);
        });
    }

    private void setupAiVoiceSettings(View view) {
        com.google.android.material.switchmaterial.SwitchMaterial switchAutoSpeak = view.findViewById(R.id.switch_auto_speak);
        com.google.android.material.switchmaterial.SwitchMaterial switchVoiceGuide = view.findViewById(R.id.switch_voice_guidance);

        if (switchAutoSpeak != null) {
            switchAutoSpeak.setChecked(com.example.currencyguard.ai.AiVoiceManager.isAutoSpeakEnabled(requireContext()));
            switchAutoSpeak.setOnCheckedChangeListener((buttonView, isChecked) -> {
                com.example.currencyguard.ai.AiVoiceManager.setAutoSpeakEnabled(requireContext(), isChecked);
                Toast.makeText(getContext(), isChecked ? "Auto-speak enabled" : "Auto-speak disabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (switchVoiceGuide != null) {
            switchVoiceGuide.setChecked(com.example.currencyguard.ai.AiVoiceManager.isVoiceGuidanceEnabled(requireContext()));
            switchVoiceGuide.setOnCheckedChangeListener((buttonView, isChecked) -> {
                com.example.currencyguard.ai.AiVoiceManager.setVoiceGuidanceEnabled(requireContext(), isChecked);
                Toast.makeText(getContext(), isChecked ? "Camera voice guidance enabled" : "Camera voice guidance muted", Toast.LENGTH_SHORT).show();
            });
        }

        view.findViewById(R.id.btn_test_voice).setOnClickListener(v -> {
            com.example.currencyguard.ai.AiVoiceManager.getInstance(requireContext())
                    .speakText("Hello! CurrencyGuard AI Voice engine is active and ready for accessibility.");
            Toast.makeText(getContext(), "Testing AI Voice...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAiModelAndKey(View view) {
        String currentKey = com.example.currencyguard.ai.GeminiApiService.getApiKey(requireContext());
        if (currentKey != null && !currentKey.isEmpty()) {
            etGeminiKey.setText(currentKey);
        }

        // Active Verification Engine identity
        TensorFlowCurrencyClassifier classifier = new TensorFlowCurrencyClassifier(requireContext());
        if (classifier.isNativeModelLoaded()) {
            tvActiveModel.setText("Verification Engine: CurrencyGuard Ensemble v1.0 (TensorFlow Lite)");
        } else {
            tvActiveModel.setText("Verification Engine: CurrencyGuard Ensemble v1.0 (On-Device Heuristic & CV)");
        }
        classifier.close();

        // WhatsApp Bot Simulator launcher
        View btnWhatsappDemo = view.findViewById(R.id.btn_open_whatsapp_demo);
        if (btnWhatsappDemo != null) {
            btnWhatsappDemo.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.example.currencyguard.whatsapp.WhatsAppBotDemoActivity.class));
            });
        }

        // Model selector radio group (Google Gemini Models)
        RadioGroup rgModels = view.findViewById(R.id.rg_gemini_models);
        String currentModel = com.example.currencyguard.ai.GeminiApiService.getActiveModel(requireContext());
        if (rgModels != null) {
            if (com.example.currencyguard.ai.GeminiApiService.MODEL_GEMINI_15_FLASH.equalsIgnoreCase(currentModel)) {
                rgModels.check(R.id.rb_model_15_flash);
            } else if (com.example.currencyguard.ai.GeminiApiService.MODEL_GEMINI_15_PRO.equalsIgnoreCase(currentModel)) {
                rgModels.check(R.id.rb_model_15_pro);
            } else {
                rgModels.check(R.id.rb_model_20_flash);
            }

            rgModels.setOnCheckedChangeListener((group, checkedId) -> {
                String chosenModel = com.example.currencyguard.ai.GeminiApiService.MODEL_GEMINI_20_FLASH;
                if (checkedId == R.id.rb_model_15_flash) {
                    chosenModel = com.example.currencyguard.ai.GeminiApiService.MODEL_GEMINI_15_FLASH;
                } else if (checkedId == R.id.rb_model_15_pro) {
                    chosenModel = com.example.currencyguard.ai.GeminiApiService.MODEL_GEMINI_15_PRO;
                }
                com.example.currencyguard.ai.GeminiApiService.setActiveModel(requireContext(), chosenModel);
                Toast.makeText(getContext(), "Active model: " + chosenModel, Toast.LENGTH_SHORT).show();
            });
        }

        TextView tvKeyStatus = view.findViewById(R.id.tv_key_status);

        view.findViewById(R.id.btn_save_key).setOnClickListener(v -> {
            String key = etGeminiKey.getText().toString().trim();
            com.example.currencyguard.ai.GeminiApiService.saveApiKey(requireContext(), key);
            if (tvKeyStatus != null) {
                tvKeyStatus.setText(key.isEmpty() ? "API key cleared. Offline fallback active." : "Google Gemini API key saved.");
            }
            Toast.makeText(getContext(), "Google Gemini API key saved successfully.", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_test_key).setOnClickListener(v -> {
            String key = etGeminiKey.getText().toString().trim();
            if (key.isEmpty()) {
                if (tvKeyStatus != null) {
                    tvKeyStatus.setText("Cannot test: Key field is empty.");
                    tvKeyStatus.setTextColor(getResources().getColor(R.color.status_fake, null));
                }
                return;
            }
            if (tvKeyStatus != null) {
                tvKeyStatus.setText("Testing connection with Google Gemini...");
                tvKeyStatus.setTextColor(getResources().getColor(R.color.primary, null));
            }
            com.example.currencyguard.ai.GeminiApiService service = new com.example.currencyguard.ai.GeminiApiService();
            service.testApiKey(requireContext(), key, (success, message) -> {
                if (tvKeyStatus != null) {
                    tvKeyStatus.setText(message);
                    tvKeyStatus.setTextColor(getResources().getColor(success ? R.color.status_genuine : R.color.status_fake, null));
                }
            });
        });
    }

    private void setupClearHistoryButton(View view) {
        view.findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to permanently delete all scan records?")
                    .setPositiveButton("Delete All", (dialog, which) -> {
                        scanRepository.deleteAll();
                        Toast.makeText(getContext(), "All scan records cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
}
