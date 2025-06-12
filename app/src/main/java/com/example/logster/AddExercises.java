package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class AddExercises implements ExercisesAdapter.OnExerciseClickListener {
    private List<ExercisesAdapter.Exercise> exercises;
    private ExercisesAdapter adapter;
    private TextView emptyView;
    private TextView addExercisesBtn;
    private MainActivity mainActivity;
    private int currentButtonColor; // Текущий цвет кнопки
    private boolean wasEnabled; // Предыдущее состояние enabled
    private static final int COLOR_UNSELECTED = 0xFF606062;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;

    public AddExercises() {
        this.exercises = new ArrayList<>();
        this.adapter = null;
        this.emptyView = null;
        this.addExercisesBtn = null;
        this.mainActivity = null;
        this.currentButtonColor = COLOR_UNSELECTED; // По умолчанию цвет неактивной кнопки
        this.wasEnabled = false; // Изначально кнопка неактивна
    }

    public void initialize(MainActivity activity, List<ExercisesAdapter.Exercise> exercises, ExercisesAdapter adapter, TextView emptyView, TextView addExercisesBtn) {
        this.mainActivity = activity;
        this.exercises = new ArrayList<>(exercises);
        this.adapter = adapter;
        this.emptyView = emptyView;
        this.addExercisesBtn = addExercisesBtn;
        Log.d("AddExercises", "Инициализировано с " + exercises.size() + " упражнениями");

        // Синхронизация состояния выбора
        List<ExercisesAdapter.Exercise> selectedExercises = mainActivity.getSelectedExercises();
        if (selectedExercises != null) {
            for (ExercisesAdapter.Exercise exercise : this.exercises) {
                exercise.setSelected(selectedExercises.stream()
                        .anyMatch(selected -> selected.getId() == exercise.getId()));
            }
            // Установка начального цвета и состояния кнопки
            boolean hasSelection = !selectedExercises.isEmpty();
            currentButtonColor = hasSelection ? COLOR_SELECTED : COLOR_UNSELECTED;
            wasEnabled = hasSelection;
            if (addExercisesBtn != null) {
                addExercisesBtn.setEnabled(hasSelection);
                addExercisesBtn.getBackground().setTint(currentButtonColor);
                Log.d("AddExercises", "Начальный цвет кнопки установлен: " + Integer.toHexString(currentButtonColor) +
                        ", enabled=" + hasSelection);
            }
            updateAddButtonState();
            Log.d("AddExercises", "Отмечено " + selectedExercises.size() + " выбранных упражнений");
        } else {
            Log.w("AddExercises", "Список выбранных упражнений null");
            updateAddButtonState();
        }
    }

    @Override
    public void onExerciseClick(View view, ExercisesAdapter.Exercise exercise) {
        if (view.getId() == R.id.checkbox || view.getId() == R.id.add_button) {
            exercise.setSelected(!exercise.isSelected());
            List<ExercisesAdapter.Exercise> selectedExercises = mainActivity.getSelectedExercises();
            if (selectedExercises != null) {
                if (exercise.isSelected()) {
                    if (!selectedExercises.contains(exercise)) {
                        selectedExercises.add(exercise);
                        Log.d("AddExercises", "Добавлено упражнение: " + exercise.getName() + ", всего выбрано: " + selectedExercises.size());
                    }
                } else {
                    selectedExercises.remove(exercise);
                    Log.d("AddExercises", "Удалено упражнение: " + exercise.getName() + ", всего выбрано: " + selectedExercises.size());
                }
                mainActivity.onExercisesSelected(new ArrayList<>(selectedExercises)); // Копируем список
                updateAddButtonState();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    Log.d("AddExercises", "Адаптер уведомлён, упражнений: " + exercises.size());
                }
                Log.d("AddExercises", "Упражнение " + exercise.getName() + " выбрано: " +
                        exercise.isSelected() + ", всего выбрано: " + selectedExercises.size());
            } else {
                Log.w("AddExercises", "Список выбранных упражнений null");
            }
        } else {
            Log.d("AddExercises", "Игнорирован клик по ID: " + view.getId() + " для упражнения: " + exercise.getName());
        }
    }

    private void updateAddButtonState() {
        if (addExercisesBtn == null) {
            Log.w("AddExercises", "addExercisesBtn is null");
            return;
        }
        List<ExercisesAdapter.Exercise> selectedExercises = mainActivity.getSelectedExercises();
        boolean hasSelection = selectedExercises != null && !selectedExercises.isEmpty();
        boolean enableChanged = hasSelection != wasEnabled; // Проверяем изменение состояния enabled
        addExercisesBtn.setEnabled(hasSelection);

        // Определяем целевой цвет
        int targetColor = hasSelection ? COLOR_SELECTED : COLOR_UNSELECTED;
        Log.d("AddExercises", "Текущий цвет: " + Integer.toHexString(currentButtonColor) +
                ", Целевой цвет: " + Integer.toHexString(targetColor) +
                ", hasSelection: " + hasSelection + ", enableChanged: " + enableChanged);

        // Запускаем анимацию только если изменился цвет и состояние enabled
        if (currentButtonColor != targetColor && enableChanged) {
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), currentButtonColor, targetColor);
            colorAnimator.setDuration(300); // Длительность анимации 300 мс
            colorAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator()); // Плавное замедление
            colorAnimator.addUpdateListener(animator -> {
                int color = (int) animator.getAnimatedValue();
                addExercisesBtn.getBackground().setTint(color);
                Log.d("AddExercises", "Анимация цвета кнопки: " + Integer.toHexString(color));
            });
            colorAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    currentButtonColor = targetColor; // Обновляем текущий цвет после анимации
                    wasEnabled = hasSelection; // Обновляем состояние enabled
                    Log.d("AddExercises", "Анимация завершена, currentButtonColor: " + Integer.toHexString(currentButtonColor) +
                            ", enabled=" + hasSelection);
                }
            });
            colorAnimator.start();
        } else {
            addExercisesBtn.getBackground().setTint(targetColor);
            currentButtonColor = targetColor; // Синхронизируем цвет
            wasEnabled = hasSelection; // Синхронизируем состояние
            Log.d("AddExercises", "Анимация не нужна, цвет установлен: " + Integer.toHexString(targetColor));
        }

        Log.d("AddExercises", "Состояние кнопки обновлено: enabled=" + hasSelection +
                ", выбранных упражнений: " + (selectedExercises != null ? selectedExercises.size() : 0));
    }
}