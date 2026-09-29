package com.evsuite.launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Binds a list of {@link AppInfo} into the drawer grid. */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    /** Long-press on a drawer entry — used to jump to its Android app-info screen. */
    public interface OnAppLongClickListener {
        void onAppLongClick(AppInfo app);
    }

    private final List<AppInfo> apps;
    private final OnAppClickListener listener;
    private final OnAppLongClickListener longClickListener;

    public AppListAdapter(List<AppInfo> apps, OnAppClickListener listener,
                           OnAppLongClickListener longClickListener) {
        this.apps = apps;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.label.setText(app.label);
        holder.itemView.setOnClickListener(v -> listener.onAppClick(app));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAppLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
        }
    }
}