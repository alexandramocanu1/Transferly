package com.example.transferly.activities;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.bumptech.glide.request.Request;
//import com.bumptech.glide.request.Request;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContract;
import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;
import com.example.transferly.adapters.ImagesAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;




public class UploadActivity extends AppCompatActivity implements ImagesAdapter.OnImageLongClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_SELECT_IMAGES = 102;
    private final List<Uri> selectedImages = new ArrayList<>();
    private ImagesAdapter imagesAdapter;
    private RecyclerView recyclerViewImages;
    private TextView uploadIntroText;
    private int currentPosition = -1;

    private FloatingActionButton fabUpload, fabAdd, fabGenerateLink;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // apelez checkPermissionsAndOpenGallery() la inceput pt a asigura ca are permisiuni
//        checkPermissionsAndOpenGallery();

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String loggedInUsername = prefs.getString("username", "guest");
        String email = prefs.getString("email", "");


        fabGenerateLink = findViewById(R.id.fabGenerateLink);
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        fabUpload = findViewById(R.id.fabUpload);
        fabAdd = findViewById(R.id.fabAdd);
        uploadIntroText = findViewById(R.id.uploadIntroText);
        ImageView reloadButton = findViewById(R.id.reloadButton);


        // init hide the reload button
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

            File dbFile = getApplicationContext().getDatabasePath("users.db");
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found!");
            } else {
                Log.d(TAG, "Database file exists.");
            }

        });

        // RecyclerView configuration
        imagesAdapter = new ImagesAdapter(this, selectedImages, this);
        recyclerViewImages.setLayoutManager(new GridLayoutManager(this, 3)); // 3 items per row
        recyclerViewImages.setAdapter(imagesAdapter);
        recyclerViewImages.setHasFixedSize(true); // Previne rearanjarea accidentala
        recyclerViewImages.setItemViewCacheSize(20);

        // spacing between grid items
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
                generateLink(); // generate link daca sunt imagini
            }
        });


        fabAdd.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        fabGenerateLink.setOnClickListener(v -> generateLink());


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



        // bara de sus A TELEFONULUI transparenta
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            // culoare uniforma pentru status bar si navigation bar
            int darkColor = Color.parseColor("#111A20");
            window.setStatusBarColor(darkColor); // bara de sus (notificari)
            window.setNavigationBarColor(darkColor); // bara de jos (navigatie)

            // pt layout fullscreen + transparent padding handling
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }


    }




    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PERMISSIONS);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSIONS);
            } else {
                openGallery();
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission denied to access photos", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGES);
    }


    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    if (currentPosition >= 0 && currentPosition < selectedImages.size()) {

                        // copiaza imaginea in cache sau storage
                        selectedImages.set(currentPosition, croppedUri);
                        imagesAdapter.notifyItemChanged(currentPosition);
                        saveImages();
                    }
                } else {
                    Exception error = result.getError();
                    Toast.makeText(this, "Eroare crop: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image selection from gallery
        if (requestCode == REQUEST_CODE_SELECT_IMAGES && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri imageUri = clipData.getItemAt(i).getUri();
                    selectedImages.add(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                selectedImages.add(imageUri);
            }

            imagesAdapter.notifyDataSetChanged();
            recyclerViewImages.setVisibility(View.VISIBLE);
            uploadIntroText.setVisibility(View.GONE);
            fabUpload.setImageResource(R.drawable.ic_generate_link);
            findViewById(R.id.reloadButton).setVisibility(View.VISIBLE);
            fabGenerateLink.setVisibility(View.VISIBLE);
            saveImages(); // persist
        }

        // Handle cropped image from FullImageActivity
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            String croppedUriStr = data.getStringExtra("croppedUri");
            int position = data.getIntExtra("position", -1);

            if (croppedUriStr != null && position >= 0 && position < selectedImages.size()) {
                Uri croppedUri = Uri.parse(croppedUriStr);
                selectedImages.set(position, croppedUri);
                imagesAdapter.notifyItemChanged(position);
                saveImages(); // update SharedPreferences
            }
        }
    }





    private void saveImages() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String loggedInUsername = userPrefs.getString("username", "guest");

        SharedPreferences prefs = getSharedPreferences("TransferlyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> imageUris = new HashSet<>();
        for (Uri uri : selectedImages) {
            imageUris.add(uri.toString());
        }

        editor.putStringSet("selectedImages_" + loggedInUsername, imageUris);
        editor.apply();
    }


    private void loadImages() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String loggedInUsername = prefs.getString("username", "guest");

        SharedPreferences imagePrefs = getSharedPreferences("TransferlyPrefs", MODE_PRIVATE);
        String key = "selectedImages_" + loggedInUsername;
        Set<String> savedImages = imagePrefs.getStringSet(key, new HashSet<>());

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
        Uri uri = selectedImages.get(position); // NU o mai ștergem!
        startCrop(uri, position); // trimitem imaginea la crop

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





    private void startCrop(Uri imageUri, int position) {
        currentPosition = position;

        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.fixAspectRatio = false;
        options.autoZoomEnabled = true;
        options.activityMenuIconColor = Color.WHITE;
        options.outputCompressFormat = Bitmap.CompressFormat.JPEG;
        options.outputCompressQuality = 90;

        CropImageContractOptions cropOptions = new CropImageContractOptions(imageUri, options);
        cropImageLauncher.launch(cropOptions);
    }

    



    private String getRealPathFromURI(Uri uri) {
        Log.d("UPLOAD_DEBUG", "Trying to resolve path for URI: " + uri);
        String filePath = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String id = DocumentsContract.getDocumentId(uri);
            Log.d("UPLOAD_DEBUG", "Document ID: " + id);

            if (id.contains(":")) {
                String[] split = id.split(":");
                String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    filePath = Environment.getExternalStorageDirectory() + "/" + split[1];
                } else {
                    Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};

                    filePath = getDataColumn(this, contentUri, selection, selectionArgs);
                }
            }
        } else {
            filePath = getDataColumn(this, uri, null, null);
        }

        Log.d("UPLOAD_DEBUG", "Resolved file path: " + filePath);
        return filePath;
    }


    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }



    private void generateLink() {
        if (selectedImages.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "Nu s-au selectat imagini", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            try {
                String folderName = "album_" + UUID.randomUUID().toString().substring(0, 8);
                String galleryLink = null;

                for (Uri uri : selectedImages) {
                    String filePath = getFilePathFromURI(this, uri);
                    if (filePath == null || filePath.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(this, "Eroare: nu s-a putut obtine calea imaginii!", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Aici primim linkul COMPLET (cu key și iv!) de la backend
                    String uploadedLink = uploadToServer(filePath, folderName);

                    if (uploadedLink == null) {
                        runOnUiThread(() -> Toast.makeText(this, "Eroare la incarcare!", Toast.LENGTH_LONG).show());
                        return;
                    }

                    galleryLink = uploadedLink; // retine ultimul link primit (toate vor avea același folder, key, iv)
                }

                if (galleryLink != null) {
                    String finalGalleryLink = galleryLink;
                    runOnUiThread(() -> showPopupWithLink(finalGalleryLink));
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare la generarea link-ului", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }




    private String uploadToNAS(String filePath, String folderName) {
        String server = "192.168.100.64";
        int port = 21;
        String user = "admin";
        String pass = "a7303450a8";

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            boolean login = ftpClient.login(user, pass);

            if (!login) {
                Log.e("FTP", "ERROR: Login failed! Check username/password.");
                return null;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Creeaza folderul pe FTP
            String remoteDir = "G/Transferly/" + folderName;
            ftpClient.makeDirectory(remoteDir);

            File file = new File(filePath);
            if (!file.exists()) {
                Log.e("FTP", "ERROR: File does not exist! Path: " + filePath);
                return null;
            }

            String remoteFile = remoteDir + "/" + file.getName();
            Log.d("FTP", "Uploading: " + filePath + " -> " + remoteFile);

            FileInputStream inputStream = new FileInputStream(file);
            boolean done = ftpClient.storeFile(remoteFile, inputStream);
            inputStream.close();
            ftpClient.logout();
            ftpClient.disconnect();

            if (done) {
                Log.d("FTP", "Upload successful: " + remoteFile);
                return remoteFile;
            } else {
                Log.e("FTP", "Upload failed for: " + filePath);
            }
        } catch (IOException ex) {
            Log.e("FTP", "FTP Exception: " + ex.getMessage());
        }
        return null;
    }



    // trimit key-ul si lista de imagini la server
    private void sendKeyAndPathsToServer(String key, List<String> imagePaths) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().build();

            JSONArray jsonArray = new JSONArray(imagePaths);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", key);
            jsonObject.put("imagePaths", jsonArray);

            RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url("http://192.168.100.52:8080/api/saveImages")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response: " + response.body().string());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // cu tot cu loading screen si popup cu link
    private void showPopupWithLink(String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.popup_generate_link, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Progress UI
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView progressText = view.findViewById(R.id.progressText);
        View loadingContainer = view.findViewById(R.id.loadingContainer);
        View linkContentContainer = view.findViewById(R.id.linkContentContainer);

        // Link UI
        EditText linkText = view.findViewById(R.id.linkText);
        ImageView copyButton = view.findViewById(R.id.copyButton);
        View closeButton = view.findViewById(R.id.closeButton);
        View shareButton = view.findViewById(R.id.shareButton);

        // Start progress animation
        new Thread(() -> {
            for (int i = 0; i <= 100; i++) {
                int progress = i;
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    progressText.setText("Generating link... " + progress + "%");
                });

                try {
                    Thread.sleep(15); // viteza de incarcare (mai mic = mai rapid)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Dupa ce progresul ajunge la 100%
            runOnUiThread(() -> {
                loadingContainer.setVisibility(View.GONE);
                linkContentContainer.setVisibility(View.VISIBLE);
                linkText.setText(link);
            });

        }).start();

        // Copy
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Shared Link", link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Link copied", Toast.LENGTH_SHORT).show();
        });

        // Share
        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, link);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // Close
        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }



    private String getFilePathFromURI(Context context, Uri uri) {
        try {
            File file = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return file.getAbsolutePath();

        } catch (IOException e) {
            Log.e("UPLOAD_DEBUG", "Failed to copy file from URI: " + e.getMessage());
            return null;
        }
    }


    private String uploadToServer(String filePath, String folderName) {
        //String serverUrl = "http://192.168.100.64:8080/api/upload";
        //String serverUrl = "http://transferly.sytes.net:8080/api/upload";
        //String serverUrl = "http://192.168.0.220:8080/api/upload";
        String serverUrl = "http://transferly.go.ro:8080/api/upload"; // endpoint u pt IP public




        OkHttpClient client = new OkHttpClient();
        File file = new File(filePath);

        if (!file.exists()) {
            Log.e("UPLOAD_DEBUG", " ERROR: File does not exist: " + filePath);
            return null;
        }

        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("image/*"), file))
                    .addFormDataPart("folder", folderName) // trimite folderul la server
                    .build();

            Request request = new Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                Log.e("UPLOAD_DEBUG", " ERROR: Server response: " + response.code() + " -> " + response.body().string());
                return null;
            }

            //  Returneaza link-ul HTTP al imaginii
            String httpFileLink = response.body().string().trim();
            Log.d("UPLOAD_DEBUG", " File uploaded successfully: " + httpFileLink);
            return httpFileLink;

        } catch (Exception e) {
            Log.e("UPLOAD_DEBUG", " ERROR: Exception during upload: " + e.getMessage());
            return null;
        }
    }



}
