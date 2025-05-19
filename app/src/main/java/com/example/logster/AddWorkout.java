package com.example.logster;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class AddWorkout {
    public interface WorkoutSelectionListener {
        void onWorkoutSelected(String workoutKey);
    }

    private static WorkoutSelectionListener selectionListener;

    public static void setWorkoutSelectionListener(WorkoutSelectionListener listener) {
        selectionListener = listener;
    }

    private static final Map<String, String[]> workoutInfoMap = new HashMap<>();

    static {
        workoutInfoMap.put("своя", new String[]{"Своя тренировка", "Полностью индивидуальная тренировка по твоей системе."});
        workoutInfoMap.put("грудь", new String[]{"Грудные мышцы", "Упражнения на верх, низ и середину груди."});
        workoutInfoMap.put("спина", new String[]{"Спина", "Комплекс для широчайших, трапеций и поясницы."});
        workoutInfoMap.put("ноги", new String[]{"Ноги", "Тренировка квадрицепсов, бицепса бедра и икр."});
        workoutInfoMap.put("плечи", new String[]{"Плечи", "Упражнения для переднего, среднего и заднего пучка дельт."});
        workoutInfoMap.put("бицепс", new String[]{"Бицепс", "Тренировка бицепса."});
        workoutInfoMap.put("трицепс", new String[]{"Трицепс", "Тренировка трицепса."});
        workoutInfoMap.put("пресс", new String[]{"Пресс", "Тренировка пресса."});
        workoutInfoMap.put("кардио", new String[]{"Кардио", "Кардио тренировка."});
        workoutInfoMap.put("фуллбади", new String[]{"Фуллбади", "Тренировка всего тела."});
        workoutInfoMap.put("верхняя часть", new String[]{"Верхняя часть", "Тренировка верхней части тела."});
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    public static Runnable scrollRunnable;
    private static TextView selectedTextView = null;
    private static String lastSelectedWorkout = null;

    public static void setupScrollHighlight(
            ScrollView scrollView,
            LinearLayout trainingList,
            View topLine,
            View bottomLine,
            ImageView centerArrow,
            TextView workoutNameTextView,
            TextView workoutDescriptionTextView
    ) {
        scrollView.post(() -> {
            int spacing = bottomLine.getTop() - topLine.getTop() + bottomLine.getHeight();

            View topSpacer = new View(scrollView.getContext());
            topSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, spacing / 2
            ));
            trainingList.addView(topSpacer, 0);

            View bottomSpacer = new View(scrollView.getContext());
            bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, spacing
            ));
            trainingList.addView(bottomSpacer);

            // Initialize with "Своя" selected
            for (int i = 0; i < trainingList.getChildCount(); i++) {
                View child = trainingList.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    if ("своя".equalsIgnoreCase(textView.getText().toString())) {
                        selectedTextView = textView;
                        textView.setSelected(true);
                        lastSelectedWorkout = "своя";
                        String[] info = workoutInfoMap.get("своя"); // Fixed: Declare info as String[]
                        workoutNameTextView.setText(info[0]);
                        workoutDescriptionTextView.setText(info[1]);
                        int scrollY = textView.getTop() + textView.getHeight() / 2
                                - (bottomLine.getTop() - topLine.getTop()) / 2
                                - topLine.getTop();
                        scrollView.scrollTo(0, scrollY);
                        break;
                    }
                }
            }
        });

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int[] topLineLocation = new int[2];
            int[] bottomLineLocation = new int[2];
            topLine.getLocationOnScreen(topLineLocation);
            bottomLine.getLocationOnScreen(bottomLineLocation);

            int topY = topLineLocation[1];
            int bottomY = bottomLineLocation[1];
            int centerY = (topY + bottomY) / 2;

            TextView closestInCenter = null;
            int minDistance = Integer.MAX_VALUE;

            for (int i = 0; i < trainingList.getChildCount(); i++) {
                View child = trainingList.getChildAt(i);
                if (!(child instanceof TextView)) continue;

                TextView textView = (TextView) child;
                int[] childLocation = new int[2];
                child.getLocationOnScreen(childLocation);
                int childCenterY = childLocation[1] + child.getHeight() / 2;

                int distance = Math.abs(childCenterY - centerY);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestInCenter = textView;
                }
            }

            for (int i = 0; i < trainingList.getChildCount(); i++) {
                View child = trainingList.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    textView.setSelected(textView == closestInCenter);
                }
            }

            final TextView finalClosestInCenter = closestInCenter;

            if (finalClosestInCenter != null) {
                centerArrow.setSelected(true);
                if (selectedTextView != finalClosestInCenter) {
                    selectedTextView = finalClosestInCenter;
                    lastSelectedWorkout = selectedTextView.getText().toString();
                    String key = lastSelectedWorkout.toLowerCase();
                    if (workoutInfoMap.containsKey(key)) {
                        String[] info = workoutInfoMap.get(key);
                        workoutNameTextView.setText(info[0]);
                        workoutDescriptionTextView.setText(info[1]);
                    } else {
                        workoutNameTextView.setText(lastSelectedWorkout);
                        workoutDescriptionTextView.setText("Описание отсутствует.");
                    }
                }
            } else {
                centerArrow.setSelected(false);
                selectedTextView = null;
                lastSelectedWorkout = null;
            }

            if (scrollRunnable != null) {
                handler.removeCallbacks(scrollRunnable);
            }

            scrollRunnable = () -> {
                if (finalClosestInCenter != null) {
                    int scrollY = finalClosestInCenter.getTop() + finalClosestInCenter.getHeight() / 2
                            - (bottomLine.getTop() - topLine.getTop()) / 2
                            - topLine.getTop();
                    scrollView.smoothScrollTo(0, scrollY);
                }
            };
            handler.postDelayed(scrollRunnable, 150);
        });

        centerArrow.setOnClickListener(v -> {
            if (selectedTextView != null && !selectedTextView.getText().toString().isEmpty()) {
                String selectedWorkout = selectedTextView.getText().toString();
                Toast.makeText(
                        centerArrow.getContext(),
                        "Вы выбрали тренировку: " + selectedWorkout,
                        Toast.LENGTH_SHORT
                ).show();
                if (selectionListener != null) {
                    selectionListener.onWorkoutSelected(selectedWorkout.toLowerCase());
                }
            } else {
                Toast.makeText(
                        centerArrow.getContext(),
                        "Сначала выберите тренировку",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}