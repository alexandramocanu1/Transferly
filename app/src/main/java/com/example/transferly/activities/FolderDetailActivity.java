package com.example.transferly.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.example.transferly.adapters.FolderImageAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class FolderDetailActivity extends AppCompatActivity {

    protected static final String PREFS_NAME = "SharedFoldersData";
    private static final String KEY_IMAGES_PREFIX = "IMAGES_";
    private static final String KEY_SUBFOLDERS_PREFIX = "SUBFOLDERS_";

    private RecyclerView likedRecyclerView, othersRecyclerView, duplicateRecyclerView, subfoldersRecyclerView;
    private View emptyFolderLayout;
    private Button startNowButton;

    private List<String> likedImages, otherImages, duplicateImages, subfolders;
    private final List<String> selectedSubfolders = new ArrayList<>();
    private Map<String, Set<String>> likesMap = new HashMap<>();


    private ActivityResultLauncher<Intent> fullScreenLauncher;




    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = data.getClipData().getItemAt(i).getUri();
                            otherImages.add(imageUri.toString());
                        }
                    } else if (data.getData() != null) {
                        Uri imageUri = data.getData();
                        otherImages.add(imageUri.toString());
                    }
                    updateUI(); // Actualizeaza UI dupa ce se adauga poze
                } else {
                    Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        FloatingActionButton fabAddPhotos = findViewById(R.id.fabAddPhotos);
//        fabAddPhotos.setOnClickListener(v -> openGalleryForPhotos());
        fabAddPhotos.setOnClickListener(v -> showOptions());



        String folderId = getIntent().getStringExtra("FOLDER_ID");

        SharedPreferences cryptoPrefs = getSharedPreferences("crypto_prefs", MODE_PRIVATE);
        String hexKey = cryptoPrefs.getString("KEY_" + folderId, null);
        if (hexKey == null) {
            Toast.makeText(this, "Missing decryption key for folder", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        String folderName = getIntent().getStringExtra("FOLDER_NAME");
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUser = prefs.getString("username", "guest");

        TextView folderMembersInfo = findViewById(R.id.folderMembersInfo);
        ImageView deleteFolderBtn = findViewById(R.id.deleteFolderBtn);
        View shareInfoBar = findViewById(R.id.shareInfoBar);

        fetchFolderMembers(folderId, members -> {
            String text = "Shared with: " + String.join(", ", members);
            folderMembersInfo.setText(text);
            shareInfoBar.setVisibility(View.VISIBLE);

            if (members.get(0).equals(currentUser)) { // merge pt început
                deleteFolderBtn.setVisibility(View.VISIBLE);
                deleteFolderBtn.setOnClickListener(v -> confirmFolderDelete(folderId, folderName, members));
            }
        });


        if (folderId != null) {
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (!sharedPreferences.contains(KEY_IMAGES_PREFIX + folderId)) {
                fetchFilesForSharedFolder(folderId);
            } else {
                Log.d(TAG, "Skipping fetchFilesForSharedFolder, images already cached");
            }
        }




        TextView folderDetailTitle = findViewById(R.id.folderDetailTitle);
        likedRecyclerView = findViewById(R.id.likedRecyclerView);
        othersRecyclerView = findViewById(R.id.othersRecyclerView);
        duplicateRecyclerView = findViewById(R.id.duplicateRecyclerView);
        subfoldersRecyclerView = findViewById(R.id.subfoldersRecyclerView);
        emptyFolderLayout = findViewById(R.id.emptyFolderLayout);
        startNowButton = findViewById(R.id.startNowButton);

        folderDetailTitle.setText(folderName != null ? folderName : "No Folder Name");

        likedImages = new ArrayList<>();
        otherImages = new ArrayList<>();
        duplicateImages = new ArrayList<>();
        subfolders = new ArrayList<>();

        if (folderName != null) {
            loadFolderData(folderName);
        }

        updateUI();

        startNowButton.setOnClickListener(v -> showOptions());

        deleteFolderBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Folder")
                    .setMessage("Are you sure you want to delete this folder?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteFolderFromServer(folderId);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });



        fullScreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Got result from FolderImageActivity: " + result.getResultCode());

                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String croppedUriStr = result.getData().getStringExtra("croppedUri");
                        int position = result.getData().getIntExtra("position", -1);

                        Log.d(TAG, "Received cropped URI: " + croppedUriStr + ", Position: " + position);

                        if (croppedUriStr != null && position >= 0) {
                            try {
                                Uri croppedUri = Uri.parse(croppedUriStr);

                                if (position < otherImages.size()) {
                                    // Update URI in the list
                                    otherImages.set(position, croppedUriStr);

                                    // Save the changes immediately
//                                    String folderName = getIntent().getStringExtra("FOLDER_NAME");
                                    saveFolderData(folderName);

                                    // Force refresh the adapter
                                    if (othersAdapter != null) {
                                        othersAdapter.updateImage(position, croppedUri);
                                        othersAdapter.notifyItemChanged(position);
                                        Log.d(TAG, "Updated adapter with new image at position " + position);
                                    } else {
                                        Log.e(TAG, "othersAdapter is null!");
                                        // If adapter is null, refresh everything
                                        updateUI();
                                    }

                                    Toast.makeText(FolderDetailActivity.this,
                                            "Image cropped and saved successfully",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e(TAG, "Position out of bounds: " + position + ", size: " + otherImages.size());
                                    Toast.makeText(FolderDetailActivity.this,
                                            "Error: Image position out of bounds",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing cropped image: " + e.getMessage(), e);
                                Toast.makeText(FolderDetailActivity.this,
                                        "Error saving cropped image: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Invalid data: cropped URI=" + croppedUriStr + ", position=" + position);
                        }
                    } else {
                        Log.d(TAG, "Result not OK or data is null");
                    }
                }
        );
    }



    private void updateUI() {
        boolean hasContent = !likedImages.isEmpty() || !otherImages.isEmpty() || !duplicateImages.isEmpty() || !subfolders.isEmpty();

        emptyFolderLayout.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        subfoldersRecyclerView.setVisibility(subfolders.isEmpty() ? View.GONE : View.VISIBLE);
        likedRecyclerView.setVisibility(likedImages.isEmpty() ? View.GONE : View.VISIBLE);
        othersRecyclerView.setVisibility(otherImages.isEmpty() ? View.GONE : View.VISIBLE);
        duplicateRecyclerView.setVisibility(duplicateImages.isEmpty() ? View.GONE : View.VISIBLE);


        View fabAddPhotos = findViewById(R.id.fabAddPhotos);
        fabAddPhotos.setVisibility(hasContent ? View.VISIBLE : View.GONE);

        setupSubfoldersRecyclerView();
        setupRecyclerView(likedRecyclerView, likedImages);
        setupRecyclerView(othersRecyclerView, otherImages);
        setupRecyclerView(duplicateRecyclerView, duplicateImages);

    }


    private void setupSubfoldersRecyclerView() {
        subfoldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        FolderAdapter adapter = new FolderAdapter(
                this,
                subfolders,
                selectedSubfolders,
                folderName -> openFolder(folderName),
                folderName -> toggleSubfolderSelection(folderName),
                viewHolder -> {
                }
        );
        subfoldersRecyclerView.setAdapter(adapter);
    }



    private void toggleSubfolderSelection(String folderName) {
        if (selectedSubfolders.contains(folderName)) {
            selectedSubfolders.remove(folderName);
        } else {
            selectedSubfolders.add(folderName);
        }
        subfoldersRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void deleteSelectedSubfolders() {
        if (selectedSubfolders.isEmpty()) {
            Toast.makeText(this, "No subfolders selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete the selected subfolders?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    subfolders.removeAll(selectedSubfolders);
                    selectedSubfolders.clear();
                    saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
                    subfoldersRecyclerView.getAdapter().notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private FolderImageAdapter othersAdapter;


    private void setupRecyclerView(RecyclerView recyclerView, List<String> images) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<Uri> imageUris = new ArrayList<>();

        Collections.sort(images, (img1, img2) -> {
            int l1 = likesMap.getOrDefault(img1, new HashSet<>()).size();
            int l2 = likesMap.getOrDefault(img2, new HashSet<>()).size();
            return Integer.compare(l2, l1); // desc
        });


        for (String path : images) {
            imageUris.add(Uri.parse(path));
            Log.d("FolderDetail", "Adding image to adapter: " + path);
        }

        // Dacă este recyclerview-ul pentru 'others', salvează adaptorul în variabila globală
        if (recyclerView.getId() == R.id.othersRecyclerView) {
            othersAdapter = new FolderImageAdapter(this, imageUris, new FolderImageAdapter.OnImageActionListener() {
                @Override
                public void onImageLiked(int position) {
                    String image = images.get(position);
                    String currentUser = "guest"; // sau ia din SharedPreferences

                    Set<String> currentLikes = likesMap.getOrDefault(image, new HashSet<>());
                    if (currentLikes.contains(currentUser)) {
                        currentLikes.remove(currentUser);
                    } else {
                        currentLikes.add(currentUser);
                    }
                    likesMap.put(image, currentLikes);
                    saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
                    updateUI();
                }


                @Override
                public void onImageDeleted(int position) {
                    images.remove(position);
                    updateUI();
                }
            }, fullScreenLauncher, likesMap);


            recyclerView.setAdapter(othersAdapter);
            Log.d("FolderDetail", "Others adapter set with " + imageUris.size() + " images");
        } else {
            // Pentru celelalte recyclerview-uri
            FolderImageAdapter adapter = new FolderImageAdapter(this, imageUris, new FolderImageAdapter.OnImageActionListener() {
                @Override
                public void onImageLiked(int position) {
                    if (!likedImages.contains(images.get(position))) {
                        likedImages.add(images.get(position));
                    } else {
                        likedImages.remove(images.get(position));
                    }
                    updateUI();
                }

                @Override
                public void onImageDeleted(int position) {
                    images.remove(position);
                    updateUI();
                }
            }, fullScreenLauncher, likesMap);


            recyclerView.setAdapter(adapter);
        }
    }

    private void showOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an Action")
                .setItems(new String[]{"Upload Photos", "Create Subfolder"}, (dialog, which) -> {
                    if (which == 0) {
                        openGalleryForPhotos();
                    } else if (which == 1) {
                        createSubfolder();
                    }
                });
        builder.show();
    }

    private void openGalleryForPhotos() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            galleryLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


//    private void openFolder(String folderName) {
//        if (folderName.equals(getIntent().getStringExtra("FOLDER_NAME"))) {
//            Toast.makeText(this, "Cannot open the same folder recursively", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        Intent intent = new Intent(this, FolderDetailActivity.class);
//        intent.putExtra("FOLDER_NAME", folderName);
//        startActivity(intent);
//    }

    private void openFolder(String folderName) {
        if (folderName.equals(getIntent().getStringExtra("FOLDER_NAME"))) {
            Toast.makeText(this, "Cannot open the same folder recursively", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, FolderDetailActivity.class);
        intent.putExtra("FOLDER_NAME", folderName);
//        intent.putExtra("FOLDER_NAME", fullPath);

        intent.putExtra("FOLDER_ID", getIntent().getStringExtra("FOLDER_ID"));

        startActivity(intent);
    }


    private void createSubfolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Subfolder");

        final EditText input = new EditText(this);
        input.setHint("Enter subfolder name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String subfolderName = input.getText().toString().trim();
            if (subfolderName.isEmpty()) {
                Toast.makeText(this, "Subfolder name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String parentFolder = getIntent().getStringExtra("FOLDER_NAME");
            String fullPath = parentFolder + "/" + subfolderName;

            if (subfolders.contains(fullPath)) {
                Toast.makeText(this, "A subfolder with this name already exists", Toast.LENGTH_SHORT).show();
            } else {
                subfolders.add(fullPath);
                saveFolderData(parentFolder); // ✅ salvăm doar în părintele

                openFolder(fullPath);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void saveFolderData(String folderName) {
        String folderId = getIntent().getStringExtra("FOLDER_ID");
        if (folderId == null) {
            Log.e(TAG, "FolderId is null, cannot save data");
            return;
        }

        Log.d(TAG, "Saving data for folder: " + folderName + " (ID: " + folderId + ")");
        Log.d(TAG, "Images count: " + otherImages.size());

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String imagesJson = gson.toJson(otherImages);
//        editor.putString(KEY_IMAGES_PREFIX + folderId, imagesJson);
//        editor.putString(KEY_SUBFOLDERS_PREFIX + folderId, gson.toJson(subfolders));
//        editor.putString("LIKES_" + folderId, gson.toJson(likesMap));

        editor.putString(KEY_IMAGES_PREFIX + folderName, imagesJson);
        editor.putString(KEY_SUBFOLDERS_PREFIX + folderName, gson.toJson(subfolders));
        editor.putString("LIKES_" + folderName, gson.toJson(likesMap));


        boolean success = editor.commit(); // Use commit() instead of apply() for immediate feedback
        Log.d(TAG, "Save result: " + (success ? "successful" : "failed"));

        // Force UI update after save
        runOnUiThread(this::updateUI);
    }


    private void loadFolderData(String folderName) {
        String folderId = getIntent().getStringExtra("FOLDER_ID");
        if (folderId == null) return;

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

//        String imagesJson = sharedPreferences.getString(KEY_IMAGES_PREFIX + folderId, null);
        String imagesJson = sharedPreferences.getString(KEY_IMAGES_PREFIX + folderName, null);
        otherImages = imagesJson != null ? gson.fromJson(imagesJson, new TypeToken<List<String>>() {}.getType()) : new ArrayList<>();

//        String subfoldersJson = sharedPreferences.getString(KEY_SUBFOLDERS_PREFIX + folderId, null);
        String subfoldersJson = sharedPreferences.getString(KEY_SUBFOLDERS_PREFIX + folderName, null);
        subfolders = subfoldersJson != null ? gson.fromJson(subfoldersJson, new TypeToken<List<String>>() {}.getType()) : new ArrayList<>();

//        String likesJson = sharedPreferences.getString("LIKES_" + folderId, null);
        String likesJson = sharedPreferences.getString("LIKES_" + folderName, null);
        if (likesJson != null) {
            Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
            likesMap = gson.fromJson(likesJson, type);
        } else {
            likesMap = new HashMap<>();
        }


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_folder_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_upload) {
            openGalleryForPhotos();
            return true;
        } else if (id == R.id.action_create_subfolder) {
            createSubfolder();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchFilesForSharedFolder(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    otherImages.clear(); // Șterge cele vechi
                    for (int i = 0; i < response.length(); i++) {
                        String fileUrl = response.optString(i);
                        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
                            downloadAndSaveImage(fileUrl, folderId); // 🆕 Apeleaza metoda
                        }
                    }
                },
                error -> {
                    Toast.makeText(this, "Failed to load shared files", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }


    private void downloadAndSaveImage(String imageUrl, String folderId) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            File dir = new File(getFilesDir(), "shared_" + folderId);
                            if (!dir.exists()) dir.mkdirs();

                            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                            File imageFile = new File(dir, fileName);
                            FileOutputStream outputStream = new FileOutputStream(imageFile);
                            resource.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // 100 = no compression
                            outputStream.close();

                            // Adauga local pentru UI
                            otherImages.add(Uri.fromFile(imageFile).toString());
                            updateUI();

                            // DELETE de pe NAS dupa descarcare
                            deleteFileFromNAS(folderId, fileName);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private void deleteFileFromNAS(String folderId, String filename) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files/" + filename;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> Log.d("NAS", "Deleted from NAS"),
                error -> Log.e("NAS", "Delete from NAS failed"));

        Volley.newRequestQueue(this).add(request);
    }

    private void fetchFolderMembers(String folderId, Consumer<List<String>> callback) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/members";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> members = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        members.add(response.optString(i));
                    }
                    callback.accept(members);
                },
                error -> Toast.makeText(this, "Could not load members", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void confirmFolderDelete(String folderId, String folderName, List<String> members) {
        String message = "Are you sure you want to delete this folder?\n\nIt will be removed from:\n- " + String.join("\n- ", members);

        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteFolderFromServer(folderId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void deleteFolderFromServer(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/delete/" + folderId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show();
                    finish(); // închide activitatea
                },
                error -> Toast.makeText(this, "Failed to delete folder", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {

            String croppedUriStr = data.getStringExtra("croppedUri");
            int position = data.getIntExtra("position", -1);

            Log.d(TAG, "onActivityResult - Cropped URI: " + croppedUriStr + ", Position: " + position);

            if (croppedUriStr != null && position >= 0 && position < otherImages.size()) {
                otherImages.set(position, croppedUriStr);

                // Save the data
                saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));

                // Update adapter directly
                if (othersAdapter != null) {
                    othersAdapter.updateImage(position, Uri.parse(croppedUriStr));
                    othersAdapter.notifyItemChanged(position);
                    Log.d(TAG, "Updated adapter in onActivityResult");
                }

                // Also update the UI
                updateUI();

            }
        }

        if (resultCode == RESULT_FIRST_USER && data != null) {
            String likedUri = data.getStringExtra("likedUri");
            int position = data.getIntExtra("position", -1);
            String currentUser = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("username", "guest");

            if (likedUri != null && position >= 0) {
                Set<String> currentLikes = likesMap.getOrDefault(likedUri, new HashSet<>());
                if (currentLikes.contains(currentUser)) {
                    currentLikes.remove(currentUser);
                } else {
                    currentLikes.add(currentUser);
                }
                likesMap.put(likedUri, currentLikes);

                othersAdapter.notifyItemChanged(position);

                saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
                updateUI();
            }
            return; // nu continuăm cu alte blocuri
        }

        if (resultCode == RESULT_CANCELED && data != null) {
            String deletedUri = data.getStringExtra("deletedUri");
            int position = data.getIntExtra("position", -1);

            if (deletedUri != null && position >= 0) {
                otherImages.remove(deletedUri);
                likesMap.remove(deletedUri);

                saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
                othersAdapter.notifyItemRemoved(position);
                updateUI();
                Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
            }
        }


    }
}
