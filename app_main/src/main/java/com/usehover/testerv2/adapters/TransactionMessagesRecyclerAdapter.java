package com.usehover.testerv2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usehover.testerv2.R;
import com.usehover.testerv2.models.TransactionDetailsMessagesModel;

import java.util.ArrayList;

public class TransactionMessagesRecyclerAdapter extends RecyclerView.Adapter<TransactionMessagesRecyclerAdapter.TransactionMessageViewHolder> {

    private ArrayList<TransactionDetailsMessagesModel> messagesModelArrayList;

    public TransactionMessagesRecyclerAdapter(ArrayList<TransactionDetailsMessagesModel> messagesModelArrayList) {
        this.messagesModelArrayList = messagesModelArrayList;
    }

    @NonNull
    @Override
    public TransactionMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_messages_items, parent, false);
        return new TransactionMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionMessageViewHolder holder, int position) {
        TransactionDetailsMessagesModel model = messagesModelArrayList.get(position);
        holder.enteredValueText.setText(model.getEnteredValue());
        holder.messageContentText.setText(model.getMessageContent());
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if(messagesModelArrayList == null) return 0;
        return messagesModelArrayList.size();
    }

    static class TransactionMessageViewHolder extends RecyclerView.ViewHolder {
        TextView enteredValueText, messageContentText;
        TransactionMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            enteredValueText = itemView.findViewById(R.id.message_enteredValue);
            messageContentText = itemView.findViewById(R.id.message_content);
        }
    }
}
