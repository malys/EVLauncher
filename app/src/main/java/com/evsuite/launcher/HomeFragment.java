package com.evsuite.launcher;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

/**
 * Carousel page 1: the launcher home. A grid of cards each launch one chosen favorite app
 * (long-press to replace or remove); the fourth column opens the drawers and the fixed
 * shortcuts.
 *
 * The grid is built in code rather than laid out in XML: the number of tiles follows how
 * many apps the user has added, and the rows/columns are chosen so the tiles stay as large
 * as that count allows — a fixed layout could only ever fit one count.
 */
public class HomeFragment extends Fragment {

    // Android 9 default Settings and Files packages, launched by the two fixed shortcuts.
    private static final String PKG_SETTINGS = "com.android.settings";
    private static final String PKG_FILES = "com.android.documentsui";

    /**
     * Tiles per row band. Up to four tiles stay on a single row (the original look); beyond
     * that a second and then a third row is added, because a fifth column on this screen is
     * narrower than a fingertip.
     */
    private static final int MAX_TILES_ONE_ROW = 4;
    private static final int MAX_TILES_TWO_ROWS = 8;

    private PreferencesManager preferencesManager;

    private GridLayout favoritesGrid;
    private ImageView settingsIcon;
    private ImageView filesIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferencesManager = new PreferencesManager(requireContext());

        favoritesGrid = view.findViewById(R.id.favorites_grid);

        view.findViewById(R.id.card_all_apps).setOnClickListener(
                v -> openDrawer(AppDrawerActivity.MODE_ALL, -1));

