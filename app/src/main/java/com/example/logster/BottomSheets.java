package com.example.logster;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

public class BottomSheets {

    private final Activity activity;
    private View sheetView;
    private ViewGroup rootLayout;
    private int screenHeight;
    private int initialTopMargin;
    private FrameLayout.LayoutParams params;
    private boolean isShowing;

    public BottomSheets(Activity activity) {
        this(activity, R.layout.widgets);
    }

    public BottomSheets(Activity activity, int layoutId) {
        this.activity = activity;
        LayoutInflater inflater = activity.getLayoutInflater();
        sheetView = inflater.inflate(layoutId, null);
        rootLayout = activity.findViewById(android.R.id.content);

        params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        initialTopMargin = dpToPx(activity, 70);
        params.topMargin = initialTopMargin;
        isShowing = false; // Initialize as not showing
    }

    public void setContentView(View view) {
        if (sheetView != null && sheetView.getParent() != null) {
            rootLayout.removeView(sheetView);
        }
        sheetView = view;
        if (sheetView != null) {
            sheetView.setLayoutParams(params);
        }
    }

    public void show() {
        if (sheetView == null || rootLayout == null) {
            Log.e("BottomSheets", "show: sheetView or rootLayout is null");
            return;
        }

        screenHeight = rootLayout.getHeight();
        if (screenHeight == 0) {
            screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        }
        params.topMargin = screenHeight;
        sheetView.setLayoutParams(params);
        sheetView.post(() -> {
            int maxHeight = screenHeight - initialTopMargin;
            sheetView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    maxHeight
            ));
        });
        if (sheetView.getParent() == null) {
            rootLayout.addView(sheetView);
        }

        sheetView.setVisibility(View.VISIBLE);
        sheetView.bringToFront();
        ValueAnimator animator = ValueAnimator.ofInt(screenHeight, initialTopMargin);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.start();
        setSwipeListener();
        isShowing = true;
    }

    public void hide(Runnable onHidden) {
        if (sheetView == null) {
            Log.e("BottomSheets", "hide: sheetView is null");
            if (onHidden != null) onHidden.run();
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, screenHeight);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (sheetView != null && sheetView.getParent() != null) {
                    rootLayout.removeView(sheetView);
                }
                if (onHidden != null) {
                    onHidden.run();
                }
                isShowing = false; // Set flag to false when hidden
            }
        });
        animator.start();
    }

    // New method to check if the sheet is showing
    public boolean isShowing() {
        return isShowing;
    }

    private void setSwipeListener() {
        if (sheetView == null) {
            Log.e("BottomSheets", "setSwipeListener: sheetView is null");
            return;
        }

        sheetView.setOnTouchListener(new View.OnTouchListener() {
            float downY = 0;
            float totalDeltaY = 0;
            long startTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float moveY = event.getRawY();
                        float deltaY = moveY - downY;
                        totalDeltaY = deltaY;

                        if (deltaY > 0) {
                            params.topMargin = (int) (initialTopMargin + deltaY);
                            sheetView.setLayoutParams(params);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startTime;
                        if (totalDeltaY > dpToPx(activity, 120) || (totalDeltaY > dpToPx(activity, 50) && duration < 150)) {
                            hide(null);
                        } else {
                            resetPosition();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void resetPosition() {
        if (sheetView == null) {
            Log.e("BottomSheets", "resetPosition: sheetView is null");
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, initialTopMargin);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.start();
    }

    private int dpToPx(Activity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public void showWithHorizontalTransition(int exitAnim, int enterAnim, View newView, Runnable onComplete) {
        if (sheetView == null || newView == null || rootLayout == null) {
            Log.e("BottomSheets", "showWithHorizontalTransition: Null detected: sheetView=" + sheetView + ", newView=" + newView + ", rootLayout=" + rootLayout);
            if (onComplete != null) onComplete.run();
            return;
        }

        // Инициализация screenHeight
        screenHeight = rootLayout.getHeight();
        if (screenHeight == 0) {
            screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        }

        // Подготавливаем новый экран
        FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        newParams.topMargin = initialTopMargin; // Устанавливаем отступ 70dp
        newView.setLayoutParams(newParams);
        newView.setVisibility(View.GONE); // Скрываем до начала анимации
        if (newView.getParent() == null) {
            rootLayout.addView(newView);
        }

        // Устанавливаем максимальную высоту, сохраняя topMargin
        int maxHeight = screenHeight - initialTopMargin;
        FrameLayout.LayoutParams heightParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxHeight
        );
        heightParams.topMargin = initialTopMargin; // Сохраняем отступ 70dp
        newView.setLayoutParams(heightParams);

        // Анимация ухода для текущего экрана
        Animation slideOut = AnimationUtils.loadAnimation(activity, exitAnim);
        // Анимация входа для нового экрана
        Animation slideIn = AnimationUtils.loadAnimation(activity, enterAnim);

        // Счётчик для отслеживания завершения обеих анимаций
        final int[] animationCount = {2}; // Две анимации: slideOut и slideIn

        // Обработчик завершения анимаций
        Animation.AnimationListener animationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                animationCount[0]--;
                if (animationCount[0] == 0) {
                    // Удаляем старый экран только после завершения обеих анимаций
                    if (sheetView != null && sheetView.getParent() != null) {
                        rootLayout.removeView(sheetView);
                    }
                    // Обновляем sheetView
                    sheetView = newView;
                    params = (FrameLayout.LayoutParams) sheetView.getLayoutParams();
                    sheetView.setVisibility(View.VISIBLE);

                    // Устанавливаем обработчик свайпов
                    setSwipeListener();

                    if (onComplete != null) {
                        onComplete.run();
                    }
                    isShowing = true; // Update flag when new view is shown
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };

        // Устанавливаем обработчик для обеих анимаций
        slideOut.setAnimationListener(animationListener);
        slideIn.setAnimationListener(animationListener);

        // Запускаем анимации одновременно
        sheetView.startAnimation(slideOut);
        newView.setVisibility(View.VISIBLE);
        newView.startAnimation(slideIn);
    }
    public View getSheetView() {
        return sheetView;
    }
}

