package com.evsuite.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The launcher's vehicle boundary, asserted rather than documented.
 *
 * <p>{@code AGENTS.md} says this app is read-only on the vehicle: it may present telemetry, but
 * only through EVHardware's typed read-only API, and it owns no property id, no vendor
 * transaction and no setter. That rule is one careless import away from being false, and the
 * failure is silent — an app that reaches {@code CarPropertyManager} directly compiles, runs,
 * and quietly becomes the sixth place in the suite where vehicle access is re-derived.
 *
 * <p>Scanning source text rather than bytecode is deliberate: what is being defended is what a
 * reviewer reads. The EVHardware submodule is not scanned — vehicle access is exactly what it
 * is for.
 */
public class VehicleBoundaryTest {

    /** Substrings that must not appear anywhere in this app's own sources. */
    private static final String[][] FORBIDDEN = {
            {"android.car.", "the launcher must not touch the car API directly"},
            {"CarPropertyManager", "property access belongs to EVHardware, not here"},
            {"SaicVehicleControl", "that class writes to the vehicle"},
            {"SaicClimate", "climate writes belong to EVProfile"},
            {"SaicCharging", "charging control belongs to EVProfile"},
            {"VehicleWriteGate", "a read-only app has no write to gate"},
            {"TaskerBridge", "no IPC to EVProfile"},
            {"ProfileBridge", "no IPC to EVProfile"},
    };

    /**
     * EVHardware entry points this app is allowed to name.
     *
     * <p>An allowlist rather than a "no EVHardware setters" pattern: the library's setters are
     * not all called {@code set*}, and naming what is permitted fails closed on the next one
     * added.
     */
    private static final List<String> ALLOWED_HARDWARE_TYPES = List.of(
            "com.evsuite.hardware.telemetry.EnergySnapshot",
            "com.evsuite.hardware.telemetry.EnergyTelemetryReader",
            "com.evsuite.hardware.catalog.VehicleEnums",
            "com.evsuite.hardware.R");

    private static List<Path> appSources() throws IOException {
        Path root = Paths.get("src");
        assertTrue("source root not found — has the module layout changed?", Files.isDirectory(root));
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    // This file names every forbidden string on purpose.
                    .filter(p -> !p.getFileName().toString().equals("VehicleBoundaryTest.java"))
                    .collect(Collectors.toList());
        }
    }

    private static String read(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    public void the_launcher_names_no_direct_vehicle_api() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path source : appSources()) {
            String text = read(source);
            for (String[] rule : FORBIDDEN) {
                if (text.contains(rule[0])) {
                    offences.add(source + " contains \"" + rule[0] + "\": " + rule[1]);
                }
            }
        }
        if (!offences.isEmpty()) {
            fail(String.join("\n", offences));
        }
    }

    @Test
    public void only_the_read_only_telemetry_surface_is_imported() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path source : appSources()) {
            for (String line : read(source).split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("import com.evsuite.hardware")) {
                    continue;
                }
                String type = trimmed.substring("import ".length()).replace(";", "").trim();
                if (ALLOWED_HARDWARE_TYPES.stream().noneMatch(type::equals)) {
                    offences.add(source + " imports " + type
                            + ", which is not part of the read-only telemetry surface");
                }
            }
        }
        if (!offences.isEmpty()) {
            fail(String.join("\n", offences));
        }
    }

    @Test
    public void the_manifest_declares_no_vehicle_write_and_no_shared_uid() throws IOException {
        String manifest = read(Paths.get("src/main/AndroidManifest.xml"));

        // A shared UID would hand the launcher the platform's own permissions wholesale, which
        // is the one thing a read-only home screen must never quietly acquire.
        assertFalse("sharedUserId must never appear in the launcher",
                manifest.contains("sharedUserId"));

        // Every car permission the app holds must be one the boundary review named. A new one
        // arriving with a feature is exactly the drift the permission gate exists to catch,
        // and this says so in the test suite as well as in the CI.
        for (String line : manifest.split("\n")) {
            if (!line.contains("android.car.permission.")) {
                continue;
            }
            boolean known = line.contains("android.car.permission.CAR_ENERGY")
                    || line.contains("android.car.permission.CAR_VENDOR_EXTENSION");
            assertTrue("unreviewed car permission in the manifest: " + line.trim(), known);
            // CONTROL_* is the platform's own naming for a permission that can write.
            assertFalse("a read-only launcher holds no CONTROL_ car permission: " + line.trim(),
                    line.contains("android.car.permission.CONTROL_"));
        }
    }
}
