package com.example.transferly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;

import java.util.List;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.ViewHolder> {
    private final List<String> friends;
    private final List<String> selected;

    public FriendSelectAdapter(List<String> friends, List<String> selected) {
        this.friends = friends;
        this.selected = selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_select_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String username = friends.get(position);
        holder.name.setText(username);
        holder.checkBox.setChecked(selected.contains(username));

        holder.itemView.setOnClickListener(v -> {
            if (selected.contains(username)) {
                selected.remove(username);
                holder.checkBox.setChecked(false);
            } else {
                selected.add(username);
                holder.checkBox.setChecked(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.friendName);
            checkBox = itemView.findViewById(R.id.friendCheckbox);
        }
    }
}
