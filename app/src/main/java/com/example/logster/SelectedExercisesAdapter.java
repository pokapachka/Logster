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

public class SelectedExercisesAdapter extends RecyclerView.Adapter<SelectedExercisesAdapter.ViewHolder> {
    private List<ExercisesAdapter.Exercise> exercises;
    private OnExerciseRemovedListener onExerciseRemovedListener;
    private MainActivity mainActivity;

    public interface OnExerciseRemovedListener {
        void onExerciseRemoved(ExercisesAdapter.Exercise exercise);
    }

    public SelectedExercisesAdapter(MainActivity mainActivity, OnExerciseRemovedListener listener) {
        this.mainActivity = mainActivity;
        this.onExerciseRemovedListener = listener;
        this.exercises = new ArrayList<>();
        Log.d("SelectedExercisesAdapter", "Initialized");
    }

    public void updateExercises(List<ExercisesAdapter.Exercise> newExercises) {
        this.exercises = new ArrayList<>(newExercises != null ? newExercises : new ArrayList<>());
        notifyDataSetChanged();
        Log.d("SelectedExercisesAdapter", "Updated exercises: " + exercises.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExercisesAdapter.Exercise exercise = exercises.get(position);
        if (holder.exerciseName != null) {
            holder.exerciseName.setText(exercise.getName());
        } else {
            Log.e("SelectedExercisesAdapter", "exerciseName is null at position: " + position);
        }
        if (holder.setsText != null) {
            int setCount = exercise.getSets().size();
            holder.setsText.setText(setCount + " " + MainActivity.getSetWordForm(setCount));
            Log.d("SelectedExercisesAdapter", "Exercise: " + exercise.getName() + ", sets: " + setCount);
        } else {
            Log.e("SelectedExercisesAdapter", "setsText is null at position: " + position);
        }
        if (holder.removeButton != null) {
            holder.removeButton.setOnClickListener(v -> {
                if (onExerciseRemovedListener != null) {
                    onExerciseRemovedListener.onExerciseRemoved(exercise);
                    Log.d("SelectedExercisesAdapter", "Removed exercise: " + exercise.getName());
                }
            });
        } else {
            Log.e("SelectedExercisesAdapter", "removeButton is null at position: " + position);
        }
        if (holder.addButton != null) {
            holder.addButton.setOnClickListener(v -> mainActivity.editSets(exercise));
        } else {
            Log.e("SelectedExercisesAdapter", "addButton is null at position: " + position);
        }
        holder.itemView.setOnClickListener(v -> mainActivity.editSets(exercise));
        Log.d("SelectedExercisesAdapter", "Bound exercise: " + exercise.getName() + ", position: " + position);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        TextView setsText;
        ImageView removeButton;
        ImageView addButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name_selected);
            setsText = itemView.findViewById(R.id.sets);
            removeButton = itemView.findViewById(R.id.remove_button);
            addButton = itemView.findViewById(R.id.add_button_selected);
            if (exerciseName == null || setsText == null || removeButton == null || addButton == null) {
                Log.e("SelectedExercisesAdapter", "ViewHolder initialization failed: exerciseName=" +
                        (exerciseName != null) + ", setsText=" + (setsText != null) +
                        ", removeButton=" + (removeButton != null) + ", addButton=" + (addButton != null));
            }
        }
    }
}