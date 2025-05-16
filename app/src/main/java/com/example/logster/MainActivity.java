package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AddWorkout.WorkoutSelectionListener {

    private BottomNavigationManager navManager;
    private BottomSheets widgetSheet;
    private String selectedWorkoutName; // Для хранения названия тренировки
    private List<String> selectedDays; // Для хранения выбранных дней
    private List<Workout> workouts; // Список тренировок
    private RecyclerView workoutRecyclerView; // RecyclerView для отображения виджетов
    private WorkoutAdapter workoutAdapter; // Адаптер для RecyclerView
    private SharedPreferences sharedPreferences; // Для локального хранения

    // Цвета для анимации
    private static final int COLOR_UNSELECTED = 0xFF606062; // #606062 (тёмно-серый)
    private static final int COLOR_SELECTED = 0xFFFFFFFF;   // #FFFFFF (белый)

    // Ключи для SharedPreferences
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WORKOUTS_KEY = "workouts";

    // Класс для хранения данных о тренировке
    public static class Workout {
        String name;
        List<String> days;

        Workout(String name, List<String> days) {
            this.name = name;
            this.days = days;
        }

        // Сериализация в JSONObject
        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            JSONArray daysArray = new JSONArray();
            for (String day : days) {
                daysArray.put(day);
            }
            json.put("days", daysArray);
            return json;
        }

        // Десериализация из JSONObject
        static Workout fromJson(JSONObject json) throws JSONException {
            String name = json.getString("name");
            JSONArray daysArray = json.getJSONArray("days");
            List<String> days = new ArrayList<>();
            for (int i = 0; i < daysArray.length(); i++) {
                days.add(daysArray.getString(i));
            }
            return new Workout(name, days);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navManager = new BottomNavigationManager(findViewById(R.id.main), this);
        navManager.setCurrentActivity("MainActivity");

        // Инициализация widgetSheet с R.layout.widgets
        widgetSheet = new BottomSheets(this, R.layout.widgets);
        AddWorkout.setWorkoutSelectionListener(this);

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Инициализация списка тренировок
        workouts = new ArrayList<>();

        // Настройка RecyclerView
        workoutRecyclerView = findViewById(R.id.workout_recycler_view);
        workoutRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 столбца

        // Создаём ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                workoutAdapter.onItemMove(fromPosition, toPosition);
                saveWorkouts(); // Сохраняем новый порядок
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                workoutAdapter.onItemDismiss(position);
                saveWorkouts(); // Сохраняем после удаления
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.5f); // Затемнение при перетаскивании
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f); // Возвращаем прозрачность
            }
        });

        // Передаём ItemTouchHelper в адаптер
        workoutAdapter = new WorkoutAdapter(workouts, this, itemTouchHelper);
        workoutRecyclerView.setAdapter(workoutAdapter);
        itemTouchHelper.attachToRecyclerView(workoutRecyclerView);

        // Добавляем ItemDecoration для центрирования
        workoutRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int columnCount = 2; // Количество столбцов
                int marginBetween = dpToPx(10); // Отступ между виджетами (10dp)

                // Отступы для каждого элемента
                outRect.bottom = marginBetween; // Отступ снизу

                // Для первого столбца добавляем дополнительный отступ слева, чтобы центрировать
                if (position % columnCount == 0) {
                    outRect.left = 0;
                    outRect.right = marginBetween / 2;
                } else {
                    outRect.left = marginBetween / 2;
                    outRect.right = 0;
                }
            }
        });

        // Загружаем сохранённые тренировки и отображаем их
        loadWorkouts();

        hideSystemUI();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    @Override
    public void onBackPressed() {
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        navManager.forceSetActiveButton("MainActivity");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navManager != null) {
            navManager.forceSetActiveButton("MainActivity");
        }
    }

    public void addWidgets(View view) {
        // Сбрасываем состояние
        selectedWorkoutName = null;
        if (selectedDays != null) {
            selectedDays.clear();
        }

        // Закрываем текущий BottomSheets, если он открыт
        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(() -> {
                // Создаём новый BottomSheets с R.layout.widgets
                widgetSheet = new BottomSheets(this, R.layout.widgets);
                widgetSheet.show();
            });
        } else {
            // Если BottomSheets не открыт, создаём новый
            widgetSheet = new BottomSheets(this, R.layout.widgets);
            widgetSheet.show();
        }
    }

    public void closeSheet(View view) {
        hideKeyboard();
        // Сбрасываем состояние
        selectedWorkoutName = null;
        if (selectedDays != null) {
            selectedDays.clear();
        }
        widgetSheet.hide(null);
    }

    private void switchSheet(int layoutId, String workoutName, boolean useHorizontalTransition, int exitAnim, int enterAnim) {
        hideKeyboard();

        View newView = LayoutInflater.from(this).inflate(layoutId, null);

        if (layoutId == R.layout.add_workout) {
            ScrollView scrollView = newView.findViewById(R.id.trainingScrollView);
            LinearLayout trainingList = newView.findViewById(R.id.trainingList);
            View topLine = newView.findViewById(R.id.top_line);
            View bottomLine = newView.findViewById(R.id.bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);
            TextView workoutNameTextView = newView.findViewById(R.id.workout_name);
            TextView workoutDescriptionTextView = newView.findViewById(R.id.workout_description);

            AddWorkout.setupScrollHighlight(
                    scrollView,
                    trainingList,
                    topLine,
                    bottomLine,
                    centerArrow,
                    workoutNameTextView,
                    workoutDescriptionTextView
            );
        } else if (layoutId == R.layout.add_workout2) {
            EditText workoutNameEditText = newView.findViewById(R.id.ETworkout_name);
            if (workoutName != null) {
                workoutNameEditText.setText(workoutName);
            }

            TextView continueBtn = newView.findViewById(R.id.continue_btn);
            continueBtn.setOnClickListener(v -> openAddWorkout3(v, workoutNameEditText));
        } else if (layoutId == R.layout.add_workout3) {
            selectedDays = new ArrayList<>();

            com.google.android.flexbox.FlexboxLayout daysContainer = newView.findViewById(R.id.days_container);
            for (int i = 0; i < daysContainer.getChildCount(); i++) {
                View child = daysContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView dayTextView = (TextView) child;

                    // Сброс состояния выбора
                    dayTextView.setTag(false);
                    dayTextView.getBackground().setTint(COLOR_UNSELECTED);

                    dayTextView.setOnClickListener(v -> {
                        boolean isSelected = (boolean) dayTextView.getTag();
                        isSelected = !isSelected;
                        dayTextView.setTag(isSelected);

                        int startColor = isSelected ? COLOR_UNSELECTED : COLOR_SELECTED;
                        int endColor = isSelected ? COLOR_SELECTED : COLOR_UNSELECTED;
                        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
                        colorAnimator.setDuration(300);
                        colorAnimator.addUpdateListener(animator -> {
                            int animatedColor = (int) animator.getAnimatedValue();
                            dayTextView.getBackground().setTint(animatedColor);
                        });
                        colorAnimator.start();

                        String day = dayTextView.getText().toString();
                        if (isSelected) {
                            selectedDays.add(day);
                        } else {
                            selectedDays.remove(day);
                        }
                    });
                }
            }

            TextView continueBtn = newView.findViewById(R.id.continue_btn);
            continueBtn.setOnClickListener(v -> {
                if (!selectedDays.isEmpty()) {
                    workouts.add(new Workout(selectedWorkoutName, new ArrayList<>(selectedDays)));
                    saveWorkouts();
                    workoutAdapter.notifyDataSetChanged();
                    widgetSheet.hide(null);
                }
            });
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

    private void openAddWorkout3(View view, EditText workoutNameEditText) {
        selectedWorkoutName = workoutNameEditText.getText().toString();
        if (!selectedWorkoutName.trim().isEmpty()) {
            switchSheet(R.layout.add_workout3, selectedWorkoutName, true, R.anim.slide_out_left, R.anim.slide_in_right);
        }
    }

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
            currentFocus.clearFocus();
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    }

    public String getNearestDay(List<String> days) {
        LocalDate today = LocalDate.now();
        DayOfWeek todayDayOfWeek = today.getDayOfWeek();

        // Маппинг русских названий дней на DayOfWeek
        List<DayOfWeek> selectedDaysOfWeek = new ArrayList<>();
        for (String day : days) {
            switch (day) {
                case "Понедельник":
                    selectedDaysOfWeek.add(DayOfWeek.MONDAY);
                    break;
                case "Вторник":
                    selectedDaysOfWeek.add(DayOfWeek.TUESDAY);
                    break;
                case "Среда":
                    selectedDaysOfWeek.add(DayOfWeek.WEDNESDAY);
                    break;
                case "Четверг":
                    selectedDaysOfWeek.add(DayOfWeek.THURSDAY);
                    break;
                case "Пятница":
                    selectedDaysOfWeek.add(DayOfWeek.FRIDAY);
                    break;
                case "Суббота":
                    selectedDaysOfWeek.add(DayOfWeek.SATURDAY);
                    break;
                case "Воскресенье":
                    selectedDaysOfWeek.add(DayOfWeek.SUNDAY);
                    break;
            }
        }

        // Находим ближайший день
        DayOfWeek nearestDay = null;
        int minDaysAhead = 8; // Больше, чем неделя, чтобы найти минимум
        for (DayOfWeek day : selectedDaysOfWeek) {
            int daysAhead = day.getValue() - todayDayOfWeek.getValue();
            if (daysAhead < 0) {
                daysAhead += 7; // Если день прошёл, добавляем неделю
            }
            if (daysAhead < minDaysAhead) {
                minDaysAhead = daysAhead;
                nearestDay = day;
            }
        }

        if (nearestDay == null) {
            return "Не выбрано";
        }

        // Возвращаем название дня на русском
        switch (nearestDay) {
            case MONDAY:
                return "Понедельник";
            case TUESDAY:
                return "Вторник";
            case WEDNESDAY:
                return "Среда";
            case THURSDAY:
                return "Четверг";
            case FRIDAY:
                return "Пятница";
            case SATURDAY:
                return "Суббота";
            case SUNDAY:
                return "Воскресенье";
            default:
                return "Неизвестно";
        }
    }

    private void saveWorkouts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            // Сериализуем список тренировок в JSON
            JSONArray workoutsArray = new JSONArray();
            for (Workout workout : workoutAdapter.getWorkouts()) {
                workoutsArray.put(workout.toJson());
            }
            editor.putString(WORKOUTS_KEY, workoutsArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.apply(); // Асинхронно сохраняем данные
    }

    private void loadWorkouts() {
        String workoutsJson = sharedPreferences.getString(WORKOUTS_KEY, null);
        if (workoutsJson != null) {
            try {
                // Десериализуем JSON в список тренировок
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                workouts.clear();
                for (int i = 0; i < workoutsArray.length(); i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    Workout workout = Workout.fromJson(workoutJson);
                    workouts.add(workout);
                }
                workoutAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void addWorkout(View view) {
        switchSheet(R.layout.add_workout, null, false, 0, 0);
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

    @Override
    public void onWorkoutSelected(String workoutKey) {
        switchSheet(R.layout.add_workout2, workoutKey, true, R.anim.slide_out_left, R.anim.slide_in_right);
    }

    // Метод для конвертации dp в пиксели
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}