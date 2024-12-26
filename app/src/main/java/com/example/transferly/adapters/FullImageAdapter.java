package com.example.transferly.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.transferly.R;

import java.util.List;

public class FullImageAdapter extends RecyclerView.Adapter<FullImageAdapter.FullImageViewHolder> {

    private final List<Uri> images;

    public FullImageAdapter(List<Uri> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public FullImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_full_image, parent, false);
        return new FullImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FullImageViewHolder holder, int position) {
        Uri imageUri = images.get(position);
        Glide.with(holder.imageView.getContext())
                .load(imageUri)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class FullImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FullImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewFull);
        }
    }
}
