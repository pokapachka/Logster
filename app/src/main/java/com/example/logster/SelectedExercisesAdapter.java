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
    private OnExerciseRemovedListener onExerciseRemoved;
    private MainActivity mainActivity;

    public interface OnExerciseRemovedListener {
        void onExerciseRemoved(ExercisesAdapter.Exercise exercise);
    }

    public SelectedExercisesAdapter(List<ExercisesAdapter.Exercise> exercises, OnExerciseRemovedListener listener, MainActivity mainActivity) {
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
        this.onExerciseRemoved = listener;
        this.mainActivity = mainActivity;
        Log.d("SelectedExercisesAdapter", "Initialized with exercises: " + this.exercises.size());
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
        if (holder.exerciseName == null) {
            Log.e("SelectedExercisesAdapter", "exerciseName TextView is null at position: " + position);
            return;
        }
        if (holder.removeButton == null) {
            Log.e("SelectedExercisesAdapter", "removeButton ImageView is null at position: " + position);
            return;
        }
        holder.exerciseName.setText(exercise.getName());
        // Удаляем android:onClick из XML, используем setOnClickListener
        holder.itemView.setOnClickListener(v -> mainActivity.editSets(exercise));
        holder.removeButton.setOnClickListener(v -> {
            if (onExerciseRemoved != null) {
                onExerciseRemoved.onExerciseRemoved(exercise);
                Log.d("SelectedExercisesAdapter", "Remove button clicked for exercise: " + exercise.getName());
            }
        });
        // Обновление текста количества подходов
        if (holder.setsText != null) {
            int setCount = exercise.getSets().size();
            holder.setsText.setText(setCount + " " + MainActivity.getSetWordForm(setCount));
        }
        Log.d("SelectedExercisesAdapter", "Bound exercise: " + exercise.getName() + ", position: " + position);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public void updateExercises(List<ExercisesAdapter.Exercise> newExercises) {
        this.exercises.clear();
        this.exercises.addAll(newExercises);
        notifyDataSetChanged();
        Log.d("SelectedExercisesAdapter", "Updated exercises: " + exercises.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        TextView setsText;
        ImageView removeButton;
        ImageView addButton;

        ViewHolder(View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name_selected);
            setsText = itemView.findViewById(R.id.sets);
            removeButton = itemView.findViewById(R.id.remove_button);
            addButton = itemView.findViewById(R.id.add_button_selected);
            if (exerciseName == null) {
                Log.e("SelectedExercisesAdapter", "Failed to find exercise_name_selected TextView in item_selected_exercise");
            }
            if (setsText == null) {
                Log.e("SelectedExercisesAdapter", "Failed to find sets TextView in item_selected_exercise");
            }
            if (removeButton == null) {
                Log.e("SelectedExercisesAdapter", "Failed to find remove_button ImageView in item_selected_exercise");
            }
            if (addButton == null) {
                Log.e("SelectedExercisesAdapter", "Failed to find add_button_selected ImageView in item_selected_exercise");
            }
            Log.d("SelectedExercisesAdapter", "ViewHolder initialized: exerciseName=" + (exerciseName != null) +
                    ", setsText=" + (setsText != null) + ", removeButton=" + (removeButton != null) +
                    ", addButton=" + (addButton != null));
        }
    }
}