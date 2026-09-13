package com.example.currencyguard.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.currencyguard.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Dedicated Privacy Policy & Security Activity.
 * Outlines 100% on-device ML execution, data protection, camera permissions,
 * and responsible AI usage.
 */
public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_privacy);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        findViewById(R.id.btn_close_privacy).setOnClickListener(v -> finish());
    }
}
