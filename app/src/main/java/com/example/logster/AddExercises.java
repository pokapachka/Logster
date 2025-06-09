package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AddExercises extends Fragment implements ExerciseSelectionListener {
    private RecyclerView recyclerView;
    private ExercisesAdapter adapter;
    private List<ExercisesAdapter.Exercise> exercises;
    private TextView emptyView;
    private TextView addExercisesBtn;
    private static final int COLOR_UNSELECTED = 0xFF606062; // Цвет для неактивной кнопки
    private static final int COLOR_SELECTED = 0xFFFFFFFF;   // Цвет для активной кнопки

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("AddExercises", "onCreateView called");
        View view = inflater.inflate(R.layout.add_exercises, container, false);

        recyclerView = view.findViewById(R.id.exercise_list);
        emptyView = view.findViewById(R.id.empty_view);
        addExercisesBtn = view.findViewById(R.id.add_exercises_btn);
        Log.d("AddExercises", "Views initialized: recyclerView=" + (recyclerView != null) +
                ", emptyView=" + (emptyView != null) + ", addExercisesBtn=" + (addExercisesBtn != null));

        exercises = new ArrayList<>(ExerciseList.getAllExercises());
        Log.d("AddExercises", "Loaded " + exercises.size() + " exercises from ExerciseList");

        // Mark exercises as selected based on MainActivity’s selectedExercises
        if (getActivity() instanceof MainActivity) {
            List<ExercisesAdapter.Exercise> selectedExercises = ((MainActivity) getActivity()).getSelectedExercises();
            if (selectedExercises != null) {
                for (ExercisesAdapter.Exercise exercise : exercises) {
                    exercise.setSelected(selectedExercises.contains(exercise));
                }
                updateAddButtonState(selectedExercises);
                Log.d("AddExercises", "Marked " + selectedExercises.size() + " exercises as selected");
            } else {
                Log.w("AddExercises", "Selected exercises list is null");
            }
        } else {
            Log.e("AddExercises", "Activity is not MainActivity or is null");
        }

        // Initialize adapter
        adapter = new ExercisesAdapter(exercises, this, requireContext());
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(exercises.isEmpty() ? View.GONE : View.VISIBLE);
            if (emptyView != null) {
                emptyView.setVisibility(exercises.isEmpty() ? View.VISIBLE : View.GONE);
            }
            Log.d("AddExercises", "RecyclerView adapter set, item count: " + (adapter != null ? adapter.getItemCount() : 0));
        } else {
            Log.e("AddExercises", "RecyclerView is null");
        }

        // Настройка кнопки закрытия
        View closeButton = view.findViewById(R.id.close);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                Log.d("AddExercises", "Close button clicked");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).backSheetAddExercises(v);
                } else {
                    Log.e("AddExercises", "Activity is not MainActivity or is null");
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        } else {
            Log.w("AddExercises", "Close button not found");
        }

        // Настройка кнопки "Выбрать"
        if (addExercisesBtn != null) {
            addExercisesBtn.setOnClickListener(v -> {
                if (addExercisesBtn.isSelected()) {
                    Log.d("AddExercises", "Select button clicked, returning to edit_workout");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).backSheetAddExercises(v);
                    }
                } else {
                    Log.d("AddExercises", "Select button is disabled");
                }
            });
        } else {
            Log.w("AddExercises", "Add exercises button not found");
        }

        return view;
    }

    @Override
    public void onExerciseSelected(View view, ExercisesAdapter.Exercise exercise) {
        exercise.setSelected(!exercise.isSelected());
        if (getActivity() instanceof MainActivity) {
            List<ExercisesAdapter.Exercise> selectedExercises = ((MainActivity) getActivity()).getSelectedExercises();
            if (selectedExercises != null) {
                if (exercise.isSelected()) {
                    if (!selectedExercises.contains(exercise)) {
                        selectedExercises.add(exercise);
                        Log.d("AddExercises", "Added exercise: " + exercise.getName());
                    }
                } else {
                    selectedExercises.remove(exercise);
                    Log.d("AddExercises", "Removed exercise: " + exercise.getName());
                }
                ((MainActivity) getActivity()).onExercisesSelected(selectedExercises);
                updateAddButtonState(selectedExercises);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    Log.d("AddExercises", "Exercise " + exercise.getName() + " selected: " +
                            exercise.isSelected() + ", total selected: " + selectedExercises.size());
                } else {
                    Log.e("AddExercises", "Adapter is null when notifying data set changed");
                }
            } else {
                Log.w("AddExercises", "Selected exercises list is null");
            }
        } else {
            Log.e("AddExercises", "Activity is not MainActivity or is null");
        }
    }

    private void updateAddButtonState(List<ExercisesAdapter.Exercise> selectedExercises) {
        boolean hasSelection = !selectedExercises.isEmpty();
        addExercisesBtn.setSelected(hasSelection);
        addExercisesBtn.setEnabled(hasSelection);

        // Анимация изменения цвета фона
        int startColor = hasSelection ? COLOR_UNSELECTED : COLOR_SELECTED;
        int endColor = hasSelection ? COLOR_SELECTED : COLOR_UNSELECTED;
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        colorAnimator.setDuration(300);
        colorAnimator.addUpdateListener(animator ->
                addExercisesBtn.getBackground().setTint((int) animator.getAnimatedValue()));
        colorAnimator.start();

        Log.d("AddExercises", "Add button state updated: enabled=" + hasSelection +
                ", selected=" + addExercisesBtn.isSelected() + ", selected exercises: " +
                selectedExercises.size());
    }
}