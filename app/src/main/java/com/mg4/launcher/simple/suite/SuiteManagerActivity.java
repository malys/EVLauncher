package com.mg4.launcher.simple.suite;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mg4.launcher.simple.AppLauncher;
import com.mg4.launcher.simple.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Installed-app inventory plus the unstable channel's signed GitHub release catalogue. */
public final class SuiteManagerActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RecyclerView list;
    private TextView repositoryStatus;
    private boolean firstResume = true;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suite_manager);
        list = findViewById(R.id.suite_app_list);
        repositoryStatus = findViewById(R.id.suite_repository_status);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.suite_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.suite_refresh_button).setOnClickListener(v -> refresh());
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (firstResume) firstResume = false; else refresh();
    }

    private void refresh() {
        repositoryStatus.setText(R.string.suite_loading);
        executor.execute(() -> {
            java.util.List<SuiteAppState> apps = SuiteRepositoryHook.inspect(this);
            runOnUiThread(() -> render(apps));
        });
    }

    private void render(java.util.List<SuiteAppState> apps) {
        if (isFinishing() || isDestroyed()) return;
        repositoryStatus.setText(SuiteRepositoryHook.isOnline()
                ? R.string.suite_online_ready : R.string.suite_stable_offline);
        list.setAdapter(new SuiteAppAdapter(apps, this::onAction));
    }

    private void onAction(SuiteAppState app) {
        if (app.action == SuiteAppState.Action.OPEN) {
            if (!AppLauncher.launch(this, app.packageName)) Toast.makeText(this,
                    app.packageName, Toast.LENGTH_SHORT).show();
            return;
        }
        String notes = app.changelog == null || app.changelog.trim().isEmpty()
                ? getString(R.string.suite_no_release_notes) : app.changelog;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.suite_release_notes, app.localVersion))
                .setMessage(notes)
                .setNegativeButton(R.string.suite_cancel, null)
                .setPositiveButton(R.string.suite_confirm_install, (dialog, which) -> downloadAndInstall(app))
                .show();
    }

    private void downloadAndInstall(SuiteAppState app) {
        repositoryStatus.setText(getString(R.string.suite_downloading, app.name));
        executor.execute(() -> {
            java.io.File apk = SuiteRepositoryHook.download(this, app);
            boolean installed = apk != null && SuiteRepositoryHook.install(this, app, apk);
            if (apk != null && apk.exists()) apk.delete();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (apk == null) {
                    repositoryStatus.setText(R.string.suite_download_failed);
                    return;
                }
                repositoryStatus.setText(installed
                        ? R.string.suite_install_complete : R.string.suite_install_failed);
                if (installed) refresh();
            });
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
