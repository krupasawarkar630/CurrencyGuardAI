package com.example.currencyguard.firebase;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Manages User Authentication via Firebase Auth.
 * Features automatic detection of Firebase configuration with
 * secure persistent local session fallback if google-services.json
 * has not yet been connected by the developer.
 */
public class FirebaseAuthManager {

    private static final String PREF_AUTH = "currency_guard_auth_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_USER_PASSWORD = "user_password"; // only used in offline fallback

    public interface AuthCallback {
        void onSuccess(String userName, String email);
        void onFailure(String errorMessage);
    }

    public static boolean isFirebaseAvailable(Context context) {
        try {
            return !FirebaseApp.getApps(context).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isUserLoggedIn(Context context) {
        if (isFirebaseAvailable(context)) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) return true;
            } catch (Exception ignored) {}
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public static void register(Context context, String name, String email, String password, AuthCallback callback) {
        if (isFirebaseAvailable(context)) {
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates);
                                saveLocalSession(context, name, email, user.getUid());
                            }
                            if (callback != null) callback.onSuccess(name, email);
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onFailure(e.getMessage());
                        });
                return;
            } catch (Exception ignored) {}
        }

        // Offline / Local fallback mode before google-services.json is connected
        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        String existingEmail = prefs.getString(KEY_USER_EMAIL, null);
        if (existingEmail != null && existingEmail.equalsIgnoreCase(email)) {
            if (callback != null) callback.onFailure("An account with this email already exists.");
            return;
        }

        String mockUid = "local_usr_" + System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_PASSWORD, password)
                .putString(KEY_USER_UID, mockUid)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();

        if (callback != null) callback.onSuccess(name, email);
    }

    public static void login(Context context, String email, String password, AuthCallback callback) {
        if (isFirebaseAvailable(context)) {
            try {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Valued User";
                            String uid = (user != null) ? user.getUid() : "uid";
                            saveLocalSession(context, name, email, uid);
                            if (callback != null) callback.onSuccess(name, email);
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onFailure(e.getMessage());
                        });
                return;
            } catch (Exception ignored) {}
        }

        // Offline / Local fallback validation
        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        String savedEmail = prefs.getString(KEY_USER_EMAIL, "");
        String savedPassword = prefs.getString(KEY_USER_PASSWORD, "");
        String savedName = prefs.getString(KEY_USER_NAME, "Valued User");
        String savedUid = prefs.getString(KEY_USER_UID, "local_usr_001");

        if (savedEmail.equalsIgnoreCase(email) && savedPassword.equals(password)) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply();
            if (callback != null) callback.onSuccess(savedName, savedEmail);
        } else if (savedEmail.isEmpty()) {
            if (callback != null) callback.onFailure("No user account found. Please create an account first.");
        } else {
            if (callback != null) callback.onFailure("Invalid email or password. Please try again.");
        }
    }

    public static void logout(Context context) {
        if (isFirebaseAvailable(context)) {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {}
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    public static UserProfile getUserProfile(Context context) {
        if (isFirebaseAvailable(context)) {
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    return new UserProfile(
                            user.getDisplayName() != null ? user.getDisplayName() : "Authenticated User",
                            user.getEmail() != null ? user.getEmail() : "user@example.com",
                            user.getUid()
                    );
                }
            } catch (Exception ignored) {}
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        return new UserProfile(
                prefs.getString(KEY_USER_NAME, "Demo User"),
                prefs.getString(KEY_USER_EMAIL, "user@currencyguard.ai"),
                prefs.getString(KEY_USER_UID, "local_usr_001")
        );
    }

    private static void saveLocalSession(Context context, String name, String email, String uid) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_UID, uid)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public static class UserProfile {
        private final String name;
        private final String email;
        private final String uid;

        public UserProfile(String name, String email, String uid) {
            this.name = name;
            this.email = email;
            this.uid = uid;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getUid() { return uid; }
    }
}
