package com.evsuite.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Persists the two ordered, user-arranged lists the carousel is built from: the favorite app
 * packages on the home page, and the metrics on the metrics page.
 *
 * Both lists are ordered and have no holes: index 0 is always the first tile. The launcher used
 * to have three fixed favourite slots, so {@link #getFavorites()} migrates the old
 * {@code favorite_0..2} keys on first read — updating the launcher keeps the home page.
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "ev_system_launcher";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_METRICS = "metrics";

    /** Legacy fixed-slot keys, read once and migrated to {@link #KEY_FAVORITES}. */
    private static final String KEY_LEGACY_PREFIX = "favorite_";
    private static final int LEGACY_COUNT = 3;

    /**
     * Upper bound on the home page. Twelve tiles is a 4x3 grid; past that an icon is
     * smaller than a fingertip on the head unit and the launcher stops being usable
     * while driving.
     */
    public static final int MAX_FAVORITES = 12;

    /** Same 4x3 ceiling on the metrics page, for the same reason: a readable card. */
    public static final int MAX_METRICS = 12;

    /**
     * What the metrics page shows before anyone customises it: the four cards of the old
     * system page followed by the three of the old vehicle page, in that order. Merging the
     * two pages must not silently take a value away from a driver who never opens the picker.
     */
    private static final List<String> DEFAULT_METRICS = Arrays.asList(
            Metric.DEVICE.name(),
            Metric.MEMORY.name(),
            Metric.STORAGE.name(),
            Metric.NETWORK.name(),
            Metric.SOC.name(),
            Metric.RANGE.name(),
            Metric.CHARGING.name());

    /** Package and metric names never contain a newline, so it can separate them safely. */
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
        return split(stored);
    }

    /**
     * Assigns a package to a tile. An index equal to the current size appends — that is what
     * the trailing "add" tile sends. Anything beyond is ignored rather than leaving a hole.
     */
    public void setFavorite(int index, String packageName) {
        setAt(KEY_FAVORITES, getFavorites(), index, packageName, MAX_FAVORITES);
    }

    public void removeFavorite(int index) {
        removeAt(KEY_FAVORITES, getFavorites(), index);
    }

    /**
     * The chosen metrics in display order, as {@link Metric} names.
     *
     * <p>Unset means "never customised", which is the default page rather than an empty one.
     * An explicitly emptied page is stored as an empty string and stays empty.
     */
    public List<String> getMetrics() {
        String stored = prefs.getString(KEY_METRICS, null);
        return stored == null ? new ArrayList<>(DEFAULT_METRICS) : split(stored);
    }

    public void setMetric(int index, String metricName) {
        setAt(KEY_METRICS, getMetrics(), index, metricName, MAX_METRICS);
    }

    public void removeMetric(int index) {
        removeAt(KEY_METRICS, getMetrics(), index);
    }

    private void setAt(String key, List<String> values, int index, String value, int max) {
        if (index < 0 || index > values.size()) {
            return;
        }
        if (index == values.size()) {
            if (values.size() >= max) {
                return;
            }
            values.add(value);
        } else {
            values.set(index, value);
        }
        persist(key, values);
    }

    private void removeAt(String key, List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return;
        }
        values.remove(index);
        persist(key, values);
    }

    private void persist(String key, List<String> values) {
        prefs.edit().putString(key, join(values)).apply();
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

    private static List<String> split(String stored) {
        List<String> values = new ArrayList<>();
        for (String value : stored.split(SEPARATOR)) {
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(value);
        }
        return sb.toString();
    }
}
