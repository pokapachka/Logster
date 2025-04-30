package com.example.logster;

import android.content.Intent;
import android.graphics.Color;
import android.icu.util.ULocale;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private BottomNavigationManager navManager;
    private TextView monthYearText;
    private GridLayout calendarGrid;
    private Calendar currentCalendar;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        navManager = new BottomNavigationManager(findViewById(R.id.calendar), this);
        navManager.setCurrentActivity("CalendarActivity");

        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        currentCalendar = Calendar.getInstance();

        // Инициализация жестов
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        // Настройка ScrollView для обработки жестов
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

        // Первоначальная загрузка календаря
        updateCalendar();
        hideSystemUI();
    }

    private void updateCalendar() {
        // 1. Обновляем заголовок
        String monthYear = getMonthNameNominative(currentCalendar) + " " + currentCalendar.get(Calendar.YEAR);
        monthYearText.setText(monthYear);

        // 2. Очищаем предыдущие дни
        calendarGrid.removeAllViews();

        // 3. Настраиваем календарь на первый день месяца
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 4. Вычисляем смещение для первого дня (для России - понедельник первый)
        int offset = (firstDayOfWeek + 5) % 7; // Оптимизированный расчет для понедельника

        // 5. Добавляем пустые ячейки в начале
        for (int i = 0; i < offset; i++) {
            addDayToCalendar("");
        }

        // 6. Добавляем дни месяца
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH) &&
                    (currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
                    (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)));
            addDayToCalendar(String.valueOf(day), isToday);
        }

        // 7. Добавляем пустые ячейки в конце (всего 6 недель)
        int totalCells = offset + daysInMonth;
        int remainingCells = 42 - totalCells; // 6 недель * 7 дней
        for (int i = 0; i < remainingCells; i++) {
            addDayToCalendar("");
        }
    }

    private void addDayToCalendar(String text) {
        addDayToCalendar(text, false);
    }

    private void addDayToCalendar(String text, boolean isToday) {
        TextView dayView = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();

        // Рассчитываем ширину ячейки
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int cellWidth = metrics.widthPixels / 7; // Ровно 1/7 экрана

        // Настройка параметров ячейки
        params.width = cellWidth;
        params.height = (int) (cellWidth * 1.2); // Немного выше, чем ширина
        params.setGravity(Gravity.CENTER);

        dayView.setLayoutParams(params);
        dayView.setText(text);
        dayView.setTextSize(16);
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextColor(Color.WHITE);

        // Стиль для текущего дня
        if (isToday) {
            dayView.setBackgroundResource(R.drawable.current_day);
            dayView.setTextColor(Color.BLACK);
        }

        calendarGrid.addView(dayView);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        // Свайп вниз - предыдущий месяц
                        showPreviousMonth();
                    } else {
                        // Свайп вверх - следующий месяц
                        showNextMonth();
                    }
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
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

    private String getMonthNameNominative(Calendar calendar) {
        final String[] MONTHS_NOMINATIVE = {
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };
        return MONTHS_NOMINATIVE[calendar.get(Calendar.MONTH)];
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

}