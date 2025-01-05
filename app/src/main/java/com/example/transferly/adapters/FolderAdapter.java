package com.example.transferly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    private final List<String> folderList;
    private final OnFolderClickListener listener;

    public FolderAdapter(List<String> folderList, OnFolderClickListener listener) {
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        String folderName = folderList.get(position);
        holder.folderNameTextView.setText(folderName);
        holder.folderIconImageView.setImageResource(R.drawable.ic_folder);

        holder.itemView.setOnClickListener(v -> listener.onFolderClick(folderName));
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        TextView folderNameTextView;
        ImageView folderIconImageView;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderNameTextView);
            folderIconImageView = itemView.findViewById(R.id.folderIconImageView);
        }
    }

    public interface OnFolderClickListener {
        void onFolderClick(String folderName);
    }
}
