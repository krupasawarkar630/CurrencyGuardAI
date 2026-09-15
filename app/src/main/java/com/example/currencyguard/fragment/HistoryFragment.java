package com.example.currencyguard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.HistoryDetailActivity;
import com.example.currencyguard.activity.ScanActivity;
import com.example.currencyguard.adapter.ScanHistoryAdapter;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.repository.ScanRepository;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Smart Note Wallet & Audit Records Fragment.
 * Organizes scanned banknotes with search, risk filters, serial lookups,
 * and comprehensive verification passport inspection.
 */
public class HistoryFragment extends Fragment {

    private ScanRepository scanRepository;
    private ScanHistoryAdapter historyAdapter;
    private View layoutEmptyState;
    private TextView tvSavedCountBadge;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private List<ScanResult> allResults = new ArrayList<>();
    private String currentFilter = "ALL";
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        scanRepository = new ScanRepository(requireActivity().getApplication());

        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvSavedCountBadge = view.findViewById(R.id.tv_saved_count_badge);
        etSearch = view.findViewById(R.id.et_wallet_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        RecyclerView rvHistory = view.findViewById(R.id.rv_history);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filters);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new ScanHistoryAdapter(getContext(), scanResult -> {
            Intent intent = new Intent(getActivity(), HistoryDetailActivity.class);
            intent.putExtra(HistoryDetailActivity.EXTRA_SCAN_ID, scanResult.getId());
            startActivity(intent);
        });
        rvHistory.setAdapter(historyAdapter);

        // Scan Now button in empty state
        View btnScanNow = view.findViewById(R.id.btn_wallet_scan_now);
        if (btnScanNow != null) {
            btnScanNow.setOnClickListener(v -> startActivity(new Intent(getActivity(), ScanActivity.class)));
        }

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        // Filter chips listener
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_low_risk)) {
                currentFilter = "LOW_RISK";
            } else if (checkedIds.contains(R.id.chip_suspicious)) {
                currentFilter = "SUSPICIOUS";
            } else if (checkedIds.contains(R.id.chip_high_risk)) {
                currentFilter = "HIGH_RISK";
            } else if (checkedIds.contains(R.id.chip_dual_sided)) {
                currentFilter = "DUAL_SIDED";
            } else {
                currentFilter = "ALL";
            }
            applyFilterAndSearch();
        });

        scanRepository.getAllScans().observe(getViewLifecycleOwner(), scans -> {
            allResults = (scans != null) ? scans : new ArrayList<>();
            tvSavedCountBadge.setText(allResults.size() + (allResults.size() == 1 ? " Note" : " Notes"));
            applyFilterAndSearch();
        });

        return view;
    }

    private void applyFilterAndSearch() {
        List<ScanResult> filtered = new ArrayList<>();
        for (ScanResult item : allResults) {
            boolean matchesFilter = false;
            int risk = item.getRiskScore() > 0 ? item.getRiskScore() : (int) Math.max(5, Math.round(100.0 - item.getConfidence()));
            String status = item.getStatus() != null ? item.getStatus() : "";

            if ("ALL".equals(currentFilter)) {
                matchesFilter = true;
            } else if ("LOW_RISK".equals(currentFilter)) {
                matchesFilter = status.contains("LOW_RISK") || status.contains("GENUINE") || risk < 30;
            } else if ("SUSPICIOUS".equals(currentFilter)) {
                matchesFilter = status.contains("SUSPICIOUS") || (risk >= 30 && risk < 70);
            } else if ("HIGH_RISK".equals(currentFilter)) {
                matchesFilter = status.contains("HIGH_RISK") || status.contains("FAKE") || risk >= 70;
            } else if ("DUAL_SIDED".equals(currentFilter)) {
                matchesFilter = item.isDualSided() || (item.getBackImagePath() != null && !item.getBackImagePath().isEmpty());
            }

            if (!matchesFilter) continue;

            // Apply search query
            if (!currentSearchQuery.isEmpty()) {
                String denom = item.getDenomination() != null ? item.getDenomination().toLowerCase(Locale.ROOT) : "";
                String serial = item.getSerialNumber() != null ? item.getSerialNumber().toLowerCase(Locale.ROOT) : "";
                String vid = item.getVerificationId() != null ? item.getVerificationId().toLowerCase(Locale.ROOT) : "";
                if (!denom.contains(currentSearchQuery) && !serial.contains(currentSearchQuery) && !vid.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filtered.add(item);
        }

        historyAdapter.setItems(filtered);
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
