package com.example.logster;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class Registration {
    private static final String TAG = "Registration";
    private final Activity activity;
    private final BottomSheets bottomSheets;
    private View regView;
    private String verificationCode;

    public Registration(Activity activity) {
        this.activity = activity;
        this.bottomSheets = new BottomSheets(activity);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000)); // 6-значный код
    }

    private void saveVerificationCode(String email, String code) {
        SharedPreferences prefs = activity.getSharedPreferences("auth_prefs", Activity.MODE_PRIVATE);
        prefs.edit().putString("verification_code_" + email, code).apply();
    }

    private boolean verifyCode(String email, String inputCode) {
        SharedPreferences prefs = activity.getSharedPreferences("auth_prefs", Activity.MODE_PRIVATE);
        String storedCode = prefs.getString("verification_code_" + email, null);
        return inputCode.equals(storedCode);
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
        TextView sendCodeButton = regView.findViewById(R.id.send_code);
        EditText emailField = regView.findViewById(R.id.email);
        EditText usernameField = regView.findViewById(R.id.username);
        EditText passwordField = regView.findViewById(R.id.password);
        EditText codeField = regView.findViewById(R.id.verify_mail);

        // Проверяем, что все элементы найдены
        if (closeButton == null || registerButton == null || sendCodeButton == null ||
                emailField == null || usernameField == null || passwordField == null || codeField == null) {
            Log.e(TAG, String.format("Один или несколько элементов UI не найдены: " +
                            "closeButton=%s, registerButton=%s, sendCodeButton=%s, " +
                            "emailField=%s, usernameField=%s, passwordField=%s, codeField=%s",
                    closeButton == null ? "null" : "found",
                    registerButton == null ? "null" : "found",
                    sendCodeButton == null ? "null" : "found",
                    emailField == null ? "null" : "found",
                    usernameField == null ? "null" : "found",
                    passwordField == null ? "null" : "found",
                    codeField == null ? "null" : "found"));
            Toast.makeText(activity, "Ошибка: элементы интерфейса не найдены", Toast.LENGTH_SHORT).show();
            return;
        }

        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка закрытия");
            bottomSheets.hide(null);
        });

        sendCodeButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String username = usernameField.getText().toString().trim();

            // Валидация email
            if (email.isEmpty()) {
                emailField.setError("Введите email");
                emailField.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.setError("Введите корректный email");
                emailField.requestFocus();
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

                    // Отправка кода подтверждения
                    verificationCode = generateVerificationCode();
                    saveVerificationCode(email, verificationCode);
                    String subject = "Подтверждение регистрации";
                    String text = "Ваш код подтверждения: " + verificationCode;
                    new YandexMailSender(new YandexMailSender.MailCallback() {
                        @Override
                        public void onSuccess() {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Код подтверждения отправлен на " + email, Toast.LENGTH_SHORT).show();
                                codeField.requestFocus();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Ошибка отправки кода: " + error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Ошибка отправки email: " + error);
                            });
                        }
                    }).execute(email, subject, text);
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

        registerButton.setOnClickListener(v -> {
            Log.d(TAG, "Нажата кнопка регистрации");
            String email = emailField.getText().toString().trim();
            String username = usernameField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String inputCode = codeField.getText().toString().trim();

            // Валидация ввода
            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || inputCode.isEmpty()) {
                Toast.makeText(activity, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.setError("Введите корректный email");
                emailField.requestFocus();
                return;
            }

            if (password.length() < 6) {
                passwordField.setError("Пароль должен содержать минимум 6 символов");
                passwordField.requestFocus();
                return;
            }

            // Проверка кода подтверждения
            if (verifyCode(email, inputCode)) {
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
                                    // Очистка кода после успешной регистрации
                                    SharedPreferences prefs = activity.getSharedPreferences("auth_prefs", Activity.MODE_PRIVATE);
                                    prefs.edit().remove("verification_code_" + email).apply();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                activity.runOnUiThread(() -> {
                                    String errorMessage = error.contains("user_already_exists") ?
                                            "Пользователь уже зарегистрирован" : "Ошибка регистрации: " + error;
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
            } else {
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Неверный код подтверждения", Toast.LENGTH_SHORT).show();
                    codeField.setError("Неверный код");
                    codeField.requestFocus();
                });
            }
        });

        bottomSheets.show();
    }
}