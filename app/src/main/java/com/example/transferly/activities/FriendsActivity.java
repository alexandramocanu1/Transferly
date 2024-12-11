package com.example.transferly.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.transferly.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // Configurăm bara de navigare
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_friends); // Selectăm tab-ul curent

        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Înlocuim switch cu if-else
            if (item.getItemId() == R.id.nav_friends) {
                // Rămânem pe această activitate
                return true;

            } else if (item.getItemId() == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (item.getItemId() == R.id.nav_shared_folders) {
                startActivity(new Intent(this, SharedFoldersActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else {
                return false;
            }
        });
    }
}
