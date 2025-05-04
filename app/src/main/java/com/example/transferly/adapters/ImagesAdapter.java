package com.example.transferly.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.transferly.R;
import com.example.transferly.activities.FolderImageActivity;
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
        Log.d("ImagesAdapter", "Adapter initialized with " + images.size() + " images.");
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

        // Incarca imaginea cu Glide
        Glide.with(context)
                .load(imageUri)
                .into(holder.imageView);

        holder.deleteButton.clearColorFilter();

        // butonul de stergere
        holder.deleteButton.setImageResource(R.drawable.ic_delete);

        // ștergerea imaginii
        holder.deleteButton.setOnClickListener(v -> {
            images.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images.size());

            if (images.isEmpty() && context instanceof UploadActivity) {
                ((UploadActivity) context).onListEmptied();
            }
        });

        // Click pentru vizualizare fullscreen
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof UploadActivity) {
                Intent intent = new Intent(context, FullImageActivity.class);
                intent.putParcelableArrayListExtra("images", new ArrayList<>(images));
                intent.putExtra("position", position);
                ((UploadActivity) context).startActivityForResult(intent, 123);
            }
        });

    }



    @Override
    public int getItemCount() {
        return images.size();
    }


    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView deleteButton; // butonul de stergere

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

}
