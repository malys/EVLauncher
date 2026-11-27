package com.evsuite.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Icons the driver chose for favourite apps, one PNG per package in {@code filesDir/icons}.
 *
 * <p>Keyed by package, not by tile, so moving a tile keeps its icon. The source image is
 * whatever the document picker returns — possibly a 50-megapixel photo — so it is decoded
 * subsampled and stored at {@link #MAX_PX} at most: the launcher is the home screen, and an
 * out-of-memory error here leaves the head unit without one.
 */
final class CustomIcons {
    private static final String TAG = "EVLauncher";
    private static final String DIR = "icons";
    private static final String SUFFIX = ".png";

    /** Longest side of a stored icon. Enough for the largest tile at the panel density. */
    static final int MAX_PX = 288;

    /** A package name, and nothing that could leave the icons directory. */
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");

    private CustomIcons() {
    }

    /** The stored icon, or null when there is none or it cannot be read. */
    @Nullable
    static Drawable load(Context context, String pkg) {
        File file = file(context, pkg);
        if (file == null || !file.isFile()) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        return bitmap == null ? null : new BitmapDrawable(context.getResources(), bitmap);
    }

    static boolean exists(Context context, String pkg) {
        File file = file(context, pkg);
        return file != null && file.isFile();
    }

    /**
     * Decodes {@code source}, scales it to {@link #MAX_PX} and replaces the package's icon.
     * Blocking — never on the main thread. False when the image cannot be read or written;
     * the previous icon is then left untouched.
     */
    static boolean save(Context context, String pkg, Uri source) {
        File target = file(context, pkg);
        if (target == null) {
            return false;
        }
        File tmp = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                if (in == null) {
                    return false;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return false;
            }
            BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_PX);
            Bitmap decoded;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                if (in == null) {
                    return false;
                }
                decoded = BitmapFactory.decodeStream(in, null, decode);
            }
            if (decoded == null) {
                return false;
            }
            Bitmap scaled = scale(decoded);

            File dir = target.getParentFile();
            if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) {
                return false;
            }
            // Unique temp file, then rename over the target: no moment without an icon file
            // and no half-written PNG if the head unit loses power mid-write. It lives in the
            // cache, not beside the icons, so retainOnly() never sees — and deletes — a save
            // still in flight; both directories are in the app's data dir, one mount.
            tmp = File.createTempFile(pkg, ".tmp", context.getCacheDir());
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                if (!scaled.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    return false;
                }
                out.getFD().sync();
            }
            if (!tmp.renameTo(target)) {
                return false;
            }
            tmp = null;
            return true;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            // Runs on a worker thread: anything escaping would take the home screen down.
            Log.w(TAG, "Custom icon refused for " + pkg, e);
            return false;
        } finally {
            if (tmp != null && !tmp.delete()) {
                Log.w(TAG, "Temporary icon not deleted: " + tmp.getName());
            }
        }
    }

    static void delete(Context context, String pkg) {
        File file = file(context, pkg);
        if (file != null && file.isFile() && !file.delete()) {
            Log.w(TAG, "Custom icon not deleted for " + pkg);
        }
    }

    /**
     * Deletes the icons of packages that are no longer on the home page — removed, replaced or
     * uninstalled — so the directory never outgrows the twelve tiles.
     */
    static void retainOnly(Context context, Collection<String> packages) {
        File[] files = new File(context.getFilesDir(), DIR).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            boolean kept = name.endsWith(SUFFIX)
                    && packages.contains(name.substring(0, name.length() - SUFFIX.length()));
            if (!kept && !file.delete()) {
                Log.w(TAG, "Stale icon not deleted: " + name);
            }
        }
    }

    /**
     * The power-of-two subsampling that keeps the decoded longest side at or above
     * {@code target} and below twice it, so the decode costs at most {@code (2·target)²} pixels
     * whatever the source size. Pure, for the JVM tests.
     */
    static int sampleSize(int width, int height, int target) {
        int longest = Math.max(width, height);
        int sample = 1;
        while (longest / (sample * 2) >= target) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap scale(Bitmap bitmap) {
        int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longest <= MAX_PX) {
            return bitmap;
        }
        float ratio = MAX_PX / (float) longest;
        int width = Math.max(1, Math.round(bitmap.getWidth() * ratio));
        int height = Math.max(1, Math.round(bitmap.getHeight() * ratio));
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    @Nullable
    private static File file(Context context, String pkg) {
        if (pkg == null || !PACKAGE.matcher(pkg).matches()) {
            return null;
        }
        return new File(new File(context.getFilesDir(), DIR), pkg + SUFFIX);
    }
}
