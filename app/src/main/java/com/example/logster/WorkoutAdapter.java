package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {
    private List<Workout> workouts;
    private MainActivity activity;

    public WorkoutAdapter(List<Workout> workouts, MainActivity activity) {
        this.workouts = new ArrayList<>(workouts);
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
        Workout workout = workouts.get(position);
        Log.d("WorkoutAdapter", "Binding workout at position: " + position + ", name: " + workout.name + ", id: " + workout.id);

        // Устанавливаем название тренировки
        holder.workoutName.setText(workout.name);
        holder.workoutName.setVisibility(View.VISIBLE);

        // Получаем дни недели для тренировки
        String daysText = activity.getAllWorkoutDays(workout.id, workout.dates);
        holder.workoutDay.setText(daysText);
        holder.workoutDay.setVisibility(View.VISIBLE);
        Log.d("WorkoutAdapter", "Workout: " + workout.name + ", days: " + daysText);

        // Подсчёт только завершённых тренировок
        long totalWorkoutCount = activity.getTotalWorkoutCount(workout.id);
        holder.workoutCount.setText(String.valueOf(totalWorkoutCount));
        holder.workoutCount.setVisibility(View.VISIBLE);
        Log.d("WorkoutAdapter", "Workout: " + workout.name + ", total workout count: " + totalWorkoutCount);

        // Обработчик клика для редактирования тренировки
        holder.itemView.setOnClickListener(v -> {
            activity.editWorkout(workout.name);
            Log.d("WorkoutAdapter", "Clicked workout: " + workout.name);
        });
    }

    @Override
    public int getItemCount() {
        int size = workouts.size();
        Log.d("WorkoutAdapter", "getItemCount: " + size);
        return size;
    }

    public void updateWorkouts(List<Workout> newWorkouts) {
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
            Log.d("WorkoutAdapter", "ViewHolder initialized");
        }
    }

}