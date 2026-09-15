package com.example.currencyguard.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.currencyguard.model.ScanResult;

import java.util.List;

/**
 * Data Access Object for Room operations on ScanResult records.
 * Provides both LiveData reactive streams and synchronous methods
 * for robust dashboard statistics updates.
 */
@Dao
public interface ScanResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ScanResult scanResult);

    @Update
    void update(ScanResult scanResult);

    @Delete
    void delete(ScanResult scanResult);

    @Query("DELETE FROM scan_results")
    void deleteAll();

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    LiveData<List<ScanResult>> getAllScans();

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<ScanResult>> getRecentScans(int limit);

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT :limit")
    List<ScanResult> getRecentScansSync(int limit);

    @Query("SELECT * FROM scan_results WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<ScanResult>> getScansByStatus(String status);

    @Query("SELECT * FROM scan_results WHERE denomination = :denomination ORDER BY timestamp ASC")
    List<ScanResult> getScansByDenominationSync(String denomination);

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    ScanResult getScanById(long id);

    @Query("SELECT COUNT(*) FROM scan_results")
    LiveData<Integer> getTotalScanCount();

    @Query("SELECT COUNT(*) FROM scan_results")
    int getTotalScanCountSync();

    @Query("SELECT AVG(confidence) FROM scan_results WHERE confidence > 0")
    LiveData<Double> getAverageConfidence();

    @Query("SELECT AVG(confidence) FROM scan_results WHERE confidence > 0")
    Double getAverageConfidenceSync();

    @Query("SELECT COUNT(*) FROM scan_results WHERE status = 'LIKELY_GENUINE'")
    LiveData<Integer> getGenuineCount();

    @Query("SELECT COUNT(*) FROM scan_results WHERE status = 'LIKELY_GENUINE'")
    int getGenuineCountSync();

    @Query("SELECT COUNT(*) FROM scan_results WHERE status != 'LIKELY_GENUINE'")
    LiveData<Integer> getSuspiciousOrFakeCount();

    @Query("SELECT COUNT(*) FROM scan_results WHERE status != 'LIKELY_GENUINE'")
    int getSuspiciousOrFakeCountSync();

    // Smart Wallet Queries
    @Query("SELECT * FROM scan_results WHERE denomination LIKE '%' || :query || '%' OR serialNumber LIKE '%' || :query || '%' OR verificationId LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    LiveData<List<ScanResult>> searchWallet(String query);

    @Query("SELECT * FROM scan_results WHERE riskLevel = :riskLevel ORDER BY timestamp DESC")
    LiveData<List<ScanResult>> getScansByRiskLevel(String riskLevel);
}
