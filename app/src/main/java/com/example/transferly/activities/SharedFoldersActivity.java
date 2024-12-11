package com.example.transferly.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.transferly.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SharedFoldersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_folders);

        // Configurăm bara de navigare
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_shared_folders); // Selectăm tab-ul Shared Folders

        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Verificăm ce tab este selectat și navigăm către activitatea corespunzătoare
            if (item.getItemId() == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(0, 0); // Fără animație între pagini
                return true;
            } else if (item.getItemId() == R.id.nav_upload) {
                startActivity(new Intent(this, UploadActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.nav_shared_folders) {
                // Rămânem pe această activitate
                return true;
            }
            return false;
        });
    }
}
