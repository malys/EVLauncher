package com.mg4.launcher.simple.suite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mg4.launcher.simple.R;

import java.util.List;

final class SuiteAppAdapter extends RecyclerView.Adapter<SuiteAppAdapter.Holder> {
    interface Listener { void onAction(SuiteAppState app); }

    private final List<SuiteAppState> apps;
    private final Listener listener;

    SuiteAppAdapter(List<SuiteAppState> apps, Listener listener) {
        this.apps = apps;
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suite_app, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        SuiteAppState app = apps.get(position);
        holder.name.setText(app.name);
        holder.status.setText(status(holder.itemView, app));
        holder.action.setText(actionLabel(holder.itemView, app.action));
        holder.action.setVisibility(app.action == SuiteAppState.Action.NONE ? View.INVISIBLE : View.VISIBLE);
        holder.action.setOnClickListener(v -> listener.onAction(app));
    }

    @Override public int getItemCount() { return apps.size(); }

    private static String status(View view, SuiteAppState app) {
        if (app.invalidApk) return view.getContext().getString(R.string.suite_apk_invalid);
        if (app.action == SuiteAppState.Action.UPDATE) return view.getContext().getString(
                R.string.suite_update_available, app.installedVersion, app.localVersion);
        if (app.action == SuiteAppState.Action.INSTALL) return view.getContext().getString(
                R.string.suite_ready_to_install, app.localVersion);
        if (app.installedVersionCode >= 0 && app.localVersion != null) return view.getContext().getString(
                R.string.suite_up_to_date, app.installedVersion);
        if (app.installedVersionCode >= 0) return view.getContext().getString(
                R.string.suite_installed, app.installedVersion);
        return view.getContext().getString(R.string.suite_not_installed);
    }

    private static int actionLabel(View view, SuiteAppState.Action action) {
        switch (action) {
            case INSTALL: return R.string.suite_install;
            case UPDATE: return R.string.suite_update;
            case OPEN: return R.string.suite_open;
            default: return R.string.suite_open;
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, status, action;
        Holder(View item) {
            super(item);
            name = item.findViewById(R.id.suite_app_name);
            status = item.findViewById(R.id.suite_app_status);
            action = item.findViewById(R.id.suite_app_action);
        }
    }
}
