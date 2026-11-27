package com.evsuite.launcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Carousel page 1: the launcher home. A grid of cards each launch one chosen favorite app
 * (long-press to replace, move, re-icon or remove); a narrow side column opens the drawer and
 * the fixed shortcuts.
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

    private static final String STATE_ICON_TARGET = "icon_target";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Decodes and writes picked icons. One thread: the driver picks one image at a time. */
    private final ExecutorService iconExecutor = Executors.newSingleThreadExecutor();

    /**
     * The package whose icon is being picked. Kept in the saved state: the launcher may be
     * killed while the document picker is in front, and the result arrives after recreation.
     */
    @Nullable
    private String iconTarget;

    /**
     * The tile picked up with "Move", or -1. While set, a tap on any tile drops it there
     * instead of launching. Cleared when the page goes away, so it never outlives the screen
     * the driver saw it on.
     */
    private int movingFrom = -1;

    private final ActivityResultLauncher<String> iconPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::onIconPicked);

    private PreferencesManager preferencesManager;

    private GridLayout favoritesGrid;
    private ImageView settingsIcon;
    private ImageView filesIcon;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            iconTarget = savedInstanceState.getString(STATE_ICON_TARGET);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ICON_TARGET, iconTarget);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iconExecutor.shutdownNow();
    }

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

    @Override
    public void onPause() {
        super.onPause();
        movingFrom = -1;
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
        // Removed, replaced and uninstalled apps all end here, so this is the one place a
        // custom icon without a tile is cleaned up.
        CustomIcons.retainOnly(requireContext(), favorites);
        if (movingFrom >= favorites.size()) {
            movingFrom = -1;
        }

        boolean hasRoom = favorites.size() < PreferencesManager.MAX_FAVORITES;
        int tiles = favorites.size() + (hasRoom ? 1 : 0);
        int rows = tiles <= MAX_TILES_ONE_ROW ? 1 : (tiles <= MAX_TILES_TWO_ROWS ? 2 : 3);
        int columns = (int) Math.ceil(tiles / (double) rows);

        favoritesGrid.removeAllViews();
        favoritesGrid.setRowCount(rows);
        favoritesGrid.setColumnCount(columns);

        int halfGap = getResources().getDimensionPixelSize(R.dimen.home_half_gap);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int index = 0; index < tiles; index++) {
            View card = inflater.inflate(R.layout.item_favorite_card, favoritesGrid, false);
            String pkg = index < favorites.size() ? favorites.get(index) : null;
            bindFavorite(card, index, pkg, rows, favorites.size());

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / columns, 1f),
                    GridLayout.spec(index % columns, 1f));
            params.width = 0;
            params.height = 0;
            // Half a gap on each side, so the spacing between two tiles is one full gap and
            // the grid lines up with the side column, whose cards carry the same margin.
            params.setMargins(halfGap, halfGap, halfGap, halfGap);
            favoritesGrid.addView(card, params);
        }
    }

    private void bindFavorite(View card, int index, @Nullable String pkg, int rows, int count) {
        ImageView icon = card.findViewById(R.id.favorite_icon);
        TextView label = card.findViewById(R.id.favorite_label);

        // Past one row the tiles are half as tall; keeping the 30sp label would leave no
        // room for the icon it describes.
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                rows == 1 ? R.dimen.favorite_label_size : R.dimen.favorite_label_size_small));

        if (pkg == null) {
            icon.setImageResource(R.drawable.ic_add);
            label.setText(R.string.add_favorite);
            card.setOnLongClickListener(null);
            card.setLongClickable(false);
            if (movingFrom >= 0) {
                // Dropping on the add tile means "at the end".
                card.setOnClickListener(v -> finishMove(count - 1));
            } else {
                card.setOnClickListener(v -> openDrawer(AppDrawerActivity.MODE_PICK, index));
            }
            return;
        }

        PackageManager pm = requireContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            label.setText(pm.getApplicationLabel(ai));
        } catch (PackageManager.NameNotFoundException e) {
            label.setText(pkg);
        }
        Drawable custom = CustomIcons.load(requireContext(), pkg);
        icon.setImageDrawable(custom != null ? custom : highResIcon(pkg));
        icon.setContentDescription(label.getText());
        if (movingFrom >= 0) {
            if (index == movingFrom) {
                card.setBackgroundResource(R.drawable.card_moving);
            }
            card.setOnClickListener(v -> finishMove(index));
            card.setOnLongClickListener(null);
            card.setLongClickable(false);
            return;
        }
        card.setOnClickListener(v -> onFavoriteClick(index, pkg));
        card.setOnLongClickListener(v -> {
            showFavoriteMenu(index, pkg);
            return true;
        });
    }

    /** Drops the picked-up tile at {@code to}; a tap on the tile itself puts it back. */
    private void finishMove(int to) {
        int from = movingFrom;
        movingFrom = -1;
        if (from >= 0 && to >= 0) {
            preferencesManager.moveFavorite(from, to);
        }
        buildFavorites();
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
     * without it a tile could only ever be swapped for another app, never taken off. The
     * "restore" entry is only offered when there is a custom icon to drop.
     */
    private void showFavoriteMenu(int index, String pkg) {
        List<CharSequence> items = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        items.add(getString(R.string.favorite_replace));
        actions.add(() -> openDrawer(AppDrawerActivity.MODE_PICK, index));
        if (preferencesManager.getFavorites().size() > 1) {
            items.add(getString(R.string.tile_move));
            actions.add(() -> startMove(index));
        }
        items.add(getString(R.string.favorite_icon_change));
        actions.add(() -> pickIcon(pkg));
        if (CustomIcons.exists(requireContext(), pkg)) {
            items.add(getString(R.string.favorite_icon_reset));
            actions.add(() -> {
                CustomIcons.delete(requireContext(), pkg);
                buildFavorites();
            });
        }
        items.add(getString(R.string.favorite_remove));
        actions.add(() -> {
            preferencesManager.removeFavorite(index);
            buildFavorites();
        });
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.favorite_menu_title)
                .setItems(items.toArray(new CharSequence[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void startMove(int index) {
        movingFrom = index;
        buildFavorites();
        Toast.makeText(requireContext(), R.string.tile_move_hint, Toast.LENGTH_LONG).show();
    }

    private void pickIcon(String pkg) {
        iconTarget = pkg;
        try {
            iconPicker.launch("image/*");
        } catch (ActivityNotFoundException e) {
            iconTarget = null;
            Toast.makeText(requireContext(), R.string.icon_picker_missing, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Picker result. The decode runs off the main thread — the image can be a full-size
     * photo — and the grid is rebuilt only if this page is still attached when it lands.
     */
    private void onIconPicked(@Nullable Uri uri) {
        String pkg = iconTarget;
        iconTarget = null;
        if (uri == null || pkg == null) {
            return;
        }
        Context app = requireContext().getApplicationContext();
        iconExecutor.execute(() -> {
            boolean saved = CustomIcons.save(app, pkg, uri);
            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) {
                    return;
                }
                if (saved) {
                    buildFavorites();
                } else {
                    Toast.makeText(requireContext(), R.string.icon_failed, Toast.LENGTH_LONG).show();
                }
            });
        });
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
