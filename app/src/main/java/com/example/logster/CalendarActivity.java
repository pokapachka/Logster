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
import android.widget.Toast;

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
import java.util.Collections;
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

        navManager = new BottomNavigationManager(findViewById(R.id.bottom_navigation), this);
        navManager.setCurrentActivity("CalendarActivity");

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        calendarSheet = new BottomSheets(this, R.layout.calendar_item);

        weeklySchedule = new HashMap<>();
        specificDates = new HashMap<>();
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
        calendarGrid.setClickable(false); // Отключаем кликабельность
        calendarGrid.setFocusable(false); // Отключаем фокус
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setHorizontalScrollBarEnabled(false);

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        scrollView.setOnTouchListener((v, event) -> {
            Log.d("SwipeGesture", "Событие касания на ScrollView: action=" + MotionEvent.actionToString(event.getAction()));
            gestureDetector.onTouchEvent(event);
            return false; // Пропускаем событие дальше
        });

        // Перенаправляем события касания с calendarGrid в GestureDetector
        calendarGrid.setOnTouchListener((v, event) -> {
            Log.d("SwipeGesture", "Событие касания на calendarGrid: action=" + MotionEvent.actionToString(event.getAction()));
            gestureDetector.onTouchEvent(event);
            return false; // Пропускаем событие для обработки кликов
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

        Map<String, Boolean> workoutDates = getWorkoutDatesForMonth(currentCalendar);

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
            boolean hasWorkout = workoutDates.containsKey(dateKey);
            boolean isCompleted = hasWorkout && workoutDates.get(dateKey);
            addDayToCalendar(String.valueOf(day), isToday, hasWorkout, isCompleted, dateKey);
        }
        int totalCells = offset + daysInMonth;
        int remainingCells = 42 - totalCells;
        for (int i = 0; i < remainingCells; i++) {
            addDayToCalendar("");
        }
    }

    private Map<String, Boolean> getWorkoutDatesForMonth(Calendar calendar) {
        Map<String, Boolean> workoutDates = new HashMap<>();
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        LocalDate today = LocalDate.now();
        LocalDate currentWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

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

            boolean hasValidWorkout = false;
            boolean isCompleted = false;

            // Проверка конкретных дат
            if (!specificWorkoutIds.isEmpty()) {
                for (String workoutId : specificWorkoutIds) {
                    for (Workout workout : workouts) {
                        if (workout.id.equals(workoutId)) {
                            hasValidWorkout = true;
                            if (workout.completedDates.containsKey(dateKey)) {
                                isCompleted = true;
                            }
                            break;
                        }
                    }
                    if (hasValidWorkout) break;
                }
            } else if (weeklyWorkoutId != null && !weeklyWorkoutId.isEmpty()) {
                // Для текущей недели учитываем тренировки до конца недели
                if (date.isAfter(today.minusDays(1)) && !date.isAfter(currentWeekEnd)) {
                    for (Workout workout : workouts) {
                        if (workout.id.equals(weeklyWorkoutId)) {
                            hasValidWorkout = true;
                            if (workout.completedDates.containsKey(dateKey)) {
                                isCompleted = true;
                            }
                            break;
                        }
                    }
                } else if (date.isAfter(currentWeekEnd)) {
                    // Для будущих недель учитываем только запланированный день
                    for (Workout workout : workouts) {
                        if (workout.id.equals(weeklyWorkoutId)) {
                            hasValidWorkout = true;
                            if (workout.completedDates.containsKey(dateKey)) {
                                isCompleted = true;
                            }
                            break;
                        }
                    }
                }
            }

            if (hasValidWorkout) {
                workoutDates.put(dateKey, isCompleted);
                Log.d("CalendarActivity", "Добавлена дата: " + dateKey + ", завершена: " + isCompleted);
            }
        }
        Log.d("CalendarActivity", "Даты тренировок за месяц: " + workoutDates);
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
        addDayToCalendar(text, false, false, false, "");
    }

    private void addDayToCalendar(String text, boolean isToday, boolean hasWorkout, boolean isCompleted, String dateKey) {
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
                    dpToPx(8), dpToPx(8)
            );
            dotParams.addRule(RelativeLayout.BELOW, dayView.getId());
            dotParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            dotParams.topMargin = dpToPx(2);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.circle);
            dot.getBackground().setTint(isCompleted ? Color.WHITE : Color.GRAY);
            dayContainer.addView(dot);
            Log.d("CalendarActivity", "Добавлен кружок для даты " + dateKey + ", завершена: " + isCompleted);
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

            dayContainer.setOnTouchListener((v, event) -> {
                Log.d("SwipeGesture", "Событие касания на dayContainer: action=" + MotionEvent.actionToString(event.getAction()));
                gestureDetector.onTouchEvent(event);
                return false;
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
            ScrollView scrollView = findViewById(R.id.scrollView);
            scrollView.requestFocus(); // Восстанавливаем фокус
            Log.d("CalendarActivity", "Фокус восстановлен на ScrollView после закрытия BottomSheet");
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

        if (workouts.isEmpty()) {
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

        for (Workout workout : workouts) {
            View workoutItem = LayoutInflater.from(this).inflate(R.layout.item_calendar, workoutsContainer, false);

            ImageView checkbox = workoutItem.findViewById(R.id.checkbox_selected);
            TextView exerciseName = workoutItem.findViewById(R.id.exercise_name_selected);
            TextView daysText = workoutItem.findViewById(R.id.days);
            TextView doneText = workoutItem.findViewById(R.id.done);

            exerciseName.setText(workout.name);

            // Получаем день недели для текущей даты (dateKey)
            String dayName = localDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("ru"));
            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1).toLowerCase();

            // Проверяем, запланирована ли тренировка на эту дату
            boolean isScheduled = specificWorkoutIds.contains(workout.id) ||
                    (weeklyWorkoutId != null && weeklyWorkoutId.equals(workout.id) && !specificDates.containsKey(dateKey));

            daysText.setText(isScheduled ? dayName : "Не запланировано");

            // Проверяем, завершена ли тренировка
            boolean isCompleted = workout.completedDates.containsKey(dateKey);
            doneText.setVisibility(isCompleted ? View.VISIBLE : View.GONE);

            // Устанавливаем состояние чекбокса
            boolean isChecked = specificWorkoutIds.contains(workout.id) ||
                    (weeklyWorkoutId != null && weeklyWorkoutId.equals(workout.id) && !specificDates.containsKey(dateKey));
            checkbox.setSelected(isChecked);

            // Обработчик клика по чекбоксу
            checkbox.setOnClickListener(v -> {
                if (!isFutureOrToday) {
                    Toast.makeText(this, "Нельзя изменять тренировки в прошлом", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean newCheckedState = !checkbox.isSelected();
                checkbox.setSelected(newCheckedState);

                // Получаем или создаём список тренировок для этой даты
                List<String> workoutIds = new ArrayList<>(specificWorkoutIds);

                if (newCheckedState) {
                    // Добавляем тренировку
                    if (!workoutIds.contains(workout.id)) {
                        workoutIds.add(workout.id);
                        specificDates.put(dateKey, workoutIds);
                        Log.d("CalendarActivity", "Добавлена тренировка " + workout.id + " на дату " + dateKey);
                    }
                } else {
                    // Удаляем тренировку
                    workoutIds.remove(workout.id);
                    if (workout.completedDates.containsKey(dateKey)) {
                        workout.completedDates.remove(dateKey);
                        saveWorkouts();
                        Log.d("CalendarActivity", "Удалён статус завершения для тренировки " + workout.id + " на дату " + dateKey);
                    }
                    // Сохраняем даже пустой список, чтобы переопределить weeklySchedule
                    specificDates.put(dateKey, workoutIds);
                    Log.d("CalendarActivity", "Удалена тренировка " + workout.id + " с даты " + dateKey + ", workoutIds=" + workoutIds);
                }

                // Сохраняем изменения
                saveSpecificDates();
                syncWorkoutDates();
                updateCalendar();
                updateWorkoutsList(dateKey, workoutsContainer, localDate, selectedDate); // Обновляем список
            });

            // Обработчик клика по всему элементу
            workoutItem.setOnClickListener(v -> openEditWorkoutSheet(workout.name, dateKey));

            workoutsContainer.addView(workoutItem);
        }
    }

    private void openEditWorkoutSheet(String workoutName, String dateKey) {
        if (calendarSheet != null && calendarSheet.isShowing()) {
            calendarSheet.hide(() -> {
                Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                intent.putExtra("edit_workout", workoutName);
                intent.putExtra("selected_date", dateKey); // Убедитесь, что dateKey не null
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent, options.toBundle());
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                Log.d("CalendarActivity", "Переход на редактирование тренировки: " + workoutName + " для даты: " + dateKey);
            });
        } else {
            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
            intent.putExtra("edit_workout", workoutName);
            intent.putExtra("selected_date", dateKey); // Убедитесь, что dateKey не null
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(intent, options.toBundle());
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            Log.d("CalendarActivity", "Прямой переход на редактирование тренировки: " + workoutName + " для даты: " + dateKey);
        }
    }

    private void syncWorkoutDates() {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for (Workout workout : workouts) {
            workout.dates.clear();
            Log.d("CalendarActivity", "Очищены даты для тренировки: " + workout.id);
        }

        // Добавляем даты из specificDates
        for (Map.Entry<String, List<String>> entry : specificDates.entrySet()) {
            String dateStr = entry.getKey();
            List<String> workoutIds = entry.getValue();
            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (!date.isBefore(today)) {
                    for (String workoutId : workoutIds) {
                        workouts.stream()
                                .filter(w -> w.id.equals(workoutId))
                                .findFirst()
                                .ifPresent(workout -> {
                                    workout.addDate(dateStr);
                                    Log.d("CalendarActivity", "Добавлена дата " + dateStr + " для тренировки " + workoutId);
                                });
                    }
                }
            } catch (Exception e) {
                Log.e("CalendarActivity", "Ошибка разбора даты: " + dateStr, e);
            }
        }

        // Добавляем еженедельные даты
        for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
            DayOfWeek dayOfWeek = entry.getKey();
            String workoutId = entry.getValue();
            if (workoutId == null || workoutId.isEmpty()) continue;
            LocalDate currentDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek));
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                List<String> specificWorkoutIds = specificDates.getOrDefault(dateStr, null);
                // Добавляем только если дата не переопределена в specificDates или содержит workoutId
                if (specificWorkoutIds == null || specificWorkoutIds.contains(workoutId)) {
                    workouts.stream()
                            .filter(w -> w.id.equals(workoutId))
                            .findFirst()
                            .ifPresent(workout -> {
                                workout.addDate(dateStr);
                                Log.d("CalendarActivity", "Добавлена еженедельная дата " + dateStr + " для тренировки " + workoutId);
                            });
                    // Обновляем specificDates только если список пустой
                    if (specificWorkoutIds == null) {
                        specificDates.put(dateStr, new ArrayList<>(Collections.singletonList(workoutId)));
                    }
                }
                currentDate = currentDate.plusWeeks(1);
            }
        }
        saveWorkouts();
        saveSpecificDates();
        Log.d("CalendarActivity", "Синхронизированы даты тренировок: specificDates=" + specificDates + ", weeklySchedule=" + weeklySchedule);
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
                JSONArray workoutIds = new JSONArray(entry.getValue());
                json.put(entry.getKey(), workoutIds);
            }
            editor.putString(SPECIFIC_DATES_KEY, json.toString());
            editor.apply();
            Log.d("CalendarActivity", "Сохранены конкретные даты: " + json.toString());
        } catch (JSONException e) {
            Log.e("CalendarActivity", "Ошибка сохранения дат: " + e.getMessage(), e);
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
                        String workoutId = workoutIdsJson.getString(i);
                        if (workouts.stream().anyMatch(w -> w.id.equals(workoutId))) {
                            workoutIds.add(workoutId);
                        } else {
                            Log.w("CalendarActivity", "Удалён невалидный workoutId: " + workoutId + " для даты " + key);
                        }
                    }
                    if (!workoutIds.isEmpty()) {
                        specificDates.put(key, workoutIds);
                    }
                }
                Log.d("CalendarActivity", "Загружены конкретные даты: " + specificDates);
            } catch (JSONException e) {
                Log.e("CalendarActivity", "Ошибка загрузки дат: " + e.getMessage(), e);
            }
        }
        // Очистка specificDates для дат с пустыми списками
        specificDates.entrySet().removeIf(entry -> {
            try {
                LocalDate.parse(entry.getKey());
                return entry.getValue().isEmpty();
            } catch (Exception e) {
                Log.e("CalendarActivity", "Ошибка разбора даты: " + entry.getKey(), e);
                return true;
            }
        });
        saveSpecificDates();
    }

    private void saveWeeklySchedule() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            JSONObject json = new JSONObject();
            Map<DayOfWeek, String> cleanedSchedule = new HashMap<>();
            Set<String> assignedWorkouts = new HashSet<>();

            // Очищаем дубли
            for (Map.Entry<DayOfWeek, String> entry : weeklySchedule.entrySet()) {
                String workoutId = entry.getValue();
                if (workoutId != null && !workoutId.isEmpty() && !assignedWorkouts.contains(workoutId)) {
                    cleanedSchedule.put(entry.getKey(), workoutId);
                    assignedWorkouts.add(workoutId);
                }
            }

            for (Map.Entry<DayOfWeek, String> entry : cleanedSchedule.entrySet()) {
                json.put(entry.getKey().toString(), entry.getValue());
            }
            editor.putString(WEEKLY_SCHEDULE_KEY, json.toString());
            editor.apply();
            weeklySchedule.clear();
            weeklySchedule.putAll(cleanedSchedule);
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
                Set<String> assignedWorkouts = new HashSet<>();
                for (DayOfWeek day : DayOfWeek.values()) {
                    String workoutId = json.optString(day.toString(), null);
                    if (workoutId != null && !workoutId.isEmpty() && !assignedWorkouts.contains(workoutId)) {
                        boolean workoutExists = workouts.stream().anyMatch(w -> w.id.equals(workoutId));
                        if (workoutExists) {
                            weeklySchedule.put(day, workoutId);
                            assignedWorkouts.add(workoutId);
                        } else {
                            Log.d("CalendarActivity", "Удалён некорректный ID тренировки " + workoutId + " из еженедельного расписания");
                        }
                    }
                }
                saveWeeklySchedule(); // Пересохраняем для очистки
                Log.d("CalendarActivity", "Загружено еженедельное расписание: " + weeklySchedule);
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
        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;
        private boolean isAnimating = false; // Добавлен для защиты от двойных свайпов

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isAnimating) {
                Log.d("SwipeGesture", "Свайп заблокирован: анимация в процессе");
                return false;
            }

            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > SWIPE_THRESHOLD
                        && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    isAnimating = true;
                    Log.d("SwipeGesture", "Обнаружен свайп: diffY=" + diffY + ", velocityY=" + velocityY);

                    Animation fadeOut = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_out);
                    fadeOut.setDuration(200);
                    calendarGrid.startAnimation(fadeOut);

                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            Log.d("SwipeGesture", "Начало анимации fade_out");
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (diffY > 0) { // Свайп вниз -> предыдущий месяц
                                Log.d("SwipeGesture", "Переход на предыдущий месяц");
                                showPreviousMonth();
                            } else { // Свайп вверх -> следующий месяц
                                Log.d("SwipeGesture", "Переход на следующий месяц");
                                showNextMonth();
                            }
                            Animation fadeIn = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_in); // Исправлена опечатка
                            fadeIn.setDuration(200);
                            calendarGrid.startAnimation(fadeIn);
                            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    Log.d("SwipeGesture", "Начало анимации fade_in");
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    isAnimating = false;
                                    Log.d("SwipeGesture", "Анимация завершена");
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {}
                            });
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    return true;
                }
            } catch (Exception e) {
                Log.e("SwipeGesture", "Ошибка свайпа", e);
                isAnimating = false;
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

    private Map<Integer, Integer> getWorkoutCountByWeek(LocalDate startDate, LocalDate endDate) {
        Map<Integer, Integer> weeklyCounts = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int currentWeekNumber = today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int weekNumber = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String dateKey = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

            List<String> specificWorkoutIds = specificDates.getOrDefault(dateKey, new ArrayList<>());
            String weeklyWorkoutId = weeklySchedule.get(date.getDayOfWeek());

            boolean hasWorkout = false;

            // Проверка specificDates
            if (!specificWorkoutIds.isEmpty()) {
                for (String workoutId : specificWorkoutIds) {
                    if (workouts.stream().anyMatch(w -> w.id.equals(workoutId))) {
                        hasWorkout = true;
                        break;
                    }
                }
            } else if (weeklyWorkoutId != null && !weeklyWorkoutId.isEmpty()) {
                // Проверка weeklySchedule для текущей недели до конца недели
                if (weekNumber == currentWeekNumber && date.isAfter(today.minusDays(1))) {
                    hasWorkout = workouts.stream().anyMatch(w -> w.id.equals(weeklyWorkoutId));
                } else if (weekNumber > currentWeekNumber) {
                    // Для будущих недель учитываем только конкретный день
                    hasWorkout = workouts.stream().anyMatch(w -> w.id.equals(weeklyWorkoutId));
                }
            }

            if (hasWorkout) {
                weeklyCounts.put(weekNumber, weeklyCounts.getOrDefault(weekNumber, 0) + 1);
            }
        }

        Log.d("CalendarActivity", "Подсчёт тренировок по неделям: " + weeklyCounts);
        return weeklyCounts;
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