        // Two fixed shortcuts to the Android 9 default Settings and Files apps.
        settingsIcon = view.findViewById(R.id.icon_settings);
        filesIcon = view.findViewById(R.id.icon_files);
        settingsIcon.setOnClickListener(v -> launch(PKG_SETTINGS));
        filesIcon.setOnClickListener(v -> launch(PKG_FILES));
    }

    @Override
    public void onResume() {
        super.onResume();
        // A favorite may have been added or reassigned in the picker, so the whole grid is
        // rebuilt every time rather than patched.
        buildFavorites();
        // Re-resolve the fixed shortcut icons too, in case a target app was installed/updated.
        bindFixedApp(settingsIcon, PKG_SETTINGS);
        bindFixedApp(filesIcon, PKG_FILES);
    }

    /**
     * Rebuilds the favorite grid from the stored list, followed by one "add" tile while
     * there is room left. The add tile is what makes the list growable without a settings
     * screen: it is always the tile after the last favorite.
     */
    private void buildFavorites() {
        List<String> favorites = preferencesManager.getFavorites();
        // A package can disappear between two launches (uninstalled app): drop it rather
        // than showing a tile that can only fail.
        for (int i = favorites.size() - 1; i >= 0; i--) {
            if (!isInstalled(favorites.get(i))) {
                preferencesManager.removeFavorite(i);
                favorites.remove(i);
            }
        }

        boolean hasRoom = favorites.size() < PreferencesManager.MAX_FAVORITES;
        int tiles = favorites.size() + (hasRoom ? 1 : 0);
        int rows = tiles <= MAX_TILES_ONE_ROW ? 1 : (tiles <= MAX_TILES_TWO_ROWS ? 2 : 3);
        int columns = (int) Math.ceil(tiles / (double) rows);

        favoritesGrid.removeAllViews();
        favoritesGrid.setRowCount(rows);
        favoritesGrid.setColumnCount(columns);

        int gap = getResources().getDimensionPixelSize(R.dimen.card_gap);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int index = 0; index < tiles; index++) {
            View card = inflater.inflate(R.layout.item_favorite_card, favoritesGrid, false);
            String pkg = index < favorites.size() ? favorites.get(index) : null;
            bindFavorite(card, index, pkg, rows);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / columns, 1f),
                    GridLayout.spec(index % columns, 1f));
            params.width = 0;
            params.height = 0;
            // Half a gap on each side, so the spacing between two tiles is one full gap and
            // the grid still lines up with the column next to it.
            params.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            favoritesGrid.addView(card, params);
        }
    }

    private void bindFavorite(View card, int index, @Nullable String pkg, int rows) {
        ImageView icon = card.findViewById(R.id.favorite_icon);
        TextView label = card.findViewById(R.id.favorite_label);

        // Past one row the tiles are half as tall; keeping the 30sp label would leave no
        // room for the icon it describes.
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                rows == 1 ? R.dimen.favorite_label_size : R.dimen.favorite_label_size_small));

        if (pkg == null) {
            icon.setImageResource(R.drawable.ic_add);
            label.setText(R.string.add_favorite);
            card.setOnClickListener(v -> openDrawer(AppDrawerActivity.MODE_PICK, index));
            card.setOnLongClickListener(null);
            card.setLongClickable(false);
            return;
        }

        PackageManager pm = requireContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            label.setText(pm.getApplicationLabel(ai));
        } catch (PackageManager.NameNotFoundException e) {
            label.setText(pkg);
        }
        icon.setImageDrawable(highResIcon(pkg));
        card.setOnClickListener(v -> onFavoriteClick(index, pkg));
        card.setOnLongClickListener(v -> {
            showFavoriteMenu(index);
            return true;
        });
    }

    private void onFavoriteClick(int index, String pkg) {
        if (!AppLauncher.launch(requireContext(), pkg)) {
            // Not launchable anymore: let the user reassign the tile.
            Toast.makeText(requireContext(), pkg, Toast.LENGTH_SHORT).show();
            openDrawer(AppDrawerActivity.MODE_PICK, index);
        }
    }

    /**
     * Long-press menu. Removing needs to exist now that the list has a variable length:
     * without it a tile could only ever be swapped for another app, never taken off.
     */
    private void showFavoriteMenu(int index) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.favorite_menu_title)
                .setItems(
                        new CharSequence[]{
                                getString(R.string.favorite_replace),
                                getString(R.string.favorite_remove)},
                        (dialog, which) -> {
                            if (which == 0) {
                                openDrawer(AppDrawerActivity.MODE_PICK, index);
                            } else {
                                preferencesManager.removeFavorite(index);
                                buildFavorites();
                            }
                        })
                .show();
    }

    private boolean isInstalled(String pkg) {
        try {
            requireContext().getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Shows the app's launcher icon, or a placeholder if it isn't installed on this build. */
    private void bindFixedApp(ImageView view, String pkg) {
        Drawable icon = highResIcon(pkg);
        if (icon != null) {
            view.setImageDrawable(icon);
        } else {
            view.setImageResource(R.drawable.ic_add);
        }
    }

    /**
     * Loads the launcher icon at a high density bucket so it stays sharp when scaled up to
     * the large card size, instead of upscaling the device-density icon. Falls back to the
     * package manager's default icon, or null if the package isn't installed.
     */
    private Drawable highResIcon(String pkg) {
        LauncherApps launcherApps = (LauncherApps)
                requireContext().getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps != null) {
            try {
                List<LauncherActivityInfo> activities =
                        launcherApps.getActivityList(pkg, Process.myUserHandle());
                if (!activities.isEmpty()) {
                    Drawable icon = activities.get(0).getIcon(DisplayMetrics.DENSITY_XXXHIGH);
                    if (icon != null) {
                        return icon;
                    }
                }
            } catch (Exception ignored) {
                // Fall back to the default-density icon below.
            }
        }
        try {
            return requireContext().getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void launch(String pkg) {
        if (!AppLauncher.launch(requireContext(), pkg)) {
            Toast.makeText(requireContext(), pkg, Toast.LENGTH_SHORT).show();
        }
    }

    private void openDrawer(String mode, int slot) {
        Intent intent = new Intent(requireContext(), AppDrawerActivity.class);
        intent.putExtra(AppDrawerActivity.EXTRA_MODE, mode);
        intent.putExtra(AppDrawerActivity.EXTRA_SLOT, slot);
        startActivity(intent);
    }
}
