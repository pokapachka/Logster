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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;
import java.util.Map;

public class BottomSheets {
    private static final String TAG = "BottomSheets";
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
        if (rootLayout == null) {
            Log.e(TAG, "rootLayout is null, cannot initialize BottomSheets");
            return;
        }
        params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        initialTopMargin = dpToPx(activity, 70);
        params.topMargin = initialTopMargin;
        isShowing = false;
    }

    public void setContentView(View view) {
        if (sheetView != null && sheetView.getParent() != null) {
            rootLayout.removeView(sheetView);
            Log.d(TAG, "Removed existing sheetView from rootLayout");
        }
        sheetView = view;
        if (sheetView != null) {
            sheetView.setLayoutParams(params);
            // Заполняем экран описания, если это exercise_description
            if (sheetView.getId() == R.id.exerciseDescriprion && activity instanceof MainActivity) {
                populateExerciseDescription();
                Log.d(TAG, "Populated exercise description in setContentView");
            }
        } else {
            Log.e(TAG, "setContentView: view is null");
        }
    }

    private void populateExerciseDescription() {
        if (!(activity instanceof MainActivity)) {
            Log.e(TAG, "Activity is not MainActivity, cannot populate exercise description");
            return;
        }
        MainActivity mainActivity = (MainActivity) activity;
        // Ищем упражнение по имени из switchSheet data
        ExercisesAdapter.Exercise exercise = ExerciseList.getAllExercises().stream()
                .filter(e -> e.getName().equals(mainActivity.getCurrentExerciseName()))
                .findFirst()
                .orElse(null);
        if (exercise != null) {
            TextView title = sheetView.findViewById(R.id.titleExercises);
            TextView description = sheetView.findViewById(R.id.description);
            FlexboxLayout tagsContainer = sheetView.findViewById(R.id.tags_container);

            if (title != null) {
                title.setText(exercise.getName());
            }
            if (description != null) {
                description.setText(exercise.getDescription());
            }
            if (tagsContainer != null) {
                tagsContainer.removeAllViews();
                List<String> tags = exercise.getTags();
                for (String tag : tags) {
                    TextView tagView = (TextView) LayoutInflater.from(activity)
                            .inflate(R.layout.item_tag, tagsContainer, false);
                    tagView.setText(tag);
                    tagsContainer.addView(tagView);
                }
            }
            Log.d(TAG, "Populated exercise description for: " + exercise.getName());
        } else {
            Log.w(TAG, "No exercise found for description with name: " + mainActivity.getCurrentExerciseName());
        }
    }

    public void show() {
        if (sheetView == null || rootLayout == null) {
            Log.e(TAG, "show: sheetView or rootLayout is null");
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
            sheetView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight));
        });
        if (sheetView.getParent() == null) {
            rootLayout.addView(sheetView);
            Log.d(TAG, "Added sheetView to rootLayout");
        }
        sheetView.setVisibility(View.VISIBLE);
        sheetView.bringToFront();
        // Поднимаем iconContainer поверх sheetView
        ValueAnimator animator = ValueAnimator.ofInt(screenHeight, initialTopMargin);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.start();
        setSwipeListener();
        isShowing = true;
        View close = sheetView.findViewById(R.id.close);
        if (close != null) {
            close.setOnClickListener(v -> hide(null));
        }
    }

    public void showWithLimitedHeightAndCallback(Runnable onHidden) {
        if (sheetView == null || rootLayout == null) {
            Log.e(TAG, "showWithLimitedHeightAndCallback: sheetView or rootLayout is null");
            if (onHidden != null) onHidden.run();
            return;
        }
        screenHeight = rootLayout.getHeight();
        if (screenHeight == 0) {
            screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        }
        int limitedHeight = dpToPx(activity, 350);
        int limitedTopMargin = screenHeight - limitedHeight;
        params.topMargin = screenHeight;
        sheetView.setLayoutParams(params);
        sheetView.post(() -> {
            sheetView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, limitedHeight));
        });
        if (sheetView.getParent() == null) {
            rootLayout.addView(sheetView);
            Log.d(TAG, "Added sheetView to rootLayout (limited height with callback)");
        }
        sheetView.setVisibility(View.VISIBLE);
        sheetView.bringToFront();
        ValueAnimator animator = ValueAnimator.ofInt(screenHeight, limitedTopMargin);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.start();
        initialTopMargin = limitedTopMargin;
        isShowing = true;
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
                        int newTopMargin = (int) (initialTopMargin + deltaY);
                        if (newTopMargin < initialTopMargin) {
                            newTopMargin = initialTopMargin;
                        }
                        params.topMargin = newTopMargin;
                        sheetView.setLayoutParams(params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        long duration = System.currentTimeMillis() - startTime;
                        if (totalDeltaY > dpToPx(activity, 120) || (totalDeltaY > dpToPx(activity, 50) && duration < 150)) {
                            hide(onHidden);
                        } else {
                            resetPosition();
                        }
                        return true;
                }
                return false;
            }
        });
        View close = sheetView.findViewById(R.id.close);
        if (close != null) {
            close.setOnClickListener(v -> hide(onHidden));
        }
    }

    public void hide(Runnable onHidden) {
        if (sheetView == null) {
            Log.e(TAG, "hide: sheetView is null");
            if (onHidden != null) onHidden.run();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, screenHeight);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (sheetView != null && sheetView.getParent() != null) {
                    rootLayout.removeView(sheetView);
                    Log.d(TAG, "Removed sheetView from rootLayout on hide, child count: " + rootLayout.getChildCount());
                }
                isShowing = false;
                if (onHidden != null) {
                    onHidden.run();
                }
            }
        });
        animator.start();
    }

    public boolean isShowing() {
        return isShowing;
    }

    private void setSwipeListener() {
        if (sheetView == null) {
            Log.e(TAG, "setSwipeListener: sheetView is null");
            return;
        }
        sheetView.setOnTouchListener(new View.OnTouchListener() {
            float downY = 0;
            float totalDeltaY = 0;
            long startTime;

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
                        int newTopMargin = (int) (initialTopMargin + deltaY);
                        if (newTopMargin < initialTopMargin) {
                            newTopMargin = initialTopMargin;
                        }
                        params.topMargin = newTopMargin;
                        sheetView.setLayoutParams(params);
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
            Log.e(TAG, "resetPosition: sheetView is null");
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
            Log.e(TAG, "showWithHorizontalTransition: Null detected: sheetView=" + sheetView + ", newView=" + newView + ", rootLayout=" + rootLayout);
            if (onComplete != null) onComplete.run();
            return;
        }
        screenHeight = rootLayout.getHeight();
        if (screenHeight == 0) {
            screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        }
        FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        newParams.topMargin = initialTopMargin;
        newView.setLayoutParams(newParams);
        newView.setVisibility(View.GONE);
        if (newView.getParent() == null) {
            rootLayout.addView(newView);
            Log.d(TAG, "Added newView to rootLayout in showWithHorizontalTransition, child count: " + rootLayout.getChildCount());
        }
        int maxHeight = screenHeight - initialTopMargin;
        FrameLayout.LayoutParams heightParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
        heightParams.topMargin = initialTopMargin;
        newView.setLayoutParams(heightParams);
        Animation slideOut = AnimationUtils.loadAnimation(activity, exitAnim);
        Animation slideIn = AnimationUtils.loadAnimation(activity, enterAnim);
        final int[] animationCount = {2};
        Animation.AnimationListener animationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                animationCount[0]--;
                if (animationCount[0] == 0) {
                    if (sheetView != null && sheetView.getParent() != null) {
                        rootLayout.removeView(sheetView);
                        Log.d(TAG, "Removed old sheetView after horizontal transition, child count: " + rootLayout.getChildCount());
                        for (int i = 0; i < rootLayout.getChildCount(); i++) {
                            View child = rootLayout.getChildAt(i);
                            Log.d(TAG, "Remaining child " + i + ": " + child.getClass().getSimpleName() + ", id=" + child.getId());
                        }
                    }
                    sheetView = newView;
                    params = (FrameLayout.LayoutParams) sheetView.getLayoutParams();
                    sheetView.setVisibility(View.VISIBLE);
                    sheetView.bringToFront();

                    // Поднимаем iconContainer поверх sheetView
                    if (activity instanceof BaseActivity) {
                        ImageView hideKeyboardIcon = ((BaseActivity) activity).getHideKeyboardIcon();
                        if (hideKeyboardIcon != null) {
                            View iconContainer = (View) hideKeyboardIcon.getParent();
                            if (iconContainer != null && iconContainer.getParent() != null) {
                                ((ViewGroup) iconContainer.getParent()).bringChildToFront(iconContainer);
                                Log.d(TAG, "Brought iconContainer to front after sheetView in showWithHorizontalTransition");
                            } else {
                                Log.w(TAG, "iconContainer or its parent is null in showWithHorizontalTransition");
                            }
                        }
                    }

                    setSwipeListener();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    isShowing = true;
                    if (sheetView.getId() == R.id.exerciseDescriprion && activity instanceof MainActivity) {
                        populateExerciseDescription();
                        Log.d(TAG, "Populated exercise description after horizontal transition");
                    }

                    // Логируем содержимое rootLayout
                    Log.d(TAG, "RootLayout child count after transition: " + rootLayout.getChildCount());
                    for (int i = 0; i < rootLayout.getChildCount(); i++) {
                        View child = rootLayout.getChildAt(i);
                        Log.d(TAG, "Child " + i + ": " + child.getClass().getSimpleName() + ", id=" + child.getId());
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        };
        slideOut.setAnimationListener(animationListener);
        slideIn.setAnimationListener(animationListener);
        sheetView.startAnimation(slideOut);
        newView.setVisibility(View.VISIBLE);
        newView.startAnimation(slideIn);
    }

    public View getContentView() {
        return sheetView;
    }

    public void switchSheet(int layoutId, String data, boolean useHorizontalTransition, int exitAnim, int enterAnim) {
        // Hide keyboard based on activity type
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).hideKeyboard();
            ((MainActivity) activity).setCurrentExerciseName(data);
        } else if (activity instanceof ChatActivity) {
            ((ChatActivity) activity).hideKeyboard();
        }

        // Inflate new layout with error handling
        View newView = null;
        try {
            newView = LayoutInflater.from(activity).inflate(layoutId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout ID: " + layoutId + ", Error: " + e.getMessage(), e);
            return; // Prevent further execution
        }

        // Set listener for close button
        View close = newView.findViewById(R.id.close);
        if (close != null) {
            close.setOnClickListener(v -> {
                if (layoutId == R.layout.exercises_description && activity instanceof MainActivity) {
                    ((MainActivity) activity).backSheetExerciseDescription(v);
                } else {
                    hide(null);
                }
            });
        }

        // Handle different layouts
        if (layoutId == R.layout.exercises_description && activity instanceof MainActivity) {
            Log.d(TAG, "Handling exercise description screen");
            setContentView(newView);
            show();
        } else if (layoutId == R.layout.add_exercises && activity instanceof MainActivity) {
            Log.d(TAG, "Handling add exercises screen");
            setContentView(newView);
            show();
        } else if (layoutId == R.layout.autorization && activity instanceof ChatActivity) {
            Authorization auth = new Authorization(activity);
            if (useHorizontalTransition && exitAnim != 0 && enterAnim != 0) {
                showWithHorizontalTransition(exitAnim, enterAnim, newView, () -> auth.show());
            } else {
                setContentView(newView);
                auth.show();
            }
            Log.d(TAG, "Initialized authorization screen");
        } else if (layoutId == R.layout.registration && activity instanceof ChatActivity) {
            Registration reg = new Registration(activity);
            if (useHorizontalTransition && exitAnim != 0 && enterAnim != 0) {
                showWithHorizontalTransition(exitAnim, enterAnim, newView, () -> reg.show());
            } else {
                setContentView(newView);
                reg.show();
            }
            Log.d(TAG, "Initialized registration screen");
        } else if (layoutId == R.layout.profile || layoutId == R.layout.profile_tag ||
                layoutId == R.layout.profile_bio || layoutId == R.layout.profile_image) {
            if (activity instanceof ChatActivity) {
                Profile profile = new Profile(activity, this);
                View back = newView.findViewById(R.id.back);
                if (back != null) {
                    back.setOnClickListener(v -> switchSheet(R.layout.profile, null, true,
                            R.anim.slide_out_right, R.anim.slide_in_left));
                }
                if (useHorizontalTransition && exitAnim != 0 && enterAnim != 0) {
                    showWithHorizontalTransition(exitAnim, enterAnim, newView,
                            () -> profile.setupProfileSheet(layoutId, data));
                } else {
                    setContentView(newView);
                    profile.setupProfileSheet(layoutId, data);
                    show();
                }
                Log.d(TAG, "Initialized profile screen: " + layoutId);
            } else {
                Log.e(TAG, "Activity is not ChatActivity for profile layout: " + layoutId);
                setContentView(newView);
                show();
            }
        }  else if (layoutId == R.layout.profile_user && activity instanceof ChatActivity) {
            Log.d(TAG, "Handling profile_user screen");
            View back = newView.findViewById(R.id.back);
            if (back != null) {
                back.setOnClickListener(v -> {
                    // Закрываем текущий BottomSheet с анимацией вниз
                    hide(null);
                });
            }

            // Загружаем данные профиля
            ImageView profileImage = newView.findViewById(R.id.image_profile_user);
            TextView tagText = newView.findViewById(R.id.tag_user);
            TextView bioText = newView.findViewById(R.id.bio_user);

            if (profileImage != null && tagText != null && bioText != null) {
                if (data != null) {
                    // Загружаем данные другого пользователя по userId
                    RegisterContext.fetchProfileById(activity, data, new RegisterContext.Callback<RegisterContext.ProfileData>() {
                        @Override
                        public void onSuccess(RegisterContext.ProfileData profileData) {
                            ((Activity) activity).runOnUiThread(() -> {
                                // Устанавливаем тег
                                String tag = profileData.username != null ? "@" + profileData.username : "pokapachka";
                                tagText.setText(tag);

                                // Устанавливаем био
                                String bio = profileData.bio != null && !profileData.bio.isEmpty() ? profileData.bio : "Биография не указана";
                                bioText.setText(bio);

                                // Устанавливаем изображение профиля
                                String imageUrl = profileData.imageUrl;
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(activity)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_profile)
                                            .error(R.drawable.default_profile)
                                            .circleCrop()
                                            .into(profileImage);
                                } else {
                                    profileImage.setImageResource(R.drawable.default_profile);
                                }
                                Log.d(TAG, "Данные профиля пользователя загружены: userId=" + data);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            ((Activity) activity).runOnUiThread(() -> {
                                Log.e(TAG, "Ошибка загрузки профиля пользователя: " + error);
                                Toast.makeText(activity, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show();
                                // Устанавливаем значения по умолчанию
                                tagText.setText("pokapachka");
                                bioText.setText("Биография не указана");
                                profileImage.setImageResource(R.drawable.default_profile);
                            });
                        }
                    });
                } else {
                    // Загружаем данные текущего пользователя
                    ChatActivity chatActivity = (ChatActivity) activity;
                    String imageUrl = chatActivity.getProfileImageUrl();
                    String tag = chatActivity.getProfileTag();
                    String bio = chatActivity.getProfileBio();

                    tagText.setText(tag != null && !tag.isEmpty() ? tag : "pokapachka");
                    bioText.setText(bio != null && !bio.isEmpty() ? bio : "Биография не указана");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(activity)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .circleCrop()
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.default_profile);
                    }
                }
            } else {
                Log.e(TAG, "Missing UI elements in profile_user layout: image_profile_user=" + (profileImage == null) +
                        ", tag_user=" + (tagText == null) + ", bio_user=" + (bioText == null));
                Toast.makeText(activity, "Ошибка загрузки интерфейса профиля", Toast.LENGTH_SHORT).show();
            }

            if (useHorizontalTransition && exitAnim != 0 && enterAnim != 0) {
                showWithHorizontalTransition(exitAnim, enterAnim, newView, null);
            } else {
                setContentView(newView);
                show();
            }
            Log.d(TAG, "Initialized profile_user screen");

        }
    }
}