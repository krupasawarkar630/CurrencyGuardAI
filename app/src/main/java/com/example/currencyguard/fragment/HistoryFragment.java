package com.example.currencyguard.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.HistoryDetailActivity;
import com.example.currencyguard.adapter.ScanHistoryAdapter;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.repository.ScanRepository;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private ScanRepository scanRepository;
    private ScanHistoryAdapter historyAdapter;
    private TextView tvEmptyState;
    private List<ScanResult> allResults = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        scanRepository = new ScanRepository(requireActivity().getApplication());

        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        RecyclerView rvHistory = view.findViewById(R.id.rv_history);
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filters);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new ScanHistoryAdapter(getContext(), scanResult -> {
            Intent intent = new Intent(getActivity(), HistoryDetailActivity.class);
            intent.putExtra(HistoryDetailActivity.EXTRA_SCAN_ID, scanResult.getId());
            startActivity(intent);
        });
        rvHistory.setAdapter(historyAdapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_genuine)) {
                currentFilter = "LIKELY_GENUINE";
            } else if (checkedIds.contains(R.id.chip_suspicious)) {
                currentFilter = "SUSPICIOUS";
            } else if (checkedIds.contains(R.id.chip_fake)) {
                currentFilter = "LIKELY_FAKE";
            } else if (checkedIds.contains(R.id.chip_unverified)) {
                currentFilter = "UNABLE_TO_VERIFY";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });

        scanRepository.getAllScans().observe(getViewLifecycleOwner(), scans -> {
            allResults = (scans != null) ? scans : new ArrayList<>();
            applyFilter();
        });

        return view;
    }

    private void applyFilter() {
        List<ScanResult> filtered = new ArrayList<>();
        for (ScanResult item : allResults) {
            if ("ALL".equals(currentFilter)) {
                filtered.add(item);
            } else if (item.getStatus() != null && item.getStatus().equalsIgnoreCase(currentFilter)) {
                filtered.add(item);
            }
        }

        historyAdapter.setItems(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
