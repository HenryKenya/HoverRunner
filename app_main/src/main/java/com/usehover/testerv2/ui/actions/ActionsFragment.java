package com.usehover.testerv2.ui.actions;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hover.sdk.actions.HoverAction;
import com.hover.sdk.api.Hover;
import com.hover.sdk.api.HoverParameters;
import com.usehover.testerv2.ApplicationInstance;
import com.usehover.testerv2.Fakesdk;
import com.usehover.testerv2.R;
import com.usehover.testerv2.adapters.HomeActionRecyclerAdapter;
import com.usehover.testerv2.api.Apis;
import com.usehover.testerv2.enums.PassageEnum;
import com.usehover.testerv2.enums.StatusEnums;
import com.usehover.testerv2.interfaces.CustomOnClickListener;
import com.usehover.testerv2.models.ActionsModel;
import com.usehover.testerv2.models.StreamlinedStepsModel;
import com.usehover.testerv2.ui.action_details.ActionDetailsActivity;
import com.usehover.testerv2.ui.actions.filter.ActionFilterActivity;
import com.usehover.testerv2.ui.actions.uncompletedVariables.UncompletedVariableActivity;
import com.usehover.testerv2.utils.NetworkUtil;
import com.usehover.testerv2.utils.UIHelper;
import com.usehover.testerv2.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActionsFragment extends Fragment implements CustomOnClickListener, Hover.DownloadListener {


    private ActionsViewModel actionsViewModel;
    private static final int FILTER_RESULT = 300;
    private static final int TEST_ALL_RESULT = 301;
    private static final int FILL_IN_UNCOMPLETED_VARIABLES = 302;
    private TextView filterText, emptyStateText;
    private ProgressBar progressBar;
    private RecyclerView homeActionsRecyclerView;
    private List<ActionsModel> rawRunnableModelList = new ArrayList<>();
    private ArrayList<ActionsModel> withCompletedVariableActionList = new ArrayList<>();
    private int actionRunCounter = 0;
    private RelativeLayout emptyStateView;
    private SwipeRefreshLayout pullToRefresh;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        actionsViewModel = new ViewModelProvider(this).get(ActionsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_actions, container, false);
        filterText  = root.findViewById(R.id.actionFilter_id);
        progressBar = root.findViewById(R.id.progress_state_1);
        emptyStateText = root.findViewById(R.id.empty_text_1);
        emptyStateView = root.findViewById(R.id.layoutForEmptyStateId);
        pullToRefresh = root.findViewById(R.id.pullToRefresh);

        homeActionsRecyclerView = root.findViewById(R.id.recyclerViewId);
        homeActionsRecyclerView.setLayoutManager(UIHelper.setMainLinearManagers(getContext()));


        UIHelper.setTextUnderline(filterText, getResources().getString(R.string.filter_text));

        //CALL THE FILTER FUNCTION
        filterText.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ActionFilterActivity.class);
            startActivityForResult(i, FILTER_RESULT);
        });

        root.findViewById(R.id.testAllActions_id).setOnClickListener(v -> {
            //Ensure action is not refreshing and running at same time to prevent errors.
            if(!pullToRefresh.isRefreshing()) {
                pullToRefresh.setRefreshing(true);

                ArrayList<ActionsModel> withUncompletedVariablesActionList = new ArrayList<>();
                withCompletedVariableActionList = new ArrayList<>();

                for (ActionsModel runnableModel : rawRunnableModelList) {
                    int variableSize = Utils.getStreamlinedStepsStepsFromRaw(runnableModel.getRootCode(), runnableModel.getSteps()).getStepVariableLabel().size();
                    switch (Utils.actionHasAllVariablesFilled(getContext(), runnableModel.getActionId(), variableSize)) {
                        case GOOD:
                            withCompletedVariableActionList.add(runnableModel);
                            break;
                        case BAD:
                            withUncompletedVariablesActionList.add(runnableModel);
                            break;
                        //SKIPPING DOES NOT REQUIRE A CASE
                        //case SKIPPED:
                        //  break;
                    }
                }

                pullToRefresh.setRefreshing(false);
                if (withUncompletedVariablesActionList.size() > 0) {
                    Intent i = new Intent(getActivity(), UncompletedVariableActivity.class);
                    i.putParcelableArrayListExtra("data", withUncompletedVariablesActionList);
                    startActivityForResult(i, FILL_IN_UNCOMPLETED_VARIABLES);
                } else {
                    if (withCompletedVariableActionList.size() > 0) runAction();
                    else
                        UIHelper.showHoverToast(getContext(), getActivity().getCurrentFocus(), getResources().getString(R.string.noRunnableAction));
                }
            }

        });

        //GET ALL ACTIONS
        actionsViewModel.getAllActions();


        pullToRefresh.setOnRefreshListener(() -> {
                if(new NetworkUtil(getContext()).isNetworkAvailable() == PassageEnum.ACCEPT) {
                    Hover.updateActionConfigs(this, (getContext() != null) ? getContext() : ApplicationInstance.getContext());
                }
                else {
                    pullToRefresh.setRefreshing(false);
                    UIHelper.showHoverToast(getContext(), getActivity()!=null ? getActivity().getCurrentFocus() : null, Apis.NO_NETWORK);
                }

        });

        return root;
    }

    private void runAction() {

        ActionsModel action = withCompletedVariableActionList.get(actionRunCounter);
        Map<String, String> actionExtra = Utils.getInitialVariableData(getContext(), action.getActionId()).second;

        HoverParameters.Builder builder = new HoverParameters.Builder(getContext());
        builder.request(action.getActionId());
        builder.setEnvironment(Apis.getTestEnvMode());

        assert  actionExtra !=null;
        for(String key : actionExtra.keySet()) {
            builder.extra(key, actionExtra.get(key));
        }
        Intent i = builder.buildIntent();
        startActivityForResult(i, TEST_ALL_RESULT);
    }

    @Override
    public void onResume() {
        super.onResume();
        actionsViewModel.getText().observe(getViewLifecycleOwner(), filterStatus-> {
            switch (filterStatus) {
                case FILTER_OFF: filterText.setTextColor(getResources().getColor(R.color.colorHoverWhite));
                    break;
                case FILTER_ON: filterText.setTextColor(getResources().getColor(R.color.colorPrimary));
                    filterText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dot_purple_24dp, 0,0,0);
                    filterText.setCompoundDrawablePadding(8);
                    break;
            }
        });

        actionsViewModel.loadActionsObs().observe(getViewLifecycleOwner(), fullActionResult -> {
            switch (fullActionResult.getActionEnum()){
                case LOADING:
                    if(homeActionsRecyclerView.getVisibility() == View.VISIBLE)homeActionsRecyclerView.setVisibility(View.GONE);
                    if(emptyStateText.getVisibility() == View.VISIBLE) {
                        emptyStateText.setVisibility(View.GONE);
                        emptyStateView.setVisibility(View.GONE);
                    }
                    progressBar.setVisibility(View.VISIBLE);
                    break;

                case EMPTY:
                    if(homeActionsRecyclerView.getVisibility() == View.VISIBLE)homeActionsRecyclerView.setVisibility(View.GONE);
                    if(progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.VISIBLE);
                    break;

                case HAS_DATA:
                    if(emptyStateText.getVisibility() == View.VISIBLE) {
                        emptyStateText.setVisibility(View.GONE);
                        emptyStateView.setVisibility(View.GONE);
                    }
                    if(progressBar.getVisibility() == View.VISIBLE) progressBar.setVisibility(View.GONE);
                    if(homeActionsRecyclerView.getVisibility() != View.VISIBLE) homeActionsRecyclerView.setVisibility(View.VISIBLE);
                    rawRunnableModelList = fullActionResult.getActionsModelList();
                    homeActionsRecyclerView.setAdapter(new HomeActionRecyclerAdapter(fullActionResult.getActionsModelList(), true,
                            this,
                            getResources().getColor(R.color.colorYellow),
                            getResources().getColor(R.color.colorRed)));
                    break;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILTER_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                String newText = data.getStringExtra("idn");
                assert newText != null;
                if (newText.equals("enteredTextValue")) {
                    actionsViewModel.setFilterOn();
                }
            }
        }
        else if (requestCode == TEST_ALL_RESULT) {
            if(actionRunCounter  < withCompletedVariableActionList.size()) {
                runAction();
                actionRunCounter = actionRunCounter + 1;
            }
            else if(actionRunCounter == withCompletedVariableActionList.size()) actionsViewModel.getAllActions();

        }
    }


    @Override
    public void customClickListener(Object... data) {
        assert data!=null;
        Intent i = new Intent(getActivity(), ActionDetailsActivity.class);
        i.putExtra(Apis.ACTION_ID, (String) data[0]);
        i.putExtra(Apis.ACTION_TITLE, (String) data[1]);
        i.putExtra(Apis.ACTION_STATUS, (StatusEnums) data[2]);
        startActivity(i);
    }

    @Override
    public void onError(String message) {
    pullToRefresh.setRefreshing(false);
    UIHelper.showHoverToastV2(getContext(), message);
    }

    @Override
    public void onSuccess(ArrayList<HoverAction> actions) {
    pullToRefresh.setRefreshing(false);
    actionsViewModel.getAllActions();
    UIHelper.showHoverToastV2(getContext(), getResources().getString(R.string.refreshed_successfully));
    }
}
