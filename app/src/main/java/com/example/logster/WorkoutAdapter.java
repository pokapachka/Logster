package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {
    private List<MainActivity.Workout> workouts;
    private MainActivity activity;

    public WorkoutAdapter(List<MainActivity.Workout> workouts, MainActivity activity) {
        this.workouts = workouts;
        this.activity = activity;
        Log.d("WorkoutAdapter", "Initialized with workouts: " + workouts.size());
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        MainActivity.Workout workout = workouts.get(position);
        Log.d("WorkoutAdapter", "Binding workout at position: " + position + ", name: " + workout.name);
        holder.workoutName.setText(workout.name);
        holder.workoutName.setVisibility(View.VISIBLE);
        holder.workoutDay.setText(activity.getNearestDay(workout.id, workout.dates));
        holder.workoutDay.setVisibility(View.VISIBLE);
        holder.workoutCount.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        int size = workouts.size();
        Log.d("WorkoutAdapter", "getItemCount: " + size);
        return size;
    }

    public void updateWorkouts(List<MainActivity.Workout> newWorkouts) {
        this.workouts.clear();
        this.workouts.addAll(newWorkouts);
        notifyDataSetChanged();
        Log.d("WorkoutAdapter", "Updated workouts: " + workouts.size());
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName;
        TextView workoutDay;
        TextView workoutCount;

        WorkoutViewHolder(View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workout_name);
            workoutDay = itemView.findViewById(R.id.workout_day);
            workoutCount = itemView.findViewById(R.id.workout_count);
        }
    }
}