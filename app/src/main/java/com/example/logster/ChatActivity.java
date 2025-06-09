package com.example.logster;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final long REFRESH_INTERVAL_MS = 5_000; // 5 секунд
    private BottomSheets bottomSheet;
    private BottomNavigationManager navManager;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private String profileUsername = "User";
    private String profileTag = "@user";
    private String profileBio = "";
    private String profileImageUrl = null;
    private ImageView loadingSpinner;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private Set<String> messageIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeUI();
        loadProfileData();
        loadMessages();
        scrollToBottom(); // Прокрутка при открытии активности
    }

    public void initializeUI() {
        setContentView(R.layout.chat);

        recyclerView = findViewById(R.id.recycler_view_chats);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(chatAdapter);

        loadingSpinner = findViewById(R.id.loading_gif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(loadingSpinner);
        loadingSpinner.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
        }

        setupNavigationAndSend();
        bottomSheet = new BottomSheets(this);
        hideSystemUI();
    }

    private void setupNavigationAndSend() {
        View bottomNavView = findViewById(R.id.bottom_navigation);
        navManager = new BottomNavigationManager(bottomNavView, this);
        navManager.setCurrentActivity("ChatActivity");
        ImageView sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(this::sendMessage);
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        profileUsername = prefs.getString("username", "User");
        profileTag = prefs.getString("tag", "@user");
        profileBio = prefs.getString("bio", "");
        profileImageUrl = prefs.getString("image_url", null);
        if (RegisterContext.isLoggedIn(this)) {
            fetchProfileData();
        } else {
            resetProfileData();
        }
    }

    public void fetchProfileData() {
        RegisterContext.fetchProfile(this, new RegisterContext.Callback<RegisterContext.ProfileData>() {
            @Override
            public void onSuccess(RegisterContext.ProfileData profileData) {
                runOnUiThread(() -> {
                    profileUsername = profileData.username != null ? profileData.username : "User";
                    profileTag = profileData.username != null ? "@" + profileData.username : "@user";
                    profileBio = profileData.bio != null ? profileData.bio : "";
                    profileImageUrl = profileData.imageUrl;
                    SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putString("username", profileUsername)
                            .putString("tag", profileTag)
                            .putString("bio", profileBio)
                            .putString("image_url", profileImageUrl)
                            .apply();
                    chatAdapter.updateCurrentUser();
                    loadMessages();
                    scrollToBottom(); // Прокрутка при входе в аккаунт
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Log.e(TAG, "Ошибка загрузки профиля: " + error));
            }
        });
    }

    public void resetProfileData() {
        profileUsername = "User";
        profileTag = "@user";
        profileBio = "";
        profileImageUrl = null;
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        prefs.edit()
                .remove("username")
                .remove("tag")
                .remove("bio")
                .remove("image_url")
                .remove("user_id")
                .apply();
        chatAdapter.clear(); // Теперь работает благодаря методу clear() в ChatAdapter
        messageIds.clear();
        scrollToBottom(); // Прокрутка при выходе из аккаунта
    }

    public void loadMessages() {
        loadingSpinner.setVisibility(View.VISIBLE);
        RegisterContext.fetchMessages(this, new RegisterContext.Callback<List<RegisterContext.Message>>() {
            @Override
            public void onSuccess(List<RegisterContext.Message> messages) {
                runOnUiThread(() -> {
                    updateMessagesWithoutDuplicates(messages);
                    loadingSpinner.setVisibility(View.GONE);
                    Log.d(TAG, "Загружено сообщений: " + messages.size());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Ошибка загрузки сообщений: " + error, Toast.LENGTH_SHORT).show();
                    loadingSpinner.setVisibility(View.GONE);
                    Log.e(TAG, "Ошибка загрузки сообщений: " + error);
                });
            }
        }, new RegisterContext.RealtimeCallback() {
            @Override
            public void onNewMessage(RegisterContext.Message message) {
                runOnUiThread(() -> {
                    if (!messageIds.contains(message.id)) {
                        chatAdapter.addMessage(message);
                        messageIds.add(message.id);
                        Log.d(TAG, "Новое сообщение через WebSocket: " + message.content);
                    }
                });
            }
        });
    }

    private void refreshMessages() {
        RegisterContext.fetchMessages(this, new RegisterContext.Callback<List<RegisterContext.Message>>() {
            @Override
            public void onSuccess(List<RegisterContext.Message> messages) {
                runOnUiThread(() -> {
                    updateMessagesWithoutDuplicates(messages);
                    Log.d(TAG, "Обновлено сообщений (рефреш): " + messages.size());
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Log.e(TAG, "Ошибка обновления сообщений: " + error));
            }
        }, null);
    }

    private void startRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshMessages();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        refreshHandler.post(refreshRunnable);
    }

    private void stopRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            Log.d(TAG, "Прокрутка вниз: count=" + chatAdapter.getItemCount());
            recyclerView.post(() -> {
                recyclerView.scrollToPosition(0);
                recyclerView.postDelayed(() -> {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int firstVisibleItem = layoutManager != null ? layoutManager.findFirstVisibleItemPosition() : -1;
                    Log.d(TAG, "Прокручено к позиции 0, текущий первый видимый элемент: " + firstVisibleItem);
                }, 500);
            });
        } else {
            Log.d(TAG, "Прокрутка пропущена: нет сообщений");
        }
    }

    private void updateMessagesWithoutDuplicates(List<RegisterContext.Message> newMessages) {
        boolean hasNewMessages = false;
        for (RegisterContext.Message message : newMessages) {
            if (!messageIds.contains(message.id)) {
                messageIds.add(message.id);
                hasNewMessages = true;
            }
        }
        if (hasNewMessages) {
            chatAdapter.updateMessages(newMessages);
            Log.d(TAG, "Обновлено сообщений: " + newMessages.size());
        } else {
            Log.d(TAG, "Обновление пропущено: нет новых сообщений");
        }
    }

    public void sendMessage(View view) {
        EditText messageInput = findViewById(R.id.message_input);
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!RegisterContext.isLoggedIn(this)) {
            Toast.makeText(this, "Войдите, чтобы отправлять сообщения", Toast.LENGTH_SHORT).show();
            autorizations(null);
            return;
        }

        // Получаем данные пользователя
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        String username = prefs.getString("username", "User");
        String userImage = prefs.getString("image_url", null);

        // Создаём временное сообщение
        String tempId = "temp_" + System.currentTimeMillis(); // Временный уникальный ID
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = sdf.format(new Date());
        RegisterContext.Message tempMessage = new RegisterContext.Message(
                tempId,
                userId,
                username,
                content,
                currentTime,
                userImage
        );

        // Добавляем временное сообщение в адаптер
        chatAdapter.addMessage(tempMessage);
        messageIds.add(tempId); // Добавляем временный ID в messageIds
        scrollToBottom(); // Прокручиваем вниз
        messageInput.setText(""); // Очищаем поле ввода
        hideKeyboard(); // Скрываем клавиатуру

        // Отправляем сообщение на сервер
        RegisterContext.sendMessage(this, content, new RegisterContext.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Сообщение отправлено", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Сообщение успешно отправлено на сервер: " + content);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Ошибка отправки: " + error, Toast.LENGTH_LONG).show();
                    if (error.contains("please log in again")) {
                        autorizations(null);
                    }
                    Log.e(TAG, "Ошибка отправки сообщения: " + error);
                });
            }
        });
    }

    public void autorizations(View view) {
        hideKeyboard();
        if (RegisterContext.isLoggedIn(this)) {
            fetchProfileData();
            switchSheet(R.layout.profile, true);
        } else {
            resetProfileData();
            switchSheet(R.layout.autorization, true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
            recyclerView.requestFocus();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void switchSheet(int layoutId, boolean isSuccess) {
        bottomSheet.switchSheet(layoutId, null, isSuccess, 0, 0);
    }

    public String getProfileTag() {
        return profileTag;
    }

    public String getProfileBio() {
        return profileBio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void updateProfileTag(String tag) {
        profileTag = tag;
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        prefs.edit().putString("tag", tag).apply();
        RegisterContext.updateProfile(this, tag, null, null, new RegisterContext.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ChatActivity.this, "Тег обновлен", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ChatActivity.this, "Ошибка обновления тега: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateProfileBio(String bio) {
        profileBio = bio;
        SharedPreferences prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        prefs.edit().putString("bio", bio).apply();
        RegisterContext.updateProfile(this, null, bio, null, new RegisterContext.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ChatActivity.this, "Био обновлено", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ChatActivity.this, "Ошибка обновления био: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    public void showProfileEditor(int layoutId) {
        switchSheet(layoutId, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
                String imagePath = FileUtils.getPath(this, data.getData());
                RegisterContext.updateProfile(this, null, null, imagePath, new RegisterContext.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Изображение профиля обновлено");
                        Toast.makeText(ChatActivity.this, "Изображение профиля обновлено", Toast.LENGTH_SHORT).show();
                        fetchProfileData();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Ошибка загрузки изображения: " + error);
                        Toast.makeText(ChatActivity.this, "Ошибка загрузки изображения: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки результата: " + e.getMessage());
        }
    }

    public void about(View view) {
        Toast.makeText(this, "О приложении", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Нажата кнопка 'О приложении'");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navManager.setCurrentActivity("ChatActivity");
        loadProfileData();
        loadMessages();
        startRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRefresh();
        RegisterContext.cleanupRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRefresh();
        RegisterContext.cleanupRealtime();
        Glide.with(this).clear(loadingSpinner);
    }
}