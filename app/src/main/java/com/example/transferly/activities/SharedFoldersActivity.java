package com.example.transferly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class SharedFoldersActivity extends AppCompatActivity {
    private TextView sharedFoldersTitle;
    private RecyclerView foldersRecyclerView;
    private FolderAdapter adapter;
    private List<String> folders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_folders);

        sharedFoldersTitle = findViewById(R.id.sharedFoldersTitle);
        foldersRecyclerView = findViewById(R.id.foldersRecyclerView);
        ImageButton addFolderButton = findViewById(R.id.addFolderButton);

        // init lista de foldere
        folders = new ArrayList<>();

        updateFoldersCount(folders.size());
        setupRecyclerView();

        // Config butonul pentru a ad foldere
        addFolderButton.setOnClickListener(view -> showAddFolderDialog());

        // config bara de navigare
        setupBottomNavigation();
    }

    private void setupRecyclerView() {
        foldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new FolderAdapter(folders, this::openFolder);
        foldersRecyclerView.setAdapter(adapter);
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Folder");
        builder.setMessage("Enter the name of the folder:");

        // fol EditText pentru introducerea textului
        final EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setPadding(20, 20, 20, 20);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                folders.add(folderName);
                adapter.notifyDataSetChanged();
                updateFoldersCount(folders.size());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void updateFoldersCount(int count) {
        sharedFoldersTitle.setText("Shared Folders (" + count + ")");
    }

    private void openFolder(String folderName) {
        Intent intent = new Intent(this, FolderDetailActivity.class);
        intent.putExtra("FOLDER_NAME", folderName);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_shared_folders);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_shared_folders) {
                return true;
            }
            return false;
        });
    }
}
