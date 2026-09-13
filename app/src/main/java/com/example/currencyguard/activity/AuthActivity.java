package com.example.currencyguard.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyguard.R;
import com.example.currencyguard.firebase.FirebaseAuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Mandatory User Authentication Gate.
 * Users must create an account or sign in before using the application.
 */
public class AuthActivity extends AppCompatActivity {

    private boolean isRegisterMode = false;

    private MaterialButtonToggleGroup toggleAuthMode;
    private TextView tvFormTitle;
    private TextView tvFormSubtitle;
    private TextInputLayout tilName;
    private TextInputEditText etName;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private TextView tvAuthError;
    private MaterialButton btnAuthSubmit;
    private ProgressBar progressAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If user is already authenticated, jump directly to Main Dashboard
        if (FirebaseAuthManager.isUserLoggedIn(this)) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_auth);

        toggleAuthMode = findViewById(R.id.toggle_auth_mode);
        tvFormTitle = findViewById(R.id.tv_form_title);
        tvFormSubtitle = findViewById(R.id.tv_form_subtitle);
        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tvAuthError = findViewById(R.id.tv_auth_error);
        btnAuthSubmit = findViewById(R.id.btn_auth_submit);
        progressAuth = findViewById(R.id.progress_auth);

        toggleAuthMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_mode_register) {
                    switchToRegisterMode();
                } else {
                    switchToLoginMode();
                }
            }
        });

        btnAuthSubmit.setOnClickListener(v -> handleAuthSubmit());
    }

    private void switchToLoginMode() {
        isRegisterMode = false;
        tvFormTitle.setText("Welcome Back");
        tvFormSubtitle.setText("Sign in to access scanner and currency tools.");
        tilName.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(View.GONE);
        btnAuthSubmit.setText("Sign In");
        tvAuthError.setVisibility(View.GONE);
    }

    private void switchToRegisterMode() {
        isRegisterMode = true;
        tvFormTitle.setText("Create New Account");
        tvFormSubtitle.setText("Join CurrencyGuard AI for currency screening & cloud sync.");
        tilName.setVisibility(View.VISIBLE);
        tilConfirmPassword.setVisibility(View.VISIBLE);
        btnAuthSubmit.setText("Create Account");
        tvAuthError.setVisibility(View.GONE);
    }

    private void handleAuthSubmit() {
        tvAuthError.setVisibility(View.GONE);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address.");
            return;
        } else {
            tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters.");
            return;
        } else {
            tilPassword.setError(null);
        }

        if (isRegisterMode) {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                tilName.setError("Please enter your full name.");
                return;
            } else {
                tilName.setError(null);
            }

            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Passwords do not match.");
                return;
            } else {
                tilConfirmPassword.setError(null);
            }

            setLoading(true);
            FirebaseAuthManager.register(this, name, email, password, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(String userName, String userEmail) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(AuthActivity.this, "Welcome to CurrencyGuard AI, " + userName + "!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(errorMessage);
                    });
                }
            });

        } else {
            // Login Mode
            setLoading(true);
            FirebaseAuthManager.login(this, email, password, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(String userName, String userEmail) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(AuthActivity.this, "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(errorMessage);
                    });
                }
            });
        }
    }

    private void setLoading(boolean loading) {
        progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAuthSubmit.setEnabled(!loading);
        btnAuthSubmit.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void showError(String message) {
        tvAuthError.setText(message);
        tvAuthError.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
