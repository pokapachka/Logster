package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity implements AddWorkout.WorkoutSelectionListener {

    private Workout currentWorkout;
    private List<ExercisesAdapter.Exercise> selectedExercises;
    private BottomNavigationManager navManager;
    private BottomSheets widgetSheet;
    private String selectedWorkoutName;
    private List<String> selectedDays;
    private List<Folder> folders;
    public List<Workout> workouts;
    private List<BodyMetric> bodyMetrics;
    private RecyclerView workoutRecyclerView;
    private CombinedAdapter combinedAdapter;
    private SharedPreferences sharedPreferences;
    private boolean isInFolder = false;
    private List<Object> mainItems;
    private FrameLayout backButton;
    private TextView titleTextView;
    private String currentFolderName = null;
    private Map<DayOfWeek, String> weeklySchedule;
    private Map<String, String> specificDates;
    private SelectedExercisesAdapter selectedExercisesAdapter;
    private static final int COLOR_UNSELECTED = 0xFF606062;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WORKOUTS_KEY = "workouts";
    private static final String METRICS_KEY = "key_metrics_body";
    private static final String FOLDERS_KEY = "folders";
    private static final String WEEKLY_SCHEDULE_KEY = "weekly_schedule";
    private static final String SPECIFIC_DATES_KEY = "specific_dates";
    private ExercisesAdapter exercisesAdapter;
    public List<ExercisesAdapter.Exercise> getSelectedExercises() {
        if (selectedExercises == null) {
            selectedExercises = new ArrayList<>();
        }
        return selectedExercises;
    }
    private String currentExerciseName;
    public String getCurrentExerciseName() {
        return currentExerciseName;
    }

    public void setCurrentExerciseName(String name) {
        this.currentExerciseName = name;
    }

    private static final Map<String, DayOfWeek> DAY_OF_WEEK_MAP = new HashMap<>();

    static {
        DAY_OF_WEEK_MAP.put("ПОНЕДЕЛЬНИК", DayOfWeek.MONDAY);
        DAY_OF_WEEK_MAP.put("ВТОРНИК", DayOfWeek.TUESDAY);
        DAY_OF_WEEK_MAP.put("СРЕДА", DayOfWeek.WEDNESDAY);
        DAY_OF_WEEK_MAP.put("ЧЕТВЕРГ", DayOfWeek.THURSDAY);
        DAY_OF_WEEK_MAP.put("ПЯТНИЦА", DayOfWeek.FRIDAY);
        DAY_OF_WEEK_MAP.put("СУББОТА", DayOfWeek.SATURDAY);
        DAY_OF_WEEK_MAP.put("ВОСКРЕСЕНЬЕ", DayOfWeek.SUNDAY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navManager = new BottomNavigationManager(findViewById(R.id.main), this);
        navManager.setCurrentActivity("MainActivity");

        widgetSheet = new BottomSheets(this, R.layout.widgets);
        AddWorkout.setWorkoutSelectionListener(this);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        folders = new ArrayList<>();
        workouts = new ArrayList<>();
        bodyMetrics = new ArrayList<>();
        weeklySchedule = new HashMap<>();
        specificDates = new HashMap<>();
        mainItems = new ArrayList<>();
        selectedExercises = new ArrayList<>();

        selectedExercisesAdapter = new SelectedExercisesAdapter(this, new SelectedExercisesAdapter.OnExerciseRemovedListener() {
            @Override
            public void onExerciseRemoved(ExercisesAdapter.Exercise exercise) {
                selectedExercises.remove(exercise);
                // Очищаем сеты упражнения и сбрасываем состояние выбора
                exercise.getSets().clear();
                exercise.setSelected(false);
                selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
                Workout workout = workouts.stream()
                        .filter(w -> w.name.equals(selectedWorkoutName))
                        .findFirst()
                        .orElse(null);
                if (workout != null) {
                    workout.exerciseIds.remove(Integer.valueOf(exercise.getId()));
                    // Удаляем сеты упражнения из workout.exerciseSets
                    workout.exerciseSets.remove(exercise.getId());
                    saveWorkouts();
                    Log.d("MainActivity", "Упражнение удалено: " + exercise.getName() + ", id=" + exercise.getId() + ", сеты очищены");
                } else {
                    Log.w("MainActivity", "Тренировка не найдена: " + selectedWorkoutName);
                }
            }
        });

        workoutRecyclerView = findViewById(R.id.workout_recycler_view);
        workoutRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        workoutRecyclerView.setNestedScrollingEnabled(true);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> exitFolder());

        titleTextView = findViewById(R.id.title_home);
        titleTextView.setText("Logster");

        combinedAdapter = new CombinedAdapter(this);
        workoutRecyclerView.setAdapter(combinedAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            private boolean isOverFolder = false;
            private Folder targetFolder = null;
            private RecyclerView.ViewHolder targetHolder = null;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                Log.d("ItemTouchHelper", "onMove: from=" + fromPos + ", to=" + toPos + ", itemsSize=" + combinedAdapter.items.size());

                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION ||
                        fromPos >= combinedAdapter.items.size() || toPos >= combinedAdapter.items.size()) {
                    Log.e("ItemTouchHelper", "Невалидные позиции: from=" + fromPos + ", to=" + toPos);
                    return false;
                }

                Object source = combinedAdapter.items.get(fromPos);
                Object targetItem = combinedAdapter.items.get(toPos);
                Log.d("ItemTouchHelper", "Source: " + source.getClass().getSimpleName() + ", Target: " + targetItem.getClass().getSimpleName());

                // Обработка нахождения над папкой (только для тренировок/метрик на главном экране)
                if (!isInFolder && targetItem instanceof Folder && !(source instanceof Folder)) {
                    if (targetFolder != targetItem) {
                        resetFolderHighlight();
                        targetFolder = (Folder) targetItem;
                        targetHolder = target;
                        animateStroke(targetHolder, "#333339", "#727275");
                        Log.d("ItemTouchHelper", "Над папкой: " + targetFolder.name);
                    }
                    isOverFolder = true;
                } else {
                    if (isOverFolder) {
                        resetFolderHighlight();
                        isOverFolder = false;
                        Log.d("ItemTouchHelper", "Выход из зоны папки");
                    }
                }

                // Перемещение (только если не над папкой)
                if (!(targetItem instanceof Folder) || source instanceof Folder) {
                    Collections.swap(combinedAdapter.items, fromPos, toPos);
                    combinedAdapter.notifyItemMoved(fromPos, toPos);
                    combinedAdapter.notifyItemRangeChanged(Math.min(fromPos, toPos), Math.abs(fromPos - toPos) + 1);
                    Log.d("ItemTouchHelper", "Перемещено: from=" + fromPos + ", to=" + toPos);
                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Log.d("ItemTouchHelper", "onSwiped: pos=" + pos + ", dir=" + direction);

                if (pos == RecyclerView.NO_POSITION || pos >= combinedAdapter.items.size()) {
                    Log.e("ItemTouchHelper", "Невалидная позиция свайпа: " + pos);
                    return;
                }

                Object item = combinedAdapter.items.get(pos);
                Log.d("ItemTouchHelper", "Свайп: тип=" + item.getClass().getSimpleName());

                try {
                    if (isInFolder) {
                        Folder folder = folders.stream()
                                .filter(f -> f.name.equals(currentFolderName))
                                .findFirst()
                                .orElse(null);
                        if (folder != null) {
                            String itemId = null;
                            if (item instanceof Workout) {
                                itemId = ((Workout) item).id;
                            } else if (item instanceof BodyMetric) {
                                itemId = ((BodyMetric) item).id;
                            }
                            if (itemId != null) {
                                folder.itemIds.remove(itemId);
                                combinedAdapter.items.remove(pos);
                                combinedAdapter.notifyItemRemoved(pos);
                                // Обновляем виджет папки
                                int folderPos = combinedAdapter.getFolderPosition(folder.name);
                                if (folderPos != -1) {
                                    combinedAdapter.notifyItemChanged(folderPos);
                                }
                                updateMainItems();
                                saveItems();
                                Log.d("ItemTouchHelper", "Удалено из папки: ID=" + itemId + ", папка=" + currentFolderName);
                            }
                        } else {
                            Log.w("ItemTouchHelper", "Папка не найдена: " + currentFolderName);
                        }
                    } else {
                        if (item instanceof Folder) {
                            folders.remove((Folder) item);
                        } else if (item instanceof Workout) {
                            Workout workout = (Workout) item;
                            workouts.remove(workout);
                            removeWorkoutFromSchedules(workout.id);
                        } else if (item instanceof BodyMetric) {
                            bodyMetrics.remove((BodyMetric) item);
                        }
                        combinedAdapter.items.remove(pos);
                        combinedAdapter.notifyItemRemoved(pos);
                        updateMainItems();
                        saveItems();
                    }
                    Log.d("ItemTouchHelper", "Свайп завершен: папки=" + folders.size() + ", mainItems=" + mainItems.size());
                } catch (Exception e) {
                    Log.e("ItemTouchHelper", "Ошибка свайпа: " + e.getMessage(), e);
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                if (viewHolder == null) {
                    Log.w("ItemTouchHelper", "onSelectedChanged: пустой holder, actionState=" + actionState);
                    return;
                }

                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    Log.w("ItemTouchHelper", "onSelectedChanged: pos=-1, actionState=" + actionState);
                    return;
                }

                Log.d("ItemTouchHelper", "onSelectedChanged: pos=" + pos + ", actionState=" + actionState);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.animate()
                            .scaleX(1.03f)
                            .scaleY(1.03f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    viewHolder.itemView.setElevation(4f);
                    Object item = combinedAdapter.items.get(pos);
                    if (item instanceof Workout || item instanceof BodyMetric || item instanceof Folder) {
                        animateStroke(viewHolder, "#333339", "#727275");
                    }
                    Log.d("ItemTouchHelper", "Перетаскивание начато: pos=" + pos);
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    viewHolder.itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    viewHolder.itemView.setElevation(0f);
                    Object item = combinedAdapter.items.get(pos);
                    if (item instanceof Workout || item instanceof BodyMetric || item instanceof Folder) {
                        animateStroke(viewHolder, "#727275", "#333339");
                    }
                    Log.d("ItemTouchHelper", "Перетаскивание завершено: pos=" + pos);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                int pos = viewHolder.getAdapterPosition();
                Log.d("ItemTouchHelper", "clearView: pos=" + pos + ", itemsSize=" + combinedAdapter.items.size());

                if (pos == RecyclerView.NO_POSITION || pos >= combinedAdapter.items.size()) {
                    Log.w("ItemTouchHelper", "Невалидная позиция в clearView: pos=" + pos);
                    resetFolderHighlight();
                    isOverFolder = false;
                    return;
                }

                viewHolder.itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                viewHolder.itemView.setElevation(0f);
                Object item = combinedAdapter.items.get(pos);
                if (item instanceof Workout || item instanceof BodyMetric || item instanceof Folder) {
                    animateStroke(viewHolder, "#727275", "#333339");
                }

                // Сброс всех ViewHolder для предотвращения случайной подсветки
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
                    if (holder != null && holder != viewHolder) {
                        Object childItem = combinedAdapter.items.get(holder.getAdapterPosition());
                        if (childItem instanceof Workout || childItem instanceof BodyMetric || childItem instanceof Folder) {
                            animateStroke(holder, "#727275", "#333339");
                        }
                        holder.itemView.setElevation(0f);
                    }
                }

                // Сброс в папку с анимацией
                if (targetHolder != null && targetFolder != null && !isInFolder) {
                    int targetPos = targetHolder.getAdapterPosition();
                    if (targetPos != RecyclerView.NO_POSITION && targetPos < combinedAdapter.items.size() &&
                            combinedAdapter.items.get(targetPos) instanceof Folder &&
                            combinedAdapter.items.get(targetPos) == targetFolder) {
                        final Folder finalTargetFolder = targetFolder;
                        Object source = combinedAdapter.items.get(pos);
                        String itemId = null;
                        if (source instanceof Workout) {
                            itemId = ((Workout) source).id;
                        } else if (source instanceof BodyMetric) {
                            itemId = ((BodyMetric) source).id;
                        }
                        if (itemId != null && !finalTargetFolder.itemIds.contains(itemId)) {
                            final String finalItemId = itemId;
                            final int finalPos = pos;
                            final View sourceView = viewHolder.itemView;
                            View targetView = targetHolder.itemView;
                            float targetX = targetView.getX() + targetView.getWidth() / 2f;
                            float targetY = targetView.getY() + targetView.getHeight() / 2f;
                            float sourceX = sourceView.getX();
                            float sourceY = sourceView.getY();

                            sourceView.animate()
                                    .translationX(targetX - sourceX)
                                    .translationY(targetY - sourceY)
                                    .scaleX(0.3f)
                                    .scaleY(0.3f)
                                    .alpha(0.0f)
                                    .setDuration(300)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .withEndAction(() -> {
                                        finalTargetFolder.addItem(finalItemId);
                                        combinedAdapter.items.remove(finalPos);
                                        combinedAdapter.notifyItemRemoved(finalPos);
                                        int folderPos = combinedAdapter.getFolderPosition(finalTargetFolder.name);
                                        if (folderPos != -1) {
                                            combinedAdapter.notifyItemChanged(folderPos);
                                        }
                                        updateMainItems();
                                        saveItems();
                                        sourceView.setTranslationX(0f);
                                        sourceView.setTranslationY(0f);
                                        sourceView.setScaleX(1.0f);
                                        sourceView.setScaleY(1.0f);
                                        sourceView.setAlpha(1.0f);
                                        Log.d("ItemTouchHelper", "Dropped into folder: ID=" + finalItemId + ", folder=" + finalTargetFolder.name);
                                    })
                                    .start();
                            saveFolders();
                        } else {
                            Log.w("ItemTouchHelper", "Item already in folder or invalid: " + itemId);
                        }
                    } else {
                        Log.d("ItemTouchHelper", "Dropped not over folder: targetPos=" + targetPos);
                    }
                } else {
                    if (isInFolder) {
                        Folder folder = folders.stream()
                                .filter(f -> f.name.equals(currentFolderName))
                                .findFirst()
                                .orElse(null);
                        if (folder != null) {
                            folder.itemIds.clear();
                            for (Object obj : combinedAdapter.items) {
                                if (obj instanceof Workout) {
                                    folder.itemIds.add(((Workout) obj).id);
                                } else if (obj instanceof BodyMetric) {
                                    folder.itemIds.add(((BodyMetric) obj).id);
                                }
                            }
                            // Сохранение папки
                            int folderPos = combinedAdapter.getFolderPosition(folder.name);
                            if (folderPos != -1) {
                                combinedAdapter.notifyItemChanged(folderPos);
                            }
                            saveItems();
                            Log.d("ItemTouchHelper", "Обновлена папка: " + currentFolderName + ", itemIds=" + folder.itemIds);
                        } else {
                            Log.w("ItemTouchHelper", "Папка не найдена: " + currentFolderName);
                        }
                    } else {
                        syncItemsWithMainItems();
                        saveItems();
                        Log.d("ItemTouchHelper", "Обновлены mainItems: " + mainItems.size() + ", items=" + mainItems);
                    }
                }

                resetFolderHighlight();
                isOverFolder = false;
                Log.d("ItemTouchHelper", "clearView завершен: isInFolder=" + isInFolder + ", itemsSize=" + combinedAdapter.items.size());
            }

            private void animateStroke(RecyclerView.ViewHolder holder, String fromColor, String toColor) {
                if (holder == null) {
                    Log.w("ItemTouchHelper", "animateStroke: holder=null");
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
                    Log.d("ItemTouchHelper", "Подсветка: pos=" + holder.getAdapterPosition() + ", from=" + fromColor);
                } else {
                    Log.w("ItemTouchHelper", "GradientDrawable не найден");
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
                Log.w("ItemTouchHelper", "GradientDrawable не найден: " + (drawable != null ? drawable.getClass().getSimpleName() : "null"));
                return null;
            }

            private void resetFolderHighlight() {
                if (targetHolder != null) {
                    animateStroke(targetHolder, "#727275", "#333339");
                    targetFolder = null;
                    targetHolder = null;
                    Log.d("ItemTouchHelper", "Подсветка папки сброшена");
                }
            }

            private void syncItemsWithMainItems() {
                mainItems.clear();
                mainItems.addAll(folders);
                try {
                    List<String> folderItemIds = folders.stream()
                            .flatMap(f -> f.itemIds != null ? f.itemIds.stream() : Stream.empty())
                            .collect(Collectors.toList());
                    for (Workout workout : workouts) {
                        if (!folderItemIds.contains(workout.id)) {
                            mainItems.add(workout);
                        } else {
                            Log.w("ItemTouchHelper", "Тренировка в папке не добавлена: " + workout.name + ", id=" + workout.id);
                        }
                    }
                    for (BodyMetric metric : bodyMetrics) {
                        if (!folderItemIds.contains(metric.id)) {
                            mainItems.add(metric);
                        }
                    }
                } catch (Exception e) {
                    Log.e("ItemTouchHelper", "Ошибка синхронизации mainItems: " + e.getMessage(), e);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(workoutRecyclerView);

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(150);
        animator.setRemoveDuration(200);
        animator.setAddDuration(200);
        animator.setChangeDuration(200);
        workoutRecyclerView.setItemAnimator(animator);

        workoutRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int columnCount = 2;
                int marginBetween = dpToPx(10);
                outRect.bottom = marginBetween;
                if (position % columnCount == 0) {
                    outRect.left = 0;
                    outRect.right = marginBetween / 2;
                } else {
                    outRect.left = marginBetween / 2;
                    outRect.right = 0;
                }
            }
        });

        com.example.logster.AddBodyMetric.setMetricSelectionListener(new com.example.logster.AddBodyMetric.MetricSelectionListener() {
            @Override
            public void onMetricSelected(String metricKey) {
                switchSheet(R.layout.add_metric2, metricKey, true, R.anim.slide_out_left, R.anim.slide_in_right);
            }

            @Override
            public void onMetricValueSelected(String metricType, String value) {
                bodyMetrics.add(new BodyMetric(UUID.randomUUID().toString(), metricType, value, System.currentTimeMillis()));
                saveBodyMetrics();
                updateMainItems();
                combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                widgetSheet.hide(null);
            }
        });

        folders = loadFolders();
        workouts = loadWorkouts();
        bodyMetrics = loadBodyMetrics();
        loadWeeklySchedule();
        loadSpecificDates();
        syncWorkoutDates();
        updateMainItems();
        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);

        hideSystemUI();
    }

    private void updateMainItems() {
        mainItems.clear();
        mainItems.addAll(folders);
        List<String> folderItemIds = folders.stream()
                .flatMap(f -> f.itemIds != null ? f.itemIds.stream() : Stream.empty())
                .collect(Collectors.toList());
        Log.d("MainActivity", "Folder item IDs: " + folderItemIds.size());
        for (Workout workout : workouts) {
            if (!folderItemIds.contains(workout.id)) {
                mainItems.add(workout);
                Log.d("MainActivity", "Added workout to mainItems: " + workout.name + ", id=" + workout.id);
            } else {
                Log.d("MainActivity", "Workout in folder, skipped: " + workout.name + ", id=" + workout.id);
            }
        }
        for (BodyMetric metric : bodyMetrics) {
            if (!folderItemIds.contains(metric.id)) {
                mainItems.add(metric);
                Log.d("MainActivity", "Added metric to mainItems: " + metric.type + ", id=" + metric.id);
            }
        }
        Log.d("MainActivity", "Updated mainItems: folders=" + folders.size() + ", workouts=" + workouts.size() + ", metrics=" + bodyMetrics.size() + ", total=" + mainItems.size());
    }

    private void updateTitleWithAnimation(String newTitle) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            titleTextView.setAlpha(alpha);
            if (alpha == 0f) {
                titleTextView.setText(newTitle);
                ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.addUpdateListener(fadeAnimation -> titleTextView.setAlpha((float) fadeAnimation.getAnimatedValue()));
                fadeIn.start();
            }
        });
        animator.start();
    }

    public void openFolder(Folder folder) {
        currentFolderName = folder.name;
        isInFolder = true;
        backButton.setVisibility(View.VISIBLE);
        updateTitleWithAnimation(folder.name);
        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
        Log.d("MainActivity", "Opened folder: " + folder.name + ", itemIds=" + folder.itemIds.size());
    }

    private void exitFolder() {
        currentFolderName = null;
        isInFolder = false;
        updateTitleWithAnimation("Logster");
        backButton.setVisibility(View.GONE);
        combinedAdapter.updateData(folders, workouts, bodyMetrics, null);
    }

    private void removeWorkoutFromSchedules(String workoutId) {
        weeklySchedule.entrySet().removeIf(entry -> entry.getValue().equals(workoutId));
        specificDates.entrySet().removeIf(entry -> entry.getValue().equals(workoutId));
        saveWeeklySchedule();
        saveSpecificDates();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onBackPressed() {
        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(null);
        } else if (isInFolder) {
            exitFolder();
        } else {
            super.onBackPressed();
            if (isTaskRoot()) {
                moveTaskToBack(true);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent, options.toBundle());
                navManager.forceSetActiveButton("MainActivity");
                finish();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        navManager.forceSetActiveButton("MainActivity");
    }

    @Override
    protected void onResume() {
        super.onResume();
        navManager.forceSetActiveButton("MainActivity");
        updateMainItems();
        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
    }

    public void addWidgets(View view) {
        selectedWorkoutName = null;
        if (selectedDays != null) selectedDays.clear();
        else selectedDays = new ArrayList<>();
        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(() -> {
                widgetSheet = new BottomSheets(this, R.layout.widgets);
                widgetSheet.show();
            });
        } else {
            widgetSheet = new BottomSheets(this, R.layout.widgets);
            widgetSheet.show();
        }
    }

    public void closeSheet(View view) {
        hideKeyboard();
        selectedWorkoutName = null;
        if (selectedDays != null) selectedDays.clear();
        widgetSheet.hide(null);
    }

    private Map<String, List<String>> getWorkoutCountByDay() {
        Map<String, List<String>> workoutCountByDay = new HashMap<>();
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        for (String day : days) {
            workoutCountByDay.put(day, new ArrayList<>());
        }

        LocalDate today = LocalDate.now();

        // Обработка еженедельного расписания
        if (weeklySchedule != null) {
            for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
                DayOfWeek dayOfWeek = entry.getKey();
                String workoutId = entry.getValue();
                if (dayOfWeek == null || workoutId == null || workoutId.isEmpty()) {
                    continue;
                }
                // Сохраняем dayName как финальную переменную
                final String dayName = dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"))
                        .substring(0, 1).toUpperCase() + dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                workouts.stream()
                        .filter(w -> w.id.equals(workoutId))
                        .findFirst()
                        .ifPresent(workout -> workoutCountByDay.computeIfAbsent(dayName, k -> new ArrayList<>()).add(workout.name));
            }
        }

        // Обработка конкретных дат
        if (specificDates != null) {
            for (Map.Entry<String, String> entry : specificDates.entrySet()) {
                String dateStr = entry.getKey();
                String workoutId = entry.getValue();
                if (dateStr == null || workoutId == null || workoutId.isEmpty()) {
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    if (!date.isBefore(today)) {
                        // Сохраняем dayName как финальную переменную
                        final String dayName = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"))
                                .substring(0, 1).toUpperCase() + date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                        workouts.stream()
                                .filter(w -> w.id.equals(workoutId))
                                .findFirst()
                                .ifPresent(workout -> workoutCountByDay.computeIfAbsent(dayName, k -> new ArrayList<>()).add(workout.name));
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Ошибка разбора даты: " + dateStr, e);
                }
            }
        }

        return workoutCountByDay;
    }

    private void openAddWorkout3(View view, EditText workoutNameEditText) {
        selectedWorkoutName = workoutNameEditText.getText().toString();
        if (!selectedWorkoutName.trim().isEmpty()) {
            switchSheet(R.layout.add_workout3, selectedWorkoutName, true, R.anim.slide_out_left, R.anim.slide_in_right);
        } else {
            Toast.makeText(this, "Введите название тренировки", Toast.LENGTH_SHORT).show();
        }
    }

    public void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
        }
    }

    public String getNearestDay(String workoutId, List<String> dates) {
        LocalDate today = LocalDate.now();
        LocalDate nearestDate = null;
        long minDaysAhead = Long.MAX_VALUE;

        for (String dateStr : dates) {
            try {
                LocalDate date = LocalDate.parse(dateStr);
                long daysAhead = java.time.temporal.ChronoUnit.DAYS.between(today, date);
                if (daysAhead >= 0 && daysAhead < minDaysAhead) {
                    minDaysAhead = daysAhead;
                    nearestDate = date;
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Ошибка разбора даты: " + dateStr, e);
            }
        }
        return nearestDate != null ?
                nearestDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).toLowerCase() :
                "Не выбрано";
    }

    private void saveWorkouts() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray workoutsArray = new JSONArray();
            for (Workout workout : workouts) workoutsArray.put(workout.toJson());
            editor.putString(WORKOUTS_KEY, workoutsArray.toString());
            editor.apply();
            combinedAdapter.setWorkouts(workouts);
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения тренировок: " + e.getMessage(), e);
        }
    }

    private List<Workout> loadWorkouts() {
        List<Workout> loadedWorkouts = new ArrayList<>();
        String workoutsJson = sharedPreferences.getString(WORKOUTS_KEY, null);
        if (workoutsJson != null && !workoutsJson.isEmpty()) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                for (int i = 0; i < workoutsArray.length(); i++) {
                    loadedWorkouts.add(Workout.fromJson(workoutsArray.getJSONObject(i)));
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки тренировок: " + e.getMessage(), e);
            }
        }
        this.workouts = loadedWorkouts;
        combinedAdapter.setWorkouts(loadedWorkouts);
        return loadedWorkouts;
    }

    private void saveBodyMetrics() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray metricsArray = new JSONArray();
            for (BodyMetric metric : bodyMetrics) metricsArray.put(metric.toJson());
            editor.putString(METRICS_KEY, metricsArray.toString());
            editor.apply();
            combinedAdapter.setBodyMetrics(bodyMetrics);
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения метрик: " + e.getMessage(), e);
        }
    }

    private List<BodyMetric> loadBodyMetrics() {
        List<BodyMetric> loadedMetrics = new ArrayList<>();
        String metricsJson = sharedPreferences.getString(METRICS_KEY, null);
        if (metricsJson != null && !metricsJson.isEmpty()) {
            try {
                JSONArray metricsArray = new JSONArray(metricsJson);
                for (int i = 0; i < metricsArray.length(); i++) {
                    loadedMetrics.add(BodyMetric.fromJson(metricsArray.getJSONObject(i)));
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки метрик: " + e.getMessage(), e);
            }
        }
        this.bodyMetrics = loadedMetrics;
        combinedAdapter.setBodyMetrics(loadedMetrics);
        return loadedMetrics;
    }

    public void addFolder(View view) {
        switchSheet(R.layout.add_folders, null, false, 0, 0);
    }

    private void saveFolders() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray foldersArray = new JSONArray();
            for (Folder folder : folders) foldersArray.put(folder.toJson());
            editor.putString(FOLDERS_KEY, foldersArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения папок: " + e.getMessage(), e);
        }
    }

    private List<Folder> loadFolders() {
        List<Folder> loadedFolders = new ArrayList<>();
        String foldersJson = sharedPreferences.getString(FOLDERS_KEY, "");
        if (!foldersJson.isEmpty()) {
            try {
                JSONArray foldersArray = new JSONArray(foldersJson);
                for (int i = 0; i < foldersArray.length(); i++) {
                    Folder folder = Folder.fromJson(foldersArray.getJSONObject(i));
// Очистка невалидных itemIds
                    List<String> validItemIds = new ArrayList<>();
                    for (String itemId : folder.itemIds) {
                        boolean exists = workouts.stream().anyMatch(w -> w.id.equals(itemId)) ||
                                bodyMetrics.stream().anyMatch(m -> m.id.equals(itemId));
                        if (exists) {
                            validItemIds.add(itemId);
                        } else {
                            Log.w("MainActivity", "Removed invalid itemId: " + itemId + " from folder: " + folder.name);
                        }
                    }
                    folder.itemIds = validItemIds;
                    if (!loadedFolders.stream().anyMatch(f -> f.name.equals(folder.name))) {
                        loadedFolders.add(folder);
                    }
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки папок: " + e.getMessage(), e);
            }
        }
        this.folders = loadedFolders;
        saveFolders(); // Сохраняем после очистки
        return loadedFolders;
    }

    private void saveWeeklySchedule() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject json = new JSONObject();
            for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
                json.put(entry.getKey().toString(), entry.getValue());
            }
            editor.putString(WEEKLY_SCHEDULE_KEY, json.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения расписания: " + e.getMessage(), e);
        }
    }

    private void loadWeeklySchedule() {
        String scheduleJson = sharedPreferences.getString(WEEKLY_SCHEDULE_KEY, null);
        if (scheduleJson != null) {
            try {
                JSONObject json = new JSONObject(scheduleJson);
                weeklySchedule.clear();
                for (DayOfWeek day : DayOfWeek.values()) {
                    String workoutId = json.optString(day.toString(), null);
                    if (workoutId != null && workouts.stream().anyMatch(w -> w.id.equals(workoutId))) {
                        weeklySchedule.put(day, workoutId);
                    }
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки расписания: " + e.getMessage(), e);
            }
        }
    }

    private void saveSpecificDates() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : specificDates.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            editor.putString(SPECIFIC_DATES_KEY, json.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения дат: " + e.getMessage(), e);
        }
    }

    private void loadSpecificDates() {
        String datesJson = sharedPreferences.getString(SPECIFIC_DATES_KEY, null);
        if (datesJson != null) {
            try {
                JSONObject json = new JSONObject(datesJson);
                specificDates.clear();
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    specificDates.put(key, json.getString(key));
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки дат: " + e.getMessage(), e);
            }
        }
    }

    private void saveItems() {
        saveFolders();
        saveWorkouts();
        saveBodyMetrics();
        saveWeeklySchedule();
        saveSpecificDates();
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (Workout workout : workouts) workout.dates.clear();

        for (Map.Entry<String, String> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;
            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (!date.isBefore(today)) {
                    workouts.stream().filter(w -> w.id.equals(workoutId)).findFirst()
                            .ifPresent(workout -> workout.addDate(dateStr));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Ошибка разбора даты: " + dateStr, e);
            }
        }

        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;
            LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                if (!specificDates.containsKey(dateStr)) {
                    workouts.stream().filter(w -> w.id.equals(workoutId)).findFirst()
                            .ifPresent(workout -> workout.addDate(dateStr));
                }
                currentDate = currentDate.plusWeeks(1);
            }
        }
        saveWorkouts();
    }

    public String formatDate(long timestamp) {
        LocalDate today = LocalDate.now();
        LocalDate date = LocalDate.ofEpochDay(timestamp / (1000 * 60 * 60 * 24));
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, today);
        if (daysDiff == 0) return "сегодня";
        else if (daysDiff == 1) return "вчера";
        else if (daysDiff == 2) return "позавчера";
        return String.format("%02d.%02d", date.getDayOfMonth(), date.getMonthValue());
    }

    public void addWorkout(View view) {
        switchSheet(R.layout.add_workout, null, false, 0, 0);
    }

    public void addWorkout2(View view) {
        hideKeyboard();
        switchSheet(R.layout.add_workout3, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
    }

    public void backSheet(View view) {
        hideKeyboard();
        switchSheet(R.layout.widgets, null, false, 0, 0);
    }

    public void backSheet2(View view) {
        hideKeyboard();
        switchSheet(R.layout.add_workout, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
    }

    public void backSheet3(View view) {
        hideKeyboard();
        switchSheet(R.layout.add_workout2, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
    }

    public void backSheetMetric2(View view) {
        hideKeyboard();
        switchSheet(R.layout.add_metric, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
    }

    @Override
    public void onWorkoutSelected(String workoutKey) {
        switchSheet(R.layout.add_workout2, workoutKey, true, R.anim.slide_out_left, R.anim.slide_in_right);
    }

    public void addBodyMetric(View view) {
        switchSheet(R.layout.add_metric, null, false, 0, 0);
    }

    public void editWorkout(String workoutName) {
        selectedWorkoutName = workoutName;
        selectedExercises.clear();
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(workoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            for (Integer exerciseId : workout.exerciseIds) {
                ExercisesAdapter.Exercise exercise = ExerciseList.getAllExercises().stream()
                        .filter(e -> e.getId() == exerciseId)
                        .findFirst()
                        .orElse(null);
                if (exercise != null) {
                    exercise.setSelected(true);
                    List<Set> workoutSets = workout.exerciseSets.getOrDefault(exercise.getId(), new ArrayList<>());
                    exercise.getSets().clear();
                    exercise.getSets().addAll(workoutSets);
                    if (!selectedExercises.contains(exercise)) {
                        selectedExercises.add(exercise);
                        Log.d("MainActivity", "Добавлено упражнение: " + exercise.getName() + ", id=" + exerciseId + ", подходов=" + workoutSets.size());
                    }
                } else {
                    Log.w("MainActivity", "Упражнение не найдено для id: " + exerciseId);
                }
            }
            Log.d("MainActivity", "Загружено " + selectedExercises.size() + " упражнений для тренировки: " + workoutName);
        } else {
            Log.w("MainActivity", "Тренировка не найдена: " + workoutName);
        }
        switchSheet(R.layout.edit_workout, workoutName, false, 0, 0);
    }

    public void backSheetEditWorkout(View view) {
        widgetSheet.hide(null);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void switchSheet(int layoutId, String data, boolean useHorizontalTransition, int exitAnim, int enterAnim) {
        hideKeyboard();
        View newView = LayoutInflater.from(this).inflate(layoutId, null);

        if (layoutId == R.layout.add_folders) {
            EditText folderNameEditText = newView.findViewById(R.id.folder_name_edit_text);
            View continueBtn = newView.findViewById(R.id.continue_btn);
            View closeBtn = newView.findViewById(R.id.close);

            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> {
                    continueBtn.setEnabled(false);
                    String folderName = folderNameEditText != null ? folderNameEditText.getText().toString().trim() : "";
                    if (!folderName.isEmpty()) {
                        String folderId = UUID.randomUUID().toString();
                        if (!folders.stream().anyMatch(f -> f.name.equals(folderName))) {
                            Folder folder = new Folder(folderId, folderName);
                            folders.add(folder);
                            saveFolders();
                            updateMainItems();
                            combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                            widgetSheet.hide(() -> continueBtn.setEnabled(true));
                        } else {
                            Toast.makeText(this, "Папка уже существует", Toast.LENGTH_SHORT).show();
                            continueBtn.setEnabled(true);
                        }
                    } else {
                        Toast.makeText(this, "Введите название папки", Toast.LENGTH_SHORT).show();
                        continueBtn.setEnabled(true);
                    }
                });
            } else {
                Log.w("MainActivity", "continueBtn not found in add_folders");
            }

            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> widgetSheet.hide(null));
            } else {
                Log.w("MainActivity", "closeBtn not found in add_folders");
            }
        } else if (layoutId == R.layout.add_workout) {
            ScrollView scrollView = newView.findViewById(R.id.trainingScrollView);
            LinearLayout trainingList = newView.findViewById(R.id.trainingList);
            View topLine = newView.findViewById(R.id.top_line);
            View bottomLine = newView.findViewById(R.id.bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);
            TextView workoutNameTextView = newView.findViewById(R.id.workout_name);
            TextView workoutDescriptionTextView = newView.findViewById(R.id.workout_description);

            if (scrollView != null && trainingList != null) {
                AddWorkout.setupScrollHighlight(
                        scrollView, trainingList, topLine, bottomLine,
                        centerArrow, workoutNameTextView, workoutDescriptionTextView);
            } else {
                Log.w("MainActivity", "ScrollView or trainingList not found in add_workout");
            }
        } else if (layoutId == R.layout.add_workout2) {
            EditText workoutNameEditText = newView.findViewById(R.id.ETworkout_name);
            if (data != null && workoutNameEditText != null) {
                workoutNameEditText.setText(data);
            }
            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> openAddWorkout3(v, workoutNameEditText));
            } else {
                Log.w("MainActivity", "continueBtn not found in add_workout2");
            }
        } else if (layoutId == R.layout.add_workout3) {
            if (selectedDays == null) selectedDays = new ArrayList<>();
            else selectedDays.clear();

            Map<String, List<String>> workoutCountByDay = getWorkoutCountByDay();
            com.google.android.flexbox.FlexboxLayout daysContainer = newView.findViewById(R.id.days_container);
            int[] dayIds = {R.id.day_monday, R.id.day_tuesday, R.id.day_wednesday,
                    R.id.day_thursday, R.id.day_friday, R.id.day_saturday, R.id.day_sunday};
            int[] countIds = {R.id.count_monday, R.id.count_tuesday, R.id.count_wednesday,
                    R.id.count_thursday, R.id.count_friday, R.id.count_saturday, R.id.count_sunday};
            String[] dayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};

            for (int i = 0; i < dayIds.length; i++) {
                TextView dayTextView = newView.findViewById(dayIds[i]);
                TextView countTextView = newView.findViewById(countIds[i]);
                String dayName = dayNames[i];
                List<String> workoutsForDay = workoutCountByDay.get(dayName);

                int workoutCount = workoutsForDay != null ? workoutsForDay.size() : 0;
                if (countTextView != null) {
                    countTextView.setText(String.valueOf(workoutCount));
                }

                if (dayTextView != null) {
                    dayTextView.setTag(false);
                    dayTextView.getBackground().setTint(COLOR_UNSELECTED);

                    boolean hasWorkouts = workoutCount > 0;
                    if (countTextView != null) {
                        countTextView.setTag(hasWorkouts);
                        countTextView.getBackground().setTint(hasWorkouts ? COLOR_SELECTED : COLOR_UNSELECTED);
                        countTextView.setEnabled(hasWorkouts);
                    }

                    dayTextView.setOnClickListener(v -> {
                        boolean isSelected = (boolean) dayTextView.getTag();
                        isSelected = !isSelected;
                        dayTextView.setTag(isSelected);

                        int startColor = isSelected ? COLOR_UNSELECTED : COLOR_SELECTED;
                        int endColor = isSelected ? COLOR_SELECTED : COLOR_UNSELECTED;
                        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
                        colorAnimator.setDuration(300);
                        colorAnimator.addUpdateListener(animator -> dayTextView.getBackground().setTint((int) animator.getAnimatedValue()));
                        colorAnimator.start();

                        String day = dayTextView.getText().toString();
                        if (isSelected) selectedDays.add(day);
                        else selectedDays.remove(day);
                    });

                    if (countTextView != null) {
                        countTextView.setOnClickListener(v -> {
                            if ((boolean) countTextView.getTag()) {
                                StringBuilder message = new StringBuilder("На этот день записаны " + workoutCount + " тренировок: ");
                                for (int j = 0; j < workoutsForDay.size(); j++) {
                                    message.append(workoutsForDay.get(j));
                                    if (j < workoutsForDay.size() - 1) message.append(", ");
                                }
                                Toast.makeText(MainActivity.this, message.toString(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }

            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> {
                    if (!selectedWorkoutName.isEmpty()) {
                        if (selectedDays.isEmpty()) {
                            Toast.makeText(this, "Выберите день", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String workoutId = UUID.randomUUID().toString();
                        if (workouts.stream().anyMatch(w -> w.name.equals(selectedWorkoutName))) {
                            Toast.makeText(this, "Тренировка с таким именем уже существует", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        for (String dayName : selectedDays) {
                            DayOfWeek dayOfWeek = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
                            weeklySchedule.remove(dayOfWeek);
                            weeklySchedule.put(dayOfWeek, workoutId);
                        }
                        saveWeeklySchedule();
                        Workout newWorkout = new Workout(workoutId, selectedWorkoutName);
                        workouts.add(newWorkout);
                        saveWorkouts();
                        syncWorkoutDates();
                        updateMainItems();
                        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                        widgetSheet.hide(null);
                    } else {
                        Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (layoutId == R.layout.add_metric) {
            ScrollView scrollView = newView.findViewById(R.id.trainingScrollView);
            LinearLayout metricList = newView.findViewById(R.id.trainingList);
            View topLine = newView.findViewById(R.id.top_line);
            View bottomLine = newView.findViewById(R.id.bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);
            TextView metricNameTextView = newView.findViewById(R.id.workout_name);
            TextView metricDescriptionTextView = newView.findViewById(R.id.workout_description);

            if (scrollView != null && metricList != null) {
                com.example.logster.AddBodyMetric.setupMetricSelection(
                        scrollView, metricList, topLine, bottomLine,
                        centerArrow, metricNameTextView, metricDescriptionTextView);
            }
        } else if (layoutId == R.layout.add_metric2) {
            ScrollView mainScrollView = newView.findViewById(R.id.main_value_scroll);
            LinearLayout mainValueList = newView.findViewById(R.id.main_value_list);
            View mainTopLine = newView.findViewById(R.id.main_top_line);
            View mainBottomLine = newView.findViewById(R.id.main_bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);

            com.example.logster.AddBodyMetric.setupNumberPicker(
                    mainScrollView, mainValueList, mainTopLine,
                    mainBottomLine, centerArrow, data);
        }     else if (layoutId == R.layout.edit_workout) {
            TextView titleTextView = newView.findViewById(R.id.title);
            RecyclerView selectedExercisesRecyclerView = newView.findViewById(R.id.selected_exercises_list);
            View addExercisesBtn = newView.findViewById(R.id.addExercises);
            FrameLayout closeBtn = newView.findViewById(R.id.close);

            if (titleTextView != null && data != null) {
                titleTextView.setText(data);
                Log.d("MainActivity", "Set title: " + data);
            } else {
                Log.w("MainActivity", "titleTextView или data не заданы в edit_workout");
            }

            if (selectedExercisesRecyclerView != null) {
                selectedExercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
                selectedExercisesRecyclerView.setAdapter(selectedExercisesAdapter);
                selectedExercisesRecyclerView.setVisibility(selectedExercises.isEmpty() ? View.GONE : View.VISIBLE);

                // Подключение ExerciseDragHelper для перетаскивания
                ExerciseDragHelper dragHelper = new ExerciseDragHelper(selectedExercisesAdapter,
                        selectedExercises, this, data);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(dragHelper);
                itemTouchHelper.attachToRecyclerView(selectedExercisesRecyclerView);
                Log.d("MainActivity", "ExerciseDragHelper подключён к selected_exercises_list");

                // Добавляем анимацию для плавного перетаскивания
                DefaultItemAnimator animator = new DefaultItemAnimator();
                animator.setMoveDuration(300);
                animator.setRemoveDuration(200);
                animator.setAddDuration(200);
                animator.setChangeDuration(300);
                selectedExercisesRecyclerView.setItemAnimator(animator);

                Log.d("MainActivity", "Обновлены выбранные упражнения в edit_workout: " + selectedExercises.size());
            } else {
                Log.e("MainActivity", "selected_exercises_list RecyclerView не найден");
            }

            if (addExercisesBtn != null) {
                addExercisesBtn.setOnClickListener(v -> openAddExercises(v));
            } else {
                Log.w("MainActivity", "addExercisesBtn не найдено в edit_workout");
            }

            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> backSheetEditWorkout(v));
            } else {
                Log.w("MainActivity", "closeBtn не найден в edit_workout");
            }
        } else if (layoutId == R.layout.add_exercises) {
            RecyclerView exerciseRecyclerView = newView.findViewById(R.id.exercise_list);
            TextView emptyView = newView.findViewById(R.id.empty_view);
            FrameLayout closeButton = newView.findViewById(R.id.close);
            TextView addExercisesButton = newView.findViewById(R.id.add_exercises_btn);

            if (exerciseRecyclerView != null) {
                exerciseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                List<ExercisesAdapter.Exercise> exercises = new ArrayList<>(ExerciseList.getAllExercises());
                // Синхронизация состояния выбора с selectedExercises
                for (ExercisesAdapter.Exercise exercise : exercises) {
                    exercise.setSelected(selectedExercises.stream()
                            .anyMatch(selected -> selected.getId() == exercise.getId()));
                }
                AddExercises addExercises = new AddExercises();
                exercisesAdapter = new ExercisesAdapter(exercises, addExercises, this);
                exerciseRecyclerView.setAdapter(exercisesAdapter);
                addExercises.initialize(this, exercises, exercisesAdapter, emptyView, addExercisesButton);
                exerciseRecyclerView.setVisibility(exercises.isEmpty() ? View.GONE : View.VISIBLE);
                if (emptyView != null) {
                    emptyView.setVisibility(exercises.isEmpty() ? View.VISIBLE : View.GONE);
                }
                Log.d("MainActivity", "Initialized add_exercises with " + exercises.size() + " exercises, selected: " + selectedExercises.size());
            } else {
                Log.w("MainActivity", "exercise_list RecyclerView is null in add_exercises");
            }

            // Initialize button state
            if (addExercisesButton != null) {
                boolean hasSelection = !selectedExercises.isEmpty();
                addExercisesButton.setEnabled(hasSelection);
                addExercisesButton.getBackground().setTint(hasSelection ? COLOR_SELECTED : COLOR_UNSELECTED);
                addExercisesButton.setOnClickListener(v -> {
                    // Save exercises to Workout
                    Workout workout = workouts.stream()
                            .filter(w -> w.name.equals(selectedWorkoutName))
                            .findFirst()
                            .orElse(null);
                    if (workout != null) {
                        workout.exerciseIds.clear();
                        for (ExercisesAdapter.Exercise exercise : selectedExercises) {
                            workout.exerciseIds.add(exercise.getId());
                        }
                        saveWorkouts();
                        selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
                        Log.d("MainActivity", "Saved " + workout.exerciseIds.size() + " exercises for workout: " + selectedWorkoutName);
                    } else {
                        Log.w("MainActivity", "Workout not found: " + selectedWorkoutName);
                    }
                    // Transition back to edit_workout
                    switchSheet(
                            R.layout.edit_workout,
                            selectedWorkoutName,
                            true,
                            R.anim.slide_out_right,
                            R.anim.slide_in_left
                    );
                    Log.d("MainActivity", "Add exercises button clicked, returned to edit_workout");
                });
                Log.d("MainActivity", "Initialized add_exercises_btn: enabled=" + hasSelection + ", selected exercises: " + selectedExercises.size());
            } else {
                Log.w("MainActivity", "add_exercises_btn not found in add_exercises");
            }

            if (closeButton != null) {
                closeButton.setOnClickListener(v -> backSheetAddExercises(v));
            } else {
                Log.w("MainActivity", "closeButton not found in add_exercises");
            }
        } else if (layoutId == R.layout.edit_exercises) {
            TextView title = newView.findViewById(R.id.title);
            RecyclerView setsList = newView.findViewById(R.id.sets_list);
            FrameLayout addSet = newView.findViewById(R.id.add_set);
            FrameLayout closeButton = newView.findViewById(R.id.close);
            ExercisesAdapter.Exercise currentExercise = selectedExercises.stream()
                    .filter(e -> e.getName().equals(data))
                    .findFirst()
                    .orElse(null);

            if (title != null && currentExercise != null) {
                title.setText(currentExercise.getName());
                Log.d("MainActivity", "Заголовок упражнения установлен: " + currentExercise.getName());
            } else {
                Log.w("MainActivity", "title или currentExercise null в edit_exercises");
            }

            if (setsList != null && currentExercise != null) {
                setsList.setLayoutManager(new LinearLayoutManager(this));
                Workout workout = workouts.stream()
                        .filter(w -> w.name.equals(selectedWorkoutName))
                        .findFirst()
                        .orElse(null);
                List<Set> sets = new ArrayList<>();
                if (workout != null) {
                    List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(currentExercise.getId(), k -> new ArrayList<>());
                    currentExercise.getSets().clear();
                    currentExercise.getSets().addAll(workoutSets);
                    sets.addAll(workoutSets);
                    if (sets.isEmpty()) {
                        String setId = UUID.randomUUID().toString();
                        Set initialSet = new Set(setId, 0.0f, 0);
                        currentExercise.addSet(initialSet);
                        workoutSets.add(initialSet);
                        sets.add(initialSet);
                        saveWorkouts();
                        Log.d("MainActivity", "Добавлен начальный подход для упражнения: " + currentExercise.getName());
                    }
                }
                SelectedSetsAdapter setsAdapter = new SelectedSetsAdapter(sets, set -> {
                    if (!set.getId().startsWith("placeholder_")) {
                        removeSet(set);
                        setsList.setVisibility(sets.isEmpty() ? View.GONE : View.VISIBLE);
                        Log.d("MainActivity", "Подход удалён через адаптер: " + set.getId());
                    }
                }, this);
                setsList.setAdapter(setsAdapter);
                setsList.setVisibility(sets.isEmpty() ? View.GONE : View.VISIBLE);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) addSet.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                params.height = dpToPx(70);
                params.setMargins(dpToPx(10), dpToPx(10), dpToPx(15), 0);
                addSet.setLayoutParams(params);
                Log.d("MainActivity", "Список подходов инициализирован с " + sets.size() + " подходами");
            } else {
                Log.w("MainActivity", "sets_list или currentExercise null в edit_exercises");
            }

            if (addSet != null && currentExercise != null) {
                addSet.setOnClickListener(v -> {
                    String setId = UUID.randomUUID().toString();
                    Set newSet = new Set(setId, 0.0f, 0);
                    currentExercise.addSet(newSet);
                    Workout workout = workouts.stream()
                            .filter(w -> w.name.equals(selectedWorkoutName))
                            .findFirst()
                            .orElse(null);
                    if (workout != null) {
                        List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(currentExercise.getId(), k -> new ArrayList<>());
                        workoutSets.add(newSet);
                        saveWorkouts();
                    }
                    if (setsList.getAdapter() != null) {
                        ((SelectedSetsAdapter) setsList.getAdapter()).updateSets(new ArrayList<>(currentExercise.getSets()));
                    }
                    setsList.setVisibility(View.VISIBLE);
                    Log.d("MainActivity", "Добавлен новый подход: " + setId);
                });
            } else {
                Log.w("MainActivity", "add_set или currentExercise null в edit_exercises");
            }

            if (closeButton != null) {
                closeButton.setOnClickListener(v -> backSheetEditExercises(v));
            } else {
                Log.w("MainActivity", "closeButton не найден в edit_exercises");
            }
        } else if (layoutId == R.layout.exercises_description) {
            View close = newView.findViewById(R.id.close);
            if (close != null) {
                close.setOnClickListener(v -> backSheetExerciseDescription(v));
            }
        }


        if (useHorizontalTransition) {
            widgetSheet.showWithHorizontalTransition(exitAnim, enterAnim, newView, null);
        } else {
            if (widgetSheet != null) {
                BottomSheets oldSheet = widgetSheet;
                oldSheet.hide(() -> {
                    widgetSheet = new BottomSheets(this);
                    widgetSheet.setContentView(newView);
                    widgetSheet.show();
                });
            } else {
                widgetSheet = new BottomSheets(this);
                widgetSheet.setContentView(newView);
                widgetSheet.show();
            }
        }
    }

    public void openAddExercises(View view) {
        switchSheet(R.layout.add_exercises, selectedWorkoutName, true, R.anim.slide_out_left, R.anim.slide_in_right);
        Log.d("MainActivity", "Opened add_exercises sheet");
    }

    public void backSheetAddExercises(View view) {
        // Save exercises to Workout
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(selectedWorkoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            workout.exerciseIds.clear();
            for (ExercisesAdapter.Exercise exercise : selectedExercises) {
                workout.exerciseIds.add(exercise.getId());
            }
            saveWorkouts();
            selectedExercisesAdapter.updateExercises(selectedExercises);
            Log.d("MainActivity", "Saved " + workout.exerciseIds.size() + " exercises for workout: " + selectedWorkoutName);
        } else {
            Log.w("MainActivity", "Workout not found: " + selectedWorkoutName);
        }
        switchSheet(
                R.layout.edit_workout,
                selectedWorkoutName,
                true,
                R.anim.slide_out_right,
                R.anim.slide_in_left
        );
        Log.d("MainActivity", "Returned to edit_workout with slide-in-left animation");
    }
    public void onExercisesSelected(List<ExercisesAdapter.Exercise> exercises) {
        if (exercises == null) {
            Log.w("MainActivity", "onExercisesSelected: exercises list is null");
            return;
        }
        selectedExercises.clear();
        selectedExercises.addAll(exercises);
        Log.d("MainActivity", "onExercisesSelected: Updated selectedExercises, size=" + selectedExercises.size() +
                ", exercises=" + selectedExercises.stream().map(ExercisesAdapter.Exercise::getName).collect(Collectors.joining(", ")));

        selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(selectedWorkoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            workout.exerciseIds.clear();
            for (ExercisesAdapter.Exercise exercise : selectedExercises) {
                workout.exerciseIds.add(exercise.getId());
            }
            saveWorkouts();
            Log.d("MainActivity", "Updated workout: " + selectedWorkoutName + " with " + workout.exerciseIds.size() + " exercises");
        } else {
            Log.w("MainActivity", "Workout not found: " + selectedWorkoutName);
        }
        // Update button state in add_exercises
        if (widgetSheet != null && widgetSheet.getContentView() != null) {
            TextView addExercisesBtn = widgetSheet.getContentView().findViewById(R.id.add_exercises_btn);
            if (addExercisesBtn != null) {
                boolean hasSelection = !selectedExercises.isEmpty();
                addExercisesBtn.setEnabled(hasSelection);
                addExercisesBtn.getBackground().setTint(hasSelection ? COLOR_SELECTED : COLOR_UNSELECTED);
                Log.d("MainActivity", "Updated add_exercises_btn state: enabled=" + hasSelection + ", selected exercises: " + selectedExercises.size());
            } else {
                Log.w("MainActivity", "add_exercises_btn not found in widgetSheet");
            }
        }
    }
    public static String getSetWordForm(int count) {
        if (count % 10 == 1 && count % 100 != 11) {
            return "подход";
        } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
            return "подхода";
        } else {
            return "подходов";
        }
    }
    public void editSets(ExercisesAdapter.Exercise exercise) {
        switchSheet(R.layout.edit_exercises, exercise.getName(), true, R.anim.slide_out_left, R.anim.slide_in_right);
    }
    public void removeSet(Set set) {
        ExercisesAdapter.Exercise exercise = selectedExercises.stream()
                .filter(e -> e.getSets().contains(set))
                .findFirst()
                .orElse(null);
        if (exercise != null) {
            exercise.removeSet(set);
            Workout workout = workouts.stream()
                    .filter(w -> w.name.equals(selectedWorkoutName))
                    .findFirst()
                    .orElse(null);
            if (workout != null) {
                List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(exercise.getId(), k -> new ArrayList<>());
                workoutSets.removeIf(s -> s.getId().equals(set.getId()));
                if (workoutSets.isEmpty()) {
                    workout.exerciseSets.remove(exercise.getId());
                }
                saveWorkouts();
                Log.d("MainActivity", "Подход удалён окончательно: id=" + set.getId() + ", упражнение=" + exercise.getName());
            } else {
                Log.w("MainActivity", "Тренировка не найдена: " + selectedWorkoutName);
            }
        } else {
            Log.w("MainActivity", "Упражнение не найдено для подхода: " + set.getId());
        }
    }
    public void saveSetToWorkout(Set set) {
        ExercisesAdapter.Exercise exercise = selectedExercises.stream()
                .filter(e -> e.getSets().stream().anyMatch(s -> s.getId().equals(set.getId())))
                .findFirst()
                .orElse(null);
        if (exercise != null) {
            Workout workout = workouts.stream()
                    .filter(w -> w.name.equals(selectedWorkoutName))
                    .findFirst()
                    .orElse(null);
            if (workout != null) {
                List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(exercise.getId(), k -> new ArrayList<>());
                workoutSets.removeIf(s -> s.getId().equals(set.getId()));
                workoutSets.add(new Set(set.getId(), set.getWeight(), set.getReps()));
                exercise.getSets().clear();
                exercise.getSets().addAll(workoutSets);
                saveWorkouts();
                Log.d("MainActivity", "Подход сохранён: id=" + set.getId() + ", вес=" + set.getWeight() + ", повторения=" + set.getReps());
            } else {
                Log.w("MainActivity", "Тренировка не найдена: " + selectedWorkoutName);
            }
        } else {
            Log.w("MainActivity", "Упражнение не найдено для подхода: " + set.getId());
        }
    }
    public void backSheetEditExercises(View view) {
        switchSheet(
                R.layout.edit_workout,
                selectedWorkoutName,
                true,
                R.anim.slide_out_right,
                R.anim.slide_in_left
        );
        Log.d("MainActivity", "Returned to edit_workout from edit_exercises");
    }
    public void saveWorkoutData() {
        saveWorkouts();
        Log.d("MainActivity", "Данные тренировок сохранены через saveWorkoutData");
    }
    public void openExerciseDescription(ExercisesAdapter.Exercise exercise) {
        setCurrentExerciseName(exercise.getName()); // Устанавливаем имя упражнения
        switchSheet(R.layout.exercises_description, exercise.getName(), true, R.anim.slide_out_left, R.anim.slide_in_right);
        Log.d("MainActivity", "Opened exercise description for: " + exercise.getName());
    }
    public void backSheetExerciseDescription(View view) {
        switchSheet(
                R.layout.add_exercises,
                selectedWorkoutName,
                true,
                R.anim.slide_out_right,
                R.anim.slide_in_left
        );
        Log.d("MainActivity", "Returned to add_exercises from exercise_description");
    }
}