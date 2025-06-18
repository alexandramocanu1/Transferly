package com.example.transferly.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.transferly.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.transferly.adapters.FriendRequestAdapter;

import java.util.ArrayList;
import java.util.List;


public class FriendsActivity extends AppCompatActivity {

    private EditText searchUsername;
    private Button searchButton;
    private com.google.android.material.floatingactionbutton.FloatingActionButton addFriendButton;

    private String loggedInUsername = "guest";

    private RequestQueue requestQueue;

    private long backPressedTime = 0;
    private Toast backToast;

    private RecyclerView friendRequestsRecycler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        requestQueue = Volley.newRequestQueue(this);


        friendRequestsRecycler = findViewById(R.id.friendRequestsRecycler);
        friendRequestsRecycler.setLayoutManager(new LinearLayoutManager(this));


        loadFriendRequestsRecycler();


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    if (backToast != null) backToast.cancel();
                    finishAffinity(); // iese din aplicație
                } else {
                    backToast = Toast.makeText(FriendsActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        loggedInUsername = prefs.getString("username", "guest");


        // init componentelor UI
        searchUsername = findViewById(R.id.searchUsername);
        searchButton = findViewById(R.id.searchButton);
        addFriendButton = findViewById(R.id.addFriendButton);


        addFriendButton.setOnClickListener(v -> {
            if (searchUsername.getVisibility() == View.GONE) {
                searchUsername.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.VISIBLE);
                searchUsername.requestFocus();
            } else {
                String friendUsername = searchUsername.getText().toString();
                if (!friendUsername.isEmpty()) {
                    sendFriendRequest(friendUsername);
                } else {
                    Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                }
            }
        });



        // config barei de navigare
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_friends);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_friends) {
                return true;
            } else if (item.getItemId() == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_shared_folders) {
                startActivity(new Intent(this, SharedFoldersActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // ad actiuni pt butoane
        searchButton.setOnClickListener(v -> {
            String username = searchUsername.getText().toString();
            if (!username.isEmpty()) {
                searchUser(username); // trimite mai departe verificarea
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            int darkColor = Color.parseColor("#111A20");

            window.setNavigationBarColor(darkColor); // bara jos sistem
            window.setStatusBarColor(darkColor);     // bara sus sistem // de pus si n folder page

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

//        loadPendingRequests();
//        loadFriendRequests();
//        loadFriendRequestsRecycler();

        getFriendsList();
        loadPendingFriendRequests();

        ImageView profileIcon = findViewById(R.id.profileIcon);


        Glide.with(this)
                .load(R.drawable.ic_default_profile)
                .circleCrop()
                .into(profileIcon);


        String profilePicPath = prefs.getString("profile_pic", null);

        if (profilePicPath != null) {
            Glide.with(this).load(profilePicPath).into(profileIcon);
        } else {
            profileIcon.setImageResource(R.drawable.ic_default_profile);
        }

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });



    }


    private void loadFriendRequestsRecycler() {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/requests";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<String> senderUsernames = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        senderUsernames.add(response.optString(i));
                    }

                    FriendRequestAdapter adapter = new FriendRequestAdapter(senderUsernames, this, loggedInUsername);
                    friendRequestsRecycler.setAdapter(adapter);
                },
                error -> Toast.makeText(this, "❌ Failed to load friend requests", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }


    private void searchUser(String username) {
        String url = "http://transferly.go.ro:8080/api/users/search?username=" + username;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String foundUser = response.getString("username");

                        if (!foundUser.equalsIgnoreCase(loggedInUsername)) {
                            isUserAlreadyInFriendsList(foundUser, () -> {
                                // 👉 Afișează cardul cu butonul "Send Request", dar nu trimite direct
                                addFriendRequestCard(foundUser, false);
                            });
                        } else {
                            Toast.makeText(this, "Nu poți adăuga propriul cont!", Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(this, "Invalid response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }


    private void loadPendingFriendRequests() {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/requests";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String senderUsername = response.getString(i);
                            addIncomingRequestCard(senderUsername);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "Failed to load friend requests", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }



    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }


    private void addIncomingRequestCard(String senderUsername) {
        LinearLayout container = findViewById(R.id.friendsListContainer);

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(params);

        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.friend_card_bg);
        card.setPadding(24, 24, 24, 24);

        TextView nameText = new TextView(this);
        nameText.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameText.setText(senderUsername);
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.white));
        nameText.setPadding(12, 0, 0, 0);

        Button acceptBtn = new Button(this);
        acceptBtn.setText("Accept");
        acceptBtn.setBackgroundTintList(getResources().getColorStateList(R.color.green));
        acceptBtn.setTextColor(getResources().getColor(android.R.color.white));
        acceptBtn.setOnClickListener(v -> {
            acceptFriend(senderUsername);
            container.removeView(card);
            getFriendsList(); // reîncarcă prietenii actuali
        });

        Button declineBtn = new Button(this);
        declineBtn.setText("Decline");
        declineBtn.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        declineBtn.setTextColor(getResources().getColor(android.R.color.white));
        declineBtn.setOnClickListener(v -> {
            declineFriend(senderUsername);
            container.removeView(card);
        });

        card.addView(nameText);
        card.addView(acceptBtn);
        card.addView(declineBtn);
        container.addView(card);
    }



    private void declineFriend(String senderUsername) {
        String url = "http://transferly.go.ro:8080/api/users/declineRequest?receiver=" + loggedInUsername + "&sender=" + senderUsername;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> Toast.makeText(this, "Declined " + senderUsername, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(this, "Failed to decline", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }



    private void isUserAlreadyInFriendsList(String username, Runnable onNotFriend) {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/friends";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    boolean isFriend = false;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            if (username.equals(response.getString(i))) {
                                isFriend = true;
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!isFriend) {
                        onNotFriend.run();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }




    private void sendFriendRequest(String receiverUsername) {
        String url = "http://transferly.go.ro:8080/api/users/sendRequest";
        JSONObject postData = new JSONObject();

        try {
            postData.put("sender", loggedInUsername); // Username of the logged-in user
            postData.put("receiver", receiverUsername); // Username of the user to send the request to
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSON_PAYLOAD", postData.toString());

        // Sending the POST request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    // Handle successful request
                    Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // Handle error response
                    String errorMessage = "Failed to send request";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            // Attempt to parse the error response
                            String jsonResponse = new String(error.networkResponse.data);
                            JSONObject jsonError = new JSONObject(jsonResponse);
                            errorMessage = jsonError.optString("message", errorMessage);
                        } catch (JSONException e) {
                            errorMessage = "Server error response is not in valid JSON format.";
                        }
                    } else {
                        errorMessage = "No network response or invalid format.";
                    }
                    Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("REQUEST_ERROR", "Error: " + error.getMessage());
                }
        );

        // Adding the request to the request queue
        requestQueue.add(request);
    }



    private void addFriendRequestCard(String username, boolean isFriend) {
        LinearLayout container = findViewById(R.id.friendsListContainer);

        Log.d("ADD_FRIEND_CARD", "Adding: " + username + " | isFriend=" + isFriend);


        for (int i = 0; i < container.getChildCount(); i++) {
            LinearLayout existingCard = (LinearLayout) container.getChildAt(i);
            TextView nameText = (TextView) existingCard.getChildAt(0);
            if (nameText != null && nameText.getText().toString().equals(username)) {
                return;
            }
        }

        LinearLayout card = new LinearLayout(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.friend_card_bg);
        card.setPadding(24, 24, 24, 24);

        TextView nameText = new TextView(this);
        nameText.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nameText.setText(username);
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.white));
        nameText.setPadding(12, 0, 0, 0);

        Button actionButton = new Button(this);
        actionButton.setText(isFriend ? "Share Folders" : "Send Request");
        actionButton.setBackgroundTintList(getResources().getColorStateList(R.color.purple));
        actionButton.setTextColor(getResources().getColor(android.R.color.white));

        if (isFriend) {
            actionButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, SharedFoldersActivity.class);
                intent.putExtra("friendUsername", username);
                startActivity(intent);
            });

            // LONG PRESS => apare DELETE
            card.setOnLongClickListener(v -> {
                showDeleteFriendButton(container, card, username);
                return true;
            });
        } else {
            actionButton.setOnClickListener(v -> sendFriendRequest(username));
        }

        card.addView(nameText);
        card.addView(actionButton);
        container.addView(card);
    }


    private void showDeleteFriendButton(LinearLayout container, LinearLayout card, String friendUsername) {
        // Evită adăugarea repetată a butonului
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                if ("Delete Friend".equals(btn.getText().toString())) {
                    return;
                }
            }
        }

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete Friend");
        deleteButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_dark));
        deleteButton.setTextColor(getResources().getColor(android.R.color.white));

        deleteButton.setOnClickListener(v -> {
            JSONObject postData = new JSONObject();
            try {
                postData.put("user1", loggedInUsername);
                postData.put("user2", friendUsername);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    "http://transferly.go.ro:8080/api/users/removeFriend", postData,
                    response -> {
                        Toast.makeText(this, "Removed " + friendUsername + " from friends", Toast.LENGTH_SHORT).show();
                        getFriendsList(); // 🔁 Reîncarcă lista de prieteni
                    },
                    error -> {
                        Toast.makeText(this, "Friend not found or already removed", Toast.LENGTH_SHORT).show();
                        getFriendsList(); // 🔄 Reîncarcă oricum, ca să curețe UI-ul
                    });

            requestQueue.add(request);
        });

        card.addView(deleteButton);
    }




    private void isUserActuallyFriend(String friendUsername, Runnable onValid) {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/friends";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String fetchedFriend = response.getString(i);
                            if (fetchedFriend.equals(friendUsername)) {
                                onValid.run(); // e prieten real, il add
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    // altfel, nu facem nimic
                },
                error -> Log.e("FRIEND_CHECK", "Failed to verify friend: " + friendUsername)
        );

        requestQueue.add(request);
    }




    private void getFriendsList() {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/friends";

        Log.d("FRIENDS_DEBUG", "Getting friends for: " + loggedInUsername);


        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    LinearLayout container = findViewById(R.id.friendsListContainer);
                    container.removeAllViews();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String friendUsername = response.getString(i);
                            runOnUiThread(() -> addFriendRequestCard(friendUsername, true));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "Failed to load friends", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }



    private void loadPendingRequests() {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/requests";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String senderUsername = response.getString(i);
                            addFriendRequestCard(senderUsername, false);  // Marcare cerere trm
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "No requests found", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }



    private void loadFriendRequests() {
        String url = "http://transferly.go.ro:8080/api/users/" + loggedInUsername + "/requests";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String senderUsername = response.getString(i);
                            addIncomingRequestCard(senderUsername);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "No requests found", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }

