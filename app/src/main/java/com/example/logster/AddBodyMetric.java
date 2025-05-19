package com.example.logster;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class AddBodyMetric {
    public interface MetricSelectionListener {
        void onMetricSelected(String metricKey);
        void onMetricValueSelected(String metricType, String value);
    }

    private static MetricSelectionListener selectionListener;

    public static void setMetricSelectionListener(MetricSelectionListener listener) {
        selectionListener = listener;
    }

    private static final Map<String, String[]> metricInfoMap = new HashMap<>();
    static {
        metricInfoMap.put("вес", new String[]{"Вес", "Ваш вес в килограммах"});
        metricInfoMap.put("рост", new String[]{"Рост", "Ваш рост в сантиметрах"});
        metricInfoMap.put("число", new String[]{"Число", "Произвольное число"});
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static TextView selectedMetricTextView = null;
    private static TextView selectedMainTextView = null;

    public static void setupNumberPicker(
            ScrollView mainScrollView,
            LinearLayout mainValueList,
            View mainTopLine,
            View mainBottomLine,
            ImageView centerArrow,
            String metricType
    ) {
        // Логирование для отладки
        Log.d("BodyMetricAdapter", "setupNumberPicker called with metricType: " + metricType);
        if (mainScrollView == null || mainValueList == null || mainTopLine == null ||
                mainBottomLine == null || centerArrow == null) {
            Log.e("BodyMetricAdapter", "Один или несколько элементов null в setupNumberPicker");
            Toast.makeText(mainScrollView.getContext(),
                    "Ошибка: элементы интерфейса не инициализированы", Toast.LENGTH_SHORT).show();
            return;
        }

        // Очистка существующих задач
        handler.removeCallbacksAndMessages(null);

        int mainMin, mainMax;
        switch (metricType.toLowerCase()) {
            case "вес":
                mainMin = 25;
                mainMax = 200;
                break;
            case "рост":
                mainMin = 50;
                mainMax = 250;
                break;
            case "число":
                mainMin = 1;
                mainMax = 100;
                break;
            default:
                Log.e("BodyMetricAdapter", "Неизвестный тип метрики: " + metricType);
                return;
        }

        Context context = mainScrollView.getContext();

        // Очистка списка перед заполнением
        mainValueList.removeAllViews();

        // Заполняем список целых чисел
        for (int i = mainMin; i <= mainMax; i++) {
            TextView textView = createNumberTextView(context, String.valueOf(i), true);
            mainValueList.addView(textView);
        }

        // Настройка прокрутки для целых чисел
        setupScrollView(
                mainScrollView,
                mainValueList,
                mainTopLine,
                mainBottomLine,
                centerArrow,
                (textView) -> selectedMainTextView = textView,
                String.valueOf(mainMin + (mainMax - mainMin) / 2)
        );

        centerArrow.setOnClickListener(v -> {
            if (selectedMainTextView != null && !selectedMainTextView.getText().toString().isEmpty()) {
                String value = selectedMainTextView.getText().toString();
                if (selectionListener != null) {
                    selectionListener.onMetricValueSelected(metricType.toLowerCase(), value);
                }
            } else {
                Toast.makeText(
                        centerArrow.getContext(),
                        "Сначала выберите значение",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    public static void setupMetricSelection(
            ScrollView scrollView,
            LinearLayout metricList,
            View topLine,
            View bottomLine,
            ImageView centerArrow,
            TextView metricNameTextView,
            TextView metricDescriptionTextView
    ) {
        // Код остаётся без изменений, так как экран add_metric работает корректно
        scrollView.post(() -> {
            int spacing = bottomLine.getTop() - topLine.getTop() + bottomLine.getHeight();

            View topSpacer = new View(scrollView.getContext());
            topSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, spacing / 2
            ));
            metricList.addView(topSpacer, 0);

            View bottomSpacer = new View(scrollView.getContext());
            bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, spacing
            ));
            metricList.addView(bottomSpacer);

            // Initialize with "вес" selected
            for (int i = 0; i < metricList.getChildCount(); i++) {
                View child = metricList.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    if ("вес".equalsIgnoreCase(textView.getText().toString())) {
                        selectedMetricTextView = textView;
                        textView.setSelected(true);
                        String[] info = metricInfoMap.get("вес");
                        metricNameTextView.setText(info[0]);
                        metricDescriptionTextView.setText(info[1]);
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

            for (int i = 0; i < metricList.getChildCount(); i++) {
                View child = metricList.getChildAt(i);
                if (!(child instanceof TextView)) continue;

                TextView textView = (TextView) child;
                int[] childLocation = new int[2];
                textView.getLocationOnScreen(childLocation);
                int childCenterY = childLocation[1] + textView.getHeight() / 2;

                int distance = Math.abs(childCenterY - centerY);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestInCenter = textView;
                }
            }

            for (int i = 0; i < metricList.getChildCount(); i++) {
                View child = metricList.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    textView.setSelected(textView == closestInCenter);
                }
            }

            final TextView finalClosestInCenter = closestInCenter;

            if (finalClosestInCenter != null) {
                centerArrow.setSelected(true);
                if (selectedMetricTextView != finalClosestInCenter) {
                    selectedMetricTextView = finalClosestInCenter;
                    String key = selectedMetricTextView.getText().toString().toLowerCase();
                    if (metricInfoMap.containsKey(key)) {
                        String[] info = metricInfoMap.get(key);
                        metricNameTextView.setText(info[0]);
                        metricDescriptionTextView.setText(info[1]);
                    } else {
                        metricNameTextView.setText(selectedMetricTextView.getText());
                        metricDescriptionTextView.setText("Описание отсутствует.");
                    }
                }
            } else {
                centerArrow.setSelected(false);
                selectedMetricTextView = null;
            }

            Runnable scrollRunnable = () -> {
                if (finalClosestInCenter != null) {
                    int scrollY = finalClosestInCenter.getTop() + finalClosestInCenter.getHeight() / 2
                            - (bottomLine.getTop() - topLine.getTop()) / 2
                            - topLine.getTop();
                    scrollView.smoothScrollTo(0, scrollY);
                }
            };
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(scrollRunnable, 150);
        });

        centerArrow.setOnClickListener(v -> {
            if (selectedMetricTextView != null && !selectedMetricTextView.getText().toString().isEmpty()) {
                String selectedMetric = selectedMetricTextView.getText().toString();
                if (selectionListener != null) {
                    selectionListener.onMetricSelected(selectedMetric.toLowerCase());
                }
            } else {
                Toast.makeText(
                        centerArrow.getContext(),
                        "Сначала выберите метрику",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private static void setupScrollView(
            ScrollView scrollView,
            LinearLayout valueList,
            View topLine,
            View bottomLine,
            ImageView centerArrow,
            OnTextViewSelectedListener listener,
            String defaultSelectedValue
    ) {
        scrollView.post(() -> {
            // Расстояние между линиями (100dp)
            int spacing = dpToPx(scrollView.getContext(), 100);

            // Верхний спейсер 0dp
            int topSpacerHeight = 0;

            View topSpacer = new View(scrollView.getContext());
            topSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, topSpacerHeight
            ));
            valueList.addView(topSpacer, 0);

            View bottomSpacer = new View(scrollView.getContext());
            bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, spacing
            ));
            valueList.addView(bottomSpacer);

            // Initialize with defaultSelectedValue
            TextView targetTextView = null;
            int targetIndex = -1;
            for (int i = 0; i < valueList.getChildCount(); i++) {
                View child = valueList.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    if (defaultSelectedValue.equalsIgnoreCase(textView.getText().toString())) {
                        targetTextView = textView;
                        targetIndex = i;
                        break;
                    }
                }
            }

            if (targetTextView != null) {
                targetTextView.setSelected(true);
                listener.onTextViewSelected(targetTextView);
                // Рассчитываем scrollY на основе индекса элемента
                int itemHeight = dpToPx(scrollView.getContext(), 70); // Высота TextView
                int scrollY = targetIndex * itemHeight - dpToPx(scrollView.getContext(), 50); // Центрирование
                scrollView.scrollTo(0, scrollY);
                centerArrow.setSelected(true);
                Log.d("BodyMetricAdapter", "Initial scrollY: " + scrollY + ", targetIndex: " + targetIndex);
            } else {
                Log.w("BodyMetricAdapter", "Target value not found: " + defaultSelectedValue);
            }
        });

        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            TextView closestInCenter = findClosestInCenter(valueList, topLine, bottomLine);
            updateSelection(valueList, closestInCenter, centerArrow, listener);

            // Эффект магнита
            final TextView finalClosestInCenter = closestInCenter;
            if (finalClosestInCenter != null) {
                Runnable scrollRunnable = () -> {
                    int scrollY = finalClosestInCenter.getTop() + finalClosestInCenter.getHeight() / 2
                            - dpToPx(scrollView.getContext(), 50);
                    scrollView.smoothScrollTo(0, scrollY);
                    Log.d("BodyMetricAdapter", "Magnet scrollY: " + scrollY);
                };
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(scrollRunnable, 150);
            } else {
                Log.w("BodyMetricAdapter", "ClosestInCenter is null");
            }
        });

        scrollView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    handler.removeCallbacksAndMessages(null);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacksAndMessages(null);
                    TextView closestInCenter = findClosestInCenter(valueList, topLine, bottomLine);
                    final TextView finalClosestInCenter = closestInCenter;
                    if (finalClosestInCenter != null) {
                        Runnable scrollRunnable = () -> {
                            int scrollY = finalClosestInCenter.getTop() + finalClosestInCenter.getHeight() / 2
                                    - dpToPx(scrollView.getContext(), 50);
                            scrollView.smoothScrollTo(0, scrollY);
                            Log.d("BodyMetricAdapter", "Touch magnet scrollY: " + scrollY);
                        };
                        handler.postDelayed(scrollRunnable, 150);
                    }
                    break;
            }
            return false;
        });
    }

    private static TextView createNumberTextView(Context context, String text, boolean isNumberPicker) {
        TextView textView = new TextView(context);
        textView.setText(text);
        LinearLayout.LayoutParams params;
        if (isNumberPicker) {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(context, 70));
            params.setMargins(0, dpToPx(context, -5), 0, dpToPx(context, -5));
            textView.setTextSize(40);
        } else {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(context, 70));
            textView.setTextSize(40);
        }
        textView.setLayoutParams(params);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setTextColor(android.graphics.Color.parseColor("#606062"));
        textView.setBackgroundResource(android.R.color.transparent);
        // Применяем селектор для белого выделения
        try {
            textView.setTextColor(context.getResources().getColorStateList(R.drawable.selected_item, null));
        } catch (Exception e) {
            Log.e("BodyMetricAdapter", "Ошибка загрузки селектора selected_item: " + e.getMessage());
            textView.setTextColor(android.graphics.Color.parseColor("#606062"));
        }
        return textView;
    }

    private static TextView findClosestInCenter(LinearLayout valueList, View topLine, View bottomLine) {
        int[] topLineLocation = new int[2];
        int[] bottomLineLocation = new int[2];
        topLine.getLocationInWindow(topLineLocation);
        bottomLine.getLocationInWindow(bottomLineLocation);

        int topY = topLineLocation[1];
        int bottomY = bottomLineLocation[1];
        int centerY = (topY + bottomY) / 2;

        TextView closestInCenter = null;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < valueList.getChildCount(); i++) {
            View child = valueList.getChildAt(i);
            if (!(child instanceof TextView)) continue;

            TextView textView = (TextView) child;
            int[] childLocation = new int[2];
            textView.getLocationInWindow(childLocation);
            int childCenterY = childLocation[1] + textView.getHeight() / 2;

            int distance = Math.abs(childCenterY - centerY);
            if (distance < minDistance) {
                minDistance = distance;
                closestInCenter = textView;
            }
        }
        Log.d("BodyMetricAdapter", "findClosestInCenter: closestInCenter=" + (closestInCenter != null));
        return closestInCenter;
    }

    private static void updateSelection(LinearLayout valueList, TextView closestInCenter, ImageView centerArrow, OnTextViewSelectedListener listener) {
        for (int i = 0; i < valueList.getChildCount(); i++) {
            View child = valueList.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setSelected(textView == closestInCenter);
            }
        }

        if (closestInCenter != null) {
            centerArrow.setSelected(true);
            listener.onTextViewSelected(closestInCenter);
        } else {
            centerArrow.setSelected(false);
            listener.onTextViewSelected(null);
        }
    }

    private interface OnTextViewSelectedListener {
        void onTextViewSelected(TextView textView);
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}