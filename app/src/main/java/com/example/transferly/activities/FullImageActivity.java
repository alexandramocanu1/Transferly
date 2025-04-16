package com.example.transferly.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.transferly.R;
import com.example.transferly.adapters.FullImageAdapter;

import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.canhub.cropper.CropImageContract;


public class FullImageActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageView blurredBackground;
    private View backgroundOverlay;
    private GestureDetectorCompat gestureDetector;
    private List<Uri> imageUris;

    private static final int SWIPE_THRESHOLD_Y = 200;
    private static final int SWIPE_VELOCITY_THRESHOLD = 150;

    private int currentPosition = 0;




    // Launcher pt crop
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    int position = viewPager.getCurrentItem();

                    // Trimite rezultatul inapoi la UploadActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("croppedUri", croppedUri.toString());
                    resultIntent.putExtra("position", position);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // inchide activitatea
                } else {
                    Exception error = result.getError();
                    error.printStackTrace();
                }
            });


    private void startCrop(Uri imageUri) {
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.autoZoomEnabled = true;
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.fixAspectRatio = false;

        CropImageContractOptions contractOptions = new CropImageContractOptions(imageUri, options);
        cropImageLauncher.launch(contractOptions);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        viewPager = findViewById(R.id.viewPager);
        blurredBackground = findViewById(R.id.blurredBackground);
        backgroundOverlay = findViewById(R.id.backgroundOverlay);

        imageUris = getIntent().getParcelableArrayListExtra("images");
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

        viewPager.getChildAt(0).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });

        ImageView cropButton = findViewById(R.id.cropButton);
        cropButton.setOnClickListener(v -> {
//            int currentPosition = viewPager.getCurrentItem();
            Uri imageToCrop = imageUris.get(currentPosition);
            startCrop(imageToCrop);
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
