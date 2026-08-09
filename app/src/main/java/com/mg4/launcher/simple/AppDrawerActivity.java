package com.mg4.launcher.simple;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mg4.launcher.simple.suite.SuiteManagerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-screen drawer that lists launchable apps. Three modes:
 *  - ALL: every launchable app, tap to launch.
 *  - SYSTEM: only system apps, tap to launch.
 *  - PICK: every launchable app, tap to assign it to a favorite slot, then return.
 */
public class AppDrawerActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SLOT = "slot";
    public static final String MODE_ALL = "all";
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_PICK = "pick";

    private static final String REPOSITORY_URL = "https://github.com/malys/MG4SimpleLauncher";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String mode;
    private int slot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_ALL;
        }
        slot = getIntent().getIntExtra(EXTRA_SLOT, -1);

        TextView title = findViewById(R.id.drawer_title);
        title.setText(titleForMode());

        // Explicit back affordance for the head unit, mirroring the system back gesture.
        findViewById(R.id.drawer_back_button).setOnClickListener(v -> finish());


        // System apps are reached from the "all apps" drawer header; redundant elsewhere.
        View systemApps = findViewById(R.id.system_apps_button);
        View suiteApps = findViewById(R.id.suite_apps_button);
        View aboutButton = findViewById(R.id.about_button);
        if (MODE_ALL.equals(mode)) {
            systemApps.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppDrawerActivity.class);
                intent.putExtra(EXTRA_MODE, MODE_SYSTEM);
                startActivity(intent);
            });
            suiteApps.setOnClickListener(v ->
                    startActivity(new Intent(this, SuiteManagerActivity.class)));
            aboutButton.setOnClickListener(v -> showAbout());
        } else {
            systemApps.setVisibility(View.GONE);
            suiteApps.setVisibility(View.GONE);
            aboutButton.setVisibility(View.GONE);
        }

        RecyclerView grid = findViewById(R.id.app_grid);
        int span = Math.max(4, getResources().getConfiguration().screenWidthDp / 130);
        grid.setLayoutManager(new GridLayoutManager(this, span));

        loadApps(grid);
    }

    private void showAbout() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = getString(R.string.about_version_unknown);
        }
        View content = getLayoutInflater().inflate(R.layout.dialog_about, null);
        content.<TextView>findViewById(R.id.about_version).setText(getString(R.string.about_version, version));
        ImageView qr = content.findViewById(R.id.about_qr_code);
        android.graphics.Bitmap bitmap = QrCode.generate(REPOSITORY_URL, 416);
        if (bitmap != null) qr.setImageBitmap(bitmap);
        content.findViewById(R.id.about_repository).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(content).create();
        content.<MaterialButton>findViewById(R.id.about_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private String titleForMode() {
        switch (mode) {
            case MODE_SYSTEM:
                return getString(R.string.system_apps);
            case MODE_PICK:
                return getString(R.string.pick_favorite_title);
            default:
                return getString(R.string.all_apps);
        }
    }

    private void loadApps(RecyclerView grid) {
        executor.execute(() -> {
            List<AppInfo> apps = queryApps();
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                grid.setAdapter(new AppListAdapter(apps, this::onAppClick));
            });
        });
    }

    private List<AppInfo> queryApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

        String ownPackage = getPackageName();
        List<AppInfo> apps = new ArrayList<>();
        for (ResolveInfo ri : resolveInfos) {
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(ownPackage)) {
                continue;
            }
            // An updated system app (e.g. preinstalled Maps the user updated) counts as
            // a user app, so it shows up in "all apps" rather than the system drawer.
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
            if (MODE_SYSTEM.equals(mode)) {
                if (!system) {
                    continue;
                }
            } else if (MODE_ALL.equals(mode)) {
                if (system) {
                    continue;
                }
            }
            // MODE_PICK keeps every app so any can be assigned as a favorite.
            String label = ri.loadLabel(pm).toString();
            apps.add(new AppInfo(label, pkg, ri.loadIcon(pm), system));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return apps;
    }

    private void onAppClick(AppInfo app) {
        if (MODE_PICK.equals(mode)) {
            if (slot >= 0) {
                new PreferencesManager(this).setFavorite(slot, app.packageName);
            }
            finish();
            return;
        }
        launch(app.packageName);
    }

    private void launch(String packageName) {
        if (!AppLauncher.launch(this, packageName)) {
            Toast.makeText(this, packageName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
