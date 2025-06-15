package com.example.logster;

import android.app.ActivityOptions;
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
import android.widget.GridLayout;
import android.widget.ImageView;
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
    private Map<String, List<String>> specificDates; // Updated type
    private List<Workout> workouts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        navManager = new BottomNavigationManager(findViewById(R.id.calendar), this);
        navManager.setCurrentActivity("CalendarActivity");

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calendarSheet = new BottomSheets(this, R.layout.calendar_item);

        weeklySchedule = new HashMap<>();
        specificDates = new HashMap<>(); // Initialize as Map<String, List<String>>
        workouts = new ArrayList<>();

        workouts = loadWorkouts();
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
                Log.e("CalendarActivity", "Ошибка разбора даты: " + dateKey, e);
                continue;
            }

            List<String> specificWorkoutIds = specificDates.getOrDefault(dateKey, new ArrayList<>());
            String weeklyWorkoutId = weeklySchedule.get(date.getDayOfWeek());
            Calendar today = Calendar.getInstance();
            boolean isFutureOrToday = tempCal.compareTo(today) >= 0;

            boolean hasValidWorkout = false;
            // Если дата есть в specificDates, проверяем только её
            if (specificDates.containsKey(dateKey)) {
                if (!specificWorkoutIds.isEmpty()) {
                    for (String workoutId : specificWorkoutIds) {
                        for (Workout workout : workouts) {
                            if (workout.id.equals(workoutId)) {
                                hasValidWorkout = true;
                                break;
                            }
                        }
                        if (hasValidWorkout) break;
                    }
                }
                // Если specificWorkoutIds пуст, тренировки нет
            } else if (weeklyWorkoutId != null && !weeklyWorkoutId.isEmpty() && isFutureOrToday) {
                // Проверяем weeklySchedule только если нет переопределения в specificDates
                for (Workout workout : workouts) {
                    if (workout.id.equals(weeklyWorkoutId)) {
                        hasValidWorkout = true;
                        break;
                    }
                }
            }

            if (hasValidWorkout) {
                workoutDates.add(dateKey);
            }
        }
        Log.d("CalendarActivity", "Даты тренировок за месяц: " + workoutDates.size());
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
        LinearLayout workoutsContainer = newView.findViewById(R.id.workouts_container);

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

        updateWorkoutsList(dateKey, workoutsContainer, localDate, selectedDate);

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
                Log.d("CalendarActivity", "Bottom sheet закрыт, выделение дня сброшено");
            }
        });
        Log.d("CalendarActivity", "Показан bottom sheet для даты: " + dateKey);
    }

    private void updateWorkoutsList(String dateKey, LinearLayout workoutsContainer, LocalDate localDate, Calendar selectedDate) {
        workoutsContainer.removeAllViews();

        List<String> specificWorkoutIds = specificDates.getOrDefault(dateKey, new ArrayList<>());
        String weeklyWorkoutId = weeklySchedule.get(localDate.getDayOfWeek());
        Calendar today = Calendar.getInstance();
        boolean isFutureOrToday = selectedDate.compareTo(today) >= 0;
        boolean isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                selectedDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

        List<Workout> todayWorkouts = new ArrayList<>();
        List<Workout> otherWorkouts = new ArrayList<>();

        for (Workout workout : workouts) {
            boolean isActive = specificWorkoutIds.contains(workout.id) ||
                    (weeklyWorkoutId != null && weeklyWorkoutId.equals(workout.id) && isFutureOrToday);
            if (isToday && isActive) {
                todayWorkouts.add(workout);
            } else {
                otherWorkouts.add(workout);
            }
        }

        List<Workout> sortedWorkouts = new ArrayList<>();
        sortedWorkouts.addAll(todayWorkouts);
        sortedWorkouts.addAll(otherWorkouts);

        if (sortedWorkouts.isEmpty()) {
            TextView emptyMessage = new TextView(this);
            emptyMessage.setText("Сначала нужно создать тренировку");
            emptyMessage.setTextSize(16);
            emptyMessage.setTextColor(Color.WHITE);
            emptyMessage.setGravity(Gravity.CENTER);
            emptyMessage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            emptyMessage.setPadding(0, dpToPx(20), 0, dpToPx(20));
            workoutsContainer.addView(emptyMessage);
            return;
        }

        for (Workout workout : sortedWorkouts) {
            View workoutItem = LayoutInflater.from(this).inflate(R.layout.item_calendar, workoutsContainer, false);

            ImageView checkbox = workoutItem.findViewById(R.id.checkbox_selected);
            TextView exerciseName = workoutItem.findViewById(R.id.exercise_name_selected);
            TextView daysText = workoutItem.findViewById(R.id.days);

            exerciseName.setText(workout.name);

            List<String> workoutDays = new ArrayList<>();
            for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
                if (entry.getValue() != null && entry.getValue().equals(workout.id)) {
                    String dayName = entry.getKey().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"));
                    workoutDays.add(dayName.substring(0, 1).toUpperCase() + dayName.substring(1).toLowerCase());
                }
            }
            daysText.setText(workoutDays.isEmpty() ? "Нет регулярных дней" : String.join(", ", workoutDays));

            boolean isChecked = specificWorkoutIds.contains(workout.id) ||
                    (weeklyWorkoutId != null && weeklyWorkoutId.equals(workout.id) && isFutureOrToday && !specificDates.containsKey(dateKey));
            checkbox.setSelected(isChecked);

            checkbox.setOnClickListener(v -> {
                boolean newCheckedState = !checkbox.isSelected();
                checkbox.setSelected(newCheckedState);

                List<String> workoutIds = specificDates.getOrDefault(dateKey, new ArrayList<>());
                if (newCheckedState) {
                    if (!workoutIds.contains(workout.id)) {
                        workoutIds.add(workout.id);
                        specificDates.put(dateKey, workoutIds);
                        Log.d("CalendarActivity", "Добавлена тренировка " + workout.id + " на дату " + dateKey);
                    }
                } else {
                    workoutIds.remove(workout.id);
                    specificDates.put(dateKey, workoutIds);
                    if (workoutIds.isEmpty()) {
                        Log.d("CalendarActivity", "Дата " + dateKey + " теперь без тренировок");
                    } else {
                        Log.d("CalendarActivity", "Удалена тренировка " + workout.id + " с даты " + dateKey);
                    }
                }
                saveSpecificDates();
                syncWorkoutDates();
                updateCalendar();
                updateWorkoutsList(dateKey, workoutsContainer, localDate, selectedDate);
            });

            // Новый обработчик клика на весь блок
            workoutItem.setOnClickListener(v -> openEditWorkoutSheet(workout.name));

            workoutsContainer.addView(workoutItem);
        }
    }
    private void openEditWorkoutSheet(String workoutName) {
        if (calendarSheet != null && calendarSheet.isShowing()) {
            calendarSheet.hide(() -> {
                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                intent.putExtra("edit_workout", workoutName);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent, options.toBundle());
                Log.d("CalendarActivity", "Переход на редактирование тренировки: " + workoutName);
            });
        } else {
            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
            intent.putExtra("edit_workout", workoutName);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(intent, options.toBundle());
            Log.d("CalendarActivity", "Прямой переход на редактирование тренировки (sheet не отображается): " + workoutName);
        }
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (Workout workout : workouts) workout.dates.clear();

        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            List<String> workoutIds = entry.getValue();
            if (workoutIds == null || workoutIds.isEmpty()) continue;
            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (!date.isBefore(today)) {
                    for (String workoutId : workoutIds) {
                        workouts.stream()
                                .filter(w -> w.id.equals(workoutId))
                                .findFirst()
                                .ifPresent(workout -> workout.addDate(dateStr));
                    }
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
                List<String> specificWorkoutIds = specificDates.getOrDefault(dateStr, new ArrayList<>());
                if (!specificWorkoutIds.contains(workoutId)) {
                    workouts.stream()
                            .filter(w -> w.id.equals(workoutId))
                            .findFirst()
                            .ifPresent(workout -> workout.addDate(dateStr));
                }
                currentDate = currentDate.plusWeeks(1);
            }
        }
        saveWorkouts();
        Log.d("MainActivity", "Синхронизированы даты тренировок");
    }

    private List<Workout> loadWorkouts() {
        List<Workout> loadedWorkouts = new ArrayList<>();
        String workoutsJson = sharedPreferences.getString(WORKOUTS_KEY, null);
        if (workoutsJson != null) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                for (int i = 0; i < workoutsArray.length(); i++) {
                    JSONObject workoutJson = workoutsArray.getJSONObject(i);
                    Workout workout = Workout.fromJson(workoutJson);
                    loadedWorkouts.add(workout);
                }
                Log.d("CalendarActivity", "Загружено тренировок: " + loadedWorkouts.size());
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Ошибка загрузки тренировок: " + e.getMessage(), e);
            }
        }
        return loadedWorkouts;
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
            Log.d("CalendarActivity", "Сохранены тренировки: " + workoutsArray.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Ошибка сохранения тренировок: " + e.getMessage(), e);
        }
    }

    private void saveSpecificDates() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject json = new JSONObject();
            for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
                if (!entry.getValue().isEmpty()) { // Save only non-empty lists
                    JSONArray workoutIds = new JSONArray(entry.getValue());
                    json.put(entry.getKey(), workoutIds);
                }
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
                    JSONArray workoutIdsJson = json.getJSONArray(key);
                    List<String> workoutIds = new ArrayList<>();
                    for (int i = 0; i < workoutIdsJson.length(); i++) {
                        workoutIds.add(workoutIdsJson.getString(i));
                    }
                    specificDates.put(key, workoutIds);
                }
                Log.d("MainActivity", "Загружены конкретные даты: " + specificDates.size());
            } catch (JSONException e) {
                Log.e("MainActivity", "Ошибка загрузки дат: " + e.getMessage(), e);
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
            Log.d("CalendarActivity", "Сохранено еженедельное расписание: " + json.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Ошибка сохранения еженедельного расписания: " + e.getMessage(), e);
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
                        boolean workoutExists = workouts.stream().anyMatch(w -> w.id.equals(workoutId));
                        if (workoutExists) {
                            weeklySchedule.put(day, workoutId);
                        } else {
                            Log.d("CalendarActivity", "Удалён некорректный ID тренировки " + workoutId + " из еженедельного расписания");
                            json.remove(day.toString());
                        }
                    }
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(WEEKLY_SCHEDULE_KEY, json.toString());
                editor.apply();
                Log.d("CalendarActivity", "Загружено еженедельное расписание: " + weeklySchedule.size());
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Ошибка загрузки еженедельного расписания: " + e.getMessage(), e);
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
                Log.e("SwipeGesture", "Ошибка свайпа", e);
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