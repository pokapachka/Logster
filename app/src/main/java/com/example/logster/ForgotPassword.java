package com.example.logster;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ForgotPassword {
    private static final String TAG = "ForgotPassword";
    private final Activity activity;
    private final BottomSheets bottomSheet;
    private View fpView;

    public ForgotPassword(Activity activity) {
        this.activity = activity;
        this.bottomSheet = new BottomSheets(activity);
    }

    public void show() {
        // Создаём View для BottomSheet
        fpView = LayoutInflater.from(activity).inflate(R.layout.forgot_password, null);
        if (fpView == null) {
            Log.e(TAG, "fpView is null after inflation");
            Toast.makeText(activity, "Ошибка: не удалось загрузить макет", Toast.LENGTH_SHORT).show();
            return;
        }
        bottomSheet.setContentView(fpView);

        // Настройка элементов
        View closeBtn = fpView.findViewById(R.id.close);
        View continueBtn = fpView.findViewById(R.id.continue_auth);
        EditText emailField = fpView.findViewById(R.id.email);
        EditText codeField = fpView.findViewById(R.id.code);
        EditText passwordField = fpView.findViewById(R.id.password);

        // Проверки на null
        if (closeBtn == null) {
            Log.e(TAG, "closeBtn is null");
            Toast.makeText(activity, "Ошибка: не найден элемент close", Toast.LENGTH_SHORT).show();
            return;
        }
        if (continueBtn == null) {
            Log.e(TAG, "continueBtn is null");
            Toast.makeText(activity, "Ошибка: не найден элемент continue_auth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (emailField == null) {
            Log.e(TAG, "emailField is null");
            Toast.makeText(activity, "Ошибка: не найден элемент email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (codeField == null) {
            Log.e(TAG, "codeField is null");
            Toast.makeText(activity, "Ошибка: не найден элемент code", Toast.LENGTH_SHORT).show();
            return;
        }
        if (passwordField == null) {
            Log.e(TAG, "passwordField is null");
            Toast.makeText(activity, "Ошибка: не найден элемент password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Обработчики
        closeBtn.setOnClickListener(v -> bottomSheet.hide(null));

        continueBtn.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String code = codeField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (email.isEmpty() || code.isEmpty() || password.isEmpty()) {
                Toast.makeText(activity, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else {
                // Заглушка для логики восстановления пароля
                Toast.makeText(activity, "Восстановление: " + email + ", код: " + code, Toast.LENGTH_SHORT).show();
                bottomSheet.hide(null);
            }
        });

        // Показываем BottomSheet
        bottomSheet.show();
    }

    public void hide() {
        bottomSheet.hide(null);
    }
}