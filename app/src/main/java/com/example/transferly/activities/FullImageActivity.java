package com.example.transferly.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;

import java.util.ArrayList;
import java.util.List;

public class FullImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImageView closeButton = findViewById(R.id.closeButton);

        // Get images from intent
        List<Uri> images = getIntent().getParcelableArrayListExtra("images");
        if (images == null || images.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current position
        int position = getIntent().getIntExtra("position", 0);
        FullImageAdapter adapter = new FullImageAdapter(images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position, false);

        // Close button functionality
        closeButton.setOnClickListener(v -> finish());
    }
}