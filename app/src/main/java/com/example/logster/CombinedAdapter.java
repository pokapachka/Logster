package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombinedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public List<Object> items;
    private List<Folder> folders;
    private List<Workout> workouts;
    private List<BodyMetric> bodyMetrics;
    private MainActivity activity;
    private String currentFolderName;

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_WORKOUT = 1;
    private static final int TYPE_METRIC = 2;

    public CombinedAdapter(MainActivity activity) {
        this.activity = activity;
        this.items = new ArrayList<>();
        this.folders = new ArrayList<>();
        this.workouts = new ArrayList<>();
        this.bodyMetrics = new ArrayList<>();
        this.currentFolderName = null;
        Log.d("CombinedAdapter", "Инициализирован с пустыми данными");
    }

    public void updateData(List<Folder> folders, List<Workout> workouts, List<BodyMetric> bodyMetrics, String currentFolderName) {
        this.folders = new ArrayList<>(folders);
        this.workouts = new ArrayList<>(workouts);
        this.bodyMetrics = new ArrayList<>(bodyMetrics);
        this.currentFolderName = currentFolderName;
        this.items.clear();

        if (currentFolderName != null) {
            // Внутри папки: отображаем только элементы, принадлежащие папке
            Folder folder = folders.stream()
                    .filter(f -> f.name.equals(currentFolderName))
                    .findFirst()
                    .orElse(null);
            if (folder != null) {
                for (String itemId : folder.itemIds) {
                    Workout workout = workouts.stream()
                            .filter(w -> w.id.equals(itemId))
                            .findFirst()
                            .orElse(null);
                    if (workout != null) {
                        items.add(workout);
                        Log.d("CombinedAdapter", "Добавлена тренировка в папку: " + workout.name + ", id=" + itemId);
                        continue;
                    }
                    BodyMetric metric = bodyMetrics.stream()
                            .filter(m -> m.id.equals(itemId))
                            .findFirst()
                            .orElse(null);
                    if (metric != null) {
                        items.add(metric);
                        Log.d("CombinedAdapter", "Добавлена метрика в папку: " + metric.type + ", id=" + itemId);
                    } else {
                        Log.w("CombinedAdapter", "Элемент не найден для id: " + itemId + " в папке: " + currentFolderName);
                    }
                }
            } else {
                Log.w("CombinedAdapter", "Папка не найдена: " + currentFolderName);
            }
        } else {
            // Главный экран: отображаем папки и элементы вне папок
            List<String> folderItemIds = folders.stream()
                    .flatMap(f -> f.itemIds.stream())
                    .collect(Collectors.toList());
            items.addAll(folders);
            for (Workout workout : workouts) {
                if (!folderItemIds.contains(workout.id)) {
                    items.add(workout);
                }
            }
            for (BodyMetric metric : bodyMetrics) {
                if (!folderItemIds.contains(metric.id)) {
                    items.add(metric);
                }
            }
            Log.d("CombinedAdapter", "Элементы главного экрана: " + items.size());
        }
        notifyDataSetChanged();
        Log.d("CombinedAdapter", "Данные обновлены: папка=" + currentFolderName + ", элементов=" + items.size() +
                ", папок=" + folders.size() + ", тренировок=" + workouts.size() + ", метрик=" + bodyMetrics.size());
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof Folder) return TYPE_FOLDER;
        if (item instanceof Workout) return TYPE_WORKOUT;
        return TYPE_METRIC;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case TYPE_FOLDER:
                view = inflater.inflate(R.layout.folder, parent, false);
                return new FolderViewHolder(view);
            case TYPE_WORKOUT:
                view = inflater.inflate(R.layout.widget, parent, false);
                return new WorkoutViewHolder(view);
            default:
                view = inflater.inflate(R.layout.widget_body, parent, false);
                return new MetricViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof FolderViewHolder) {
            Folder folder = (Folder) item;
            FolderViewHolder folderHolder = (FolderViewHolder) holder;
            folderHolder.folderName.setText(folder.name);
            folderHolder.itemsCount.setText(getItemsText(folder.getItemCount()));
            folderHolder.itemView.setOnClickListener(v -> activity.openFolder(folder));
            Log.d("CombinedAdapter", "Привязана папка: " + folder.name + ", элементов=" + folder.getItemCount());
        } else if (holder instanceof WorkoutViewHolder) {
            Workout workout = (Workout) item;
            WorkoutViewHolder workoutHolder = (WorkoutViewHolder) holder;
            workoutHolder.workoutName.setText(workout.name);
            workoutHolder.workoutDay.setText(activity.getNearestDay(workout.id, workout.dates));
            // Подсчёт тренировок на текущей неделе
            LocalDate today = LocalDate.now();
            LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            long weekWorkoutCount = workout.dates.stream()
                    .filter(date -> {
                        try {
                            LocalDate localDate = LocalDate.parse(date);
                            return !localDate.isBefore(today) && !localDate.isAfter(endOfWeek);
                        } catch (Exception e) {
                            Log.e("CombinedAdapter", "Ошибка разбора даты: " + date, e);
                            return false;
                        }
                    })
                    .count();
            workoutHolder.workoutCount.setText(String.valueOf(weekWorkoutCount));
            workoutHolder.itemView.setOnClickListener(v -> activity.editWorkout(workout.name));
            Log.d("CombinedAdapter", "Привязана тренировка: " + workout.name + ", тренировок на неделе=" + weekWorkoutCount);
        } else if (holder instanceof MetricViewHolder) {
            BodyMetric metric = (BodyMetric) item;
            MetricViewHolder metricHolder = (MetricViewHolder) holder;
            String displayValue = metric.type.toLowerCase().equals("вес") ? metric.value + " кг" :
                    metric.type.toLowerCase().equals("рост") ? metric.value + " см" : metric.value;
            metricHolder.metricValue.setText(displayValue);
            metricHolder.timestamp.setText(activity.formatDate(metric.timestamp));
            Log.d("CombinedAdapter", "Привязана метрика: " + metric.type + ", значение=" + displayValue);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getItemsText(int count) {
        if (count == 0) return "0 элементов";
        if (count == 1) return "1 элемент";
        if (count >= 2 && count <= 4) return count + " элемента";
        return count + " элементов";
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = new ArrayList<>(workouts);
        Log.d("CombinedAdapter", "Обновлены тренировки: " + workouts.size());
    }

    public void setBodyMetrics(List<BodyMetric> bodyMetrics) {
        this.bodyMetrics = new ArrayList<>(bodyMetrics);
        Log.d("CombinedAdapter", "Обновлены метрики: " + bodyMetrics.size());
    }

    public int getFolderPosition(String folderName) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Folder && ((Folder) items.get(i)).name.equals(folderName)) {
                return i;
            }
        }
        Log.w("CombinedAdapter", "Позиция папки не найдена для: " + folderName);
        return -1;
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        TextView itemsCount;

        FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folder_name);
            itemsCount = itemView.findViewById(R.id.items);
            if (folderName == null || itemsCount == null) {
                Log.e("CombinedAdapter", "Ошибка инициализации FolderViewHolder: folderName=" +
                        (folderName != null) + ", itemsCount=" + (itemsCount != null));
            }
        }
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName;
        TextView workoutDay;
        TextView workoutCount;

        WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workout_name);
            workoutDay = itemView.findViewById(R.id.workout_day);
            workoutCount = itemView.findViewById(R.id.workout_count);
            if (workoutName == null || workoutDay == null || workoutCount == null) {
                Log.e("CombinedAdapter", "Ошибка инициализации WorkoutViewHolder: workoutName=" +
                        (workoutName != null) + ", workoutDay=" + (workoutDay != null) +
                        ", workoutCount=" + (workoutCount != null));
            }
        }
    }

    static class MetricViewHolder extends RecyclerView.ViewHolder {
        TextView metricValue;
        TextView timestamp;

        MetricViewHolder(@NonNull View itemView) {
            super(itemView);
            metricValue = itemView.findViewById(R.id.count_body_metrics);
            timestamp = itemView.findViewById(R.id.notes);
            if (metricValue == null || timestamp == null) {
                Log.e("CombinedAdapter", "Ошибка инициализации MetricViewHolder: metricValue=" +
                        (metricValue != null) + ", timestamp=" + (timestamp != null));
            }
        }
    }
}