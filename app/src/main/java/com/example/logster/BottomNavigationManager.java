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

    private ImageView currentActiveButton;

    public BottomNavigationManager(View rootView, Context context) {
        this.context = context;
        navHome = rootView.findViewById(R.id.nav_home);
        navCalendar = rootView.findViewById(R.id.nav_calendar);
        navStatistics = rootView.findViewById(R.id.nav_statistics);
        navChat = rootView.findViewById(R.id.nav_chat);
        currentActiveButton = navHome;
        setupListeners();
        setActiveButton(currentActiveButton);
    }

    private void setupListeners() {
        navHome.setOnClickListener(v -> {
            if (currentActiveButton != navHome) {
                setActiveButton(navHome);
                Intent intent = new Intent(context, MainActivity.class);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(context,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
                context.startActivity(intent, options.toBundle());
            }
        });

        navCalendar.setOnClickListener(v -> {
            if (currentActiveButton != navCalendar) {
                setActiveButton(navCalendar);
                Intent intent = new Intent(context, CalendarActivity.class);
                intent.putExtra("goToToday", true);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(context,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
                context.startActivity(intent, options.toBundle());
            } else {
                Intent intent = new Intent(context, CalendarActivity.class);
                intent.putExtra("goToToday", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });

        navStatistics.setOnClickListener(v -> {
            if (currentActiveButton != navStatistics) {
                setActiveButton(navStatistics);
                Intent intent = new Intent(context, StatisticsActivity.class);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(context,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
                context.startActivity(intent, options.toBundle());
            }
        });

        navChat.setOnClickListener(v -> {
            if (currentActiveButton != navChat) {
                setActiveButton(navChat);
                Intent intent = new Intent(context, ChatActivity.class);
                ActivityOptions options = ActivityOptions.makeCustomAnimation(context,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                );
                context.startActivity(intent, options.toBundle());
            }
        });
    }

    public void setActiveButton(ImageView activeButton) {
        navHome.setSelected(false);
        navCalendar.setSelected(false);
        navStatistics.setSelected(false);
        navChat.setSelected(false);
        animateButton(navHome, false);
        animateButton(navCalendar, false);
        animateButton(navStatistics, false);
        animateButton(navChat, false);
        activeButton.setSelected(true);
        animateButton(activeButton, true);
        currentActiveButton = activeButton;
    }

    private void animateButton(ImageView button, boolean isSelected) {
        float scale = isSelected ? 1.1f : 1.0f;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(button, "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(button, "scaleY", scale);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.start();
    }

    public void setCurrentActivity(String activityName) {
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
    public void forceSetActiveButton(String activityName) {
        navHome.setSelected(false);
        navCalendar.setSelected(false);
        navStatistics.setSelected(false);
        navChat.setSelected(false);
        switch (activityName) {
            case "MainActivity":
                navHome.setSelected(true);
                currentActiveButton = navHome;
                break;
            case "CalendarActivity":
                navCalendar.setSelected(true);
                currentActiveButton = navCalendar;
                break;
            case "StatisticsActivity":
                navStatistics.setSelected(true);
                currentActiveButton = navStatistics;
                break;
            case "ChatActivity":
                navChat.setSelected(true);
                currentActiveButton = navChat;
                break;
        }
    }
}
