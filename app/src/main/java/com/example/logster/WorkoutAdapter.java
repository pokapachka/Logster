package com.example.logster;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<MainActivity.Workout> workouts;
    private final MainActivity mainActivity; // Для доступа к getNearestDay
    private ItemTouchHelper itemTouchHelper; // Для управления перетаскиванием
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private final long LONG_PRESS_TIMEOUT = 300; // Время долгого нажатия (200 мс)
    private Runnable longPressRunnable;

    public WorkoutAdapter(List<MainActivity.Workout> workouts, MainActivity mainActivity, ItemTouchHelper itemTouchHelper) {
        this.workouts = workouts != null ? workouts : new ArrayList<>();
        this.mainActivity = mainActivity;
        this.itemTouchHelper = itemTouchHelper;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget, parent, false);
        // Рассчитываем ширину виджета
        int screenWidth = parent.getResources().getDisplayMetrics().widthPixels;
        WorkoutViewHolder holder = new WorkoutViewHolder(view);
        // Настраиваем кастомное долгое нажатие
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Создаём Runnable для долгого нажатия
                    longPressRunnable = () -> {
                        itemTouchHelper.startDrag(holder); // Инициируем перетаскивание
                    };
                    longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Отменяем долгое нажатие, если палец убран
                    longPressHandler.removeCallbacks(longPressRunnable);
                    break;
            }
            return false; // Возвращаем false, чтобы другие слушатели могли обработать событие
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        MainActivity.Workout workout = workouts.get(position);
        holder.nameTextView.setText(workout.name);
        holder.dayTextView.setText("Ближайший день: " + mainActivity.getNearestDay(workout.days));
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    // Перемещение элементов при drag-and-drop
    public void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(workouts, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    // Удаление элемента при свайпе
    public void onItemDismiss(int position) {
        workouts.remove(position);
        notifyItemRemoved(position);
    }

    // Получение списка тренировок (для сохранения в SharedPreferences)
    public List<MainActivity.Workout> getWorkouts() {
        return workouts;
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView dayTextView;

        WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.workout_name);
            dayTextView = itemView.findViewById(R.id.workout_day);
        }
    }

    private int dpToPx(int dp) {
        float density = mainActivity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}