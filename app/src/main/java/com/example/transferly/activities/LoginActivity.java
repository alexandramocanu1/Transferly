package com.example.transferly.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.transferly.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String SERVER_URL = "http://transferly.go.ro:8080/api/users"; // IP-ul serverului

    private GoogleSignInClient mGoogleSignInClient;
    private EditText emailInput, passwordInput;
    private Button loginButton, googleSignInButton;

    // Activity Result Launcher pt Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign-In ResultCode: " + result.getResultCode());

                if (result.getData() == null) {
                    Log.e(TAG, " Google Sign-In data is null");
                    Toast.makeText(this, "Google Sign-In failed: data is null", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);

                    if (account != null) {
                        String email = account.getEmail();
                        String token = account.getIdToken();

                        Log.d(TAG, " Google Sign-In Success");
                        Log.d(TAG, " Email: " + email);
                        Log.d(TAG, " ID Token: " + token);

                        Toast.makeText(this, "Signed in as: " + email, Toast.LENGTH_SHORT).show();

                        registerUserOnServer(email, "google_dummy_password", true);
                        startActivity(new Intent(this, UploadActivity.class));
                        finish();

                    } else {
                        Log.e(TAG, " GoogleSignInAccount is null!");
                        Toast.makeText(this, "Google account is null", Toast.LENGTH_SHORT).show();
                    }

                } catch (ApiException e) {
                    Log.e(TAG, " Google Sign-In failed: " + e.getStatusCode(), e);
                    Toast.makeText(this, "Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, " Unknown exception: " + e.getMessage(), e);
                    Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        TextView registerLink = findViewById(R.id.registerLink);
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });



        // configurare Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        loginButton.setOnClickListener(v -> loginUser());

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    // cand o merge baza de date n o mai fi comentat
    private void loginUser() {
        String identifier = emailInput.getText().toString().trim(); // email sau username
        String password = passwordInput.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("username", identifier) // save `username` || `email` in SharedPreferences
                .putString("email", identifier) // Salvează `email`
                .apply();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject loginJson = new JSONObject();
                loginJson.put("identifier", identifier);
                loginJson.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(loginJson.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, UploadActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Server error!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }




//    // pt cand nu merge server u, login direct
//    private void loginUser() {
//        // Ignora verificarea serverului si trece direct la UploadActivity
//        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
//        startActivity(intent);
//        finish();
//    }


// de revenit, pb cu asta
    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In...");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void registerUserOnServer(String email, String password, boolean isGoogleUser) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL + "/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject userJson = new JSONObject();
                userJson.put("username", email);
                userJson.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(userJson.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Log.d(TAG, "User registered on server: " + email);
                } else {
                    Log.e(TAG, "Failed to register user on server.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
