package com.example.logster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
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
import android.graphics.drawable.TransitionDrawable;
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
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
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
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainActivity extends BaseActivity implements AddWorkout.WorkoutSelectionListener {

    private Workout currentWorkout;
    private List<ExercisesAdapter.Exercise> selectedExercises;
    private BottomNavigationManager navManager;
    private BottomSheets widgetSheet;
    private String selectedWorkoutName;
    private List<String> selectedDays;
    public List<Folder> folders;
    public List<Workout> workouts;
    public List<BodyMetric> bodyMetrics;
    private RecyclerView workoutRecyclerView;
    public CombinedAdapter combinedAdapter;
    private SharedPreferences sharedPreferences;
    private boolean isInFolder = false;
    private List<Object> mainItems;
    private FrameLayout backButton;
    private TextView titleTextView;
    public String currentFolderName = null;
    private Map<DayOfWeek, String> weeklySchedule;
    private Map<String, List<String>> specificDates;
    private SelectedExercisesAdapter selectedExercisesAdapter;
    private static final int COLOR_UNSELECTED = 0xFF606062;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WORKOUTS_KEY = "workouts";
    private static final String METRICS_KEY = "key_metrics_body";
    private static final String FOLDERS_KEY = "folders";
    private static final String WEEKLY_SCHEDULE_KEY = "weekly_schedule";
    private static final String SPECIFIC_DATES_KEY = "specific_dates";
    private String selectedDate;
    private ExercisesAdapter exercisesAdapter;
    private PersonalWorkoutData personalWorkoutData;
    private ConfirmationBottomSheet confirmationSheet;
    private Map<String, Long> workoutCountCache = new HashMap<>();
    public abstract class WorkoutProgramItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EXERCISE = 1;

        public abstract int getViewType();
    }

    public class DayHeader extends WorkoutProgramItem {
        private final String dayName;
        private final String workoutName;

        public DayHeader(String dayName, String workoutName) {
            this.dayName = dayName;
            this.workoutName = workoutName;
        }

        public String getDayName() {
            return dayName;
        }

        public String getWorkoutName() {
            return workoutName;
        }

        @Override
        public int getViewType() {
            return TYPE_HEADER;
        }
    }

    public class WorkoutExerciseItem extends WorkoutProgramItem {
        private final WorkoutProgramGenerator.WorkoutExercise exercise;

        public WorkoutExerciseItem(WorkoutProgramGenerator.WorkoutExercise exercise) {
            this.exercise = exercise;
        }

        public WorkoutProgramGenerator.WorkoutExercise getExercise() {
            return exercise;
        }

        @Override
        public int getViewType() {
            return TYPE_EXERCISE;
        }
    }
    public List<ExercisesAdapter.Exercise> getSelectedExercises() {
        if (selectedExercises == null) {
            selectedExercises = new ArrayList<>();
        }
        return selectedExercises;
    }
    private String currentExerciseName;
    public String getSelectedWorkoutName() {
        return selectedWorkoutName;
    }
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

        // Initialize navigation and other components
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

        // Initialize adapters and recycler view
        selectedExercisesAdapter = new SelectedExercisesAdapter(this, new SelectedExercisesAdapter.OnExerciseRemovedListener() {
            @Override
            public void onExerciseRemoved(ExercisesAdapter.Exercise exercise) {
                selectedExercises.remove(exercise);
                exercise.getSets().clear();
                exercise.setSelected(false);
                selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
                Workout workout = workouts.stream()
                        .filter(w -> w.name.equals(selectedWorkoutName))
                        .findFirst()
                        .orElse(null);
                if (workout != null) {
                    workout.exerciseIds.remove(Integer.valueOf(exercise.getId()));
                    workout.exerciseSets.remove(exercise.getId());
                    saveWorkouts();
                    Log.d("MainActivity", "Упражнение удалено: " + exercise.getName() + ", id=" + exercise.getId());
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
        confirmationSheet = new ConfirmationBottomSheet(this);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            private boolean isOverFolder = false;
            private Folder targetFolder = null;
            private RecyclerView.ViewHolder targetHolder = null;
            private boolean isSwiping = false;

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

                // Обработка нахождения над папкой
                if (!isInFolder && targetItem instanceof Folder && !(source instanceof Folder)) {
                    if (targetFolder != targetItem) {
                        resetFolderHighlight();
                        targetFolder = (Folder) targetItem;
                        targetHolder = target;
                        animateStroke(targetHolder, "#333339", "#727275");
                        Log.d("ItemTouchHelper", "Над папкой: " + targetFolder.name);
                    }
                    isOverFolder = true;
                    return false; // Не перемещаем, если над папкой
                } else {
                    if (isOverFolder) {
                        resetFolderHighlight();
                        isOverFolder = false;
                        Log.d("ItemTouchHelper", "Выход из зоны папки");
                    }
                }

                // Перемещение
                if (!(targetItem instanceof Folder) || source instanceof Folder) {
                    Collections.swap(combinedAdapter.items, fromPos, toPos);
                    recyclerView.post(() -> {
                        combinedAdapter.notifyItemMoved(fromPos, toPos);
                        Log.d("ItemTouchHelper", "Перемещено: from=" + fromPos + ", to=" + toPos);
                    });
                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Log.d("ItemTouchHelper", "onSwiped: pos=" + pos + ", direction=" + direction);

                if (pos == RecyclerView.NO_POSITION || pos >= combinedAdapter.items.size()) {
                    Log.e("ItemTouchHelper", "Невалидная позиция свайпа: " + pos);
                    return;
                }

                if (isSwiping) {
                    Log.w("ItemTouchHelper", "Свайп игнорируется, другой свайп в процессе: pos=" + pos);
                    combinedAdapter.notifyItemChanged(pos);
                    return;
                }
                isSwiping = true;

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
                            String itemName = null;
                            String itemType = null;
                            if (item instanceof Workout) {
                                itemId = ((Workout) item).id;
                                itemName = ((Workout) item).name;
                                itemType = "Workout";
                            } else if (item instanceof BodyMetric) {
                                itemId = ((BodyMetric) item).id;
                                itemName = ((BodyMetric) item).type;
                                itemType = "BodyMetric";
                            }
                            if (itemId != null) {
                                final String finalItemId = itemId;
                                confirmationSheet.show(
                                        "Удалить",
                                        itemName,
                                        itemType,
                                        itemId,
                                        () -> {
                                            folder.itemIds.remove(finalItemId);
                                            combinedAdapter.items.remove(pos);
                                            combinedAdapter.notifyItemRemoved(pos);
                                            int folderPos = combinedAdapter.getFolderPosition(folder.name);
                                            if (folderPos != -1) {
                                                combinedAdapter.notifyItemChanged(folderPos);
                                            }
                                            workoutCountCache.clear();
                                            updateMainItems();
                                            combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                                            saveItems();
                                            isSwiping = false;
                                            Log.d("ItemTouchHelper", "Удалено из папки: ID=" + finalItemId + ", папка=" + currentFolderName);
                                        },
                                        () -> {
                                            Log.d("ItemTouchHelper", "Отменено действие: ID=" + finalItemId);
                                            viewHolder.itemView.postDelayed(() -> {
                                                combinedAdapter.notifyItemChanged(pos);
                                                isSwiping = false;
                                            }, 100); // Задержка для синхронизации анимации
                                        }
                                );
                            } else {
                                viewHolder.itemView.postDelayed(() -> {
                                    combinedAdapter.notifyItemChanged(pos);
                                    isSwiping = false;
                                }, 100);
                            }
                        } else {
                            Log.w("ItemTouchHelper", "Папка не найдена: " + currentFolderName);
                            viewHolder.itemView.postDelayed(() -> {
                                combinedAdapter.notifyItemChanged(pos);
                                isSwiping = false;
                            }, 100);
                        }
                    } else {
                        if (item instanceof Folder) {
                            Folder folder = (Folder) item;
                            confirmationSheet.show(
                                    "Удалить",
                                    folder.name,
                                    "Folder",
                                    folder.id,
                                    () -> {
                                        folders.remove(folder);
                                        combinedAdapter.items.remove(pos);
                                        combinedAdapter.notifyItemRemoved(pos);
                                        updateMainItems();
                                        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                                        saveItems();
                                        isSwiping = false;
                                        Log.d("ItemTouchHelper", "Папка удалена: " + folder.name);
                                    },
                                    () -> {
                                        Log.d("ItemTouchHelper", "Отменено действие: " + folder.name);
                                        viewHolder.itemView.postDelayed(() -> {
                                            combinedAdapter.notifyItemChanged(pos);
                                            isSwiping = false;
                                        }, 100);
                                    }
                            );
                        } else if (item instanceof Workout) {
                            Workout workout = (Workout) item;
                            confirmationSheet.show(
                                    "Удалить",
                                    workout.name,
                                    "Workout",
                                    workout.id,
                                    () -> {
                                        workouts.remove(workout);
                                        removeWorkoutFromSchedules(workout.id);
                                        combinedAdapter.items.remove(pos);
                                        combinedAdapter.notifyItemRemoved(pos);
                                        workoutCountCache.clear();
                                        updateMainItems();
                                        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                                        saveItems();
                                        isSwiping = false;
                                        Log.d("ItemTouchHelper", "Тренировка удалена: " + workout.name);
                                    },
                                    () -> {
                                        Log.d("ItemTouchHelper", "Отменено действие: " + workout.name);
                                        viewHolder.itemView.postDelayed(() -> {
                                            combinedAdapter.notifyItemChanged(pos);
                                            isSwiping = false;
                                        }, 100);
                                    }
                            );
                        } else if (item instanceof BodyMetric) {
                            BodyMetric metric = (BodyMetric) item;
                            confirmationSheet.show(
                                    "Удалить",
                                    metric.type,
                                    "BodyMetric",
                                    metric.id,
                                    () -> {
                                        bodyMetrics.remove(metric);
                                        combinedAdapter.items.remove(pos);
                                        combinedAdapter.notifyItemRemoved(pos);
                                        updateMainItems();
                                        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                                        saveItems();
                                        isSwiping = false;
                                        Log.d("ItemTouchHelper", "Метрика удалена: " + metric.type);
                                    },
                                    () -> {
                                        Log.d("ItemTouchHelper", "Отменено действие: " + metric.type);
                                        viewHolder.itemView.postDelayed(() -> {
                                            combinedAdapter.notifyItemChanged(pos);
                                            isSwiping = false;
                                        }, 100);
                                    }
                            );
                        }
                    }
                } catch (Exception e) {
                    Log.e("ItemTouchHelper", "Ошибка свайпа: " + e.getMessage(), e);
                    viewHolder.itemView.postDelayed(() -> {
                        combinedAdapter.notifyItemChanged(pos);
                        isSwiping = false;
                    }, 100);
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

                Object item = combinedAdapter.items.get(pos);
                boolean isFolder = item instanceof Folder;

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Анимация для всех элементов, включая папки
                    viewHolder.itemView.animate()
                            .scaleX(1.03f)
                            .scaleY(1.03f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    viewHolder.itemView.setElevation(4f);
                    if (item instanceof Workout || item instanceof BodyMetric || isFolder) {
                        animateStroke(viewHolder, "#333339", "#727275");
                    }
                    Log.d("ItemTouchHelper", "Перетаскивание начато: pos=" + pos + ", тип=" + item.getClass().getSimpleName());
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    // Плавный возврат для всех элементов, но только если не над папкой для не-папок
                    if (!isOverFolder || isFolder) {
                        viewHolder.itemView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                        viewHolder.itemView.setElevation(0f);
                        if (item instanceof Workout || item instanceof BodyMetric || isFolder) {
                            animateStroke(viewHolder, "#727275", "#333339");
                        }
                        Log.d("ItemTouchHelper", "Перетаскивание завершено: pos=" + pos + ", тип=" + item.getClass().getSimpleName());
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                int pos = viewHolder.getAdapterPosition();
                Log.d("ItemTouchHelper", "clearView: pos=" + pos + ", itemsSize=" + combinedAdapter.items.size());

                // Проверка на невалидную позицию
                if (pos == RecyclerView.NO_POSITION || pos >= combinedAdapter.items.size()) {
                    Log.w("ItemTouchHelper", "Невалидная позиция в clearView: pos=" + pos);
                    resetFolderHighlight();
                    isOverFolder = false;
                    return;
                }

                Object item = combinedAdapter.items.get(pos);
                boolean isFolder = item instanceof Folder;

                // Пропускаем анимацию возврата для папок, если анимация RecyclerView активна
                if (!isOverFolder || isFolder) {
                    if (isFolder && recyclerView.getItemAnimator() != null && recyclerView.getItemAnimator().isRunning()) {
                        Log.d("ItemTouchHelper", "Пропуск анимации возврата для папки, анимация активна: pos=" + pos);
                        return;
                    }
                    // Анимация возврата только если не выполняется другая анимация
                    viewHolder.itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .translationX(0f)
                            .translationY(0f)
                            .setDuration(300)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    viewHolder.itemView.setElevation(0f);
                    if (item instanceof Workout || item instanceof BodyMetric || isFolder) {
                        animateStroke(viewHolder, "#727275", "#333339");
                    }
                }

                // Сброс подсветки других элементов
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

                // Добавление в папку только для тренировок/метрик
                if (targetHolder != null && targetFolder != null && !isInFolder && !isFolder) {
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
                                        recyclerView.post(() -> {
                                            combinedAdapter.notifyItemRemoved(finalPos);
                                            int folderPos = combinedAdapter.getFolderPosition(finalTargetFolder.name);
                                            if (folderPos != -1) {
                                                combinedAdapter.notifyItemChanged(folderPos);
                                            }
                                            updateMainItems();
                                            saveItems();
                                        });
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
                            int folderPos = combinedAdapter.getFolderPosition(folder.name);
                            if (folderPos != -1) {
                                recyclerView.post(() -> combinedAdapter.notifyItemChanged(folderPos));
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

// Настройка анимации RecyclerView
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(300);
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

        workouts = loadWorkouts();
        bodyMetrics = loadBodyMetrics();
        folders = loadFolders(); // Загружаем папки после тренировок и метрик
        loadWeeklySchedule();
        loadSpecificDates();
        syncWorkoutDates();
        updateMainItems(); // Обновляем mainItems после загрузки всех данных
        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);

        // Handle edit_workout intent
        Intent intent = getIntent();
        handleEditWorkoutIntent(intent);

        hideSystemUI();
    }

    private void handleEditWorkoutIntent(Intent intent) {
        if (intent != null && intent.hasExtra("edit_workout")) {
            String workoutName = intent.getStringExtra("edit_workout");
            selectedDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE); // Всегда текущая дата
            Log.d("MainActivity", "handleEditWorkoutIntent: workoutName=" + workoutName + ", selectedDate=" + selectedDate);
            if (workoutName != null && !workoutName.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    editWorkout(workoutName);
                    Log.d("MainActivity", "Обработка edit_workout: " + workoutName + ", дата: " + selectedDate);
                });
            } else {
                Log.w("MainActivity", "Пустое имя тренировки в extra edit_workout");
            }
        }
    }

    public void updateMainItems() {
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
        // Удаляем workoutId из списков в specificDates
        for (List<String> workoutIds : specificDates.values()) {
            workoutIds.remove(workoutId);
        }
        // Удаляем пустые списки
        specificDates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        // Удаляем из weeklySchedule (для обратной совместимости)
        weeklySchedule.entrySet().removeIf(entry -> entry.getValue().equals(workoutId));
        saveWeeklySchedule();
        saveSpecificDates();
        Log.d("MainActivity", "Тренировка удалена из расписания: " + workoutId);
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
        setIntent(intent); // Update the intent
        navManager.forceSetActiveButton("MainActivity");
        handleEditWorkoutIntent(intent);
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

        // Weekly schedule
        if (weeklySchedule != null) {
            for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
                DayOfWeek dayOfWeek = entry.getKey();
                String workoutId = entry.getValue();
                if (dayOfWeek == null || workoutId == null || workoutId.isEmpty()) continue;
                final String dayName = dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"))
                        .substring(0, 1).toUpperCase() + dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                workouts.stream()
                        .filter(w -> w.id.equals(workoutId))
                        .findFirst()
                        .ifPresent(workout -> workoutCountByDay.computeIfAbsent(dayName, k -> new ArrayList<>()).add(workout.name));
            }
        }

        // Specific dates
        if (specificDates != null) {
            for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
                String dateStr = entry.getKey();
                List<String> workoutIds = entry.getValue();
                if (dateStr == null || workoutIds == null || workoutIds.isEmpty()) continue;
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    if (!date.isBefore(today)) {
                        final String dayName = date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"))
                                .substring(0, 1).toUpperCase() + date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                        for (String workoutId : workoutIds) {
                            workouts.stream()
                                    .filter(w -> w.id.equals(workoutId))
                                    .findFirst()
                                    .ifPresent(workout -> workoutCountByDay.computeIfAbsent(dayName, k -> new ArrayList<>()).add(workout.name));
                        }
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


    public String getAllWorkoutDays(String workoutId, List<String> dates) {
        java.util.Set<String> workoutDays = new java.util.HashSet<>();

        // Собираем дни недели из weeklySchedule
        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            if (entry.getValue().equals(workoutId)) {
                String dayName = entry.getKey().getDisplayName(TextStyle.FULL, new Locale("ru"))
                        .substring(0, 1).toUpperCase() +
                        entry.getKey().getDisplayName(TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                workoutDays.add(dayName);
            }
        }

        // Собираем дни недели из specificDates
        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            List<String> workoutIds = entry.getValue();
            if (workoutIds.contains(workoutId)) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"))
                            .substring(0, 1).toUpperCase() +
                            date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                    workoutDays.add(dayName);
                } catch (DateTimeParseException e) {
                    Log.e("MainActivity", "Ошибка разбора даты в getAllWorkoutDays: " + dateStr, e);
                }
            }
        }

        // Форматируем дни недели в строку
        List<String> dayNames = workoutDays.stream()
                .sorted((d1, d2) -> {
                    DayOfWeek dow1 = DAY_OF_WEEK_MAP.get(d1.toUpperCase());
                    DayOfWeek dow2 = DAY_OF_WEEK_MAP.get(d2.toUpperCase());
                    return dow1.getValue() - dow2.getValue();
                })
                .collect(Collectors.toList());

        String result = String.join(", ", dayNames);
        Log.d("MainActivity", "getAllWorkoutDays для тренировки ID " + workoutId + ": " + result);
        return result.isEmpty() ? "Не выбрано" : result;
    }

    public void saveWorkouts() {
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

    public void saveFolders() {
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
                    // Сохраняем все itemIds без немедленной очистки
                    loadedFolders.add(folder);
                    Log.d("MainActivity", "Loaded folder: " + folder.name + ", itemIds=" + folder.itemIds.size());
                }
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки папок: " + e.getMessage(), e);
            }
        }
        this.folders = loadedFolders;

        // Очистка невалидных itemIds после загрузки workouts и bodyMetrics
        for (Folder folder : loadedFolders) {
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
        }

        saveFolders(); // Сохраняем после очистки
        Log.d("MainActivity", "Loaded and cleaned folders: " + loadedFolders.size());
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
        } else {
            // Миграция для старых данных
            for (Workout workout : workouts) {
                java.util.Set<DayOfWeek> days = new java.util.HashSet<>();
                for (String dateStr : workout.dates) {
                    try {
                        LocalDate date = LocalDate.parse(dateStr);
                        days.add(date.getDayOfWeek());
                    } catch (Exception e) {
                        Log.e("MainActivity", "Ошибка миграции даты: " + dateStr, e);
                    }
                }
                for (DayOfWeek day : days) {
                    weeklySchedule.put(day, workout.id);
                }
            }
            saveWeeklySchedule();
        }
    }

    private void saveSpecificDates() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject json = new JSONObject();
            for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
                JSONArray workoutIds = new JSONArray(entry.getValue());
                json.put(entry.getKey(), workoutIds);
            }
            editor.putString(SPECIFIC_DATES_KEY, json.toString());
            editor.apply();
            Log.d("MainActivity", "Сохранены конкретные даты: " + json.toString());
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
                    try {
                        LocalDate.parse(key); // Проверяем валидность даты
                        JSONArray workoutIdsJson = json.getJSONArray(key);
                        List<String> workoutIds = new ArrayList<>();
                        for (int i = 0; i < workoutIdsJson.length(); i++) {
                            String workoutId = workoutIdsJson.getString(i);
                            // Проверяем, существует ли тренировка
                            if (workouts.stream().anyMatch(w -> w.id.equals(workoutId))) {
                                workoutIds.add(workoutId);
                            } else {
                                Log.w("MainActivity", "Пропущен невалидный workoutId: " + workoutId + " для даты: " + key);
                            }
                        }
                        if (!workoutIds.isEmpty()) {
                            specificDates.put(key, workoutIds);
                        }
                    } catch (DateTimeParseException e) {
                        Log.w("MainActivity", "Невалидная дата в specificDates: " + key + ", пропущена");
                    }
                }
                Log.d("MainActivity", "Загружены конкретные даты: " + specificDates.size());
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки дат: " + e.getMessage(), e);
            }
        }
    }

    private void saveItems() {
        saveWorkouts();
        saveBodyMetrics();
        saveFolders(); // Сохраняем папки после тренировок и метрик
        saveWeeklySchedule();
        saveSpecificDates();
        Log.d("MainActivity", "Saved all items: workouts=" + workouts.size() + ", bodyMetrics=" + bodyMetrics.size() + ", folders=" + folders.size());
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        // Очищаем workout.dates, чтобы пересинхронизировать
        for (Workout workout : workouts) {
            workout.dates.clear();
        }

        // Добавляем даты из specificDates (только записанные тренировки)
        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            List<String> workoutIds = entry.getValue();
            try {
                LocalDate date = LocalDate.parse(dateStr);
                for (String workoutId : workoutIds) {
                    workouts.stream()
                            .filter(w -> w.id.equals(workoutId))
                            .findFirst()
                            .ifPresent(workout -> workout.addDate(dateStr));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Ошибка разбора даты: " + dateStr, e);
            }
        }

        // Добавляем запланированные даты из weeklySchedule в workout.dates (но не в specificDates)
        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;
            LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                workouts.stream()
                        .filter(w -> w.id.equals(workoutId))
                        .findFirst()
                        .ifPresent(workout -> workout.addDate(dateStr));
                currentDate = currentDate.plusWeeks(1);
            }
        }

        saveWorkouts();
        // saveSpecificDates() не вызываем, так как specificDates не изменился
        Log.d("MainActivity", "Синхронизированы даты тренировок");
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
        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(() -> {
                widgetSheet = new BottomSheets(this, R.layout.widgets);
                widgetSheet.show();
                Log.d("MainActivity", "Back to widgets sheet from backSheet");
            });
        } else {
            Log.w("MainActivity", "widgetSheet is null or not showing in backSheet");
        }
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

    protected int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void switchSheet(int layoutId, Object data, boolean useHorizontalTransition, int exitAnim, int enterAnim) {
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
                            widgetSheet.hide(() -> {
                                continueBtn.setEnabled(true);
                                // Убрано повторное открытие widgets
                                Log.d("MainActivity", "Папка создана и лист закрыт: " + folderName);
                            });
                        } else {
                            Toast.makeText(this, "Папка уже существует", Toast.LENGTH_SHORT).show();
                            continueBtn.setEnabled(true);
                        }
                    } else {
                        Toast.makeText(this, "Введите название папки", Toast.LENGTH_SHORT).show();
                        continueBtn.setEnabled(true);
                    }
                });
            }
        } else if (layoutId == R.layout.add_workout) {
            ScrollView scrollView = newView.findViewById(R.id.trainingScrollView);
            LinearLayout trainingList = newView.findViewById(R.id.trainingList);
            View topLine = newView.findViewById(R.id.top_line);
            View bottomLine = newView.findViewById(R.id.bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);
            TextView workoutNameTextView = newView.findViewById(R.id.workout_name);
            TextView workoutDescriptionTextView = newView.findViewById(R.id.workout_description);
            View backBtn = newView.findViewById(R.id.back);

            if (scrollView != null && trainingList != null) {
                AddWorkout.setupScrollHighlight(
                        scrollView, trainingList, topLine, bottomLine,
                        centerArrow, workoutNameTextView, workoutDescriptionTextView);
            }
        } else if (layoutId == R.layout.add_workout2) {
            EditText workoutNameEditText = newView.findViewById(R.id.ETworkout_name);
            if (data != null && workoutNameEditText != null) {
                workoutNameEditText.setText((String) data);
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

            Map<String, Integer> workoutCountByDay = getUniqueWorkoutCountByDay();
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
                Integer workoutCount = workoutCountByDay.getOrDefault(dayName, 0);

                if (countTextView != null) {
                    countTextView.setText(String.valueOf(workoutCount));
                }

                if (dayTextView != null) {
                    dayTextView.setText(dayName);
                    DayOfWeek dayOfWeek = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
                    String existingWorkoutId = weeklySchedule.get(dayOfWeek);
                    boolean isSelected = existingWorkoutId != null && workouts.stream()
                            .filter(w -> w.id.equals(existingWorkoutId))
                            .anyMatch(w -> w.name.equals(selectedWorkoutName));
                    dayTextView.setTag(Boolean.valueOf(isSelected)); // Explicitly use Boolean object
                    dayTextView.getBackground().setTint(isSelected ? COLOR_SELECTED : COLOR_UNSELECTED);

                    boolean hasWorkouts = workoutCount > 0;
                    if (countTextView != null) {
                        countTextView.setTag(Boolean.valueOf(hasWorkouts)); // Explicitly use Boolean object
                        countTextView.getBackground().setTint(hasWorkouts ? COLOR_SELECTED : COLOR_UNSELECTED);
                        countTextView.setEnabled(hasWorkouts);
                    }

                    dayTextView.setOnClickListener(v -> {
                        Boolean currentState = (Boolean) dayTextView.getTag(); // Safe cast to Boolean
                        if (currentState == null) currentState = false; // Fallback if tag is null
                        currentState = !currentState;
                        dayTextView.setTag(Boolean.valueOf(currentState));

                        int startColor = currentState ? COLOR_UNSELECTED : COLOR_SELECTED;
                        int endColor = currentState ? COLOR_SELECTED : COLOR_UNSELECTED;
                        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
                        colorAnimator.setDuration(300);
                        colorAnimator.addUpdateListener(animator -> dayTextView.getBackground().setTint((int) animator.getAnimatedValue()));
                        colorAnimator.start();

                        String day = dayTextView.getText().toString();
                        if (currentState) {
                            selectedDays.add(day);
                        } else {
                            selectedDays.remove(day);
                        }
                    });

                    if (countTextView != null) {
                        countTextView.setOnClickListener(v -> {
                            Boolean hasWorkoutsTag = (Boolean) countTextView.getTag(); // Safe cast to Boolean
                            if (hasWorkoutsTag != null && hasWorkoutsTag) {
                                List<String> workoutsForDay = getWorkoutsForDay(dayName);
                                StringBuilder message = new StringBuilder("На этот день записаны " + workoutCount + " тренировок: ");
                                message.append(String.join(", ", workoutsForDay));
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
                        String workoutId = workouts.stream()
                                .filter(w -> w.name.equals(selectedWorkoutName))
                                .findFirst()
                                .map(w -> w.id)
                                .orElse(UUID.randomUUID().toString());

                        if (workouts.stream().noneMatch(w -> w.id.equals(workoutId))) {
                            Workout newWorkout = new Workout(workoutId, selectedWorkoutName);
                            workouts.add(newWorkout);
                        }

                        // Добавляем тренировку в specificDates для выбранных дней
                        LocalDate today = LocalDate.now();
                        LocalDate endDate = today.plusYears(1);
                        for (String dayName : selectedDays) {
                            DayOfWeek dayOfWeek = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
                            if (dayOfWeek != null) {
                                LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                                while (!currentDate.isAfter(endDate)) {
                                    String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                                    List<String> workoutIds = specificDates.getOrDefault(dateStr, new ArrayList<>());
                                    if (!workoutIds.contains(workoutId)) {
                                        workoutIds.add(workoutId);
                                        specificDates.put(dateStr, workoutIds);
                                        Log.d("MainActivity", "Добавлена тренировка " + workoutId + " на дату " + dateStr);
                                    }
                                    currentDate = currentDate.plusWeeks(1);
                                }
                            }
                        }

                        // Сохраняем данные
                        saveWorkouts();
                        saveSpecificDates();
                        syncWorkoutDates();

                        // Обновляем workoutCountByDay
                        Map<String, Integer> getworkoutCountByDay = getUniqueWorkoutCountByDay();
                        // Обновляем countTextView для всех дней
                        for (int i = 0; i < dayIds.length; i++) {
                            TextView countTextView = newView.findViewById(countIds[i]);
                            String dayName = dayNames[i];
                            Integer workoutCount = workoutCountByDay.getOrDefault(dayName, 0);
                            if (countTextView != null) {
                                countTextView.setText(String.valueOf(workoutCount));
                                boolean hasWorkouts = workoutCount > 0;
                                countTextView.setTag(Boolean.valueOf(hasWorkouts));
                                countTextView.getBackground().setTint(hasWorkouts ? COLOR_SELECTED : COLOR_UNSELECTED);
                                countTextView.setEnabled(hasWorkouts);
                            }
                        }

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
            View backBtn = newView.findViewById(R.id.back);

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
            TextView unitTextView = newView.findViewById(R.id.type_metric);

            if (data instanceof String) {
                com.example.logster.AddBodyMetric.setupNumberPicker(
                        mainScrollView, mainValueList, mainTopLine,
                        mainBottomLine, centerArrow, (String) data, unitTextView
                );
            } else {
                Log.e(TAG, "Data is not a String for add_metric2: " + data);
            }
        }  else if (layoutId == R.layout.edit_workout) {
            TextView titleTextView = newView.findViewById(R.id.title);
            RecyclerView selectedExercisesRecyclerView = newView.findViewById(R.id.selected_exercises_list);
            View addExercisesBtn = newView.findViewById(R.id.addExercises);
            FrameLayout closeBtn = newView.findViewById(R.id.close);
            View logBtn = newView.findViewById(R.id.log_btn); // Добавляем кнопку "записать"

            if (titleTextView != null && data instanceof String) {
                titleTextView.setText((String) data);
            }
            if (selectedExercisesRecyclerView != null) {
                selectedExercisesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                selectedExercisesAdapter.updateExercises(new ArrayList<>(selectedExercises));
                selectedExercisesRecyclerView.setAdapter(selectedExercisesAdapter);
                selectedExercisesRecyclerView.setVisibility(selectedExercises.isEmpty() ? View.GONE : View.VISIBLE);

                ExerciseDragHelper dragHelper = new ExerciseDragHelper(selectedExercisesAdapter,
                        selectedExercises, this, data instanceof String ? (String) data : null);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(dragHelper);
                itemTouchHelper.attachToRecyclerView(selectedExercisesRecyclerView);

                DefaultItemAnimator animator = new DefaultItemAnimator();
                animator.setMoveDuration(300);
                animator.setRemoveDuration(200);
                animator.setAddDuration(200);
                animator.setChangeDuration(300);
                selectedExercisesRecyclerView.setItemAnimator(animator);
            }

            if (addExercisesBtn != null) {
                addExercisesBtn.setOnClickListener(v -> openAddExercises(v));
            }

            if (closeBtn != null) {
                closeBtn.setOnClickListener(v -> backSheetEditWorkout(v));
            }

            if (logBtn != null) {
                logBtn.setOnClickListener(v -> logWorkout(v)); // Привязываем обработчик
            } else {
                Log.w("MainActivity", "log_btn не найден в edit_workout");
            }
        } else if (layoutId == R.layout.add_exercises) {

            RecyclerView exerciseRecyclerView = newView.findViewById(R.id.exercise_list);
            TextView emptyView = newView.findViewById(R.id.empty_view);
            FrameLayout closeButton = newView.findViewById(R.id.close);
            TextView addExercisesButton = newView.findViewById(R.id.add_exercises_btn);
            EditText searchBar = newView.findViewById(R.id.search_bar); // Находим EditText

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

                // Настройка поиска
                if (searchBar != null) {
                    searchBar.addTextChangedListener(new android.text.TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                        @Override
                        public void afterTextChanged(android.text.Editable s) {
                            String query = s.toString().trim();
                            exercisesAdapter.filterExercises(query);
                            if (emptyView != null) {
                                emptyView.setVisibility(exercisesAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                                exerciseRecyclerView.setVisibility(exercisesAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
                            }
                            Log.d("MainActivity", "Search query: " + query + ", results: " + exercisesAdapter.getItemCount());
                        }
                    });
                } else {
                    Log.w("MainActivity", "search_bar EditText is null in add_exercises");
                }

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

                // Подключение SetDragHelper для перетаскивания подходов
                SetDragHelper dragHelper = new SetDragHelper(setsAdapter, sets, this, currentExercise);
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(dragHelper);
                itemTouchHelper.attachToRecyclerView(setsList);
                Log.d("MainActivity", "SetDragHelper подключён к sets_list");

                // Анимация для плавного перетаскивания
                DefaultItemAnimator animator = new DefaultItemAnimator();
                animator.setMoveDuration(300);
                animator.setRemoveDuration(200);
                animator.setAddDuration(200);
                animator.setChangeDuration(300);
                setsList.setItemAnimator(animator);

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
                    // Сохраняем данные всех текущих подходов
                    SelectedSetsAdapter setsAdapter = (SelectedSetsAdapter) setsList.getAdapter();
                    if (setsAdapter != null) {
                        setsAdapter.saveAllSets();
                    }

                    // Добавляем новый подход
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
                    if (setsAdapter != null) {
                        setsAdapter.updateSets(new ArrayList<>(currentExercise.getSets()));
                        // Прокручиваем к новому подходу
                        setsList.scrollToPosition(setsAdapter.getItemCount() - 1);
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
        } else if (layoutId == R.layout.add_p_workout) {
            View muscleBtn = newView.findViewById(R.id.muscle);
            View slimBtn = newView.findViewById(R.id.slim);

            muscleBtn.setOnClickListener(v -> {
                animateButtonSelection(v, true);
                personalWorkoutData.setGoal("muscle");
                switchSheet(R.layout.add_p_workout2, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            });
            slimBtn.setOnClickListener(v -> {
                animateButtonSelection(v, true);
                personalWorkoutData.setGoal("slim");
                switchSheet(R.layout.add_p_workout2, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            });
        } else if (layoutId == R.layout.add_p_workout2) {
            View visibleMusclesBtn = newView.findViewById(R.id.visibleMuscules);
            View strongBtn = newView.findViewById(R.id.strong);
            View bigBtn = newView.findViewById(R.id.big);
            View leanerBtn = newView.findViewById(R.id.leaner);
            View backBtn = newView.findViewById(R.id.back);

            View.OnClickListener nextListener = v -> {
                animateButtonSelection(v, true);
                String motivation = v.getId() == R.id.visibleMuscules ? "visibleMuscles" :
                        v.getId() == R.id.strong ? "strong" :
                                v.getId() == R.id.big ? "big" : "leaner";
                personalWorkoutData.setMotivation(motivation);
                switchSheet(R.layout.add_p_workout3, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            };
            visibleMusclesBtn.setOnClickListener(nextListener);
            strongBtn.setOnClickListener(nextListener);
            bigBtn.setOnClickListener(nextListener);
            leanerBtn.setOnClickListener(nextListener);
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout3) {
            View beginnerBtn = newView.findViewById(R.id.beginner);
            View intermediateBtn = newView.findViewById(R.id.average);
            View advancedBtn = newView.findViewById(R.id.advanced);
            View backBtn = newView.findViewById(R.id.back);

            View.OnClickListener nextListener = v -> {
                animateButtonSelection(v, true);
                String level = v.getId() == R.id.beginner ? "beginner" :
                        v.getId() == R.id.average ? "intermediate" : "advanced";
                personalWorkoutData.setFitnessLevel(level);
                switchSheet(R.layout.add_p_workout4, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            };
            beginnerBtn.setOnClickListener(nextListener);
            intermediateBtn.setOnClickListener(nextListener);
            advancedBtn.setOnClickListener(nextListener);
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout2, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout4) {
            EditText heightEt = newView.findViewById(R.id.tell_et);
            View continueBtn = newView.findViewById(R.id.continue_p4);
            View backBtn = newView.findViewById(R.id.back);

            continueBtn.setOnClickListener(v -> {
                animateButtonSelection(v, true);
                String height = heightEt.getText().toString().trim();
                if (!height.isEmpty()) {
                    personalWorkoutData.setHeight(height);

                    switchSheet(R.layout.add_p_workout5, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
                    hideKeyboard();
                } else {
                    Toast.makeText(this, "Введите рост", Toast.LENGTH_SHORT).show();
                }
            });

            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout3, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            hideKeyboard();
        } else if (layoutId == R.layout.add_p_workout5) {
            EditText weightEt = newView.findViewById(R.id.weight_et);
            View continueBtn = newView.findViewById(R.id.continue_p5);
            View backBtn = newView.findViewById(R.id.back);

            continueBtn.setOnClickListener(v -> {
                animateButtonSelection(v, true);
                String weight = weightEt.getText().toString().trim();
                if (!weight.isEmpty()) {
                    personalWorkoutData.setWeight(weight);
                    switchSheet(R.layout.add_p_workout6, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
                } else {
                    Toast.makeText(this, "Введите вес", Toast.LENGTH_SHORT).show();
                }
            });
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout4, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout6) {
            EditText ageEt = newView.findViewById(R.id.weight_et); // Переименовать в XML на @id/age_et
            View continueBtn = newView.findViewById(R.id.continue_p5); // Переименовать в XML на @id/continue_p6
            View backBtn = newView.findViewById(R.id.back);

            continueBtn.setOnClickListener(v -> {
                animateButtonSelection(v, true);
                String age = ageEt.getText().toString().trim();
                if (!age.isEmpty()) {
                    personalWorkoutData.setAge(age);
                    switchSheet(R.layout.add_p_workout7, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
                } else {
                    Toast.makeText(this, "Введите возраст", Toast.LENGTH_SHORT).show();
                }
            });
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout5, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout7) {
            View armsBtn = newView.findViewById(R.id.arms);
            View backPBtn = newView.findViewById(R.id.back_p);
            View chestBtn = newView.findViewById(R.id.chest);
            View legsBtn = newView.findViewById(R.id.legs);
            View coreBtn = newView.findViewById(R.id.core);
            View dontBtn = newView.findViewById(R.id.dont);
            View backBtn = newView.findViewById(R.id.back);

            View.OnClickListener nextListener = v -> {
                animateButtonSelection(v, true);
                String bodyPart = v.getId() == R.id.arms ? "arms" :
                        v.getId() == R.id.back_p ? "back_p" :
                                v.getId() == R.id.chest ? "chest" :
                                        v.getId() == R.id.legs ? "legs" :
                                                v.getId() == R.id.core ? "core" : "dont";
                personalWorkoutData.setBodyPart(bodyPart);
                switchSheet(R.layout.add_p_workout8, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            };
            armsBtn.setOnClickListener(nextListener);
            backPBtn.setOnClickListener(nextListener);
            chestBtn.setOnClickListener(nextListener);
            legsBtn.setOnClickListener(nextListener);
            coreBtn.setOnClickListener(nextListener);
            dontBtn.setOnClickListener(nextListener);
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout6, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout8) {
            View gymBtn = newView.findViewById(R.id.gym);
            View homeBtn = newView.findViewById(R.id.home);
            View backBtn = newView.findViewById(R.id.back);

            View.OnClickListener nextListener = v -> {
                animateButtonSelection(v, true);
                String location = v.getId() == R.id.gym ? "gym" : "home";
                personalWorkoutData.setLocation(location);
                switchSheet(R.layout.add_p_workout9, null, true, R.anim.slide_out_left, R.anim.slide_in_right);
            };
            gymBtn.setOnClickListener(nextListener);
            homeBtn.setOnClickListener(nextListener);
            backBtn.setOnClickListener(v -> switchSheet(R.layout.add_p_workout7, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout9) {
            // Код для add_p_workout9 остаётся без изменений, кроме создания программы
            int[] dayIds = {R.id.monday, R.id.tuesday, R.id.wednesday, R.id.thursday, R.id.friday, R.id.saturday, R.id.sunday};
            String[] dayNames = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
            View createBtn = newView.findViewById(R.id.create);
            View backBtn = newView.findViewById(R.id.back);
            RelativeLayout countDayLayout = newView.findViewById(R.id.count_day);
            TextView countDayTextView = newView.findViewById(R.id.count_day_text);

            TextView[] dayTextViews = new TextView[dayIds.length];

            if (createBtn == null || backBtn == null || countDayLayout == null || countDayTextView == null) {
                Log.e("MainActivity", "One or more UI elements in add_p_workout9 are null");
                Toast.makeText(this, "Ошибка загрузки интерфейса", Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < dayIds.length; i++) {
                TextView dayTextView = newView.findViewById(dayIds[i]);
                if (dayTextView == null) {
                    Log.e("MainActivity", "Day TextView not found for ID: " + dayIds[i]);
                    continue;
                }
                dayTextViews[i] = dayTextView;
                String dayName = dayNames[i];
                dayTextView.setTag(false);
                dayTextView.setBackgroundResource(R.drawable.done_btn_selector2);
                dayTextView.setSelected(false);

                dayTextView.setOnClickListener(v -> {
                    boolean isSelected = !(Boolean) dayTextView.getTag();
                    int selectedCount = personalWorkoutData.getTrainingDays().size();

                    if (isSelected && selectedCount >= 3) {
                        Toast.makeText(this, "Можно выбрать не более 3 дней", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dayTextView.setTag(isSelected);
                    dayTextView.setSelected(isSelected);
                    animateButtonSelection(dayTextView, isSelected);

                    if (isSelected) {
                        personalWorkoutData.addTrainingDay(dayName);
                    } else {
                        personalWorkoutData.removeTrainingDay(dayName);
                    }

                    Log.d("MainActivity", "Day: " + dayName + ", isSelected: " + isSelected +
                            ", Days: " + personalWorkoutData.getTrainingDays() +
                            ", Count: " + personalWorkoutData.getTrainingDays().size());

                    countDayTextView.setText(personalWorkoutData.getTrainingDays().size() + " из 3");

                    selectedCount = personalWorkoutData.getTrainingDays().size();
                    for (TextView otherDay : dayTextViews) {
                        if (otherDay != null) {
                            boolean isOtherSelected = (Boolean) otherDay.getTag();
                            otherDay.setEnabled(isOtherSelected || selectedCount < 3);
                            otherDay.setAlpha(!isOtherSelected && selectedCount >= 3 ? 0.5f : 1f);
                        }
                    }

                    createBtn.setEnabled(selectedCount > 0);
                    updateCreateButtonAnimation(createBtn, selectedCount > 0);
                });
            }

            countDayTextView.setText(personalWorkoutData.getTrainingDays().size() + " из 3");

            createBtn.setOnClickListener(v -> {
                if (personalWorkoutData.getTrainingDays().isEmpty()) {
                    Toast.makeText(this, "Выберите хотя бы один день", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (personalWorkoutData.getGoal() == null || personalWorkoutData.getFitnessLevel() == null ||
                        personalWorkoutData.getBodyPart() == null) {
                    Toast.makeText(this, "Заполните все параметры тренировки", Toast.LENGTH_SHORT).show();
                    switchSheet(R.layout.add_p_workout8, null, true, R.anim.slide_out_right, R.anim.slide_in_left);
                    return;
                }

                WorkoutProgramGenerator generator = new WorkoutProgramGenerator(personalWorkoutData);
                WorkoutProgramGenerator.WorkoutProgram program = generator.generateProgram();

                if (program.getDailyWorkouts().isEmpty()) {
                    Log.w("MainActivity", "Generated program is empty");
                    Toast.makeText(this, "Не удалось создать программу. Проверьте параметры.", Toast.LENGTH_SHORT).show();
                    return;
                }

                switchSheet(R.layout.add_p_workout_final, program, true, R.anim.slide_out_left, R.anim.slide_in_right);
                Toast.makeText(this, "Программа создана", Toast.LENGTH_SHORT).show();
            });

            backBtn.setOnClickListener(v ->
                    switchSheet(R.layout.add_p_workout8, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
        } else if (layoutId == R.layout.add_p_workout_final) {
            RecyclerView recyclerView = newView.findViewById(R.id.selected_exercises_list);
            TextView targetText = newView.findViewById(R.id.target);
            TextView levelText = newView.findViewById(R.id.level);
            TextView daysText = newView.findViewById(R.id.days_week);
            TextView areaText = newView.findViewById(R.id.area);
            View createBtn = newView.findViewById(R.id.create_program);
            View backBtn = newView.findViewById(R.id.close);

            if (recyclerView == null || targetText == null || levelText == null ||
                    daysText == null || areaText == null || createBtn == null || backBtn == null) {
                Log.e("MainActivity", "One or more UI elements in add_p_workout_final are null");
                Toast.makeText(this, "Ошибка загрузки интерфейса", Toast.LENGTH_SHORT).show();
                return;
            }

            if (data instanceof WorkoutProgramGenerator.WorkoutProgram) {
                WorkoutProgramGenerator.WorkoutProgram program = (WorkoutProgramGenerator.WorkoutProgram) data;

                if (program.getDailyWorkouts() == null || program.getDailyWorkouts().isEmpty()) {
                    Log.e("MainActivity", "Program is null or empty");
                    Toast.makeText(this, "Программа не содержит упражнений", Toast.LENGTH_SHORT).show();
                    recyclerView.setVisibility(View.GONE);
                    return;
                }

                // Логирование для проверки данных
                for (WorkoutProgramGenerator.DailyWorkout dailyWorkout : program.getDailyWorkouts()) {
                    Log.d("MainActivity", "Day: " + dailyWorkout.getDayName() + ", Focus: " + dailyWorkout.getFocusMuscle() +
                            ", Exercises: " + dailyWorkout.getExercises().size());
                    for (WorkoutProgramGenerator.WorkoutExercise exercise : dailyWorkout.getExercises()) {
                        StringBuilder setsInfo = new StringBuilder();
                        for (com.example.logster.Set set : exercise.getSets()) {
                            setsInfo.append("Вес: ").append(set.getWeight()).append(", Повторений: ").append(set.getReps()).append("; ");
                        }
                        Log.d("MainActivity", "Exercise: " + exercise.getExercise().getName() +
                                ", Sets: " + setsInfo +
                                ", Tags: " + String.join(", ", exercise.getExercise().getTags()));
                    }
                }

                // Подготовка данных для адаптера
                List<WorkoutProgramItem> items = prepareWorkoutItems(program.getDailyWorkouts());
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(new WorkoutExerciseAdapter(items));
                recyclerView.setVisibility(View.VISIBLE);

                String goal = personalWorkoutData.getGoal();
                String fitnessLevel = personalWorkoutData.getFitnessLevel();
                List<String> trainingDays = personalWorkoutData.getTrainingDays();

                targetText.setText("Программа: " + program.getProgramName());
                levelText.setText("Уровень: " + (fitnessLevel != null ? translateFitnessLevel(fitnessLevel) : "Не указан"));
                daysText.setText("Дней в неделю: " + (trainingDays != null ? trainingDays.size() : 0));
                areaText.setText("Целевая область: " + program.getProgramName());

                createBtn.setOnClickListener(v -> {
                    if (personalWorkoutData.getTrainingDays().isEmpty()) {
                        Toast.makeText(this, "Дни тренировок не выбраны", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createProgramFolderAndWorkouts(program.getDailyWorkouts());
                    updateMainItems();
                    combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                    widgetSheet.hide(() -> Toast.makeText(this, "Программа сохранена", Toast.LENGTH_SHORT).show());
                });

                backBtn.setOnClickListener(v ->
                        switchSheet(R.layout.add_p_workout9, null, true, R.anim.slide_out_right, R.anim.slide_in_left));
            } else {
                Log.e("MainActivity", "Invalid data type for add_p_workout_final: " +
                        (data != null ? data.getClass().getName() : "null"));
                Toast.makeText(this, "Ошибка отображения программы", Toast.LENGTH_SHORT).show();
                recyclerView.setVisibility(View.GONE);
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
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(selectedWorkoutName))
                .findFirst()
                .orElse(null);
        if (workout != null) {
            ExercisesAdapter.Exercise currentExercise = selectedExercises.stream()
                    .filter(e -> e.getSets().contains(set))
                    .findFirst()
                    .orElse(null);
            if (currentExercise != null) {
                List<Set> workoutSets = workout.exerciseSets.computeIfAbsent(currentExercise.getId(), k -> new ArrayList<>());
                int index = workoutSets.indexOf(set);
                if (index >= 0) {
                    workoutSets.set(index, set); // Обновляем существующий подход
                } else {
                    workoutSets.add(set); // Добавляем новый подход
                }
                saveWorkouts();
                Log.d("MainActivity", "Сохранён подход в тренировке: id=" + set.getId() + ", вес=" + set.getWeight() + ", повт=" + set.getReps());
            } else {
                Log.w("MainActivity", "Упражнение для подхода не найдено: id=" + set.getId());
            }
        } else {
            Log.w("MainActivity", "Тренировка не найдена: " + selectedWorkoutName);
        }
    }
    public void backSheetEditExercises(View view) {
        RecyclerView setsList = findViewById(R.id.sets_list);
        if (setsList != null && setsList.getAdapter() instanceof SelectedSetsAdapter) {
            ((SelectedSetsAdapter) setsList.getAdapter()).saveAllSets();
        }
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

    private void logWorkout(View view) {
        if (selectedWorkoutName == null) {
            Toast.makeText(this, "Тренировка не выбрана", Toast.LENGTH_SHORT).show();
            return;
        }
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(selectedWorkoutName))
                .findFirst()
                .orElse(null);
        if (workout == null) {
            Toast.makeText(this, "Тренировка не найдена", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        Log.d("MainActivity", "Запись тренировки на дату: " + selectedDate);

        List<CompletedExercise> completedExercises = new ArrayList<>();
        for (ExercisesAdapter.Exercise exercise : selectedExercises) {
            List<Set> sets = new ArrayList<>(exercise.getSets());
            // Устанавливаем isCompleted=true только для подходов с reps > 0
            for (Set set : sets) {
                if (set.getReps() > 0) {
                    set.setCompleted(true);
                    Log.d("MainActivity", "Set marked as completed: ID=" + set.getId() + ", Weight=" + set.getWeight() + ", Reps=" + set.getReps());
                } else {
                    Log.d("MainActivity", "Set skipped (invalid reps): ID=" + set.getId() + ", Weight=" + set.getWeight() + ", Reps=" + set.getReps());
                }
            }
            completedExercises.add(new CompletedExercise(exercise.getId(), sets));
        }

        workout.addCompletedDate(selectedDate, completedExercises);
        if (!workout.dates.contains(selectedDate)) {
            List<String> workoutIds = specificDates.getOrDefault(selectedDate, new ArrayList<>());
            if (!workoutIds.contains(workout.id)) {
                workoutIds.add(workout.id);
                specificDates.put(selectedDate, workoutIds);
                workout.addDate(selectedDate);
                Log.d("MainActivity", "Добавлена дата " + selectedDate + " для тренировки " + workout.name);
            }
        }
        workoutCountCache.remove(workout.id); // Сбрасываем кэш для этой тренировки

        // Сохраняем данные
        saveWorkouts();
        saveSpecificDates();
        syncWorkoutDates();

        // Обновляем UI
        updateMainItems();
        combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
        // Обновляем WorkoutAdapter, если он используется
        if (workoutRecyclerView.getAdapter() instanceof WorkoutAdapter) {
            ((WorkoutAdapter) workoutRecyclerView.getAdapter()).updateWorkouts(workouts);
            Log.d("MainActivity", "WorkoutAdapter обновлён после записи тренировки");
        }

        Toast.makeText(this, "Тренировка записана", Toast.LENGTH_SHORT).show();

        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(() -> Log.d("MainActivity", "Лист скрыт после записи тренировки"));
        }

        Log.d("MainActivity", "Тренировка записана: " + selectedWorkoutName + " на дату " + selectedDate);
    }

    private Map<String, Integer> getUniqueWorkoutCountByDay() {
        Map<String, java.util.Set<String>> workoutNamesByDay = new HashMap<>();
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        for (String day : days) {
            workoutNamesByDay.put(day, new HashSet<>());
        }

        LocalDate today = LocalDate.now();

        // 1. Учитываем завершённые тренировки из completedDates
        for (Workout workout : workouts) {
            for (Map.Entry<String, List<CompletedExercise>> entry : workout.completedDates.entrySet()) {
                String dateStr = entry.getKey();
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"))
                            .substring(0, 1).toUpperCase() +
                            date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                    workoutNamesByDay.get(dayName).add(workout.name);
                } catch (Exception e) {
                    Log.e("MainActivity", "Ошибка разбора даты в getUniqueWorkoutCountByDay (completedDates): " + dateStr, e);
                }
            }
        }

        // 2. Учитываем запланированные тренировки из weeklySchedule
        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId != null && !workoutId.isEmpty()) {
                String dayName = dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("ru"))
                        .substring(0, 1).toUpperCase() +
                        dayOfWeek.getDisplayName(TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                workouts.stream()
                        .filter(w -> w.id.equals(workoutId))
                        .findFirst()
                        .ifPresent(workout -> workoutNamesByDay.get(dayName).add(workout.name));
            }
        }

        // 3. Учитываем запланированные тренировки из specificDates
        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            List<String> workoutIds = entry.getValue();
            if (workoutIds == null || workoutIds.isEmpty()) continue;
            try {
                LocalDate date = LocalDate.parse(dateStr);
                String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"))
                        .substring(0, 1).toUpperCase() +
                        date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")).substring(1).toLowerCase();
                for (String workoutId : workoutIds) {
                    workouts.stream()
                            .filter(w -> w.id.equals(workoutId))
                            .findFirst()
                            .ifPresent(workout -> workoutNamesByDay.get(dayName).add(workout.name));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Ошибка разбора даты в getUniqueWorkoutCountByDay (specificDates): " + dateStr, e);
            }
        }

        // Преобразуем в количество уникальных тренировок
        Map<String, Integer> workoutCountByDay = new HashMap<>();
        for (String day : days) {
            workoutCountByDay.put(day, workoutNamesByDay.get(day).size());
        }

        Log.d("MainActivity", "getUniqueWorkoutCountByDay: " + workoutCountByDay);
        return workoutCountByDay;
    }

    private List<String> getWorkoutsForDay(String dayName) {
        java.util.Set<String> workoutNames = new java.util.HashSet<>();

        // Map for mapping DayOfWeek to Russian day names (Понедельник, Вторник, ...)
        Map<DayOfWeek, String> dayNames = Map.of(
                DayOfWeek.MONDAY, "Понедельник",
                DayOfWeek.TUESDAY, "Вторник",
                DayOfWeek.WEDNESDAY, "Среда",
                DayOfWeek.THURSDAY, "Четверг",
                DayOfWeek.FRIDAY, "Пятница",
                DayOfWeek.SATURDAY, "Суббота",
                DayOfWeek.SUNDAY, "Воскресенье"
        );

        // Check weekly schedule
        DayOfWeek targetDay = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
        if (targetDay != null) {
            String workoutId = weeklySchedule.get(targetDay);
            if (workoutId != null) {
                findWorkoutName(workoutId).ifPresent(workoutNames::add);
            }
        }

        // Check specific dates
        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                String formattedDay = dayNames.get(date.getDayOfWeek());
                if (dayName.equals(formattedDay)) {
                    for (String workoutId : entry.getValue()) {
                        findWorkoutName(workoutId).ifPresent(workoutNames::add);
                    }
                }
            } catch (java.time.format.DateTimeParseException e) {
                Log.e("MainActivity", "Ошибка разбора даты: " + entry.getKey(), e);
            }
        }

        return new ArrayList<>(workoutNames);
    }

    // Helper method to find workout name by ID
    private Optional<String> findWorkoutName(String workoutId) {
        return workouts.stream()
                .filter(w -> w.id.equals(workoutId))
                .map(w -> w.name)
                .findFirst();
    }

    public void addPersonalWorkout(View view) {
        personalWorkoutData = new PersonalWorkoutData(); // Инициализация данных
        switchSheet(R.layout.add_p_workout, null, false, 0, 0);
        Log.d("MainActivity", "Started personal workout creation");
    }
    private void animateButtonSelection(View button, boolean isSelected) {
        // Мгновенно устанавливаем новое состояние
        button.setSelected(isSelected);
        button.setBackgroundResource(R.drawable.done_btn_selector2);

        // Анимация масштабирования
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f);
        // Легкая анимация прозрачности
        ObjectAnimator alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.8f, 1f);

        // Настраиваем анимацию
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
        animatorSet.setDuration(120); // 120 мс для плавности
        animatorSet.setInterpolator(new OvershootInterpolator(1.5f)); // Легкий отскок
        animatorSet.start();
    }
    private void updateCreateButtonAnimation(View button, boolean isEnabled) {
        button.setEnabled(isEnabled);
        // Устанавливаем селектор сразу, чтобы он отобразил правильное состояние
        button.setBackgroundResource(R.drawable.enter_btn_selector2);

        // Плавная анимация для переходов
        float startAlpha = isEnabled ? 0.5f : 1f;
        float endAlpha = isEnabled ? 1f : 0.5f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(button, "alpha", startAlpha, endAlpha);
        animator.setDuration(200); // 200 мс для плавности
        animator.setInterpolator(new LinearInterpolator()); // Линейный для простоты
        animator.start();
    }
    private String translateGoal(String goal) {
        switch (goal) {
            case "muscle": return "Набрать массу";
            case "slim": return "Похудеть";
            default: return "Не указано";
        }
    }

    private String translateFitnessLevel(String level) {
        switch (level) {
            case "beginner": return "Новичок";
            case "intermediate": return "Средний";
            case "advanced": return "Продвинутый";
            default: return "Не указано";
        }
    }

    private String translateBodyPart(String bodyPart) {
        switch (bodyPart) {
            case "arms": return "Руки";
            case "back_p": return "Спина";
            case "chest": return "Грудь";
            case "legs": return "Ноги";
            case "core": return "Кор";
            case "dont": return "Все тело";
            default: return "Не указано";
        }
    }
    private List<WorkoutProgramItem> prepareWorkoutItems(List<WorkoutProgramGenerator.DailyWorkout> dailyWorkouts) {
        List<WorkoutProgramItem> items = new ArrayList<>();
        if (dailyWorkouts == null || dailyWorkouts.isEmpty()) {
            Log.w("MainActivity", "Daily workouts are null or empty");
            return items;
        }

        for (WorkoutProgramGenerator.DailyWorkout dailyWorkout : dailyWorkouts) {
            String dayName = dailyWorkout.getDayName();
            String workoutName = translateBodyPart(dailyWorkout.getFocusMuscle()); // Название по акценту
            items.add(new DayHeader(dayName, workoutName));

            for (WorkoutProgramGenerator.WorkoutExercise exercise : dailyWorkout.getExercises()) {
                items.add(new WorkoutExerciseItem(exercise));
            }
        }

        Log.d("MainActivity", "Prepared " + items.size() + " workout items for " + dailyWorkouts.size() + " days");
        return items;
    }

    private void createProgramFolderAndWorkouts(List<WorkoutProgramGenerator.DailyWorkout> dailyWorkouts) {
        // Создаем уникальное имя папки
        String programName = translateBodyPart(personalWorkoutData.getBodyPart());
        String folderName = programName;
        int suffix = 1;

        // Проверяем существование папки и генерируем уникальное имя
        while (true) {
            String candidateName = folderName; // Временная переменная для проверки
            if (!folders.stream().anyMatch(f -> f.name.equals(candidateName))) {
                break; // Имя уникально, выходим из цикла
            }
            folderName = programName + " " + suffix++; // Обновляем folderName для следующей итерации
        }

        // Создаем папку
        String folderId = UUID.randomUUID().toString();
        Folder folder = new Folder(folderId, folderName);
        folders.add(folder);

        // Создаем тренировки для каждого дня
        for (WorkoutProgramGenerator.DailyWorkout dailyWorkout : dailyWorkouts) {
            String dayName = dailyWorkout.getDayName();
            String focusMuscle = dailyWorkout.getFocusMuscle();
            String workoutName;

            // Если акцент "general", формируем название "Общая (день недели)"
            if (focusMuscle.equals("general")) {
                workoutName = "Общая (" + dayName + ")";
            } else {
                workoutName = translateBodyPart(focusMuscle); // Название по акценту
            }

            String workoutId = UUID.randomUUID().toString();
            Workout newWorkout = new Workout(workoutId, workoutName);

            // Добавляем упражнения
            for (WorkoutProgramGenerator.WorkoutExercise exercise : dailyWorkout.getExercises()) {
                newWorkout.exerciseIds.add(exercise.getExercise().getId());
                List<com.example.logster.Set> sets = new ArrayList<>(exercise.getSets());
                newWorkout.exerciseSets.put(exercise.getExercise().getId(), sets);
            }

            workouts.add(newWorkout);
            folder.addItem(workoutId);

            // Обновляем weeklySchedule
            DayOfWeek dayOfWeek = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
            if (dayOfWeek != null) {
                weeklySchedule.put(dayOfWeek, workoutId);
            }
        }

        // Сохраняем данные
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String programKey = "personal_workout_" + folderId;
            editor.putString(programKey, personalWorkoutData.toJson().toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e("MainActivity", "Ошибка сохранения программы: " + e.getMessage(), e);
        }

        saveFolders();
        saveWorkouts();
        saveWeeklySchedule();
        syncWorkoutDates();

        Log.d("MainActivity", "Создана папка: " + folderName + " с " + dailyWorkouts.size() + " тренировками");
    }

    public void onDeleteWorkoutClicked(View view) {
        Log.d("DeleteWorkout", "Кнопка удаления тренировки нажата");

        // Получаем текущую тренировку по selectedWorkoutName
        Workout workout = workouts.stream()
                .filter(w -> w.name.equals(selectedWorkoutName))
                .findFirst()
                .orElse(null);

        if (workout == null) {
            Log.e("DeleteWorkout", "Тренировка не найдена: " + selectedWorkoutName);
            Toast.makeText(this, "Тренировка не найдена", Toast.LENGTH_SHORT).show();
            return;
        }

        // Находим позицию тренировки в combinedAdapter.items
        int position = combinedAdapter.items.indexOf(workout);
        if (position == -1) {
            Log.w("DeleteWorkout", "Тренировка не найдена в combinedAdapter.items: " + workout.name);
        }

        String confirmationMessage;
        Runnable onConfirmAction;

        if (isInFolder) {
            // Тренировка в папке: переносим на главную страницу
            confirmationMessage = "Удалить \"" + workout.name + "\" из папки?";
            onConfirmAction = () -> {
                try {
                    // Находим текущую папку
                    Folder folder = folders.stream()
                            .filter(f -> f.name.equals(currentFolderName))
                            .findFirst()
                            .orElse(null);
                    if (folder != null) {
                        // Удаляем тренировку из папки
                        folder.itemIds.remove(workout.id);
                        Log.d("DeleteWorkout", "Тренировка \"" + workout.name + "\" удалена из папки: " + currentFolderName);

                        // Устанавливаем флаг, что тренировка не в папке
                        isInFolder = false; // Предполагаем, что у Workout есть поле isInFolder
                        Log.d("DeleteWorkout", "Тренировка \"" + workout.name + "\" перенесена на главную страницу");

                        // Обновляем UI папки
                        int folderPos = combinedAdapter.getFolderPosition(folder.name);
                        if (folderPos != -1) {
                            combinedAdapter.notifyItemChanged(folderPos);
                        }

                        // Закрываем BottomSheet без анимации
                        widgetSheet.hide(() -> {
                            updateMainItems();
                            saveItems();
                            combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                            Log.d("DeleteWorkout", "Лист скрыт, адаптер обновлен");
                        });
                    } else {
                        Log.w("DeleteWorkout", "Папка \"" + currentFolderName + "\" не найдена");
                        Toast.makeText(this, "Папка не найдена", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("DeleteWorkout", "Ошибка переноса тренировки: " + e.getMessage(), e);
                    Toast.makeText(this, "Ошибка при переносе тренировки", Toast.LENGTH_SHORT).show();
                }
            };
        } else {
            // Тренировка не в папке: полное удаление
            confirmationMessage = "Удалить";
            onConfirmAction = () -> {
                try {
                    // Удаляем тренировку
                    workouts.remove(workout);
                    removeWorkoutFromSchedules(workout.id);
                    Log.d("DeleteWorkout", "Тренировка удалена: " + workout.name);

                    // Удаляем из combinedAdapter.items
                    if (position != -1) {
                        combinedAdapter.items.remove(position);
                        combinedAdapter.notifyItemRemoved(position);
                        Log.d("DeleteWorkout", "Тренировка удалена из адаптера на позиции: " + position);
                    }

                    // Анимация "растворения" (Fade Out)
                    RelativeLayout editWorkoutLayout = widgetSheet.getContentView().findViewById(R.id.edit_workout);
                    if (editWorkoutLayout != null) {
                        editWorkoutLayout.animate()
                                .alpha(0f) // Постепенное исчезновение
                                .setDuration(300) // Длительность анимации 300 мс
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        editWorkoutLayout.setVisibility(View.GONE);
                                        // Закрываем BottomSheet
                                        widgetSheet.hide(() -> {
                                            updateMainItems();
                                            saveItems();
                                            combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                                            Log.d("DeleteWorkout", "Лист скрыт, адаптер обновлен");
                                        });
                                    }
                                })
                                .start();
                    } else {
                        Log.w("DeleteWorkout", "edit_workout layout не найден в widgetSheet");
                        // Если layout не найден, просто закрываем лист
                        widgetSheet.hide(() -> {
                            updateMainItems();
                            saveItems();
                            combinedAdapter.updateData(folders, workouts, bodyMetrics, currentFolderName);
                            Log.d("DeleteWorkout", "Лист скрыт без анимации, адаптер обновлен");
                        });
                    }
                } catch (Exception e) {
                    Log.e("DeleteWorkout", "Ошибка удаления: " + e.getMessage(), e);
                    Toast.makeText(this, "Ошибка при удалении тренировки", Toast.LENGTH_SHORT).show();
                }
            };
        }

        // Показываем confirmationSheet с нужным текстом
        confirmationSheet.show(
                confirmationMessage,
                workout.name,
                "Workout",
                workout.id,
                onConfirmAction,
                () -> Log.d("DeleteWorkout", "Отменено действие: " + workout.name)
        );
    }
    public long getTotalWorkoutCount(String workoutId) {
        if (workoutCountCache.containsKey(workoutId)) {
            long cachedCount = workoutCountCache.get(workoutId);
            Log.d("MainActivity", "Cached workout count for ID " + workoutId + ": " + cachedCount);
            return cachedCount;
        }

        // Подсчёт только завершённых тренировок из completedDates
        Workout workout = workouts.stream()
                .filter(w -> w.id.equals(workoutId))
                .findFirst()
                .orElse(null);
        long count = (workout != null) ? workout.completedDates.size() : 0;
        workoutCountCache.put(workoutId, count);
        Log.d("MainActivity", "Total workout count for ID " + workoutId + ": " + count);
        return count;
    }

}