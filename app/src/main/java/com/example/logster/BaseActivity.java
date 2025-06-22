package com.example.logster;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String TAG = "BaseActivity";
    private ImageView hideKeyboardIcon;
    private boolean isKeyboardVisible = false;
    private int keyboardHeight = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupKeyboardListener();
    }

    private void setupKeyboardListener() {
        final View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            Log.w(TAG, "Root view is null, cannot setup keyboard listener");
            return;
        }

        // Создаём контейнер для иконки
        FrameLayout iconContainer = new FrameLayout(this);
        iconContainer.setId(View.generateViewId());
        iconContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Создаём ImageView для down.png
        hideKeyboardIcon = new ImageView(this);
        hideKeyboardIcon.setId(View.generateViewId());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(40));
        params.gravity = Gravity.END | Gravity.BOTTOM;
        hideKeyboardIcon.setLayoutParams(params);
        hideKeyboardIcon.setImageResource(R.drawable.down);
        hideKeyboardIcon.setVisibility(View.GONE);
        hideKeyboardIcon.setElevation(20f);
        hideKeyboardIcon.setForeground(getResources().getDrawable(android.R.drawable.list_selector_background));
        hideKeyboardIcon.setBackgroundResource(android.R.color.transparent);
        hideKeyboardIcon.setBackgroundColor(Color.RED); // Временный отладочный фон

        // Проверяем загрузку drawable
        try {
            if (hideKeyboardIcon.getDrawable() == null) {
                Log.e(TAG, "Drawable down.png is null! Falling back to system resource.");
                hideKeyboardIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Запасной ресурс
            } else {
                Log.d(TAG, "Drawable down.png loaded successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking down.png drawable: " + e.getMessage(), e);
            hideKeyboardIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Запасной ресурс
        }

        // Добавляем иконку в контейнер
        iconContainer.addView(hideKeyboardIcon);
        if (rootView instanceof ViewGroup) {
            ((ViewGroup) rootView).addView(iconContainer);
            iconContainer.bringToFront();
            Log.d(TAG, "Added iconContainer to rootView, child count: " + ((ViewGroup) rootView).getChildCount());
        } else {
            Log.w(TAG, "Root view is not a ViewGroup");
        }

        // Отслеживание клавиатуры
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            android.graphics.Rect r = new android.graphics.Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15f) {
                if (!isKeyboardVisible) {
                    keyboardHeight = keypadHeight;
                    isKeyboardVisible = true;
                    showKeyboardIcon();
                    Log.d(TAG, "Keyboard opened, keypadHeight: " + keypadHeight);
                }
            } else {
                if (isKeyboardVisible) {
                    isKeyboardVisible = false;
                    hideKeyboardIcon();
                    Log.d(TAG, "Keyboard closed");
                }
            }
        });

        hideKeyboardIcon.setOnClickListener(v -> {
            hideKeyboard();
            hideKeyboardIcon();
            isKeyboardVisible = false;
            Log.d(TAG, "Hide keyboard icon clicked");
        });
    }

    private void showKeyboardIcon() {
        if (hideKeyboardIcon == null) {
            Log.w(TAG, "hideKeyboardIcon is null in showKeyboardIcon");
            return;
        }

        hideKeyboardIcon.setVisibility(View.VISIBLE);
        hideKeyboardIcon.bringToFront();
        ViewGroup parent = (ViewGroup) hideKeyboardIcon.getParent();
        if (parent != null) {
            parent.bringToFront();
            Log.d(TAG, "Brought iconContainer to front");
        }

        Log.d(TAG, "Set hideKeyboardIcon visibility to VISIBLE and brought to front");

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) hideKeyboardIcon.getLayoutParams();
        params.gravity = Gravity.END | Gravity.BOTTOM;
        params.setMargins(0, 0, dpToPx(16), keyboardHeight + dpToPx(16));
        hideKeyboardIcon.setLayoutParams(params);
        Log.d(TAG, "Set icon margins: right=" + dpToPx(16) + ", bottom=" + (keyboardHeight + dpToPx(16)));

        // Упрощённая анимация для тестирования
        hideKeyboardIcon.setTranslationX(hideKeyboardIcon.getWidth());
        hideKeyboardIcon.setAlpha(0f);
        hideKeyboardIcon.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .withStartAction(() -> Log.d(TAG, "Starting showKeyboardIcon animation"))
                .withEndAction(() -> Log.d(TAG, "Finished showKeyboardIcon animation"))
                .start();
    }

    private void hideKeyboardIcon() {
        if (hideKeyboardIcon == null || hideKeyboardIcon.getVisibility() == View.GONE) {
            return;
        }

        hideKeyboardIcon.animate()
                .translationX(hideKeyboardIcon.getWidth())
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AnticipateInterpolator())
                .withEndAction(() -> {
                    hideKeyboardIcon.setVisibility(View.GONE);
                    Log.d(TAG, "hideKeyboardIcon set to GONE");
                })
                .start();
    }

    protected void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
            Log.d(TAG, "Keyboard hidden programmatically");
        } else {
            Log.d(TAG, "No current focus, skipping keyboard hide");
        }
    }

    // Метод для доступа к hideKeyboardIcon
    public ImageView getHideKeyboardIcon() {
        return hideKeyboardIcon;
    }

    // Метод для отладки содержимого rootLayout
    public void debugRootLayout() {
        ViewGroup rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Log.d(TAG, "RootLayout child count: " + rootView.getChildCount());
            for (int i = 0; i < rootView.getChildCount(); i++) {
                View child = rootView.getChildAt(i);
                Log.d(TAG, "Child " + i + ": " + child.getClass().getSimpleName() + ", id=" + child.getId());
            }
        } else {
            Log.w(TAG, "RootLayout is null in debugRootLayout");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hideKeyboardIcon != null && hideKeyboardIcon.getParent() != null) {
            ViewGroup parent = (ViewGroup) hideKeyboardIcon.getParent();
            if (parent.getParent() != null) {
                ((ViewGroup) parent.getParent()).removeView(parent);
                Log.d(TAG, "Removed iconContainer from rootLayout");
            }
            hideKeyboardIcon = null;
        }
    }

    protected abstract int dpToPx(int dp);
}