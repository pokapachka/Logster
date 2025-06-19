package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class WorkoutExerciseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<MainActivity.WorkoutProgramItem> items;

    public WorkoutExerciseAdapter(List<MainActivity.WorkoutProgramItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MainActivity.WorkoutProgramItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_day_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_selected_exercise2, parent, false);
            return new ExerciseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MainActivity.WorkoutProgramItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((MainActivity.DayHeader) item);
        } else if (holder instanceof ExerciseViewHolder) {
            ((ExerciseViewHolder) holder).bind((MainActivity.WorkoutExerciseItem) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayNameTextView;
        private final TextView workoutNameTextView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNameTextView = itemView.findViewById(R.id.day_name);
            workoutNameTextView = itemView.findViewById(R.id.workout_name);
        }

        void bind(MainActivity.DayHeader header) {
            dayNameTextView.setText(header.getDayName());
            workoutNameTextView.setText(header.getWorkoutName());
        }
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private final TextView exerciseNameTextView;
        private final TextView setsTextView;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exercise_name_selected2);
            setsTextView = itemView.findViewById(R.id.sets2);
        }

        void bind(MainActivity.WorkoutExerciseItem item) {
            WorkoutProgramGenerator.WorkoutExercise exercise = item.getExercise();
            exerciseNameTextView.setText(exercise.getExercise().getName());
            // Формируем строку только с количеством подходов и повторений
            List<com.example.logster.Set> sets = exercise.getSets();
            if (!sets.isEmpty()) {
                int numSets = sets.size();
                int reps = sets.get(0).getReps(); // Берем повторения из первого подхода
                setsTextView.setText(numSets + " подходов х " + reps + " повторений");
            } else {
                setsTextView.setText("0 подходов");
            }
        }
    }
}