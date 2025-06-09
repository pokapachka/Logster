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

public class SelectedSetsAdapter extends RecyclerView.Adapter<SelectedSetsAdapter.ViewHolder> {
    private List<Set> sets;
    private OnSetRemovedListener onSetRemovedListener;
    private MainActivity mainActivity;

    public interface OnSetRemovedListener {
        void onSetRemoved(Set set);
    }

    public SelectedSetsAdapter(List<Set> sets, OnSetRemovedListener listener, MainActivity mainActivity) {
        this.sets = new ArrayList<>(sets != null ? sets : new ArrayList<>());
        this.onSetRemovedListener = listener;
        this.mainActivity = mainActivity;
        Log.d("SelectedSetsAdapter", "Инициализирован с " + this.sets.size() + " подходами");
    }

    public void updateSets(List<Set> newSets) {
        this.sets.clear();
        this.sets.addAll(newSets != null ? newSets : new ArrayList<>());
        notifyDataSetChanged();
        Log.d("SelectedSetsAdapter", "Обновлено подходов: " + sets.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sets, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Set set = sets.get(position);
        if (holder.weightEditText != null) {
            holder.weightEditText.setText(String.valueOf(set.getWeight()));
        } else {
            Log.e("SelectedSetsAdapter", "weightEditText null на позиции: " + position);
        }
        if (holder.repsEditText != null) {
            holder.repsEditText.setText(String.valueOf(set.getReps()));
        } else {
            Log.e("SelectedSetsAdapter", "repsEditText null на позиции: " + position);
        }
        if (holder.removeButton != null) {
            holder.removeButton.setOnClickListener(v -> {
                if (onSetRemovedListener != null && !set.getId().startsWith("placeholder_")) {
                    onSetRemovedListener.onSetRemoved(set);
                    sets.remove(set);
                    notifyItemRemoved(position);
                    Log.d("SelectedSetsAdapter", "Удалён подход: id=" + set.getId());
                }
            });
        } else {
            Log.e("SelectedSetsAdapter", "removeButton null на позиции: " + position);
        }
        // Сохранение изменений веса и повторений
        if (holder.weightEditText != null && holder.repsEditText != null) {
            holder.weightEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        float weight = Float.parseFloat(holder.weightEditText.getText().toString());
                        set.setWeight(weight);
                        mainActivity.saveSetToWorkout(set);
                        Log.d("SelectedSetsAdapter", "Сохранён вес: " + weight + " для подхода: " + set.getId());
                    } catch (NumberFormatException e) {
                        Log.w("SelectedSetsAdapter", "Некорректный вес для подхода: " + set.getId());
                    }
                }
            });
            holder.repsEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        int reps = Integer.parseInt(holder.repsEditText.getText().toString());
                        set.setReps(reps);
                        mainActivity.saveSetToWorkout(set);
                        Log.d("SelectedSetsAdapter", "Сохранены повторения: " + reps + " для подхода: " + set.getId());
                    } catch (NumberFormatException e) {
                        Log.w("SelectedSetsAdapter", "Некорректное количество повторений для подхода: " + set.getId());
                    }
                }
            });
        }
        Log.d("SelectedSetsAdapter", "Привязан подход: id=" + set.getId() + ", позиция=" + position);
    }

    @Override
    public int getItemCount() {
        return sets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        EditText weightEditText;
        EditText repsEditText;
        ImageView removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            weightEditText = itemView.findViewById(R.id.edit_weight);
            repsEditText = itemView.findViewById(R.id.edit_reps);
            removeButton = itemView.findViewById(R.id.remove_set);
            if (weightEditText == null || repsEditText == null || removeButton == null) {
                Log.e("SelectedSetsAdapter", "Ошибка инициализации ViewHolder: weightEditText=" +
                        (weightEditText != null) + ", repsEditText=" + (repsEditText != null) +
                        ", removeButton=" + (removeButton != null));
            }
        }
    }
}