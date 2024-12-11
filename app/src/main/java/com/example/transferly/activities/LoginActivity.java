//package com.example.transferly.activities;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.TextView;
//import androidx.appcompat.app.AppCompatActivity;
//import com.example.transferly.R;
//
//public class LoginActivity extends AppCompatActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login); // Asigură-te că ai un layout pentru login
//
//        // Găsește linkul de înregistrare
//        TextView registerLink = findViewById(R.id.registerLink);
//
//        // Adaugă un OnClickListener pentru a naviga la RegisterActivity
//        registerLink.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Deschide RegisterActivity
//                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
//                startActivity(intent);
//            }
//        });
//    }
//}




package com.example.transferly.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.transferly.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Asigură-te că ai un layout pentru login

        // Găsește linkul de înregistrare
        TextView registerLink = findViewById(R.id.registerLink);

        // Găsește butonul de login
        Button loginButton = findViewById(R.id.loginButton);

        // Adaugă un OnClickListener pentru a naviga la RegisterActivity
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deschide RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Adaugă un OnClickListener pentru butonul de login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dacă utilizatorul apasă pe "login", navighează la UploadActivity
                Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                startActivity(intent);
                finish(); // Închide LoginActivity pentru a nu putea reveni la ea cu back
            }
        });
    }
}
