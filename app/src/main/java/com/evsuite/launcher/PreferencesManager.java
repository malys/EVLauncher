package com.evsuite.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists the favorite app packages shown on the home page.
 *
 * The list is ordered and has no holes: index 0 is always the first tile. The launcher used
 * to have three fixed slots, so {@link #getFavorites()} migrates the old
 * {@code favorite_0..2} keys on first read — updating the launcher keeps the home page.
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "ev_system_launcher";
    private static final String KEY_FAVORITES = "favorites";

    /** Legacy fixed-slot keys, read once and migrated to {@link #KEY_FAVORITES}. */
    private static final String KEY_LEGACY_PREFIX = "favorite_";
    private static final int LEGACY_COUNT = 3;

    /**
     * Upper bound on the home page. Twelve tiles is a 4x3 grid; past that an icon is
     * smaller than a fingertip on the head unit and the launcher stops being usable
     * while driving.
     */
    public static final int MAX_FAVORITES = 12;

    /** Package names never contain a newline, so it can separate them safely. */
    private static final String SEPARATOR = "\n";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The favorites in display order. Never null, possibly empty. */
    public List<String> getFavorites() {
        String stored = prefs.getString(KEY_FAVORITES, null);
        if (stored == null) {
            return migrateLegacy();
        }
        List<String> favorites = new ArrayList<>();
        for (String pkg : stored.split(SEPARATOR)) {
            if (!pkg.isEmpty()) {
                favorites.add(pkg);
            }
        }
        return favorites;
    }

    /**
     * Assigns a package to a tile. An index equal to the current size appends — that is what
     * the trailing "add" tile sends. Anything beyond is ignored rather than leaving a hole.
     */
    public void setFavorite(int index, String packageName) {
        List<String> favorites = getFavorites();
        if (index < 0 || index > favorites.size()) {
            return;
        }
        if (index == favorites.size()) {
            if (favorites.size() >= MAX_FAVORITES) {
                return;
            }
            favorites.add(packageName);
        } else {
            favorites.set(index, packageName);
        }
        persist(favorites);
    }

    public void removeFavorite(int index) {
        List<String> favorites = getFavorites();
        if (index < 0 || index >= favorites.size()) {
            return;
        }
        favorites.remove(index);
        persist(favorites);
    }

    private void persist(List<String> favorites) {
        prefs.edit().putString(KEY_FAVORITES, join(favorites)).apply();
    }

    /** Reads the three old fixed slots once, writes them as a list, and drops the old keys. */
    private List<String> migrateLegacy() {
        List<String> favorites = new ArrayList<>();
        SharedPreferences.Editor edit = prefs.edit();
        for (int slot = 0; slot < LEGACY_COUNT; slot++) {
            String pkg = prefs.getString(KEY_LEGACY_PREFIX + slot, null);
            if (pkg != null) {
                favorites.add(pkg);
            }
            edit.remove(KEY_LEGACY_PREFIX + slot);
        }
        edit.putString(KEY_FAVORITES, join(favorites)).apply();
        return favorites;
    }

    private static String join(List<String> favorites) {
        StringBuilder sb = new StringBuilder();
        for (String pkg : favorites) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(pkg);
        }
        return sb.toString();
    }
}
