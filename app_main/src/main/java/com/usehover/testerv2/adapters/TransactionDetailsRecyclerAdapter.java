package com.usehover.testerv2.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usehover.testerv2.R;
import com.usehover.testerv2.enums.ClickTypeEnum;
import com.usehover.testerv2.interfaces.CustomOnClickListener;
import com.usehover.testerv2.models.TransactionDetailsInfoModels;
import com.usehover.testerv2.utils.UIHelper;

import java.util.ArrayList;


public class TransactionDetailsRecyclerAdapter extends RecyclerView.Adapter<TransactionDetailsRecyclerAdapter.TDViewHolder> {

    private ArrayList<TransactionDetailsInfoModels> modelsArrayList;
    private CustomOnClickListener customOnClickListener;
    private String actionId = "";

    private int colorRed, colorYellow, colorGreen;
    
    public TransactionDetailsRecyclerAdapter(ArrayList<TransactionDetailsInfoModels> modelsArrayList, CustomOnClickListener customOnClickListener) {
        this.modelsArrayList = modelsArrayList;
        this.customOnClickListener = customOnClickListener;

        for(TransactionDetailsInfoModels models : modelsArrayList) {
            if(models.getLabel().equals("ActionID")) {
                actionId = models.getValue();
                return;
            }
        }
    }

    public TransactionDetailsRecyclerAdapter(ArrayList<TransactionDetailsInfoModels> modelsArrayList, CustomOnClickListener customOnClickListener, int colorRed, int colorYellow, int colorGreen) {
        this.modelsArrayList = modelsArrayList;
        this.customOnClickListener = customOnClickListener;
        this.colorRed = colorRed;
        this.colorYellow = colorYellow;
        this.colorGreen = colorGreen;
    }

    @NonNull
    @Override
    public TDViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transac_details_list_items, parent, false);
        return new TDViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TDViewHolder holder, int position) {
    TransactionDetailsInfoModels infoModels = modelsArrayList.get(position);
    
    holder.label.setText(infoModels.getLabel());
    if(infoModels.getLabel().equals("Status")) {
        switch (infoModels.getStatusEnums()) {
            case UNSUCCESSFUL:
                holder.value.setTextColor(colorRed);
                break;
            case PENDING:
                holder.value.setTextColor(colorYellow);
                break;
            case SUCCESS:
                holder.value.setTextColor(colorGreen);
                break;
        }
    }
    if(infoModels.isClickable()) {
        UIHelper.setTextUnderline(holder.value, infoModels.getValue());
        if(infoModels.getLabel().contains("Action"))
            holder.value.setOnClickListener(v -> customOnClickListener.customClickListener(actionId, ClickTypeEnum.CLICK_ACTION));
        else holder.value.setOnClickListener(v -> customOnClickListener.customClickListener(infoModels.getValue(), ClickTypeEnum.CLICK_PARSER));
    }
    else holder.value.setText(infoModels.getLabel());

    }

    @Override
    public int getItemCount() {
        if(modelsArrayList == null) return 0;
        return modelsArrayList.size();
    }
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class TDViewHolder extends RecyclerView.ViewHolder {
        TextView label, value;
         TDViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.transac_det_label);
            value = itemView.findViewById(R.id.transac_det_value);

        }
    }
}