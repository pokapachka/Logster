package com.example.logster;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.logster.CalendarActivity;
import com.example.logster.MainActivity;
import com.example.logster.R;
import com.example.logster.StatisticsActivity;
import com.example.logster.ChatActivity;

public class BottomNavigationManager {
    private ImageView navHome;
    private ImageView navCalendar;
    private ImageView navStatistics;
    private ImageView navChat;
    private Context context;

    private ImageView currentActiveButton; // Отслеживаем текущую активную кнопку

    public BottomNavigationManager(View rootView, Context context) {
        this.context = context;
        navHome = rootView.findViewById(R.id.nav_home);
        navCalendar = rootView.findViewById(R.id.nav_calendar);
        navStatistics = rootView.findViewById(R.id.nav_statistics);
        navChat = rootView.findViewById(R.id.nav_chat);

        // Инициализируем текущую активную кнопку
        currentActiveButton = navHome; // По умолчанию кнопка "Home" активна

        setupListeners();
        setActiveButton(currentActiveButton); // Устанавливаем выделение для "Home" на старте
    }

    private void setupListeners() {
        navHome.setOnClickListener(v -> {
            if (currentActiveButton != navHome) {
                setActiveButton(navHome);
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
            }
        });

        navCalendar.setOnClickListener(v -> {
            if (currentActiveButton != navCalendar) {
                setActiveButton(navCalendar);
                Intent intent = new Intent(context, CalendarActivity.class);
                context.startActivity(intent);
            }
        });

        navStatistics.setOnClickListener(v -> {
            if (currentActiveButton != navStatistics) {
                setActiveButton(navStatistics);
                Intent intent = new Intent(context, StatisticsActivity.class);
                context.startActivity(intent);
            }
        });

        navChat.setOnClickListener(v -> {
            if (currentActiveButton != navChat) {
                setActiveButton(navChat);
                Intent intent = new Intent(context, ChatActivity.class);
                context.startActivity(intent);
            }
        });
    }

    public void setActiveButton(ImageView activeButton) {
        // Сбрасываем выделение всех кнопок
        navHome.setSelected(false);
        navCalendar.setSelected(false);
        navStatistics.setSelected(false);
        navChat.setSelected(false);

        // Плавное затемнение неактивных кнопок
        animateButton(navHome, false);
        animateButton(navCalendar, false);
        animateButton(navStatistics, false);
        animateButton(navChat, false);

        // Устанавливаем выделение активной кнопки
        activeButton.setSelected(true);

        // Плавное высветление активной кнопки
        animateButton(activeButton, true);

        // Обновляем текущую активную кнопку
        currentActiveButton = activeButton;
    }

    // Сбрасываем выделение всех кнопок
    public void resetButtonSelection() {
        navHome.setSelected(false);
        navCalendar.setSelected(false);
        navStatistics.setSelected(false);
        navChat.setSelected(false);

        // Сбрасываем анимацию на все кнопки
        animateButton(navHome, false);
        animateButton(navCalendar, false);
        animateButton(navStatistics, false);
        animateButton(navChat, false);
    }

    private void animateButton(ImageView button, boolean isSelected) {
        float scale = isSelected ? 1.1f : 1.0f;

        // Плавная анимация изменения масштаба
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", scale);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(400);  // Длительность анимации
        animatorSet.setInterpolator(new DecelerateInterpolator()); // Плавное замедление в конце анимации
        animatorSet.start();
    }

    public void setCurrentActivity(String activityName) {
        // Проверяем, на каком экране мы находимся, и устанавливаем нужное выделение
        switch (activityName) {
            case "MainActivity":
                setActiveButton(navHome);
                break;
            case "CalendarActivity":
                setActiveButton(navCalendar);
                break;
            case "StatisticsActivity":
                setActiveButton(navStatistics);
                break;
            case "ChatActivity":
                setActiveButton(navChat);
                break;
        }
    }
}
