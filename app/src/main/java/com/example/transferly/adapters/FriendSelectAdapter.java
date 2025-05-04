package com.example.transferly.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FriendSelectAdapter extends RecyclerView.Adapter<FriendSelectAdapter.ViewHolder> {

    private final List<String> friendList;
    private final Set<String> selectedFriends;
    private final Consumer<String> toggleSelectionCallback;

    public FriendSelectAdapter(List<String> friendList, Consumer<String> toggleSelectionCallback) {
        this.friendList = friendList;
        this.toggleSelectionCallback = toggleSelectionCallback;
        this.selectedFriends = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_select_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String username = friendList.get(position);
        holder.name.setText(username);
        holder.checkBox.setChecked(selectedFriends.contains(username));

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleSelectionCallback.accept(username);
            if (isChecked) {
                selectedFriends.add(username);
            } else {
                selectedFriends.remove(username);
            }
        });

        // Click pe întreaga linie
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.checkBox.isChecked();
            holder.checkBox.setChecked(newState);
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
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
