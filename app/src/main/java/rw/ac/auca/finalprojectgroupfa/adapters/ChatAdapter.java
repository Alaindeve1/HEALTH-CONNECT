package rw.ac.auca.finalprojectgroupfa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import rw.ac.auca.finalprojectgroupfa.R;
import rw.ac.auca.finalprojectgroupfa.models.Message;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat;

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else { // viewType == VIEW_TYPE_RECEIVED
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    // ViewHolder for messages sent by the current user
    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        private final TextView timeText;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }

        void bind(Message message) {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(message.getText());

            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            }
        }
    }

    // ViewHolder for messages received from other users
    private class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        private final TextView timeText;
        private final TextView senderNameText;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            senderNameText = itemView.findViewById(R.id.senderName);
        }

        void bind(Message message) {
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(message.getText());

            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            }
            if (message.getSenderName() != null) {
                senderNameText.setText(message.getSenderName());
                senderNameText.setVisibility(View.VISIBLE);
            } else {
                senderNameText.setVisibility(View.GONE);
            }
        }
    }
}
