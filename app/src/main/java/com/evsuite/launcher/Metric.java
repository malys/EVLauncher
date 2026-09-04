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
 * <p>The vehicle entries cover EVHardware's whole read-only snapshot, including signals this
 * launcher holds no car permission for. Those are not a lie: they read as {@code —} with
 * "unavailable on this car", which is what the page already says about anything the vehicle
 * does not answer. See {@code docs/CR-011-metrics-page.md}.
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

    // Vehicle — every field of EVHardware's EnergySnapshot.
    SOC(R.string.veh_charge, true),
    RANGE(R.string.veh_range, true),
    CHARGING(R.string.veh_charging, true),
    CHARGE_PORT(R.string.metric_charge_port, true),
    SPEED(R.string.metric_speed, true),
    BATTERY_POWER(R.string.metric_battery_power, true),
    BATTERY_ENERGY(R.string.metric_battery_energy, true),
    BATTERY_CAPACITY(R.string.metric_battery_capacity, true),
    BATTERY_TEMP(R.string.metric_battery_temp, true),
    OUTSIDE_TEMP(R.string.metric_outside_temp, true),
    CABIN_TEMP(R.string.metric_cabin_temp, true),
    ODOMETER(R.string.metric_odometer, true),
    PARKED(R.string.metric_parked, true),
    CLIMATE_POWER(R.string.metric_climate_power, true),
    CLIMATE_AC(R.string.metric_climate_ac, true),
    CLIMATE_AUTO(R.string.metric_climate_auto, true),
    CLIMATE_ECON(R.string.metric_climate_econ, true),
    CLIMATE_RECIRC(R.string.metric_climate_recirc, true),
    CLIMATE_FAN(R.string.metric_climate_fan, true),
    CLIMATE_DRIVER_TEMP(R.string.metric_climate_driver, true),
    CLIMATE_PASSENGER_TEMP(R.string.metric_climate_passenger, true),
    TIRE_FRONT_LEFT(R.string.metric_tire_fl, true),
    TIRE_FRONT_RIGHT(R.string.metric_tire_fr, true),
    TIRE_REAR_LEFT(R.string.metric_tire_rl, true),
    TIRE_REAR_RIGHT(R.string.metric_tire_rr, true);

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
