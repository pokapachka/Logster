package com.example.logster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ConfirmationBottomSheet {
    private static final String TAG = "ConfirmationBottomSheet";
    private final Context context;
    private View sheetView;
    private FrameLayout.LayoutParams params;
    private final FrameLayout rootLayout;
    private boolean isShowing = false;
    private final int sheetHeightDp = 250; // Высота листа
    private int initialTopMargin;
    private int finalTopMargin;
    private int screenHeight;
    private Runnable onConfirmAction;
    private Runnable onCancelAction;
    private ValueAnimator currentAnimator;

    public ConfirmationBottomSheet(Context context) {
        this.context = context;
        this.rootLayout = ((Activity) context).findViewById(android.R.id.content);
        this.screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    public void show(String actionText, String itemName, String itemType, String itemId, Runnable confirmAction, Runnable cancelAction) {
        if (isShowing) {
            Log.w(TAG, "Лист уже отображается");
            return;
        }
        this.onConfirmAction = confirmAction;
        this.onCancelAction = cancelAction;

        // Инициализация листа
        sheetView = LayoutInflater.from(context).inflate(R.layout.verify_item, null);
        params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        initialTopMargin = screenHeight; // Начальная позиция за экраном
        finalTopMargin = screenHeight - dpToPx(sheetHeightDp); // Конечная позиция внизу экрана
        params.topMargin = initialTopMargin;
        sheetView.setLayoutParams(params);
        rootLayout.addView(sheetView);

        // Настройка UI
        TextView titleView = sheetView.findViewById(R.id.title_verify);
        TextView deleteButton = sheetView.findViewById(R.id.delete_verify);
        TextView cancelButton = sheetView.findViewById(R.id.cancel_verify);
        if (titleView != null) {
            String titleText;
            if (itemType.equals("Logout")) {
                titleText = "Выйти из аккаунта?";
            } else if (itemType.equals("DeleteAccount")) {
                titleText = "Удалить аккаунт?";
            } else {
                titleText = String.format("Удалить \"%s\"?", itemName);
            }
            titleView.setText(titleText);
            Log.d(TAG, "Установлено название: " + titleText);
        }
        if (deleteButton != null) {
            deleteButton.setText(itemType.equals("Logout") ? "Выйти" : "Удалить");
            deleteButton.setOnClickListener(v -> {
                if (onConfirmAction != null) {
                    onConfirmAction.run();
                    Log.d(TAG, "Подтверждено действие: " + itemType + ", ID=" + itemId);
                }
                hide(null);
            });
        }
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                if (onCancelAction != null) {
                    onCancelAction.run();
                    Log.d(TAG, "Отменено действие: " + itemType + ", ID=" + itemId);
                }
                hide(null);
            });
        }

        // Установка максимальной высоты
        sheetView.post(() -> {
            params.height = dpToPx(sheetHeightDp);
            sheetView.setLayoutParams(params);
        });

        sheetView.setVisibility(View.VISIBLE);
        sheetView.bringToFront();

        // Анимация появления
        ValueAnimator animator = ValueAnimator.ofInt(initialTopMargin, finalTopMargin);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isShowing = true;
            }
        });
        currentAnimator = animator;
        animator.start();
        Log.d(TAG, "Лист показан для: " + itemType + ", ID=" + itemId);

        // Обработка смахивания
        setSwipeListener(deleteButton, cancelButton, itemType, itemId);
    }

    public void hide(Runnable onHidden) {
        if (!isShowing || sheetView == null) {
            Log.w(TAG, "Лист не отображается или sheetView null");
            if (onHidden != null) {
                onHidden.run();
            }
            return;
        }

        // Отменяем текущую анимацию
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
            Log.d(TAG, "Текущая анимация отменена");
        }

        // Анимация скрытия
        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, screenHeight);
        animator.setDuration(300); // Ускорено до 300 мс
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (sheetView != null) {
                params.topMargin = (int) animation.getAnimatedValue();
                sheetView.setLayoutParams(params);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (sheetView != null) {
                    sheetView.setVisibility(View.GONE);
                    try {
                        rootLayout.removeView(sheetView);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "sheetView уже удален из rootLayout: " + e.getMessage());
                    }
                }
                isShowing = false;
                sheetView = null;
                currentAnimator = null;
                if (onHidden != null) {
                    onHidden.run();
                }
                Log.d(TAG, "Лист скрыт");
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (sheetView != null) {
                    sheetView.setVisibility(View.GONE);
                    try {
                        rootLayout.removeView(sheetView);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "sheetView уже удален из rootLayout при отмене: " + e.getMessage());
                    }
                }
                isShowing = false;
                sheetView = null;
                currentAnimator = null;
                Log.d(TAG, "Анимация скрытия отменена");
            }
        });
        currentAnimator = animator;
        animator.start();
    }

    private void resetPosition() {
        if (sheetView == null) return;

        // Отменяем текущую анимацию
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
        }

        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, finalTopMargin);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        currentAnimator = animator;
        animator.start();
    }

    private void setSwipeListener(View deleteButton, View cancelButton, String itemType, String itemId) {
        sheetView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private int startTopMargin;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isInteractiveElementTouched(event, deleteButton, cancelButton)) {
                    return false; // Пропускаем клик по кнопкам
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        startTopMargin = params.topMargin;
                        isDragging = true;
                        if (currentAnimator != null && currentAnimator.isRunning()) {
                            currentAnimator.cancel();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging) return false;
                        float deltaY = event.getRawY() - startY;
                        int newTopMargin = (int) (startTopMargin + deltaY);
                        if (newTopMargin >= finalTopMargin) {
                            params.topMargin = newTopMargin;
                            sheetView.setLayoutParams(params);
                            Log.d(TAG, "ACTION_MOVE: deltaY=" + deltaY + ", newTopMargin=" + newTopMargin);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) return false;
                        isDragging = false;
                        if (params.topMargin > finalTopMargin + dpToPx(100)) {
                            hide(() -> {
                                if (onCancelAction != null) {
                                    onCancelAction.run();
                                    Log.d(TAG, "Отменено действие: " + itemType + ", ID=" + itemId);
                                }
                            });
                        } else {
                            resetPosition();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isInteractiveElementTouched(MotionEvent event, View... interactiveViews) {
        for (View view : interactiveViews) {
            if (view != null && isPointInsideView(event.getRawX(), event.getRawY(), view)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        return (x >= viewX && x <= (viewX + viewWidth) && y >= viewY && y <= (viewY + viewHeight));
    }

    public boolean isShowing() {
        return isShowing;
    }
}