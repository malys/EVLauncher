package com.evsuite.launcher.suite;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private static final String TAG = "SuiteManager";
    /** The document picker covers this activity: the system may recreate us before the result. */
    private static final String STATE_PENDING_APK = "pending_apk";
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RecyclerView list;
    private TextView repositoryStatus;
    private File pendingApk;
    private final ActivityResultLauncher<String> saveApk = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(APK_MIME),
            this::copyPendingApk);

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suite_manager);
        list = findViewById(R.id.suite_app_list);
        repositoryStatus = findViewById(R.id.suite_repository_status);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.suite_back_button).setOnClickListener(v -> finish());
        findViewById(R.id.suite_refresh_button).setOnClickListener(v -> refresh());
        if (savedInstanceState != null) {
            String path = savedInstanceState.getString(STATE_PENDING_APK);
            if (path != null) pendingApk = new File(path);
        }
        refresh();
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingApk != null) outState.putString(STATE_PENDING_APK, pendingApk.getAbsolutePath());
    }

    private void refresh() {
        repositoryStatus.setText(R.string.suite_loading);
        File keep = pendingApk;
        executor.execute(() -> {
            java.util.List<SuiteAppState> apps = SuiteRepository.inspect(this, keep);
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
        String fileName = safeFileName(app);
        executor.execute(() -> {
            SuiteDownload result = SuiteRepository.download(this, app);
            File apk = result.file;
            // Head units often ship no document picker, so write straight into the public
            // Downloads folder and keep the picker as the fallback.
            boolean exported = apk != null && saveToDownloads(apk, fileName);
            if (exported) apk.delete();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (apk == null) {
                    repositoryStatus.setText(failureMessage(result));
                    return;
                }
                if (exported) {
                    repositoryStatus.setText(R.string.suite_download_saved);
                    return;
                }
                pendingApk = apk;
                repositoryStatus.setText(R.string.suite_choose_destination);
                try {
                    saveApk.launch(fileName);
                } catch (ActivityNotFoundException e) {
                    // A head unit without a document picker: say so instead of crashing home.
                    Log.w(TAG, "No document picker to export the APK", e);
                    pendingApk = null;
                    apk.delete();
                    repositoryStatus.setText(R.string.suite_no_file_picker);
                }
            });
        });
    }

    /** Naming the cause is what makes a failed update reportable from the car. */
    private String failureMessage(SuiteDownload result) {
        String message = getString(result.failure.messageId);
        return result.detail == null ? message : message + " (" + result.detail + ")";
    }

    /** Writes the verified APK into the public Downloads collection without any picker. */
    private boolean saveToDownloads(File source, String fileName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, APK_MIME);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        // Stays invisible to other apps until the copy is complete.
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri item = null;
        try {
            item = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (item == null) return false;
            if (!copy(source, item)) {
                resolver.delete(item, null, null);
                return false;
            }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(item, values, null, null);
            return true;
        } catch (Exception e) {
            // Falling back to the picker is the point; logcat carries the actual cause.
            Log.w(TAG, "Direct save to Downloads failed", e);
            if (item != null) {
                try { resolver.delete(item, null, null); } catch (Exception ignored) { }
            }
            return false;
        }
    }

    private String safeFileName(SuiteAppState app) {
        String version = app.localVersion == null ? "latest"
                : app.localVersion.replaceAll("[^0-9A-Za-z._-]", "_");
        return app.name.replaceAll("[^0-9A-Za-z._-]", "-") + "-" + version + ".apk";
    }

    private void copyPendingApk(Uri destination) {
        File source = pendingApk;
        pendingApk = null;
        if (destination == null) {
            if (source != null) source.delete();
            repositoryStatus.setText(R.string.suite_online_ready);
            return;
        }
        if (source == null || !source.isFile()) {
            Log.w(TAG, "Export destination chosen but the verified APK is gone");
            repositoryStatus.setText(R.string.suite_save_failed);
            return;
        }
        executor.execute(() -> {
            boolean saved;
            try {
                saved = copy(source, destination);
            } finally {
                source.delete();
            }
            runOnUiThread(() -> repositoryStatus.setText(saved
                    ? R.string.suite_download_saved : R.string.suite_save_failed));
        });
    }

    private boolean copy(File source, Uri destination) {
        // "wt" truncates: "w" alone can leave the tail of a larger existing file behind.
        try (FileInputStream input = new FileInputStream(source);
             OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new java.io.IOException("No destination stream");
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
            return true;
        } catch (Exception e) {
            // The user-facing message stays generic; logcat carries the actual cause.
            Log.w(TAG, "APK export failed", e);
            return false;
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        // Keep the verified APK when the system only recreates us behind the picker; the
        // restored instance still has to copy it. The cache purge cleans it up otherwise.
        if (pendingApk != null && isFinishing()) pendingApk.delete();
        executor.shutdownNow();
    }
}
