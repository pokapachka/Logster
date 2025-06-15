package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetDragHelper extends ItemTouchHelper.SimpleCallback {
    private final SelectedSetsAdapter adapter;
    private final List<Set> sets;
    private final MainActivity mainActivity;
    private final ExercisesAdapter.Exercise currentExercise;
    private final Context context;

    public SetDragHelper(SelectedSetsAdapter adapter, List<Set> sets, MainActivity mainActivity,
                         ExercisesAdapter.Exercise currentExercise) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0); // Только перетаскивание вверх/вниз
        this.adapter = adapter;
        this.sets = sets;
        this.mainActivity = mainActivity;
        this.currentExercise = currentExercise;
        this.context = mainActivity.getApplicationContext();
        Log.d("SetDragHelper", "Инициализирован для упражнения: " + currentExercise.getName());
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        int fromPos = viewHolder.getAdapterPosition();
        int toPos = target.getAdapterPosition();

        if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION ||
                fromPos >= sets.size() || toPos >= sets.size()) {
            Log.e("SetDragHelper", "Невалидные позиции: from=" + fromPos + ", to=" + toPos);
            return false;
        }

        Collections.swap(sets, fromPos, toPos);
        adapter.notifyItemMoved(fromPos, toPos);
        Log.d("SetDragHelper", "Перемещён подход: from=" + fromPos + ", to=" + toPos);
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
            Log.w("SetDragHelper", "onSelectedChanged: viewHolder null, actionState=" + actionState);
            return;
        }

        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) {
            Log.w("SetDragHelper", "onSelectedChanged: pos=-1, actionState=" + actionState);
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Устанавливаем ex_background с начальной прозрачностью 0
            Drawable dragBackground = ContextCompat.getDrawable(context, R.drawable.ex_background);
            if (dragBackground != null) {
                dragBackground = dragBackground.mutate();
                dragBackground.setAlpha(0);
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
                Log.w("SetDragHelper", "ex_background не найден");
            }
            // Анимация масштабирования
            viewHolder.itemView.animate()
                    .scaleX(1.03f)
                    .scaleY(1.03f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            viewHolder.itemView.setElevation(4f);
            // Анимация подсветки границы
            animateStroke(viewHolder, "#333339", "#727275");
            Log.d("SetDragHelper", "Перетаскивание начато: pos=" + pos + ", установлен ex_background с анимацией");
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
                        viewHolder.itemView.setBackground(null);
                    }
                });
                fadeOut.start();
            } else {
                viewHolder.itemView.setBackground(null);
            }
            // Анимация возврата масштаба
            viewHolder.itemView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start(); // Исправлено: заменено .ONU на .start()
            viewHolder.itemView.setElevation(0f);
            // Сброс подсветки границы
            animateStroke(viewHolder, "#727275", "#333339");
            Log.d("SetDragHelper", "Перетаскивание завершено: pos=" + pos + ", ex_background удалён с анимацией");
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION || pos >= sets.size()) {
            Log.w("SetDragHelper", "clearView: невалидная позиция pos=" + pos);
            return;
        }

        // Убедимся, что фон удалён
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
        // Сброс подсветки границы
        animateStroke(viewHolder, "#727275", "#333339");

        // Сохранение нового порядка подходов
        saveSetOrder();
        Log.d("SetDragHelper", "clearView завершен: pos=" + pos);
    }

    private void saveSetOrder() {
        String workoutName = mainActivity.getSelectedWorkoutName();
        if (workoutName == null || workoutName.isEmpty()) {
            Log.w("SetDragHelper", "selectedWorkoutName is null or empty, cannot save set order for exercise: " +
                    currentExercise.getName());
            return;
        }

        Workout workout = mainActivity.workouts.stream()
                .filter(w -> w.name.equals(workoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(currentExercise.getId(), k -> new ArrayList<>());
            workoutSets.clear();
            workoutSets.addAll(sets);
            currentExercise.getSets().clear();
            currentExercise.getSets().addAll(sets);
            mainActivity.saveWorkouts();
            Log.d("SetDragHelper", "Сохранён порядок подходов для упражнения: " + currentExercise.getName() +
                    ", setCount=" + sets.size());
        } else {
            Log.w("SetDragHelper", "Тренировка не найдена: " + workoutName);
        }
    }

    private void animateStroke(RecyclerView.ViewHolder holder, String fromColor, String toColor) {
        if (holder == null) {
            Log.w("SetDragHelper", "animateStroke: holder=null");
            return;
        }
        Drawable background = holder.itemView.getBackground();
        GradientDrawable drawable = getGradientDrawable(background);
        if (drawable != null) {
            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    Color.parseColor(fromColor), Color.parseColor(toColor));
            animator.setDuration(300);
            animator.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                drawable.setStroke(2, color);
            });
            animator.start();
            Log.d("SetDragHelper", "Подсветка границы: pos=" + holder.getAdapterPosition() + ", from=" + fromColor);
        } else {
            Log.w("SetDragHelper", "GradientDrawable не найден");
        }
    }

    private GradientDrawable getGradientDrawable(Drawable drawable) {
        if (drawable instanceof GradientDrawable) {
            return (GradientDrawable) drawable;
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            if (layerDrawable.getNumberOfLayers() > 0 && layerDrawable.getDrawable(0) instanceof GradientDrawable) {
                return (GradientDrawable) layerDrawable.getDrawable(0);
            }
        }
        Log.w("SetDragHelper", "GradientDrawable не найден: " + (drawable != null ? drawable.getClass().getSimpleName() : "null"));
        return null;
    }
}