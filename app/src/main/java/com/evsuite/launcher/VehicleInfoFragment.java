package com.evsuite.launcher;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.evsuite.hardware.catalog.VehicleEnums;
import com.evsuite.hardware.telemetry.EnergySnapshot;
import com.evsuite.hardware.telemetry.EnergyTelemetryReader;

import java.util.Locale;

/**
 * Carousel page 3: read-only vehicle telemetry — state of charge, remaining range, charging
 * state.
 *
 * <p>The launcher owns no vehicle access of its own. Every value here comes from EVHardware's
 * typed, nullable telemetry API; this class holds no property id, no vendor transaction and no
 * setter, and there is nothing on this page a driver can press. See
 * {@code docs/CR-010-vehicle-page.md} for why this is a page of its own rather than three more
 * cards on the system-information page.
 *
 * <p><b>Unavailable is not zero.</b> A car that does not report its range shows {@code —} and a
 * caption saying why, because a launcher that prints "0 km" is telling the driver something
 * false about a car that said nothing.
 *
 * <p><b>Nothing is read while the page is not in front of the driver.</b> The reader is built
 * the first time this page becomes visible — a driver who never swipes here never binds the
 * vehicle layer at all — and the refresh ticker runs only between {@code onResume} and
 * {@code onPause}. EVHardware owns the binding itself and keeps its own reconnection watchdog,
 * so there is deliberately nothing here that tears a bound service down underneath it.
 */
public class VehicleInfoFragment extends Fragment {

    /**
     * Slower than the system page's three seconds. State of charge and range move over
     * minutes, not seconds, and this tick reaches the vehicle layer rather than a
     * {@code /proc} read.
     */
    private static final long REFRESH_MS = 5_000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    private EnergyTelemetryReader reader;

    private TextView socValue;
    private TextView socCaption;
    private TextView rangeValue;
    private TextView rangeCaption;
    private TextView chargingValue;
    private TextView chargingCaption;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        socValue = view.findViewById(R.id.tv_soc_value);
        socCaption = view.findViewById(R.id.tv_soc_caption);
        rangeValue = view.findViewById(R.id.tv_range_value);
        rangeCaption = view.findViewById(R.id.tv_range_caption);
        chargingValue = view.findViewById(R.id.tv_charging_value);
        chargingCaption = view.findViewById(R.id.tv_charging_caption);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // The reader outlives no view: a fragment recreated by the pager builds a new one
        // against the application context rather than inheriting a stale reference.
        reader = null;
    }

    private void refresh() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        // The launcher is the home screen: a vehicle layer that throws must cost a value on a
        // card, never the screen the driver returns to.
        try {
            render(readerFor(ctx).read(System.currentTimeMillis()));
        } catch (Throwable t) {
            renderUnavailable(getString(R.string.veh_no_vehicle_data));
        }
    }

    /**
     * Builds the reader on first use.
     *
     * <p>Constructing it initialises EVHardware and binds the vendor service hub, which is why
     * it does not happen in {@code onCreate}: a driver who stays on the home page never asks
     * the car anything.
     */
    private EnergyTelemetryReader readerFor(Context ctx) {
        if (reader == null) {
            reader = new EnergyTelemetryReader(ctx.getApplicationContext());
        }
        return reader;
    }

    private void render(EnergySnapshot snapshot) {
        // Two different silences. Nothing at all readable means the vehicle layer is not
        // answering — a bind that has not landed, or a head unit that is not a car. One
        // missing value among several that answered means this car does not publish it.
        String absent = snapshot.getHasVehicleData()
                ? getString(R.string.veh_unavailable_here)
                : getString(R.string.veh_no_vehicle_data);

        Float soc = snapshot.getSocPercent();
        socValue.setText(soc == null
                ? getString(R.string.veh_unknown)
                : String.format(Locale.getDefault(), "%.0f %%", soc));
        socCaption.setText(soc == null ? absent : getString(R.string.veh_soc_caption));

        Float range = snapshot.getRangeKm();
        rangeValue.setText(range == null
                ? getString(R.string.veh_unknown)
                : String.format(Locale.getDefault(), "%.0f km", range));
        rangeCaption.setText(range == null ? absent : getString(R.string.veh_range_caption));

        renderCharging(snapshot, absent);
    }

    /**
     * The charging card reads two independent signals, and says so.
     *
     * <p>The status is the vendor charging service's; the port flag is a standard AAOS
     * property. Either can answer while the other does not, and "plugged in" is worth showing
     * on its own — it is the difference between a cable the driver forgot and a charge that
     * never started.
     */
    private void renderCharging(EnergySnapshot snapshot, String absent) {
        Integer status = snapshot.getChargingStatus();
        Boolean plugged = snapshot.getChargePortConnected();

        if (status != null) {
            chargingValue.setText(chargingStatusLabel(status));
        } else if (plugged != null) {
            chargingValue.setText(plugged
                    ? getString(R.string.veh_plugged_in)
                    : getString(com.evsuite.hardware.R.string.charging_unplugged));
        } else {
            chargingValue.setText(getString(R.string.veh_unknown));
        }

        if (status == null && plugged == null) {
            chargingCaption.setText(absent);
        } else if (plugged == null) {
            chargingCaption.setText(getString(R.string.veh_charging_caption));
        } else {
            chargingCaption.setText(plugged
                    ? getString(R.string.veh_plugged_in)
                    : getString(R.string.veh_not_plugged_in));
        }
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

    private void renderUnavailable(String reason) {
        String dash = getString(R.string.veh_unknown);
        socValue.setText(dash);
        rangeValue.setText(dash);
        chargingValue.setText(dash);
        socCaption.setText(reason);
        rangeCaption.setText(reason);
        chargingCaption.setText(reason);
    }
}
