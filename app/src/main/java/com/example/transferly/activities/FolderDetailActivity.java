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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.example.transferly.adapters.FolderImageAdapter;
import com.example.transferly.adapters.FolderMemberAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

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

    // Constants
    protected static final String PREFS_NAME = "SharedFoldersData";
    private static final String KEY_IMAGES_PREFIX = "IMAGES_";
    private static final String KEY_SUBFOLDERS_PREFIX = "SUBFOLDERS_";
    private static final String KEY_LIKES_PREFIX = "LIKES_";
    private static final int REQUEST_CODE_FULLSCREEN = 123;

    // UI Elements
    private RecyclerView likedRecyclerView, othersRecyclerView, duplicateRecyclerView, subfoldersRecyclerView;
    private View emptyFolderLayout;
    private Button startNowButton;
    private TextView folderDetailTitle;
    private ImageView deleteFolderBtn;
    private LinearLayout memberIconsLayout;
    private FloatingActionButton fabAddPhotos;

    // Data
    private List<String> likedImages, otherImages, duplicateImages, subfolders;
    private final List<String> selectedSubfolders = new ArrayList<>();
    private Map<String, Set<String>> likesMap = new HashMap<>();

    // Folder info
    private String folderId;
    private String folderName;
    private String currentUser;

    // Adapters
    private FolderImageAdapter othersAdapter;

    // Network
    private RequestQueue requestQueue;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> fullScreenLauncher;
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleGalleryResult(result.getResultCode(), result.getData())
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        initializeVariables();
        initializeViews();
        setupActivityResultLaunchers();

        if (!validateFolderAccess()) {
            return;
        }

        setupUI();
        loadInitialData();
    }

    private void initializeVariables() {
        folderId = getIntent().getStringExtra("FOLDER_ID");
        folderName = getIntent().getStringExtra("FOLDER_NAME");

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUser = prefs.getString("username", "guest");

        requestQueue = Volley.newRequestQueue(this);

        likedImages = new ArrayList<>();
        otherImages = new ArrayList<>();
        duplicateImages = new ArrayList<>();
        subfolders = new ArrayList<>();
        likesMap = new HashMap<>();
    }

    private void initializeViews() {
        folderDetailTitle = findViewById(R.id.folderDetailTitle);
        likedRecyclerView = findViewById(R.id.likedRecyclerView);
        othersRecyclerView = findViewById(R.id.othersRecyclerView);
        duplicateRecyclerView = findViewById(R.id.duplicateRecyclerView);
        subfoldersRecyclerView = findViewById(R.id.subfoldersRecyclerView);
        emptyFolderLayout = findViewById(R.id.emptyFolderLayout);
        startNowButton = findViewById(R.id.startNowButton);
        deleteFolderBtn = findViewById(R.id.deleteFolderBtn);
        memberIconsLayout = findViewById(R.id.memberIconsLayout);
        fabAddPhotos = findViewById(R.id.fabAddPhotos);
    }

    private void setupActivityResultLaunchers() {
        fullScreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleFullScreenResult(result.getResultCode(), result.getData())
        );
    }

    private boolean validateFolderAccess() {
        if (folderId == null) {
            Toast.makeText(this, "Invalid folder ID", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        SharedPreferences cryptoPrefs = getSharedPreferences("crypto_prefs", MODE_PRIVATE);
        String hexKey = cryptoPrefs.getString("KEY_" + folderId, null);
        if (hexKey == null) {
            Toast.makeText(this, "Missing decryption key for folder", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        return true;
    }

    private void setupUI() {
        folderDetailTitle.setText(folderName != null ? folderName : "No Folder Name");

        fabAddPhotos.setOnClickListener(v -> showOptions());
        startNowButton.setOnClickListener(v -> showOptions());
        deleteFolderBtn.setOnClickListener(v -> showDeleteConfirmation());

        fetchAndDisplayFolderMembers();
    }

    private void loadInitialData() {
        // Load cached data first
        if (folderName != null) {
            loadFolderData(folderName);
        }

        // Check if we need to fetch from server
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!sharedPreferences.contains(KEY_IMAGES_PREFIX + folderId)) {
            fetchFilesForSharedFolder(folderId);
        } else {
            Log.d(TAG, "Using cached images for folder: " + folderId);
        }

        updateUI();
    }

    private void handleGalleryResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> newImages = new ArrayList<>();

        if (data.getClipData() != null) {
            // Multiple images selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                newImages.add(imageUri.toString());
            }
        } else if (data.getData() != null) {
            // Single image selected
            Uri imageUri = data.getData();
            newImages.add(imageUri.toString());
        }

        if (!newImages.isEmpty()) {
            otherImages.addAll(newImages);
            saveFolderData(folderName);
            updateUI();
            Toast.makeText(this, "Added " + newImages.size() + " image(s)", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFullScreenResult(int resultCode, Intent data) {
        Log.d(TAG, "handleFullScreenResult - resultCode: " + resultCode);

        if (resultCode == RESULT_OK && data != null) {
            handleCroppedImage(data);
        } else if (resultCode == RESULT_FIRST_USER && data != null) {
            handleImageLiked(data);
        } else if (resultCode == RESULT_CANCELED && data != null) {
            handleImageDeleted(data);
        }
    }

    private void handleCroppedImage(Intent data) {
        String croppedUriStr = data.getStringExtra("croppedUri");
        int position = data.getIntExtra("position", -1);

        Log.d(TAG, "Cropped image - URI: " + croppedUriStr + ", Position: " + position);

        if (croppedUriStr != null && position >= 0 && position < otherImages.size()) {
            try {
                otherImages.set(position, croppedUriStr);
                saveFolderData(folderName);

                if (othersAdapter != null) {
                    othersAdapter.updateImage(position, Uri.parse(croppedUriStr));
                    othersAdapter.notifyItemChanged(position);
                }

                updateUI();
                Toast.makeText(this, "Image cropped and saved successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error processing cropped image", e);
                Toast.makeText(this, "Error saving cropped image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleImageLiked(Intent data) {
        String likedUri = data.getStringExtra("likedUri");
        int position = data.getIntExtra("position", -1);

        if (likedUri != null && position >= 0) {
            toggleImageLike(likedUri, position);
        }
    }

    private void handleImageDeleted(Intent data) {
        String deletedUri = data.getStringExtra("deletedUri");
        int position = data.getIntExtra("position", -1);

        if (deletedUri != null && position >= 0 && otherImages.contains(deletedUri)) {
            otherImages.remove(deletedUri);
            likesMap.remove(deletedUri);
            saveFolderData(folderName);

            if (othersAdapter != null) {
                othersAdapter.notifyItemRemoved(position);
            }

            updateUI();
            Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleImageLike(String imageUri, int position) {
        Set<String> currentLikes = likesMap.getOrDefault(imageUri, new HashSet<>());

        if (currentLikes.contains(currentUser)) {
            currentLikes.remove(currentUser);
        } else {
            currentLikes.add(currentUser);
        }

        likesMap.put(imageUri, currentLikes);
        saveFolderData(folderName);

        if (othersAdapter != null) {
            othersAdapter.notifyItemChanged(position);
        }

        updateUI();
    }

    private void updateUI() {
        boolean hasContent = !likedImages.isEmpty() || !otherImages.isEmpty() ||
                !duplicateImages.isEmpty() || !subfolders.isEmpty();

        // Update visibility
        emptyFolderLayout.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        fabAddPhotos.setVisibility(hasContent ? View.VISIBLE : View.GONE);

        subfoldersRecyclerView.setVisibility(subfolders.isEmpty() ? View.GONE : View.VISIBLE);
        likedRecyclerView.setVisibility(likedImages.isEmpty() ? View.GONE : View.VISIBLE);
        othersRecyclerView.setVisibility(otherImages.isEmpty() ? View.GONE : View.VISIBLE);
        duplicateRecyclerView.setVisibility(duplicateImages.isEmpty() ? View.GONE : View.VISIBLE);

        // Setup RecyclerViews
        setupSubfoldersRecyclerView();
        setupImageRecyclerView(likedRecyclerView, likedImages, false);
        setupImageRecyclerView(othersRecyclerView, otherImages, true);
        setupImageRecyclerView(duplicateRecyclerView, duplicateImages, false);
    }

    private void setupSubfoldersRecyclerView() {
        if (subfolders.isEmpty()) return;

        subfoldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        FolderAdapter adapter = new FolderAdapter(
                this,
                subfolders,
                selectedSubfolders,
                this::openFolder,
                this::toggleSubfolderSelection,
                viewHolder -> {
                    // Additional setup if needed
                }
        );
        subfoldersRecyclerView.setAdapter(adapter);
    }

    private void setupImageRecyclerView(RecyclerView recyclerView, List<String> images, boolean isOthersRecycler) {
        if (images.isEmpty()) return;

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // Sort images by likes (descending)
        List<String> sortedImages = new ArrayList<>(images);
        Collections.sort(sortedImages, (img1, img2) -> {
            int likes1 = likesMap.getOrDefault(img1, new HashSet<>()).size();
            int likes2 = likesMap.getOrDefault(img2, new HashSet<>()).size();
            return Integer.compare(likes2, likes1);
        });

        // Convert to URIs
        List<Uri> imageUris = new ArrayList<>();
        for (String path : sortedImages) {
            imageUris.add(Uri.parse(path));
        }

        FolderImageAdapter adapter = new FolderImageAdapter(
                this,
                imageUris,
                createImageActionListener(sortedImages),
                fullScreenLauncher,
                likesMap
        );

        recyclerView.setAdapter(adapter);

        // Save reference to others adapter
        if (isOthersRecycler) {
            othersAdapter = adapter;
        }
    }

    private FolderImageAdapter.OnImageActionListener createImageActionListener(List<String> images) {
        return new FolderImageAdapter.OnImageActionListener() {
            @Override
            public void onImageLiked(int position) {
                if (position >= 0 && position < images.size()) {
                    String imageUri = images.get(position);
                    toggleImageLike(imageUri, position);
                }
            }

            @Override
            public void onImageDeleted(int position) {
                if (position >= 0 && position < images.size()) {
                    String imageUri = images.get(position);
                    images.remove(position);
                    likesMap.remove(imageUri);
                    saveFolderData(folderName);
                    updateUI();
                }
            }
        };
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
            Log.e(TAG, "Error opening gallery", e);
            Toast.makeText(this, "Error opening gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

            String fullPath = folderName + "/" + subfolderName;

            if (subfolders.contains(fullPath)) {
                Toast.makeText(this, "A subfolder with this name already exists", Toast.LENGTH_SHORT).show();
            } else {
                subfolders.add(fullPath);
                saveFolderData(folderName);
                updateUI();
                openFolder(fullPath);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openFolder(String targetFolderName) {
        if (targetFolderName.equals(folderName)) {
            Toast.makeText(this, "Cannot open the same folder recursively", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, FolderDetailActivity.class);
        intent.putExtra("FOLDER_NAME", targetFolderName);
        intent.putExtra("FOLDER_ID", folderId);
        startActivity(intent);
    }

    private void toggleSubfolderSelection(String folderName) {
        if (selectedSubfolders.contains(folderName)) {
            selectedSubfolders.remove(folderName);
        } else {
            selectedSubfolders.add(folderName);
        }

        if (subfoldersRecyclerView.getAdapter() != null) {
            subfoldersRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete this folder?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFolderFromServer(folderId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveFolderData(String folderName) {
        if (folderName == null || folderId == null) {
            Log.e(TAG, "Cannot save data - folderName or folderId is null");
            return;
        }

        Log.d(TAG, "Saving data for folder: " + folderName + " (ID: " + folderId + ")");

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();

        // Save using folderName as key for consistency
        editor.putString(KEY_IMAGES_PREFIX + folderName, gson.toJson(otherImages));
        editor.putString(KEY_SUBFOLDERS_PREFIX + folderName, gson.toJson(subfolders));
        editor.putString(KEY_LIKES_PREFIX + folderName, gson.toJson(likesMap));

        // Also save using folderId for server sync
        editor.putString(KEY_IMAGES_PREFIX + folderId, gson.toJson(otherImages));

        boolean success = editor.commit();
        Log.d(TAG, "Save result: " + (success ? "successful" : "failed"));

        if (success) {
            runOnUiThread(this::updateUI);
        }
    }

    private void loadFolderData(String folderName) {
        if (folderName == null) return;

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();

        // Load images
        String imagesJson = sharedPreferences.getString(KEY_IMAGES_PREFIX + folderName, null);
        if (imagesJson != null) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> loadedImages = gson.fromJson(imagesJson, type);
            if (loadedImages != null) {
                otherImages.clear();
                otherImages.addAll(loadedImages);
            }
        }

        // Load subfolders
        String subfoldersJson = sharedPreferences.getString(KEY_SUBFOLDERS_PREFIX + folderName, null);
        if (subfoldersJson != null) {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> loadedSubfolders = gson.fromJson(subfoldersJson, type);
            if (loadedSubfolders != null) {
                subfolders.clear();
                subfolders.addAll(loadedSubfolders);
            }
        }

        // Load likes
        String likesJson = sharedPreferences.getString(KEY_LIKES_PREFIX + folderName, null);
        if (likesJson != null) {
            Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
            Map<String, Set<String>> loadedLikes = gson.fromJson(likesJson, type);
            if (loadedLikes != null) {
                likesMap.clear();
                likesMap.putAll(loadedLikes);
            }
        }

        Log.d(TAG, "Loaded data - Images: " + otherImages.size() +
                ", Subfolders: " + subfolders.size() +
                ", Likes: " + likesMap.size());
    }

    private void fetchFilesForSharedFolder(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Fetched " + response.length() + " files from server");
                    otherImages.clear();

                    for (int i = 0; i < response.length(); i++) {
                        String fileUrl = response.optString(i);
                        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
                            downloadAndSaveImage(fileUrl, folderId);
                        }
                    }
                },
                error -> {
                    Log.e(TAG, "Failed to fetch files", error);
                    Toast.makeText(this, "Failed to load shared files", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
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
                            if (!dir.exists()) {
                                boolean created = dir.mkdirs();
                                Log.d(TAG, "Created directory: " + created);
                            }

                            String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                            File imageFile = new File(dir, fileName);

                            try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                                resource.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            }

                            String localUri = Uri.fromFile(imageFile).toString();
                            otherImages.add(localUri);

                            runOnUiThread(() -> {
                                updateUI();
                                saveFolderData(folderName);
                            });

                            // Delete from server after successful download
                            deleteFileFromNAS(folderId, fileName);

                        } catch (IOException e) {
                            Log.e(TAG, "Error saving image", e);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // No action needed
                    }
                });
    }

    private void deleteFileFromNAS(String folderId, String filename) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files/" + filename;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> Log.d(TAG, "Successfully deleted " + filename + " from NAS"),
                error -> Log.e(TAG, "Failed to delete " + filename + " from NAS", error));

        requestQueue.add(request);
    }

    private void fetchAndDisplayFolderMembers() {
        fetchFolderMembers(folderId, members -> {
            if (members != null && !members.isEmpty()) {
                String text = "Shared with: " + String.join(", ", members);
//                folderMembersInfo.setText(text);
//                shareInfoBar.setVisibility(View.VISIBLE);

                // Show delete button only for folder owner (first member)
                if (members.get(0).equals(currentUser)) {
                    deleteFolderBtn.setVisibility(View.VISIBLE);
                }

                displayMemberIcons(members);
            }
        });
    }

    private void displayMemberIcons(List<String> members) {
        memberIconsLayout.removeAllViews();

        for (String member : members) {
            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(4, 4, 4, 4);
            icon.setLayoutParams(params);
            icon.setPadding(12, 12, 12, 12);
            icon.setImageResource(R.drawable.ic_default_profile);
            memberIconsLayout.addView(icon);
        }

        memberIconsLayout.setOnClickListener(v -> showFolderMembersPopup(folderId, members));
    }

    private void fetchFolderMembers(String folderId, Consumer<List<String>> callback) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/members";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> members = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        String member = response.optString(i);
                        if (member != null && !member.trim().isEmpty()) {
                            members.add(member);
                        }
                    }
                    callback.accept(members);
                },
                error -> {
                    Log.e(TAG, "Failed to fetch folder members", error);
                    Toast.makeText(this, "Could not load members", Toast.LENGTH_SHORT).show();
                    callback.accept(new ArrayList<>());
                });

        requestQueue.add(request);
    }

    private void deleteFolderFromServer(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/delete/" + folderId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> {
                    Toast.makeText(this, "Folder deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Log.e(TAG, "Failed to delete folder", error);
                    Toast.makeText(this, "Failed to delete folder", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void showFolderMembersPopup(String folderId, List<String> currentMembers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_folder_members, null);
        RecyclerView recycler = view.findViewById(R.id.recyclerMembers);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        FolderMemberAdapter adapter = new FolderMemberAdapter(currentMembers, username -> {
            new AlertDialog.Builder(this)
                    .setTitle("Remove member")
                    .setMessage("Remove " + username + " from this folder?")
                    .setPositiveButton("Yes", (d, w) -> removeMemberFromFolder(folderId, username))
                    .setNegativeButton("No", null)
                    .show();
        });

        recycler.setAdapter(adapter);

        view.findViewById(R.id.btnAddFriend).setOnClickListener(v ->
                showAddableFriendsDialog(folderId, currentMembers));

        builder.setView(view)
                .setTitle("Folder Members")
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAddableFriendsDialog(String folderId, List<String> existingMembers) {
        getMyFriends(allFriends -> {
            List<String> addable = new ArrayList<>();
            for (String f : allFriends) {
                if (!existingMembers.contains(f)) addable.add(f);
            }

            String[] array = addable.toArray(new String[0]);
            boolean[] checked = new boolean[array.length];

            new AlertDialog.Builder(this)
                    .setTitle("Add Friends")
                    .setMultiChoiceItems(array, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton("Add", (dialog, which) -> {
                        for (int i = 0; i < array.length; i++) {
                            if (checked[i]) {
                                String friend = array[i];
                                Log.d("ADD_MEMBER", "Sending add request for friend: " + friend);
                                addFriendToFolderOnServer(folderId, friend);

                            }
                        }
                    })

                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }


    private void removeMemberFromFolder(String folderId, String username) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/removeMember";

        try {
            JSONObject body = new JSONObject();
            body.put("member", username);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        Toast.makeText(this, "Removed " + username, Toast.LENGTH_SHORT).show();
                        // Refresh the members list
                        fetchAndDisplayFolderMembers();
                    },
                    error -> {
                        Log.e(TAG, "Failed to remove member", error);
                        Toast.makeText(this, "Failed to remove " + username, Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error creating remove member request", e);
            Toast.makeText(this, "Error removing member", Toast.LENGTH_SHORT).show();
        }
    }

    private void getMyFriends(Consumer<List<String>> callback) {
        String url = "http://transferly.go.ro:8080/api/users/" + currentUser + "/friends";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> friends = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        String friend = response.optString(i);
                        if (friend != null && !friend.trim().isEmpty()) {
                            friends.add(friend);
                        }
                    }
                    callback.accept(friends);
                },
                error -> {
                    Log.e(TAG, "Failed to fetch friends", error);
                    Toast.makeText(this, "Could not load friends", Toast.LENGTH_SHORT).show();
                    callback.accept(new ArrayList<>());
                });

        requestQueue.add(request);
    }

    private void addFriendToFolderOnServer(String folderId, String friendUsername) {
        try {
            Long longFolderId = Long.parseLong(folderId);
            String url = "http://transferly.go.ro:8080/api/shared/" + longFolderId + "/addMember";

            JSONObject body = new JSONObject();
            body.put("member", friendUsername); 

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        Toast.makeText(this, "Added " + friendUsername + " to folder", Toast.LENGTH_SHORT).show();
                        fetchAndDisplayFolderMembers();
                    },
                    error -> {
                        Log.e("ADD_MEMBER", "Failed to add " + friendUsername + " to folder", error);
                        Toast.makeText(this, "Failed to add " + friendUsername, Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            Log.e("ADD_MEMBER", "Exception while adding " + friendUsername, e);
            Toast.makeText(this, "Error adding friend", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.folder_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            fetchFilesForSharedFolder(folderId);
            fetchAndDisplayFolderMembers();
            return true;
        } else if (id == R.id.action_share_folder) {
            shareFolder();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareFolder() {
        String shareText = "Join my shared folder: " + folderName +
                "\nFolder ID: " + folderId +
                "\nDownload Transferly app to access shared photos!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Folder - " + folderName);

        startActivity(Intent.createChooser(shareIntent, "Share Folder"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (folderName != null) {
            loadFolderData(folderName);
            updateUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    // Helper method to check if current user is folder owner
    private boolean isCurrentUserOwner(List<String> members) {
        return members != null && !members.isEmpty() && members.get(0).equals(currentUser);
    }

    // Helper method to format member count text
    private String formatMemberText(List<String> members) {
        if (members == null || members.isEmpty()) {
            return "Private folder";
        }

        int count = members.size();
        if (count == 1) {
            return "Private folder";
        } else if (count == 2) {
            return "Shared with 1 person";
        } else {
            return "Shared with " + (count - 1) + " people";
        }
    }

    // Method to handle network connectivity issues
    private void handleNetworkError(String operation) {
        String message = "Network error during " + operation + ". Please check your connection.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    // Method to validate image URI before processing
    private boolean isValidImageUri(String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) {
            return false;
        }

        try {
            Uri uri = Uri.parse(uriString);
            return uri != null && (uri.getScheme() != null);
        } catch (Exception e) {
            Log.e(TAG, "Invalid URI: " + uriString, e);
            return false;
        }
    }

    // Method to clean up temporary files
    private void cleanupTempFiles() {
        try {
            File tempDir = new File(getCacheDir(), "temp_images");
            if (tempDir.exists() && tempDir.isDirectory()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            boolean deleted = file.delete();
                            Log.d(TAG, "Deleted temp file: " + file.getName() + " - " + deleted);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up temp files", e);
        }
    }
}