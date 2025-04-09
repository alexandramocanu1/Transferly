package com.example.transferly.db;

import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class UserDatabaseHelper {
    private static final String SERVER_URL = "http://transferly.go.ro:8080/api/users"; // ip public pe port "server"

    // inregistrare user pe server
    public boolean registerUser(String username, String email, String password) {
        try {
            URL url = new URL(SERVER_URL + "/register");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setInstanceFollowRedirects(false); // ⚠️ Adaugă asta aici
            conn.setDoOutput(true);

            JSONObject userJson = new JSONObject();
            userJson.put("username", username);
            userJson.put("email", email); // 🔥 Adaugă-l!
            userJson.put("password", password);

            OutputStream os = conn.getOutputStream();
            os.write(userJson.toString().getBytes());
            os.flush();
            os.close();

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            Log.e("UserDatabaseHelper", "Eroare la înregistrare: " + e.getMessage());
            return false;
        }
    }


    // validare autentificare user pe server
    public boolean loginUser(String username, String password) {
        try {
            URL url = new URL(SERVER_URL + "/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject loginJson = new JSONObject();
            loginJson.put("username", username);
            loginJson.put("password", password);

            OutputStream os = conn.getOutputStream();
            os.write(loginJson.toString().getBytes());
            os.flush();
            os.close();

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            Log.e("UserDatabaseHelper", "Eroare la autentificare: " + e.getMessage());
            return false;
        }
    }
}