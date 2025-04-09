package com.example.transferly.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.transferly.R;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
