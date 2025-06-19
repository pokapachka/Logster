package com.example.logster;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class WorkoutProgramGenerator {
    private final PersonalWorkoutData userData;
    private final List<ExercisesAdapter.Exercise> availableExercises;
    private static final double[] AVAILABLE_DUMBBELL_WEIGHTS = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44,
            46, 48, 50, 52, 54, 56, 58, 60, 60, 62, 64, 66, 68, 70, 72, 74, 76, 78, 80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100,
            102, 104, 106, 108, 110, 112, 114, 116, 118, 120, 122, 124, 126, 128, 130, 132, 134, 136, 138, 140, 142, 144, 146, 148, 150, 152, 154,
            156, 158, 160, 162, 164, 166, 168, 170, 172, 174, 176, 178, 180, 182, 184, 186, 188, 190, 192, 194, 196, 198, 200};

    public static class WorkoutExercise {
        private final ExercisesAdapter.Exercise exercise;
        private final List<com.example.logster.Set> sets;
        private final boolean isToFailure;

        public WorkoutExercise(ExercisesAdapter.Exercise exercise, List<com.example.logster.Set> sets, boolean isToFailure) {
            this.exercise = exercise;
            this.sets = sets;
            this.isToFailure = isToFailure;
        }

        public ExercisesAdapter.Exercise getExercise() {
            return exercise;
        }

        public List<com.example.logster.Set> getSets() {
            return sets;
        }

        public boolean isToFailure() {
            return isToFailure;
        }
    }

    public WorkoutProgramGenerator(PersonalWorkoutData userData) {
        this.userData = userData;
        this.availableExercises = ExerciseList.getAllExercises();
    }

    public List<WorkoutExercise> generateProgram() {
        List<WorkoutExercise> program = new ArrayList<>();
        int trainingDays = userData.getTrainingDays().size();
        String goal = userData.getGoal();
        String motivation = userData.getMotivation();
        String fitnessLevel = userData.getFitnessLevel();
        String location = userData.getLocation();
        String bodyPart = userData.getBodyPart();
        int weight = parseIntOrDefault(userData.getWeight(), 70);
        int age = parseIntOrDefault(userData.getAge(), 30);

        int exercisesPerDay = calculateExercisesPerDay(fitnessLevel, goal, motivation);
        List<ExercisesAdapter.Exercise> filteredExercises = filterExercises(location, bodyPart, goal);

        Random random = new Random();
        Set<String> usedExercises = new HashSet<>();
        for (int day = 0; day < trainingDays; day++) {
            List<ExercisesAdapter.Exercise> dayExercises = new ArrayList<>(filteredExercises);
            Collections.shuffle(dayExercises, random);
            int exercisesToAdd = Math.min(exercisesPerDay, dayExercises.size());
            int added = 0;

            List<ExercisesAdapter.Exercise> priorityExercises = getPriorityExercises(dayExercises, goal, motivation);

            // Определяем тип тренировки для дня (сила, гипертрофия, выносливость)
            String dayType = getTrainingDayType(day, trainingDays);

            for (ExercisesAdapter.Exercise exercise : priorityExercises) {
                if (added < exercisesToAdd && !usedExercises.contains(exercise.getName())) {
                    addExerciseToProgram(program, exercise, fitnessLevel, goal, motivation, weight, age, location, dayType);
                    usedExercises.add(exercise.getName());
                    added++;
                }
            }

            for (ExercisesAdapter.Exercise exercise : dayExercises) {
                if (added < exercisesToAdd && !usedExercises.contains(exercise.getName())) {
                    addExerciseToProgram(program, exercise, fitnessLevel, goal, motivation, weight, age, location, dayType);
                    usedExercises.add(exercise.getName());
                    added++;
                }
            }

            if (day < trainingDays - 1) {
                usedExercises.clear();
            }
        }

        Log.d("WorkoutProgramGenerator", "Generated program with " + program.size() + " exercises for " + trainingDays + " days");
        return program;
    }

    private void addExerciseToProgram(List<WorkoutExercise> program, ExercisesAdapter.Exercise exercise,
                                      String fitnessLevel, String goal, String motivation, int userWeight, int age, String location, String dayType) {
        int[] setsAndReps = calculateSetsAndReps(fitnessLevel, goal, motivation, dayType);
        boolean isToFailure = dayType.equals("strength") && fitnessLevel.equals("advanced");
        List<com.example.logster.Set> sets = calculateSets(exercise, userWeight, age, fitnessLevel, location, setsAndReps[0], setsAndReps[1], isToFailure);
        program.add(new WorkoutExercise(exercise, sets, isToFailure));
    }

    private int calculateExercisesPerDay(String fitnessLevel, String goal, String motivation) {
        int baseCount;
        switch (fitnessLevel) {
            case "beginner":
                baseCount = 3; // Меньше упражнений для новичков
                break;
            case "intermediate":
                baseCount = 4;
                break;
            case "advanced":
                baseCount = 5;
                break;
            default:
                baseCount = 4;
        }
        if (motivation.equals("big") || motivation.equals("strong")) {
            baseCount = Math.min(baseCount + 1, 6); // Ограничение на максимум упражнений
        } else if (motivation.equals("leaner")) {
            baseCount = Math.max(baseCount - 1, 3);
        }
        return baseCount;
    }

    private List<ExercisesAdapter.Exercise> filterExercises(String location, String bodyPart, String goal) {
        List<ExercisesAdapter.Exercise> filtered = new ArrayList<>();
        boolean isGym = location.equals("gym");

        Log.d("WorkoutProgramGenerator", "Filtering exercises: location=" + location + ", bodyPart=" + bodyPart + ", goal=" + goal);
        for (ExercisesAdapter.Exercise exercise : availableExercises) {
            boolean matchesLocation = isGym || isHomeFriendly(exercise);
            String mappedBodyPart = mapBodyPartToMuscle(bodyPart);
            boolean matchesBodyPart = bodyPart.equals("dont") || exercise.getTags().contains(mappedBodyPart);
            boolean matchesGoal = goal.equals("slim") ? (exercise.getTags().contains("cardio") || matchesBodyPart) : matchesBodyPart;

            Log.d("WorkoutProgramGenerator", "Exercise: " + exercise.getName() +
                    ", matchesLocation=" + matchesLocation +
                    ", matchesBodyPart=" + matchesBodyPart +
                    ", matchesGoal=" + matchesGoal +
                    ", tags=" + exercise.getTags());

            if (matchesLocation && matchesGoal) {
                filtered.add(exercise);
            }
        }
        Log.d("WorkoutProgramGenerator", "Filtered exercises count: " + filtered.size());
        return filtered;
    }

    private boolean isHomeFriendly(ExercisesAdapter.Exercise exercise) {
        String name = exercise.getName().toLowerCase();
        boolean isHomeFriendly = name.contains("бёрпи") || name.contains("подтягивания") ||
                name.contains("скручивания") || name.contains("подъемы коленей") ||
                name.contains("подъемы ног") || name.contains("прыжки") ||
                name.contains("махи гирей") || name.contains("выпады") ||
                name.contains("планка") || name.contains("ягодичный мостик") ||
                name.contains("отжимания"); // Добавлены дополнительные домашние упражнения
        Log.d("WorkoutProgramGenerator", "Exercise " + exercise.getName() + " isHomeFriendly: " + isHomeFriendly);
        return isHomeFriendly;
    }

    private String mapBodyPartToMuscle(String bodyPart) {
        switch (bodyPart) {
            case "arms":
                return "arms"; // Соответствует тегу в ExerciseList
            case "back_p":
                return "back"; // Соответствует тегу в ExerciseList
            case "chest":
                return "chest";
            case "legs":
                return "legs";
            case "core":
                return "core";
            case "shoulders":
                return "shoulders";
            case "cardio":
                return "cardio";
            default:
                return "general"; // Для упражнений без явной категории
        }
    }

    private List<ExercisesAdapter.Exercise> getPriorityExercises(List<ExercisesAdapter.Exercise> exercises, String goal, String motivation) {
        List<ExercisesAdapter.Exercise> priority = new ArrayList<>();
        if (goal.equals("muscle") && (motivation.equals("big") || motivation.equals("strong"))) {
            for (ExercisesAdapter.Exercise exercise : exercises) {
                String name = exercise.getName().toLowerCase();
                if (name.contains("жим лежа") || name.contains("приседания") || name.contains("становая тяга") ||
                        name.contains("подтягивания") || name.contains("жим штанги")) {
                    priority.add(exercise);
                }
            }
        }
        return priority;
    }

    private String getTrainingDayType(int day, int totalDays) {
        // Периодизация: чередование типов тренировок
        if (totalDays <= 3) {
            return "hypertrophy"; // Для коротких программ фокус на гипертрофию
        }
        int cycle = day % 3;
        switch (cycle) {
            case 0:
                return "strength"; // День силы
            case 1:
                return "hypertrophy"; // День гипертрофии
            case 2:
                return "endurance"; // День выносливости
            default:
                return "hypertrophy";
        }
    }

    private int[] calculateSetsAndReps(String fitnessLevel, String goal, String motivation, String dayType) {
        int sets, baseReps;
        switch (fitnessLevel) {
            case "beginner":
                sets = 3;
                baseReps = dayType.equals("strength") ? 8 : dayType.equals("hypertrophy") ? 12 : 15;
                break;
            case "intermediate":
                sets = 4;
                baseReps = dayType.equals("strength") ? 6 : dayType.equals("hypertrophy") ? 10 : 12;
                break;
            case "advanced":
                sets = dayType.equals("strength") ? 5 : 4;
                baseReps = dayType.equals("strength") ? 4 : dayType.equals("hypertrophy") ? 8 : 10;
                break;
            default:
                sets = 3;
                baseReps = 12;
        }
        if (motivation.equals("big")) {
            baseReps = Math.max(baseReps - 2, 6);
            sets = Math.min(sets + 1, 6);
        } else if (motivation.equals("strong")) {
            baseReps = Math.max(baseReps - 4, 4);
            sets = Math.min(sets + 1, 6);
        } else if (motivation.equals("leaner")) {
            baseReps = Math.min(baseReps + 2, 15);
        }
        return new int[]{sets, baseReps};
    }

    private List<com.example.logster.Set> calculateSets(ExercisesAdapter.Exercise exercise, int userWeight, int age,
                                                        String fitnessLevel, String location, int numSets, int baseReps,
                                                        boolean isToFailure) {
        List<com.example.logster.Set> sets = new ArrayList<>();
        String name = exercise.getName().toLowerCase();

        // Упражнения без веса (кардио, скручивания, бёрпи и т.д.)
        if (location.equals("home") || exercise.getTags().contains("cardio") ||
                name.contains("скручивания") || name.contains("подъемы коленей") ||
                name.contains("подъемы ног") || name.contains("бёрпи") ||
                name.contains("прыжки") || name.contains("планка")) {
            for (int i = 0; i < numSets; i++) {
                int reps = baseReps + (i % 2 == 0 ? 2 : 0); // Легкая вариация повторений
                sets.add(new com.example.logster.Set(UUID.randomUUID().toString(), 0.0f, reps));
            }
            Log.d("WorkoutProgramGenerator", "Weight set to 0 for exercise: " + name + ", sets: " + numSets + ", reps: " + baseReps);
            return sets;
        }

        // Оценка 1RM и базового веса
        double estimated1RM = estimate1RM(userWeight, fitnessLevel, name);
        double baseWeight = estimated1RM * getIntensityPercentage(fitnessLevel, isToFailure);

        // Корректировка веса в зависимости от возраста
        baseWeight *= (age > 50 ? 0.7 : age > 40 ? 0.85 : age > 30 ? 0.95 : 1.0);

        // Корректировка веса в зависимости от упражнения
        if (name.contains("жим лежа") && !name.contains("наклонной")) {
            baseWeight *= 0.8; // Обычный жим лежа
        } else if (name.contains("жим гантелей") || name.contains("жим лежа на наклонной")) {
            baseWeight *= 0.7; // Жим гантелей и наклонный жим легче на ~15%
        } else if (name.contains("жим штанги сидя") || name.contains("жим штанги стоя") || name.contains("армейский жим")) {
            baseWeight *= 0.6; // Жим сидя/стоя легче на ~25%
        } else if (name.contains("приседания") || name.contains("становая тяга")) {
            baseWeight *= 1.2; // Тяжелее упражнения
        } else if (name.contains("подтягивания")) {
            baseWeight = 0; // Подтягивания с весом тела
        } else if (name.contains("махи гирей")) {
            baseWeight *= 0.5; // Легче для махов
        }

        double finalWeight = roundToNearestDumbbellWeight(baseWeight);

        // Прогрессия весов и повторений в подходах
        for (int i = 0; i < numSets; i++) {
            int reps = baseReps;
            float weight = (float) finalWeight;

            // Прогрессивная нагрузка
            if (i >= numSets - 2 && fitnessLevel.equals("advanced") && !isToFailure) {
                weight = (float) (finalWeight * 1.05); // +5% в последних подходах
                weight = (float) roundToNearestDumbbellWeight(weight);
                reps = Math.max(baseReps - 2, 6); // Minimum 6 reps for advanced
            } else if (isToFailure) {
                reps = Math.max(baseReps - 2, 4); // Minimum 4 reps for to-failure
                weight = (float) (finalWeight * 1.1); // +10% для подходов до отказа
                weight = (float) roundToNearestDumbbellWeight(weight);
            } else if (weight >= userWeight * 1.0) { // Adjusted threshold for heavy weights
                reps = Math.max(baseReps - 2, 6); // Minimum 6 reps for heavy weights
            } else if (weight <= userWeight * 0.4) { // Adjusted threshold for light weights
                reps = Math.min(baseReps + 4, 20); // More reps for lighter weights
            }

            sets.add(new com.example.logster.Set(UUID.randomUUID().toString(), weight, reps));
        }

        Log.d("WorkoutProgramGenerator", "Calculated sets for " + name + ": " + sets.size() +
                ", weight: " + finalWeight + ", to failure: " + isToFailure);
        return sets;
    }

    private double estimate1RM(int userWeight, String fitnessLevel, String exerciseName) {
        // Примерная оценка 1RM на основе веса тела и уровня подготовки
        double multiplier;
        switch (fitnessLevel) {
            case "beginner":
                multiplier = 0.4;
                break;
            case "intermediate":
                multiplier = 1.2;
                break;
            case "advanced":
                multiplier = 2.4;
                break;
            default:
                multiplier = 0.5;
        }
        // Корректировка для типа упражнения
        if (exerciseName.contains("жим лежа") && !exerciseName.contains("наклонной")) {
            multiplier *= 0.9; // Обычный жим лежа
        } else if (exerciseName.contains("жим гантелей") || exerciseName.contains("жим лежа на наклонной")) {
            multiplier *= 0.8; // Жим гантелей и наклонный жим легче
        } else if (exerciseName.contains("жим штанги сидя") || exerciseName.contains("жим штанги стоя") || exerciseName.contains("армейский жим")) {
            multiplier *= 0.7; // Жим сидя/стоя еще легче
        } else if (exerciseName.contains("приседания") || exerciseName.contains("становая тяга")) {
            multiplier *= 1.3; // Тяжелее упражнения
        }
        return userWeight * multiplier;
    }

    private double getIntensityPercentage(String fitnessLevel, boolean isToFailure) {
        // Интенсивность (% от 1RM) в зависимости от уровня подготовки и типа подхода
        if (isToFailure) {
            return fitnessLevel.equals("advanced") ? 0.95 : 0.90; // Increased for to-failure sets
        }
        switch (fitnessLevel) {
            case "beginner":
                return 0.6; // Unchanged: 60% of 1RM for beginners
            case "intermediate":
                return 0.80; // Increased from 0.75 to 80%
            case "advanced":
                return 0.90; // Increased from 0.85 to 90%
            default:
                return 0.7;
        }
    }

    private double roundToNearestDumbbellWeight(double weight) {
        double closestWeight = AVAILABLE_DUMBBELL_WEIGHTS[0];
        double minDifference = Math.abs(weight - closestWeight);

        for (double dumbbellWeight : AVAILABLE_DUMBBELL_WEIGHTS) {
            double difference = Math.abs(weight - dumbbellWeight);
            if (difference < minDifference) {
                minDifference = difference;
                closestWeight = dumbbellWeight;
            }
        }
        return closestWeight;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}