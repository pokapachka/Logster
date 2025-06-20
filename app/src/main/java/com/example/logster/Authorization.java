package com.example.logster;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Authorization {
    private static final String TAG = "Authorization";
    private final Activity activity;
    private final BottomSheets bottomSheet;
    private View authView;

    public Authorization(Activity activity) {
        this.activity = activity;
        this.bottomSheet = new BottomSheets(activity);
    }

    public void show() {
        if (RegisterContext.isLoggedIn(activity)) {
            bottomSheet.switchSheet(R.layout.profile, null, false, 0, 0);
            return;
        }
        authView = LayoutInflater.from(activity).inflate(R.layout.autorization, null);
        if (authView == null) {
            Toast.makeText(activity, "Ошибка: макет не загружен", Toast.LENGTH_SHORT).show();
            return;
        }
        bottomSheet.setContentView(authView);
        View closeButton = authView.findViewById(R.id.close);
        View continueButton = authView.findViewById(R.id.continue_auth);
        View registrationButton = authView.findViewById(R.id.registration);
        View forgotPasswordButton = authView.findViewById(R.id.forgotPassword);
        EditText emailField = authView.findViewById(R.id.email);
        EditText passwordField = authView.findViewById(R.id.password);
        if (closeButton == null || continueButton == null || registrationButton == null ||
                forgotPasswordButton == null || emailField == null || passwordField == null) {
            Toast.makeText(activity, "Ошибка: элементы UI не найдены", Toast.LENGTH_SHORT).show();
            return;
        }
        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked");
            bottomSheet.hide(null);
        });
        continueButton.setOnClickListener(v -> {
            Log.d(TAG, "Continue button clicked");
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            RegisterContext.login(activity, email, password, new RegisterContext.LoginCallback() {
                @Override
                public void onSuccess(String userId, String email) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Авторизация успешна", Toast.LENGTH_SHORT).show();
                        bottomSheet.hide(null);
                        if (activity instanceof ChatActivity) {
                            ((ChatActivity) activity).fetchProfileData();
                            ((ChatActivity) activity).loadMessages();
                        }
                    });
                }
                @Override
                public void onError(String error) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
        registrationButton.setOnClickListener(v -> {
            bottomSheet.hide(() -> bottomSheet.switchSheet(R.layout.registration, null, false, 0, 0));
        });
        forgotPasswordButton.setOnClickListener(v -> {
            Log.d(TAG, "Переход на экран восстановления пароля");
            bottomSheet.hide(() -> {
                ForgotPassword forgotPassword = new ForgotPassword(activity);
                forgotPassword.show();
            });
        });
        bottomSheet.show();
    }
}