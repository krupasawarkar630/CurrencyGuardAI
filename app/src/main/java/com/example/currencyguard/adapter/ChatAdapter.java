package com.example.currencyguard.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.currencyguard.R;
import com.example.currencyguard.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for conversation bubbles in Ask CurrencyGuard AI.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public interface OnSpeakClickListener {
        void onSpeak(String text);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private OnSpeakClickListener speakClickListener;

    public void setOnSpeakClickListener(OnSpeakClickListener listener) {
        this.speakClickListener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (message.isUser()) {
            holder.containerUser.setVisibility(View.VISIBLE);
            holder.containerBot.setVisibility(View.GONE);
            holder.tvUserText.setText(message.getText());
        } else {
            holder.containerBot.setVisibility(View.VISIBLE);
            holder.containerUser.setVisibility(View.GONE);
            holder.tvBotText.setText(message.getText());

            if (holder.btnSpeakChat != null) {
                holder.btnSpeakChat.setOnClickListener(v -> {
                    if (speakClickListener != null) {
                        speakClickListener.onSpeak(message.getText());
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout containerBot;
        final LinearLayout containerUser;
        final TextView tvBotText;
        final TextView tvUserText;
        final View btnSpeakChat;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            containerBot = itemView.findViewById(R.id.container_bot);
            containerUser = itemView.findViewById(R.id.container_user);
            tvBotText = itemView.findViewById(R.id.tv_bot_text);
            tvUserText = itemView.findViewById(R.id.tv_user_text);
            btnSpeakChat = itemView.findViewById(R.id.btn_speak_chat);
        }
    }
}
