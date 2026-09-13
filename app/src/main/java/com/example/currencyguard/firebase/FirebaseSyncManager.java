package com.example.currencyguard.firebase;

import android.content.Context;
import android.util.Log;

import com.example.currencyguard.model.ScanResult;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Cloud Firestore Synchronization Manager.
 * Automatically mirrors local Room database scan results
 * to the authenticated user's private cloud Firestore collection:
 * `users/{uid}/scans/{scanId}`.
 */
public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSyncManager";

    public interface SyncCallback {
        void onSyncSuccess(String cloudDocId);
        void onSyncFailed(String error);
    }

    public static void syncScanRecord(Context context, ScanResult scan, SyncCallback callback) {
        if (!FirebaseAuthManager.isFirebaseAvailable(context)) {
            Log.d(TAG, "Firebase not connected yet; scan saved locally in Room database.");
            if (callback != null) callback.onSyncFailed("Firebase not connected.");
            return;
        }

        FirebaseAuthManager.UserProfile profile = FirebaseAuthManager.getUserProfile(context);
        String uid = profile.getUid();
        if (uid == null || uid.isEmpty()) {
            if (callback != null) callback.onSyncFailed("User is not authenticated.");
            return;
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> data = new HashMap<>();
            data.put("id", scan.getId());
            data.put("timestamp", scan.getTimestamp());
            data.put("denomination", scan.getDenomination());
            data.put("currency", scan.getCurrency());
            data.put("confidence", scan.getConfidence());
            data.put("imageQuality", scan.getImageQuality());
            data.put("visualScore", scan.getVisualScore());
            data.put("securityScore", scan.getSecurityScore());
            data.put("ocrScore", scan.getOcrScore());
            data.put("geometryScore", scan.getGeometryScore());
            data.put("anomalyScore", scan.getAnomalyScore());
            data.put("status", scan.getStatus());
            data.put("aiExplanation", scan.getAiExplanation());
            data.put("uncertaintyReasons", scan.getUncertaintyReasonsJson());

            String docId = (scan.getId() > 0) ? String.valueOf(scan.getId()) : "scan_" + scan.getTimestamp();

            db.collection("users")
                    .document(uid)
                    .collection("scans")
                    .document(docId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Scan synced successfully to Firestore: " + docId);
                        if (callback != null) callback.onSyncSuccess(docId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore sync error: " + e.getMessage());
                        if (callback != null) callback.onSyncFailed(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Firebase Firestore error: " + e.getMessage());
            if (callback != null) callback.onSyncFailed(e.getMessage());
        }
    }
}
