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
 * RecyclerView adapter for scan records in History and Home Recent Scans.
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

        holder.tvDenom.setText(item.getDenomination() + " (" + item.getCurrency() + ")");

        String dateStr = new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US).format(new Date(item.getTimestamp()));
        holder.tvDate.setText(dateStr);

        holder.tvConfidence.setText((int) item.getConfidence() + "%");

        String status = item.getStatus() != null ? item.getStatus() : "LIKELY_GENUINE";
        if (status.contains("NOT_A_CURRENCY") || status.contains("NOT_CURRENCY")) {
            holder.tvStatusBadge.setText("🚫 NOT A CURRENCY");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#D32F2F"));
            holder.tvConfidence.setText("N/A");
            holder.tvConfidence.setTextColor(Color.parseColor("#D32F2F"));
        } else if (status.contains("GENUINE")) {
            holder.tvStatusBadge.setText("🛡️ REAL (Genuine)");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#00C853"));
            holder.tvConfidence.setTextColor(Color.parseColor("#00C853"));
        } else if (status.contains("SUSPICIOUS")) {
            holder.tvStatusBadge.setText("⚠️ SUSPICIOUS");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#FFB300"));
            holder.tvConfidence.setTextColor(Color.parseColor("#FFB300"));
        } else if (status.contains("FAKE")) {
            holder.tvStatusBadge.setText("🚫 FAKE (Counterfeit)");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#F44336"));
            holder.tvConfidence.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.tvStatusBadge.setText("❓ UNVERIFIED");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#78909C"));
            holder.tvConfidence.setTextColor(Color.parseColor("#78909C"));
        }

        // Load thumbnail if available
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
        final TextView tvDate;
        final TextView tvStatusBadge;
        final TextView tvConfidence;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_item_thumbnail);
            tvDenom = itemView.findViewById(R.id.tv_item_denomination);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvStatusBadge = itemView.findViewById(R.id.tv_item_status_badge);
            tvConfidence = itemView.findViewById(R.id.tv_item_confidence);
        }
    }
}
