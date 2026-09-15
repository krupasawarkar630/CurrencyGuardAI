package com.example.currencyguard.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.activity.AnalysisActivity;
import com.example.currencyguard.activity.ChatAssistantActivity;
import com.example.currencyguard.activity.HistoryDetailActivity;
import com.example.currencyguard.activity.MainActivity;
import com.example.currencyguard.activity.ScanActivity;
import com.example.currencyguard.adapter.ScanHistoryAdapter;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.repository.ScanRepository;

import java.util.List;

/**
 * Main Dashboard Fragment.
 * Displays real-time summary statistics (Total Scans, Avg Confidence, Likely Genuine, Suspicious Notes)
 * and the Recent Screenings list with immediate live-refresh support on onResume().
 */
public class HomeFragment extends Fragment {

    private ScanRepository scanRepository;
    private ScanHistoryAdapter recentAdapter;

    private TextView tvTotalScans;
    private TextView tvAvgConfidence;
    private TextView tvGenuineCount;
    private TextView tvSuspiciousCount;
    private TextView tvEmptyHistory;
    private TextView tvHomeWalletCount;
    private RecyclerView rvRecentScans;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && getActivity() != null) {
                    Intent intent = new Intent(getActivity(), AnalysisActivity.class);
                    intent.putExtra(AnalysisActivity.EXTRA_IMAGE_URI, uri.toString());
                    intent.putExtra(AnalysisActivity.EXTRA_SIDE, "Front");
                    startActivity(intent);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        scanRepository = new ScanRepository(requireActivity().getApplication());

        tvTotalScans = view.findViewById(R.id.tv_total_scans);
        tvAvgConfidence = view.findViewById(R.id.tv_avg_confidence);
        tvGenuineCount = view.findViewById(R.id.tv_genuine_count);
        tvSuspiciousCount = view.findViewById(R.id.tv_suspicious_count);
        tvEmptyHistory = view.findViewById(R.id.tv_empty_history);
        tvHomeWalletCount = view.findViewById(R.id.tv_home_wallet_count);
        rvRecentScans = view.findViewById(R.id.rv_recent_scans);

        view.findViewById(R.id.btn_scan_camera).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ScanActivity.class));
        });

        view.findViewById(R.id.btn_upload_gallery).setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });



        View btnConverter = view.findViewById(R.id.btn_open_converter);
        if (btnConverter != null) {
            btnConverter.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.currencyguard.activity.CurrencyConverterActivity.class);
                intent.putExtra(com.example.currencyguard.activity.CurrencyConverterActivity.EXTRA_AMOUNT, 200.0);
                startActivity(intent);
            });
        }

        View cardWallet = view.findViewById(R.id.card_wallet_shortcut);
        if (cardWallet != null) {
            cardWallet.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bnv = getActivity().findViewById(R.id.bottom_navigation);
                    if (bnv != null) {
                        bnv.setSelectedItemId(R.id.nav_history);
                    }
                }
            });
        }

        View cardWhatsapp = view.findViewById(R.id.card_home_whatsapp_banner);
        if (cardWhatsapp != null) {
            cardWhatsapp.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), com.example.currencyguard.whatsapp.WhatsAppBotDemoActivity.class));
            });
        }

        View cardCopilot = view.findViewById(R.id.card_home_copilot);
        if (cardCopilot != null) {
            cardCopilot.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), ChatAssistantActivity.class));
            });
        }

        TextView tvWelcome = view.findViewById(R.id.tv_home_welcome_title);
        if (tvWelcome != null && getContext() != null) {
            com.example.currencyguard.firebase.FirebaseAuthManager.UserProfile user =
                    com.example.currencyguard.firebase.FirebaseAuthManager.getUserProfile(getContext());
            if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                String firstName = user.getName().split(" ")[0];
                tvWelcome.setText("Welcome, " + firstName + "! 👋");
            }
        }

        rvRecentScans.setLayoutManager(new LinearLayoutManager(getContext()));
        recentAdapter = new ScanHistoryAdapter(getContext(), scanResult -> {
            Intent intent = new Intent(getActivity(), HistoryDetailActivity.class);
            intent.putExtra(HistoryDetailActivity.EXTRA_SCAN_ID, scanResult.getId());
            startActivity(intent);
        });
        rvRecentScans.setAdapter(recentAdapter);

        observeDatabase();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Force synchronous dashboard metrics query whenever the user navigates back
        refreshDashboardDirectly();
    }

    private void observeDatabase() {
        scanRepository.getTotalScanCount().observe(getViewLifecycleOwner(), count -> {
            int total = count != null ? count : 0;
            tvTotalScans.setText(String.valueOf(total));
            if (tvHomeWalletCount != null) {
                tvHomeWalletCount.setText(total + (total == 1 ? " note saved in audit trail" : " notes saved in audit trail"));
            }
        });

        scanRepository.getAverageConfidence().observe(getViewLifecycleOwner(), avg -> {
            if (avg != null && avg > 0) {
                tvAvgConfidence.setText((int) Math.round(avg) + "%");
            } else {
                tvAvgConfidence.setText("--");
            }
        });

        scanRepository.getGenuineCount().observe(getViewLifecycleOwner(), count -> {
            tvGenuineCount.setText(String.valueOf(count != null ? count : 0));
        });

        scanRepository.getSuspiciousOrFakeCount().observe(getViewLifecycleOwner(), count -> {
            tvSuspiciousCount.setText(String.valueOf(count != null ? count : 0));
        });

        scanRepository.getRecentScans(5).observe(getViewLifecycleOwner(), this::updateRecentList);
    }

    private void refreshDashboardDirectly() {
        if (scanRepository == null || getActivity() == null) return;

        scanRepository.getDashboardStats((total, avgConfidence, genuine, suspicious, recentScans) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvTotalScans.setText(String.valueOf(total));
                    if (tvHomeWalletCount != null) {
                        tvHomeWalletCount.setText(total + (total == 1 ? " note saved in audit trail" : " notes saved in audit trail"));
                    }
                    if (avgConfidence > 0) {
                        tvAvgConfidence.setText((int) Math.round(avgConfidence) + "%");
                    } else {
                        tvAvgConfidence.setText("--");
                    }
                    tvGenuineCount.setText(String.valueOf(genuine));
                    tvSuspiciousCount.setText(String.valueOf(suspicious));
                    updateRecentList(recentScans);
                });
            }
        });
    }

    private void updateRecentList(List<ScanResult> scans) {
        if (scans != null && !scans.isEmpty()) {
            tvEmptyHistory.setVisibility(View.GONE);
            rvRecentScans.setVisibility(View.VISIBLE);
            recentAdapter.setItems(scans);
        } else {
            tvEmptyHistory.setVisibility(View.VISIBLE);
            rvRecentScans.setVisibility(View.GONE);
            recentAdapter.setItems(null);
        }
    }
}
