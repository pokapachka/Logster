package com.example.logster;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        hideSystemUI();

        // Find TextViews
        TextView targetText = findViewById(R.id.logster); // Top-left TextView (target position)
        TextView animatedText = findViewById(R.id.logster1); // Centered TextView to animate
        if (targetText == null || animatedText == null) {
            android.util.Log.w("SplashActivity", "One or both TextViews not found");
            transitionToMainActivity();
            return;
        }

        // Perform animation after layout is complete
        animatedText.post(() -> {
            // Get positions (top-left corners of the views)
            int[] startPos = new int[2];
            int[] targetPos = new int[2];
            animatedText.getLocationInWindow(startPos);
            targetText.getLocationInWindow(targetPos);
            float startX = startPos[0];
            float startY = startPos[1];
            float targetX = targetPos[0];
            float targetY = targetPos[1];

            // Calculate scale factor (from 70sp to 40sp)
            final float scaleFactor = 40f / 70f; // ~0.571

            // Adjust for scale difference to align top-left corners
            float pivotX = animatedText.getWidth() * (1 - scaleFactor) / 2;
            float pivotY = animatedText.getHeight() * (1 - scaleFactor) / 2;

            // Log for debugging
            android.util.Log.d("SplashActivity", "startX: " + startX + ", startY: " + startY +
                    ", targetX: " + targetX + ", targetY: " + targetY +
                    ", pivotX: " + pivotX + ", pivotY: " + pivotY);

            // First animation: Fade in color for animatedText (1 second)
            ValueAnimator colorAnim = ValueAnimator.ofObject(new android.animation.ArgbEvaluator(),
                    Color.TRANSPARENT, Color.WHITE);
            colorAnim.setDuration(1000);
            colorAnim.addUpdateListener(animation -> {
                animatedText.setTextColor((int) animation.getAnimatedValue());
            });
            colorAnim.setInterpolator(new android.view.animation.LinearInterpolator());

            // Second animation: Scale and move animatedText (1.5 seconds)
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(animatedText, "scaleX", 1.0f, scaleFactor);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(animatedText, "scaleY", 1.0f, scaleFactor);
            ObjectAnimator translateX = ObjectAnimator.ofFloat(animatedText, "translationX", 0f, targetX - startX - pivotX);
            ObjectAnimator translateY = ObjectAnimator.ofFloat(animatedText, "translationY", 0f, targetY - startY - pivotY);

            AnimatorSet secondStage = new AnimatorSet();
            secondStage.playTogether(scaleX, scaleY, translateX, translateY);
            secondStage.setDuration(1500);
            secondStage.setInterpolator(new android.view.animation.DecelerateInterpolator());

            // Combine animations sequentially
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(colorAnim, secondStage);
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Make targetText visible and animatedText invisible
                    targetText.setTextColor(Color.WHITE);
                    animatedText.setVisibility(TextView.GONE);
                    transitionToMainActivity();
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            animatorSet.start();
        });
    }

    private void transitionToMainActivity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_FIRST_LAUNCH, false);
        editor.apply();

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getInsetsController().hide(android.view.WindowInsets.Type.systemBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}