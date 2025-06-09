package com.example.logster;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class RegisterContext {
    private static final String TAG = "RegisterContext";
    public static final String BASE_URL = "https://zzpkcqipkfitozrhbxbt.supabase.co";
    private static final String REST_URL = BASE_URL + "/rest/v1/profiles";
    private static final String MESSAGES_URL = BASE_URL + "/rest/v1/messages";
    private static final String CHECK_USER_URL = REST_URL + "?select=email,username,id";
    private static final String AUTH_URL = BASE_URL + "/auth/v1/signup";
    private static final String LOGIN_URL = BASE_URL + "/auth/v1/token?grant_type=password";
    private static final String REFRESH_TOKEN_URL = BASE_URL + "/auth/v1/token?grant_type=refresh_token";
    private static final String STORAGE_URL = BASE_URL + "/storage/v1/object/avatars/";
    private static final String CHAT_USERS_URL = BASE_URL + "/rest/v1/chat_users";
    private static final String ADMIN_USERS_URL = BASE_URL + "/auth/v1/admin/users";
    private static final String REALTIME_URL = BASE_URL + "/realtime/v1/websocket?apikey=%s&vsn=1.0.0";
    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp6cGtjcWlwa2ZpdG96cmhieGJ0Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0ODUwMzcwOSwiZXhwIjoyMDY0MDc5NzA5fQ.lxoPr5OHjvKqBSd1CC5HBIhJzH4p4NlzVF8SwER5g-0";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp6cGtjcWlwa2ZpdG96cmhieGJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg1MDM3MDksImV4cCI6MjA2NDA3OTcwOX0.ZpzVkbGMBQEVRuFzXPg5lj_hdF3iGN76-GqlUHrvK2o";

    private static WebSocket webSocket;
    private static String webSocketChannelRef;
    private static boolean isWebSocketConnected = false;
    private static final long RECONNECT_DELAY_MS = 5000;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface RealtimeCallback {
        void onNewMessage(Message message);
    }

    public static class ProfileData {
        public String username;
        public String bio;
        public String imageUrl;

        public ProfileData(String username, String bio, String imageUrl) {
            this.username = username;
            this.bio = bio;
            this.imageUrl = imageUrl;
        }
    }

    public static class Message {
        public String id;
        public String userId;
        public String sender_login;
        public String content;
        public String createdAt;
        public String user_image;

        public Message(String id, String userId, String sender_login, String content, String createdAt, String user_image) {
            this.id = id;
            this.userId = userId;
            this.sender_login = sender_login;
            this.content = content;
            this.createdAt = createdAt;
            this.user_image = user_image;
        }
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null) != null;
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        cleanupRealtime();
    }

    public static void fetchMessages(Context context, Callback<List<Message>> callback, RealtimeCallback realtimeCallback) {
        cleanupRealtime();
        new AsyncTask<Void, Void, List<Message>>() {
            private String error;

            @Override
            protected List<Message> doInBackground(Void... voids) {
                try {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .build();
                    Request request = new Request.Builder()
                            .url(BASE_URL + "/rest/v1/messages?select=*,profiles(sender_login:username,user_image:image)&chat_id=eq.1&order=created_at.desc")
                            .header("Authorization", ANON_KEY)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .get()
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    Log.d(TAG, "FetchMessages Response: Code " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        error = "Failed to load messages: HTTP " + response.code();
                        return null;
                    }
                    JSONArray messagesArray = new JSONArray(responseBody);
                    List<Message> messages = new ArrayList<>();
                    for (int i = 0; i < messagesArray.length(); i++) {
                        JSONObject msgObj = messagesArray.getJSONObject(i);
                        JSONObject profile = msgObj.getJSONObject("profiles");
                        Message message = new Message(
                                msgObj.getString("id"),
                                msgObj.optString("user_id", null),
                                profile.getString("sender_login"),
                                msgObj.getString("content"),
                                msgObj.getString("created_at"),
                                profile.optString("user_image", null)
                        );
                        messages.add(message);
                    }
                    return messages;
                } catch (Exception e) {
                    error = "Error loading messages: " + e.getMessage();
                    Log.e(TAG, "Fetch messages error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Message> messages) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(messages);
                    setupRealtimeSubscription(context, realtimeCallback);
                }
            }
        }.execute();
    }

    private static void setupRealtimeSubscription(Context context, RealtimeCallback realtimeCallback) {
        if (isWebSocketConnected && webSocket != null) {
            Log.d(TAG, "WebSocket already connected, skipping setup");
            return;
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .build();
            String wsUrl = String.format(REALTIME_URL, ANON_KEY.replace("Bearer ", ""));
            Request request = new Request.Builder().url(wsUrl).build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    isWebSocketConnected = true;
                    Log.d(TAG, "WebSocket opened successfully");
                    try {
                        JSONObject joinMessage = new JSONObject();
                        joinMessage.put("topic", "realtime:public:messages");
                        joinMessage.put("event", "phx_join");
                        joinMessage.put("payload", new JSONObject().put("config", new JSONObject()));
                        joinMessage.put("ref", "1");
                        webSocket.send(joinMessage.toString());
                        webSocketChannelRef = "1";
                        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("realtime_channel_ref", webSocketChannelRef).apply();
                        Log.d(TAG, "Sent phx_join for messages channel");
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending join message: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    Log.d(TAG, "WebSocket received: " + text);
                    try {
                        JSONObject message = new JSONObject(text);
                        String topic = message.optString("topic", "");
                        String event = message.optString("event", "");
                        JSONObject payload = message.optJSONObject("payload");

                        if ("realtime:public:messages".equals(topic) && "INSERT".equals(event) && payload != null) {
                            JSONObject record = payload.getJSONObject("record");
                            JSONObject profile = record.getJSONObject("profiles");
                            Message newMessage = new Message(
                                    record.getString("id"),
                                    record.optString("user_id", null),
                                    profile.getString("sender_login"),
                                    record.getString("content"),
                                    record.getString("created_at"),
                                    profile.optString("user_image", null)
                            );
                            if (realtimeCallback != null) {
                                realtimeCallback.onNewMessage(newMessage);
                                Log.d(TAG, "New message received via WebSocket: " + newMessage.content);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing WebSocket message: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    isWebSocketConnected = false;
                    Log.e(TAG, "WebSocket failure: " + t.getMessage(), t);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (!isWebSocketConnected) {
                            Log.d(TAG, "Attempting WebSocket reconnection");
                            setupRealtimeSubscription(context, realtimeCallback);
                        }
                    }, RECONNECT_DELAY_MS);
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    isWebSocketConnected = false;
                    Log.d(TAG, "WebSocket closing: code=" + code + ", reason=" + reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    isWebSocketConnected = false;
                    Log.d(TAG, "WebSocket closed: code=" + code + ", reason=" + reason);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Realtime subscription: " + e.getMessage(), e);
        }
    }

    public static void cleanupRealtime() {
        if (webSocket != null && isWebSocketConnected) {
            try {
                JSONObject leaveMessage = new JSONObject();
                leaveMessage.put("topic", "realtime:public:messages");
                leaveMessage.put("event", "phx_leave");
                leaveMessage.put("payload", new JSONObject());
                leaveMessage.put("ref", webSocketChannelRef);
                webSocket.send(leaveMessage.toString());
                webSocket.close(1000, "Normal closure");
                Log.d(TAG, "WebSocket channel unsubscribed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing WebSocket: " + e.getMessage(), e);
            }
            webSocket = null;
            webSocketChannelRef = null;
            isWebSocketConnected = false;
        }
    }

    public static void sendMessage(Context context, String content, Callback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            private String error;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String accessToken = prefs.getString("access_token", null);
                    String userId = prefs.getString("user_id", null);
                    String username = prefs.getString("username", null);
                    String userImage = prefs.getString("image_url", null);
                    if (accessToken == null || userId == null || username == null) {
                        error = "User not authenticated or missing profile data";
                        Log.e(TAG, "SendMessage error: accessToken=" + accessToken + ", userId=" + userId + ", username=" + username);
                        return null;
                    }

                    OkHttpClient client = new OkHttpClient();
                    String encodedUserId = URLEncoder.encode(userId, "UTF-8");
                    Request checkChatRequest = new Request.Builder()
                            .url(CHAT_USERS_URL + "?chat_id=eq.1&user_id=eq." + encodedUserId)
                            .header("Authorization", TOKEN)
                            .header("apikey", TOKEN.replace("Bearer ", ""))
                            .header("Content-Type", "application/json")
                            .get()
                            .build();
                    Response checkChatResponse = client.newCall(checkChatRequest).execute();
                    String checkChatBody = checkChatResponse.body() != null ? checkChatResponse.body().string() : "";
                    Log.d(TAG, "CheckChat Response: Code " + checkChatResponse.code() + ", Body: " + checkChatBody);

                    if (checkChatResponse.isSuccessful() && new JSONArray(checkChatBody).length() == 0) {
                        JSONObject chatUserData = new JSONObject();
                        chatUserData.put("chat_id", 1);
                        chatUserData.put("user_id", userId);
                        RequestBody chatUserBody = RequestBody.create(chatUserData.toString(), MediaType.parse("application/json"));
                        Request chatUserRequest = new Request.Builder()
                                .url(CHAT_USERS_URL)
                                .header("Authorization", TOKEN)
                                .header("apikey", TOKEN.replace("Bearer ", ""))
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .post(chatUserBody)
                                .build();
                        Response chatUserResponse = client.newCall(chatUserRequest).execute();
                        String chatUserResponseBody = chatUserResponse.body() != null ? chatUserResponse.body().string() : "";
                        Log.d(TAG, "AddChatUser Response: Code " + checkChatResponse.code() + ", Body: " + checkChatBody);
                        if (!chatUserResponse.isSuccessful()) {
                            error = "Failed to add user to chat: HTTP " + chatUserResponse.code();
                            return null;
                        }
                    }

                    JSONObject messageData = new JSONObject();
                    messageData.put("chat_id", 1);
                    messageData.put("user_id", userId);
                    messageData.put("sender_login", username);
                    messageData.put("content", content);
                    if (userImage != null) {
                        messageData.put("user_image", userImage);
                    }

                    RequestBody messageBody = RequestBody.create(messageData.toString(), MediaType.parse("application/json"));
                    Request messageRequest = new Request.Builder()
                            .url(MESSAGES_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .post(messageBody)
                            .build();
                    Response response = client.newCall(messageRequest).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "SendMessage Response: Code " + response.code() + ", Body: " + responseBody);

                    if (response.code() == 401 && responseBody.contains("JWT expired")) {
                        String refreshToken = prefs.getString("refresh_token", null);
                        if (refreshToken != null) {
                            try {
                                JSONObject refreshData = new JSONObject();
                                refreshData.put("refresh_token", refreshToken);
                                RequestBody refreshBody = RequestBody.create(refreshData.toString(), MediaType.parse("application/json"));
                                Request refreshRequest = new Request.Builder()
                                        .url(REFRESH_TOKEN_URL)
                                        .header("apikey", ANON_KEY)
                                        .header("Content-Type", "application/json")
                                        .post(refreshBody)
                                        .build();
                                Response refreshResponse = client.newCall(refreshRequest).execute();
                                String refreshResponseBody = refreshResponse.body() != null ? refreshResponse.body().string() : "";
                                if (refreshResponse.isSuccessful()) {
                                    JSONObject refreshJson = new JSONObject(refreshResponseBody);
                                    String newAccessToken = refreshJson.getString("access_token");
                                    String newRefreshToken = refreshJson.getString("refresh_token");
                                    prefs.edit()
                                            .putString("access_token", newAccessToken)
                                            .putString("refresh_token", newRefreshToken)
                                            .apply();

                                    Request retryRequest = new Request.Builder()
                                            .url(MESSAGES_URL)
                                            .header("Authorization", "Bearer " + newAccessToken)
                                            .header("apikey", ANON_KEY)
                                            .header("Content-Type", "application/json")
                                            .header("Prefer", "return=minimal")
                                            .post(messageBody)
                                            .build();
                                    Response retryResponse = client.newCall(retryRequest).execute();
                                    if (retryResponse.isSuccessful()) {
                                        return null;
                                    } else {
                                        error = "Failed to send message after token refresh: HTTP " + retryResponse.code();
                                        return null;
                                    }
                                } else {
                                    error = "Failed to refresh token: HTTP " + refreshResponse.code();
                                    logout(context);
                                    return null;
                                }
                            } catch (Exception e) {
                                error = "Error refreshing token: " + e.getMessage();
                                logout(context);
                                return null;
                            }
                        } else {
                            error = "No refresh token available, please log in again";
                            logout(context);
                            return null;
                        }
                    } else if (!response.isSuccessful()) {
                        error = "Failed to send message: HTTP " + response.code();
                        return null;
                    }
                    return null;
                } catch (Exception e) {
                    error = "Error sending message: " + e.getMessage();
                    Log.e(TAG, "Send message error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(null);
                }
            }
        }.execute();
    }

    public static void fetchProfile(Context context, Callback<ProfileData> callback) {
        new AsyncTask<Void, Void, ProfileData>() {
            private String error;

            @Override
            protected ProfileData doInBackground(Void... voids) {
                try {
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String userId = prefs.getString("user_id", null);
                    String accessToken = prefs.getString("access_token", null);
                    if (userId == null || accessToken == null) {
                        error = "User not authenticated";
                        return null;
                    }

                    OkHttpClient client = new OkHttpClient();
                    String encodedUserId = URLEncoder.encode(userId, "UTF-8");
                    Request request = new Request.Builder()
                            .url(REST_URL + "?id=eq." + encodedUserId)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .get()
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    Log.d(TAG, "FetchProfile Response: Code " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        error = "Failed to load profile: HTTP " + response.code();
                        return null;
                    }
                    JSONArray profiles = new JSONArray(responseBody);
                    if (profiles.length() == 0) {
                        error = "Profile not found";
                        return null;
                    }
                    JSONObject profile = profiles.getJSONObject(0);
                    return new ProfileData(
                            profile.getString("username"),
                            profile.optString("bio", ""),
                            profile.optString("image", null)
                    );
                } catch (Exception e) {
                    error = "Error loading profile: " + e.getMessage();
                    Log.e(TAG, "Fetch profile error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(ProfileData profileData) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(profileData);
                }
            }
        }.execute();
    }

    public static void updateProfile(Context context, String tag, String bio, String imagePath, Callback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            private String error;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String userId = prefs.getString("user_id", null);
                    String accessToken = prefs.getString("access_token", null);
                    if (userId == null || accessToken == null) {
                        error = "User not authenticated";
                        return null;
                    }

                    OkHttpClient client = new OkHttpClient();
                    String encodedUserId = URLEncoder.encode(userId, "UTF-8");

                    if (imagePath != null) {
                        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
                        java.io.File file = new java.io.File(imagePath);
                        RequestBody imageBody = new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", fileName, RequestBody.create(file, MediaType.parse("image/jpeg")))
                                .build();
                        Request uploadRequest = new Request.Builder()
                                .url(STORAGE_URL + fileName)
                                .header("Authorization", "Bearer " + accessToken)
                                .header("apikey", ANON_KEY)
                                .post(imageBody)
                                .build();
                        Response uploadResponse = client.newCall(uploadRequest).execute();
                        String uploadResponseBody = uploadResponse.body() != null ? uploadResponse.body().string() : "";
                        Log.d(TAG, "UploadImage Response: Code " + uploadResponse.code() + ", Body: " + uploadResponseBody);
                        if (!uploadResponse.isSuccessful()) {
                            error = "Failed to upload image: HTTP " + uploadResponse.code();
                            return null;
                        }
                    }

                    JSONObject updateData = new JSONObject();
                    if (tag != null) updateData.put("username", tag.replace("@", ""));
                    if (bio != null) updateData.put("bio", bio);
                    if (imagePath != null) updateData.put("image", STORAGE_URL + userId + "_" + System.currentTimeMillis() + ".jpg");

                    if (updateData.length() > 0) {
                        RequestBody updateBody = RequestBody.create(updateData.toString(), MediaType.parse("application/json"));
                        Request updateRequest = new Request.Builder()
                                .url(REST_URL + "?id=eq." + encodedUserId)
                                .header("Authorization", "Bearer " + accessToken)
                                .header("apikey", ANON_KEY)
                                .header("Content-Type", "application/json")
                                .header("Prefer", "return=minimal")
                                .patch(updateBody)
                                .build();
                        Response updateResponse = client.newCall(updateRequest).execute();
                        String updateResponseBody = updateResponse.body() != null ? updateResponse.body().string() : "";
                        Log.d(TAG, "UpdateProfile Response: Code " + updateResponse.code() + ", Body: " + updateResponseBody);
                        if (!updateResponse.isSuccessful()) {
                            error = "Failed to update profile: HTTP " + updateResponse.code();
                            return null;
                        }
                    }
                    return null;
                } catch (Exception e) {
                    error = "Error updating profile: " + e.getMessage();
                    Log.e(TAG, "Update profile error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(null);
                }
            }
        }.execute();
    }

    public static void deleteAccount(Context context, Callback<Void> callback) {
        new AsyncTask<Void, Void, Void>() {
            private String error;

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    String userId = prefs.getString("user_id", null);
                    String accessToken = prefs.getString("access_token", null);
                    if (userId == null || accessToken == null) {
                        error = "User not authenticated";
                        return null;
                    }

                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(ADMIN_USERS_URL + "/" + userId)
                            .header("Authorization", TOKEN)
                            .header("apikey", TOKEN.replace("Bearer ", ""))
                            .header("Content-Type", "application/json")
                            .delete()
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "DeleteAccount Response: Code " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        error = "Failed to delete account: HTTP " + response.code();
                        return null;
                    }
                    logout(context);
                    return null;
                } catch (Exception e) {
                    error = "Error deleting account: " + e.getMessage();
                    Log.e(TAG, "Delete account error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(null);
                }
            }
        }.execute();
    }

    public interface RegisterCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    public static void registerUser(Context context, String email, String username, String password, String bio, RegisterCallback callback) {
        new AsyncTask<Void, Void, String>() {
            private String error;

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject signupData = new JSONObject();
                    signupData.put("email", email);
                    signupData.put("password", password);
                    RequestBody signupBody = RequestBody.create(signupData.toString(), MediaType.parse("application/json"));
                    Request signupRequest = new Request.Builder()
                            .url(AUTH_URL)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .post(signupBody)
                            .build();
                    Response signupResponse = client.newCall(signupRequest).execute();
                    String signupResponseBody = signupResponse.body() != null ? signupResponse.body().string() : "";
                    Log.d(TAG, "Signup Response: Code " + signupResponse.code() + ", Body: " + signupResponseBody);
                    if (!signupResponse.isSuccessful()) {
                        JSONObject errorJson = new JSONObject(signupResponseBody);
                        String errorCode = errorJson.optString("error_code", "");
                        error = errorCode.equals("user_already_exists") ? "user_already_exists" : "Failed to register user: HTTP " + signupResponse.code();
                        return null;
                    }
                    JSONObject signupJson = new JSONObject(signupResponseBody);
                    String userId = signupJson.getJSONObject("user").getString("id");
                    String accessToken = signupJson.getString("access_token");
                    String refreshToken = signupJson.getString("refresh_token");

                    JSONObject profileData = new JSONObject();
                    profileData.put("id", userId);
                    profileData.put("email", email); // Добавляем email
                    profileData.put("username", username);
                    if (bio != null) profileData.put("bio", bio);
                    Log.d(TAG, "Creating profile with data: " + profileData.toString()); // Логируем данные профиля
                    RequestBody profileBody = RequestBody.create(profileData.toString(), MediaType.parse("application/json"));
                    Request profileRequest = new Request.Builder()
                            .url(REST_URL)
                            .header("Authorization", "Bearer " + accessToken)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=minimal")
                            .post(profileBody)
                            .build();
                    Response profileResponse = client.newCall(profileRequest).execute();
                    String profileResponseBody = profileResponse.body() != null ? profileResponse.body().string() : "";
                    Log.d(TAG, "CreateProfile Response: Code " + profileResponse.code() + ", Body: " + profileResponseBody);
                    if (!profileResponse.isSuccessful()) {
                        error = "Failed to create profile: HTTP " + profileResponse.code() + ", " + profileResponseBody;
                        return null;
                    }

                    SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                    prefs.edit()
                            .putString("access_token", accessToken)
                            .putString("refresh_token", refreshToken)
                            .putString("user_id", userId)
                            .putString("username", username)
                            .putString("email", email) // Сохраняем email
                            .apply();
                    return userId;
                } catch (Exception e) {
                    error = "Error registering: " + e.getMessage();
                    Log.e(TAG, "Register error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String userId) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(userId);
                }
            }
        }.execute();
    }

    public static void checkUserExists(String email, String username, Callback<Boolean> callback) {
        new AsyncTask<Void, Void, Boolean>() {
            private String error;

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    String encodedEmail = URLEncoder.encode(email, "UTF-8");
                    String encodedUsername = URLEncoder.encode(username, "UTF-8");
                    String query = CHECK_USER_URL + "&or=(email.eq." + encodedEmail + ",username.eq." + encodedUsername + ")";
                    Log.d(TAG, "CheckUser Query: " + query); // Логируем запрос
                    Request request = new Request.Builder()
                            .url(query)
                            .header("Authorization", ANON_KEY)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .get()
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "CheckUser Response: Code " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        error = "Failed to check user: HTTP " + response.code();
                        return null;
                    }
                    JSONArray users = new JSONArray(responseBody);
                    return users.length() == 0; // Возвращаем true, если пользователь не существует
                } catch (Exception e) {
                    error = "Error checking user: " + e.getMessage();
                    Log.e(TAG, "Check user error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean isAvailable) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(isAvailable);
                }
            }
        }.execute();
    }

    public interface LoginCallback {
        void onSuccess(String userId, String email);
        void onError(String error);
    }

    public static void login(Context context, String email, String password, LoginCallback callback) {
        new AsyncTask<Void, Void, JSONObject>() {
            private String error;

            @Override
            protected JSONObject doInBackground(Void... voids) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject loginData = new JSONObject();
                    loginData.put("email", email);
                    loginData.put("password", password);
                    RequestBody loginBody = RequestBody.create(loginData.toString(), MediaType.parse("application/json"));
                    Request request = new Request.Builder()
                            .url(LOGIN_URL)
                            .header("Authorization", ANON_KEY)
                            .header("apikey", ANON_KEY)
                            .header("Content-Type", "application/json")
                            .post(loginBody)
                            .build();
                    Response response = client.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Login Response: Code " + response.code() + ", Body: " + responseBody);
                    if (!response.isSuccessful()) {
                        error = "Failed to log in: HTTP " + response.code();
                        return null;
                    }
                    return new JSONObject(responseBody);
                } catch (Exception e) {
                    error = "Error logging in: " + e.getMessage();
                    Log.e(TAG, "Login error", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    try {
                        String userId = result.getJSONObject("user").getString("id");
                        String accessToken = result.getString("access_token");
                        String refreshToken = result.getString("refresh_token");
                        String userEmail = result.getJSONObject("user").getString("email");
                        SharedPreferences prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("access_token", accessToken)
                                .putString("refresh_token", refreshToken)
                                .putString("user_id", userId)
                                .apply();
                        callback.onSuccess(userId, userEmail);
                    } catch (Exception e) {
                        callback.onError("Error processing JSON: " + e.getMessage());
                        Log.e(TAG, "JSON processing error", e);
                    }
                }
            }
        }.execute();
    }
}