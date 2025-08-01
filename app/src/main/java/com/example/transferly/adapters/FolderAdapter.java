package com.example.transferly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.transferly.R;

import java.util.ArrayList;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private final Context context;
    private final List<String> folders;
    private final List<String> selectedFolders;
    private final OnFolderClickListener clickListener;
    private final OnFolderLongClickListener longClickListener;
    private final OnStartDragListener dragListener;

    public interface OnFolderClickListener {
        void onFolderClick(String folderName);
    }

    public interface OnFolderLongClickListener {
        void onFolderLongClick(String folderName);
    }

    // ✅ Interfața corectată - nume consistent
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public List<String> getFolders() {
        return folders;
    }

    public FolderAdapter(Context context, List<String> folders, List<String> selectedFolders,
                         OnFolderClickListener clickListener, OnFolderLongClickListener longClickListener,
                         OnStartDragListener dragListener) {
        this.context = context;
        this.folders = folders;
        this.selectedFolders = selectedFolders != null ? selectedFolders : new ArrayList<>();
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.dragListener = dragListener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.folder_item, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        String folderName = folders.get(position);

        holder.folderIcon.setImageResource(R.drawable.ic_folder);
        holder.folderName.setText(folderName);

        // Afisez cerculetul daca folderul este selectat
        holder.selectCircle.setVisibility(selectedFolders.contains(folderName) ? View.VISIBLE : View.GONE);

        // Click pe folder
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFolderClick(folderName);
            }
        });

        // ✅ Long click pentru drag - implementare îmbunătățită
        holder.itemView.setOnLongClickListener(v -> {
            if (dragListener != null) {
                dragListener.onStartDrag(holder);
                return true;
            }
            // Fallback la long click listener dacă nu e drag
            if (longClickListener != null) {
                longClickListener.onFolderLongClick(folderName);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        ImageView folderIcon, selectCircle;
        TextView folderName;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderIcon = itemView.findViewById(R.id.folderIcon);
            selectCircle = itemView.findViewById(R.id.selectCircle);
            folderName = itemView.findViewById(R.id.folderName);
        }
    }
}