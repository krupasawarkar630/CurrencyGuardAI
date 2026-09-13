package com.example.currencyguard.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Manages user preferences including Theme (Light, Dark, System Default)
 * and In-App Language (English, Spanish, Hindi, French, German, Arabic).
 */
public class SettingsManager {

    private static final String PREF_NAME = "currency_guard_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LANGUAGE = "app_language";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public static final String[] SUPPORTED_LANG_CODES = {"en", "es", "hi", "fr", "de", "ar"};
    public static final String[] SUPPORTED_LANG_NAMES = {
            "English",
            "Español (Spanish)",
            "हिन्दी (Hindi)",
            "Français (French)",
            "Deutsch (German)",
            "العربية (Arabic)"
    };

    /**
     * Initializes saved theme and locale upon application startup.
     */
    public static void init(Context context) {
        applyTheme(getThemeMode(context));
        applyLanguage(context, getLanguage(context));
    }

    // --- Theme Management ---

    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme(themeMode);
    }

    public static void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // --- Language / Locale Management ---

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public static void setLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply();
        applyLanguage(context, langCode);
    }

    public static void applyLanguage(Context context, String langCode) {
        // Modern AndroidX AppCompat application locale API
        try {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(langCode);
            AppCompatDelegate.setApplicationLocales(appLocale);
        } catch (Exception ignored) {
        }

        // Configuration update fallback for older Android releases
        try {
            Locale locale = new Locale(langCode);
            Locale.setDefault(locale);
            Resources resources = context.getResources();
            Configuration config = resources.getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(new LocaleList(locale));
            } else {
                config.locale = locale;
            }
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        } catch (Exception ignored) {
        }
    }

    public static int getLanguageIndex(Context context) {
        String current = getLanguage(context);
        for (int i = 0; i < SUPPORTED_LANG_CODES.length; i++) {
            if (SUPPORTED_LANG_CODES[i].equalsIgnoreCase(current)) {
                return i;
            }
        }
        return 0;
    }
}
