package com.example.logster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
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
    private View dimView;
    private FrameLayout.LayoutParams params;
    private final FrameLayout rootLayout;
    private boolean isShowing = false;
    private Runnable onConfirmAction;
    private Runnable onCancelAction;
    private ValueAnimator currentAnimator;

    public ConfirmationBottomSheet(Context context) {
        this.context = context;
        this.rootLayout = ((Activity) context).findViewById(android.R.id.content);
    }

    public void show(String actionText, String itemName, String itemType, String itemId, Runnable confirmAction, Runnable cancelAction) {
        if (isShowing) {
            Log.w(TAG, "Лист уже отображается");
            return;
        }
        this.onConfirmAction = confirmAction;
        this.onCancelAction = cancelAction;

        // Добавляем затемнённый фоновый слой
        dimView = new View(context);
        dimView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dimView.setBackgroundColor(0x80000000); // Полупрозрачный чёрный
        dimView.setClickable(true);
        rootLayout.addView(dimView);

        // Инициализация листа
        sheetView = LayoutInflater.from(context).inflate(R.layout.verify_item, null);
        params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM; // Фиксируем к нижнему краю
        sheetView.setLayoutParams(params);
        sheetView.setTranslationY(rootLayout.getHeight()); // Начальная позиция за экраном снизу
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

        // Анимация появления
        sheetView.post(() -> {
            float sheetHeight = sheetView.getMeasuredHeight();
            ValueAnimator animator = ValueAnimator.ofFloat(sheetHeight, 0f);
            animator.setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                sheetView.setTranslationY((float) animation.getAnimatedValue());
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    isShowing = true;
                }
            });
            currentAnimator = animator;
            animator.start();
            Log.d(TAG, "Лист показан для: " + itemType + ", ID=" + itemId + ", height=" + sheetHeight);
        });

        sheetView.setVisibility(View.VISIBLE);
        sheetView.bringToFront();
        dimView.bringToFront();
        sheetView.bringToFront();
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

        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
            Log.d(TAG, "Текущая анимация отменена");
        }

        // Проверяем, что sheetView не null перед использованием
        float sheetHeight = sheetView != null ? sheetView.getMeasuredHeight() : 0f;
        if (sheetHeight == 0f) {
            Log.w(TAG, "sheetHeight=0, пропускаем анимацию");
            cleanupViews(onHidden);
            return;
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, sheetHeight);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (sheetView != null) {
                sheetView.setTranslationY((float) animation.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                cleanupViews(onHidden);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cleanupViews(null);
            }
        });
        currentAnimator = animator;
        animator.start();
    }

    private void cleanupViews(Runnable onHidden) {
        if (sheetView != null) {
            sheetView.setVisibility(View.GONE);
            try {
                rootLayout.removeView(sheetView);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "sheetView уже удалён из rootLayout: " + e.getMessage());
            }
            sheetView = null; // Явно обнуляем после удаления
        }
        if (dimView != null) {
            try {
                rootLayout.removeView(dimView);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "dimView уже удалён из rootLayout: " + e.getMessage());
            }
            dimView = null; // Явно обнуляем после удаления
        }
        isShowing = false;
        currentAnimator = null;
        if (onHidden != null) {
            onHidden.run();
        }
        Log.d(TAG, "Лист скрыт");
    }

    private void resetPosition() {
        if (sheetView == null) return;

        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
            Log.d(TAG, "Анимация возврата отменена");
        }

        ValueAnimator animator = ValueAnimator.ofFloat(sheetView.getTranslationY(), 0f);
        animator.setDuration(200); // Ускоряем для плавности
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (sheetView != null) {
                sheetView.setTranslationY((float) animation.getAnimatedValue());
            }
        });
        currentAnimator = animator;
        animator.start();
    }

    private void setSwipeListener(View deleteButton, View cancelButton, String itemType, String itemId) {
        sheetView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private float startTranslationY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isInteractiveElementTouched(event, deleteButton, cancelButton)) {
                    return false;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        startTranslationY = sheetView.getTranslationY();
                        isDragging = true;
                        if (currentAnimator != null && currentAnimator.isRunning()) {
                            currentAnimator.cancel();
                            Log.d(TAG, "Анимация отменена перед началом свайпа");
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging) return false;
                        float deltaY = event.getRawY() - startY;
                        float newTranslationY = startTranslationY + deltaY;
                        if (newTranslationY >= 0) { // Ограничиваем движение вверх
                            sheetView.setTranslationY(newTranslationY);
                            Log.d(TAG, "ACTION_MOVE: deltaY=" + deltaY + ", newTranslationY=" + newTranslationY);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) return false;
                        isDragging = false;
                        float currentTranslationY = sheetView.getTranslationY();
                        float sheetHeight = sheetView.getMeasuredHeight();
                        if (currentTranslationY > dpToPx(100)) { // Смахивание вниз
                            // Плавное скрытие
                            ValueAnimator animator = ValueAnimator.ofFloat(currentTranslationY, sheetHeight);
                            animator.setDuration(200); // Ускоряем для плавности
                            animator.setInterpolator(new DecelerateInterpolator());
                            animator.addUpdateListener(animation -> {
                                if (sheetView != null) {
                                    sheetView.setTranslationY((float) animation.getAnimatedValue());
                                }
                            });
                            animator.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (onCancelAction != null) {
                                        onCancelAction.run();
                                    }
                                    cleanupViews(null);
                                }
                            });
                            currentAnimator = animator;
                            animator.start();
                        } else {
                            // Плавный возврат в исходное положение
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