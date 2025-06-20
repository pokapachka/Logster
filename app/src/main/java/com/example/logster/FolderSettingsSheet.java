package com.example.logster;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FolderSettingsSheet {
    private final Activity activity;
    private final BottomSheets bottomSheets;
    private final Folder folder;
    private final MainActivity mainActivity;
    private View sheetView;

    public FolderSettingsSheet(MainActivity activity, Folder folder) {
        this.activity = activity;
        this.mainActivity = activity;
        this.folder = folder;
        this.bottomSheets = new BottomSheets(activity, R.layout.settings_widget);
        initializeSheet();
    }

    private void initializeSheet() {
        sheetView = bottomSheets.getContentView();
        if (sheetView == null) {
            return;
        }

        // Настройка заголовка и описания
        TextView head = sheetView.findViewById(R.id.head);
        TextView description = sheetView.findViewById(R.id.description);
        if (head != null) {
            head.setText("Настройки папки");
        }
        if (description != null) {
            description.setText("Измените название папки или просмотрите содержимое");
        }

        // Настройка поля ввода имени папки
        EditText folderNameEditText = sheetView.findViewById(R.id.folder_name_edit_text);
        if (folderNameEditText != null) {
            folderNameEditText.setText(folder.name);
            folderNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String newName = s.toString().trim();
                    if (!newName.isEmpty() && !newName.equals(folder.name)) {
                        // Проверка на уникальность имени
                        boolean nameExists = mainActivity.folders.stream()
                                .anyMatch(f -> f.name.equals(newName) && !f.id.equals(folder.id));
                        if (!nameExists) {
                            folder.name = newName;
                            mainActivity.saveFolders();
                            mainActivity.updateMainItems();
                            mainActivity.combinedAdapter.updateData(
                                    mainActivity.folders,
                                    mainActivity.workouts,
                                    mainActivity.bodyMetrics,
                                    mainActivity.currentFolderName
                            );
                        } else {
                            folderNameEditText.setError("Папка с таким именем уже существует");
                        }
                    }
                }
            });
        }

        // Настройка списка упражнений/элементов (exercise_list)
        RecyclerView exerciseList = sheetView.findViewById(R.id.exercise_list);
        if (exerciseList != null) {
            exerciseList.setLayoutManager(new LinearLayoutManager(activity));
            List<Object> folderItems = new ArrayList<>();
            for (String itemId : folder.itemIds) {
                mainActivity.workouts.stream()
                        .filter(w -> w.id.equals(itemId))
                        .findFirst()
                        .ifPresent(folderItems::add);
                mainActivity.bodyMetrics.stream()
                        .filter(m -> m.id.equals(itemId))
                        .findFirst()
                        .ifPresent(folderItems::add);
            }
            FolderContentAdapter adapter = new FolderContentAdapter(folderItems);
            exerciseList.setAdapter(adapter);
        }

        // Настройка кнопки закрытия
        View closeButton = sheetView.findViewById(R.id.close);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> bottomSheets.hide(null));
        }
    }

    public void show() {
        bottomSheets.show();
    }

    public boolean isShowing() {
        return bottomSheets.isShowing();
    }

    public void hide() {
        bottomSheets.hide(null);
    }
}