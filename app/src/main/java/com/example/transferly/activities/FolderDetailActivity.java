package com.example.transferly.activities;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
            fetchFilesForSharedFolder(folderId);
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

    }

    private void updateUI() {
        boolean hasContent = !likedImages.isEmpty() || !otherImages.isEmpty() || !duplicateImages.isEmpty() || !subfolders.isEmpty();

        emptyFolderLayout.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        likedRecyclerView.setVisibility(likedImages.isEmpty() ? View.GONE : View.VISIBLE);
        othersRecyclerView.setVisibility(otherImages.isEmpty() ? View.GONE : View.VISIBLE);
        duplicateRecyclerView.setVisibility(duplicateImages.isEmpty() ? View.GONE : View.VISIBLE);
        subfoldersRecyclerView.setVisibility(subfolders.isEmpty() ? View.GONE : View.VISIBLE);

        setupRecyclerView(likedRecyclerView, likedImages);
        setupRecyclerView(othersRecyclerView, otherImages);
        setupRecyclerView(duplicateRecyclerView, duplicateImages);
        setupSubfoldersRecyclerView();

        saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
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

    private void setupRecyclerView(RecyclerView recyclerView, List<String> images) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        FolderImageAdapter adapter = new FolderImageAdapter(this, images, new FolderImageAdapter.OnImageActionListener() {
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
        });
        recyclerView.setAdapter(adapter);
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


    private void openFolder(String folderName) {
        if (folderName.equals(getIntent().getStringExtra("FOLDER_NAME"))) {
            Toast.makeText(this, "Cannot open the same folder recursively", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FolderDetailActivity.class);
        intent.putExtra("FOLDER_NAME", folderName);
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

            if (subfolderName.equals(getIntent().getStringExtra("FOLDER_NAME"))) {
                Toast.makeText(this, "Subfolder name cannot match parent folder name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (subfolders.contains(subfolderName)) {
                Toast.makeText(this, "A subfolder with this name already exists", Toast.LENGTH_SHORT).show();
            } else {
                subfolders.add(subfolderName);
                saveFolderData(getIntent().getStringExtra("FOLDER_NAME"));
                openFolder(subfolderName);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void saveFolderData(String folderName) {
        String folderId = getIntent().getStringExtra("FOLDER_ID");
        if (folderId == null) return;

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        editor.putString(KEY_IMAGES_PREFIX + folderId, gson.toJson(otherImages));
        editor.putString(KEY_SUBFOLDERS_PREFIX + folderId, gson.toJson(subfolders));
        editor.apply();
    }



    private void loadFolderData(String folderName) {
        String folderId = getIntent().getStringExtra("FOLDER_ID");
        if (folderId == null) return;

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        String imagesJson = sharedPreferences.getString(KEY_IMAGES_PREFIX + folderId, null);
        otherImages = imagesJson != null ? gson.fromJson(imagesJson, new TypeToken<List<String>>() {}.getType()) : new ArrayList<>();

        String subfoldersJson = sharedPreferences.getString(KEY_SUBFOLDERS_PREFIX + folderId, null);
        subfolders = subfoldersJson != null ? gson.fromJson(subfoldersJson, new TypeToken<List<String>>() {}.getType()) : new ArrayList<>();
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





}
