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

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ExerciseViewHolder> {
    private List<Exercise> exercises;
    private final OnExerciseClickListener listener;
    private final MainActivity mainActivity;

    public interface OnExerciseClickListener {
        void onExerciseClick(View view, Exercise exercise);
    }

    public ExercisesAdapter(List<Exercise> exercises, OnExerciseClickListener listener, MainActivity mainActivity) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        this.listener = listener;
        this.mainActivity = mainActivity;
        Log.d("ExercisesAdapter", "Initialized with " + this.exercises.size() + " exercises");
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.exerciseName.setText(exercise.getName());
        holder.checkbox.setSelected(exercise.isSelected());

        // Обработка клика по чекбоксу (выбор/снятие выбора упражнения)
        holder.checkbox.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExerciseClick(v, exercise);
                Log.d("ExercisesAdapter", "Checkbox clicked for exercise: " + exercise.getName());
            }
        });

        // Обработка клика по всему элементу (открытие описания)
        holder.itemView.setOnClickListener(v -> {
            if (v.getId() != R.id.checkbox && v.getId() != R.id.add_button) {
                mainActivity.openExerciseDescription(exercise);
                Log.d("ExercisesAdapter", "ItemView clicked, opening description for: " + exercise.getName());
            }
        });

        // Обработка клика по кнопке добавления
        holder.addButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExerciseClick(v, exercise);
                Log.d("ExercisesAdapter", "Add button clicked for exercise: " + exercise.getName());
            }
        });

        Log.d("ExercisesAdapter", "Bound exercise: " + exercise.getName() + ", selected: " + exercise.isSelected());
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public void updateExercises(List<Exercise> newExercises) {
        this.exercises = new ArrayList<>(newExercises);
        notifyDataSetChanged();
        Log.d("ExercisesAdapter", "Exercises updated, new size: " + exercises.size());
    }

    public static class Exercise {
        private final int id;
        private final String name;
        private final List<String> tags;
        private final String description;
        private boolean selected;
        private final List<Set> sets;

        public Exercise(int id, String name, List<String> tags, String description) {
            this.id = id;
            this.name = name;
            this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
            this.description = description;
            this.selected = false;
            this.sets = new ArrayList<>();
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<String> getTags() {
            return new ArrayList<>(tags);
        }

        public String getDescription() {
            return description;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public List<Set> getSets() {
            return sets;
        }

        public void addSet(Set set) {
            sets.add(set);
        }

        public void removeSet(Set set) {
            sets.remove(set);
        }
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        ImageView checkbox;
        ImageView addButton;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            checkbox = itemView.findViewById(R.id.checkbox);
            addButton = itemView.findViewById(R.id.add_button);
        }
    }
}