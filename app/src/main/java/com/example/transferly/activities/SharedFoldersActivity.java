package com.example.transferly.activities;

import static com.example.transferly.activities.FolderDetailActivity.PREFS_NAME;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SharedFoldersActivity extends AppCompatActivity {
    private TextView sharedFoldersTitle;
    private RecyclerView foldersRecyclerView;
    private FolderAdapter adapter;
    private Map<String, String> folderNames;           // Maps folderId to folderName
    private Map<String, List<String>> folderStructure;
    private static final int MAX_SUBFOLDERS = 5;

    private ImageButton deleteFoldersButton;
    private List<String> selectedFolders = new ArrayList<>();
    private List<String> orderedFolderIds = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_folders);

        sharedFoldersTitle = findViewById(R.id.sharedFoldersTitle);
        foldersRecyclerView = findViewById(R.id.foldersRecyclerView);
        ImageButton addFolderButton = findViewById(R.id.addFolderButton);

        loadMainFolders();
        updateFoldersCount();
        setupRecyclerView();

        addFolderButton.setOnClickListener(view -> showAddFolderDialog(null));
        setupBottomNavigation();

        deleteFoldersButton = findViewById(R.id.deleteFoldersButton);
        deleteFoldersButton.setOnClickListener(v -> confirmDeleteSelectedFolders());


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            int navColor = Color.parseColor("#111A20"); // ac culoare ca bara

            window.setNavigationBarColor(navColor);
            window.setStatusBarColor(navColor);

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }


    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_shared_folders);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_shared_folders) {
                // Deja aici, nu face nimic
                return true;
            }

            return false;
        });
    }


    private void setupRecyclerView() {
        if (folderStructure == null) {
            folderStructure = new HashMap<>();
        }
        foldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
//        List<String> displayFolders = new ArrayList<>(folderNames.values());
//        adapter = new FolderAdapter(this, new ArrayList<>(folderNames.values()), selectedFolders,
        List<String> displayFolders = new ArrayList<>();
        for (String id : orderedFolderIds) {
            String name = folderNames.get(id);
            if (name != null) displayFolders.add(name);
        }

        adapter = new FolderAdapter(this, displayFolders, selectedFolders,

                this::onFolderClick, this::onFolderLongClick);
        foldersRecyclerView.setAdapter(adapter);

    // activate drag and drop
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END,
                0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                // mut in adapter (nume)
                Collections.swap(adapter.getFolders(), from, to);

                // mut in lista de ID-uri (care se salveaza)
                Collections.swap(orderedFolderIds, from, to);

                // actalizare vizual
                adapter.notifyItemMoved(from, to);

                // salvez dupa fiecare mutare
                saveMainFolders();


                return true;
            }


            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        touchHelper.attachToRecyclerView(foldersRecyclerView);

    }

    private void updateFoldersCount() {
        sharedFoldersTitle.setText("Shared Folders (" + folderStructure.size() + ")");
    }

    private void showAddFolderDialog(String parentFolder) {
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

            String folderId = UUID.randomUUID().toString();
            folderNames.put(folderId, folderName);

            orderedFolderIds.add(folderId);

            if (!folderStructure.containsKey(folderId)) {
                folderStructure.put(folderId, new ArrayList<>());
            }


            if (parentFolder != null) {
                folderStructure.putIfAbsent(parentFolder, new ArrayList<>());
                List<String> subfolders = folderStructure.get(parentFolder);
                if (subfolders.size() >= MAX_SUBFOLDERS) {
                    Toast.makeText(this, "Maximum of 5 subfolders reached", Toast.LENGTH_SHORT).show();
                    return;
                }
                subfolders.add(folderId);
            }

            saveMainFolders();
            setupRecyclerView(); // Update UI
            updateFoldersCount();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openFolder(String folderName) {
        String folderId = null;
        for (Map.Entry<String, String> entry : folderNames.entrySet()) {
            if (entry.getValue().equals(folderName)) {
                folderId = entry.getKey();
                break;
            }
        }

        if (folderId != null) {
            Intent intent = new Intent(this, FolderDetailActivity.class);
            intent.putExtra("FOLDER_ID", folderId);
            intent.putExtra("FOLDER_NAME", folderName);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMainFolders() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String foldersJson = sharedPreferences.getString("MAIN_FOLDERS", null);
        String namesJson = sharedPreferences.getString("FOLDER_NAMES", null);

        if (foldersJson != null) {
            folderStructure = gson.fromJson(foldersJson, new TypeToken<Map<String, List<String>>>() {}.getType());
        } else {
            folderStructure = new HashMap<>();
        }

        if (namesJson != null) {
            folderNames = gson.fromJson(namesJson, new TypeToken<Map<String, String>>() {}.getType());
        } else {
            folderNames = new HashMap<>();
        }

        String orderJson = sharedPreferences.getString("FOLDER_ORDER", null);
        if (orderJson != null) {
            orderedFolderIds = gson.fromJson(orderJson, new TypeToken<List<String>>() {}.getType());
        } else {
            // dc nu exista, init cu toate ID-urile
            orderedFolderIds = new ArrayList<>(folderNames.keySet());
        }

    }

    private void saveMainFolders() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        editor.putString("MAIN_FOLDERS", gson.toJson(folderStructure));
        editor.putString("FOLDER_NAMES", gson.toJson(folderNames));
        editor.putString("FOLDER_ORDER", gson.toJson(orderedFolderIds));
        editor.apply();
    }


    private void onFolderClick(String folderName) {
//        if (!selectedFolders.isEmpty()) {
//            toggleSelection(folderName);
//        } else {
            openFolder(folderName);
        //}
    }

    private void onFolderLongClick(String folderName) {
//        toggleSelection(folderName);
    }

// asta era pt selectare folder
    private void toggleSelection(String folderName) {
        if (selectedFolders.contains(folderName)) {
            selectedFolders.remove(folderName);
        } else {
            selectedFolders.add(folderName);
        }
        adapter.notifyDataSetChanged();
        deleteFoldersButton.setVisibility(selectedFolders.isEmpty() ? View.GONE : View.VISIBLE);
    }


    private void confirmDeleteSelectedFolders() {
        StringBuilder message = new StringBuilder("Are you sure you want to delete:\n");
        for (String name : selectedFolders) {
            message.append("• ").append(name).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete folders")
                .setMessage(message.toString())
                .setPositiveButton("Yes", (dialog, which) -> {
                    for (String name : selectedFolders) {
                        String folderIdToRemove = null;
                        for (Map.Entry<String, String> entry : folderNames.entrySet()) {
                            if (entry.getValue().equals(name)) {
                                folderIdToRemove = entry.getKey();
                                break;
                            }
                        }
                        if (folderIdToRemove != null) {
                            folderNames.remove(folderIdToRemove);
                            folderStructure.remove(folderIdToRemove);
                        }
                    }
                    selectedFolders.clear();
                    deleteFoldersButton.setVisibility(View.GONE);
                    saveMainFolders();
                    setupRecyclerView();
                    updateFoldersCount();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
