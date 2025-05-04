package com.example.transferly.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.transferly.R;

import android.widget.EditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;



public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView usernameText, emailText;
    private Button logoutButton, changePicButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profileImage);
        usernameText = findViewById(R.id.profileUsername);
        emailText = findViewById(R.id.profileEmail);
        logoutButton = findViewById(R.id.logoutButton);
        changePicButton = findViewById(R.id.changePicButton);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "Unknown");
        String email = prefs.getString("email", "N/A");
        String profilePicPath = prefs.getString("profile_pic", null);

        usernameText.setText("Username: " + username);
        emailText.setText("Email: " + email);

        if (profilePicPath != null) {
            Glide.with(this).load(profilePicPath).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_default_profile);
        }

        logoutButton.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finishAffinity();
        });

        changePicButton.setOnClickListener(v -> {
            // Lansează intent pentru a selecta imagine nouă
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });


        Button editUsernameButton = findViewById(R.id.editUsernameButton);

        editUsernameButton.setOnClickListener(v -> {
//            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String currentUsername = prefs.getString("username", "");

            EditText input = new EditText(this);
            input.setText(currentUsername);
            input.setSelection(currentUsername.length());

            new AlertDialog.Builder(this)
                    .setTitle("Edit Username")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newUsername = input.getText().toString().trim();
                        if (!newUsername.isEmpty()) {
                            // ✅ Trimite request pentru a verifica dacă username-ul există
                            checkIfUsernameExists(newUsername, exists -> {
                                if (exists) {
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "Username already taken!", Toast.LENGTH_SHORT).show()
                                    );
                                } else {
                                    prefs.edit().putString("username", newUsername).apply();
                                    usernameText.setText("Username: " + newUsername);
                                    runOnUiThread(() ->
                                            Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            prefs.edit().putString("profile_pic", selectedImage.toString()).apply();
            Glide.with(this).load(selectedImage).into(profileImage);
        }
    }


    private void checkIfUsernameExists(String username, java.util.function.Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                URL url = new URL("http://transferly.go.ro:8080/api/users/exists/" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    boolean exists = jsonResponse.optBoolean("exists", false);
                    callback.accept(exists);
                } else {
                    callback.accept(false); // pp ca nu exista
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(false);
            }
        }).start();
    }

}
