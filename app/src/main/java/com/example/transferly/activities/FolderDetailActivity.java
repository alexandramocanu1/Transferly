package com.example.transferly.activities;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

//import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
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

import org.apache.commons.net.ftp.FTP;
//import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Request;



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
        // Load cached data first for immediate display
        if (folderName != null) {
            loadFolderData(folderName);
            updateUI();
        }

        fetchFilesForSharedFolder(folderId);
    }



    private void handleGalleryResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> newImages = new ArrayList<>();

        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                newImages.add(imageUri.toString());
            }
        } else if (data.getData() != null) {
            newImages.add(data.getData().toString());
        }

        if (newImages.isEmpty()) return;

        Toast.makeText(this, "Uploading " + newImages.size() + " image(s)...", Toast.LENGTH_SHORT).show();

        // ✅ Extrage subfolderul relativ (fără numele folderului principal)
        final String subfolder;
        if (folderName != null && folderName.contains("/")) {
            subfolder = folderName.substring(folderName.indexOf("/") + 1);
        } else {
            subfolder = null;  // folder principal
        }

        final String ftpFolder = "shared_" + folderId;
        final String currentFolderName = folderName;

        new Thread(() -> {
            int successCount = 0;
            List<String> successfulUploads = new ArrayList<>();
            StringBuilder errorLog = new StringBuilder();

            for (String imageUriStr : newImages) {
                try {
                    Uri imageUri = Uri.parse(imageUriStr);
                    String filePath = getFilePathFromURI(this, imageUri);

                    if (filePath == null) {
                        errorLog.append("❌ Failed to get file path for: ").append(imageUriStr).append("\n");
                        continue;
                    }

                    Log.d(TAG, "Uploading file: " + filePath);

                    String uploadedFilename = uploadToNAS(filePath, ftpFolder, subfolder);

                    if (uploadedFilename != null) {
                        Log.d(TAG, "✅ FTP upload successful: " + uploadedFilename);

                        boolean registered = registerFileInBackendSync(uploadedFilename, subfolder);
                        if (registered) {
                            successCount++;
                            String localPath = getFilesDir() + "/shared_" + folderId + "/" + uploadedFilename;
                            Uri fileUri = Uri.fromFile(new File(localPath));
                            successfulUploads.add(fileUri.toString());

                            Log.d(TAG, "✅ File registered successfully: " + uploadedFilename);
                        } else {
                            errorLog.append("❌ Failed to register: ").append(uploadedFilename).append(" on server\n");
                        }
                    } else {
                        errorLog.append("❌ FTP upload failed for: ").append(filePath).append("\n");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error uploading image: " + imageUriStr, e);
                    errorLog.append("❌ Exception for: ").append(imageUriStr).append(" - ").append(e.getMessage()).append("\n");
                }
            }

            final int finalSuccessCount = successCount;
            final String finalErrorLog = errorLog.toString();

            runOnUiThread(() -> {
                saveFolderData(currentFolderName);
                updateUI();

                if (finalSuccessCount > 0) {
                    Toast.makeText(this, "✅ Uploaded " + finalSuccessCount + "/" + newImages.size() + " image(s)", Toast.LENGTH_LONG).show();

                    // 👉 Forțează refresh complet, ca să apară noile fișiere:
                    fetchFilesForSharedFolder(folderId);
                    fetchSubfolders(folderId);

                } else {
                    Toast.makeText(this, "❌ Upload failed for all images", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Upload errors:\n" + finalErrorLog);
                }


                Log.d(TAG, "Upload summary - Success: " + finalSuccessCount + "/" + newImages.size());
                if (!finalErrorLog.isEmpty()) {
                    Log.e(TAG, "Upload errors:\n" + finalErrorLog);
                }
            });
        }).start();
    }



    private boolean registerFileInBackendSync(String filename, @Nullable String subfolder) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/file";

        Log.d(TAG, "🔄 Registering file on server: " + filename);
        Log.d(TAG, "📡 URL: " + url);

        try {
            java.net.URL urlObj = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000); // 10 secunde timeout
            conn.setReadTimeout(10000);

            String jsonData = "{\"filename\":\"" + filename + "\",\"uploadedBy\":\"" + currentUser + "\"";

            if (subfolder != null) {
                jsonData += ",\"subfolder\":\"" + subfolder + "\"";
            }

            jsonData += "}";

            Log.d(TAG, "📤 Sending JSON: " + jsonData);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonData.getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            Log.d(TAG, "📡 Server response: " + responseCode + " - " + responseMessage);

            if (responseCode >= 200 && responseCode < 300) {
                Log.d(TAG, "✅ File registered successfully: " + filename);
                return true;
            } else {
                try (java.io.InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = readInputStream(errorStream);
                        Log.e(TAG, "❌ Server error response: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "❌ Server timeout for file: " + filename, e);
            return false;
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "❌ Cannot reach server for file: " + filename, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to register file: " + filename, e);
            return false;
        }
    }


    private String readInputStream(java.io.InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }



    private String getFilePathFromURI(Context context, Uri uri) {
        Log.d(TAG, "🔄 Converting URI to file path: " + uri.toString());

        try {
            String fileName = "temp_image_" + System.currentTimeMillis() + ".jpg";
            File file = new File(context.getCacheDir(), fileName);

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "❌ Cannot open input stream for URI: " + uri);
                return null;
            }

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "✅ File created: " + file.getAbsolutePath());
            Log.d(TAG, "📋 File size: " + totalBytes + " bytes");

            return file.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to copy file from URI: " + uri, e);
            return null;
        }
    }


    private String uploadToNAS(String filePath, String ftpFolderName, @Nullable String subfolder)
    {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "❌ File does not exist: " + filePath);
            return null;
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            String folderIdStr = folderId;

            String url = "http://transferly.go.ro:8080/api/shared/" + folderIdStr + "/upload";
            if (subfolder != null) {
                url += "?subfolder=" + Uri.encode(subfolder);
            }


            Log.d(TAG, "🌐 Uploading to: " + url);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("image/*"), file))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(TAG, "❌ Upload failed: " + response.code() + " - " + response.message());
                return null;
            }

            Log.d(TAG, "✅ Upload successful: " + file.getName());
            return file.getName();  // Trebuie să se potrivească cu ceea ce trimiti în registerFileInBackendSync

        } catch (Exception e) {
            Log.e(TAG, "❌ HTTP upload exception: " + e.getMessage(), e);
            return null;
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
            // Extract filename from URI to delete from server
            String fileName = getFileNameFromUri(deletedUri);
            if (fileName != null) {
                deleteImageFromServer(fileName);
            }

            // Remove locally
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


    private void deleteImageFromServer(String fileName) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files/" + fileName;

        Log.d(TAG, "🌐 Deleting file from server: " + url);

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .delete()
                    .build();

            try {
                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Successfully deleted " + fileName + " from server");
                    runOnUiThread(this::notifyFolderUpdated);
                } else {
                    Log.e(TAG, "❌ Server delete failed. Code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(this, "Failed to delete from server", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception deleting file: " + fileName, e);
                runOnUiThread(() -> Toast.makeText(this, "Error deleting file", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private String getFileNameFromUri(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if ("file".equals(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null) {
                    return path.substring(path.lastIndexOf('/') + 1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting filename from URI: " + uriString, e);
        }
        return null;
    }


    private void notifyFolderUpdated() {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/notify-update";

        Map<String, String> data = new HashMap<>();
        data.put("updatedBy", currentUser);
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        JSONObject json = new JSONObject(data);

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        new Thread(() -> {
            try {
                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Folder update notification sent");

                    // 👉 AICI forțăm sync pe UI thread
                    runOnUiThread(() -> {
                        fetchFilesForSharedFolder(folderId);
                    });

                } else {
                    Log.e(TAG, "❌ Failed to notify update - Code: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception while notifying folder update", e);
            }
        }).start();
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

            // Build the full path for the new subfolder
            String fullPath;
            if (folderName == null || folderName.isEmpty()) {
                fullPath = subfolderName;
            } else {
                fullPath = folderName + "/" + subfolderName;
            }

            // Check if subfolder already exists
            boolean alreadyExists = false;
            for (String existingSubfolder : subfolders) {
                if (existingSubfolder.equals(fullPath)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (alreadyExists) {
                Toast.makeText(this, "A subfolder with this name already exists", Toast.LENGTH_SHORT).show();
            } else {
                // Add to local list (this will be filtered properly when fetched from server)
                subfolders.add(fullPath);
                saveFolderData(folderName);
                updateUI();

                // Register on server
                registerSubfolderInBackend(fullPath);

                // Open the new subfolder
                openFolder(fullPath);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void openFolder(String targetFolderName) {
        // ✅ Nu permite să redeschizi același folder
        if (targetFolderName.equals(folderName)) {
            Toast.makeText(this, "Already in this folder", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Deschide FolderDetailActivity cu același folderId, dar cu targetFolderName ca subfolder
        Intent intent = new Intent(this, FolderDetailActivity.class);
        intent.putExtra("FOLDER_ID", folderId);
        intent.putExtra("FOLDER_NAME", targetFolderName);
        startActivity(intent);
        finish();  // Închide activitatea curentă pentru a evita stocarea folderului vechi
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


    private String extractFileName(String uriStr) {
        try {
            Uri uri = Uri.parse(uriStr);
            String path = uri.getPath();
            if (path != null) {
                return path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting file name from URI: " + uriStr, e);
        }
        return uriStr;
    }





    private void fetchFilesForSharedFolder(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/files";

        if (folderName != null && folderName.contains("/")) {
            String[] parts = folderName.split("/", 2);
            String subfolderName = parts[1];
            url += "?subfolder=" + Uri.encode(subfolderName);
        }

        OkHttpClient client = new OkHttpClient();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try {
                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Failed to fetch files. Code: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                JSONArray jsonArray = new JSONArray(responseBody);

                Log.d(TAG, "Fetched " + jsonArray.length() + " files from server");

                List<String> serverImages = new ArrayList<>();

//                for (int i = 0; i < jsonArray.length(); i++) {
//                    String entry = jsonArray.optString(i);
//                    if (entry == null || entry.trim().isEmpty()) continue;
//
//                    String fileName = entry.substring(entry.lastIndexOf('/') + 1);
//                    File localFile = new File(getFilesDir() + "/shared_" + folderId, fileName);
//
//                    if (localFile.exists()) {
//                        String localUri = Uri.fromFile(localFile).toString();
//                        serverImages.add(localUri);
//                    } else {
//                        downloadAndSaveImage(entry, folderId, serverImages);
//                    }
//                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    String entry = jsonArray.optString(i);
                    if (entry == null || entry.trim().isEmpty()) continue;

                    if (entry.startsWith("folder:")) {
                        String subfolderName = entry.substring("folder:".length());
                        subfolders.add(subfolderName);
                        continue;
                    }

                    downloadAndSaveImage(entry, folderId, serverImages);
                }


                runOnUiThread(() -> {
                    otherImages.clear();
                    otherImages.addAll(serverImages);
                    updateUI();
                    saveFolderData(folderName);
                });

                // După ce a luat fișierele, face request separat la /subfolders:
                fetchSubfolders(folderId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception fetching files", e);
            }
        }).start();
    }



    private void fetchSubfolders(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/subfolders";
        OkHttpClient client = new OkHttpClient();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try {
                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Failed to fetch subfolders. Code: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                JSONArray jsonArray = new JSONArray(responseBody);

                List<String> serverSubfolders = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    serverSubfolders.add(jsonArray.getString(i));
                }

                // Filter subfolders to show only direct children of current folder
                List<String> filteredSubfolders = filterSubfoldersForCurrentLevel(serverSubfolders);

                runOnUiThread(() -> {
                    Set<String> uniqueSubfolders = new HashSet<>(subfolders);
                    uniqueSubfolders.addAll(filteredSubfolders);
                    subfolders.clear();
                    subfolders.addAll(uniqueSubfolders);

//                    subfolders.addAll(filteredSubfolders);
                    updateUI();
                    saveFolderData(folderName);
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception fetching subfolders", e);
            }
        }).start();
    }

//    private List<String> filterSubfoldersForCurrentLevel(List<String> allSubfolders) {
//        List<String> filteredSubfolders = new ArrayList<>();
//
//        String currentPath = folderName;
//        if (currentPath == null || currentPath.isEmpty()) {
//            for (String subfolder : allSubfolders) {
//                if (!subfolder.contains("/")) {
//                    filteredSubfolders.add(subfolder);
//                }
//            }
//        } else {
//            String currentPathWithSlash = currentPath + "/";
//
//            for (String subfolder : allSubfolders) {
//                if (subfolder.startsWith(currentPathWithSlash)) {
//                    String remainingPath = subfolder.substring(currentPathWithSlash.length());
//
//                    if (!remainingPath.contains("/")) {
//                        filteredSubfolders.add(subfolder);
//                    }
//                }
//            }
//        }
//
//        Log.d(TAG, "Filtered subfolders for level '" + currentPath + "': " + filteredSubfolders.size() + " items");
//        return filteredSubfolders;
//    }


    private List<String> filterSubfoldersForCurrentLevel(List<String> allSubfolders) {
        List<String> filteredSubfolders = new ArrayList<>();

        String currentPath = folderName != null ? folderName : "";
        String currentPathWithSlash = currentPath.isEmpty() ? "" : currentPath + "/";

        for (String subfolder : allSubfolders) {
            if (!subfolder.startsWith(currentPathWithSlash)) continue;

            String remainingPath = subfolder.substring(currentPathWithSlash.length());

            if (!remainingPath.contains("/")) {
                filteredSubfolders.add(currentPathWithSlash + remainingPath);
            }
        }

        Log.d(TAG, "Filtered subfolders for level '" + currentPath + "': " + filteredSubfolders);
        return filteredSubfolders;
    }




    private void downloadAndSaveImage(String imageUrl, String folderId, List<String> serverImages) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            FileOutputStream outputStream = null;

            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                File dir = new File(getFilesDir(), "shared_" + folderId);
                if (!dir.exists()) dir.mkdirs();

                File imageFile = new File(dir, fileName);

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(imageUrl)
                        .get()
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "❌ Failed to download " + fileName + ": " + response.code());
                    return;
                }

                outputStream = new FileOutputStream(imageFile);
                outputStream.write(response.body().bytes());
                outputStream.flush();

                String localUri = Uri.fromFile(imageFile).toString();


                serverImages.add(localUri);


                runOnUiThread(() -> {
                    otherImages.clear();
                    otherImages.addAll(serverImages);
                    updateUI();
                    saveFolderData(folderName);
                });


                Log.d(TAG, "✅ Downloaded and saved: " + fileName);

            } catch (Exception e) {
                Log.e(TAG, "❌ Error downloading file via HTTP: " + imageUrl, e);
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }).start();
    }



    private void fetchAndDisplayFolderMembers() {
        fetchFolderMembers(folderId, members -> {
            if (members != null && !members.isEmpty()) {
                String text = "Shared with: " + String.join(", ", members);
//                folderMembersInfo.setText(text);
//                shareInfoBar.setVisibility(View.VISIBLE);

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

        OkHttpClient client = new OkHttpClient();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            List<String> members = new ArrayList<>();

            try {
                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Failed to fetch folder members. Code: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Could not load members", Toast.LENGTH_SHORT).show();
                        callback.accept(members);
                    });
                    return;
                }

                String responseBody = response.body().string();
                JSONArray jsonArray = new JSONArray(responseBody);

                for (int i = 0; i < jsonArray.length(); i++) {
                    String member = jsonArray.optString(i);
                    if (member != null && !member.trim().isEmpty()) {
                        members.add(member);
                    }
                }

                runOnUiThread(() -> callback.accept(members));

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception while fetching folder members", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Could not load members", Toast.LENGTH_SHORT).show();
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }


    private void deleteFolderFromServer(String folderId) {
        String url = "http://transferly.go.ro:8080/api/shared/delete/" + folderId + "?username=" + currentUser;

        OkHttpClient client = new OkHttpClient();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .delete()
                .build();

        new Thread(() -> {
            try {
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d("DELETE_FOLDER", "Server response: " + response.body().string());

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Folder deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    Log.e("DELETE_FOLDER", "❌ Failed with code: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(this, "❌ Failed to delete folder", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                Log.e("DELETE_FOLDER", "❌ Exception while deleting folder", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Failed to delete folder", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
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

        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject body = new JSONObject();
            body.put("member", username);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            new Thread(() -> {
                try {
                    okhttp3.Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Removed " + username, Toast.LENGTH_SHORT).show();
                            fetchAndDisplayFolderMembers();
                        });
                    } else {
                        Log.e(TAG, "❌ Failed to remove member. Code: " + response.code());
                        runOnUiThread(() ->
                                Toast.makeText(this, "Failed to remove " + username, Toast.LENGTH_SHORT).show()
                        );
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Exception while removing member", e);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error removing member", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating request JSON", e);
            Toast.makeText(this, "Error removing member", Toast.LENGTH_SHORT).show();
        }
    }


    private void getMyFriends(Consumer<List<String>> callback) {
        String url = "http://transferly.go.ro:8080/api/users/" + currentUser + "/friends";

        OkHttpClient client = new OkHttpClient();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            List<String> friends = new ArrayList<>();

            try {
                okhttp3.Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Failed to fetch friends. Code: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Could not load friends", Toast.LENGTH_SHORT).show();
                        callback.accept(friends);
                    });
                    return;
                }

                String responseBody = response.body().string();
                JSONArray jsonArray = new JSONArray(responseBody);

                for (int i = 0; i < jsonArray.length(); i++) {
                    String friend = jsonArray.optString(i);
                    if (friend != null && !friend.trim().isEmpty()) {
                        friends.add(friend);
                    }
                }

                runOnUiThread(() -> callback.accept(friends));

            } catch (Exception e) {
                Log.e(TAG, "❌ Exception while fetching friends", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Could not load friends", Toast.LENGTH_SHORT).show();
                    callback.accept(new ArrayList<>());
                });
            }
        }).start();
    }


    private void addFriendToFolderOnServer(String folderId, String friendUsername) {
        try {
            Long longFolderId = Long.parseLong(folderId);
            String url = "http://transferly.go.ro:8080/api/shared/" + longFolderId + "/requestAddMember";

            JSONObject body = new JSONObject();
            body.put("member", friendUsername);
            body.put("requester", currentUser);

            System.out.println("📤 Sending add member request:");
            System.out.println("   Member: " + friendUsername);
            System.out.println("   Requester: " + currentUser);
            System.out.println("   Folder: " + folderId);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            OkHttpClient client = new OkHttpClient();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            new Thread(() -> {
                try {
                    okhttp3.Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        System.out.println("✅ Server response: " + response.body().string());
                        runOnUiThread(() ->
                                Toast.makeText(this, "⏳ Request sent! Awaiting approvals.", Toast.LENGTH_SHORT).show());
                    } else {
                        System.err.println("❌ Failed to request add " + friendUsername + ": " + response.code());
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        System.err.println("Error body: " + errorBody);
                        runOnUiThread(() ->
                                Toast.makeText(this, "❌ Failed to send request", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Exception while requesting add " + friendUsername + ": " + e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Error adding friend", Toast.LENGTH_SHORT).show());
                }
            }).start();

        } catch (Exception e) {
            System.err.println("❌ Exception parsing folderId or building request: " + e.getMessage());
            e.printStackTrace();
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

        if (folderName != null) {
            loadFolderData(folderName);
            updateUI();
        }

        fetchFilesForSharedFolder(folderId);
        fetchAndDisplayFolderMembers();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }


    private void registerSubfolderInBackend(String subfolderName) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/subfolder";

        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        try {
            json.put("name", subfolderName);
            json.put("createdBy", currentUser);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error building subfolder JSON", e);
            return;
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build();

        new Thread(() -> {
            try {
                okhttp3.Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Subfolder registered: " + subfolderName);
                    runOnUiThread(this::notifyFolderUpdated);  // Notifică ceilalți
                } else {
                    Log.e(TAG, "❌ Failed to register subfolder: " + response.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Error registering subfolder", e);
            }
        }).start();
    }


}