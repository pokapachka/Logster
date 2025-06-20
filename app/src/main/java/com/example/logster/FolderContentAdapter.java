package com.example.logster;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FolderContentAdapter extends RecyclerView.Adapter<FolderContentAdapter.ViewHolder> {
    private final List<Object> items;

    public FolderContentAdapter(List<Object> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);
        String name;
        if (item instanceof Workout) {
            name = ((Workout) item).name;
        } else if (item instanceof BodyMetric) {
            name = ((BodyMetric) item).type + ": " + ((BodyMetric) item).value;
        } else {
            name = "Неизвестный элемент";
        }
        holder.textView.setText(name);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}