package com.example.logster;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class StatisticsActivity extends AppCompatActivity {
    private static final String TAG = "StatisticsActivity";
    private static final String PREFS_NAME = "WorkoutPrefs";
    private static final String METRICS_KEY = "key_metrics_body";
    private BottomNavigationManager navManager;
    private BottomSheets bottomSheets;
    private RelativeLayout rootLayout;
    private View sheetView;
    private FrameLayout.LayoutParams params;
    private int screenHeight;
    private int initialTopMargin;
    private boolean isShowing;
    private SharedPreferences sharedPreferences;
    private List<BodyMetric> bodyMetrics;
    private List<CompletedExercise> completedExercises;
    private Map<Float, String> dateMap;
    private Spinner periodSpinner;
    private Spinner exerciseTypeSpinner;
    private String currentPeriod = "Неделя";
    private String currentExerciseMode = "Максимальный вес";
    private int currentExerciseId = -1;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);

        // Инициализация SharedPreferences и данных
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bodyMetrics = loadBodyMetrics();
        completedExercises = loadCompletedExercises();
        Log.d(TAG, "onCreate: Loaded bodyMetrics size=" + bodyMetrics.size() + ", completedExercises size=" + completedExercises.size());

        // Инициализация корневого layout
        rootLayout = findViewById(R.id.statistics);

        titleTextView = findViewById(R.id.title_home);
        titleTextView.setText("Прогресс");
        Log.d(TAG, "onCreate: Title set to Вес");

        // Настройка навигации
        navManager = new BottomNavigationManager(rootLayout, this);
        navManager.setCurrentActivity("StatisticsActivity");

        // Настройка списков
        setupSpinners();

        // Настройка графика (по умолчанию вес)
        setupProgressChart();

        // Настройка нижней панели
        setupBottomSheet();

        // Скрытие системного UI
        hideSystemUI();
    }

    private void setupSpinners() {
        periodSpinner = findViewById(R.id.period_spinner);
        exerciseTypeSpinner = findViewById(R.id.exercise_type_spinner);

        // Настройка списка периодов
        List<String> periodOptions = new ArrayList<>();
        periodOptions.add("Неделя");
        periodOptions.add("Месяц");
        periodOptions.add("Год");
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, periodOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (parent.getBackground() == null) {
                    parent.setBackgroundResource(R.drawable.background_graf2);
                }
                return view;
            }
        };
        periodAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        periodSpinner.setAdapter(periodAdapter);
        periodSpinner.setSelection(0); // Устанавливаем "Неделя" по умолчанию
        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedPeriod = periodOptions.get(position);
                if (!selectedPeriod.equals(currentPeriod)) {
                    currentPeriod = selectedPeriod;
                    Log.d(TAG, "Период изменён на: " + currentPeriod);
                    refreshChart();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // Скрытие системного UI при открытии выпадающего списка
        periodSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideSystemUI();
            }
            return false; // Продолжаем стандартную обработку
        });

        // Настройка списка типов упражнений
        List<String> exerciseTypeOptions = new ArrayList<>();
        exerciseTypeOptions.add("Максимальный вес");
        exerciseTypeOptions.add("Все веса");
        exerciseTypeOptions.add("Повторы");
        ArrayAdapter<String> exerciseTypeAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, exerciseTypeOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (parent.getBackground() == null) {
                    parent.setBackgroundResource(R.drawable.background_graf2);
                }
                return view;
            }
        };
        exerciseTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        exerciseTypeSpinner.setAdapter(exerciseTypeAdapter);
        exerciseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMode = exerciseTypeOptions.get(position);
                if (!selectedMode.equals(currentExerciseMode)) {
                    currentExerciseMode = selectedMode;
                    Log.d(TAG, "Режим упражнений изменён на: " + currentExerciseMode);
                    if (currentExerciseId != -1) {
                        setupExerciseChart(currentExerciseId);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // Скрытие системного UI при открытии выпадающего списка
        exerciseTypeSpinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideSystemUI();
            }
            return false;
        });
    }

    private void refreshChart() {
        if (currentExerciseId != -1) {
            setupExerciseChart(currentExerciseId);
        } else {
            setupProgressChart();
        }
    }

    private void setupProgressChart() {
        LineChart chart = findViewById(R.id.progress_chart);
        Log.d(TAG, "setupProgressChart: Starting chart configuration for weight");

        List<BodyMetric> weightMetrics = filterByPeriod(bodyMetrics.stream()
                .filter(m -> "weight".equals(m.type) || "вес".equals(m.type))
                .sorted((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp))
                .collect(Collectors.toList()));
        Log.d(TAG, "setupProgressChart: Weight metrics found: " + weightMetrics.size());

        // Если нет данных за период, берём последнюю метрику
        if (weightMetrics.isEmpty() && !bodyMetrics.isEmpty()) {
            BodyMetric lastMetric = bodyMetrics.stream()
                    .filter(m -> "weight".equals(m.type) || "вес".equals(m.type))
                    .max((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp))
                    .orElse(null);
            if (lastMetric != null) {
                weightMetrics.add(lastMetric);
                Log.d(TAG, "setupProgressChart: Added last weight metric: " + lastMetric.value);
            }
        }

        List<Entry> entries = new ArrayList<>();
        dateMap = new HashMap<>();
        int index = 1;
        for (BodyMetric metric : weightMetrics) {
            try {
                String value = metric.value.replace(" кг", "").trim();
                float weight = Float.parseFloat(value);
                entries.add(new Entry(index, weight));
                LocalDate date = LocalDate.ofEpochDay(metric.timestamp / (24 * 60 * 60 * 1000));
                dateMap.put((float) index, date.format(DateTimeFormatter.ofPattern("dd.MM")));
                Log.d(TAG, "setupProgressChart: Added entry: index=" + index + ", weight=" + weight + ", date=" + dateMap.get((float) index));
                index++;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Ошибка парсинга веса: " + metric.value, e);
            }
        }
        Log.d(TAG, "setupProgressChart: Entries added: " + entries);

        configureChart(chart, entries, "Вес (кг)");
    }

    private void setupHeightChart() {
        LineChart chart = findViewById(R.id.progress_chart);
        Log.d(TAG, "setupHeightChart: Starting chart configuration for height");

        List<BodyMetric> heightMetrics = filterByPeriod(bodyMetrics.stream()
                .filter(m -> "height".equals(m.type) || "рост".equals(m.type))
                .sorted((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp))
                .collect(Collectors.toList()));
        Log.d(TAG, "setupHeightChart: Height metrics found: " + heightMetrics.size());

        // Если нет данных за период, берём последнюю метрику
        if (heightMetrics.isEmpty() && !bodyMetrics.isEmpty()) {
            BodyMetric lastMetric = bodyMetrics.stream()
                    .filter(m -> "height".equals(m.type) || "рост".equals(m.type))
                    .max((m1, m2) -> Long.compare(m1.timestamp, m2.timestamp))
                    .orElse(null);
            if (lastMetric != null) {
                heightMetrics.add(lastMetric);
                Log.d(TAG, "setupHeightChart: Added last height metric: " + lastMetric.value);
            }
        }

        List<Entry> entries = new ArrayList<>();
        dateMap = new HashMap<>();
        int index = 1;
        for (BodyMetric metric : heightMetrics) {
            try {
                String value = metric.value.replace(" см", "").trim();
                float height = Float.parseFloat(value);
                entries.add(new Entry(index, height));
                LocalDate date = LocalDate.ofEpochDay(metric.timestamp / (24 * 60 * 60 * 1000));
                dateMap.put((float) index, date.format(DateTimeFormatter.ofPattern("dd.MM")));
                Log.d(TAG, "setupHeightChart: Added entry: index=" + index + ", height=" + height + ", date=" + dateMap.get((float) index));
                index++;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Ошибка парсинга роста: " + metric.value, e);
            }
        }
        Log.d(TAG, "setupHeightChart: Entries added: " + entries);

        configureChart(chart, entries, "Рост (см)");
    }

    private void setupExerciseChart(int exerciseId) {
        currentExerciseId = exerciseId;
        LineChart chart = findViewById(R.id.progress_chart);
        Log.d(TAG, "setupExerciseChart: Starting chart configuration for exercise ID=" + exerciseId);

        Map<String, List<CompletedExercise>> datedExercises = new HashMap<>();
        String workoutsJson = sharedPreferences.getString("workouts", null);
        if (workoutsJson != null && !workoutsJson.isEmpty()) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                for (int i = 0; i < workoutsArray.length(); i++) {
                    Workout workout = Workout.fromJson(workoutsArray.getJSONObject(i));
                    for (Map.Entry<String, List<CompletedExercise>> entry : workout.completedDates.entrySet()) {
                        String date = entry.getKey();
                        for (CompletedExercise ex : entry.getValue()) {
                            if (ex.exerciseId == exerciseId) {
                                datedExercises.computeIfAbsent(date, k -> new ArrayList<>()).add(ex);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Ошибка загрузки тренировок: " + e.getMessage(), e);
            }
        }

        // Фильтруем по периоду, но сохраняем хотя бы одну тренировку
        datedExercises = filterExercisesByPeriod(datedExercises);

        List<String> sortedDates = new ArrayList<>(datedExercises.keySet());
        sortedDates.sort(String::compareTo);
        Log.d(TAG, "setupExerciseChart: Dated exercises found for " + sortedDates.size() + " dates");

        List<Entry> entries = new ArrayList<>();
        dateMap = new HashMap<>();
        int index = 1;
        for (String date : sortedDates) {
            if ("Максимальный вес".equals(currentExerciseMode)) {
                float maxWeight = 0;
                int completedSetsCount = 0;
                for (CompletedExercise exercise : datedExercises.get(date)) {
                    for (com.example.logster.Set set : exercise.sets) {
                        if (set.isCompleted()) {
                            maxWeight = Math.max(maxWeight, set.getWeight());
                            completedSetsCount++;
                        }
                    }
                }
                if (maxWeight > 0 || completedSetsCount > 0) {
                    entries.add(new Entry(index, maxWeight));
                    LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                    dateMap.put((float) index, localDate.format(DateTimeFormatter.ofPattern("dd.MM")));
                    Log.d(TAG, "setupExerciseChart: Added max weight entry: index=" + index + ", maxWeight=" + maxWeight + ", date=" + dateMap.get((float) index));
                    index++;
                }
            } else if ("Все веса".equals(currentExerciseMode)) {
                for (CompletedExercise exercise : datedExercises.get(date)) {
                    for (com.example.logster.Set set : exercise.sets) {
                        if (set.isCompleted()) {
                            entries.add(new Entry(index, set.getWeight()));
                            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                            dateMap.put((float) index, localDate.format(DateTimeFormatter.ofPattern("dd.MM")));
                            Log.d(TAG, "setupExerciseChart: Added all weights entry: index=" + index + ", weight=" + set.getWeight() + ", date=" + dateMap.get((float) index));
                            index++;
                        }
                    }
                }
            } else if ("Повторы".equals(currentExerciseMode)) {
                for (CompletedExercise exercise : datedExercises.get(date)) {
                    for (com.example.logster.Set set : exercise.sets) {
                        if (set.isCompleted()) {
                            entries.add(new Entry(index, set.getReps()));
                            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
                            dateMap.put((float) index, localDate.format(DateTimeFormatter.ofPattern("dd.MM")));
                            Log.d(TAG, "setupExerciseChart: Added repetitions entry: index=" + index + ", reps=" + set.getReps() + ", date=" + dateMap.get((float) index));
                            index++;
                        }
                    }
                }
            }
        }
        Log.d(TAG, "setupExerciseChart: Entries added: " + entries);

        String label = "Повторы".equals(currentExerciseMode) ? "Повторения" : "Вес (кг)";
        configureChart(chart, entries, label);
    }

    private List<BodyMetric> filterByPeriod(List<BodyMetric> metrics) {
        LocalDate now = LocalDate.now();
        long cutoffTimestamp;
        if ("Неделя".equals(currentPeriod)) {
            cutoffTimestamp = now.minusDays(7).toEpochDay() * 24 * 60 * 60 * 1000;
        } else if ("Месяц".equals(currentPeriod)) {
            cutoffTimestamp = now.minusMonths(1).toEpochDay() * 24 * 60 * 60 * 1000;
        } else {
            cutoffTimestamp = now.minusYears(1).toEpochDay() * 24 * 60 * 60 * 1000;
        }
        return metrics.stream()
                .filter(m -> m.timestamp >= cutoffTimestamp)
                .collect(Collectors.toList());
    }

    private Map<String, List<CompletedExercise>> filterExercisesByPeriod(Map<String, List<CompletedExercise>> exercises) {
        LocalDate now = LocalDate.now();
        LocalDate cutoffDate;
        if ("Неделя".equals(currentPeriod)) {
            cutoffDate = now.minusDays(7);
        } else if ("Месяц".equals(currentPeriod)) {
            cutoffDate = now.minusMonths(1);
        } else {
            cutoffDate = now.minusYears(1);
        }
        Map<String, List<CompletedExercise>> filtered = new HashMap<>();
        for (Map.Entry<String, List<CompletedExercise>> entry : exercises.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (!date.isBefore(cutoffDate)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        // Если нет данных в периоде, добавляем последнюю тренировку
        if (filtered.isEmpty() && !exercises.isEmpty()) {
            String latestDate = exercises.keySet().stream()
                    .max(String::compareTo)
                    .orElse(null);
            if (latestDate != null) {
                filtered.put(latestDate, exercises.get(latestDate));
                Log.d(TAG, "filterExercisesByPeriod: Added latest exercise date: " + latestDate);
            }
        }
        return filtered;
    }

    private void configureChart(LineChart chart, List<Entry> entries, String label) {
        chart.setNoDataText("Выберите данные из списка");
        chart.setNoDataTextColor(Color.WHITE);
        chart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO).setTextSize(dpToPx(15));
        chart.getPaint(com.github.mikephil.charting.charts.Chart.PAINT_INFO).setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        Log.d(TAG, "configureChart: No data text set to 'Выберите данные из списка' with white color and 15sp");

        if (entries.isEmpty()) {
            chart.invalidate();
            return;
        }

        Map<Float, String> originalYValues = new HashMap<>();
        Set<Float> uniqueYValues = new HashSet<>();
        for (Entry entry : entries) {
            originalYValues.put(entry.getY(), String.valueOf(entry.getY()));
            uniqueYValues.add(entry.getY());
        }
        Log.d(TAG, "configureChart: Original Y values mapped: " + originalYValues);

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.WHITE);
        dataSet.setCircleColor(Color.WHITE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(Color.TRANSPARENT);
        dataSet.setHighlightLineWidth(1f);
        dataSet.setDrawHorizontalHighlightIndicator(true);
        dataSet.setDrawVerticalHighlightIndicator(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value);
            }
        });
        Log.d(TAG, "configureChart: LineDataSet configured");

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        Log.d(TAG, "configureChart: LineData set to chart");

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(true);
        chart.getAxisRight().setTextColor(Color.WHITE);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setGranularity(1f);
        chart.getAxisRight().setGranularityEnabled(true);
        chart.getAxisRight().setLabelCount(uniqueYValues.size(), true);
        chart.getAxisRight().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (uniqueYValues.contains(value)) {
                    return String.format("%.0f", value);
                }
                return "";
            }
        });

        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextSize(15f);
        xAxis.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        xAxis.setYOffset(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String date = dateMap.get(value);
                return date != null ? date : "";
            }
        });

        // Установка масштаба оси X
        float minX, maxX;
        if (entries.size() == 1) {
            minX = 0f;
            maxX = 2f; // Диапазон [0, 2] для одной точки, x=1.0 в центре
        } else {
            minX = 0f;
            maxX = entries.size() + 1f; // Добавляем 1 для симметрии
        }
        chart.getXAxis().setAxisMinimum(minX);
        chart.getXAxis().setAxisMaximum(maxX);
        Log.d(TAG, "configureChart: XAxis set to min=" + minX + ", max=" + maxX + " for " + entries.size() + " entries");

        float minY = entries.stream().map(Entry::getY).min(Float::compare).orElse(0f) - 5f;
        float maxY = entries.stream().map(Entry::getY).max(Float::compare).orElse(100f) + 5f;
        chart.getAxisRight().setAxisMinimum(minY);
        chart.getAxisRight().setAxisMaximum(maxY);
        Log.d(TAG, "configureChart: Y-axis range set: min=" + minY + ", max=" + maxY);

        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDragEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraOffsets(20f, 20f, 20f, 20f);
        chart.setHighlightPerTapEnabled(true);
        Log.d(TAG, "configureChart: Chart interaction configured");

        ViewGroup.LayoutParams layoutParams = chart.getLayoutParams();
        layoutParams.height = dpToPx(200);
        chart.setLayoutParams(layoutParams);
        Log.d(TAG, "configureChart: Chart height set to " + layoutParams.height + "px");

        chart.setMinimumWidth(0);
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        chart.setLayoutParams(layoutParams);
        chart.setBackgroundColor(Color.parseColor("#26262B"));
        Log.d(TAG, "configureChart: Chart width set to MATCH_PARENT");

        chart.post(() -> {
            if (entries.isEmpty() || isFinishing() || isDestroyed()) {
                Log.w(TAG, "configureChart: No entries or activity is finishing");
                return;
            }
            float viewportWidth = chart.getWidth();
            float viewportHeight = chart.getHeight();
            if (viewportWidth == 0 || viewportHeight == 0) {
                Log.w(TAG, "configureChart: Viewport size is 0, skipping centering");
                return;
            }
            Log.d(TAG, "configureChart: Viewport size: width=" + viewportWidth + ", height=" + viewportHeight);

            // Рассчитываем среднюю позицию всех точек
            float sumX = 0f;
            float avgY = 0f;
            for (Entry entry : entries) {
                sumX += entry.getX();
                avgY += entry.getY();
            }
            float centerX = sumX / entries.size();
            float centerY = avgY / entries.size();

            // Устанавливаем область видимости для центрирования
            float viewportWidthInDataUnits = entries.size() == 1 ? 2f : entries.size(); // Ширина области видимости
            float viewportStartX = centerX - (viewportWidthInDataUnits / 2f);

            // Корректируем, чтобы не выйти за границы оси X
            if (viewportStartX < minX) {
                viewportStartX = minX;
            }
            if (viewportStartX + viewportWidthInDataUnits > maxX) {
                viewportStartX = maxX - viewportWidthInDataUnits;
            }

            chart.zoom(1f, 1f, 0f, 0f, YAxis.AxisDependency.RIGHT); // Сбрасываем масштаб
            chart.moveViewToX(viewportStartX); // Устанавливаем начало области видимости
            Log.d(TAG, "configureChart: Centered at X=" + centerX + ", Y=" + centerY + ", viewportStartX=" + viewportStartX);
        });

        dataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
        Log.d(TAG, "configureChart: Chart invalidated");
    }

    private void setupBottomSheet() {
        bottomSheets = new BottomSheets(this, R.layout.statistic_item);
        FrameLayout addStatistic = findViewById(R.id.add_statistic);
        if (addStatistic != null) {
            addStatistic.setOnClickListener(v -> {
                displayStatisticsInBottomSheet();
                bottomSheets.showWithLimitedHeightAndCallback(() -> {
                    Log.d(TAG, "Нижняя панель закрыта");
                });
            });
        } else {
            Log.e(TAG, "setupBottomSheet: add_statistic FrameLayout not found");
        }
    }

    private void displayStatisticsInBottomSheet() {
        sheetView = bottomSheets.getContentView();
        if (sheetView == null) {
            Log.e(TAG, "displayStatisticsInBottomSheet: sheetView is null");
            return;
        }

        LinearLayout statisticContainer = sheetView.findViewById(R.id.statistic_container);
        if (statisticContainer == null) {
            Log.e(TAG, "displayStatisticsInBottomSheet: statistic_container not found");
            return;
        }
        statisticContainer.removeAllViews();
        Log.d(TAG, "displayStatisticsInBottomSheet: Cleared statistic_container");

        TextView titleStatistic = sheetView.findViewById(R.id.title_statistic);
        TextView textStatistic = sheetView.findViewById(R.id.text_statistic);
        if (titleStatistic != null && textStatistic != null) {
            titleStatistic.setText("Выберите");
            textStatistic.setText("Выбор из списка роста, веса или упражнения");
            Log.d(TAG, "displayStatisticsInBottomSheet: Set title and text to default values");
        } else {
            Log.w(TAG, "displayStatisticsInBottomSheet: title_statistic or text_statistic not found");
        }

        List<BodyMetric> bodyMetrics = loadBodyMetrics();
        List<CompletedExercise> completedExercises = loadCompletedExercises();
        Log.d(TAG, "displayStatisticsInBottomSheet: Loaded bodyMetrics size=" + bodyMetrics.size() +
                ", completedExercises size=" + completedExercises.size());

        for (BodyMetric metric : bodyMetrics) {
            Log.d(TAG, "displayStatisticsInBottomSheet: BodyMetric: type=" + metric.type +
                    ", value=" + metric.value + ", timestamp=" + metric.timestamp);
        }

        StatisticsAdapter adapter = new StatisticsAdapter(this, item -> {
            String newTitle = "";
            if (item instanceof StatisticsAdapter.MetricItem) {
                StatisticsAdapter.MetricItem metricItem = (StatisticsAdapter.MetricItem) item;
                Log.d(TAG, "Clicked metric: " + metricItem.name + ", type=" + metricItem.type);
                currentExerciseId = -1;
                if (metricItem.type == StatisticsAdapter.TYPE_WEIGHT) {
                    newTitle = "Вес";
                    setupProgressChart();
                } else if (metricItem.type == StatisticsAdapter.TYPE_HEIGHT) {
                    newTitle = "Рост";
                    setupHeightChart();
                }
            } else if (item instanceof StatisticsAdapter.ExerciseItem) {
                StatisticsAdapter.ExerciseItem exerciseItem = (StatisticsAdapter.ExerciseItem) item;
                Log.d(TAG, "Clicked exercise: " + exerciseItem.name + ", id=" + exerciseItem.exerciseId);
                newTitle = exerciseItem.name; // Название упражнения
                setupExerciseChart(exerciseItem.exerciseId);
            }
            updateTitleWithAnimation(newTitle); // Обновляем заголовок с анимацией
            bottomSheets.hide(() -> Log.d(TAG, "Bottom sheet hidden after selection"));
        });
        adapter.updateData(bodyMetrics, completedExercises);
        Log.d(TAG, "displayStatisticsInBottomSheet: Adapter item count after updateData: " + adapter.getItemCount());

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < adapter.getItemCount(); i++) {
            View itemView = inflater.inflate(R.layout.item_exercise, statisticContainer, false);
            RecyclerView.ViewHolder holder = new StatisticsAdapter.ItemViewHolder(itemView);

            adapter.onBindViewHolder(holder, i);
            statisticContainer.addView(itemView);
            Log.d(TAG, "displayStatisticsInBottomSheet: Added item at position " + i + ", viewType=" + adapter.getItemViewType(i));
        }
    }

    private List<BodyMetric> loadBodyMetrics() {
        List<BodyMetric> loadedMetrics = new ArrayList<>();
        String metricsJson = sharedPreferences.getString(METRICS_KEY, null);
        Log.d(TAG, "loadBodyMetrics: Raw JSON from SharedPreferences: " + metricsJson);
        if (metricsJson != null && !metricsJson.isEmpty()) {
            try {
                JSONArray metricsArray = new JSONArray(metricsJson);
                for (int i = 0; i < metricsArray.length(); i++) {
                    BodyMetric metric = BodyMetric.fromJson(metricsArray.getJSONObject(i));
                    loadedMetrics.add(metric);
                    Log.d(TAG, "loadBodyMetrics: Loaded metric: type=" + metric.type +
                            ", value=" + metric.value + ", timestamp=" + metric.timestamp);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Ошибка загрузки метрик: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "loadBodyMetrics: No metrics found in SharedPreferences");
        }
        this.bodyMetrics = loadedMetrics;
        return loadedMetrics;
    }

    private List<CompletedExercise> loadCompletedExercises() {
        List<CompletedExercise> exercises = new ArrayList<>();
        String workoutsJson = sharedPreferences.getString("workouts", null);
        Log.d(TAG, "loadCompletedExercises: Raw workouts JSON: " + workoutsJson);
        if (workoutsJson != null && !workoutsJson.isEmpty()) {
            try {
                JSONArray workoutsArray = new JSONArray(workoutsJson);
                for (int i = 0; i < workoutsArray.length(); i++) {
                    Workout workout = Workout.fromJson(workoutsArray.getJSONObject(i));
                    for (List<CompletedExercise> completedList : workout.completedDates.values()) {
                        exercises.addAll(completedList);
                        Log.d(TAG, "loadCompletedExercises: Added " + completedList.size() +
                                " exercises from workout: " + workout.name);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Ошибка загрузки тренировок: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "loadCompletedExercises: No workouts found in SharedPreferences");
        }
        Log.d(TAG, "loadCompletedExercises: Total exercises loaded: " + exercises.size());
        return exercises;
    }

    private void showBottomSheet() {
        if (sheetView == null || rootLayout == null) {
            Log.e(TAG, "showBottomSheet: sheetView или rootLayout равны null");
            return;
        }
        screenHeight = rootLayout.getHeight();
        if (screenHeight == 0) {
            screenHeight = getResources().getDisplayMetrics().heightPixels;
        }
        int limitedHeight = dpToPx(350);
        int limitedTopMargin = screenHeight - limitedHeight;
        params.topMargin = screenHeight;
        sheetView.setLayoutParams(params);
        if (sheetView.getParent() == null) {
            rootLayout.addView(sheetView);
            Log.d(TAG, "showBottomSheet: sheetView добавлен в rootLayout");
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
            private float downY;
            private float totalDeltaY;
            private long startTime;

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
                        if (totalDeltaY > dpToPx(120) || (totalDeltaY > dpToPx(50) && duration < 150)) {
                            hideBottomSheet(null);
                        } else {
                            resetBottomSheetPosition();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        View closeButton = sheetView.findViewById(R.id.close);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hideBottomSheet(null));
        }
    }

    private void hideBottomSheet(Runnable onHidden) {
        if (sheetView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, screenHeight);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                sheetView.setVisibility(View.GONE);
                rootLayout.removeView(sheetView);
                isShowing = false;
                if (onHidden != null) {
                    onHidden.run();
                }
            }
        });
        animator.start();
    }

    private void resetBottomSheetPosition() {
        if (sheetView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(params.topMargin, initialTopMargin);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            params.topMargin = (int) animation.getAnimatedValue();
            sheetView.setLayoutParams(params);
        });
        animator.start();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    private void updateTitleWithAnimation(String newTitle) {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            titleTextView.setAlpha(alpha);
            if (alpha == 0f) {
                titleTextView.setText(newTitle);
                ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.addUpdateListener(fadeAnimation -> titleTextView.setAlpha((float) fadeAnimation.getAnimatedValue()));
                fadeIn.start();
            }
        });
        animator.start();
        Log.d(TAG, "updateTitleWithAnimation: Title changed to " + newTitle);
    }
}