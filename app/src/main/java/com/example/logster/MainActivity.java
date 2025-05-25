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
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
    private static final String WEEKLY_SCHEDULE_KEY = "weekly_schedule";
    private static final String SPECIFIC_DATES_KEY = "specific_dates";

    private Map<DayOfWeek, String> weeklySchedule;
    private Map<String, String> specificDates;

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

    public static class Workout {
        public String id;
        public String name;
        public List<String> dates;

        public Workout(String id, String name) {
            this.id = id;
            this.name = name;
            this.dates = new ArrayList<>();
        }

        public Workout(String id, String name, List<String> dates) {
            this.id = id;
            this.name = name;
            this.dates = new ArrayList<>(dates);
        }

        public void addDate(String date) {
            if (!dates.contains(date)) {
                dates.add(date);
            }
        }

        public void removeDate(String date) {
            dates.remove(date);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            JSONArray datesArray = new JSONArray();
            for (String date : dates) {
                datesArray.put(date);
            }
            json.put("dates", datesArray);
            return json;
        }

        public static Workout fromJson(JSONObject json) throws JSONException {
            String id = json.optString("id", UUID.randomUUID().toString());
            String name = json.getString("name");
            Workout workout = new Workout(id, name);
            JSONArray datesArray = json.getJSONArray("dates");
            for (int i = 0; i < datesArray.length(); i++) {
                workout.dates.add(datesArray.getString(i));
            }
            return workout;
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
        weeklySchedule = new HashMap<>();
        specificDates = new HashMap<>();

        workoutRecyclerView = findViewById(R.id.workout_recycler_view);
        workoutRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        workoutRecyclerView.setNestedScrollingEnabled(true);

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
                Object item = combinedAdapter.items.get(position);
                if (item instanceof Workout) {
                    Workout workout = (Workout) item;
                    workouts.remove(workout);
                    removeWorkoutFromSchedules(workout.id);
                    saveWorkouts();
                    saveWeeklySchedule();
                    saveSpecificDates();
                    syncWorkoutDates();
                    combinedAdapter.updateData(workouts, bodyMetrics);
                    Log.d("MainActivity", "Removed workout: " + workout.name + " with ID: " + workout.id);
                }
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
                widgetSheet.hide(null);
            }
        });

        loadWorkouts();
        loadBodyMetrics();
        loadWeeklySchedule();
        loadSpecificDates();
        syncWorkoutDates();
        combinedAdapter.updateData(workouts, bodyMetrics);
        hideSystemUI();
    }

    private void removeWorkoutFromSchedules(String workoutId) {
        Iterator<Map.Entry<DayOfWeek, String>> weeklyIter = weeklySchedule.entrySet().iterator();
        while (weeklyIter.hasNext()) {
            Map.Entry<DayOfWeek, String> entry = weeklyIter.next();
            if (entry.getValue().equals(workoutId)) {
                weeklyIter.remove();
                Log.d("MainActivity", "Removed workout ID " + workoutId + " from weekly schedule on " + entry.getKey());
            }
        }
        Iterator<Map.Entry<String, String>> specificIter = specificDates.entrySet().iterator();
        while (specificIter.hasNext()) {
            Map.Entry<String, String> entry = specificIter.next();
            if (entry.getValue().equals(workoutId)) {
                specificIter.remove();
                Log.d("MainActivity", "Removed workout ID " + workoutId + " from specific date " + entry.getKey());
            }
        }
        saveWeeklySchedule();
        saveSpecificDates();
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
        if (widgetSheet != null && widgetSheet.isShowing()) {
            widgetSheet.hide(null);
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
        combinedAdapter.updateData(workouts, bodyMetrics);
    }

    public void addWidgets(View view) {
        selectedWorkoutName = null;
        if (selectedDays != null) {
            selectedDays.clear();
        } else {
            selectedDays = new ArrayList<>();
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
        if (widgetSheet != null && widgetSheet.isShowing() && widgetSheet.getContentView() != null) {
            com.google.android.flexbox.FlexboxLayout daysContainer = widgetSheet.getContentView().findViewById(R.id.days_container);
            if (daysContainer != null) {
                int[] dayIds = {R.id.day_monday, R.id.day_tuesday, R.id.day_wednesday,
                        R.id.day_thursday, R.id.day_friday, R.id.day_saturday, R.id.day_sunday};
                for (int id : dayIds) {
                    TextView dayTextView = daysContainer.findViewById(id);
                    if (dayTextView != null) {
                        dayTextView.setTag(false);
                        dayTextView.getBackground().setTint(COLOR_UNSELECTED);
                    }
                }
            }
        }
        widgetSheet.hide(null);
    }

    private Map<String, List<String>> getWorkoutCountByDay() {
        Map<String, List<String>> workoutCountByDay = new HashMap<>();
        String[] days = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        for (String day : days) {
            workoutCountByDay.put(day, new ArrayList<>());
        }

        LocalDate today = LocalDate.now();
        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            String dayName = entry.getKey().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"));
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
            String workoutId = entry.getValue();
            if (workoutId != null && !workoutId.isEmpty()) {
                for (Workout workout : workouts) {
                    if (workout.id.equals(workoutId)) {
                        workoutCountByDay.get(dayName).add(workout.name);
                        break;
                    }
                }
            }
        }

        for (Map.Entry<String, String> entry : specificDates.entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                if (!date.isBefore(today)) {
                    String dayName = date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL, new Locale("ru"));
                    dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
                    String workoutId = entry.getValue();
                    if (workoutId != null && !workoutId.isEmpty()) {
                        for (Workout workout : workouts) {
                            if (workout.id.equals(workoutId)) {
                                workoutCountByDay.get(dayName).add(workout.name);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error parsing date in getWorkoutCountByDay: " + entry.getKey(), e);
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

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
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
                Log.e("MainActivity", "Error parsing date in getNearestDay: " + dateStr, e);
            }
        }

        if (nearestDate == null) {
            return "Не выбрано";
        }

        return nearestDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru")).toLowerCase();
    }

    private void saveWorkouts() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray workoutsArray = new JSONArray();
            for (Workout workout : workouts) {
                workoutsArray.put(workout.toJson());
            }
            editor.putString(WORKOUTS_KEY, workoutsArray.toString());
            editor.apply();
            Log.d("MainActivity", "Saved workouts: " + workoutsArray.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving workouts: " + e.getMessage(), e);
        }
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
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading workouts: " + e.getMessage(), e);
            }
        }
    }

    private void saveBodyMetrics() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray metricsArray = new JSONArray();
            for (BodyMetric metric : bodyMetrics) {
                metricsArray.put(metric.toJson());
            }
            editor.putString(METRICS_KEY, metricsArray.toString());
            editor.apply();
            Log.d("MainActivity", "Saved body metrics: " + metricsArray.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving body metrics: " + e.getMessage(), e);
        }
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
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading body metrics: " + e.getMessage(), e);
            }
        }
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
            Log.d("MainActivity", "Saved weekly schedule: " + json.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving weekly schedule: " + e.getMessage(), e);
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
                    if (workoutId != null) {
                        weeklySchedule.put(day, workoutId);
                    }
                }
                Log.d("MainActivity", "Loaded weekly schedule: " + weeklySchedule.size());
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading weekly schedule: " + e.getMessage(), e);
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
            Log.d("MainActivity", "Saved specific dates: " + json.toString());
        } catch (JSONException e) {
            Log.e("MainActivity", "Error saving specific dates: " + e.getMessage(), e);
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
                    String workoutId = json.getString(key);
                    specificDates.put(key, workoutId);
                }
                Log.d("MainActivity", "Loaded specific dates: " + specificDates.size());
            } catch (JSONException e) {
                Log.e("MainActivity", "Error loading specific dates: " + e.getMessage(), e);
            }
        }
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        for (Workout workout : workouts) {
            workout.dates.clear();
        }

        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;

            LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                String specificWorkoutId = specificDates.get(dateStr);
                if (specificWorkoutId == null || specificWorkoutId.equals(workoutId)) {
                    for (Workout workout : workouts) {
                        if (workout.id.equals(workoutId)) {
                            workout.addDate(dateStr);
                            break;
                        }
                    }
                }
                currentDate = currentDate.plusWeeks(1);
            }
        }

        for (Map.Entry<String, String> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;

            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (!date.isBefore(today)) {
                    for (Workout workout : workouts) {
                        if (workout.id.equals(workoutId)) {
                            workout.addDate(dateStr);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error parsing date in syncWorkoutDates: " + dateStr, e);
            }
        }

        saveWorkouts();
        Log.d("MainActivity", "Synced workouts: " + workouts.size() + " with dates.");
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
        }
        switchSheet(R.layout.edit_workout, workoutName, false, 0, 0);
    }

    public void backSheetEditWorkout(View view) {
        widgetSheet.hide(null);
    }

    public void openCalendarForDate(LocalDate date) {
        Intent intent = new Intent(this, CalendarActivity.class);
        intent.putExtra("goToToday", date.equals(LocalDate.now()));
        startActivity(intent);
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
                    scrollView, trainingList, topLine, bottomLine,
                    centerArrow, workoutNameTextView, workoutDescriptionTextView
            );
        } else if (layoutId == R.layout.add_workout2) {
            EditText workoutNameEditText = newView.findViewById(R.id.ETworkout_name);
            if (data != null) {
                workoutNameEditText.setText(data);
            }
            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> openAddWorkout3(v, workoutNameEditText));
            }
        } else if (layoutId == R.layout.add_workout3) {
            if (selectedDays == null) {
                selectedDays = new ArrayList<>();
            } else {
                selectedDays.clear();
            }

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

                int workoutCount = workoutsForDay != null ? workoutsForDay.size() : 0;
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

            View continueBtn = newView.findViewById(R.id.continue_btn);
            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> {
                    if (!selectedWorkoutName.isEmpty()) {
                        if (selectedDays.isEmpty()) {
                            Toast.makeText(this, "Выберите хотя бы один день недели", Toast.LENGTH_SHORT).show();
                            return; // Остаёмся на экране
                        }
                        try {
                            String workoutId = UUID.randomUUID().toString();
                            for (String dayName : selectedDays) {
                                DayOfWeek dayOfWeek = DAY_OF_WEEK_MAP.get(dayName.toUpperCase());
                                weeklySchedule.put(dayOfWeek, workoutId);
                            }
                            saveWeeklySchedule();
                            Workout newWorkout = new Workout(workoutId, selectedWorkoutName);
                            workouts.add(newWorkout);
                            saveWorkouts();
                            syncWorkoutDates();
                            combinedAdapter.updateData(workouts, bodyMetrics);
                            Log.d("MainActivity", "Added workout: " + selectedWorkoutName + " with ID: " + workoutId);
                            widgetSheet.hide(null);
                        } catch (Exception e) {
                            Log.e("MainActivity", "Error adding workout: " + e.getMessage(), e);
                            Toast.makeText(this, "Ошибка при добавлении тренировки", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Введите название тренировки", Toast.LENGTH_SHORT).show();
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

            com.example.logster.AddBodyMetric.setupMetricSelection(
                    scrollView, metricList, topLine, bottomLine,
                    centerArrow, metricNameTextView, metricDescriptionTextView
            );
        } else if (layoutId == R.layout.add_metric2) {
            ScrollView mainScrollView = newView.findViewById(R.id.main_value_scroll);
            LinearLayout mainValueList = newView.findViewById(R.id.main_value_list);
            View mainTopLine = newView.findViewById(R.id.main_top_line);
            View mainBottomLine = newView.findViewById(R.id.main_bottom_line);
            ImageView centerArrow = newView.findViewById(R.id.center_arrow);

            com.example.logster.AddBodyMetric.setupNumberPicker(
                    mainScrollView, mainValueList, mainTopLine,
                    mainBottomLine, centerArrow, data
            );
        } else if (layoutId == R.layout.edit_workout) {
            TextView titleTextView = newView.findViewById(R.id.title);
            if (titleTextView != null && data != null) {
                titleTextView.setText(data);
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
        }

        public void updateData(List<Workout> newWorkouts, List<BodyMetric> newBodyMetrics) {
            this.workouts = newWorkouts;
            this.bodyMetrics = newBodyMetrics;
            this.items.clear();
            this.items.addAll(workouts);
            this.items.addAll(bodyMetrics);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof Workout ? TYPE_WORKOUT : TYPE_METRIC;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget, parent, false);
            return viewType == TYPE_WORKOUT ? new WorkoutAdapter.WorkoutViewHolder(view) : new BodyMetricAdapter.MetricViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = items.get(position);
            if (holder instanceof WorkoutAdapter.WorkoutViewHolder) {
                Workout workout = (Workout) item;
                WorkoutAdapter.WorkoutViewHolder workoutHolder = (WorkoutAdapter.WorkoutViewHolder) holder;
                workoutHolder.workoutName.setText(workout.name);
                workoutHolder.workoutName.setVisibility(View.VISIBLE);
                workoutHolder.workoutDay.setText(activity.getNearestDay(workout.id, workout.dates));
                workoutHolder.workoutDay.setVisibility(View.VISIBLE);
                workoutHolder.workoutCount.setVisibility(View.GONE);
                workoutHolder.itemView.setOnClickListener(v -> activity.editWorkout(workout.name));
            } else if (holder instanceof BodyMetricAdapter.MetricViewHolder) {
                BodyMetric metric = (BodyMetric) item;
                BodyMetricAdapter.MetricViewHolder metricHolder = (BodyMetricAdapter.MetricViewHolder) holder;
                String displayValue = metric.type.toLowerCase().equals("вес") ? metric.value + "кг" :
                        metric.type.toLowerCase().equals("рост") ? metric.value + "см" : metric.value;
                metricHolder.workoutCount.setText(displayValue);
                metricHolder.workoutCount.setVisibility(View.VISIBLE);
                metricHolder.workoutDay.setText(activity.formatDate(metric.timestamp));
                metricHolder.workoutDay.setVisibility(View.VISIBLE);
                metricHolder.workoutName.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
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
            activity.saveWorkouts();
            activity.saveBodyMetrics();
        }
    }
}