package com.example.transferly.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.transferly.R;
import com.example.transferly.activities.FolderImageActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FolderImageAdapter extends RecyclerView.Adapter<FolderImageAdapter.ImageViewHolder> {

    private static final String TAG = "FolderImageAdapter";
    private final Context context;
    private final List<Uri> imageUris;
    private final OnImageActionListener listener;
    private final Map<String, Set<String>> likesMap;
    private ActivityResultLauncher<Intent> fullScreenLauncher;
    private static final int IMAGE_ACTIVITY_REQUEST_CODE = 123;


//    Map<String, Set<String>> likesMap = new HashMap<>();

    public interface OnImageActionListener {
        void onImageLiked(int position);
        void onImageDeleted(int position);
    }

    public FolderImageAdapter(Context context, List<Uri> imageUris,
                              OnImageActionListener listener,
                              ActivityResultLauncher<Intent> fullScreenLauncher,
                              Map<String, Set<String>> likesMap) {
        this.context = context;
        this.imageUris = imageUris;
        this.listener = listener;
        this.fullScreenLauncher = fullScreenLauncher;
        this.likesMap = likesMap;
    }


    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_folder_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        Log.d(TAG, "Loading image at position " + position + ": " + imageUri);

        holder.imageView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(context, FolderImageActivity.class);
                ArrayList<Uri> uriList = new ArrayList<>(imageUris);
                intent.putParcelableArrayListExtra("images", uriList);
                intent.putExtra("position", adapterPosition);

                Log.d(TAG, "Opening image at position " + adapterPosition);

                if (context instanceof Activity && fullScreenLauncher != null) {
                    Log.d(TAG, "Launching FolderImageActivity via launcher from: " + context.getClass().getSimpleName());
                    fullScreenLauncher.launch(intent);
                } else if (context instanceof Activity) {
                    Log.w(TAG, "Launcher is null, using fallback startActivityForResult");
                    ((Activity) context).startActivityForResult(intent, IMAGE_ACTIVITY_REQUEST_CODE);
                } else {
                    Log.e(TAG, "Context is not an Activity! Cannot launch FolderImageActivity.");
                }

            }
        });

        // Load the image using Glide with cache control
        Glide.with(context)
                .load(imageUri)
                .signature(new ObjectKey(imageUri.toString() + System.currentTimeMillis()))  // Force refresh
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Skip cache
                .skipMemoryCache(true)  // Skip memory cache
                .into(holder.imageView);


        int likeCount = likesMap.getOrDefault(imageUri.toString(), new HashSet<>()).size();
        String currentUser = "guest"; // Sau ia din SharedPreferences dacă ai acces aici
        boolean userAlreadyLiked = likesMap.getOrDefault(imageUri.toString(), new HashSet<>()).contains(currentUser);

// Arată inimioara doar dacă are like de la cineva
        if (likeCount > 0) {
            holder.likeCircle.setVisibility(View.VISIBLE);
            holder.likeCircle.setImageResource(userAlreadyLiked ? R.drawable.ic_like : R.drawable.ic_liked);
            holder.likeCircle.setColorFilter(null); // remove any tint
        } else {
            holder.likeCircle.setVisibility(View.GONE);
        }

// Dacă are cel puțin 2 like-uri, arată și numărul
        if (likeCount >= 2) {
            holder.likeCountText.setVisibility(View.VISIBLE);
            holder.likeCountText.setText(String.valueOf(likeCount));
            holder.likeCountText.setTextColor(Color.BLACK); // se vede pe inimioară roșie
        } else {
            holder.likeCountText.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, likeCircle, deleteCircle;
        TextView likeCountText;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            likeCircle = itemView.findViewById(R.id.likeCircle);
            deleteCircle = itemView.findViewById(R.id.deleteIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
        }
    }

    public void updateImage(int position, Uri newUri) {
        if (position >= 0 && position < imageUris.size()) {
            Log.d(TAG, "Updating image at position " + position + " with new URI: " + newUri);
            imageUris.set(position, newUri);

            // Force refresh by notifying adapter
            notifyItemChanged(position);

            // Clear Glide cache for this URI
//            Glide.with(context).clear((View) null);
        } else {
            Log.e(TAG, "Failed to update image: position " + position + " out of bounds (size: " + imageUris.size() + ")");
        }
    }
}