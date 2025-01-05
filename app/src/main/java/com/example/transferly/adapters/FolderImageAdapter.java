package com.example.transferly.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.transferly.R;

import java.util.List;

public class FolderImageAdapter extends RecyclerView.Adapter<FolderImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUris; // Lista cu URI-urile imaginilor
    private final OnImageLikeClickListener onImageLikeClickListener;

    public interface OnImageLikeClickListener {
        void onLikeClick(int position);
    }

    public FolderImageAdapter(Context context, List<String> imageUris, OnImageLikeClickListener likeListener) {
        this.context = context;
        this.imageUris = imageUris;
        this.onImageLikeClickListener = likeListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_folder_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUri = imageUris.get(position);

        // Fol Glide pentru a incarca imaginea
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // Set comportamentul pentru butonul de like
        holder.likeButton.setOnClickListener(v -> {
            onImageLikeClickListener.onLikeClick(position);
            holder.likeButton.setImageResource(R.drawable.ic_liked); // Schimb iconița
        });

        // Ex: ad o eticheta „Liked” daca imaginea este deja apreciata
        holder.likedLabel.setVisibility(View.GONE); // Default ascuns
        if (isLiked(position)) {
            holder.likedLabel.setVisibility(View.VISIBLE);
        }
    }

    private boolean isLiked(int position) {
        // de implementat mutatul in sectiunea liked
        return false;
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView likeButton;
        TextView likedLabel;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            likeButton = itemView.findViewById(R.id.likeButton);
            likedLabel = itemView.findViewById(R.id.likedLabel);
        }
    }
}
