package com.evsuite.launcher;

import android.Manifest;
import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.evsuite.hardware.DataUsage;
import com.evsuite.hardware.catalog.VehicleEnums;
import com.evsuite.hardware.saic.SaicWeather;
import com.evsuite.hardware.telemetry.EnergySnapshot;
import com.evsuite.hardware.telemetry.EnergyTelemetryReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Carousel page 2: one customisable grid of metrics, head unit and vehicle together.
 *
 * <p>It replaces the two fixed pages the launcher used to have — device/memory/storage/network,
 * then charge/range/charging. Those seven are still what a fresh install shows, but they are
 * now a default rather than a layout: the driver adds, replaces and removes cards with exactly
 * the gestures the favourite apps already use on the home page (tap the trailing tile to add,
 * long-press a card to replace or remove).
 *
 * <p><b>The launcher owns no vehicle access of its own.</b> Every vehicle value here comes from
 * EVHardware's typed, nullable telemetry API; this class holds no property id, no vendor
 * transaction and no setter, and no card on this page writes anything to the car.
 *
 * <p><b>Unavailable is not zero.</b> A car that does not report a value shows {@code —} and a
 * caption saying why, because a launcher that prints "0 km" is telling the driver something
 * false about a car that said nothing.
 *
 * <p><b>The car is asked nothing until a vehicle card is on the page.</b> The reader is built
 * on the first refresh that actually needs it, so a page of system cards binds no vehicle
 * layer at all, and the ticker runs only between {@code onResume} and {@code onPause}.
 */
public class MetricsFragment extends Fragment {

    /** System values move in seconds; this tick is a {@code /proc}-level read. */
    private static final long SYSTEM_REFRESH_MS = 3_000;

    /**
     * Vehicle values are read at most this often, whatever the tick rate. State of charge and
     * range move over minutes, and this read reaches the vehicle layer rather than the kernel.
     */
    private static final long VEHICLE_REFRESH_MS = 5_000;

    /**
     * The weather is a bound service call that can reach the network, so it is read far more
     * rarely than anything else here — the sky does not change between two 3 s ticks.
     */
    private static final long WEATHER_REFRESH_MS = 10 * 60_000L;

    /** Position cadence for the weather card. A city's weather is not a lane-level question. */
    private static final long LOCATION_INTERVAL_MS = 60_000L;
    private static final float LOCATION_DISTANCE_M = 1_000f;

    private static final double GB = 1024d * 1024d * 1024d;

    /**
     * Tiles per row band, matching the home page: up to four cards stay on a single row,
     * beyond that a second and then a third row is added.
     */
    private static final int MAX_TILES_ONE_ROW = 4;
    private static final int MAX_TILES_TWO_ROWS = 8;

    /** Past this many characters the display size clips on a quarter-width card. */
    private static final int LONG_VALUE_CHARS = 12;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private PreferencesManager preferencesManager;
    private GridLayout grid;

    /** The chosen metrics, parallel to the cards in {@link #grid}. */
    private final List<Metric> shown = new ArrayList<>();

    @Nullable
    private EnergyTelemetryReader reader;
    @Nullable
    private EnergySnapshot snapshot;
    private long snapshotReadMs;

    /**
     * Previous {@code /proc/stat} totals. CPU load is a ratio between two samples, so the
     * first tick after the page opens has nothing to compare against and shows the dash.
     */
    private long cpuTotalTicks;
    private long cpuIdleTicks;

    /**
     * The weather query blocks for up to two seconds waiting on a callback, so it never runs
     * on the tick. One thread is enough: there is at most one query in flight.
     */
    @Nullable
    private ExecutorService weatherExecutor;
    @Nullable
    private volatile SaicWeather.Reading weather;
    private long weatherReadMs;

    /** Last position, from the subscription below. Null until a provider has delivered one. */
    @Nullable
    private volatile Location location;
    @Nullable
    private LocationListener locationListener;

    /** Asked once per page display, and only when a weather card is actually on the page. */
    private boolean locationAsked;

