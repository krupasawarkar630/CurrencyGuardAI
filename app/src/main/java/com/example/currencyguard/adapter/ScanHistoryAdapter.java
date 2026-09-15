package com.example.currencyguard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.model.ScanResult;
import com.example.currencyguard.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for Smart Note Wallet and Screening History records.
 * Presents risk scores, confidence, serial numbers, and dual-side verification tags.
 */
public class ScanHistoryAdapter extends RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {

    public interface OnScanClickListener {
        void onScanClick(ScanResult scanResult);
    }

    private final Context context;
    private final List<ScanResult> items = new ArrayList<>();
    private final OnScanClickListener listener;

    public ScanHistoryAdapter(Context context, OnScanClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<ScanResult> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult item = items.get(position);

        holder.tvDenom.setText(item.getDenomination() + " " + item.getCurrency());

        String dateStr = new SimpleDateFormat("dd MMM • hh:mm a", Locale.US).format(new Date(item.getTimestamp()));
        holder.tvDate.setText(dateStr);

        // Serial Number
        String serial = item.getSerialNumber();
        if (serial != null && !serial.isEmpty() && !"Unclear".equalsIgnoreCase(serial)) {
            holder.tvSerial.setText("Serial: " + serial);
        } else {
            holder.tvSerial.setText("Serial: Not Detected");
        }

        // Dual-Side vs Single-Side Tag
        if (item.isDualSided() || (item.getBackImagePath() != null && !item.getBackImagePath().isEmpty())) {
            holder.tvSidesTag.setText("Dual-Sided");
            holder.tvSidesTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvSidesTag.setText("Single-Side");
            holder.tvSidesTag.setVisibility(View.VISIBLE);
        }

        // Risk & Confidence Display
        int risk = item.getRiskScore() > 0 ? item.getRiskScore() : (int) Math.max(5, Math.round(100.0 - item.getConfidence()));
        holder.tvRiskScore.setText(risk + "/100");
        holder.tvConfidence.setText("Conf: " + (int) item.getConfidence() + "%");

        String status = item.getStatus() != null ? item.getStatus() : "LOW_RISK";
        if (status.contains("NOT_A_CURRENCY") || status.contains("NOT_CURRENCY")) {
            holder.tvStatusBadge.setText("🚫 NOT A CURRENCY");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#D32F2F"));
            holder.tvRiskScore.setText("N/A");
            holder.tvRiskScore.setTextColor(Color.parseColor("#D32F2F"));
            holder.tvConfidence.setText("Confidence: 0%");
        } else if (status.contains("LOW_RISK") || status.contains("GENUINE") || risk < 30) {
            holder.tvStatusBadge.setText("🟢 LOW RISK");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#00C853"));
            holder.tvRiskScore.setTextColor(Color.parseColor("#00C853"));
        } else if (status.contains("SUSPICIOUS") || (risk >= 30 && risk < 70)) {
            holder.tvStatusBadge.setText("🟡 SUSPICIOUS");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#FFB300"));
            holder.tvRiskScore.setTextColor(Color.parseColor("#FFB300"));
        } else {
            holder.tvStatusBadge.setText("🔴 HIGH RISK");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F44336"));
            holder.tvRiskScore.setTextColor(Color.parseColor("#F44336"));
        }

        // Load thumbnail safely
        if (item.getFrontImagePath() != null) {
            Bitmap thumb = ImageUtils.loadAndCorrectOrientation(item.getFrontImagePath());
            if (thumb != null) {
                holder.ivThumbnail.setImageBitmap(thumb);
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.bg_reticle);
            }
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.bg_reticle);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onScanClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumbnail;
        final TextView tvDenom;
        final TextView tvSerial;
        final TextView tvSidesTag;
        final TextView tvDate;
        final TextView tvStatusBadge;
        final TextView tvRiskScore;
        final TextView tvConfidence;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_item_thumbnail);
            tvDenom = itemView.findViewById(R.id.tv_item_denomination);
            tvSerial = itemView.findViewById(R.id.tv_item_serial);
            tvSidesTag = itemView.findViewById(R.id.tv_item_sides_tag);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvStatusBadge = itemView.findViewById(R.id.tv_item_status_badge);
            tvRiskScore = itemView.findViewById(R.id.tv_item_risk_score);
            tvConfidence = itemView.findViewById(R.id.tv_item_confidence);
        }
    }
}
