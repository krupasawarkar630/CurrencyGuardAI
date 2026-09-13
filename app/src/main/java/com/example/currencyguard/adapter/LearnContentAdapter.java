package com.example.currencyguard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.model.LearnTopic;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for educational currency security topics (Section 17).
 */
public class LearnContentAdapter extends RecyclerView.Adapter<LearnContentAdapter.ViewHolder> {

    private final List<LearnTopic> topics = new ArrayList<>();

    public void setTopics(List<LearnTopic> newTopics) {
        topics.clear();
        if (newTopics != null) {
            topics.addAll(newTopics);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_learn_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LearnTopic topic = topics.get(position);
        holder.tvTitle.setText(topic.getTitle());
        holder.tvTag.setText(topic.getCategory());
        holder.tvDesc.setText(topic.getDescription());
        holder.tvHowToCheck.setText(topic.getHowToCheck());
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvTag;
        final TextView tvDesc;
        final TextView tvHowToCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_learn_title);
            tvTag = itemView.findViewById(R.id.tv_learn_tag);
            tvDesc = itemView.findViewById(R.id.tv_learn_description);
            tvHowToCheck = itemView.findViewById(R.id.tv_learn_how_to_check);
        }
    }
}
