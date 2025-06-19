package com.example.logster;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PersonalWorkoutData {
    private String goal;
    private String motivation;
    private String fitnessLevel;
    private String height;
    private String weight;
    private String age;
    private String bodyPart;
    private String location;
    private List<String> trainingDays = new ArrayList<>();

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getGoal() {
        return goal != null ? goal : "";
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public String getMotivation() {
        return motivation != null ? motivation : "";
    }

    public void setFitnessLevel(String fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    public String getFitnessLevel() {
        return fitnessLevel != null ? fitnessLevel : "";
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHeight() {
        return height != null ? height : "";
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getWeight() {
        return weight != null ? weight : "";
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAge() {
        return age != null ? age : "";
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    public String getBodyPart() {
        return bodyPart != null ? bodyPart : "";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location != null ? location : "";
    }

    public void addTrainingDay(String day) {
        if (!trainingDays.contains(day)) {
            trainingDays.add(day);
        }
    }

    public void removeTrainingDay(String day) {
        trainingDays.remove(day);
    }

    public List<String> getTrainingDays() {
        return new ArrayList<>(trainingDays);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("goal", goal);
        json.put("motivation", motivation);
        json.put("fitnessLevel", fitnessLevel);
        json.put("height", height);
        json.put("weight", weight);
        json.put("age", age);
        json.put("bodyPart", bodyPart);
        json.put("location", location);
        json.put("trainingDays", trainingDays);
        return json;
    }
}