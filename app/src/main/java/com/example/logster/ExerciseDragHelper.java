package com.example.logster;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class ExerciseDragHelper extends ItemTouchHelper.SimpleCallback {
    private final SelectedExercisesAdapter adapter;
    private final List<ExercisesAdapter.Exercise> exercises;
    private final MainActivity mainActivity;
    private final String workoutName;
    private final Context context;

    public ExerciseDragHelper(SelectedExercisesAdapter adapter, List<ExercisesAdapter.Exercise> exercises,
                              MainActivity mainActivity, String workoutName) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0); // Только перетаскивание вверх/вниз, без свайпов
        this.adapter = adapter;
        this.exercises = exercises;
        this.mainActivity = mainActivity;
        this.workoutName = workoutName;
        this.context = mainActivity.getApplicationContext();
        Log.d("ExerciseDragHelper", "Инициализирован для тренировки: " + workoutName);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION ||
                fromPos >= exercises.size() || toPos >= exercises.size()) {
            Log.e("ExerciseDragHelper", "Невалидные позиции: from=" + fromPos + ", to=" + toPos);
            return false;
        }

        // Перемещаем упражнение в списке
        Collections.swap(exercises, fromPos, toPos);
        adapter.notifyItemMoved(fromPos, toPos);
        Log.d("ExerciseDragHelper", "Перемещено упражнение: from=" + fromPos + ", to=" + toPos);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Свайпы отключены
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        if (viewHolder == null) {
            Log.w("ExerciseDragHelper", "onSelectedChanged: viewHolder null, actionState=" + actionState);
            return;
        }

        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) {
            Log.w("ExerciseDragHelper", "onSelectedChanged: pos=-1, actionState=" + actionState);
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Устанавливаем ex_background с начальной прозрачностью 0
            Drawable dragBackground = ContextCompat.getDrawable(context, R.drawable.ex_background);
            if (dragBackground != null) {
                dragBackground = dragBackground.mutate(); // Создаём копию для независимой анимации
                dragBackground.setAlpha(0); // Начальная прозрачность
                viewHolder.itemView.setBackground(dragBackground);
                // Анимация появления фона
                ValueAnimator fadeIn = ValueAnimator.ofInt(0, 255);
                fadeIn.setDuration(300);
                fadeIn.setInterpolator(new DecelerateInterpolator());
                fadeIn.addUpdateListener(animator -> {
                    int alpha = (int) animator.getAnimatedValue();
                    Drawable background = viewHolder.itemView.getBackground();
                    if (background != null) {
                        background.setAlpha(alpha);
                    }
                });
                fadeIn.start();
            } else {
                Log.w("ExerciseDragHelper", "ex_background не найден");
            }
            // Анимация масштабирования
            viewHolder.itemView.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            viewHolder.itemView.setElevation(4f);
            Log.d("ExerciseDragHelper", "Перетаскивание начато: pos=" + pos + ", установлен ex_background с анимацией");
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            // Анимация исчезновения фона
            Drawable background = viewHolder.itemView.getBackground();
            if (background != null) {
                ValueAnimator fadeOut = ValueAnimator.ofInt(255, 0);
                fadeOut.setDuration(300);
                fadeOut.setInterpolator(new DecelerateInterpolator());
                fadeOut.addUpdateListener(animator -> {
                    int alpha = (int) animator.getAnimatedValue();
                    Drawable bg = viewHolder.itemView.getBackground();
                    if (bg != null) {
                        bg.setAlpha(alpha);
                    }
                });
                fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        viewHolder.itemView.setBackground(null); // Устанавливаем null после анимации
                    }
                });
                fadeOut.start();
            } else {
                viewHolder.itemView.setBackground(null); // На случай, если фона нет
            }
            // Анимация возврата масштаба
            viewHolder.itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            viewHolder.itemView.setElevation(0f);
            Log.d("ExerciseDragHelper", "Перетаскивание завершено: pos=" + pos + ", ex_background удалён с анимацией");
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || pos >= exercises.size()) {
            Log.w("ExerciseDragHelper", "clearView: невалидная позиция pos=" + pos);
            return;
        }

        // Убедимся, что фон удалён (на случай, если onSelectedChanged не сработал)
        Drawable background = viewHolder.itemView.getBackground();
        if (background != null) {
            ValueAnimator fadeOut = ValueAnimator.ofInt(background.getAlpha(), 0);
            fadeOut.setDuration(300);
            fadeOut.setInterpolator(new DecelerateInterpolator());
            fadeOut.addUpdateListener(animator -> {
                int alpha = (int) animator.getAnimatedValue();
                Drawable bg = viewHolder.itemView.getBackground();
                if (bg != null) {
                    bg.setAlpha(alpha);
                }
            });
            fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    viewHolder.itemView.setBackground(null);
                }
            });
            fadeOut.start();
        } else {
            viewHolder.itemView.setBackground(null);
        }

        // Сброс анимации
        viewHolder.itemView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        viewHolder.itemView.setElevation(0f);

        // Сохранение нового порядка упражнений
        saveExerciseOrder();
        Log.d("ExerciseDragHelper", "clearView завершен: pos=" + pos);
    }

    private void saveExerciseOrder() {
        Workout workout = mainActivity.workouts.stream()
                .filter(w -> w.name.equals(workoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            workout.exerciseIds.clear();
            for (ExercisesAdapter.Exercise exercise : exercises) {
                workout.exerciseIds.add(exercise.getId());
            }
            mainActivity.saveWorkoutData();
            Log.d("ExerciseDragHelper", "Сохранён порядок упражнений для тренировки: " + workoutName +
                    ", exerciseIds=" + workout.exerciseIds.size());
        } else {
            Log.w("ExerciseDragHelper", "Тренировка не найдена: " + workoutName);
        }
    }
}