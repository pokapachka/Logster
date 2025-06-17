package com.example.logster;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CompletedExercise {
    public int exerciseId;
    public List<Set> sets;

    public CompletedExercise(int exerciseId, List<Set> sets) {
        this.exerciseId = exerciseId;
        this.sets = new ArrayList<>(sets);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("exerciseId", exerciseId);
        JSONArray setsArray = new JSONArray();
        for (Set set : sets) {
            setsArray.put(set.toJson());
        }
        json.put("sets", setsArray);
        return json;
    }

    public static CompletedExercise fromJson(JSONObject json) throws JSONException {
        int exerciseId = json.getInt("exerciseId");
        JSONArray setsArray = json.getJSONArray("sets");
        List<Set> sets = new ArrayList<>();
        for (int i = 0; i < setsArray.length(); i++) {
            sets.add(Set.fromJson(setsArray.getJSONObject(i)));
        }
        return new CompletedExercise(exerciseId, sets);
    }
}