//    private void addIncomingRequestCard(String username) {
//        LinearLayout card = new LinearLayout(this);
//        card.setOrientation(LinearLayout.HORIZONTAL);
//        card.setBackgroundResource(R.drawable.friend_card_bg);
//        card.setPadding(24, 24, 24, 24);
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        layoutParams.setMargins(0, 0, 0, 24);
//        card.setLayoutParams(layoutParams);
//
//        TextView nameText = new TextView(this);
//        nameText.setLayoutParams(new LinearLayout.LayoutParams(0,
//                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
//        nameText.setText(username);
//        nameText.setTextSize(16);
//        nameText.setTextColor(getResources().getColor(android.R.color.white));
//        nameText.setPadding(12, 0, 0, 0);
//
//        Button actionButton = new Button(this);
//        actionButton.setText("Accept Request");
//        actionButton.setBackgroundTintList(getResources().getColorStateList(R.color.purple));
//        actionButton.setTextColor(getResources().getColor(android.R.color.white));
//
//        actionButton.setOnClickListener(v -> {
//            acceptFriend(username);
//
//            // trans butonul in Share Folders
//            actionButton.setText("Share Folders");
//            actionButton.setOnClickListener(folderClick -> {
//                Intent intent = new Intent(this, SharedFoldersActivity.class);
//                intent.putExtra("friendUsername", username);
//                startActivity(intent);
//            });
//        });
//
//        card.addView(nameText);
//        card.addView(actionButton);
//
//        LinearLayout container = findViewById(R.id.friendsListContainer);
//        container.addView(card);
//    }




    private void acceptFriend(String senderUsername) {
        String url = "http://transferly.go.ro:8080/api/users/acceptRequest";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(this, "Now friends with " + senderUsername, Toast.LENGTH_SHORT).show();
                    getFriendsList(); // Refresh list after accepting
                },
                error -> {
                    String errorMessage = "Failed to accept request";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMessage += ": " + new String(error.networkResponse.data);
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("ACCEPT_ERROR", errorMessage, error);
                }) {
            @Override
            public byte[] getBody() {
                JSONObject postData = new JSONObject();
                try {
                    postData.put("receiver", loggedInUsername);
                    postData.put("sender", senderUsername);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return postData.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        requestQueue.add(request);
    }


}
