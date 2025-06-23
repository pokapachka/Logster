package com.example.logster;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SelectedSetsAdapter extends RecyclerView.Adapter<SelectedSetsAdapter.SetViewHolder> {
    private List<Set> sets;
    private final OnSetRemovedListener listener;
    private final MainActivity mainActivity;

    public interface OnSetRemovedListener {
        void onSetRemoved(Set set);
    }

    public SelectedSetsAdapter(List<Set> sets, OnSetRemovedListener listener, MainActivity mainActivity) {
        this.sets = sets != null ? sets : new ArrayList<>();
        this.listener = listener;
        this.mainActivity = mainActivity;
        Log.d("SelectedSetsAdapter", "Инициализирован с " + this.sets.size() + " подходами");
    }

    @NonNull
    @Override
    public SetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sets, parent, false);
        return new SetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetViewHolder holder, int position) {
        Set set = sets.get(position);
        holder.bind(set);
        Log.d("SelectedSetsAdapter", "Привязан подход: id=" + set.getId() + ", позиция=" + position);
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    public void updateSets(List<Set> newSets) {
        this.sets.clear();
        this.sets.addAll(newSets);
        notifyDataSetChanged();
        Log.d("SelectedSetsAdapter", "Обновлены подходы: новый размер=" + sets.size());
    }

    public void saveAllSets() {
        RecyclerView recyclerView = (RecyclerView) mainActivity.findViewById(R.id.sets_list);
        if (recyclerView != null) {
            for (int i = 0; i < getItemCount(); i++) {
                SetViewHolder holder = (SetViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null) {
                    holder.saveSetData();
                } else {
                    // Если ViewHolder не виден, обновляем данные напрямую из sets
                    Set set = sets.get(i);
                    mainActivity.saveSetToWorkout(set);
                    Log.d("SelectedSetsAdapter", "Сохранён подход напрямую: id=" + set.getId());
                }
            }
        }
        Log.d("SelectedSetsAdapter", "Все подходы сохранены, размер=" + sets.size());
    }

    class SetViewHolder extends RecyclerView.ViewHolder {
        EditText editWeight, editReps;
        ImageView removeSet;
        TextWatcher weightWatcher, repsWatcher;

        SetViewHolder(@NonNull View itemView) {
            super(itemView);
            editWeight = itemView.findViewById(R.id.edit_weight);
            editReps = itemView.findViewById(R.id.edit_reps);
            removeSet = itemView.findViewById(R.id.remove_set);

            removeSet.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < sets.size()) {
                    Set set = sets.get(position);
                    listener.onSetRemoved(set);
                    sets.remove(position);
                    notifyItemRemoved(position);
                    Log.d("SelectedSetsAdapter", "Удалён подход: id=" + set.getId() + ", позиция=" + position);
                }
            });
        }

        void bind(Set set) {
            // Очищаем предыдущие TextWatcher'ы
            if (weightWatcher != null) {
                editWeight.removeTextChangedListener(weightWatcher);
            }
            if (repsWatcher != null) {
                editReps.removeTextChangedListener(repsWatcher);
            }

            // Устанавливаем значения только если они не нулевые
            editWeight.setText(set.getWeight() != 0.0f ? String.valueOf(set.getWeight()) : "");
            editReps.setText(set.getReps() != 0 ? String.valueOf(set.getReps()) : "");

            // Создаём новые TextWatcher'ы
            weightWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    saveSetData();
                }
            };
            repsWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    saveSetData();
                }
            };

            // Добавляем новые TextWatcher'ы
            editWeight.addTextChangedListener(weightWatcher);
            editReps.addTextChangedListener(repsWatcher);

            Log.d("SelectedSetsAdapter", "Bind: id=" + set.getId() + ", вес=" + set.getWeight() + ", повт=" + set.getReps());
        }

        void saveSetData() {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION || position >= sets.size()) {
                Log.w("SelectedSetsAdapter", "Невалидная позиция для обновления: " + position);
                return;
            }
            Set set = sets.get(position);
            try {
                String weightText = editWeight.getText().toString();
                String repsText = editReps.getText().toString();
                float weight = weightText.isEmpty() ? 0.0f : Float.parseFloat(weightText);
                int reps = repsText.isEmpty() ? 0 : Integer.parseInt(repsText);
                set.setWeight(weight);
                set.setReps(reps);
                mainActivity.saveSetToWorkout(set);
                Log.d("SelectedSetsAdapter", "Сохранён подход: id=" + set.getId() + ", вес=" + weight + ", повт=" + reps);
            } catch (NumberFormatException e) {
                Log.e("SelectedSetsAdapter", "Ошибка формата числа: " + e.getMessage());
            }
        }
    }
}