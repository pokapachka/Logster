package com.example.logster;

import android.content.Intent;
import android.graphics.Color;
import android.icu.util.ULocale;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

        boolean goToToday = getIntent().getBooleanExtra("goToToday", false);
        if (goToToday) {
            currentCalendar = Calendar.getInstance();
        }
        monthYearText = findViewById(R.id.monthYearText);
        calendarGrid = findViewById(R.id.calendarGrid);
        currentCalendar = Calendar.getInstance();
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
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
        for (int i = 0; i < offset; i++) {
            addDayToCalendar("");
        }
        Calendar today = Calendar.getInstance();
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH) &&
                    (currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
                    (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)));
            addDayToCalendar(String.valueOf(day), isToday);
        }
        int totalCells = offset + daysInMonth;
        int remainingCells = 42 - totalCells;
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
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int cellWidth = metrics.widthPixels / 7;
        params.width = cellWidth;
        params.height = (int) (cellWidth * 1.2);
        params.setGravity(Gravity.CENTER);
        dayView.setLayoutParams(params);
        dayView.setText(text);
        dayView.setTextSize(16);
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextColor(Color.WHITE);
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
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean goToToday = intent.getBooleanExtra("goToToday", false);
        if (goToToday) {
            currentCalendar = Calendar.getInstance();
            Animation fadeOut = AnimationUtils.loadAnimation(CalendarActivity.this, android.R.anim.fade_out);
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