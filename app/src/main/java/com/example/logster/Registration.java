package com.example.logster;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Registration {
    private static final String TAG = "Registration";
    private final Activity activity;
    private final BottomSheets bottomSheets;
    private View regView;

    public Registration(Activity activity) {
        this.activity = activity;
        this.bottomSheets = new BottomSheets(activity);
    }

    public void show() {
        regView = LayoutInflater.from(activity).inflate(R.layout.registration, null);
        if (regView == null) {
            Log.e(TAG, "Не удалось загрузить макет регистрации");
            Toast.makeText(activity, "Ошибка: макет не загружен", Toast.LENGTH_SHORT).show();
            return;
        }

        bottomSheets.setContentView(regView);

        View closeButton = regView.findViewById(R.id.close);
        View registerButton = regView.findViewById(R.id.continue_reg);
        EditText emailField = regView.findViewById(R.id.email);
        EditText usernameField = regView.findViewById(R.id.username);
        EditText passwordField = regView.findViewById(R.id.password);

        // Проверяем, что ВСЕ элементы найдены (не null)
        if (closeButton == null || registerButton == null || emailField == null || usernameField == null || passwordField == null) {
            Log.e(TAG, "Один или несколько элементов UI не найдены: " +
                    "closeButton=" + (closeButton == null ? "null" : "found") +
                    ", registerButton=" + (registerButton == null ? "null" : "found") +
                    ", emailField=" + (emailField == null ? "null" : "found") +
                    ", usernameField=" + (usernameField == null ? "null" : "found") +
                    ", passwordField=" + (passwordField == null ? "null" : "found"));
            Toast.makeText(activity, "Ошибка: элементы интерфейса не найдены", Toast.LENGTH_SHORT).show();
            return;
        }

        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка закрытия");
            bottomSheets.hide(null);
        });

        registerButton.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка регистрации");
            String email = emailField.getText().toString().trim();
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            RegisterContext.checkUserExists(email, username, new RegisterContext.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean isAvailable) {
                    if (!isAvailable) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Email или имя пользователя уже заняты", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Проверка: пользователь уже существует (email=" + email + ", username=" + username + ")");
                        });
                        return;
                    }

                    RegisterContext.registerUser(activity, email, username, password, null, new RegisterContext.RegisterCallback() {
                        @Override
                        public void onSuccess(String userId) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                                bottomSheets.hide(() -> bottomSheets.switchSheet(R.layout.autorization, null, false, 0, 0));
                                Log.d(TAG, "Регистрация успешна: userId=" + userId);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            activity.runOnUiThread(() -> {
                                String errorMessage = error.contains("user_already_exists") ? "Пользователь уже зарегистрирован" : "Ошибка регистрации: " + error;
                                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Ошибка регистрации: " + error);
                            });
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Ошибка проверки: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Ошибка проверки пользователя: " + error);
                    });
                }
            });
        });

        bottomSheets.show();
    }
}