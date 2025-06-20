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

public class ForgotPassword {
    private static final String TAG = "ForgotPassword";
    private final Activity activity;
    private final BottomSheets bottomSheet;
    private View fpView;
    private String verificationCode;

    public ForgotPassword(Activity activity) {
        this.activity = activity;
        this.bottomSheet = new BottomSheets(activity);
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
        fpView = LayoutInflater.from(activity).inflate(R.layout.forgot_password, null);
        if (fpView == null) {
            Log.e(TAG, "fpView is null after inflation");
            Toast.makeText(activity, "Ошибка: не удалось загрузить макет", Toast.LENGTH_SHORT).show();
            return;
        }
        bottomSheet.setContentView(fpView);

        View closeBtn = fpView.findViewById(R.id.close);
        View continueBtn = fpView.findViewById(R.id.continue_auth);
        TextView sendCodeBtn = fpView.findViewById(R.id.send_code);
        EditText emailField = fpView.findViewById(R.id.email);
        EditText codeField = fpView.findViewById(R.id.code);
        EditText passwordField = fpView.findViewById(R.id.password);

        if (closeBtn == null || continueBtn == null || sendCodeBtn == null ||
                emailField == null || codeField == null || passwordField == null) {
            Log.e(TAG, String.format("Один или несколько элементов UI не найдены: " +
                            "closeBtn=%s, continueBtn=%s, sendCodeBtn=%s, emailField=%s, codeField=%s, passwordField=%s",
                    closeBtn == null ? "null" : "found",
                    continueBtn == null ? "null" : "found",
                    sendCodeBtn == null ? "null" : "found",
                    emailField == null ? "null" : "found",
                    codeField == null ? "null" : "found",
                    passwordField == null ? "null" : "found"));
            Toast.makeText(activity, "Ошибка: элементы интерфейса не найдены", Toast.LENGTH_SHORT).show();
            return;
        }

        closeBtn.setOnClickListener(v -> bottomSheet.hide(null));

        sendCodeBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();

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

            RegisterContext.checkUserExists(email, "", new RegisterContext.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean isAvailable) {
                    if (isAvailable) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Пользователь с таким email не найден", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Пользователь не существует: email=" + email);
                        });
                        return;
                    }

                    verificationCode = generateVerificationCode();
                    saveVerificationCode(email, verificationCode);
                    String subject = "Восстановление пароля";
                    String text = "Ваш код для восстановления пароля: " + verificationCode;
                    new YandexMailSender(new YandexMailSender.MailCallback() {
                        @Override
                        public void onSuccess() {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Код отправлен на " + email, Toast.LENGTH_SHORT).show();
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

        continueBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String code = codeField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || code.isEmpty() || password.isEmpty()) {
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

            if (verifyCode(email, code)) {
                // Обновление пароля в Supabase
                RegisterContext.updatePassword(activity, email, password, new RegisterContext.Callback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Пароль успешно обновлен", Toast.LENGTH_SHORT).show();
                            SharedPreferences prefs = activity.getSharedPreferences("auth_prefs", Activity.MODE_PRIVATE);
                            prefs.edit().remove("verification_code_" + email).apply();
                            bottomSheet.hide(() -> bottomSheet.switchSheet(R.layout.autorization, null, false, 0, 0));
                        });
                    }

                    @Override
                    public void onError(String error) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Ошибка обновления пароля: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Ошибка обновления пароля: " + error);
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

        bottomSheet.show();
    }

    public void hide() {
        bottomSheet.hide(null);
    }
}