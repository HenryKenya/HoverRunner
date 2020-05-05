package com.usehover.testerv2.ui.actions.filter;

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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

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

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ActionFilterFragment extends Fragment {

    private ProgressBar loadingProgressBar;
    private FilterDataFullModel filterDataFullModel;
    private TextView resetText, countryEntry, networkEntry, categoryEntry, datePickerView;
    private EditText searchActionEdit;
    private AppCompatCheckBox status_success, status_pending, status_fail, status_noTrans, withParser, onlyWithSimPresent;
    private Timer timer = new Timer();
    private boolean resetActivated = false;
    private ActionFilterViewModel actionFilterViewModel;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.action_filter_fragment, container, false);


        TextView toolText = view.findViewById(R.id.actionFilterBackId);
        LinearLayout entryFilterView = view.findViewById(R.id.entry_filter_view);
        TextView showActionsText = view.findViewById(R.id.showActions_id);
        loadingProgressBar = view.findViewById(R.id.filter_progressBar);
        resetText = view.findViewById(R.id.reset_id);
        countryEntry = view.findViewById(R.id.countryEntryId);
        networkEntry = view.findViewById(R.id.networkEntryId);
        categoryEntry = view.findViewById(R.id.categoryEntryId);
        searchActionEdit = view.findViewById(R.id.searchEditId);
        status_success = view.findViewById(R.id.checkbox_success);
        status_pending = view.findViewById(R.id.checkbox_pending);
        status_fail = view.findViewById(R.id.checkbox_fail);
        status_noTrans = view.findViewById(R.id.checkbox_no_transaction);
        withParser = view.findViewById(R.id.checkbox_parsers);
        onlyWithSimPresent = view.findViewById(R.id.checkbox_sim);

        toolText.setOnClickListener(v -> { if(getActivity() !=null)getActivity().finish(); });
        loadingProgressBar.setVisibility(View.VISIBLE);
        loadingProgressBar.setIndeterminate(true);
        UIHelper.setTextUnderline(resetText, "Reset");



        actionFilterViewModel = new ViewModelProvider(this).get(ActionFilterViewModel.class);
        actionFilterViewModel.loadAllDataObs().observe(getViewLifecycleOwner(), result-> {
            if(result.getActionEnum() == StatusEnums.HAS_DATA) {
                loadingProgressBar.setVisibility(View.GONE);
                filterDataFullModel = result;
                entryFilterView.setVisibility(View.VISIBLE);
                filterThroughActions();
            }
            else if(result.getActionEnum() == StatusEnums.EMPTY) {
                filterDataFullModel = null;
                UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.no_actions_yet));
                if(getActivity()!=null)getActivity().finish();
            }
        });


        showActionsText.setOnClickListener(v -> {
            //We are setting two list of actions, so that if user clicks cancel, it shows the previously filtered actions
            //When users clicks this button, it then adds the latest filtered actions into this bucket.
            if(Apis.actionFilterIsInNormalState()) {
                ApplicationInstance.setResultFilter_Actions_LOAD(new ArrayList<>());
                ApplicationInstance.setResultFilter_Actions(new ArrayList<>());
            }
            else {
                ApplicationInstance.setResultFilter_Actions_LOAD(ApplicationInstance.getResultFilter_Actions());
                ApplicationInstance.setResultFilter_Actions(new ArrayList<>());
            }

            if(getActivity() !=null)getActivity().finish();
        });
        actionFilterViewModel.loadActionsObs().observe(getViewLifecycleOwner(), filterResult-> {
            if(filterResult.getActionEnum() == StatusEnums.HAS_DATA) {
                showActionsText.setClickable(true);
                showActionsText.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                String suffixAction ="actions";
                if(filterResult.getActionsModelList().size() == 1) suffixAction = "action";
                showActionsText.setText(String.format(Locale.ENGLISH, "Show %d %s", filterResult.getActionsModelList().size(), suffixAction));
            }
            else if(filterResult.getActionEnum() == StatusEnums.LOADING) {
                showActionsText.setClickable(false);
                showActionsText.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                showActionsText.setText(getResources().getString(R.string.loadingText));
            }
            else {
                //It means empty
                showActionsText.setClickable(false);
                showActionsText.setBackgroundColor(getResources().getColor(R.color.colorMainGrey));
                showActionsText.setText(getResources().getString(R.string.no_actions_filter_result));
            }

        });


        searchActionEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ApplicationInstance.setActionSearchText(s.toString());
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

        if(ApplicationInstance.getResultFilter_Actions_LOAD().size() > 0) {
            activateReset();
        }
        resetText.setOnClickListener(v->{
            if(resetActivated) {
                new Apis().resetActionFilterDataset();
                reloadAllMajorViews();
                ApplicationInstance.setResultFilter_Actions(new ArrayList<>());
                ApplicationInstance.setResultFilter_Actions_LOAD(new ArrayList<>());
                deactivateReset();

                UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.reset_successful));
                new Handler().postDelayed(this::deactivateReset, 800);
            }
        });

        datePickerView = view.findViewById(R.id.dateRangeEditId);
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<?> picker = builder.setTitleText(getResources().getString(R.string.selected_range)).build();

        datePickerView.setOnClickListener(v -> {
            if(hasLoaded()) picker.show(getParentFragmentManager(), picker.toString());
        });
        picker.addOnPositiveButtonClickListener(selection -> {
            Pair<Long, Long> datePairs = (Pair<Long, Long>) selection;
            ApplicationInstance.setDateRange(datePairs);
            setOrReloadDateRange();
            filterThroughActions();

        });

        setUpViewClicks();
        setupCheckboxes();
        actionFilterViewModel.getFullDataFirst();

        return view;
    }


    private void filterThroughActions() {
        if(filterDataFullModel == null) onCreate(null);
        else {
            actionFilterViewModel.getOrReloadFilterActions(filterDataFullModel.getActionsModelList(), filterDataFullModel.getTransactionModelsList());
        }
    }

    private void deactivateReset() {
        resetText.setTextColor(getResources().getColor(R.color.colorMainGrey));
        resetActivated = false;
    }
    private void activateReset() {
        if(!Apis.actionFilterIsInNormalState()) {
            resetText.setTextColor(getResources().getColor(R.color.colorHoverWhite));
            resetActivated = true;
        }

    }
    private void setupCheckboxes() {
        status_success.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setStatusSuccess(status);
            activateReset();
            filterThroughActions();
        });

        status_noTrans.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setStatusNoTrans(status);
            activateReset();
            filterThroughActions();
        });

        status_fail.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setStatusFailed(status);
            activateReset();
            filterThroughActions();
        });

        status_pending.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setStatusPending(status);
            activateReset();
            filterThroughActions();
        });

        withParser.setOnCheckedChangeListener((v, status) -> {
            ApplicationInstance.setWithParsers(status);
            activateReset();
            filterThroughActions();
        });

        onlyWithSimPresent.setOnCheckedChangeListener((v, status)-> {
            ApplicationInstance.setOnlyWithSimPresent(status);
            activateReset();
            filterThroughActions();
        });
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
        setOrReloadCategoryText();
        setOrReloadCheckboxes();
    }


    private void setUpViewClicks() {
        countryEntry.setOnClickListener(v-> {
            Intent i = new Intent(getActivity(), FilterByCountries.class);
            i.putExtra("data", new Apis().getCountriesForActionFilter(filterDataFullModel.getAllCountries()));
            i.putExtra("filter_type", 0);
            startActivity(i);
        });

        networkEntry.setOnClickListener(v->{
            Intent i = new Intent(getActivity(), FilterByNetworks.class);
            i.putExtra("data", new Apis().getNetworksForActionFilter(filterDataFullModel.getAllNetworks()));
            i.putExtra("filter_type", 0);
            startActivity(i);
        });

        categoryEntry.setOnClickListener(v->{
            Intent i = new Intent(getActivity(), FilterByCategories.class);
            i.putExtra("data", new Apis().getCategoriesForActionFilter(filterDataFullModel.getAllCategories()));
            startActivity(i);
        });


    }

    private void setOrReloadSearchEdit() {
        if(ApplicationInstance.getActionSearchText() !=null) {
            if(!ApplicationInstance.getActionSearchText().isEmpty()) {
                searchActionEdit.setText(ApplicationInstance.getActionSearchText());
            }else searchActionEdit.setText("");
        } else searchActionEdit.setText("");
    }
    private void setOrReloadCountriesText() {
        if(ApplicationInstance.getCountriesFilter().size()>0) {
            countryEntry.setText(new Apis().getSelectedCountriesAsText());
            activateReset();
        }
        else countryEntry.setText("");
    }



    private void setOrReloadNetworkText() {
        if(ApplicationInstance.getNetworksFilter().size()>0) {
            networkEntry.setText(new Apis().getSelectedNetworksAsText());
            activateReset();
        }
        else networkEntry.setText("");
    }

    private void setOrReloadDateRange() {
        if(ApplicationInstance.getDateRange() != null) {
            Pair<Long, Long> dateRange = ApplicationInstance.getDateRange();
            datePickerView.setText(String.format(Locale.ENGLISH, "%s - %s", Utils.formatDateV2(dateRange.first), Utils.formatDateV3(dateRange.second)));
            activateReset();
        }
        else datePickerView.setText(getResources().getString(R.string.from_account_creation_to_today));
    }

    private void setOrReloadCategoryText() {
        if(ApplicationInstance.getCategoryFilter().size() > 0) {
            categoryEntry.setText(new Apis().getSelectedCategoriesAsText());
            activateReset();
        }
        else categoryEntry.setText("");
    }

    private void setOrReloadCheckboxes() {
        status_success.setChecked(ApplicationInstance.isStatusSuccess());
        status_pending.setChecked(ApplicationInstance.isStatusPending());
        status_fail.setChecked(ApplicationInstance.isStatusFailed());
        status_noTrans.setChecked(ApplicationInstance.isStatusNoTrans());
        withParser.setChecked(ApplicationInstance.isWithParsers());
        onlyWithSimPresent.setChecked(ApplicationInstance.isOnlyWithSimPresent());
    }

    private boolean hasLoaded() {
        return loadingProgressBar.getVisibility() == View.GONE;
    }

}