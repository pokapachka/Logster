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
import java.util.function.Consumer;

public class SelectedSetsAdapter extends RecyclerView.Adapter<SelectedSetsAdapter.ViewHolder> {
    private List<Set> sets;
    private Consumer<Set> onSetRemoved;
    private MainActivity mainActivity;

    public SelectedSetsAdapter(List<Set> sets, Consumer<Set> onSetRemoved, MainActivity mainActivity) {
        this.sets = sets != null ? new ArrayList<>(sets) : new ArrayList<>();
        this.onSetRemoved = onSetRemoved;
        this.mainActivity = mainActivity;
        Log.d("SelectedSetsAdapter", "Initialized with sets: " + this.sets.size());
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
        boolean isPlaceholder = set.getId().startsWith("placeholder_");

        holder.editWeight.setEnabled(!isPlaceholder);
        holder.editReps.setEnabled(!isPlaceholder);

        if (!isPlaceholder) {
            holder.editWeight.setText(set.getWeight() > 0 ? String.valueOf(set.getWeight()) : "");
            holder.editReps.setText(set.getReps() > 0 ? String.valueOf(set.getReps()) : "");
        } else {
            holder.editWeight.setText("");
            holder.editReps.setText("");
        }

        holder.editWeight.removeTextChangedListener(holder.weightWatcher);
        holder.editReps.removeTextChangedListener(holder.repsWatcher);

        holder.weightWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float weight = s.toString().isEmpty() ? 0.0f : Float.parseFloat(s.toString());
                    set.setWeight(weight);
                    saveSet(set);
                } catch (NumberFormatException e) {
                    set.setWeight(0.0f);
                    saveSet(set);
                }
            }
        };

        holder.repsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int reps = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                    set.setReps(reps);
                    saveSet(set);
                } catch (NumberFormatException e) {
                    set.setReps(0);
                    saveSet(set);
                }
            }
        };

        holder.editWeight.addTextChangedListener(holder.weightWatcher);
        holder.editReps.addTextChangedListener(holder.repsWatcher);

        holder.removeButton.setOnClickListener(v -> {
            if (onSetRemoved != null && !isPlaceholder) {
                onSetRemoved.accept(set);
                mainActivity.removeSet(set);
            }
        });

        holder.addButton.setVisibility(position == sets.size() - 1 && !isPlaceholder ? View.VISIBLE : View.GONE);

        Log.d("SelectedSetsAdapter", "Bound set: " + set.getId() + ", weight: " + set.getWeight() + ", reps: " + set.getReps());
    }

    private void saveSet(Set set) {
        if (!set.getId().startsWith("placeholder_")) {
            mainActivity.saveSetToWorkout(set);
            Log.d("SelectedSetsAdapter", "Saved set: " + set.getId());
        }
    }
    @Override
    public int getItemCount() {
        return sets.size();
    }

    public void updateSets(List<Set> newSets) {
        this.sets.clear();
        this.sets.addAll(newSets);
        notifyDataSetChanged();
        Log.d("SelectedSetsAdapter", "Updated sets: " + sets.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        EditText editWeight;
        EditText editReps;
        ImageView removeButton;
        ImageView addButton;
        TextWatcher weightWatcher;
        TextWatcher repsWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            editWeight = itemView.findViewById(R.id.edit_weight);
            editReps = itemView.findViewById(R.id.edit_reps);
            removeButton = itemView.findViewById(R.id.checkbox);
            addButton = itemView.findViewById(R.id.add_button);
            Log.d("SelectedSetsAdapter", "ViewHolder initialized: editWeight=" + (editWeight != null) +
                    ", editReps=" + (editReps != null) + ", removeButton=" + (removeButton != null) +
                    ", addButton=" + (addButton != null));
        }
    }
}