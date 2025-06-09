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
import java.util.stream.Stream;

public class CombinedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public List<Object> items;
    private List<Folder> folders;
    private List<Workout> workouts;
    private List<Object> mainItems;
    private List<BodyMetric> bodyMetrics;
    private MainActivity activity;

    private static final int TYPE_FOLDER = 0;
    private static final int TYPE_WORKOUT = 1;
    private static final int TYPE_METRIC = 2;

    public CombinedAdapter(List<Folder> folders, List<Workout> workouts, List<BodyMetric> bodyMetrics, MainActivity activity) {
        this.folders = folders;
        this.workouts = workouts;
        this.bodyMetrics = bodyMetrics;
        this.activity = activity;
        this.items = new ArrayList<>();
        this.mainItems = new ArrayList<>();
        Log.d("CombinedAdapter", "Initialized: folders=" + folders.size() + ", workouts=" + workouts.size() + ", bodyMetrics=" + bodyMetrics.size());
    }

    public void updateData(List<Folder> folders, List<Object> mainItems, List<BodyMetric> bodyMetrics, String currentFolderName) {
        this.items.clear();
        this.folders = folders;
        this.mainItems = mainItems;
        this.bodyMetrics = bodyMetrics;

        if (currentFolderName != null) {
            // Внутри папки: показываем только элементы, принадлежащие папке
            Folder folder = folders.stream()
                    .filter(f -> f.name.equals(currentFolderName))
                    .findFirst()
                    .orElse(null);
            if (folder != null) {
                Log.d("CombinedAdapter", "Processing folder: " + folder.name + ", itemIds=" + folder.itemIds);
                for (String itemId : folder.itemIds) {
                    Workout workout = workouts.stream()
                            .filter(w -> w.id.equals(itemId))
                            .findFirst()
                            .orElse(null);
                    if (workout != null) {
                        this.items.add(workout);
                        Log.d("CombinedAdapter", "Added workout to folder: " + workout.name + ", id=" + itemId);
                        continue;
                    }
                    BodyMetric metric = bodyMetrics.stream()
                            .filter(m -> m.id.equals(itemId))
                            .findFirst()
                            .orElse(null);
                    if (metric != null) {
                        this.items.add(metric);
                        Log.d("CombinedAdapter", "Added metric to folder: " + metric.type + ", id=" + itemId);
                    } else {
                        Log.w("CombinedAdapter", "Item not found for id: " + itemId);
                    }
                }
            } else {
                Log.w("CombinedAdapter", "Folder not found: " + currentFolderName);
            }
        } else {
            // Главный экран: показываем папки и элементы вне папок
            this.items.addAll(mainItems);
            Log.d("CombinedAdapter", "Main screen items: " + mainItems.size());
        }
        notifyDataSetChanged();
        Log.d("CombinedAdapter", "Updated data: folder=" + currentFolderName + ", items=" + items.size());
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof Folder) return TYPE_FOLDER;
        else if (item instanceof Workout) return TYPE_WORKOUT;
        return TYPE_METRIC;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOLDER) {
            View view = inflater.inflate(R.layout.folder, parent, false);
            return new FolderViewHolder(view);
        } else if (viewType == TYPE_WORKOUT) {
            View view = inflater.inflate(R.layout.widget, parent, false);
            return new WorkoutViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.widget_body, parent, false);
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
            // Используем getItemCount() из Folder
            folderHolder.items.setText(getItemsText(folder.getItemCount()));
            folderHolder.itemView.setOnClickListener(v -> activity.openFolder(folder));
        } else if (holder instanceof WorkoutViewHolder) {
            Workout workout = (Workout) item;
            WorkoutViewHolder workoutHolder = (WorkoutViewHolder) holder;
            workoutHolder.workoutName.setText(workout.name);
            workoutHolder.workoutDay.setText(activity.getNearestDay(workout.id, workout.dates));
            LocalDate today = LocalDate.now();
            LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            long weekWorkoutCount = workout.dates.stream()
                    .map(date -> {
                        try {
                            return LocalDate.parse(date);
                        } catch (Exception e) {
                            Log.e("CombinedAdapter", "Error parsing date: " + date, e);
                            return null;
                        }
                    })
                    .filter(date -> date != null && !date.isBefore(today) && !date.isAfter(endOfWeek))
                    .count();
            workoutHolder.workoutCount.setText(String.valueOf(weekWorkoutCount));
            workoutHolder.itemView.setOnClickListener(v -> activity.editWorkout(workout.name));
        } else if (holder instanceof MetricViewHolder) {
            BodyMetric metric = (BodyMetric) item;
            MetricViewHolder metricHolder = (MetricViewHolder) holder;
            String displayValue = metric.type.toLowerCase().equals("вес") ? metric.value + " кг" :
                    metric.type.toLowerCase().equals("рост") ? metric.value + " см" : metric.value;
            metricHolder.countBodyMetrics.setText(displayValue);
            metricHolder.notes.setText(activity.formatDate(metric.timestamp));
        }
    }

    private String getItemsText(int count) {
        if (count == 0) return "0 элементов";
        if (count == 1) return "1 элемент";
        if (count == 2 || count == 3 || count == 4) return count + " элемента";
        return count + " элементов";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        TextView items;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folder_name);
            items = itemView.findViewById(R.id.items);
        }
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView workoutName;
        TextView workoutDay;
        TextView workoutCount;

        WorkoutViewHolder(View itemView) {
            super(itemView);
            workoutName = itemView.findViewById(R.id.workout_name);
            workoutDay = itemView.findViewById(R.id.workout_day);
            workoutCount = itemView.findViewById(R.id.workout_count);
        }
    }

    static class MetricViewHolder extends RecyclerView.ViewHolder {
        TextView countBodyMetrics;
        TextView notes;

        MetricViewHolder(View itemView) {
            super(itemView);
            countBodyMetrics = itemView.findViewById(R.id.count_body_metrics);
            notes = itemView.findViewById(R.id.notes);
        }
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
        Log.d("CombinedAdapter", "Set workouts: size=" + (workouts != null ? workouts.size() : 0));
    }

    public void setBodyMetrics(List<BodyMetric> bodyMetrics) {
        this.bodyMetrics = bodyMetrics;
        Log.d("CombinedAdapter", "Set bodyMetrics: size=" + (bodyMetrics != null ? bodyMetrics.size() : 0));
    }

    // Новый метод для получения позиции папки по её имени
    public int getFolderPosition(String folderName) {
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof Folder && ((Folder) item).name.equals(folderName)) {
                return i;
            }
        }
        return -1;
    }
}