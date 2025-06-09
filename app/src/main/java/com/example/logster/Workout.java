package com.example.logster;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Workout {
    public String id;
    public String name;
    public List<Integer> exerciseIds;
    public Map<Integer, List<Set>> exerciseSets;
    public List<String> dates;

    public Workout(String id, String name) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.exerciseIds = new ArrayList<>();
        this.exerciseSets = new HashMap<>();
        this.dates = new ArrayList<>();
    }

    public void addDate(String date) {
        if (!dates.contains(date)) dates.add(date);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        JSONArray exerciseIdsArray = new JSONArray();
        for (Integer exerciseId : exerciseIds) {
            exerciseIdsArray.put(exerciseId);
        }
        json.put("exerciseIds", exerciseIdsArray);
        JSONObject exerciseSetsJson = new JSONObject();
        for (Map.Entry<Integer, List<Set>> entry : exerciseSets.entrySet()) {
            JSONArray setsArray = new JSONArray();
            for (Set set : entry.getValue()) {
                setsArray.put(set.toJson());
            }
            exerciseSetsJson.put(String.valueOf(entry.getKey()), setsArray);
        }
        json.put("exerciseSets", exerciseSetsJson);
        JSONArray datesArray = new JSONArray();
        for (String date : dates) {
            datesArray.put(date);
        }
        json.put("dates", datesArray);
        return json;
    }

    public static Workout fromJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String name = json.getString("name");
        Workout workout = new Workout(id, name);
        JSONArray exerciseIdsArray = json.getJSONArray("exerciseIds");
        for (int i = 0; i < exerciseIdsArray.length(); i++) {
            workout.exerciseIds.add(exerciseIdsArray.getInt(i));
        }
        JSONObject exerciseSetsJson = json.getJSONObject("exerciseSets");
        for (Iterator<String> it = exerciseSetsJson.keys(); it.hasNext(); ) {
            String exerciseIdStr = it.next();
            Integer exerciseId = Integer.parseInt(exerciseIdStr);
            JSONArray setsArray = exerciseSetsJson.getJSONArray(exerciseIdStr);
            List<Set> sets = new ArrayList<>();
            for (int i = 0; i < setsArray.length(); i++) {
                sets.add(Set.fromJson(setsArray.getJSONObject(i)));
            }
            workout.exerciseSets.put(exerciseId, sets);
        }
        JSONArray datesArray = json.getJSONArray("dates");
        for (int i = 0; i < datesArray.length(); i++) {
            workout.dates.add(datesArray.getString(i));
        }
        return workout;
    }
}