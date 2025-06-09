package com.example.logster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SelectedExercisesAdapter extends RecyclerView.Adapter<SelectedExercisesAdapter.ViewHolder> {

    private List<ExercisesAdapter.Exercise> exercises;
    private OnExerciseRemovedListener listener;
    private Context context;

    public interface OnExerciseRemovedListener {
        void onExerciseRemoved(ExercisesAdapter.Exercise exercise);
    }

    public SelectedExercisesAdapter(List<ExercisesAdapter.Exercise> exercises, OnExerciseRemovedListener listener, Context context) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        this.listener = listener;
        this.context = context;
    }

    public void updateExercises(List<ExercisesAdapter.Exercise> newExercises) {
        this.exercises.clear();
        if (newExercises != null) {
            this.exercises.addAll(newExercises);
        }
        notifyDataSetChanged();
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
        holder.exerciseName.setText(exercise.getName());
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).editSets(exercise);
            }
        });
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExerciseRemoved(exercise);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        View removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            removeButton = itemView.findViewById(R.id.remove_button);
        }
    }
}