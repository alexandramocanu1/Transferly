package com.example.transferly.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;

import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class FullImageActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageView blurredBackground;
    private View backgroundOverlay;
    private GestureDetectorCompat gestureDetector;

    private static final int SWIPE_THRESHOLD_Y = 200;
    private static final int SWIPE_VELOCITY_THRESHOLD = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        viewPager = findViewById(R.id.viewPager);
        blurredBackground = findViewById(R.id.blurredBackground);
        backgroundOverlay = findViewById(R.id.backgroundOverlay);

        List<Uri> imageUris = getIntent().getParcelableArrayListExtra("images");
        int currentPosition = getIntent().getIntExtra("position", 0);

        if (imageUris != null && !imageUris.isEmpty()) {
            FullImageAdapter adapter = new FullImageAdapter(this, imageUris);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(currentPosition, false);
            applyBlurEffect(imageUris.get(currentPosition));
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                applyBlurEffect(imageUris.get(position));
            }
        });

        backgroundOverlay.setOnClickListener(v -> finishWithAnimation());

        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        // pun OnTouchListener pe ViewPager2 pt a detecta swipe-ul in jos
        viewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // ViewPager2 inca primeste evenimente
        });
    }

    private void applyBlurEffect(Uri imageUri) {
        Glide.with(this)
                .asBitmap()
                .load(imageUri)
                .apply(RequestOptions.bitmapTransform(new BlurTransformation(25, 3)))
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(blurredBackground);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffY = e2.getY() - e1.getY();

            if (diffY > SWIPE_THRESHOLD_Y && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                finishWithAnimation();
                return true;
            }
            return false;
        }
    }

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }
}
