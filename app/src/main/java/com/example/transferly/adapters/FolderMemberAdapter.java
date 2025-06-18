package com.example.transferly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class FolderMemberAdapter extends RecyclerView.Adapter<FolderMemberAdapter.ViewHolder> {
    private final List<String> members;
    private final Consumer<String> onLongClick;

    public FolderMemberAdapter(List<String> members, Consumer<String> onLongClick) {
        this.members = members;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String username = members.get(position);
        holder.textView.setText(username);
        holder.itemView.setOnLongClickListener(v -> {
            onLongClick.accept(username);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