    private final ActivityResultLauncher<String[]> locationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            granted -> startLocationUpdates());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, SYSTEM_REFRESH_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_metrics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferencesManager = new PreferencesManager(requireContext());
        grid = view.findViewById(R.id.metrics_grid);
        weatherExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onResume() {
        super.onResume();
        buildGrid();
        // The position subscription follows the card, not the app: no weather card on the
        // page means no provider is ever subscribed to.
        if (shown.contains(Metric.WEATHER)) {
            startLocationUpdates();
        }
        handler.post(ticker);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        stopLocationUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // The reader outlives no view: a fragment recreated by the pager builds a new one
        // against the application context rather than inheriting a stale reference.
        reader = null;
        snapshot = null;
        snapshotReadMs = 0;
        cpuTotalTicks = 0;
        cpuIdleTicks = 0;
        weather = null;
        weatherReadMs = 0;
        location = null;
        locationAsked = false;
        if (weatherExecutor != null) {
            weatherExecutor.shutdownNow();
            weatherExecutor = null;
        }
    }

    /**
     * Rebuilds the whole grid from the stored list, followed by one "add" tile while there is
     * room left. Rebuilding rather than patching is what makes the list growable without a
     * settings screen — the add tile is always the tile after the last metric.
     */
    private void buildGrid() {
        shown.clear();
        for (String key : preferencesManager.getMetrics()) {
            Metric metric = Metric.byKey(key);
            // A key this build does not know (downgrade, or a metric that was dropped) is
            // skipped rather than shown as an empty card.
            if (metric != null) {
                shown.add(metric);
            }
        }

        boolean hasRoom = shown.size() < PreferencesManager.MAX_METRICS;
        int tiles = shown.size() + (hasRoom ? 1 : 0);
        int rows = tiles <= MAX_TILES_ONE_ROW ? 1 : (tiles <= MAX_TILES_TWO_ROWS ? 2 : 3);
        int columns = (int) Math.ceil(tiles / (double) rows);

        grid.removeAllViews();
        grid.setRowCount(rows);
        grid.setColumnCount(columns);

        int gap = getResources().getDimensionPixelSize(R.dimen.card_gap);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int index = 0; index < tiles; index++) {
            View card = inflater.inflate(R.layout.item_metric_card, grid, false);
            bindCard(card, index, index < shown.size() ? shown.get(index) : null);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / columns, 1f),
                    GridLayout.spec(index % columns, 1f));
            params.width = 0;
            params.height = 0;
            // Half a gap on each side, so the spacing between two cards is one full gap and
            // the outer edge still lines up with the home page.
            params.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            grid.addView(card, params);
        }

        refresh();
    }

    private void bindCard(View card, int index, @Nullable Metric metric) {
        TextView label = card.findViewById(R.id.metric_label);
        TextView value = card.findViewById(R.id.metric_value);
        TextView caption = card.findViewById(R.id.metric_caption);

        if (metric == null) {
            label.setText(R.string.metric_add);
            value.setText(R.string.metric_add_sign);
            caption.setText("");
            card.setOnClickListener(v -> showPicker(index));
            card.setOnLongClickListener(null);
            card.setLongClickable(false);
            return;
        }

        label.setText(metric.labelRes);
        card.setOnClickListener(null);
        card.setClickable(false);
        card.setOnLongClickListener(v -> {
            showCardMenu(index);
            return true;
        });
    }

    /**
     * Long-press menu, mirroring the favourite apps: replace this card with another metric, or
     * take it off the page. Removing has to exist — without it a card could only ever be
     * swapped, never dropped.
     */
    private void showCardMenu(int index) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.metric_menu_title)
                .setItems(
                        new CharSequence[]{
                                getString(R.string.metric_replace),
                                getString(R.string.metric_remove)},
                        (dialog, which) -> {
                            if (which == 0) {
                                showPicker(index);
                            } else {
                                preferencesManager.removeMetric(index);
                                buildGrid();
                            }
                        })
                .show();
    }

    /**
     * Opens the full-screen picker for the tile at {@code index} — appending when it is the
     * add tile, replacing otherwise.
     *
     * <p>The picker writes the choice and finishes, like the app drawer does for favourites;
     * {@code onResume} rebuilds the grid from preferences, so there is one owner of the write
     * and no result to plumb back.
     */
    private void showPicker(int index) {
        Intent intent = new Intent(requireContext(), MetricPickerActivity.class);
        intent.putExtra(MetricPickerActivity.EXTRA_SLOT, index);
        startActivity(intent);
    }

    private void refresh() {
        Context ctx = getContext();
        if (ctx == null || grid.getChildCount() == 0) {
            return;
        }
        readVehicleIfDue(ctx);
        readWeatherIfDue(ctx);
        for (int i = 0; i < shown.size(); i++) {
            renderCard(grid.getChildAt(i), shown.get(i), ctx);
        }
    }

    /**
     * Refreshes the vehicle snapshot at most every {@link #VEHICLE_REFRESH_MS}, and only when
     * a vehicle card is actually on the page.
     *
     * <p>Building the reader initialises EVHardware and binds the vendor service hub, which is
     * why it happens here and not in {@code onCreate}: a driver whose page holds only system
     * cards never asks the car anything. EVHardware owns the binding and its own reconnection
     * watchdog, so there is deliberately nothing here that tears a bound service down.
     */
    private void readVehicleIfDue(Context ctx) {
        boolean needed = false;
        for (Metric metric : shown) {
            if (metric.vehicle) {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (snapshotReadMs != 0 && now - snapshotReadMs < VEHICLE_REFRESH_MS) {
            return;
        }
        snapshotReadMs = now;
        // The launcher is the home screen: a vehicle layer that throws must cost a value on a
        // card, never the screen the driver returns to.
        try {
            if (reader == null) {
                reader = new EnergyTelemetryReader(ctx.getApplicationContext());
            }
            snapshot = reader.read(System.currentTimeMillis());
        } catch (Throwable t) {
            snapshot = null;
        }
    }

    private void renderCard(View card, Metric metric, Context ctx) {
        TextView value = card.findViewById(R.id.metric_value);
        TextView caption = card.findViewById(R.id.metric_caption);

        String[] rendered = read(metric, ctx);
        value.setText(rendered[0]);
        caption.setText(rendered[1]);
        // "Plugged in, not charging" is a sentence, not a number: at the display size it would
        // wrap to three lines on a quarter of the panel.
        value.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                rendered[0].length() > LONG_VALUE_CHARS
                        ? R.dimen.text_title
                        : R.dimen.sys_value_size));
    }

    /** The value and caption of one card. A read that throws costs the card, not the page. */
    private String[] read(Metric metric, Context ctx) {
        try {
            return metric.vehicle ? readVehicle(metric) : readSystem(metric, ctx);
        } catch (Exception e) {
            // System services and filesystem stats can throw transiently (e.g. /data
            // remounting during an OTA); a refresh tick must never crash the launcher.
            return new String[]{getString(R.string.veh_unknown), ""};
        }
    }

    // --- Head unit ------------------------------------------------------------------------

    private String[] readSystem(Metric metric, Context ctx) {
        String absent = getString(R.string.metric_unreadable);
        switch (metric) {
            case MEMORY:
                return new String[]{memoryText(ctx), getString(R.string.sys_used_total)};
            case STORAGE:
                return new String[]{storageText(), getString(R.string.sys_free_total)};
            case NETWORK:
                return networkText(ctx);
            case CHIPSET:
                return new String[]{chipset(), cpuDescription()};
            case CPU_LOAD:
                return card(cpuLoad(), R.string.metric_cap_cpu_load, absent);
            case IP_ADDRESS:
                return card(ipAddress(), R.string.metric_cap_ip_address, absent);
            case DATA_TODAY:
                return card(dataOver(ctx, DataUsage.INSTANCE.startOfDay()),
                        R.string.metric_cap_data_today, absent);
            case DATA_MONTH:
                return card(dataOver(ctx, DataUsage.INSTANCE.startOfMonth()),
                        R.string.metric_cap_data_month, absent);
            case DATA_SINCE_BOOT:
                return new String[]{
                        megabytes(DataUsage.INSTANCE.sinceBoot().getTotalBytes() / 1_048_576L),
                        getString(R.string.metric_cap_data_since_boot)};
            case DATE:
                return new String[]{
                        android.text.format.DateFormat.getMediumDateFormat(ctx).format(new Date()),
                        new java.text.SimpleDateFormat("EEEE", Locale.getDefault())
                                .format(new Date())};
            case TIME:
                // The system's own 12/24-hour setting, not a format this app decides.
                return new String[]{
                        android.text.format.DateFormat.getTimeFormat(ctx).format(new Date()),
                        TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)};
            case WEATHER:
                return weatherText(ctx);
            case UPTIME:
                return new String[]{
                        formatUptime(SystemClock.elapsedRealtime()),
                        getString(R.string.metric_cap_uptime)};
            case ANDROID_VERSION:
                return new String[]{
                        Build.VERSION.RELEASE,
                        getString(R.string.metric_cap_android_version, Build.VERSION.SDK_INT)};
            case KERNEL:
                return card(System.getProperty("os.version"), R.string.metric_cap_kernel, absent);
            case SCREEN:
                return screenText();
            case LAUNCHER_VERSION:
                return launcherText(ctx, absent);
            case DEVICE:
            default:
                return deviceText(ctx);
        }
    }

    /** The head unit's chipset. {@code SOC_MODEL} is API 31+, so AAOS 9 falls back to the board. */
    private String chipset() {
        String soc = Build.VERSION.SDK_INT >= 31 ? Build.SOC_MODEL : null;
        if (soc == null || soc.isEmpty() || Build.UNKNOWN.equals(soc)) {
            soc = Build.BOARD == null || Build.BOARD.isEmpty() ? Build.HARDWARE : Build.BOARD;
        }
        return soc == null || soc.isEmpty() ? getString(R.string.veh_unknown) : soc;
    }

    /** "8 cores · 2.0 GHz", dropping the clock when the kernel does not publish it. */
    private String cpuDescription() {
        String cores = getString(R.string.metric_cpu_cores,
                Runtime.getRuntime().availableProcessors());
        String khz = readFirstLine("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        if (khz == null) {
            return cores;
        }
        try {
            return cores + " · " + String.format(Locale.getDefault(), "%.1f GHz",
                    Long.parseLong(khz.trim()) / 1_000_000d);
        } catch (NumberFormatException e) {
            return cores;
        }
    }

    /**
     * Processor load as a percentage, from the jiffy counters in {@code /proc/stat}.
     *
     * <p>It is a ratio between this tick and the previous one, so the first read after the
     * page opens only primes the counters and shows the dash.
     */
    @Nullable
    private String cpuLoad() {
        String line = readFirstLine("/proc/stat");
        if (line == null || !line.startsWith("cpu ")) {
            return null;
        }
        long total = 0;
        long idle = 0;
        String[] fields = line.trim().split("\\s+");
        for (int i = 1; i < fields.length; i++) {
            try {
                long ticks = Long.parseLong(fields[i]);
                total += ticks;
                // Fields 4 and 5 after the label are idle and iowait.
                if (i == 4 || i == 5) {
                    idle += ticks;
                }
            } catch (NumberFormatException ignored) {
                // A field the kernel formats differently is not worth failing the card over.
            }
        }

        long totalDelta = total - cpuTotalTicks;
        long idleDelta = idle - cpuIdleTicks;
        cpuTotalTicks = total;
        cpuIdleTicks = idle;
        if (totalDelta <= 0) {
            return null;
        }
        long busy = Math.max(0, totalDelta - idleDelta);
        return Math.round(busy * 100d / totalDelta) + " %";
    }

    /** The first non-loopback IPv4 address of an interface that is up, or null. */
    @Nullable
    private static String ipAddress() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nif.isLoopback() || !nif.isUp()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nif.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // No route to read is the same as no address to show.
        }
        return null;
    }

    /**
     * Data carried since {@code startMs}, or null when the counters are not readable.
     *
     * <p>Null rather than zero, deliberately: "used nothing" and "could not read" must not
     * look alike, or a permission the head unit did not grant passes for a quiet month.
     */
    @Nullable
    private String dataOver(Context ctx, long startMs) {
        Integer mb = DataUsage.INSTANCE.megabytesSince(ctx.getApplicationContext(), startMs);
        return mb == null ? null : megabytes(mb);
    }

    private String[] screenText() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return new String[]{
                dm.widthPixels + " × " + dm.heightPixels,
                getString(R.string.metric_cap_screen, dm.densityDpi)};
    }

    private String[] launcherText(Context ctx, String absent) {
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return new String[]{
                    pi.versionName,
                    getString(R.string.metric_cap_launcher_version, pi.getLongVersionCode())};
        } catch (PackageManager.NameNotFoundException e) {
            return new String[]{getString(R.string.veh_unknown), absent};
        }
    }

    /** Megabytes below a gigabyte, gigabytes above — a five-digit MB count is unreadable. */
    private static String megabytes(long mb) {
        return mb < 1024
                ? mb + " MB"
                : String.format(Locale.getDefault(), "%.1f GB", mb / 1024d);
    }

    @Nullable
    private static String readFirstLine(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.readLine();
        } catch (Exception e) {
            // /proc and /sys entries are not guaranteed readable on every build.
            return null;
        }
    }

    private String[] deviceText(Context ctx) {
        String model = capitalize(Build.MANUFACTURER) + " " + Build.MODEL;
        String android = getString(R.string.sys_android,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
        String uptime = getString(R.string.sys_uptime,
                formatUptime(SystemClock.elapsedRealtime()));
        String launcher;
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            launcher = getString(R.string.sys_launcher, pi.versionName, pi.getLongVersionCode());
        } catch (PackageManager.NameNotFoundException e) {
            launcher = "";
        }
        return new String[]{model, android + "\n" + uptime + "\n" + launcher};
    }

    private String memoryText(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return getString(R.string.veh_unknown);
        }
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return formatGb(mi.totalMem - mi.availMem) + " / " + formatGb(mi.totalMem) + " GB";
    }

    private String storageText() {
        try {
            // Matches the figures the user sees in system Settings (whole primary volume).
            StorageStatsManager stats = (StorageStatsManager)
                    requireContext().getSystemService(Context.STORAGE_STATS_SERVICE);
            return formatGb(stats.getFreeBytes(StorageManager.UUID_DEFAULT)) + " / "
                    + formatGb(stats.getTotalBytes(StorageManager.UUID_DEFAULT)) + " GB";
        } catch (Exception e) {
            // Fall back to the data partition figures if storage stats are unavailable.
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            return formatGb(fs.getAvailableBytes()) + " / " + formatGb(fs.getTotalBytes()) + " GB";
        }
    }

    private String[] networkText(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        String type = getString(R.string.net_offline);
        String detail = "";
        if (cm != null) {
            Network active = cm.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : cm.getNetworkCapabilities(active);
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    type = getString(R.string.net_wifi);
                    detail = wifiLinkSpeed(ctx);
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    type = getString(R.string.net_mobile);
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    type = getString(R.string.net_ethernet);
                }
            }
        }
        return new String[]{type, detail};
    }

    // --- Weather --------------------------------------------------------------------------

    /**
     * Current conditions where the car is, from the head unit's own weather service.
     *
     * <p>Three silences, and the card says which: no permission yet, no position yet, or a
     * service that did not answer. None of them is a zero degrees.
     */
    private String[] weatherText(Context ctx) {
        String dash = getString(R.string.veh_unknown);
        if (!hasLocationPermission(ctx)) {
            return new String[]{dash, getString(R.string.metric_cap_weather_permission)};
        }
        if (location == null) {
            return new String[]{dash, getString(R.string.metric_cap_weather_locating)};
        }
        SaicWeather.Reading reading = weather;
        if (reading == null) {
            return new String[]{dash, getString(R.string.metric_cap_weather_unavailable)};
        }

        Double celsius = reading.getTemperatureCelsius();
        String value = celsius == null
                ? reading.getText()
                : String.format(Locale.getDefault(), "%.0f °C", celsius);
        // The phrase is the provider's own wording, in the head unit's language; the city is
        // what it decided the position is in. Either may be empty.
        String caption = celsius == null ? reading.getCity() : reading.getText();
        if (celsius != null && !reading.getCity().isEmpty()) {
            caption = caption + " · " + reading.getCity();
        }
        return new String[]{value, caption};
    }

    /**
     * Re-queries the weather at most every {@link #WEATHER_REFRESH_MS}, off the main thread,
     * and only when a weather card is on the page and a position is known.
     *
     * <p>The service takes a callback binder and answers on it, so EVHardware makes the call
     * synchronous with a two-second bound. Two seconds is a frozen home screen, which is why
     * this never runs on the tick.
     */
    private void readWeatherIfDue(Context ctx) {
        ExecutorService executor = weatherExecutor;
        Location fix = location;
        if (executor == null || fix == null || !shown.contains(Metric.WEATHER)) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (weatherReadMs != 0 && now - weatherReadMs < WEATHER_REFRESH_MS) {
            return;
        }
        weatherReadMs = now;

        Context app = ctx.getApplicationContext();
        String language = Locale.getDefault().getLanguage();
        double latitude = fix.getLatitude();
        double longitude = fix.getLongitude();
        executor.execute(() -> {
            // A head unit that is not a car, or a map stack that is not installed, must cost
            // this one card and nothing else. The next tick renders whatever landed here.
            try {
                SaicWeather.INSTANCE.connect(app);
                weather = SaicWeather.INSTANCE.currentAt(latitude, longitude, language);
            } catch (Throwable t) {
                weather = null;
            }
        });
    }

    /**
     * Either grade counts: since API 31 the user may grant the approximate one when the
     * precise one was asked for, and a city's weather does not need the precise one.
     */
    private static boolean hasLocationPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Subscribes to a position while the weather card is visible.
     *
     * <p>A subscription rather than {@code getLastKnownLocation} alone, because on a head unit
     * with no other GPS client the cache is empty and stays empty — the same reason
     * EVTasker's vehicle service holds one. It is dropped in {@code onPause}, so the launcher
     * subscribes to nothing while the driver is not looking at the page.
     */
    private void startLocationUpdates() {
        Context ctx = getContext();
        if (ctx == null || locationListener != null) {
            return;
        }
        if (!hasLocationPermission(ctx)) {
            // Asked when the card is first shown, never at launch, and once per page display:
            // a driver who declines is not asked again every time they swipe back.
            if (!locationAsked) {
                locationAsked = true;
                locationPermission.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
            return;
        }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            return;
        }
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location fix) {
                location = fix;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        try {
            for (String provider : new String[]{
                    LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
                if (!lm.isProviderEnabled(provider)) {
                    continue;
                }
                // A cached fix, when some other client left one, spares the first query a wait.
                if (location == null) {
                    location = lm.getLastKnownLocation(provider);
                }
                lm.requestLocationUpdates(provider, LOCATION_INTERVAL_MS, LOCATION_DISTANCE_M,
                        listener, Looper.getMainLooper());
                locationListener = listener;
                return;
            }
        } catch (SecurityException | IllegalArgumentException e) {
            // A permission revoked between the check and the call, or a provider this build
            // does not have: the card says "waiting for a position" rather than crashing.
            locationListener = null;
        }
    }

    private void stopLocationUpdates() {
        Context ctx = getContext();
        if (ctx == null || locationListener == null) {
            return;
        }
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            lm.removeUpdates(locationListener);
        }
        locationListener = null;
    }

    // --- Vehicle --------------------------------------------------------------------------

    private String[] readVehicle(Metric metric) {
        EnergySnapshot s = snapshot;
        // Two different silences. Nothing at all readable means the vehicle layer is not
        // answering — a bind that has not landed, or a head unit that is not a car. One
        // missing value among several that answered means this car does not publish it.
        String absent = s != null && s.getHasVehicleData()
                ? getString(R.string.veh_unavailable_here)
                : getString(R.string.veh_no_vehicle_data);
        if (s == null) {
            return new String[]{getString(R.string.veh_unknown), absent};
        }

        switch (metric) {
            case SOC:
                return card(number(s.getSocPercent(), "%.0f %%"), R.string.veh_soc_caption, absent);
            case RANGE:
                return card(number(s.getRangeKm(), "%.0f km"), R.string.veh_range_caption, absent);
            case CHARGING:
                return charging(s, absent);
            case OUTSIDE_TEMP:
                return card(number(s.getOutsideTempCelsius(), "%.0f °C"),
                        R.string.metric_cap_temp, absent);
            case PARKED:
                return card(parked(s.getParked()), R.string.metric_cap_parked, absent);
            case CLIMATE_POWER:
                return card(onOff(s.getClimate().getPowerOn()), R.string.metric_cap_climate, absent);
            case CLIMATE_AC:
                return card(onOff(s.getClimate().getAcOn()), R.string.metric_cap_climate, absent);
            case CLIMATE_AUTO:
                return card(onOff(s.getClimate().getAutoOn()), R.string.metric_cap_climate, absent);
            case CLIMATE_ECON:
                return card(onOff(s.getClimate().getEconOn()), R.string.metric_cap_climate, absent);
            case CLIMATE_RECIRC:
                return card(onOff(s.getClimate().getRecirculationOn()),
                        R.string.metric_cap_climate, absent);
            case CLIMATE_FAN:
                return card(fanLevel(s), R.string.metric_cap_fan, absent);
            case CLIMATE_DRIVER_TEMP:
                return card(number(s.getClimate().getDriverTargetCelsius(), "%.1f °C"),
                        R.string.metric_cap_target, absent);
            case CLIMATE_PASSENGER_TEMP:
            default:
                return card(number(s.getClimate().getPassengerTargetCelsius(), "%.1f °C"),
                        R.string.metric_cap_target, absent);
        }
    }

    /**
     * The charging card, from the vendor charging service alone.
     *
     * <p>It used to add the AAOS charge-port flag as its caption. That property needs
     * {@code CAR_ENERGY_PORTS}, which no app in the suite holds, so the flag was always null
     * and the caption always the fallback — a second signal that never once answered.
     */
    private String[] charging(EnergySnapshot s, String absent) {
        Integer status = s.getChargingStatus();
        return status == null
                ? new String[]{getString(R.string.veh_unknown), absent}
                : new String[]{chargingStatusLabel(status), getString(R.string.veh_charging_caption)};
    }

    /**
     * The suite's own charging vocabulary, translated once in EVHardware.
     *
     * <p>A status the library does not name is shown as unknown rather than as a number: the
     * raw value means nothing to a driver, and inventing a label for it would be a guess about
     * a firmware nobody has read.
     */
    private String chargingStatusLabel(int status) {
        switch (status) {
            case VehicleEnums.CHARGING_UNPLUGGED:
                return getString(com.evsuite.hardware.R.string.charging_unplugged);
            case VehicleEnums.CHARGING_AC:
                return getString(com.evsuite.hardware.R.string.charging_ac);
            case VehicleEnums.CHARGING_DC:
                return getString(com.evsuite.hardware.R.string.charging_dc);
            case VehicleEnums.CHARGING_PLUGGED_IDLE:
                return getString(com.evsuite.hardware.R.string.charging_plugged_idle);
            case VehicleEnums.CHARGING_DONE:
                return getString(com.evsuite.hardware.R.string.charging_done);
            case VehicleEnums.CHARGING_FAULT:
                return getString(com.evsuite.hardware.R.string.charging_fault);
            default:
                return getString(R.string.veh_unknown);
        }
    }

    /** Fan level, with the car's own maximum when it publishes one ("3 / 7"). */
    @Nullable
    private String fanLevel(EnergySnapshot s) {
        Integer level = s.getClimate().getFanLevel();
        if (level == null) {
            return null;
        }
        Integer max = s.getClimate().getFanLevelMax();
        return max == null ? String.valueOf(level) : level + " / " + max;
    }

    /** A card whose value is null is the dash plus the caption that says which silence it is. */
    private String[] card(@Nullable String value, @StringRes int captionRes, String absent) {
        return value == null
                ? new String[]{getString(R.string.veh_unknown), absent}
                : new String[]{value, getString(captionRes)};
    }

    @Nullable
    private static String number(@Nullable Float value, String pattern) {
        return value == null ? null : String.format(Locale.getDefault(), pattern, value);
    }

    @Nullable
    private String onOff(@Nullable Boolean on) {
        return on == null ? null : getString(on ? R.string.metric_on : R.string.metric_off);
    }

    @Nullable
    private String parked(@Nullable Boolean inPark) {
        return inPark == null ? null : getString(inPark
                ? R.string.metric_parked_yes
                : R.string.metric_parked_no);
    }

    private static String formatGb(long bytes) {
        return String.format(Locale.getDefault(), "%.1f", bytes / GB);
    }

    /** Human-readable uptime, e.g. "1d 3h 12m" (days dropped when zero). */
    private static String formatUptime(long elapsedMs) {
        long totalSeconds = elapsedMs / 1000;
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        return sb.append(hours).append("h ").append(minutes).append("m").toString();
    }

    /** Wi-Fi negotiated link speed (e.g. "120 Mbps"), or empty when unavailable. */
    private static String wifiLinkSpeed(Context ctx) {
        WifiManager wm = (WifiManager)
                ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            WifiInfo info = wm.getConnectionInfo();
            if (info != null && info.getLinkSpeed() >= 0) {
                return info.getLinkSpeed() + " Mbps";
            }
        }
        return "";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
