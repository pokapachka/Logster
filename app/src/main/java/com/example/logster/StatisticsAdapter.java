package com.example.logster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_WEIGHT = 0;
    public static final int TYPE_HEIGHT = 1;
    private static final int TYPE_EXERCISE = 2;
    private final List<Object> items = new ArrayList<>();
    private final Context context;
    private final OnItemClickListener clickListener;

    // Интерфейс для обработки кликов
    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public StatisticsAdapter(Context context, OnItemClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public void updateData(List<BodyMetric> bodyMetrics, List<CompletedExercise> completedExercises) {
        items.clear();

        // Добавляем элемент для веса, если есть метрики веса
        boolean hasWeight = bodyMetrics.stream().anyMatch(m -> "weight".equals(m.type) || "вес".equals(m.type));
        if (hasWeight) {
            items.add(new MetricItem("Вес", TYPE_WEIGHT));
        }

        // Добавляем элемент для роста, если есть метрики роста
        boolean hasHeight = bodyMetrics.stream().anyMatch(m -> "height".equals(m.type) || "рост".equals(m.type));
        if (hasHeight) {
            items.add(new MetricItem("Рост", TYPE_HEIGHT));
        }

        // Добавляем уникальные упражнения по exerciseId
        if (completedExercises != null) {
            Set<Integer> uniqueExerciseIds = new HashSet<>();
            for (CompletedExercise exercise : completedExercises) {
                if (uniqueExerciseIds.add(exercise.exerciseId)) {
                    String exerciseName = ExerciseList.getAllExercises().stream()
                            .filter(e -> e.getId() == exercise.exerciseId)
                            .map(ExercisesAdapter.Exercise::getName)
                            .findFirst()
                            .orElse("Неизвестное упражнение");
                    items.add(new ExerciseItem(exerciseName, exercise.exerciseId));
                }
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof MetricItem) {
            return ((MetricItem) item).type;
        } else if (item instanceof ExerciseItem) {
            return TYPE_EXERCISE;
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        Object item = items.get(position);
        int viewType = getItemViewType(position);

        // Устанавливаем название элемента и изображение
        if (item instanceof MetricItem) {
            MetricItem metricItem = (MetricItem) item;
            itemHolder.exerciseName.setText(metricItem.name);
            itemHolder.icon.setImageResource(R.drawable.metric); // Устанавливаем metric.png для веса и роста
        } else if (item instanceof ExerciseItem) {
            ExerciseItem exerciseItem = (ExerciseItem) item;
            itemHolder.exerciseName.setText(exerciseItem.name);
            itemHolder.icon.setImageResource(R.drawable.workout_statistic); // Устанавливаем workout_statistic.png для упражнений
        }

        // Показываем иконку (всегда видимая)
        itemHolder.icon.setVisibility(View.VISIBLE);
        itemHolder.checkbox.setVisibility(View.GONE);

        // Скрываем кнопку добавления
        itemHolder.addButton.setVisibility(View.GONE);

        // Устанавливаем обработчик кликов
        itemHolder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item); // Вызываем обратный вызов для обработки выбора
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        ImageView icon;
        ImageView addButton;
        ImageView checkbox;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            icon = itemView.findViewById(R.id.icon);
            addButton = itemView.findViewById(R.id.add_button);
            checkbox = itemView.findViewById(R.id.checkbox);
        }
    }

    // Класс для представления метрик (вес, рост)
    public static class MetricItem {
        public final String name;
        public final int type;

        public MetricItem(String name, int type) {
            this.name = name;
            this.type = type;
        }
    }

    public static class ExerciseItem {
        public final String name;
        public final int exerciseId;

        public ExerciseItem(String name, int exerciseId) {
            this.name = name;
            this.exerciseId = exerciseId;
        }
    }
}