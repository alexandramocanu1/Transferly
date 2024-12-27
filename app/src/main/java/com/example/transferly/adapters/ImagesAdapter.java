package com.example.transferly.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.transferly.R;
import com.example.transferly.activities.FullImageActivity;
import com.example.transferly.activities.UploadActivity;

import java.util.ArrayList;
import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {

    private final Context context;
    private final List<Uri> images;
    private final OnImageLongClickListener onImageLongClickListener;

    public interface OnImageLongClickListener {
        void onImageLongClick(int position);
    }

    public ImagesAdapter(Context context, List<Uri> images, OnImageLongClickListener listener) {
        this.context = context;
        this.images = images;
        this.onImageLongClickListener = listener;
        Log.d("ImagesAdapter", "Notified data set changed.");

    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = images.get(position);

        // Use Glide to load the image into the ImageView
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        // Set up delete button functionality
        holder.itemView.findViewById(R.id.deleteButton).setOnClickListener(v -> {
            images.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images.size());
            if (images.isEmpty() && context instanceof UploadActivity) {
                ((UploadActivity) context).onListEmptied(); // Notify the activity if the list becomes empty
            }
        });

        // Set up click listener for viewing the image
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullImageActivity.class);
            intent.putParcelableArrayListExtra("images", new ArrayList<>(images));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });

        // Setează listener pentru clic
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullImageActivity.class);
            intent.putExtra("imageUri", imageUri.toString()); // Transmite URI-ul imaginii
            context.startActivity(intent);
        });
    }



    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
