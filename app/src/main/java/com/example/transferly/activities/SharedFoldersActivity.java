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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.transferly.R;
import com.example.transferly.adapters.FolderAdapter;
import com.example.transferly.adapters.FriendSelectAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.android.volley.toolbox.StringRequest;

import androidx.annotation.Nullable;

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

    private ImageView trashIcon;
    private ItemTouchHelper touchHelper;

    private long backPressedTime = 0;
    private Toast backToast;

    // ✅ Track pending requests to avoid duplicates
    private Map<String, List<String>> checkedPendingRequests = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_folders);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    if (backToast != null) backToast.cancel();
                    finishAffinity();
                } else {
                    backToast = Toast.makeText(SharedFoldersActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });

        folderStructure = new HashMap<>();
        folderNames = new HashMap<>();
        orderedFolderIds = new ArrayList<>();

        sharedFoldersTitle = findViewById(R.id.sharedFoldersTitle);
        foldersRecyclerView = findViewById(R.id.foldersRecyclerView);
        ImageButton addFolderButton = findViewById(R.id.addFolderButton);

        trashIcon = findViewById(R.id.trashIcon);
        trashIcon.setVisibility(View.GONE);

        loadFoldersFromServer();
        updateFoldersCount();
        setupRecyclerView();

        addFolderButton.setOnClickListener(view -> showAddSharedFolderDialog());
        setupBottomNavigation();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            int navColor = Color.parseColor("#111A20");
            window.setNavigationBarColor(navColor);
            window.setStatusBarColor(navColor);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        ImageView profileIcon = findViewById(R.id.profileIcon);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String profilePicPath = prefs.getString("profile_pic", null);

        if (profilePicPath != null) {
            Glide.with(this).load(profilePicPath).into(profileIcon);
        } else {
            profileIcon.setImageResource(R.drawable.ic_default_profile);
        }

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(SharedFoldersActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
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
                return true;
            }

            return false;
        });
    }

    private void setupRecyclerView() {
        foldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        List<String> displayFolders = new ArrayList<>();
        for (String id : orderedFolderIds) {
            String name = folderNames.get(id);
            if (name != null) displayFolders.add(name);
        }

        adapter = new FolderAdapter(this, displayFolders, selectedFolders,
                this::onFolderClick, this::onFolderLongClick,
                viewHolder -> touchHelper.startDrag(viewHolder)
        );

        foldersRecyclerView.setAdapter(adapter);
        updateFoldersCount();

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(adapter.getFolders(), from, to);
                Collections.swap(orderedFolderIds, from, to);
                adapter.notifyItemMoved(from, to);
                saveMainFolders();
                return true;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                View itemView = viewHolder.itemView;
                int[] trashPos = new int[2];
                trashIcon.getLocationOnScreen(trashPos);
                int[] itemPos = new int[2];
                itemView.getLocationOnScreen(itemPos);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Required method, but not used
            }
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
            setupRecyclerView();
            updateFoldersCount();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void openFolder(String folderName) {
        String folderId = null;

        if (!folderNames.containsValue(folderName)) {
            Toast.makeText(this, "Folder doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Map.Entry<String, String> entry : folderNames.entrySet()) {
            if (entry.getValue().equals(folderName)) {
                folderId = entry.getKey();
                break;
            }
        }

        if (folderId != null) {
            final String finalFolderId = folderId;

            String url = "http://transferly.go.ro:8080/api/secret/reconstruct/" + finalFolderId + "?k=2";
            Log.d("RECONSTRUCT", "Requesting secret for folder ID = " + finalFolderId);

            JsonObjectRequest keyRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        String hexKey = response.optString("secret", null);

                        if (hexKey != null && !hexKey.isEmpty()) {
                            SharedPreferences prefs = getSharedPreferences("crypto_prefs", MODE_PRIVATE);
                            prefs.edit().putString("KEY_" + finalFolderId, hexKey).apply();

                            Intent intent = new Intent(this, FolderDetailActivity.class);
                            intent.putExtra("FOLDER_ID", finalFolderId);
                            intent.putExtra("FOLDER_NAME", folderName);
                            startActivity(intent);
                        } else {
                            String errorMsg = response.optString("message", "Could not retrieve key");
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "❌ Failed to reconstruct key", Toast.LENGTH_SHORT).show();
                    });

            Volley.newRequestQueue(this).add(keyRequest);
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
        openFolder(folderName);
    }

    private void onFolderLongClick(String folderName) {
        // Optional: Add context menu here
    }

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
                            orderedFolderIds.remove(folderIdToRemove);
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

    private void createSharedFolderOnServer(String folderName, List<String> memberUsernames) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String creator = prefs.getString("username", "guest");

        Map<String, Object> data = new HashMap<>();
        data.put("creator", creator);
        data.put("folderName", folderName);
        data.put("members", memberUsernames);

        String url = "http://transferly.go.ro:8080/api/shared/create";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(data),
                response -> {
                    Toast.makeText(this, "Shared folder created!", Toast.LENGTH_SHORT).show();
                    loadFoldersFromServer();
                },
                error -> {
                    Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void loadFoldersFromServer() {
        folderStructure.clear();
        folderNames.clear();
        orderedFolderIds.clear();

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "guest");
        String url = "http://transferly.go.ro:8080/api/shared/" + username;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject folderJson = response.optJSONObject(i);
                        if (folderJson != null) {
                            String name = folderJson.optString("folderName");
                            String id = String.valueOf(folderJson.optLong("id"));

                            folderNames.put(id, name);
                            if (!orderedFolderIds.contains(id)) {
                                orderedFolderIds.add(id);
                            }
                            folderStructure.put(id, new ArrayList<>());
                        }
                    }

                    saveMainFolders();
                    setupRecyclerView();
                    updateFoldersCount();

                    // ✅ Check pending requests AFTER folders are loaded
                    checkPendingRequests();
                },
                error -> {
                    Toast.makeText(this, "Failed to load folders", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void showAddSharedFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_shared_folder, null);
        builder.setView(dialogView);

        EditText folderNameInput = dialogView.findViewById(R.id.folderNameInput);
        RecyclerView friendsRecyclerView = dialogView.findViewById(R.id.friendsRecyclerView);
        friendsRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        getMyFriends(allFriends -> {
            List<String> selectedFriends = new ArrayList<>();

            FriendSelectAdapter adapter = new FriendSelectAdapter(allFriends, friendUsername -> {
                if (selectedFriends.contains(friendUsername)) {
                    selectedFriends.remove(friendUsername);
                } else {
                    selectedFriends.add(friendUsername);
                }
            });

            friendsRecyclerView.setAdapter(adapter);

            builder.setTitle("Create Shared Folder")
                    .setPositiveButton("Create", (dialog, which) -> {
                        String folderName = folderNameInput.getText().toString().trim();
                        if (folderName.isEmpty()) {
                            Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.d("SHARED_FOLDER_CREATE", "Selected friends: " + selectedFriends);
                        createSharedFolderOnServer(folderName, selectedFriends);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            builder.show();
        });
    }

    private void getMyFriends(Consumer<List<String>> callback) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "guest");
        String url = "http://transferly.go.ro:8080/api/users/" + username + "/friends";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> friends = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        friends.add(response.optString(i));
                    }
                    callback.accept(friends);
                },
                error -> {
                    Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show();
                    callback.accept(Collections.emptyList());
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void confirmFolderDeleteByName(String folderName) {
        String folderId = null;
        for (Map.Entry<String, String> entry : folderNames.entrySet()) {
            if (entry.getValue().equals(folderName)) {
                folderId = entry.getKey();
                break;
            }
        }

        if (folderId == null) return;

        final String finalFolderId = folderId;

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUser = prefs.getString("username", "guest");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete folder")
                .setMessage("Are you sure you want to delete '" + folderName + "'?")
                .setPositiveButton("Yes", (dialog, which) -> {

                    String url = "http://transferly.go.ro:8080/api/shared/delete/" + finalFolderId + "?username=" + currentUser;

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                            response -> {
                                Log.d("DELETE_FOLDER", "Response: " + response);
                                folderNames.remove(finalFolderId);
                                folderStructure.remove(finalFolderId);
                                orderedFolderIds.remove(finalFolderId);
                                saveMainFolders();
                                setupRecyclerView();
                                updateFoldersCount();
                                Toast.makeText(this, "✅ Folder deleted", Toast.LENGTH_SHORT).show();
                            },
                            error -> {
                                if (error.networkResponse == null) {
                                    Log.w("DELETE_FAIL", "No response received, assuming deletion worked.");
                                    folderNames.remove(finalFolderId);
                                    folderStructure.remove(finalFolderId);
                                    orderedFolderIds.remove(finalFolderId);
                                    saveMainFolders();
                                    setupRecyclerView();
                                    updateFoldersCount();
                                    Toast.makeText(this, "⚠️ No response, but folder likely deleted", Toast.LENGTH_SHORT).show();
                                } else {
                                    int statusCode = error.networkResponse.statusCode;
                                    String responseText = new String(error.networkResponse.data);
                                    Log.e("DELETE_FAIL", "Status Code: " + statusCode);
                                    Log.e("DELETE_FAIL", "Response: " + responseText);
                                    Toast.makeText(this, "❌ Failed to delete folder", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );

                    Volley.newRequestQueue(this).add(request);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void checkPendingRequests() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUser = prefs.getString("username", "guest");

        Log.d("PENDING_CHECK", "Checking pending requests for user: " + currentUser);
        Log.d("PENDING_CHECK", "Number of folders to check: " + orderedFolderIds.size());

        for (String folderId : orderedFolderIds) {
            String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/pendingRequests";
            Log.d("PENDING_CHECK", "Checking folder ID: " + folderId);

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        Log.d("PENDING_CHECK", "Response for folder " + folderId + ": " + response.toString());

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject requestObj = response.optJSONObject(i);
                            if (requestObj != null) {
                                String requestedUser = requestObj.optString("requestedUser");
                                String folderName = folderNames.get(folderId);
                                JSONArray approvalsArray = requestObj.optJSONArray("approvals");

                                // ✅ Extrage aprobările existente
                                List<String> approvals = new ArrayList<>();
                                if (approvalsArray != null) {
                                    for (int j = 0; j < approvalsArray.length(); j++) {
                                        approvals.add(approvalsArray.optString(j));
                                    }
                                }

                                // ✅ Verifică dacă userul curent a aprobat deja
                                if (approvals.contains(currentUser)) {
                                    Log.d("PENDING_CHECK", "User " + currentUser + " already approved request for " + requestedUser);
                                    continue;
                                }

                                // ✅ Create unique key to avoid duplicate prompts
                                String requestKey = folderId + "_" + requestedUser;

                                if (!checkedPendingRequests.containsKey(requestKey)) {
                                    checkedPendingRequests.put(requestKey, new ArrayList<>());
                                    Log.d("PENDING_APPROVAL", "Showing approval dialog for: " + requestedUser + " in folder: " + folderName);
                                    showApprovalDialog(folderId, folderName, requestedUser, currentUser, requestKey);
                                } else {
                                    Log.d("PENDING_APPROVAL", "Request already processed: " + requestKey);
                                }
                            }
                        }
                    },
                    error -> {
                        Log.e("PENDING_REQUESTS", "Failed to load pending requests for folder " + folderId + ": " + error.toString());
                    });

            Volley.newRequestQueue(this).add(request);
        }
    }


    private void showApprovalDialog(String folderId, String folderName, String requestedUser, String currentUser, String requestKey) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🔐 Folder Join Request")
                    .setMessage("👤 " + requestedUser + " wants to join the folder:\n📁 '" + folderName + "'\n\n✅ Do you approve this request?")
                    .setPositiveButton("✅ Approve", (dialog, which) -> {
                        Log.d("APPROVAL", "User " + currentUser + " approved " + requestedUser + " for folder " + folderId);
                        approveUserRequest(folderId, requestedUser, currentUser, requestKey);
                    })
                    .setNegativeButton("Deny", (dialog, which) -> {
                        Log.d("APPROVAL", "User " + currentUser + " denied " + requestedUser + " for folder " + folderId);
                        rejectJoinRequestOnServer(folderId, requestedUser);
                        checkedPendingRequests.put(requestKey, List.of("DENIED"));
                        Toast.makeText(this, "Request denied", Toast.LENGTH_SHORT).show();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    private void approveUserRequest(String folderId, String requestedUser, String currentUser, String requestKey) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/approveAddMember";

        Map<String, String> data = new HashMap<>();
        data.put("member", requestedUser);
        data.put("approver", currentUser);

        Log.d("APPROVAL_REQUEST", "Sending approval: " + data.toString());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(data),
                response -> {
                    Log.d("APPROVAL_RESPONSE", "Response: " + response.toString());

                    boolean success = response.optBoolean("success", false);
                    String message = response.optString("message", "Unknown response");

                    checkedPendingRequests.put(requestKey, List.of(success ? "APPROVED" : "RECORDED"));

                    if (success) {
                        Toast.makeText(this, "✅ " + message, Toast.LENGTH_SHORT).show();
                        if (message.contains("Member added")) {
                            loadFoldersFromServer(); // 🔁 refresh dacă membrul a fost adăugat
                        }
                    } else {
                        Toast.makeText(this, "⚠️ Approval recorded, waiting for others", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("APPROVAL_FAIL", "Error approving: " + error.toString());
                    Toast.makeText(this, "❌ Error sending approval", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(request);
    }


    private void rejectJoinRequestOnServer(String folderId, String requestedUser) {
        String url = "http://transferly.go.ro:8080/api/shared/" + folderId + "/rejectRequest/" + requestedUser;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                response -> Log.d("REJECT_REQUEST", "Successfully rejected: " + requestedUser),
                error -> Log.e("REJECT_REQUEST", "Error rejecting request: " + error.toString())
        );

        Volley.newRequestQueue(this).add(request);
    }


}
