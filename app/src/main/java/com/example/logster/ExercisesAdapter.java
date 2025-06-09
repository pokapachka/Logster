package com.example.logster;

import android.content.Context;
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

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
    private final List<Exercise> exercises;
    private final ExerciseSelectionListener listener;
    private final Context context;

    public static class Exercise {
        private final int id;
        private final String name;
        private final List<String> tags;
        private boolean isSelected;
        private List<Set> sets;

        public Exercise(int id, String name, List<String> tags) {
            this.id = id;
            this.name = name;
            this.tags = new ArrayList<>(tags);
            this.isSelected = false;
            this.sets = new ArrayList<>();
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<String> getTags() {
            return tags;
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

        public int getSetCount() {
            return sets.size();
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Exercise exercise = (Exercise) o;
            return id == exercise.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    public ExercisesAdapter(List<Exercise> exercises, ExerciseSelectionListener listener, Context context) {
        this.exercises = exercises;
        this.listener = listener;
        this.context = context;
        Log.d("ExercisesAdapter", "Initialized with " + exercises.size() + " exercises");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        Log.d("ExercisesAdapter", "Created ViewHolder for item_exercise");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.exerciseName.setText(exercise.getName());
        holder.checkbox.setSelected(exercise.isSelected());
        Log.d("ExercisesAdapter", "Bound exercise: " + exercise.getName() + ", selected: " + exercise.isSelected());

        holder.checkbox.setOnClickListener(v -> {
            Log.d("ExercisesAdapter", "Checkbox clicked for: " + exercise.getName());
            if (listener != null) {
                listener.onExerciseSelected(v, exercise);
            }
        });

        holder.addButton.setOnClickListener(v -> Log.d("ExercisesAdapter", "Add button clicked for " + exercise.getName()));
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        ImageView checkbox;
        ImageView addButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            checkbox = itemView.findViewById(R.id.checkbox);
            addButton = itemView.findViewById(R.id.add_button);
            Log.d("ViewHolder", "Initialized ViewHolder: exerciseName=" + (exerciseName != null) + ", checkbox=" + (checkbox != null) + ", addButton=" + (addButton != null));
        }
    }
}