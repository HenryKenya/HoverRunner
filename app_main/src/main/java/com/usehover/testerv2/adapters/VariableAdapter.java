package com.usehover.testerv2.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.usehover.testerv2.R;
import com.usehover.testerv2.interfaces.VariableEditinterface;
import com.usehover.testerv2.models.StreamlinedStepsModel;

import java.util.Map;
import java.util.Objects;

public  class VariableAdapter extends  RecyclerView.Adapter<HoverAdapters.VariableItemListView> {
         private StreamlinedStepsModel stepsModel;
         private VariableEditinterface editinterface;
         private Map<String, String> initialData;
         private String actionId;

        public VariableAdapter(String actionId, StreamlinedStepsModel stepsModel, VariableEditinterface editinterface,  Map<String, String> initialData) {
            this.stepsModel = stepsModel;
            this.editinterface = editinterface;
            this.initialData = initialData;
            this.actionId = actionId;
        }

        @NonNull
        @Override
        public HoverAdapters.VariableItemListView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.variables_items, parent, false);
            return new HoverAdapters.VariableItemListView(view);
        }

        @Override
        public void onBindViewHolder(final @NonNull HoverAdapters.VariableItemListView holder, int position) {
            String label = stepsModel.getStepVariableLabel().get(position);
            String desc = stepsModel.getStepsVariableDesc().get(position);

            holder.view.setTag(actionId+label);
            holder.labelText.setText(label);
            holder.editText.setHint(desc);
            if(initialData.get(label) !=null){
                if(!Objects.requireNonNull(initialData.get(label)).isEmpty()) holder.editText.setText(initialData.get(label));
            }

            holder.editText.addTextChangedListener(new TextWatcher() {
                @Override public void afterTextChanged(Editable s) { }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    editinterface.onEditStringChanged(label, s.toString());
                }
            });
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
            if(stepsModel == null) return 0;
            if(stepsModel.getStepVariableLabel().size() != stepsModel.getStepsVariableDesc().size()) return 0;
            return stepsModel.getStepVariableLabel().size();
        }


    }
