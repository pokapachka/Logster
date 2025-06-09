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
    private final ExerciseSelectionListener listener;
    private final MainActivity activity;

    public ExercisesAdapter(List<Exercise> exercises, ExerciseSelectionListener listener, MainActivity activity) {
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
        this.listener = listener;
        this.activity = activity;
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
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public static class Exercise {
        private final int id;
        private final String name;
        private boolean isSelected;
        private final List<Set> sets;
        private final List<String> muscleGroups; // Новое поле для групп мышц

        public Exercise(int id, String name, List<String> muscleGroups) {
            this.id = id;
            this.name = name;
            this.isSelected = false;
            this.sets = new ArrayList<>();
            this.muscleGroups = muscleGroups != null ? new ArrayList<>(muscleGroups) : new ArrayList<>();
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
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

        public List<String> getMuscleGroups() {
            return new ArrayList<>(muscleGroups);
        }
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        ImageView checkBox;

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            checkBox = itemView.findViewById(R.id.checkbox);
            if (checkBox == null || exerciseName == null) {
                Log.w("ExercisesAdapter", "checkBox or exerciseName not found in item_exercise");
            }
        }

        void bind(Exercise exercise) {
            if (exerciseName != null) {
                exerciseName.setText(exercise.getName());
            }
            if (checkBox != null) {
                checkBox.setSelected(exercise.isSelected());
            }
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExerciseSelected(v, exercise);
                }
            });
            if (checkBox != null) {
                checkBox.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onExerciseSelected(v, exercise);
                    }
                });
            }
        }
    }
}