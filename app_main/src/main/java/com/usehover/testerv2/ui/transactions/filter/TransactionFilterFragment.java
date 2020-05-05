package com.usehover.testerv2.ui.transactions.filter;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.usehover.testerv2.ApplicationInstance;
import com.usehover.testerv2.R;
import com.usehover.testerv2.api.Apis;
import com.usehover.testerv2.enums.StatusEnums;
import com.usehover.testerv2.models.FilterDataFullModel;
import com.usehover.testerv2.ui.filter_pages.FilterByActions;
import com.usehover.testerv2.ui.filter_pages.FilterByCategories;
import com.usehover.testerv2.ui.filter_pages.FilterByCountries;
import com.usehover.testerv2.ui.filter_pages.FilterByNetworks;
import com.usehover.testerv2.utils.UIHelper;
import com.usehover.testerv2.utils.Utils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TransactionFilterFragment extends Fragment {
    private TextView datePickerView, countryEntry, networkEntry,  actionEntry, resetText;
    private ProgressBar loadingProgressBar;
    private Timer timer = new Timer();
    private EditText searchTransactionEdit;
    private boolean resetActivated = false;
    private AppCompatCheckBox status_success, status_pending, status_fail;
    private FilterDataFullModel filterDataFullModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transaction_filter_fragment, container, false);

        TextView toolText = view.findViewById(R.id.transactionFilterBackId);
        resetText = view.findViewById(R.id.reset_id);
        toolText.setOnClickListener(v -> getActivity().finish());
        loadingProgressBar = view.findViewById(R.id.filter_progressBar);
        loadingProgressBar.setVisibility(View.VISIBLE);

        searchTransactionEdit = view.findViewById(R.id.searchEditId);
        countryEntry = view.findViewById(R.id.countryEntryId);
        networkEntry = view.findViewById(R.id.networkEntryId);
        actionEntry = view.findViewById(R.id.actionsSelectEntryId);
        status_success = view.findViewById(R.id.checkbox_success);
        status_pending = view.findViewById(R.id.checkbox_pending);
        status_fail = view.findViewById(R.id.checkbox_fail);

        filterDataFullModel = new Apis().doGetDataForActionFilter();
        if(filterDataFullModel.getActionEnum() == StatusEnums.HAS_DATA) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        else {
            UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.no_transactions_yet));
            if(getActivity()!=null)getActivity().finish();
        }

        searchTransactionEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ApplicationInstance.setTransactionSearchText(s.toString());
                timer.cancel();
                timer = new Timer();
                long DELAY = 1500;
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        activateReset();
                        filterThroughActions();
                    }
                }, DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        resetText.setOnClickListener(v->{
            if(resetActivated) {
                new Apis().resetTransactionFilterDataset();
                reloadAllMajorViews();
                UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.reset_successful));
                new Handler().postDelayed(() -> {
                    UIHelper.removeTextUnderline(resetText);
                    resetText.setTextColor(getResources().getColor(R.color.colorMainGrey));
                    resetActivated = false;

                }, 1500);

            }

        });

        datePickerView = view.findViewById(R.id.dateRangeEntryId);
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<?> picker = builder.setTitleText(getResources().getString(R.string.selected_range)).build();

        datePickerView.setOnClickListener(v -> {
            if(hasLoaded()) picker.show(getParentFragmentManager(), picker.toString());
        });
        picker.addOnPositiveButtonClickListener(selection -> {
            Pair<Long, Long> datePairs = (Pair<Long, Long>) selection;
            ApplicationInstance.setTransactionDateRange(datePairs);
            setOrReloadDateRange();

        });

        view.findViewById(R.id.showActions_id).setOnClickListener(v -> getActivity().finish());

        setUpViewClicks();
        setupCheckboxes();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadAllMajorViews();
    }

    private void reloadAllMajorViews() {
        setOrReloadSearchEdit();
        setOrReloadCountriesText();
        setOrReloadNetworkText();
        setOrReloadDateRange();
        setOrReloadCheckboxes();
        setOrReloadActionsText();
    }

    private void activateReset() {
        if(!resetActivated) {
            resetText.setTextColor(getResources().getColor(R.color.colorHoverWhite));
            UIHelper.setTextUnderline(resetText, "Reset");
            resetActivated = true;
        }

    }
    private void setupCheckboxes() {
        status_success.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusSuccess(status);
            activateReset();
            filterThroughActions();
        });

        status_fail.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusFailed(status);
            activateReset();
            filterThroughActions();
        });

        status_pending.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusPending(status);
            activateReset();
            filterThroughActions();
        });

    }

    private void setUpViewClicks() {
        countryEntry.setOnClickListener(v-> {
            Intent i = new Intent(getActivity(), FilterByCountries.class);
            i.putExtra("data", new Apis().getCountriesForTransactionFilter(filterDataFullModel.getAllCountries()));
            i.putExtra("filter_type", 1);
            startActivity(i);
        });

        networkEntry.setOnClickListener(v->{
            Intent i = new Intent(getActivity(), FilterByNetworks.class);
            i.putExtra("data", new Apis().getNetworksForTransactionFilter(filterDataFullModel.getAllNetworks()));
            i.putExtra("filter_type", 1);
            startActivity(i);
        });

        actionEntry.setOnClickListener(v-> {
            Intent i = new Intent(getActivity(), FilterByActions.class);
            i.putExtra("data", new Apis().getTransactionSelectedActionsFilter(filterDataFullModel.getActionsModelList()));
            startActivity(i);
        });
    }

    private void setOrReloadSearchEdit() {
        if(ApplicationInstance.getTransactionSearchText() !=null) {
            if(!ApplicationInstance.getTransactionSearchText().isEmpty()) {
                searchTransactionEdit.setText(ApplicationInstance.getTransactionSearchText());
            }else searchTransactionEdit.setText("");
        } else searchTransactionEdit.setText("");
    }
    private void setOrReloadCountriesText() {
        if(ApplicationInstance.getTransactionCountriesFilter().size()>0) {
            countryEntry.setText(new Apis().getSelectedTransactionCountriesAsText());
            activateReset();
        }
        else countryEntry.setText("");
    }

    private void setOrReloadActionsText() {
        if(ApplicationInstance.getTransactionActionsSelectedFilter().size()>0) {
            actionEntry.setText(new Apis().getSelectedActionsForTransactionAsText());
            activateReset();
        }
        else actionEntry.setText("");
    }

    private void setOrReloadNetworkText() {
        if(ApplicationInstance.getTransactionNetworksFilter().size()>0) {
            networkEntry.setText(new Apis().getSelectedTransactionNetworksAsText());
            activateReset();
        }
        else networkEntry.setText("");
    }

    private void setOrReloadDateRange() {
        if(ApplicationInstance.getTransactionDateRange() != null) {
            Pair<Long, Long> dateRange = ApplicationInstance.getTransactionDateRange();
            datePickerView.setText(String.format(Locale.ENGLISH, "%s - %s", Utils.formatDateV2(dateRange.first), Utils.formatDateV3(dateRange.second)));
            activateReset();
        }
        else datePickerView.setText(getResources().getString(R.string.from_account_creation_to_today));
    }

    private void setOrReloadCheckboxes() {
        status_success.setChecked(ApplicationInstance.isTransactionStatusSuccess());
        status_pending.setChecked(ApplicationInstance.isTransactionStatusPending());
        status_fail.setChecked(ApplicationInstance.isTransactionStatusFailed());
    }

    private void filterThroughActions() {

    }
    private boolean hasLoaded() {
        return loadingProgressBar.getVisibility() == View.GONE;
    }
}