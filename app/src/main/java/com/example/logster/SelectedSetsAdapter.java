package com.example.logster;

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

    class SetViewHolder extends RecyclerView.ViewHolder {
        EditText editWeight, editReps;
        ImageView removeSet;

        SetViewHolder(@NonNull View itemView) {
            super(itemView);
            editWeight = itemView.findViewById(R.id.edit_weight);
            editReps = itemView.findViewById(R.id.edit_reps);
            removeSet = itemView.findViewById(R.id.remove_set);

            editWeight.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    updateSet(getAdapterPosition());
                }
            });
            editReps.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    updateSet(getAdapterPosition());
                }
            });
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
            editWeight.setText(set.getWeight() > 0 ? String.valueOf(set.getWeight()) : "");
            editReps.setText(set.getReps() > 0 ? String.valueOf(set.getReps()) : "");
            Log.d("SelectedSetsAdapter", "Bind: id=" + set.getId() + ", вес=" + set.getWeight() + ", повт=" + set.getReps());
        }

        private void updateSet(int position) {
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
                Log.d("SelectedSetsAdapter", "Обновлён подход: id=" + set.getId() + ", вес=" + weight + ", повт=" + reps);
            } catch (NumberFormatException e) {
                Log.e("SelectedSetsAdapter", "Ошибка формата числа: " + e.getMessage());
            }
        }
    }
}