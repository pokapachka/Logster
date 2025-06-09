package com.example.logster;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExerciseList {
    private static final List<ExercisesAdapter.Exercise> EXERCISES = new ArrayList<>();

    static {
        EXERCISES.add(new ExercisesAdapter.Exercise(1, "Жим лежа", Arrays.asList("Грудь", "Трицепс", "Плечи")));
        EXERCISES.add(new ExercisesAdapter.Exercise(2, "Приседания", Arrays.asList("Ноги", "Ягодицы", "Спина")));
        EXERCISES.add(new ExercisesAdapter.Exercise(3, "Бег", Arrays.asList("Кардио", "Ноги")));
        EXERCISES.add(new ExercisesAdapter.Exercise(4, "Становая тяга", Arrays.asList("Спина", "Ноги", "Ягодицы")));
        EXERCISES.add(new ExercisesAdapter.Exercise(5, "Подтягивания", Arrays.asList("Спина", "Бицепс", "Плечи")));
        Log.d("ExerciseList", "Initialized with " + EXERCISES.size() + " exercises");
    }

    public static List<ExercisesAdapter.Exercise> getAllExercises() {
        Log.d("ExerciseList", "Returning " + EXERCISES.size() + " exercises");
        return new ArrayList<>(EXERCISES); // Защитная копия
    }
}