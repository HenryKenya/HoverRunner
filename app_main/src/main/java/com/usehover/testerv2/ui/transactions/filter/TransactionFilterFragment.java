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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.usehover.testerv2.ApplicationInstance;
import com.usehover.testerv2.R;
import com.usehover.testerv2.api.Apis;
import com.usehover.testerv2.enums.StatusEnums;
import com.usehover.testerv2.models.FilterDataFullModel;
import com.usehover.testerv2.ui.filter_pages.FilterByActions;
import com.usehover.testerv2.ui.filter_pages.FilterByCountries;
import com.usehover.testerv2.ui.filter_pages.FilterByNetworks;
import com.usehover.testerv2.utils.UIHelper;
import com.usehover.testerv2.utils.Utils;

import java.util.ArrayList;
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
    private TransactionFilterViewModel transactionFilterViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transaction_filter_fragment, container, false);

        TextView toolText = view.findViewById(R.id.transactionFilterBackId);
        LinearLayout entryFilterView = view.findViewById(R.id.entry_filter_view);
        TextView showTransactionsText = view.findViewById(R.id.showActions_id);

        resetText = view.findViewById(R.id.reset_id);
        toolText.setOnClickListener(v -> {
            if(getActivity() !=null) getActivity().finish();
        });

        loadingProgressBar = view.findViewById(R.id.filter_progressBar);
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setVisibility(View.VISIBLE);

        datePickerView = view.findViewById(R.id.dateRangeEntryId);
        searchTransactionEdit = view.findViewById(R.id.searchEditId);
        countryEntry = view.findViewById(R.id.countryEntryId);
        networkEntry = view.findViewById(R.id.networkEntryId);
        actionEntry = view.findViewById(R.id.actionsSelectEntryId);
        status_success = view.findViewById(R.id.checkbox_success);
        status_pending = view.findViewById(R.id.checkbox_pending);
        status_fail = view.findViewById(R.id.checkbox_fail);
        UIHelper.setTextUnderline(resetText, "Reset");


        transactionFilterViewModel = new ViewModelProvider(this).get(TransactionFilterViewModel.class);
        transactionFilterViewModel.loadAllDataObs().observe(getViewLifecycleOwner(), result-> {
            if(result.getActionEnum() == StatusEnums.HAS_DATA) {
                loadingProgressBar.setVisibility(View.GONE);
                filterDataFullModel = result;
                entryFilterView.setVisibility(View.VISIBLE);
                filterThroughTransactions();
            }
            else if(result.getActionEnum() == StatusEnums.EMPTY) {
                filterDataFullModel = null;
                UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.no_actions_yet));
                if(getActivity()!=null)getActivity().finish();
            }
        });

        showTransactionsText.setOnClickListener(v -> {
            //We are setting two list of actions, so that if user clicks cancel, it shows the previously filtered actions
            //When users clicks this button, it then adds the latest filtered actions into this bucket.
            if(Apis.transactionFilterIsInNormalState()) {
                ApplicationInstance.setResultFilter_Transactions_LOAD(new ArrayList<>());
                ApplicationInstance.setResultFilter_Transactions(new ArrayList<>());
            }
            else {
                ApplicationInstance.setResultFilter_Transactions_LOAD(ApplicationInstance.getResultFilter_Transactions());
                ApplicationInstance.setResultFilter_Transactions(new ArrayList<>());
            }

            if(getActivity() !=null)getActivity().finish();
        });

        transactionFilterViewModel.loadTransactionsObs().observe(getViewLifecycleOwner(), filterResult->{
            if(filterResult.getEnums() == StatusEnums.HAS_DATA) {
                showTransactionsText.setClickable(true);
                showTransactionsText.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                String suffixAction ="transactions";
                if(filterResult.getTransactionModelsList().size() == 1) suffixAction = "transaction";
                showTransactionsText.setText(String.format(Locale.ENGLISH, "Show %d %s", filterResult.getTransactionModelsList().size(), suffixAction));
            }
            else if(filterResult.getEnums() == StatusEnums.LOADING) {
                showTransactionsText.setClickable(false);
                showTransactionsText.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                showTransactionsText.setText(getResources().getString(R.string.loadingText));
            }
            else {
                //It means empty
                showTransactionsText.setClickable(false);
                showTransactionsText.setBackgroundColor(getResources().getColor(R.color.colorMainGrey));
                showTransactionsText.setText(getResources().getString(R.string.no_transactions_filter_result));
            }
        });



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
                        filterThroughTransactions();
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
                ApplicationInstance.setResultFilter_Transactions(new ArrayList<>());
                ApplicationInstance.setResultFilter_Transactions_LOAD(new ArrayList<>());

                deactivateReset();
                UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.reset_successful));
                new Handler().postDelayed(this::deactivateReset, 1500);

            }

        });


        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Pair<Long, Long>> picker = builder.setTitleText(getResources().getString(R.string.selected_range)).build();

        datePickerView.setOnClickListener(v -> {
            if(hasLoaded()) picker.show(getParentFragmentManager(), picker.toString());
        });
        picker.addOnPositiveButtonClickListener(selection -> {
            ApplicationInstance.setTransactionDateRange(selection);
            setOrReloadDateRange();
            filterThroughTransactions();

        });


        setUpViewClicks();
        setupCheckboxes();
        transactionFilterViewModel.getFullDataFirst();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadAllMajorViews();
    }

    private void deactivateReset() {
        resetText.setTextColor(getResources().getColor(R.color.colorMainGrey));
        resetActivated = false;
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
        if(!Apis.transactionFilterIsInNormalState()) {
            resetText.setTextColor(getResources().getColor(R.color.colorHoverWhite));
            resetActivated = true;
        }

    }
    private void setupCheckboxes() {
        status_success.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusSuccess(status);
            activateReset();
            filterThroughTransactions();
        });

        status_fail.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusFailed(status);
            activateReset();
            filterThroughTransactions();
        });

        status_pending.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setTransactionStatusPending(status);
            activateReset();
            filterThroughTransactions();
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
            datePickerView.setText(String.format(Locale.ENGLISH,
                    "%s - %s", Utils.formatDateV2((long) Utils.nonNullDateRange(dateRange.first)),
                    Utils.formatDateV3((long) Utils.nonNullDateRange(dateRange.second))));
            activateReset();
        }
        else datePickerView.setText(getResources().getString(R.string.from_account_creation_to_today));
    }

    private void setOrReloadCheckboxes() {
        status_success.setChecked(ApplicationInstance.isTransactionStatusSuccess());
        status_pending.setChecked(ApplicationInstance.isTransactionStatusPending());
        status_fail.setChecked(ApplicationInstance.isTransactionStatusFailed());
    }

    private void filterThroughTransactions() {
        if(filterDataFullModel == null) onCreate(null);
        else {
            transactionFilterViewModel.getOrReloadFilterTransactions(filterDataFullModel.getActionsModelList(), filterDataFullModel.getTransactionModelsList());
        }
    }
    private boolean hasLoaded() {
        return loadingProgressBar.getVisibility() == View.GONE;
    }
}
