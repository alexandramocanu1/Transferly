package com.example.transferly.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.transferly.R;
import com.example.transferly.activities.FolderImageActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class FolderImageAdapter extends RecyclerView.Adapter<FolderImageAdapter.ImageViewHolder> {

    private static final String TAG = "FolderImageAdapter";
    private final Context context;
    private final List<Uri> imageUris;
    private final OnImageActionListener listener;
    private final Map<String, Set<String>> likesMap;
    private ActivityResultLauncher<Intent> fullScreenLauncher;
    private static final int IMAGE_ACTIVITY_REQUEST_CODE = 123;

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

        // ✅ Load the image using Glide cu cache control îmbunătățit
        Glide.with(context)
                .load(imageUri)
                .signature(new ObjectKey(imageUri.toString() + "_" + imageUri.getLastPathSegment()))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Permite cache pentru performanță
                .into(holder.imageView);

        // ✅ Gestionare like-uri îmbunătățită
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

    // ✅ Metodă optimizată pentru actualizarea unei singure imagini
    public void updateImage(int position, Uri newUri) {
        if (position >= 0 && position < imageUris.size()) {
            Log.d(TAG, "Updating image at position " + position + " with new URI: " + newUri);
            imageUris.set(position, newUri);
            notifyItemChanged(position);
        } else {
            Log.e(TAG, "Failed to update image: position " + position + " out of bounds (size: " + imageUris.size() + ")");
        }
    }

    /**
     * ✅ Actualizează lista de imagini fără să recreeze adapter-ul complet
     * Folosește DiffUtil pentru update-uri eficiente și animații smooth
     */
    public void updateImages(List<Uri> newImages) {
        Log.d(TAG, "Updating images list. Old size: " + imageUris.size() + ", New size: " + newImages.size());

        // ✅ Folosește DiffUtil pentru update-uri eficiente
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return imageUris.size();
            }

            @Override
            public int getNewListSize() {
                return newImages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition >= imageUris.size() || newItemPosition >= newImages.size()) {
                    return false;
                }
                return imageUris.get(oldItemPosition).equals(newImages.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition >= imageUris.size() || newItemPosition >= newImages.size()) {
                    return false;
                }
                // Pentru imagini, dacă URI-ul e același, conținutul e același
                return imageUris.get(oldItemPosition).equals(newImages.get(newItemPosition));
            }
        });

        // Actualizează lista
        imageUris.clear();
        imageUris.addAll(newImages);

        // Aplică schimbările cu animații
        diffResult.dispatchUpdatesTo(this);

        Log.d(TAG, "Images updated successfully. Final size: " + imageUris.size());
    }

    /**
     * ✅ Verifică dacă adapter-ul are aceleași imagini ca lista dată
     * Util pentru a evita update-uri inutile
     */
    public boolean hasSameImages(List<Uri> otherImages) {
        if (imageUris.size() != otherImages.size()) {
            return false;
        }

        for (int i = 0; i < imageUris.size(); i++) {
            if (!imageUris.get(i).equals(otherImages.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * ✅ Returnează lista curentă de imagini (doar pentru citire)
     */
    public List<Uri> getCurrentImages() {
        return new ArrayList<>(imageUris);
    }

    /**
     * ✅ Verifică dacă adapter-ul e gol
     */
    public boolean isEmpty() {
        return imageUris.isEmpty();
    }

    /**
     * ✅ Adaugă o imagine nouă la sfârșitul listei
     */
    public void addImage(Uri imageUri) {
        imageUris.add(imageUri);
        notifyItemInserted(imageUris.size() - 1);
        Log.d(TAG, "Added new image at position " + (imageUris.size() - 1));
    }

    /**
     * ✅ Adaugă multiple imagini la sfârșitul listei
     */
    public void addImages(List<Uri> newImages) {
        int startPosition = imageUris.size();
        imageUris.addAll(newImages);
        notifyItemRangeInserted(startPosition, newImages.size());
        Log.d(TAG, "Added " + newImages.size() + " images starting at position " + startPosition);
    }

    /**
     * ✅ Șterge o imagine de la poziția dată
     */
    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
            Log.d(TAG, "Removed image at position " + position);
        }
    }

    /**
     * ✅ Curăță toate imaginile
     */
    public void clearImages() {
        int size = imageUris.size();
        imageUris.clear();
        notifyItemRangeRemoved(0, size);
        Log.d(TAG, "Cleared all " + size + " images");
    }

    /**
     * ✅ Force refresh pentru o anumită poziție (useful pentru like updates)
     */
    public void refreshItem(int position) {
        if (position >= 0 && position < imageUris.size()) {
            notifyItemChanged(position);
        }
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
}