package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BodyMetricAdapter extends RecyclerView.Adapter<BodyMetricAdapter.MetricViewHolder> {
    private List<BodyMetric> metrics;
    private MainActivity activity;

    public BodyMetricAdapter(List<BodyMetric> metrics, MainActivity activity) {
        this.metrics = new ArrayList<>(metrics);
        this.activity = activity;
        Log.d("BodyMetricAdapter", "Initialized with metrics: " + metrics.size());
    }

    @NonNull
    @Override
    public MetricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_body, parent, false);
        return new MetricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricViewHolder holder, int position) {
        BodyMetric metric = metrics.get(position);
        Log.d("BodyMetricAdapter", "Binding metric at position: " + position + ", type: " + metric.type);
        String displayValue = metric.type.toLowerCase().equals("вес") ? metric.value + " кг" :
                metric.type.toLowerCase().equals("рост") ? metric.value + " см" : metric.value;
        holder.countBodyMetrics.setText(displayValue);
        holder.notes.setText(activity.formatDate(metric.timestamp));
    }

    @Override
    public int getItemCount() {
        int size = metrics.size();
        Log.d("BodyMetricAdapter", "getItemCount: " + size);
        return size;
    }

    public void updateMetrics(List<BodyMetric> newMetrics) {
        this.metrics.clear();
        this.metrics.addAll(newMetrics);
        notifyDataSetChanged();
        Log.d("BodyMetricAdapter", "Updated metrics: " + metrics.size());
    }

    static class MetricViewHolder extends RecyclerView.ViewHolder {
        TextView countBodyMetrics;
        TextView notes;

        MetricViewHolder(View itemView) {
            super(itemView);
            countBodyMetrics = itemView.findViewById(R.id.count_body_metrics);
            notes = itemView.findViewById(R.id.notes);
        }
    }
}