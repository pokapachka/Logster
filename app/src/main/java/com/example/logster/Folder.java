package com.example.logster;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Folder {
    public String id;
    public String name;
    public List<String> itemIds;

    public Folder(String id, String name) {
        this.id = id;
        this.name = name;
        this.itemIds = new ArrayList<>();
    }

    public void addItem(String itemId) {
        if (!itemIds.contains(itemId)) {
            itemIds.add(itemId);
        }
    }

    // Метод для получения количества элементов в папке
    public int getItemCount() {
        return itemIds != null ? itemIds.size() : 0;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        JSONArray itemIdsArray = new JSONArray();
        for (String itemId : itemIds) {
            itemIdsArray.put(itemId);
        }
        json.put("itemIds", itemIdsArray);
        return json;
    }

    public static Folder fromJson(JSONObject json) throws JSONException {
        String id = json.optString("id", UUID.randomUUID().toString());
        String name = json.getString("name");
        Folder folder = new Folder(id, name);
        JSONArray itemIdsArray = json.optJSONArray("itemIds"); // Используем optJSONArray для обработки null
        if (itemIdsArray != null) {
            for (int i = 0; i < itemIdsArray.length(); i++) {
                folder.itemIds.add(itemIdsArray.getString(i));
            }
        }
        return folder;
    }
}