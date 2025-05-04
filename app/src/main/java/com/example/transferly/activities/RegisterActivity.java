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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RegisterActivity extends AppCompatActivity {

    //    private static final String SERVER_URL = "http://192.168.100.64:8080/api/users/register";
    private static final String SERVER_URL = "http://transferly.go.ro:8080/api/users/register";


    private Button registerButton;
    private boolean isRequestInProgress = false; // Previne cereri multiple
    private EditText usernameInput, emailInput, passwordInput;

    private GoogleSignInClient mGoogleSignInClient;
    private Button googleRegisterButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_register);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Init toate componentele din layout
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            if (!isRequestInProgress) {
                registerUser();
            }
        });

        // Link spre pagina de login
        TextView loginLink = findViewById(R.id.loginLink);
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });


        googleRegisterButton = findViewById(R.id.googleRegisterButton);

// Configurare Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });


// Buton Google Register
        googleRegisterButton.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

    }


    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) {
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);

                    if (account != null) {
                        String email = account.getEmail();
                        String token = account.getIdToken();
                        String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

                        // ✅ Salvează utilizatorul în SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        prefs.edit()
                                .putString("email", email)
                                .putString("username", username)
                                .apply();

                        Log.d("RegisterActivity", "Google Sign-In success: " + email);
                        Toast.makeText(this, "Registering with Google: " + email, Toast.LENGTH_SHORT).show();

                        // Înregistrare în backend
                        registerGoogleUser(email);
                    } else {
                        Toast.makeText(this, "Google account is null", Toast.LENGTH_SHORT).show();
                    }

                } catch (ApiException e) {
                    Log.e("RegisterActivity", "Google Sign-In failed", e);
                    Toast.makeText(this, "Google Sign-In error", Toast.LENGTH_SHORT).show();
                }
            });

    private void registerGoogleUser(String email) {
        new Thread(() -> {
            try {
                URL url = new URL("http://transferly.go.ro:8080/api/users/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Creează username-ul din email (ex: "john.doe@gmail.com" -> "john.doe")
                String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;

                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("email", email);
                json.put("password", "google_dummy_password");

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()
                ));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                Log.d("RegisterActivity", "Server response code: " + responseCode);
                Log.d("RegisterActivity", "Server response: " + response);

                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Registered with Google!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, UploadActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed: " + response.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e("RegisterActivity", "Register Google user failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }



    private void registerUser() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();


        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            Log.e("RegisterActivity", "Fields are empty!");
            return;
        }


        isRequestInProgress = true; // Blocheaza alte request-uri
        registerButton.setEnabled(false); // Dezactiveaza butonul pt nu nu spam

        new Thread(() -> {
            try {
                Log.e("RegisterActivity", "Starting registration request...");

                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000); // Timeout 5 sec
                conn.setReadTimeout(5000); // Timeout pent raspuns
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("email", email);
                json.put("password", password);


                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.e("RegisterActivity", "Server Response Code: " + responseCode);

                InputStream inputStream = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.e("RegisterActivity", "Server Response: " + response.toString());

                runOnUiThread(() -> {
                    isRequestInProgress = false; // Permite noi cereri
                    registerButton.setEnabled(true); // Re-activeaza butonul

                    if (responseCode == 200) {
                        Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + response.toString(), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("RegisterActivity", "Exception: " + e.getMessage());
                runOnUiThread(() -> {
                    isRequestInProgress = false;
                    registerButton.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Server error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
