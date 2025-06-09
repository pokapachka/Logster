package com.example.logster;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Objects;
import java.util.UUID;

public class Set {
    private String id;
    private float weight;
    private int reps;

    public Set(String id, float weight, int reps) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.weight = weight;
        this.reps = reps;
    }

    public String getId() {
        return id;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("weight", weight);
        json.put("reps", reps);
        return json;
    }

    public static Set fromJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        float weight = (float) json.getDouble("weight");
        int reps = json.getInt("reps");
        return new Set(id, weight, reps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Set set = (Set) o;
        return Objects.equals(id, set.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}