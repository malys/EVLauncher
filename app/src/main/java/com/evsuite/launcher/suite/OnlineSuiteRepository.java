package com.evsuite.launcher.suite;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class OnlineSuiteRepository {
    private static final String TAG = "SuiteRepository";
    private static final int TIMEOUT_MS = 10_000;
    private static final long MAX_APK_BYTES = 200L * 1024L * 1024L;
    private static final Map<String, String> RELEASE_APIS = new HashMap<>();

    static {
        RELEASE_APIS.put("com.evsuite.profile", "https://api.github.com/repos/malys/EVProfile/releases/latest");
        RELEASE_APIS.put("com.evsuite.tasker", "https://api.github.com/repos/malys/EVTasker/releases/latest");
        RELEASE_APIS.put("com.evsuite.abrp", "https://api.github.com/repos/malys/EVABRPUploader/releases/latest");
        RELEASE_APIS.put("com.evsuite.swipe", "https://api.github.com/repos/malys/EVSwipe/releases/latest");
        RELEASE_APIS.put("com.evsuite.launcher", "https://api.github.com/repos/malys/EVLauncher/releases/latest");
    }

    private OnlineSuiteRepository() {}

    static List<SuiteAppState> inspect(Context context) {
        return inspect(context, null);
    }

    /** {@code keep} is the verified APK an export still waits on; the purge has to spare it. */
    static List<SuiteAppState> inspect(Context context, File keep) {
        purgeCachedApks(context, keep);
        List<SuiteAppState> apps = SuiteCatalog.installed(context);
        ExecutorService pool = Executors.newFixedThreadPool(apps.size());
        List<Future<?>> checks = new ArrayList<>();
        try {
            for (SuiteAppState app : apps) {
                checks.add(pool.submit(() -> {
                    try {
                        readRelease(app, releaseApi(app.packageName));
                        if (app.downloadUrl == null) return;
                        if (app.installedVersion == null) app.action = SuiteAppState.Action.INSTALL;
                        else if (VersionOrder.isNewer(app.localVersion, app.installedVersion)) {
                            app.action = SuiteAppState.Action.UPDATE;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Release check failed for " + app.packageName, e);
                    }
                }));
            }
            for (Future<?> check : checks) {
                try { check.get(); }
                catch (Exception e) { Log.w(TAG, "Release check worker failed", e); }
            }
        } finally {
            pool.shutdownNow();
        }
        return apps;
    }

    static String releaseApi(String packageName) {
        return RELEASE_APIS.get(packageName);
    }

    private static void readRelease(SuiteAppState app, String api) throws Exception {
        if (!isAllowedUrl(api)) return;
        HttpURLConnection connection = open(new URL(api));
        try {
            if (connection.getResponseCode() != 200) return;
            String body = readText(connection.getInputStream());
            JSONObject release = new JSONObject(body);
            app.changelog = limit(release.optString("body", ""), 6000);
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) return;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) continue;
                String name = asset.optString("name", "").toLowerCase(Locale.US);
                String url = asset.optString("browser_download_url", "");
                if (name.endsWith(".apk") && isStableAsset(name) && isAllowedUrl(url)) {
                    String assetVersion = versionFromAssetName(name);
                    String tagVersion = release.optString("tag_name", "").replaceFirst("^[vV]", "");
                    app.localVersion = assetVersion == null ? tagVersion : assetVersion;
                    app.downloadUrl = url;
                    return;
                }
            }
        } finally { connection.disconnect(); }
    }

    static SuiteDownload download(Context context, SuiteAppState app) {
        if (app.downloadUrl == null || !isAllowedUrl(app.downloadUrl)) {
            return SuiteDownload.failed(SuiteDownload.Failure.BLOCKED_URL);
        }
        File directory = new File(context.getCacheDir(), "suite-apks");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.w(TAG, "Cannot create the APK cache directory: " + directory);
            return SuiteDownload.failed(SuiteDownload.Failure.STORAGE);
        }
        File temporary = new File(directory, app.packageName + ".tmp");
        File target = new File(directory, app.packageName + ".apk");
        try {
            URL current = new URL(app.downloadUrl);
            for (int redirect = 0; redirect < 6; redirect++) {
                if (!isAllowedUrl(current.toString())) {
                    return SuiteDownload.failed(SuiteDownload.Failure.BLOCKED_URL);
                }
                HttpURLConnection connection = open(current);
                try {
                    int status = connection.getResponseCode();
                    if (status >= 300 && status <= 399) {
                        String location = connection.getHeaderField("Location");
                        if (location == null) {
                            return SuiteDownload.failed(SuiteDownload.Failure.SERVER,
                                    status + " without Location");
                        }
                        current = current.toURI().resolve(location).toURL();
                        continue;
                    }
                    if (status != 200) {
                        Log.w(TAG, "Asset request answered " + status + " for " + app.packageName);
                        return SuiteDownload.failed(SuiteDownload.Failure.SERVER,
                                String.valueOf(status));
                    }
                    long total = 0;
                    try (InputStream input = connection.getInputStream();
                         FileOutputStream output = new FileOutputStream(temporary)) {
                        byte[] buffer = new byte[64 * 1024];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            total += count;
                            if (total > MAX_APK_BYTES) {
                                return SuiteDownload.failed(SuiteDownload.Failure.TOO_LARGE);
                            }
                            output.write(buffer, 0, count);
                        }
                        output.getFD().sync();
                    }
                    Files.move(temporary.toPath(), target.toPath(),
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    if (!SuiteApkSecurity.isTrustedSuiteApk(context, target, app.packageName)) {
                        target.delete();
                        return SuiteDownload.failed(SuiteDownload.Failure.REJECTED);
                    }
                    return SuiteDownload.of(target);
                } finally { connection.disconnect(); }
            }
            return SuiteDownload.failed(SuiteDownload.Failure.SERVER, "too many redirects");
        } catch (java.io.IOException e) {
            Log.w(TAG, "APK download failed for " + app.packageName, e);
            // A full or read-only cache reports itself as an IOException on the write.
            boolean storage = temporary.exists() || !directory.canWrite();
            return SuiteDownload.failed(
                    storage ? SuiteDownload.Failure.STORAGE : SuiteDownload.Failure.NETWORK,
                    e.getClass().getSimpleName());
        } catch (Exception e) {
            Log.w(TAG, "APK download failed for " + app.packageName, e);
            return SuiteDownload.failed(SuiteDownload.Failure.NETWORK, e.getClass().getSimpleName());
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    static void purgeCachedApks(Context context) {
        purgeCachedApks(context, null);
    }

    static void purgeCachedApks(Context context, File keep) {
        File directory = new File(context.getCacheDir(), "suite-apks");
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".apk") || name.endsWith(".tmp"));
        if (files == null) return;
        for (File file : files) {
            if (keep != null && keep.getAbsolutePath().equals(file.getAbsolutePath())) continue;
            if (!file.delete()) Log.w(TAG, "Could not delete temporary suite APK: " + file.getName());
        }
    }

    static boolean isAllowedUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            String host = uri.getHost();
            return "api.github.com".equalsIgnoreCase(host)
                    || "github.com".equalsIgnoreCase(host)
                    || "objects.githubusercontent.com".equalsIgnoreCase(host)
                    || "release-assets.githubusercontent.com".equalsIgnoreCase(host);
        } catch (Exception ignored) { return false; }
    }

    /** Every suite release ships its stable APK as {@code <App>-stable-<version>.apk}. */
    static boolean isStableAsset(String name) {
        return !name.contains("unstable") && (name.contains("stable") || name.contains("release"));
    }

    static String versionFromAssetName(String name) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("-(\\d[0-9.]*)\\.apk$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(name);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static HttpURLConnection open(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "EVLauncher-Android");
        return connection;
    }

    private static String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String readText(InputStream input) throws Exception {
        try (InputStream source = input;
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
