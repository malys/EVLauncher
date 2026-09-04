package com.evsuite.launcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen picker for one metric card, the counterpart of {@link AppDrawerActivity}.
 *
 * <p>It replaces the list dialog the page shipped with first. Forty-odd entries in a dialog is
 * a scrolling column of small rows on a panel the driver is reaching across — the wrong
 * component for the number of choices. Here they are a grid of touch-target-sized cells under
 * two section headers, which is the shape the app picker already uses in this launcher.
 *
 * <p>Like the app picker, it writes the choice itself and finishes; {@code MetricsFragment}
 * rebuilds from preferences in {@code onResume}. No result plumbing, one owner of the write.
 */
public class MetricPickerActivity extends AppCompatActivity {

    /** The card being filled. Equal to the current size when the "add" tile sent us here. */
    public static final String EXTRA_SLOT = "slot";

    /** Cell width band; four to six columns on the MG4's 1920 dp panel. */
    private static final int CELL_WIDTH_DP = 300;

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_METRIC = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metric_picker);

        int slot = getIntent().getIntExtra(EXTRA_SLOT, -1);
        if (slot < 0) {
            finish();
            return;
        }

        findViewById(R.id.picker_back_button).setOnClickListener(v -> finish());

        List<Object> rows = buildRows(slot);
        int span = Math.max(2, getResources().getConfiguration().screenWidthDp / CELL_WIDTH_DP);

        GridLayoutManager layout = new GridLayoutManager(this, span);
        // A section header is a full-width row; only the metrics themselves are cells.
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return rows.get(position) instanceof Metric ? 1 : span;
            }
        });

        RecyclerView grid = findViewById(R.id.metric_grid);
        grid.setLayoutManager(layout);
        grid.setAdapter(new ChoiceAdapter(rows, slot));
    }

    /**
     * The catalogue minus what is already on the page, grouped into its two sections.
     *
     * <p>The metric currently in this slot is kept, so a replace that changes nothing is
     * possible and the driver can see what they are replacing. A section with nothing left in
     * it is dropped rather than shown empty.
     */
    private List<Object> buildRows(int slot) {
        List<String> chosen = new PreferencesManager(this).getMetrics();
        String here = slot < chosen.size() ? chosen.get(slot) : null;

        List<Object> rows = new ArrayList<>();
        for (boolean vehicle : new boolean[]{false, true}) {
            List<Metric> section = new ArrayList<>();
            for (Metric metric : Metric.values()) {
                if (metric.vehicle != vehicle) {
                    continue;
                }
                if (!chosen.contains(metric.name()) || metric.name().equals(here)) {
                    section.add(metric);
                }
            }
            if (!section.isEmpty()) {
                rows.add(getString(vehicle
                        ? R.string.metric_group_vehicle
                        : R.string.metric_group_system));
                rows.addAll(section);
            }
        }
        return rows;
    }

    private class ChoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Object> rows;
        private final int slot;

        ChoiceAdapter(List<Object> rows, int slot) {
            this.rows = rows;
            this.slot = slot;
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof Metric ? TYPE_METRIC : TYPE_SECTION;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    type == TYPE_METRIC
                            ? R.layout.item_metric_choice
                            : R.layout.item_metric_section,
                    parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object row = rows.get(position);
            if (!(row instanceof Metric)) {
                ((TextView) holder.itemView).setText((String) row);
                return;
            }
            Metric metric = (Metric) row;
            TextView label = holder.itemView.findViewById(R.id.choice_label);
            TextView state = holder.itemView.findViewById(R.id.choice_state);
            label.setText(metric.labelRes);
            // Naming what the card would read from is the only warning the picker can give
            // about a value the car may not publish at all.
            state.setText(metric.vehicle
                    ? R.string.metric_source_vehicle
                    : R.string.metric_source_system);
            holder.itemView.setOnClickListener(v -> {
                new PreferencesManager(MetricPickerActivity.this).setMetric(slot, metric.name());
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
