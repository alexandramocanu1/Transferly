package com.example.transferly.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;

import java.util.List;

public class FullImageActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Obt lista de imagini si poziția curenta
        List<Uri> imageUris = getIntent().getParcelableArrayListExtra("images");
        int currentPosition = getIntent().getIntExtra("position", 0);

        if (imageUris != null && !imageUris.isEmpty()) {
            FullImageAdapter adapter = new FullImageAdapter(imageUris);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition);
        }

        // init GestureDetector
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Set un OnTouchListener pt ViewPager2
        viewPager.getChildAt(0).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 200;
        private static final int SWIPE_VELOCITY_THRESHOLD = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            return true; // pt a detecta alte gesturi
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();

            if (diffY > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                // swipe în jos
                onSwipeDown();
                return true;
            }
            return false;
        }
    }

    private void onSwipeDown() {
        finish(); // inchid activitatea
        overridePendingTransition(0, R.anim.slide_out_down); // animatie de închidere
    }
}
