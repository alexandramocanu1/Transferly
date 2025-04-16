package com.example.transferly.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.transferly.R;
import com.example.transferly.activities.FullImageActivity;

import java.util.ArrayList;
import java.util.List;

public class FolderImageAdapter extends RecyclerView.Adapter<FolderImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUris;
    private final OnImageActionListener listener;

    public interface OnImageActionListener {
        void onImageLiked(int position);
        void onImageDeleted(int position);
    }

    public FolderImageAdapter(Context context, List<String> imageUris, OnImageActionListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_folder_item, parent, false);
        return new ImageViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUri = imageUris.get(position);

        holder.imageView.setOnClickListener(v -> {
            ArrayList<Uri> imageUrisParcelable = new ArrayList<>();
            for (String uriStr : imageUris) {
                imageUrisParcelable.add(Uri.parse(uriStr));
            }

            Intent intent = new Intent(context, FullImageActivity.class);
            intent.putParcelableArrayListExtra("images", imageUrisParcelable);
            intent.putExtra("position", holder.getAdapterPosition());
            context.startActivity(intent);
        });




        // Load the image using Glide
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // Reset icon visibility
        holder.likeCircle.setVisibility(View.GONE);

        // Check if image is liked
        if (holder.likeCircle.getTag() != null && holder.likeCircle.getTag().equals("liked")) {
            holder.likeCircle.setVisibility(View.VISIBLE);
        }

        // Handle touch events for showing icons and performing actions
        holder.imageView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Show icons when user starts touching
                    holder.likeCircle.setVisibility(View.VISIBLE);
                    holder.deleteCircle.setVisibility(View.VISIBLE);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float x = event.getX();
                    float y = event.getY();

                    // Highlight the like icon if hovered
                    if (isInsideIcon(holder.likeCircle, x, y)) {
                        holder.likeCircle.setColorFilter(Color.RED);
                        holder.deleteCircle.setColorFilter(null);
                    } else if (isInsideIcon(holder.deleteCircle, x, y)) {
                        holder.deleteCircle.setColorFilter(Color.RED);
                        holder.likeCircle.setColorFilter(null);
                    } else {
                        // Reset colors if no icon is hovered
                        holder.likeCircle.setColorFilter(null);
                        holder.deleteCircle.setColorFilter(null);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    float upX = event.getX();
                    float upY = event.getY();

                    // Perform actions based on which icon is hovered
                    if (isInsideIcon(holder.likeCircle, upX, upY)) {
                        // Toggle like state
                        if (holder.likeCircle.getTag() != null && holder.likeCircle.getTag().equals("liked")) {
                            holder.likeCircle.setTag(null);
                            holder.likeCircle.setVisibility(View.GONE);
                        } else {
                            holder.likeCircle.setTag("liked");
                            holder.likeCircle.setVisibility(View.VISIBLE);
                        }
                        listener.onImageLiked(position);
                    } else if (isInsideIcon(holder.deleteCircle, upX, upY)) {
                        listener.onImageDeleted(position);
                    }

                    // Hide icons
                    holder.likeCircle.setColorFilter(null);
                    holder.deleteCircle.setColorFilter(null);
                    holder.likeCircle.setVisibility(View.GONE);
                    holder.deleteCircle.setVisibility(View.GONE);
                    break;

                case MotionEvent.ACTION_CANCEL:
                    // Hide icons when touch is canceled
                    holder.likeCircle.setColorFilter(null);
                    holder.deleteCircle.setColorFilter(null);
                    holder.likeCircle.setVisibility(View.GONE);
                    holder.deleteCircle.setVisibility(View.GONE);
                    break;
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    private boolean isInsideIcon(View icon, float x, float y) {
        int[] location = new int[2];
        icon.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + icon.getWidth();
        int bottom = top + icon.getHeight();

        return x >= left && x <= right && y >= top && y <= bottom;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, likeCircle, deleteCircle;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            likeCircle = itemView.findViewById(R.id.likeCircle);
            deleteCircle = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
