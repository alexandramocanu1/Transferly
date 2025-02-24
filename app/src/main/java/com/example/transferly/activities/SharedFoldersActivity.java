package com.example.transferly.activities;

import static com.example.transferly.activities.FolderDetailActivity.PREFS_NAME;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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

        // Inițializează lista de foldere
        loadMainFolders();

        // Actualizează titlul și afișarea folderelor
        updateFoldersCount(folders.size());
        setupRecyclerView();

        // Configurează butonul pentru a adăuga foldere
        addFolderButton.setOnClickListener(view -> showAddFolderDialog());

        // Configurează bara de navigare
        setupBottomNavigation();
    }

    private void setupRecyclerView() {
        List<String> selectedFolders = new ArrayList<>(); // Inițializăm lista de foldere selectate

        foldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new FolderAdapter(
                this,
                folders,
                selectedFolders,
                folderName -> openFolder(folderName), // Click pe folder
                folderName -> {
                    // Apăsare lungă pentru selectare/deselectare
                    if (selectedFolders.contains(folderName)) {
                        selectedFolders.remove(folderName);
                    } else {
                        selectedFolders.add(folderName);
                    }
                    adapter.notifyDataSetChanged(); // Actualizăm vizual selecțiile
                }
        );
        foldersRecyclerView.setAdapter(adapter);
    }



    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Folder");
        builder.setMessage("Enter the name of the folder:");

        final EditText input = new EditText(this);
        input.setHint("Folder name");
        input.setPadding(20, 20, 20, 20);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (folderName.isEmpty()) {
                Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verific dc exista deja un folder cu ac nume
            if (folders.contains(folderName)) {
                Toast.makeText(this, "A folder with this name already exists", Toast.LENGTH_SHORT).show();
            } else {
                folders.add(folderName);
                adapter.notifyDataSetChanged();
                updateFoldersCount(folders.size());
                saveMainFolders(); // Salvăm folderele după adăugare
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void updateFoldersCount(int count) {
        sharedFoldersTitle.setText("Shared Folders (" + count + ")");
    }

    private void openFolder(String folderName) {
        if (folders.contains(folderName)) {
            Intent intent = new Intent(this, FolderDetailActivity.class);
            intent.putExtra("FOLDER_NAME", folderName);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
        }
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

    private static final String KEY_MAIN_FOLDERS = "MAIN_FOLDERS";

    private void saveMainFolders() {
        SharedPreferences sharedPreferences = getSharedPreferences(FolderDetailActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String foldersJson = gson.toJson(folders);
        editor.putString(KEY_MAIN_FOLDERS, foldersJson);
        editor.apply();
    }

    private void loadMainFolders() {
        SharedPreferences sharedPreferences = getSharedPreferences(FolderDetailActivity.PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String foldersJson = sharedPreferences.getString(KEY_MAIN_FOLDERS, null);
        if (foldersJson != null) {
            folders = gson.fromJson(foldersJson, new TypeToken<List<String>>() {}.getType());
        } else {
            folders = new ArrayList<>();
        }

        // Debugging
        if (folders.isEmpty()) {
            Toast.makeText(this, "No folders found", Toast.LENGTH_SHORT).show();
        }
    }
}
