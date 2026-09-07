package com.evsuite.launcher;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Every value the metrics page can show — head unit and vehicle in one list.
 *
 * <p>The constant name is the persisted key: renaming one drops it from the pages of every
 * driver who had chosen it, so a label change belongs in {@code strings.xml}, never here.
 *
 * <p>{@link #vehicle} is not cosmetic. It decides whether the page builds EVHardware's
 * telemetry reader at all: a driver whose page holds only head-unit cards never binds the
 * vehicle layer, exactly as the standalone vehicle page used not to. It is also what the
 * picker groups by, which is why the two blocks below are declared in display order.
 *
 * <p>The vehicle entries are the ones this launcher can actually read: the SAIC vendor
 * services, which are bound AIDL interfaces needing no car permission, plus the gear position,
 * which EVHardware reads the same vendor way. The standard AAOS properties are gone — every one
 * of them is gated behind a car permission this app does not hold and does not request, so they
 * could only ever have rendered {@code —} on this car. Offering a card that cannot answer is
 * worse than not offering it. See {@code docs/CR-011-metrics-page.md}.
 */
public enum Metric {

    // Head unit. Permission-free reads, except the two data-usage windows (which fall back to
    // the since-boot counter without the usage-stats permission) and WEATHER, which is the
    // only card in the catalogue that needs a runtime permission and a position — see
    // docs/CR-011-metrics-page.md.
    DEVICE(R.string.sys_device, false),
    CHIPSET(R.string.metric_chipset, false),
    CPU_LOAD(R.string.metric_cpu_load, false),
    MEMORY(R.string.sys_memory, false),
    STORAGE(R.string.sys_storage, false),
    NETWORK(R.string.sys_network, false),
    IP_ADDRESS(R.string.metric_ip_address, false),
    DATA_TODAY(R.string.metric_data_today, false),
    DATA_MONTH(R.string.metric_data_month, false),
    DATA_SINCE_BOOT(R.string.metric_data_since_boot, false),
    DATE(R.string.metric_date, false),
    TIME(R.string.metric_time, false),
    WEATHER(R.string.metric_weather, false),
    UPTIME(R.string.metric_uptime, false),
    ANDROID_VERSION(R.string.metric_android_version, false),
    KERNEL(R.string.metric_kernel, false),
    SCREEN(R.string.metric_screen, false),
    LAUNCHER_VERSION(R.string.metric_launcher_version, false),

    // Vehicle — the fields of EVHardware's EnergySnapshot the vendor services answer.
    SOC(R.string.veh_charge, true),
    RANGE(R.string.veh_range, true),
    CHARGING(R.string.veh_charging, true),
    OUTSIDE_TEMP(R.string.metric_outside_temp, true),
    PARKED(R.string.metric_parked, true),
    CLIMATE_POWER(R.string.metric_climate_power, true),
    CLIMATE_AC(R.string.metric_climate_ac, true),
    CLIMATE_AUTO(R.string.metric_climate_auto, true),
    CLIMATE_ECON(R.string.metric_climate_econ, true),
    CLIMATE_RECIRC(R.string.metric_climate_recirc, true),
    CLIMATE_FAN(R.string.metric_climate_fan, true),
    CLIMATE_DRIVER_TEMP(R.string.metric_climate_driver, true),
    CLIMATE_PASSENGER_TEMP(R.string.metric_climate_passenger, true);

    @StringRes
    public final int labelRes;

    /** True when reading it goes through EVHardware rather than the head unit itself. */
    public final boolean vehicle;

    Metric(@StringRes int labelRes, boolean vehicle) {
        this.labelRes = labelRes;
        this.vehicle = vehicle;
    }

    /** The section the picker files this metric under. */
    @StringRes
    public int groupRes() {
        return vehicle ? R.string.metric_group_vehicle : R.string.metric_group_system;
    }

    /** The metric a stored key names, or null when the key is from an older/newer build. */
    @Nullable
    public static Metric byKey(String key) {
        for (Metric metric : values()) {
            if (metric.name().equals(key)) {
                return metric;
            }
        }
        return null;
    }
}
