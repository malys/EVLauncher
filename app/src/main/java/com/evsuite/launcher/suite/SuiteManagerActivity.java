package com.evsuite.launcher.suite;

import android.os.Bundle;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evsuite.launcher.AppLauncher;
import com.evsuite.launcher.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/** User-initiated release check and verified APK export. Installation is always manual. */
public final class SuiteManagerActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RecyclerView list;
    private TextView repositoryStatus;
    private File pendingApk;
    private final ActivityResultLauncher<String> saveApk = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/vnd.android.package-archive"),
            this::copyPendingApk);

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

    private void refresh() {
        repositoryStatus.setText(R.string.suite_loading);
        executor.execute(() -> {
            java.util.List<SuiteAppState> apps = SuiteRepository.inspect(this);
            runOnUiThread(() -> render(apps));
        });
    }

    private void render(java.util.List<SuiteAppState> apps) {
        if (isFinishing() || isDestroyed()) return;
        repositoryStatus.setText(R.string.suite_online_ready);
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
                .setMessage(notes + "\n\n" + getString(R.string.suite_manual_install_help))
                .setNegativeButton(R.string.suite_cancel, null)
                .setPositiveButton(R.string.suite_confirm_download, (dialog, which) -> download(app))
                .show();
    }

    private void download(SuiteAppState app) {
        repositoryStatus.setText(getString(R.string.suite_downloading, app.name));
        executor.execute(() -> {
            File apk = SuiteRepository.download(this, app);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (apk == null) {
                    repositoryStatus.setText(R.string.suite_download_failed);
                    return;
                }
                pendingApk = apk;
                repositoryStatus.setText(R.string.suite_choose_destination);
                saveApk.launch(safeFileName(app));
            });
        });
    }

    private String safeFileName(SuiteAppState app) {
        String version = app.localVersion == null ? "latest"
                : app.localVersion.replaceAll("[^0-9A-Za-z._-]", "_");
        return app.name.replaceAll("[^0-9A-Za-z._-]", "-") + "-" + version + ".apk";
    }

    private void copyPendingApk(Uri destination) {
        File source = pendingApk;
        pendingApk = null;
        if (source == null) return;
        if (destination == null) {
            source.delete();
            repositoryStatus.setText(R.string.suite_online_ready);
            return;
        }
        executor.execute(() -> {
            boolean saved = false;
            try (FileInputStream input = new FileInputStream(source);
                 OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                if (output == null) throw new java.io.IOException("No destination stream");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.flush();
                saved = true;
            } catch (Exception ignored) {
                // The actionable, localized error is shown below; no path or URI is exposed.
            } finally {
                source.delete();
            }
            boolean result = saved;
            runOnUiThread(() -> repositoryStatus.setText(result
                    ? R.string.suite_download_saved : R.string.suite_save_failed));
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (pendingApk != null) pendingApk.delete();
        executor.shutdownNow();
    }
}
