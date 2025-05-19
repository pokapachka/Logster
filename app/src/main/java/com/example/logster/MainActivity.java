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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AddWorkout.WorkoutSelectionListener {

    private BottomNavigationManager navManager;
    private BottomSheets widgetSheet;
    private String selectedWorkoutName;
    private List<String> selectedDays;
    private List<Workout> workouts;
    private List<BodyMetric> bodyMetrics;
    private RecyclerView workoutRecyclerView;
    private CombinedAdapter combinedAdapter;
    private SharedPreferences sharedPreferences;

    private static final int COLOR_UNSELECTED = 0xFF606062;
    private static final int COLOR_SELECTED = 0xFFFFFFFF;

    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WORKOUTS_KEY = "workouts";
    private static final String METRICS_KEY = "body_metrics";

    public static class Workout {
        String name;
        List<String> days;

        Workout(String name, List<String> days) {
            this.name = name;
            this.days = days;
        }

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

    public static class BodyMetric {
        String type;
        String value;
        long timestamp;

        BodyMetric(String type, String value, long timestamp) {
            this.type = type;
            this.value = value;
            this.timestamp = timestamp;
        }

        JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("value", value);
            json.put("timestamp", timestamp);
            return json;
        }

        static BodyMetric fromJson(JSONObject json) throws JSONException {
            String type = json.getString("type");
            String value = json.getString("value");
            long timestamp = json.getLong("timestamp");
            return new BodyMetric(type, value, timestamp);
        }
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

        workouts = new ArrayList<>();
        bodyMetrics = new ArrayList<>();

        workoutRecyclerView = findViewById(R.id.workout_recycler_view);
        workoutRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        combinedAdapter = new CombinedAdapter(workouts, bodyMetrics, this);
        workoutRecyclerView.setAdapter(combinedAdapter);

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
                combinedAdapter.onItemMove(fromPosition, toPosition);
                saveWorkouts();
                saveBodyMetrics();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                combinedAdapter.onItemDismiss(position);
                saveWorkouts();
                saveBodyMetrics();
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder.itemView.setAlpha(0.5f);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
            }
        });
        itemTouchHelper.attachToRecyclerView(workoutRecyclerView);

        workoutRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
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

        // Установка слушателя для AddBodyMetric
        com.example.logster.AddBodyMetric.setMetricSelectionListener(new com.example.logster.AddBodyMetric.MetricSelectionListener() {
            @Override
            public void onMetricSelected(String metricKey) {
                Log.d("MainActivity", "Выбрана метрика: " + metricKey);
                switchSheet(R.layout.add_metric2, metricKey, true, R.anim.slide_out_left, R.anim.slide_in_right);
            }

            @Override
            public void onMetricValueSelected(String metricType, String value) {
                Log.d("MainActivity", "Выбрано значение для " + metricType + ": " + value);
                bodyMetrics.add(new BodyMetric(metricType, value, System.currentTimeMillis()));
                saveBodyMetrics();
                combinedAdapter.updateData(workouts, bodyMetrics);
                Log.d("MainActivity", "bodyMetrics size: " + bodyMetrics.size());
                widgetSheet.hide(null);
            }
        });

        loadWorkouts();
        loadBodyMetrics();
        combinedAdapter.updateData(workouts, bodyMetrics);
        Log.d("MainActivity", "After load: workouts size: " + workouts.size() + ", bodyMetrics size: " + bodyMetrics.size());
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
        combinedAdapter.updateData(workouts, bodyMetrics);
        Log.d("MainActivity", "onResume: workouts size: " + workouts.size() + ", bodyMetrics size: " + bodyMetrics.size());
    }

    public void addWidgets(View view) {
        selectedWorkoutName = null;
        if (selectedDays != null) {
            selectedDays.clear();
        }

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
        if (selectedDays != null) {
            selectedDays.clear();
        }
        widgetSheet.hide(null);
    }

    private Map<String, List<String>> getWorkoutCountByDay() {
        Map<String, List<String>> workoutCountByDay = new HashMap<>();
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        for (String day : days) {
            workoutCountByDay.put(day, new ArrayList<>());
        }
        for (Workout workout : workouts) {
            for (String day : workout.days) {
                workoutCountByDay.get(day).add(workout.name);
            }
        }
        return workoutCountByDay;
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

        DayOfWeek nearestDay = null;
        int minDaysAhead = 8;
        for (DayOfWeek day : selectedDaysOfWeek) {
            int daysAhead = day.getValue() - todayDayOfWeek.getValue();
            if (daysAhead < 0) {
                daysAhead += 7;
            }
            if (daysAhead < minDaysAhead) {
                minDaysAhead = daysAhead;
                nearestDay = day;
            }
        }

        if (nearestDay == null) {
            return "Не выбрано";
        }

        switch (nearestDay) {
            case MONDAY:
                return "понедельник";
            case TUESDAY:
                return "вторник";
            case WEDNESDAY:
                return "среда";
            case THURSDAY:
                return "четверг";
            case FRIDAY:
                return "пятница";
            case SATURDAY:
                return "суббота";
            case SUNDAY:
                return "воскресенье";
            default:
                return "неизвестно";
        }
    }

    private void saveWorkouts() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            JSONArray workoutsArray = new JSONArray();
            for (Workout workout : workouts) {
                workoutsArray.put(workout.toJson());
            }
            editor.putString(WORKOUTS_KEY, workoutsArray.toString());
            Log.d("MainActivity", "Saved workouts: " + workoutsArray.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving workouts: " + e.getMessage());
        }
        editor.apply();
    }

    private void loadWorkouts() {
        String workoutsJson = sharedPreferences.getString(WORKOUTS_KEY, null);
        if (workoutsJson != null) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                workouts.clear();
                for (int i = 0; i < workoutsArray.length(); i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    Workout workout = Workout.fromJson(workoutJson);
                    workouts.add(workout);
                }
                Log.d("MainActivity", "Loaded workouts: " + workouts.size());
                combinedAdapter.updateData(workouts, bodyMetrics);
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading workouts: " + e.getMessage());
            }
        } else {
            Log.d("MainActivity", "No workouts found in SharedPreferences");
        }
    }

    private void saveBodyMetrics() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {
            JSONArray metricsArray = new JSONArray();
            for (BodyMetric metric : bodyMetrics) {
                metricsArray.put(metric.toJson());
            }
            editor.putString(METRICS_KEY, metricsArray.toString());
            Log.d("MainActivity", "Saved body metrics: " + metricsArray.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving body metrics: " + e.getMessage());
        }
        editor.apply();
    }

    private void loadBodyMetrics() {
        String metricsJson = sharedPreferences.getString(METRICS_KEY, null);
        if (metricsJson != null) {
            try {
                JSONArray metricsArray = new JSONArray(metricsJson);
                bodyMetrics.clear();
                for (int i = 0; i < metricsArray.length(); i++) {
                    JSONObject metricJson = metricsArray.getJSONObject(i);
                    BodyMetric metric = BodyMetric.fromJson(metricJson);
                    bodyMetrics.add(metric);
                }
                Log.d("MainActivity", "Loaded body metrics: " + bodyMetrics.size());
                combinedAdapter.updateData(workouts, bodyMetrics);
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading body metrics: " + e.getMessage());
            }
        } else {
            Log.d("MainActivity", "No body metrics found in SharedPreferences");
        }
    }

    public String formatDate(long timestamp) {
        LocalDate today = LocalDate.now();
        LocalDate date = LocalDate.ofEpochDay(timestamp / (1000 * 60 * 60 * 24));
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(date, today);

        if (daysDiff == 0) {
            return "сегодня";
        } else if (daysDiff == 1) {
            return "вчера";
        } else if (daysDiff == 2) {
            return "позавчера";
        } else {
            return String.format("%02d.%02d", date.getDayOfMonth(), date.getMonthValue());
        }
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
        View newView = LayoutInflater.from(this).inflate(R.layout.edit_workout, null);
        TextView titleTextView = newView.findViewById(R.id.title);
        if (titleTextView != null) {
            titleTextView.setText(workoutName);
        } else {
            Log.e("MainActivity", "title not found in edit_workout");
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
            if (data != null) {
                workoutNameEditText.setText(data);
            }

            // Используем View вместо TextView
            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> openAddWorkout3(v, workoutNameEditText));
            } else {
                Log.e("MainActivity", "continue_btn not found in add_workout2");
            }
        } else if (layoutId == R.layout.add_workout3) {
            selectedDays = new ArrayList<>();

            Map<String, List<String>> workoutCountByDay = getWorkoutCountByDay();

            com.google.android.flexbox.FlexboxLayout daysContainer = newView.findViewById(R.id.days_container);
            int[] dayIds = {
                    R.id.day_monday, R.id.day_tuesday, R.id.day_wednesday,
                    R.id.day_thursday, R.id.day_friday, R.id.day_saturday, R.id.day_sunday
            };
            int[] countIds = {
                    R.id.count_monday, R.id.count_tuesday, R.id.count_wednesday,
                    R.id.count_thursday, R.id.count_friday, R.id.count_saturday, R.id.count_sunday
            };
            String[] dayNames = {
                    "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
            };

            for (int i = 0; i < dayIds.length; i++) {
                TextView dayTextView = newView.findViewById(dayIds[i]);
                TextView countTextView = newView.findViewById(countIds[i]);
                String dayName = dayNames[i];
                List<String> workoutsForDay = workoutCountByDay.get(dayName);

                int workoutCount = workoutsForDay.size();
                countTextView.setText(String.valueOf(workoutCount));

                dayTextView.setTag(false);
                dayTextView.getBackground().setTint(COLOR_UNSELECTED);

                boolean hasWorkouts = workoutCount > 0;
                countTextView.setTag(hasWorkouts);
                countTextView.getBackground().setTint(hasWorkouts ? COLOR_SELECTED : COLOR_UNSELECTED);
                countTextView.setEnabled(hasWorkouts);

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

                countTextView.setOnClickListener(v -> {
                    if ((boolean) countTextView.getTag()) {
                        StringBuilder message = new StringBuilder("На этот день записаны " + workoutCount + " тренировок: ");
                        for (int j = 0; j < workoutsForDay.size(); j++) {
                            message.append(workoutsForDay.get(j));
                            if (j < workoutsForDay.size() - 1) {
                                message.append(", ");
                            }
                        }
                        Toast.makeText(MainActivity.this, message.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            // Используем View вместо TextView
            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> {
                    if (!selectedDays.isEmpty()) {
                        workouts.add(new Workout(selectedWorkoutName, new ArrayList<>(selectedDays)));
                        saveWorkouts();
                        combinedAdapter.updateData(workouts, bodyMetrics);
                        Log.d("MainActivity", "Added workout: " + selectedWorkoutName + ", workouts size: " + workouts.size());
                        widgetSheet.hide(null);
                    } else {
                        Toast.makeText(MainActivity.this, "Выберите хотя бы один день", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("MainActivity", "continue_btn not found in add_workout3");
            }
        } else if (layoutId == R.layout.add_metric) {
            ScrollView scrollView = newView.findViewById(R.id.trainingScrollView);
            LinearLayout metricList = newView.findViewById(R.id.trainingList);
            View topLine = newView.findViewById(R.id.top_line);
            View bottomLine = newView.findViewById(R.id.bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);
            TextView metricNameTextView = newView.findViewById(R.id.workout_name);
            TextView metricDescriptionTextView = newView.findViewById(R.id.workout_description);

            com.example.logster.AddBodyMetric.setupMetricSelection(
                    scrollView,
                    metricList,
                    topLine,
                    bottomLine,
                    centerArrow,
                    metricNameTextView,
                    metricDescriptionTextView
            );
        } else if (layoutId == R.layout.add_metric2) {
            ScrollView mainScrollView = newView.findViewById(R.id.main_value_scroll);
            LinearLayout mainValueList = newView.findViewById(R.id.main_value_list);
            View mainTopLine = newView.findViewById(R.id.main_top_line);
            View mainBottomLine = newView.findViewById(R.id.main_bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);

            com.example.logster.AddBodyMetric.setupNumberPicker(
                    mainScrollView,
                    mainValueList,
                    mainTopLine,
                    mainBottomLine,
                    centerArrow,
                    data
            );
        } else if (layoutId == R.layout.edit_workout) {
            TextView titleTextView = newView.findViewById(R.id.title);
            if (titleTextView != null && data != null) {
                titleTextView.setText(data);
                Log.d("MainActivity", "Set workout name to title: " + data);
            } else {
                Log.e("MainActivity", "title not found or data is null in edit_workout");
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

    // Исправленный CombinedAdapter
    private static class CombinedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Object> items;
        private List<Workout> workouts;
        private List<BodyMetric> bodyMetrics;
        private MainActivity activity;

        private static final int TYPE_WORKOUT = 0;
        private static final int TYPE_METRIC = 1;

        public CombinedAdapter(List<Workout> workouts, List<BodyMetric> bodyMetrics, MainActivity activity) {
            this.workouts = workouts;
            this.bodyMetrics = bodyMetrics;
            this.activity = activity;
            this.items = new ArrayList<>();
            this.items.addAll(workouts);
            this.items.addAll(bodyMetrics);
            Log.d("CombinedAdapter", "Initialized with workouts: " + workouts.size() + ", bodyMetrics: " + bodyMetrics.size());
        }

        public void updateData(List<Workout> newWorkouts, List<BodyMetric> newBodyMetrics) {
            this.workouts = newWorkouts;
            this.bodyMetrics = newBodyMetrics;
            this.items.clear();
            this.items.addAll(workouts);
            this.items.addAll(bodyMetrics);
            notifyDataSetChanged();
            Log.d("CombinedAdapter", "Updated data: workouts: " + workouts.size() + ", bodyMetrics: " + bodyMetrics.size());
        }

        @Override
        public int getItemViewType(int position) {
            Object item = items.get(position);
            if (item instanceof Workout) {
                return TYPE_WORKOUT;
            } else if (item instanceof BodyMetric) {
                return TYPE_METRIC;
            }
            Log.e("CombinedAdapter", "Unknown item type at position: " + position);
            return TYPE_WORKOUT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget, parent, false);
            if (viewType == TYPE_WORKOUT) {
                return new WorkoutAdapter.WorkoutViewHolder(view);
            } else {
                return new BodyMetricAdapter.MetricViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = items.get(position);
            Log.d("CombinedAdapter", "Binding item at position: " + position + ", type: " + (item instanceof Workout ? "Workout" : "BodyMetric"));
            if (holder instanceof WorkoutAdapter.WorkoutViewHolder) {
                Workout workout = (Workout) item;
                WorkoutAdapter.WorkoutViewHolder workoutHolder = (WorkoutAdapter.WorkoutViewHolder) holder;
                workoutHolder.workoutName.setText(workout.name);
                workoutHolder.workoutName.setVisibility(View.VISIBLE);
                workoutHolder.workoutDay.setText(activity.getNearestDay(workout.days));
                workoutHolder.workoutDay.setVisibility(View.VISIBLE);
                workoutHolder.workoutCount.setVisibility(View.GONE);

                // Добавляем слушатель нажатия для открытия экрана редактирования
                workoutHolder.itemView.setOnClickListener(v -> activity.editWorkout(workout.name));
            } else if (holder instanceof BodyMetricAdapter.MetricViewHolder) {
                BodyMetric metric = (BodyMetric) item;
                BodyMetricAdapter.MetricViewHolder metricHolder = (BodyMetricAdapter.MetricViewHolder) holder;
                String displayValue;
                switch (metric.type.toLowerCase()) {
                    case "вес":
                        displayValue = metric.value + "кг";
                        break;
                    case "рост":
                        displayValue = metric.value + "см";
                        break;
                    default:
                        displayValue = metric.value;
                }
                metricHolder.workoutCount.setText(displayValue);
                metricHolder.workoutCount.setVisibility(View.VISIBLE);
                metricHolder.workoutDay.setText(activity.formatDate(metric.timestamp));
                metricHolder.workoutDay.setVisibility(View.VISIBLE);
                metricHolder.workoutName.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            int size = items.size();
            Log.d("CombinedAdapter", "getItemCount: " + size);
            return size;
        }

        public void onItemMove(int fromPosition, int toPosition) {
            Object movedItem = items.remove(fromPosition);
            items.add(toPosition, movedItem);

            workouts.clear();
            bodyMetrics.clear();
            for (Object item : items) {
                if (item instanceof Workout) {
                    workouts.add((Workout) item);
                } else if (item instanceof BodyMetric) {
                    bodyMetrics.add((BodyMetric) item);
                }
            }

            notifyItemMoved(fromPosition, toPosition);

            Log.d("CombinedAdapter", "Moved item from position " + fromPosition + " to " + toPosition);
            Log.d("CombinedAdapter", "New order - workouts: " + workouts.size() + ", bodyMetrics: " + bodyMetrics.size());
            Log.d("CombinedAdapter", "Items after move: " + items.toString());

            activity.saveWorkouts();
            activity.saveBodyMetrics();
        }

        public void onItemDismiss(int position) {
            Object item = items.remove(position);
            if (item instanceof Workout) {
                workouts.remove(item);
            } else if (item instanceof BodyMetric) {
                bodyMetrics.remove(item);
            }
            notifyItemRemoved(position);
            Log.d("CombinedAdapter", "Dismissed item at position: " + position);
            activity.saveWorkouts();
            activity.saveBodyMetrics();
        }
    }
}