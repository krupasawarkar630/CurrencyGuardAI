package com.example.currencyguard.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.currencyguard.database.AppDatabase;
import com.example.currencyguard.database.ScanResultDao;
import com.example.currencyguard.model.ScanResult;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository pattern implementation coordinating Room Database operations
 * and background thread execution.
 */
public class ScanRepository {

    private final ScanResultDao scanResultDao;
    private final ExecutorService executorService;

    public interface OnInsertCallback {
        void onInserted(long id);
    }

    public interface OnDashboardStatsCallback {
        void onStatsLoaded(int total, double avgConfidence, int genuine, int suspicious, List<ScanResult> recentScans);
    }

    private final Application application;

    public ScanRepository(Application application) {
        this.application = application;
        AppDatabase database = AppDatabase.getInstance(application);
        this.scanResultDao = database.scanResultDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void insert(ScanResult scanResult, OnInsertCallback callback) {
        executorService.execute(() -> {
            long id = scanResultDao.insert(scanResult);
            scanResult.setId(id);
            // Synchronize scan record to Firebase Cloud Firestore
            com.example.currencyguard.firebase.FirebaseSyncManager.syncScanRecord(application, scanResult, null);
            if (callback != null) {
                callback.onInserted(id);
            }
        });
    }

    public void delete(ScanResult scanResult) {
        executorService.execute(() -> scanResultDao.delete(scanResult));
    }

    public void deleteAll() {
        executorService.execute(scanResultDao::deleteAll);
    }

    public LiveData<List<ScanResult>> getAllScans() {
        return scanResultDao.getAllScans();
    }

    public LiveData<List<ScanResult>> getRecentScans(int limit) {
        return scanResultDao.getRecentScans(limit);
    }

    public LiveData<List<ScanResult>> getScansByStatus(String status) {
        return scanResultDao.getScansByStatus(status);
    }

    public List<ScanResult> getScansByDenominationSync(String denomination) {
        Future<List<ScanResult>> future = executorService.submit(() ->
                scanResultDao.getScansByDenominationSync(denomination));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return new java.util.ArrayList<>();
        }
    }

    public ScanResult getScanByIdSync(long id) {
        Future<ScanResult> future = executorService.submit(() -> scanResultDao.getScanById(id));
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }

    public LiveData<Integer> getTotalScanCount() {
        return scanResultDao.getTotalScanCount();
    }

    public LiveData<Double> getAverageConfidence() {
        return scanResultDao.getAverageConfidence();
    }

    public LiveData<Integer> getGenuineCount() {
        return scanResultDao.getGenuineCount();
    }

    public LiveData<Integer> getSuspiciousOrFakeCount() {
        return scanResultDao.getSuspiciousOrFakeCount();
    }

    /**
     * Synchronously retrieves all dashboard statistics on a background thread
     * and returns them via callback for instant UI synchronization on onResume().
     */
    public void getDashboardStats(OnDashboardStatsCallback callback) {
        executorService.execute(() -> {
            int total = scanResultDao.getTotalScanCountSync();
            Double avg = scanResultDao.getAverageConfidenceSync();
            int genuine = scanResultDao.getGenuineCountSync();
            int suspicious = scanResultDao.getSuspiciousOrFakeCountSync();
            List<ScanResult> recent = scanResultDao.getRecentScansSync(5);
            if (callback != null) {
                callback.onStatsLoaded(total, avg != null ? avg : 0.0, genuine, suspicious, recent);
            }
        });
    }

    public LiveData<List<ScanResult>> searchWallet(String query) {
        return scanResultDao.searchWallet(query);
    }

    public LiveData<List<ScanResult>> getScansByRiskLevel(String riskLevel) {
        return scanResultDao.getScansByRiskLevel(riskLevel);
    }
}
