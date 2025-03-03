package com.example.transferly.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.Request;
import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;
import com.example.transferly.adapters.ImagesAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.graphics.Rect;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;




public class UploadActivity extends AppCompatActivity implements ImagesAdapter.OnImageLongClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_SELECT_IMAGES = 102;
    private final List<Uri> selectedImages = new ArrayList<>();
    private ImagesAdapter imagesAdapter;
    private RecyclerView recyclerViewImages;
    private FloatingActionButton fabUpload;
    private TextView uploadIntroText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        fabUpload = findViewById(R.id.fabUpload);
        uploadIntroText = findViewById(R.id.uploadIntroText);
        ImageView reloadButton = findViewById(R.id.reloadButton);

        // Initially hide the reload button
        reloadButton.setVisibility(View.GONE);

        reloadButton.setOnClickListener(v -> {
            if (!selectedImages.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setMessage("Want to start over?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            selectedImages.clear();
                            imagesAdapter.notifyDataSetChanged();
                            recyclerViewImages.setVisibility(View.GONE);
                            uploadIntroText.setVisibility(View.VISIBLE);
                            reloadButton.setVisibility(View.GONE); // Hide reload button
                            fabUpload.setImageResource(R.drawable.ic_plus); // Change FAB to "+"
                            Toast.makeText(this, "All images cleared!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
            }
        });

        // RecyclerView configuration
        imagesAdapter = new ImagesAdapter(this, selectedImages, this);
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3)); // 3 items per row
        recyclerViewImages.setAdapter(imagesAdapter);
        recyclerViewImages.setHasFixedSize(true); // Previne rearanjarea accidentala
        recyclerViewImages.setItemViewCacheSize(20);

        // Ad si adapterul pt vizualizare full-screen
//        FullImageAdapter fullImageAdapter = new FullImageAdapter(this, selectedImages);
//        recyclerViewImages.setAdapter(fullImageAdapter);

        // Add spacing between grid items
        recyclerViewImages.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int spacing = 8; // Spacing in pixels
                outRect.left = spacing;
                outRect.right = spacing;
                outRect.top = spacing;
                outRect.bottom = spacing;
            }
        });

        fabUpload.setOnClickListener(v -> {
            if (selectedImages.isEmpty()) {
                checkPermissionsAndOpenGallery();
            } else {
                generateLink();
            }
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_upload);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_upload) {
                return true;
            } else if (item.getItemId() == R.id.nav_shared_folders) {
                startActivity(new Intent(this, SharedFoldersActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        loadImages();
    }



    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PERMISSIONS
                );
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSIONS
                );
            } else {
                openGallery();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_IMAGES && resultCode == RESULT_OK && data != null) {
            boolean imagesAdded = false;

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (!selectedImages.contains(imageUri)) {
                        selectedImages.add(imageUri);
                        imagesAdded = true;
                    }
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                if (!selectedImages.contains(imageUri)) {
                    selectedImages.add(imageUri);
                    imagesAdded = true;
                }
            }

            if (imagesAdded) {
                // notify adapter to refresh the RecyclerView
                imagesAdapter.notifyDataSetChanged();
                recyclerViewImages.setVisibility(View.VISIBLE);
                uploadIntroText.setVisibility(View.GONE);
                fabUpload.setImageResource(R.drawable.ic_generate_link); // change FAB icon to generate link button
                findViewById(R.id.reloadButton).setVisibility(View.VISIBLE); // show reload button
            } else {
                Toast.makeText(this, "No new images selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveImages() {
        SharedPreferences prefs = getSharedPreferences("TransferlyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> imageUris = new HashSet<>();
        for (Uri uri : selectedImages) {
            imageUris.add(uri.toString());
        }
        editor.putStringSet("selectedImages", imageUris);
        editor.apply();
    }

    private void loadImages() {
        SharedPreferences prefs = getSharedPreferences("TransferlyPrefs", MODE_PRIVATE);
        Set<String> savedImages = prefs.getStringSet("selectedImages", new HashSet<>());
        selectedImages.clear();
        for (String uri : savedImages) {
            selectedImages.add(Uri.parse(uri));
        }

        if (!selectedImages.isEmpty()) {
            imagesAdapter.notifyDataSetChanged();
            recyclerViewImages.setVisibility(View.VISIBLE);
            uploadIntroText.setVisibility(View.GONE);
            fabUpload.setImageResource(R.drawable.ic_generate_link);
            findViewById(R.id.reloadButton).setVisibility(View.VISIBLE);
        } else {
            recyclerViewImages.setVisibility(View.GONE);
            uploadIntroText.setVisibility(View.VISIBLE);
            findViewById(R.id.reloadButton).setVisibility(View.GONE);
        }
    }


    @Override
    public void onImageLongClick(int position) {
        selectedImages.remove(position);
        imagesAdapter.notifyItemRemoved(position);
        imagesAdapter.notifyItemRangeChanged(position, selectedImages.size());
        saveImages();

        if (selectedImages.isEmpty()) {
            recyclerViewImages.setVisibility(View.GONE);
            uploadIntroText.setVisibility(View.VISIBLE);
            fabUpload.setImageResource(R.drawable.ic_plus);
        }
    }

    public void onListEmptied() {
        recyclerViewImages.setVisibility(View.GONE);
        uploadIntroText.setVisibility(View.VISIBLE);
        findViewById(R.id.reloadButton).setVisibility(View.GONE); // hide reload button
        fabUpload.setImageResource(R.drawable.ic_plus); // Change FAB to "+"
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveImages();
    }



    private String getRealPathFromURI(Uri uri) {
        String result = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (cursor.moveToFirst()) {
                result = cursor.getString(columnIndex);
            }
            cursor.close();
        }
        return result;
    }



    private void generateLink() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Nu s-au selectat imagini", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String serverUrl = "http://192.168.100.52:8080/api/upload";
                OkHttpClient client = new OkHttpClient();
                List<String> uploadedFileNames = new ArrayList<>();

                for (Uri uri : selectedImages) {
                    String filePath = getRealPathFromURI(uri);
                    File file = new File(filePath);

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", file.getName(),
                                    RequestBody.create(MediaType.parse("image/*"), file))
                            .build();

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(serverUrl)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string().trim();

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response: " + responseBody);
                    }

                    uploadedFileNames.add(responseBody);
                }

                // Generează link-ul catre pagina HTML care conține toate imaginile
                String galleryUrl = "http://192.168.100.52:8080/gallery?files=" + String.join(",", uploadedFileNames);

                runOnUiThread(() -> showPopupWithLink(galleryUrl));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }




    private void showPopupWithLink(String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.popup_generate_link, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText linkText = view.findViewById(R.id.linkText);
        linkText.setText(link);

        view.findViewById(R.id.copyButton).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Shared Link", link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.shareButton).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, link);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // 🔹 Inchiderea pop-up-ului
        view.findViewById(R.id.closeButton).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
