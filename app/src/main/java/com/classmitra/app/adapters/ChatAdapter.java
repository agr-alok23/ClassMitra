package com.classmitra.app.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.classmitra.app.R;
import com.classmitra.app.models.ChatMessage;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;

import java.util.List;

/**
 * Modern Adapter for the Chat RecyclerView in Ask Mitra with Markdown support.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages;
    private Markwon markwon;
    private OnMessageActionListener actionListener;

    public interface OnMessageActionListener {
        void onRegenerate(int position);
    }

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
    }

    private void initMarkwon(Context context) {
        if (markwon == null) {
            markwon = Markwon.builder(context)
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TablePlugin.create(context))
                    .usePlugin(HtmlPlugin.create())
                    .build();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        initMarkwon(parent.getContext());
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_mitra, parent, false);
            return new MitraViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getMessage());
            userHolder.tvTimestamp.setText(message.getFormattedTime());
            
            if (message.getImage() != null) {
                userHolder.ivAttachment.setImageBitmap(message.getImage());
                userHolder.ivAttachment.setVisibility(View.VISIBLE);
            } else {
                userHolder.ivAttachment.setVisibility(View.GONE);
            }
        } else {
            MitraViewHolder mitraHolder = (MitraViewHolder) holder;
            
            if (message.isTyping()) {
                mitraHolder.tvMessage.setText("Mitra is typing...");
                mitraHolder.layoutActions.setVisibility(View.GONE);
                mitraHolder.tvTimestamp.setVisibility(View.INVISIBLE);
            } else {
                markwon.setMarkdown(mitraHolder.tvMessage, message.getMessage());
                mitraHolder.tvTimestamp.setText(message.getFormattedTime());
                mitraHolder.tvTimestamp.setVisibility(View.VISIBLE);

                // Show actions for the last AI message
                boolean isLastAiMessage = (position == messages.size() - 1);
                mitraHolder.layoutActions.setVisibility(isLastAiMessage ? View.VISIBLE : View.GONE);

                mitraHolder.btnHelpful.setOnClickListener(v -> {
                    Toast.makeText(v.getContext(), "Glad you found it helpful!", Toast.LENGTH_SHORT).show();
                });

                mitraHolder.btnCopy.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Mitra Response", message.getMessage());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                });

                mitraHolder.btnRegenerate.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onRegenerate(position);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;
        ImageView ivAttachment;

        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            ivAttachment = itemView.findViewById(R.id.ivUserAttachment);
        }
    }

    static class MitraViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;
        View layoutActions;
        View btnCopy, btnHelpful, btnRegenerate;

        MitraViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnHelpful = itemView.findViewById(R.id.btnHelpful);
            btnRegenerate = itemView.findViewById(R.id.btnRegenerate);
        }
    }
}