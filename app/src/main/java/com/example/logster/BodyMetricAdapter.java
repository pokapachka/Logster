package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BodyMetricAdapter extends RecyclerView.Adapter<BodyMetricAdapter.MetricViewHolder> {
    private List<MainActivity.BodyMetric> bodyMetrics;
    private MainActivity activity;

    public BodyMetricAdapter(List<MainActivity.BodyMetric> bodyMetrics, MainActivity activity) {
        this.bodyMetrics = bodyMetrics;
        this.activity = activity;
        Log.d("BodyMetricAdapter", "Initialized with bodyMetrics: " + bodyMetrics.size());
    }

    @NonNull
    @Override
    public MetricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget, parent, false);
        return new MetricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricViewHolder holder, int position) {
        MainActivity.BodyMetric metric = bodyMetrics.get(position);
        Log.d("BodyMetricAdapter", "Binding metric at position: " + position + ", type: " + metric.type + ", value: " + metric.value);
        String displayValue;
        switch (metric.type.toLowerCase()) {
            case "вес":
                displayValue = metric.value + "кг";
                break;
            case "рост":
                displayValue = metric.value + "см";
                break;
            default:
                displayValue = metric.value;
        }
        holder.workoutCount.setText(displayValue);
        holder.workoutCount.setVisibility(View.VISIBLE);
        holder.workoutDay.setText(activity.formatDate(metric.timestamp));
        holder.workoutDay.setVisibility(View.VISIBLE);
        holder.workoutName.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        int size = bodyMetrics.size();
        Log.d("BodyMetricAdapter", "getItemCount: " + size);
        return size;
    }

    static class MetricViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName;
        TextView workoutDay;
        TextView workoutCount;

        MetricViewHolder(View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workout_name);
            workoutDay = itemView.findViewById(R.id.workout_day);
            workoutCount = itemView.findViewById(R.id.workout_count);
        }
    }
}