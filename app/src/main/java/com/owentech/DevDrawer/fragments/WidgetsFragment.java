package com.owentech.DevDrawer.fragments;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owentech.DevDrawer.DevDrawerApplication;
import com.owentech.DevDrawer.R;
import com.owentech.DevDrawer.adapters.FilterListAdapter;
import com.owentech.DevDrawer.di.DaggerApplicationComponent;
import com.owentech.DevDrawer.di.ApplicationModule;
import com.owentech.DevDrawer.dialogs.AddPackageDialogFragment;
import com.owentech.DevDrawer.dialogs.ChangeWidgetNameDialogFragment;
import com.owentech.DevDrawer.dialogs.ChooseWidgetDialogFragment;
import com.owentech.DevDrawer.events.ChangeWidgetEvent;
import com.owentech.DevDrawer.utils.AppConstants;
import com.owentech.DevDrawer.utils.OttoManager;
import com.owentech.DevDrawer.events.PackageAddedEvent;
import com.owentech.DevDrawer.events.WidgetRenamedEvent;
import com.owentech.DevDrawer.utils.AppWidgetUtil;
import com.owentech.DevDrawer.utils.Database;
import com.owentech.DevDrawer.utils.RxUtils;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.reactivex.functions.Consumer;

public class WidgetsFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {

    @InjectView(R.id.selectionLayout) RelativeLayout selectionLayout;
    @InjectView(R.id.selectionShadow) View selectionShadow;
    @InjectView(R.id.recyclerView) RecyclerView recyclerView;
    @InjectView(R.id.noWidgets) CardView noWidgets;
    @InjectView(R.id.currentWidgetName) TextView currentWidgetName;
    @InjectView(R.id.fab) ImageButton fab;
    @Inject
    Database database;

    private int[] mAppWidgetIds;
    private FilterListAdapter filterListAdapter;
    float originalFabY;

    public WidgetsFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.widgets_fragment, container, false);
        ButterKnife.inject(this, view);
        setHasOptionsMenu(true);
        selectionLayout.setOnClickListener(this);
        selectionLayout.setOnLongClickListener(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddPackageDialog();
            }
        });
        originalFabY = fab.getTranslationY();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        OttoManager.getInstance().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        OttoManager.getInstance().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((DevDrawerApplication)getActivity().getApplication()).getApplicationComponent().inject(this);

        mAppWidgetIds = AppWidgetUtil.findAppWidgetIds(getActivity());
        showHideListView();

        if (mAppWidgetIds.length != 0) {
            if (FilterListAdapter.currentWidgetId == -1 || FilterListAdapter.currentWidgetId == AppConstants.NOTIFICATION) {
                FilterListAdapter.currentWidgetId = mAppWidgetIds[0];
            }
        }

        filterListAdapter = new FilterListAdapter(getActivity());

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(filterListAdapter);

        RxUtils.fromCallable(database.getWidgetNames(getActivity()))
                .subscribe(new Consumer<SparseArray<String>>() {
                    @Override
                    public void accept(SparseArray<String> stringSparseArray) throws Exception {
                        String widgetName = stringSparseArray.get(FilterListAdapter.currentWidgetId);
                        if (AppConstants.UNNAMED.equals(widgetName)){
                            widgetName = "Unnamed Widget";
                        }
                        currentWidgetName.setText(widgetName);
                    }
                });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        if (view == selectionLayout) {
            showChooseWidgetDialog();
        }
    }

    private void showHideListView(){
        if (mAppWidgetIds.length == 0){
            selectionLayout.setVisibility(View.GONE);
            selectionShadow.setVisibility(View.GONE);
            recyclerView.setVisibility(View.INVISIBLE);
            noWidgets.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        }
        else{
            recyclerView.setVisibility(View.VISIBLE);
            noWidgets.setVisibility(View.INVISIBLE);
            fab.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.notifications_menu, menu);
        if (mAppWidgetIds.length == 0){
            menu.findItem(R.id.menu_add).setVisible(false);
        }
        else{
            // TODO: Remove / Fix for final L release
            if (Build.VERSION.SDK_INT != 20) {
                menu.findItem(R.id.menu_add).setVisible(true);
            }
            else{
                menu.findItem(R.id.menu_add).setVisible(false);
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.menu_add) {
            showAddPackageDialog();
        }
        return false;
    }

    private void showAddPackageDialog(){
        AddPackageDialogFragment addPackageDialogFragment = AddPackageDialogFragment.newInstance(null, FilterListAdapter.currentWidgetId);
        addPackageDialogFragment.setTargetFragment(WidgetsFragment.this, 101);
        addPackageDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    private void showChooseWidgetDialog(){
        ChooseWidgetDialogFragment chooseWidgetDialogFragment = ChooseWidgetDialogFragment.newInstance(mAppWidgetIds[0]);
        chooseWidgetDialogFragment.setTargetFragment(WidgetsFragment.this, 101);
        chooseWidgetDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    private void showChangeWidgetNameDialog(){
        ChangeWidgetNameDialogFragment changeWidgetNameDialogFragment = ChangeWidgetNameDialogFragment.newInstance(FilterListAdapter.currentWidgetId, currentWidgetName.getText().toString());
        changeWidgetNameDialogFragment.setTargetFragment(WidgetsFragment.this, 101);
        changeWidgetNameDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    @Subscribe
    public void packageAdded(PackageAddedEvent event){
        if (filterListAdapter != null){
            filterListAdapter.updatePackageCollections();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        showChangeWidgetNameDialog();
        return true;
    }

    @Subscribe
    public void changeWidget(ChangeWidgetEvent event){
        FilterListAdapter.currentWidgetId = event.widgetId;
        filterListAdapter.notifyDataSetChanged();
        RxUtils.fromCallable(database.getWidgetNames(getActivity()))
                .subscribe(new Consumer<SparseArray<String>>() {
                    @Override
                    public void accept(SparseArray<String> stringSparseArray) throws Exception {
                        currentWidgetName.setText(stringSparseArray.get(FilterListAdapter.currentWidgetId));
                    }
                });
    }

    @Subscribe
    public void widgetRenamed(WidgetRenamedEvent event){
        RxUtils.fromCallable(database.getWidgetNames(getActivity()))
                .subscribe(new Consumer<SparseArray<String>>() {
                    @Override
                    public void accept(SparseArray<String> stringSparseArray) throws Exception {
                        currentWidgetName.setText(stringSparseArray.get(FilterListAdapter.currentWidgetId));
                    }
                });
    }
}