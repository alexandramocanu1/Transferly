package com.example.transferly.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
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

public class FullImageAdapter extends RecyclerView.Adapter<FullImageAdapter.FullImageViewHolder> {

    private final List<Uri> images;
    private final Context context;

    public FullImageAdapter(Context context, List<Uri> images) {
        this.context = context;
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

        // incarc imaginea în ImageView
        Glide.with(holder.imageView.getContext()).load(imageUri).into(holder.imageView);

        // Click pe imagine -> deschide FullImageActivity (dar vf sa nu fie deja deschis)
        holder.imageView.setOnClickListener(v -> {
            if (!(context instanceof FullImageActivity)) {
                Intent intent = new Intent(context, FullImageActivity.class);
                intent.putParcelableArrayListExtra("images", new ArrayList<>(images));
                intent.putExtra("position", position);
                context.startActivity(intent);
            }
        });
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

    @Override
    public long getItemId(int position) {
        return position;
    }
}
