package com.example.logster;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    private List<Folder> folders;
    private MainActivity activity;

    public FolderAdapter(List<Folder> folders, MainActivity activity) {
        this.folders = new ArrayList<>(folders);
        this.activity = activity;
        Log.d("FolderAdapter", "Initialized with folders: " + folders.size());
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);
        holder.folderName.setText(folder.name);
        int itemCount = folder.itemIds != null ? folder.itemIds.size() : 0;
        String itemsText = getItemsText(itemCount);
        holder.items.setText(itemsText);
        holder.itemView.setOnClickListener(v -> activity.openFolder(folder));
    }

    private String getItemsText(int count) {
        if (count == 0) {
            return "0 элементов";
        }
        if (count == 1) {
            return "1 элемент";
        }
        return count + " элементов";
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public void updateFolders(List<Folder> newFolders) {
        this.folders.clear();
        this.folders.addAll(newFolders);
        notifyDataSetChanged();
        Log.d("FolderAdapter", "Обновлено папок: " + folders.size());
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderName;
        TextView items;

        FolderViewHolder(View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folder_name);
            items = itemView.findViewById(R.id.items);
        }
    }
}