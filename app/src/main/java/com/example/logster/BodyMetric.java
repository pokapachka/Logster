package com.example.logster;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public class BodyMetric {
    String id;
    String type;
    String value;
    long timestamp;

    BodyMetric(String id, String type, String value, long timestamp) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("type", type);
        json.put("value", value);
        json.put("timestamp", timestamp);
        return json;
    }

    static BodyMetric fromJson(JSONObject json) throws JSONException {
        String id = json.optString("id", UUID.randomUUID().toString());
        String type = json.getString("type");
        String value = json.getString("value");
        long timestamp = json.getLong("timestamp");
        return new BodyMetric(id, type, value, timestamp);
    }
}