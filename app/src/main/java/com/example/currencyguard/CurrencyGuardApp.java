package com.example.currencyguard;

import android.app.Application;

import com.example.currencyguard.utils.SettingsManager;

/**
 * Custom Application class for CurrencyGuard AI.
 * Initializes persistent Theme (Light/Dark/System) and preferred Language.
 */
public class CurrencyGuardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Theme and Locale from saved preferences
        SettingsManager.init(this);
    }
}
