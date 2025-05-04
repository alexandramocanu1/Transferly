package com.example.transferly.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.canhub.cropper.CropImageContract;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderImagePagerAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class FolderImageActivity extends AppCompatActivity {

    private static final String TAG = "FolderImageActivity";
    private ViewPager2 viewPager;
    private ImageView blurredBackground;
    private View backgroundOverlay;
    private GestureDetectorCompat gestureDetector;
    private List<Uri> imageUris;

    private static final int SWIPE_THRESHOLD_Y = 200;
    private static final int SWIPE_VELOCITY_THRESHOLD = 150;

    private int currentPosition = 0;

    private ImageView likeButton;
    private Map<String, Set<String>> likesMap;
    private String currentUser;


    // Launcher for crop
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                Log.d(TAG, "Crop result received: " + (result.isSuccessful() ? "SUCCESS" : "FAIL"));

                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    int position = viewPager.getCurrentItem();

                    Log.d(TAG, "Cropped image URI: " + croppedUri + ", at position: " + position);

                    if (croppedUri != null) {
                        try {
                            // 1. Open stream and decode cropped image
                            InputStream inputStream = getContentResolver().openInputStream(croppedUri);
                            Bitmap croppedBitmap = BitmapFactory.decodeStream(inputStream);

                            if (croppedBitmap == null) {
                                Log.e(TAG, "Failed to decode cropped bitmap");
                                Toast.makeText(this, "Failed to process cropped image", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Log.d(TAG, "Cropped bitmap size: " + croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());

                            // 2. Create a file in app storage
                            File outputDir = new File(getFilesDir(), "cropped_images");
                            if (!outputDir.exists()) outputDir.mkdirs();

                            String filename = "cropped_" + System.currentTimeMillis() + ".jpg";
                            File outputFile = new File(outputDir, filename);

                            FileOutputStream fos = new FileOutputStream(outputFile);
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.close();

                            Log.d(TAG, "Saved cropped image to: " + outputFile.getAbsolutePath());

                            // 3. Send the URI of the new saved file
                            Uri savedUri = Uri.fromFile(outputFile);
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("croppedUri", savedUri.toString());
                            resultIntent.putExtra("position", position);
//                            setResult(RESULT_OK, resultIntent);

                            setResult(RESULT_OK, resultIntent);
                            finish();
                            overridePendingTransition(0, R.anim.slide_out_down); // anim soft


//                            Log.d(TAG, "Returning result with URI: " + savedUri);
//                            Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show();

//                            finish();
                        } catch (IOException e) {
                            Log.e(TAG, "Error saving cropped image: " + e.getMessage(), e);
                            Toast.makeText(this, "Failed to save cropped image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Crop failed: URI is null");
                        Toast.makeText(this, "Crop failed: URI is null", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Crop failed: " + result.getError());
                    Toast.makeText(this, "Crop failed: " + result.getError(), Toast.LENGTH_SHORT).show();
                }
            });

    private void startCrop(Uri imageUri) {
        if (imageUri == null) {
            Log.e(TAG, "Cannot start crop: image URI is null");
            Toast.makeText(this, "Cannot crop: image not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting crop for image: " + imageUri);

        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.autoZoomEnabled = true;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.fixAspectRatio = false;

        try {
            CropImageContractOptions contractOptions = new CropImageContractOptions(imageUri, options);
            cropImageLauncher.launch(contractOptions);
            Log.d(TAG, "Crop launcher started");
        } catch (Exception e) {
            Log.e(TAG, "Error launching crop: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting crop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_image);

        Log.d(TAG, "onCreate called");

        viewPager = findViewById(R.id.viewPager);
        blurredBackground = findViewById(R.id.blurredBackground);
        backgroundOverlay = findViewById(R.id.backgroundOverlay);

        // Get image list from intent
        ArrayList<Uri> receivedUris = getIntent().getParcelableArrayListExtra("images");
        if (receivedUris != null) {
            imageUris = new ArrayList<>(receivedUris);
            Log.d(TAG, "Received " + imageUris.size() + " images");
        } else {
            imageUris = new ArrayList<>();
            Log.e(TAG, "No images received from intent");
        }

        currentPosition = getIntent().getIntExtra("position", 0);
        Log.d(TAG, "Starting position: " + currentPosition);

        if (imageUris != null && !imageUris.isEmpty()) {
            FolderImagePagerAdapter adapter = new FolderImagePagerAdapter(this, imageUris);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);

            if (currentPosition < imageUris.size()) {
                applyBlurEffect(imageUris.get(currentPosition));
            }
        } else {
            Log.e(TAG, "No images to display");
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                Log.d(TAG, "Page changed to position: " + position);
                if (position < imageUris.size()) {
                    applyBlurEffect(imageUris.get(position));
                }

                updateLikeButtonState(likeButton, likesMap, currentUser);
            }
        });

        backgroundOverlay.setOnTouchListener((v, event) -> false);
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        // Crop button
        ImageView cropButton = findViewById(R.id.cropButton);
        cropButton.setOnClickListener(v -> {
            int currentPos = viewPager.getCurrentItem();
            if (imageUris == null || imageUris.isEmpty() || currentPos >= imageUris.size()) {
                Toast.makeText(this, "No image to crop", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri imageToCrop = imageUris.get(currentPos);
            Log.d(TAG, "Starting crop for image: " + imageToCrop);
            Toast.makeText(this, "Starting crop...", Toast.LENGTH_SHORT).show();
            startCrop(imageToCrop);
        });

        // Like button logic
        likeButton = findViewById(R.id.likeButton);


        ImageView deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();
            if (position >= 0 && position < imageUris.size()) {
                Uri imageUri = imageUris.get(position);

                new AlertDialog.Builder(FolderImageActivity.this)
                        .setTitle("Delete Photo")
                        .setMessage("Are you sure you want to delete this photo?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("deletedUri", imageUri.toString());
                            resultIntent.putExtra("position", position);
                            setResult(RESULT_CANCELED, resultIntent);  // poți folosi și alt cod dacă vrei
                            finish();
                            overridePendingTransition(0, R.anim.slide_out_down);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });



        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUser = prefs.getString("username", "guest");


        String folderId = getIntent().getStringExtra("FOLDER_ID");
        SharedPreferences folderPrefs = getSharedPreferences("SharedFoldersData", MODE_PRIVATE);
        String likesKey = "LIKES_" + folderId;

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
        likesMap = gson.fromJson(folderPrefs.getString(likesKey, "{}"), type);

        // Inițializează starea inimioarei pentru imaginea curentă
        String initialImageKey = imageUris.get(currentPosition).toString();
        Set<String> initialLikes = likesMap.getOrDefault(initialImageKey, new HashSet<>());
        if (initialLikes.contains(currentUser)) {
            likeButton.setImageResource(R.drawable.ic_liked);
            likeButton.invalidate();
        } else {
            likeButton.setImageResource(R.drawable.ic_like);
        }

        likeButton.setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();
            if (imageUris != null && position < imageUris.size()) {
                Uri imageUri = imageUris.get(position);
                String key = imageUri.toString();

                Set<String> likes = likesMap.getOrDefault(key, new HashSet<>());
                if (likes.contains(currentUser)) {
                    likes.remove(currentUser);
                    likeButton.setImageResource(R.drawable.ic_like);
                } else {
                    likes.add(currentUser);
                    likeButton.setImageResource(R.drawable.ic_liked);
                }
                likesMap.put(key, likes);
                folderPrefs.edit().putString(likesKey, gson.toJson(likesMap)).apply();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("likedUri", imageUri.toString());
                resultIntent.putExtra("position", position);
                setResult(RESULT_FIRST_USER, resultIntent);
                updateLikeButtonState(likeButton, likesMap, currentUser);

            }
        });
    }

    private void updateLikeButtonState(ImageView likeButton, Map<String, Set<String>> likesMap, String currentUser) {
        String folderId = getIntent().getStringExtra("FOLDER_ID");
        if (imageUris == null || currentPosition >= imageUris.size()) return;

        String imageKey = imageUris.get(currentPosition).toString();
        Set<String> currentLikes = likesMap.getOrDefault(imageKey, new HashSet<>());

        if (currentLikes.contains(currentUser)) {
            likeButton.setImageResource(R.drawable.ic_liked);
        } else {
            likeButton.setImageResource(R.drawable.ic_like);
        }
    }



    private void applyBlurEffect(Uri imageUri) {
        try {
            Glide.with(this)
                    .asBitmap()
                    .load(imageUri)
                    .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                    .transition(BitmapTransitionOptions.withCrossFade())
                    .into(blurredBackground);
        } catch (Exception e) {
            Log.e(TAG, "Error applying blur effect: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffY = e2.getY() - e1.getY();

            if (diffY > SWIPE_THRESHOLD_Y && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                finishWithAnimation();
                return true;
            }
            return false;
        }
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }
}