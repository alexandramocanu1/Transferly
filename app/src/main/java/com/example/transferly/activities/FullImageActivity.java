package com.example.transferly.activities;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.transferly.R;

public class FullImageActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        ImageView imageViewFull = findViewById(R.id.imageViewFull);

        // Primește URI-ul imaginii din intent
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);

            // Încarcă imaginea în ImageView
            Glide.with(this)
                    .load(imageUri)
                    .into(imageViewFull);
        }

        // Initializează GestureDetector
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Setează un touch listener pe View-ul principal
        imageViewFull.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeDown();
                }
                return true;
            }
            return false;
        }
    }

    private void onSwipeDown() {
        finish(); // Închide activitatea
        overridePendingTransition(0, R.anim.slide_out_down); // Animație de închidere (opțional)
    }
}
