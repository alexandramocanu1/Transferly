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
//        setContentView(R.layout.activity_login); // Asigura-te ca ai un layout pentru login
//
//        // Gaseste linkul de inregistrare
//        TextView registerLink = findViewById(R.id.registerLink);
//
//        // Adauga un OnClickListener pentru a naviga la RegisterActivity
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
        setContentView(R.layout.activity_login); 

        // linkul de inregistrare
        TextView registerLink = findViewById(R.id.registerLink);

        // butonul de login
        Button loginButton = findViewById(R.id.loginButton);

        // OnClickListener pt a naviga la RegisterActivity
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deschide RegisterActivity
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // OnClickListener pentru butonul de login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Daca utilizatorul apasa pe "login", navigheaza la UploadActivity
                Intent intent = new Intent(LoginActivity.this, UploadActivity.class);
                startActivity(intent);
                finish(); // inchide LoginActivity pt a nu putea reveni la ea cu back
            }
        });
    }
}
