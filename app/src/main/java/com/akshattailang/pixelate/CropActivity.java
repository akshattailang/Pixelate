package com.akshattailang.pixelate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import static com.akshattailang.pixelate.MainActivity.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE;
import static com.akshattailang.pixelate.MainActivity.CROP_IMAGE_EXTRA_BUNDLE;
import static com.akshattailang.pixelate.MainActivity.CROP_IMAGE_EXTRA_OPTIONS;
import static com.akshattailang.pixelate.MainActivity.CROP_IMAGE_EXTRA_RESULT;
import static com.akshattailang.pixelate.MainActivity.CROP_IMAGE_EXTRA_SOURCE;

public class CropActivity extends AppCompatActivity implements CropImageView.OnSetImageUriCompleteListener,
        CropImageView.OnCropImageCompleteListener{

    private CropImageView mCropImageView;

    private CropImageOptions mOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        mCropImageView = findViewById(R.id.crop_iv);

        Bundle bundle = getIntent().getBundleExtra(CROP_IMAGE_EXTRA_BUNDLE);
        mOptions = bundle.getParcelable(CROP_IMAGE_EXTRA_OPTIONS);
        Uri mCropImageUri = bundle.getParcelable(CROP_IMAGE_EXTRA_SOURCE);
        Log.e("CropActivity", "URI " + mCropImageUri);
        if (savedInstanceState == null) {
            mCropImageView.setImageUriAsync(mCropImageUri);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            CharSequence title = mOptions != null &&
                    mOptions.activityTitle != null && mOptions.activityTitle.length() > 0
                    ? mOptions.activityTitle : "";
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCropImageView.setOnSetImageUriCompleteListener(this);
        mCropImageView.setOnCropImageCompleteListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCropImageView.setOnSetImageUriCompleteListener(null);
        mCropImageView.setOnCropImageCompleteListener(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_image_menu, menu);

        if (!mOptions.allowRotation) {
            menu.removeItem(R.id.crop_image_menu_rotate_left);
            menu.removeItem(R.id.crop_image_menu_rotate_right);
        } else if (mOptions.allowCounterRotation) {
            menu.findItem(R.id.crop_image_menu_rotate_left).setVisible(true);
        }

        if (!mOptions.allowFlipping) {
            menu.removeItem(R.id.crop_image_menu_flip);
        }

        if (mOptions.cropMenuCropButtonTitle != null) {
            menu.findItem(R.id.crop_image_menu_crop).setTitle(mOptions.cropMenuCropButtonTitle);
        }

        Drawable cropIcon = null;
        try {
            if (mOptions.cropMenuCropButtonIcon != 0) {
                cropIcon = ContextCompat.getDrawable(this, mOptions.cropMenuCropButtonIcon);
                menu.findItem(R.id.crop_image_menu_crop).setIcon(cropIcon);
            }
        } catch (Exception e) {
            Log.w("AIC", "Failed to read menu crop drawable", e);
        }

        if (mOptions.activityMenuIconColor != 0) {
            updateMenuItemIconColor(
                    menu, R.id.crop_image_menu_rotate_left, mOptions.activityMenuIconColor);
            updateMenuItemIconColor(
                    menu, R.id.crop_image_menu_rotate_right, mOptions.activityMenuIconColor);
            updateMenuItemIconColor(menu, R.id.crop_image_menu_flip, mOptions.activityMenuIconColor);
            if (cropIcon != null) {
                updateMenuItemIconColor(menu, R.id.crop_image_menu_crop, mOptions.activityMenuIconColor);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.crop_image_menu_crop) {
            cropImage();
            return true;
        }
        if (item.getItemId() == R.id.crop_image_menu_rotate_left) {
            rotateImage(-mOptions.rotationDegrees);
            return true;
        }
        if (item.getItemId() == R.id.crop_image_menu_rotate_right) {
            rotateImage(mOptions.rotationDegrees);
            return true;
        }
        if (item.getItemId() == R.id.crop_image_menu_flip_horizontally) {
            mCropImageView.flipImageHorizontally();
            return true;
        }
        if (item.getItemId() == R.id.crop_image_menu_flip_vertically) {
            mCropImageView.flipImageVertically();
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            setResultCancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Cancel of cropping activity. */
    protected void setResultCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    /** Rotate the image in the crop image view. */
    protected void rotateImage(int degrees) {
        mCropImageView.rotateImage(degrees);
    }

    /** Execute crop image and save the result tou output uri. */
    protected void cropImage() {
        if (mOptions.noOutputImage) {
            setResult(null, null, 1);
        } else {
            Uri outputUri = getOutputUri();
            mCropImageView.saveCroppedImageAsync(
                    outputUri,
                    mOptions.outputCompressFormat,
                    mOptions.outputCompressQuality,
                    mOptions.outputRequestWidth,
                    mOptions.outputRequestHeight,
                    mOptions.outputRequestSizeOptions);
        }
    }

    /**
     * Get Android uri to save the cropped image into.<br>
     * Use the given in options or create a temp file.
     */
    protected Uri getOutputUri() {
        Uri outputUri = mOptions.outputUri;
        if (outputUri == null || outputUri.equals(Uri.EMPTY)) {
            try {
                String ext =
                        mOptions.outputCompressFormat == Bitmap.CompressFormat.JPEG
                                ? ".jpg"
                                : mOptions.outputCompressFormat == Bitmap.CompressFormat.PNG ? ".png" : ".webp";
                outputUri = Uri.fromFile(File.createTempFile("cropped", ext, getCacheDir()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp file for output image", e);
            }
        }
        return outputUri;
    }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        Log.e("CropActivity", "onSetImageUriComplete " + error);
        if (error == null) {
            if (mOptions.initialCropWindowRectangle != null) {
                mCropImageView.setCropRect(mOptions.initialCropWindowRectangle);
            }
            if (mOptions.initialRotation > -1) {
                mCropImageView.setRotatedDegrees(mOptions.initialRotation);
            }
        } else {
            setResult(null, error, 1);
        }
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
        setResult(result.getUri(), result.getError(), result.getSampleSize());
    }

    /** Update the color of a specific menu item to the given color. */
    private void updateMenuItemIconColor(Menu menu, int itemId, int color) {
        MenuItem menuItem = menu.findItem(itemId);
        if (menuItem != null) {
            Drawable menuItemIcon = menuItem.getIcon();
            if (menuItemIcon != null) {
                try {
                    menuItemIcon.mutate();
                    menuItemIcon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    menuItem.setIcon(menuItemIcon);
                } catch (Exception e) {
                    Log.w("AIC", "Failed to update menu item color", e);
                }
            }
        }
    }

    /** Result with cropped image data or error if failed. */
    protected void setResult(Uri uri, Exception error, int sampleSize) {
        int resultCode = error == null ? RESULT_OK : CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE;
        setResult(resultCode, getResultIntent(uri, error, sampleSize));
        finish();
    }

    /** Get intent instance to be used for the result of this activity. */
    protected Intent getResultIntent(Uri uri, Exception error, int sampleSize) {
        MainActivity.ActivityResult result =
                new MainActivity.ActivityResult(
                        mCropImageView.getImageUri(),
                        uri,
                        error,
                        mCropImageView.getCropPoints(),
                        mCropImageView.getCropRect(),
                        mCropImageView.getRotatedDegrees(),
                        mCropImageView.getWholeImageRect(),
                        sampleSize);
        Intent intent = new Intent();
        intent.putExtras(getIntent());
        intent.putExtra(CROP_IMAGE_EXTRA_RESULT, result);
        return intent;
    }

}
