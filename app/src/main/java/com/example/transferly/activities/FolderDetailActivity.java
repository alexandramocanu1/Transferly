package com.example.transferly.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.transferly.R;
import com.example.transferly.adapters.FolderImageAdapter;

import java.util.ArrayList;
import java.util.List;

public class FolderDetailActivity extends AppCompatActivity {

    private RecyclerView likedRecyclerView, othersRecyclerView, duplicateRecyclerView;
    private View emptyFolderLayout;
    private Button startNowButton;

    private List<String> likedImages, otherImages, duplicateImages, subfolders;

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
                    updateUI();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        TextView folderDetailTitle = findViewById(R.id.folderDetailTitle);
        likedRecyclerView = findViewById(R.id.likedRecyclerView);
        othersRecyclerView = findViewById(R.id.othersRecyclerView);
        duplicateRecyclerView = findViewById(R.id.duplicateRecyclerView);
        emptyFolderLayout = findViewById(R.id.emptyFolderLayout);
        startNowButton = findViewById(R.id.startNowButton);

        // numele folderului
        String folderName = getIntent().getStringExtra("FOLDER_NAME");
        folderDetailTitle.setText(folderName != null ? folderName : "No Folder Name");

        // nitializez listele
        likedImages = new ArrayList<>();
        otherImages = new ArrayList<>();
        duplicateImages = new ArrayList<>();
        subfolders = new ArrayList<>();

        updateUI();

        // config butonul "Start Now"
        startNowButton.setOnClickListener(v -> showOptions());
    }

    private void updateUI() {
        if (likedImages.isEmpty() && otherImages.isEmpty() && duplicateImages.isEmpty()) {
            // Folder gol
            emptyFolderLayout.setVisibility(View.VISIBLE);
            likedRecyclerView.setVisibility(View.GONE);
            othersRecyclerView.setVisibility(View.GONE);
            duplicateRecyclerView.setVisibility(View.GONE);
        } else {
            // Folder continut
            emptyFolderLayout.setVisibility(View.GONE);

            likedRecyclerView.setVisibility(likedImages.isEmpty() ? View.GONE : View.VISIBLE);
            othersRecyclerView.setVisibility(otherImages.isEmpty() ? View.GONE : View.VISIBLE);
            duplicateRecyclerView.setVisibility(duplicateImages.isEmpty() ? View.GONE : View.VISIBLE);

            setupRecyclerView(likedRecyclerView, likedImages);
            setupRecyclerView(othersRecyclerView, otherImages);
            setupRecyclerView(duplicateRecyclerView, duplicateImages);
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<String> images) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        FolderImageAdapter adapter = new FolderImageAdapter(this, images, position -> {
            Toast.makeText(this, "Liked image at position: " + position, Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(Intent.createChooser(intent, "Select Pictures"));
    }

    private void createSubfolder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Subfolder");

        final EditText input = new EditText(this);
        input.setHint("Enter subfolder name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String subfolderName = input.getText().toString().trim();
            if (!subfolderName.isEmpty()) {
                subfolders.add(subfolderName);
                Toast.makeText(this, "Subfolder created: " + subfolderName, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
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

}
