package com.example.logster;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private BottomNavigationManager navManager;
    private TextView monthYearText;
    private GridLayout calendarGrid;
    private Calendar currentCalendar;
    private GestureDetector gestureDetector;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String WORKOUTS_KEY = "workouts";
    private static final String WEEKLY_SCHEDULE_KEY = "weekly_schedule";
    private static final String SPECIFIC_DATES_KEY = "specific_dates";
    private BottomSheets calendarSheet;
    private View selectedDayView;
    private Map<DayOfWeek, String> weeklySchedule;
    private Map<String, String> specificDates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        navManager = new BottomNavigationManager(findViewById(R.id.calendar), this);
        navManager.setCurrentActivity("CalendarActivity");

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calendarSheet = new BottomSheets(this, R.layout.calendar_item);

        weeklySchedule = new HashMap<>();
        specificDates = new HashMap<>();
        loadWeeklySchedule();
        loadSpecificDates();

        boolean goToToday = getIntent().getBooleanExtra("goToToday", false);
        currentCalendar = Calendar.getInstance();
        if (goToToday) {
            currentCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        updateCalendar();
        hideSystemUI();
    }

    private void updateCalendar() {
        String monthYear = getMonthNameNominative(currentCalendar) + " " + currentCalendar.get(Calendar.YEAR);
        monthYearText.setText(monthYear);

        calendarGrid.removeAllViews();

        currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int offset = (firstDayOfWeek + 5) % 7;

        Set<String> workoutDates = getWorkoutDatesForMonth(currentCalendar);

        for (int i = 0; i < offset; i++) {
            addDayToCalendar("");
        }
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH) &&
                    currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR));
            String dateKey = String.format("%d-%02d-%02d", currentCalendar.get(Calendar.YEAR),
                    currentCalendar.get(Calendar.MONTH) + 1, day);
            boolean hasWorkout = workoutDates.contains(dateKey);
            addDayToCalendar(String.valueOf(day), isToday, hasWorkout, dateKey);
        }
        int totalCells = offset + daysInMonth;
        int remainingCells = 42 - totalCells;
        for (int i = 0; i < remainingCells; i++) {
            addDayToCalendar("");
        }
    }

    private Set<String> getWorkoutDatesForMonth(Calendar calendar) {
        Set<String> workoutDates = new HashSet<>();
        List<MainActivity.Workout> workouts = loadWorkouts();
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String dateKey = String.format("%d-%02d-%02d", tempCal.get(Calendar.YEAR),
                    tempCal.get(Calendar.MONTH) + 1, day);
            LocalDate date;
            try {
                date = LocalDate.parse(dateKey);
            } catch (Exception e) {
                Log.e("CalendarActivity", "Error parsing date: " + dateKey, e);
                continue;
            }

            String specificWorkoutId = specificDates.get(dateKey);
            String weeklyWorkoutId = weeklySchedule.get(date.getDayOfWeek());
            Calendar today = Calendar.getInstance();
            boolean isFutureOrToday = tempCal.compareTo(today) >= 0;

            boolean hasValidWorkout = false;
            if (specificWorkoutId != null && !specificWorkoutId.isEmpty()) {
                for (MainActivity.Workout workout : workouts) {
                    if (workout.id.equals(specificWorkoutId)) {
                        hasValidWorkout = true;
                        break;
                    }
                }
                if (hasValidWorkout) {
                    workoutDates.add(dateKey);
                }
            } else if (weeklyWorkoutId != null && !weeklyWorkoutId.isEmpty() && isFutureOrToday) {
                for (MainActivity.Workout workout : workouts) {
                    if (workout.id.equals(weeklyWorkoutId)) {
                        hasValidWorkout = true;
                        break;
                    }
                }
                if (hasValidWorkout) {
                    workoutDates.add(dateKey);
                }
            }
        }
        Log.d("CalendarActivity", "Workout dates for month: " + workoutDates.size());
        return workoutDates;
    }

    private String getMonthNameNominative(Calendar calendar) {
        final String[] MONTHS_NOMINATIVE = {
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };
        return MONTHS_NOMINATIVE[calendar.get(Calendar.MONTH)];
    }

    private void addDayToCalendar(String text) {
        addDayToCalendar(text, false, false, "");
    }

    private void addDayToCalendar(String text, boolean isToday, boolean hasWorkout, String dateKey) {
        RelativeLayout dayContainer = new RelativeLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int cellWidth = metrics.widthPixels / 7;
        params.width = cellWidth;
        params.height = (int) (cellWidth * 1.2);
        params.setGravity(Gravity.CENTER);
        dayContainer.setLayoutParams(params);

        TextView dayView = new TextView(this);
        dayView.setId(View.generateViewId());
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        dayView.setLayoutParams(textParams);
        dayView.setText(text);
        dayView.setTextSize(16);
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextColor(Color.WHITE);
        if (isToday) {
            dayView.setBackgroundResource(R.drawable.current_day);
            dayView.setTextColor(Color.BLACK);
        }
        dayContainer.addView(dayView);

        if (hasWorkout && !text.isEmpty()) {
            View dot = new View(this);
            RelativeLayout.LayoutParams dotParams = new RelativeLayout.LayoutParams(
                    dpToPx(6), dpToPx(6)
            );
            dotParams.addRule(RelativeLayout.BELOW, dayView.getId());
            dotParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            dotParams.topMargin = dpToPx(2);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.circle);
            dot.getBackground().setTint(Color.GRAY);
            dayContainer.addView(dot);
        }

        if (!text.isEmpty()) {
            dayContainer.setOnClickListener(v -> {
                if (selectedDayView != null) {
                    selectedDayView.setBackground(null);
                    TextView prevDayText = selectedDayView.findViewById(selectedDayView.getId());
                    Calendar todayCheck = Calendar.getInstance();
                    boolean wasToday = Integer.parseInt(prevDayText.getText().toString()) == todayCheck.get(Calendar.DAY_OF_MONTH) &&
                            currentCalendar.get(Calendar.MONTH) == todayCheck.get(Calendar.MONTH) &&
                            currentCalendar.get(Calendar.YEAR) == todayCheck.get(Calendar.YEAR);
                    if (wasToday) {
                        prevDayText.setBackgroundResource(R.drawable.current_day);
                        prevDayText.setTextColor(Color.BLACK);
                    } else {
                        prevDayText.setTextColor(Color.WHITE);
                    }
                }

                dayView.setBackgroundResource(R.drawable.selected_day);
                dayView.setTextColor(Color.WHITE);
                selectedDayView = dayView;

                showCalendarItem(dateKey);
            });
        }

        calendarGrid.addView(dayContainer);
    }

    private void showCalendarItem(String dateKey) {
        hideSystemUI();
        View newView = LayoutInflater.from(this).inflate(R.layout.calendar_item, null);
        TextView dayOfWeek = newView.findViewById(R.id.day_of_week);
        TextView dateText = newView.findViewById(R.id.date_text);
        TextView todayIndicator = newView.findViewById(R.id.today_indicator);
        LinearLayout currentWorkoutsContainer = newView.findViewById(R.id.current_workouts_container);
        LinearLayout availableWorkoutsContainer = newView.findViewById(R.id.available_workouts_container);

        String[] dateParts = dateKey.split("-");
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]) - 1;
        int day = Integer.parseInt(dateParts[2]);
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        LocalDate localDate = LocalDate.of(year, month + 1, day);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("ru"));
        dayOfWeek.setText(dayFormat.format(selectedDate.getTime()));

        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("ru"));
        dateText.setText(dateFormat.format(selectedDate.getTime()));

        Calendar today = Calendar.getInstance();
        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                selectedDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
            todayIndicator.setVisibility(View.VISIBLE);
        } else {
            todayIndicator.setVisibility(View.GONE);
        }

        List<MainActivity.Workout> workouts = loadWorkouts();
        String specificWorkoutId = specificDates.get(dateKey);
        String weeklyWorkoutId = weeklySchedule.get(localDate.getDayOfWeek());
        List<MainActivity.Workout> currentWorkouts = new ArrayList<>();
        boolean isFutureOrToday = selectedDate.compareTo(today) >= 0;

        if (specificWorkoutId != null && !specificWorkoutId.isEmpty()) {
            for (MainActivity.Workout workout : workouts) {
                if (workout.id.equals(specificWorkoutId)) {
                    currentWorkouts.add(workout);
                    break;
                }
            }
        } else if (weeklyWorkoutId != null && !weeklyWorkoutId.isEmpty() && isFutureOrToday) {
            for (MainActivity.Workout workout : workouts) {
                if (workout.id.equals(weeklyWorkoutId)) {
                    currentWorkouts.add(workout);
                    break;
                }
            }
        }

        currentWorkoutsContainer.removeAllViews();
        for (MainActivity.Workout workout : currentWorkouts) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(workout.name);
            checkBox.setTextSize(16);
            checkBox.setTextColor(Color.WHITE);
            checkBox.setChecked(true);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    specificDates.remove(dateKey);
                    saveSpecificDates();
                    syncWorkoutDates();
                    updateCalendar();
                    Log.d("CalendarActivity", "Removed workout " + workout.id + " from date " + dateKey);
                }
            });
            currentWorkoutsContainer.addView(checkBox);
        }

        for (MainActivity.Workout workout : workouts) {
            boolean isCurrent = currentWorkouts.stream().anyMatch(w -> w.id.equals(workout.id));
            if (!isCurrent) {
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(workout.name);
                checkBox.setTextSize(16);
                checkBox.setTextColor(Color.WHITE);
                checkBox.setChecked(false);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        specificDates.put(dateKey, workout.id);
                        saveSpecificDates();
                        syncWorkoutDates();
                        updateCalendar();
                        Log.d("CalendarActivity", "Added workout " + workout.id + " to date " + dateKey);
                    }
                });
                availableWorkoutsContainer.addView(checkBox);
            }
        }

        calendarSheet.setContentView(newView);
        calendarSheet.showWithLimitedHeightAndCallback(() -> {
            if (selectedDayView != null) {
                selectedDayView.setBackground(null);
                TextView dayText = selectedDayView.findViewById(selectedDayView.getId());
                Calendar todayCheck = Calendar.getInstance();
                boolean isToday = Integer.parseInt(dayText.getText().toString()) == todayCheck.get(Calendar.DAY_OF_MONTH) &&
                        currentCalendar.get(Calendar.MONTH) == todayCheck.get(Calendar.MONTH) &&
                        currentCalendar.get(Calendar.YEAR) == todayCheck.get(Calendar.YEAR);
                if (isToday) {
                    dayText.setBackgroundResource(R.drawable.current_day);
                    dayText.setTextColor(Color.BLACK);
                } else {
                    dayText.setTextColor(Color.WHITE);
                }
                selectedDayView = null;
            }
        });
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        List<MainActivity.Workout> workouts = loadWorkouts();
        for (MainActivity.Workout workout : workouts) {
            workout.dates.clear();
        }

        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;

            boolean workoutExists = workouts.stream().anyMatch(w -> w.id.equals(workoutId));
            if (!workoutExists) {
                Log.d("CalendarActivity", "Skipping invalid workout ID " + workoutId + " in weekly schedule");
                continue;
            }

            LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                String specificWorkoutId = specificDates.get(dateStr);
                if (specificWorkoutId == null || specificWorkoutId.equals(workoutId)) {
                    for (MainActivity.Workout workout : workouts) {
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

            boolean workoutExists = workouts.stream().anyMatch(w -> w.id.equals(workoutId));
            if (!workoutExists) {
                Log.d("CalendarActivity", "Skipping invalid workout ID " + workoutId + " in specific dates");
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (!date.isBefore(today)) {
                    for (MainActivity.Workout workout : workouts) {
                        if (workout.id.equals(workoutId)) {
                            workout.addDate(dateStr);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("CalendarActivity", "Error parsing date in syncWorkoutDates: " + dateStr, e);
            }
        }

        saveWorkouts();
        Log.d("CalendarActivity", "Synced workouts: " + workouts.size() + " with dates.");
    }

    private List<MainActivity.Workout> loadWorkouts() {
        List<MainActivity.Workout> workouts = new ArrayList<>();
        String workoutsJson = sharedPreferences.getString(WORKOUTS_KEY, null);
        if (workoutsJson != null) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                for (int i = 0; i < workoutsArray.length(); i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    MainActivity.Workout workout = MainActivity.Workout.fromJson(workoutJson);
                    workouts.add(workout);
                }
                Log.d("CalendarActivity", "Loaded workouts: " + workouts.size());
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Error loading workouts: " + e.getMessage(), e);
            }
        }
        return workouts;
    }

    private void saveWorkouts() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONArray workoutsArray = new JSONArray();
            List<MainActivity.Workout> workouts = loadWorkouts();
            for (MainActivity.Workout workout : workouts) {
                workoutsArray.put(workout.toJson());
            }
            editor.putString(WORKOUTS_KEY, workoutsArray.toString());
            editor.apply();
            Log.d("CalendarActivity", "Saved workouts: " + workoutsArray.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Error saving workouts: " + e.getMessage(), e);
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
            Log.d("CalendarActivity", "Saved specific dates: " + json.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Error saving specific dates: " + e.getMessage(), e);
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
                Log.d("CalendarActivity", "Loaded specific dates: " + specificDates.size());
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Error loading specific dates: " + e.getMessage(), e);
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
            Log.d("CalendarActivity", "Saved weekly schedule: " + json.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Error saving weekly schedule: " + e.getMessage(), e);
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
                Log.d("CalendarActivity", "Loaded weekly schedule: " + weeklySchedule.size());
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Error loading weekly schedule: " + e.getMessage(), e);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    Animation fadeOut = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_out);
                    fadeOut.setDuration(200);
                    calendarGrid.startAnimation(fadeOut);

                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (diffY > 0) {
                                showPreviousMonth();
                            } else {
                                showNextMonth();
                            }
                            Animation fadeIn = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_in);
                            fadeIn.setDuration(200);
                            calendarGrid.startAnimation(fadeIn);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    return true;
                }
            } catch (Exception e) {
                Log.e("SwipeGesture", "Swipe error", e);
            }
            return false;
        }
    }

    private void showNextMonth() {
        currentCalendar.add(Calendar.MONTH, 1);
        updateCalendar();
    }

    private void showPreviousMonth() {
        currentCalendar.add(Calendar.MONTH, -1);
        updateCalendar();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean goToToday = intent.getBooleanExtra("goToToday", false);
        if (goToToday) {
            currentCalendar = Calendar.getInstance();
            Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOut.setDuration(200);
            calendarGrid.startAnimation(fadeOut);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateCalendar();
                    Animation fadeIn = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_in);
                    fadeIn.setDuration(200);
                    calendarGrid.startAnimation(fadeIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
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
}