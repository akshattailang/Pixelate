package com.akshattailang.pixelate;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener {

    private static final int CAMERA_REQUEST = 1888;

    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private static final int MY_IMAGE_PICK_CODE = 101;

    private BottomSheetDialog bottomSheetDialog;

    private LinearLayout addImageLinearLayout;

    /**
     * The key used to pass crop image source URI
     */
    public static final String CROP_IMAGE_EXTRA_SOURCE = "CROP_IMAGE_EXTRA_SOURCE";
    /**
     * The key used to pass crop image options.
     */
    public static final String CROP_IMAGE_EXTRA_OPTIONS = "CROP_IMAGE_EXTRA_OPTIONS";
    /**
     * The key used to pass crop image bundle data.
     */
    public static final String CROP_IMAGE_EXTRA_BUNDLE = "CROP_IMAGE_EXTRA_BUNDLE";

    /**
     * The key used to pass crop image result data back from {@link CropActivity}.
     */
    public static final String CROP_IMAGE_EXTRA_RESULT = "CROP_IMAGE_EXTRA_RESULT";


    public static final int PICK_IMAGE_PERMISSIONS_REQUEST_CODE = 201;

    public static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;

    public static final int CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE = 204;

    private Uri mCropImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);// Make sure this is before calling super.onCreate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inflateViews();
        setListeners();
    }

    private void inflateViews() {
        this.addImageLinearLayout = findViewById(R.id.add_ll);
    }

    private void setListeners() {
        this.addImageLinearLayout.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();
        if (itemThatWasClickedId == R.id.action_camera) {
            showPopup(findViewById(R.id.action_camera));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showPopup(View v) {
        PopupMenu popup = new PopupMenu(MainActivity.this, v);
        popup.setOnMenuItemClickListener(MainActivity.this);
        popup.inflate(R.menu.popup_menu);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera_item:
                requestCamera();
                return true;
            case R.id.gallery_item:
                requestGallery();
                return true;

            default:
                return false;
        }
    }

    private void requestGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, MY_IMAGE_PICK_CODE);
    }

    private void requestCamera() {
        int permissionCheckRead = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheckRead != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_PERMISSION_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_PERMISSION_CODE);
            }
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.akshattailang.pixelate.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == PICK_IMAGE_PERMISSIONS_REQUEST_CODE) {
            if (mCropImageUri != null
                    && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // required permissions granted, start crop image activity
                startCropActivity(mCropImageUri);
            } else {
                Toast.makeText(this, "Read permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("MainActivity", "requestCode " + requestCode);
        Log.e("MainActivity", "resultCode " + resultCode);
        Log.e("MainActivity", "data " + data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            mCropImageUri = data.getData();
            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (isReadExternalStoragePermissionsRequired(this, mCropImageUri)) {
                Log.e("MainActivity", "isReadExternalStoragePermissionsRequired");
                // request permissions and handle the result in onRequestPermissionsResult()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
                }
            } else {
                // no permissions required or already grunted, can start crop image activity
                startCropActivity(mCropImageUri);
            }
        } else if (requestCode == MY_IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            startCropActivity(uri);
        } else if (requestCode == CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            ActivityResult result = getActivityResult(data);
            if (resultCode == RESULT_OK) {
                if (result != null) {
                    ((ImageView) findViewById(R.id.test_iv)).setImageURI(result.getUri());
                }
            } else if (resultCode == CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                if (result != null) {
                    Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static boolean isReadExternalStoragePermissionsRequired(
            @NonNull Context context, @NonNull Uri uri) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                && isUriRequiresPermissions(context, uri);
    }

    public static boolean isUriRequiresPermissions(@NonNull Context context, @NonNull Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream stream = resolver.openInputStream(uri);
            if (stream != null) {
                stream.close();
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /*public Uri getPickImageResultUri(@Nullable Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera || data.getData() == null ? getCaptureImageOutputUri() : data.getData();
    }

    public Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }*/

    public static ActivityResult getActivityResult(@Nullable Intent data) {
        return data != null ? (ActivityResult) data.getParcelableExtra(CROP_IMAGE_EXTRA_RESULT) : null;
    }

    public static ActivityBuilder activity(@Nullable Uri uri) {
        return new ActivityBuilder(uri);
    }

    private void startCropActivity(Uri imageUri) {
        Log.e("MainActivity", "startCropActivity " + imageUri);
        activity(imageUri).setGuidelines(CropImageView.Guidelines.ON)
                .setActivityTitle("Pixelate")
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("Done")
                .start(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.add_ll) {
            showBottomSheet();

        } else if (id == R.id.camera_tv) {
            bottomSheetDialog.dismiss();
            requestCamera();

        } else if (id == R.id.gallery_tv) {
            bottomSheetDialog.dismiss();
            requestGallery();
        }
    }

    private void showBottomSheet() {
        bottomSheetDialog = new BottomSheetDialog(this);
        final ViewGroup nullParent = null;
        View sheetView = this.getLayoutInflater().inflate(R.layout.add_bottom_sheet, nullParent);
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();

        TextView cameraTextView = sheetView.findViewById(R.id.camera_tv);
        cameraTextView.setOnClickListener(this);

        TextView galleryTextView = sheetView.findViewById(R.id.gallery_tv);
        galleryTextView.setOnClickListener(this);
    }

    /**
     * Builder used for creating Image Crop Activity by user request.
     */
    public static final class ActivityBuilder {

        /**
         * The image to crop source Android uri.
         */
        @Nullable
        private final Uri mSource;

        /**
         * Options for image crop UX
         */
        private final CropImageOptions mOptions;

        private ActivityBuilder(@Nullable Uri source) {
            mSource = source;
            mOptions = new CropImageOptions();
        }

        /**
         * Get {@link CropActivity} intent to start the activity.
         */
        Intent getIntent(@NonNull Context context) {
            return getIntent(context, CropActivity.class);
        }

        /**
         * Get {@link CropActivity} intent to start the activity.
         */
        Intent getIntent(@NonNull Context context, @Nullable Class<?> cls) {
            mOptions.validate();

            Intent intent = new Intent();
            if (cls != null)
                intent.setClass(context, cls);
            Bundle bundle = new Bundle();
            bundle.putParcelable(CROP_IMAGE_EXTRA_SOURCE, mSource);
            bundle.putParcelable(CROP_IMAGE_EXTRA_OPTIONS, mOptions);
            intent.putExtra(CROP_IMAGE_EXTRA_BUNDLE, bundle);
            return intent;
        }

        /**
         * Start {@link CropActivity}.
         *
         * @param activity activity to receive result
         */
        public void start(@NonNull Activity activity) {
            mOptions.validate();
            activity.startActivityForResult(getIntent(activity), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
        }

        /**
         * Start {@link CropActivity}.
         *
         * @param activity activity to receive result
         */
        public void start(@NonNull Activity activity, @Nullable Class<?> cls) {
            mOptions.validate();
            activity.startActivityForResult(getIntent(activity, cls), CROP_IMAGE_ACTIVITY_REQUEST_CODE);
        }


        /**
         * The shape of the cropping window.<br>
         * To set square/circle crop shape set aspect ratio to 1:1.<br>
         * <i>Default: RECTANGLE</i>
         */
        public ActivityBuilder setCropShape(@NonNull CropImageView.CropShape cropShape) {
            mOptions.cropShape = cropShape;
            return this;
        }

        /**
         * An edge of the crop window will snap to the corresponding edge of a specified bounding box
         * when the crop window edge is less than or equal to this distance (in pixels) away from the
         * bounding box edge (in pixels).<br>
         * <i>Default: 3dp</i>
         */
        public ActivityBuilder setSnapRadius(float snapRadius) {
            mOptions.snapRadius = snapRadius;
            return this;
        }

        /**
         * The radius of the touchable area around the handle (in pixels).<br>
         * We are basing this value off of the recommended 48dp Rhythm.<br>
         * See: http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm<br>
         * <i>Default: 48dp</i>
         */
        public ActivityBuilder setTouchRadius(float touchRadius) {
            mOptions.touchRadius = touchRadius;
            return this;
        }

        /**
         * whether the guidelines should be on, off, or only showing when resizing.<br>
         * <i>Default: ON_TOUCH</i>
         */
        public ActivityBuilder setGuidelines(@NonNull CropImageView.Guidelines guidelines) {
            mOptions.guidelines = guidelines;
            return this;
        }

        /**
         * The initial scale type of the image in the crop image view<br>
         * <i>Default: FIT_CENTER</i>
         */
        public ActivityBuilder setScaleType(@NonNull CropImageView.ScaleType scaleType) {
            mOptions.scaleType = scaleType;
            return this;
        }

        /**
         * if to show crop overlay UI what contains the crop window UI surrounded by background over the
         * cropping image.<br>
         * <i>default: true, may disable for animation or frame transition.</i>
         */
        public ActivityBuilder setShowCropOverlay(boolean showCropOverlay) {
            mOptions.showCropOverlay = showCropOverlay;
            return this;
        }

        /**
         * if auto-zoom functionality is enabled.<br>
         * default: true.
         */
        public ActivityBuilder setAutoZoomEnabled(boolean autoZoomEnabled) {
            mOptions.autoZoomEnabled = autoZoomEnabled;
            return this;
        }

        /**
         * if multi touch functionality is enabled.<br>
         * default: true.
         */
        public ActivityBuilder setMultiTouchEnabled(boolean multiTouchEnabled) {
            mOptions.multiTouchEnabled = multiTouchEnabled;
            return this;
        }

        /**
         * The max zoom allowed during cropping.<br>
         * <i>Default: 4</i>
         */
        public ActivityBuilder setMaxZoom(int maxZoom) {
            mOptions.maxZoom = maxZoom;
            return this;
        }

        /**
         * The initial crop window padding from image borders in percentage of the cropping image
         * dimensions.<br>
         * <i>Default: 0.1</i>
         */
        public ActivityBuilder setInitialCropWindowPaddingRatio(float initialCropWindowPaddingRatio) {
            mOptions.initialCropWindowPaddingRatio = initialCropWindowPaddingRatio;
            return this;
        }

        /**
         * whether the width to height aspect ratio should be maintained or free to change.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setFixAspectRatio(boolean fixAspectRatio) {
            mOptions.fixAspectRatio = fixAspectRatio;
            return this;
        }

        /**
         * the X,Y value of the aspect ratio.<br>
         * Also sets fixes aspect ratio to TRUE.<br>
         * <i>Default: 1/1</i>
         *
         * @param aspectRatioX the width
         * @param aspectRatioY the height
         */
        public ActivityBuilder setAspectRatio(int aspectRatioX, int aspectRatioY) {
            mOptions.aspectRatioX = aspectRatioX;
            mOptions.aspectRatioY = aspectRatioY;
            mOptions.fixAspectRatio = true;
            return this;
        }

        /**
         * the thickness of the guidelines lines (in pixels).<br>
         * <i>Default: 3dp</i>
         */
        public ActivityBuilder setBorderLineThickness(float borderLineThickness) {
            mOptions.borderLineThickness = borderLineThickness;
            return this;
        }

        /**
         * the color of the guidelines lines.<br>
         * <i>Default: Color.argb(170, 255, 255, 255)</i>
         */
        public ActivityBuilder setBorderLineColor(int borderLineColor) {
            mOptions.borderLineColor = borderLineColor;
            return this;
        }

        /**
         * thickness of the corner line (in pixels).<br>
         * <i>Default: 2dp</i>
         */
        public ActivityBuilder setBorderCornerThickness(float borderCornerThickness) {
            mOptions.borderCornerThickness = borderCornerThickness;
            return this;
        }

        /**
         * the offset of corner line from crop window border (in pixels).<br>
         * <i>Default: 5dp</i>
         */
        public ActivityBuilder setBorderCornerOffset(float borderCornerOffset) {
            mOptions.borderCornerOffset = borderCornerOffset;
            return this;
        }

        /**
         * the length of the corner line away from the corner (in pixels).<br>
         * <i>Default: 14dp</i>
         */
        public ActivityBuilder setBorderCornerLength(float borderCornerLength) {
            mOptions.borderCornerLength = borderCornerLength;
            return this;
        }

        /**
         * the color of the corner line.<br>
         * <i>Default: WHITE</i>
         */
        public ActivityBuilder setBorderCornerColor(int borderCornerColor) {
            mOptions.borderCornerColor = borderCornerColor;
            return this;
        }

        /**
         * the thickness of the guidelines lines (in pixels).<br>
         * <i>Default: 1dp</i>
         */
        public ActivityBuilder setGuidelinesThickness(float guidelinesThickness) {
            mOptions.guidelinesThickness = guidelinesThickness;
            return this;
        }

        /**
         * the color of the guidelines lines.<br>
         * <i>Default: Color.argb(170, 255, 255, 255)</i>
         */
        public ActivityBuilder setGuidelinesColor(int guidelinesColor) {
            mOptions.guidelinesColor = guidelinesColor;
            return this;
        }

        /**
         * the color of the overlay background around the crop window cover the image parts not in the
         * crop window.<br>
         * <i>Default: Color.argb(119, 0, 0, 0)</i>
         */
        public ActivityBuilder setBackgroundColor(int backgroundColor) {
            mOptions.backgroundColor = backgroundColor;
            return this;
        }

        /**
         * the min size the crop window is allowed to be (in pixels).<br>
         * <i>Default: 42dp, 42dp</i>
         */
        public ActivityBuilder setMinCropWindowSize(int minCropWindowWidth, int minCropWindowHeight) {
            mOptions.minCropWindowWidth = minCropWindowWidth;
            mOptions.minCropWindowHeight = minCropWindowHeight;
            return this;
        }

        /**
         * the min size the resulting cropping image is allowed to be, affects the cropping window
         * limits (in pixels).<br>
         * <i>Default: 40px, 40px</i>
         */
        public ActivityBuilder setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
            mOptions.minCropResultWidth = minCropResultWidth;
            mOptions.minCropResultHeight = minCropResultHeight;
            return this;
        }

        /**
         * the max size the resulting cropping image is allowed to be, affects the cropping window
         * limits (in pixels).<br>
         * <i>Default: 99999, 99999</i>
         */
        public ActivityBuilder setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
            mOptions.maxCropResultWidth = maxCropResultWidth;
            mOptions.maxCropResultHeight = maxCropResultHeight;
            return this;
        }

        /**
         * the title<br>
         * <i>Default: ""</i>
         */
        ActivityBuilder setActivityTitle(CharSequence activityTitle) {
            mOptions.activityTitle = activityTitle;
            return this;
        }

        /**
         * the color to use for action bar items icons.<br>
         * <i>Default: NONE</i>
         */
        public ActivityBuilder setActivityMenuIconColor(int activityMenuIconColor) {
            mOptions.activityMenuIconColor = activityMenuIconColor;
            return this;
        }

        /**
         * the Android Uri to save the cropped image to.<br>
         * <i>Default: NONE, will create a temp file</i>
         */
        public ActivityBuilder setOutputUri(Uri outputUri) {
            mOptions.outputUri = outputUri;
            return this;
        }

        /**
         * the compression format to use when writting the image.<br>
         * <i>Default: JPEG</i>
         */
        public ActivityBuilder setOutputCompressFormat(Bitmap.CompressFormat outputCompressFormat) {
            mOptions.outputCompressFormat = outputCompressFormat;
            return this;
        }

        /**
         * the quility (if applicable) to use when writting the image (0 - 100).<br>
         * <i>Default: 90</i>
         */
        public ActivityBuilder setOutputCompressQuality(int outputCompressQuality) {
            mOptions.outputCompressQuality = outputCompressQuality;
            return this;
        }

        /**
         * the size to resize the cropped image to.<br>
         * Uses {@link CropImageView.RequestSizeOptions#RESIZE_INSIDE} option.<br>
         * <i>Default: 0, 0 - not set, will not resize</i>
         */
        public ActivityBuilder setRequestedSize(int reqWidth, int reqHeight) {
            return setRequestedSize(reqWidth, reqHeight, CropImageView.RequestSizeOptions.RESIZE_INSIDE);
        }

        /**
         * the size to resize the cropped image to.<br>
         * <i>Default: 0, 0 - not set, will not resize</i>
         */
        ActivityBuilder setRequestedSize(
                int reqWidth, int reqHeight, CropImageView.RequestSizeOptions options) {
            mOptions.outputRequestWidth = reqWidth;
            mOptions.outputRequestHeight = reqHeight;
            mOptions.outputRequestSizeOptions = options;
            return this;
        }

        /**
         * if the result of crop image activity should not save the cropped image bitmap.<br>
         * Used if you want to crop the image manually and need only the crop rectangle and rotation
         * data.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setNoOutputImage(boolean noOutputImage) {
            mOptions.noOutputImage = noOutputImage;
            return this;
        }

        /**
         * the initial rectangle to set on the cropping image after loading.<br>
         * <i>Default: NONE - will initialize using initial crop window padding ratio</i>
         */
        public ActivityBuilder setInitialCropWindowRectangle(Rect initialCropWindowRectangle) {
            mOptions.initialCropWindowRectangle = initialCropWindowRectangle;
            return this;
        }

        /**
         * the initial rotation to set on the cropping image after loading (0-360 degrees clockwise).
         * <br>
         * <i>Default: NONE - will read image exif data</i>
         */
        public ActivityBuilder setInitialRotation(int initialRotation) {
            mOptions.initialRotation = (initialRotation + 360) % 360;
            return this;
        }

        /**
         * if to allow rotation during cropping.<br>
         * <i>Default: true</i>
         */
        public ActivityBuilder setAllowRotation(boolean allowRotation) {
            mOptions.allowRotation = allowRotation;
            return this;
        }

        /**
         * if to allow flipping during cropping.<br>
         * <i>Default: true</i>
         */
        public ActivityBuilder setAllowFlipping(boolean allowFlipping) {
            mOptions.allowFlipping = allowFlipping;
            return this;
        }

        /**
         * if to allow counter-clockwise rotation during cropping.<br>
         * Note: if rotation is disabled this option has no effect.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setAllowCounterRotation(boolean allowCounterRotation) {
            mOptions.allowCounterRotation = allowCounterRotation;
            return this;
        }

        /**
         * The amount of degreees to rotate clockwise or counter-clockwise (0-360).<br>
         * <i>Default: 90</i>
         */
        public ActivityBuilder setRotationDegrees(int rotationDegrees) {
            mOptions.rotationDegrees = (rotationDegrees + 360) % 360;
            return this;
        }

        /**
         * whether the image should be flipped horizontally.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setFlipHorizontally(boolean flipHorizontally) {
            mOptions.flipHorizontally = flipHorizontally;
            return this;
        }

        /**
         * whether the image should be flipped vertically.<br>
         * <i>Default: false</i>
         */
        public ActivityBuilder setFlipVertically(boolean flipVertically) {
            mOptions.flipVertically = flipVertically;
            return this;
        }

        /**
         * optional, set crop menu crop button title.<br>
         * <i>Default: null, will use resource string: crop_image_menu_crop</i>
         */
        public ActivityBuilder setCropMenuCropButtonTitle(CharSequence title) {
            mOptions.cropMenuCropButtonTitle = title;
            return this;
        }

        /**
         * Image resource id to use for crop icon instead of text.<br>
         * <i>Default: 0</i>
         */
        public ActivityBuilder setCropMenuCropButtonIcon(@DrawableRes int drawableResource) {
            mOptions.cropMenuCropButtonIcon = drawableResource;
            return this;
        }
    }

    public static final class ActivityResult extends CropImageView.CropResult implements Parcelable {

        public static final Creator<ActivityResult> CREATOR =
                new Creator<ActivityResult>() {
                    @Override
                    public ActivityResult createFromParcel(Parcel in) {
                        return new ActivityResult(in);
                    }

                    @Override
                    public ActivityResult[] newArray(int size) {
                        return new ActivityResult[size];
                    }
                };

        ActivityResult(
                Uri originalUri,
                Uri uri,
                Exception error,
                float[] cropPoints,
                Rect cropRect,
                int rotation,
                Rect wholeImageRect,
                int sampleSize) {
            super(
                    null,
                    originalUri,
                    null,
                    uri,
                    error,
                    cropPoints,
                    cropRect,
                    wholeImageRect,
                    rotation,
                    sampleSize);
        }

        ActivityResult(Parcel in) {
            super(
                    null,
                    (Uri) in.readParcelable(Uri.class.getClassLoader()),
                    null,
                    (Uri) in.readParcelable(Uri.class.getClassLoader()),
                    (Exception) in.readSerializable(),
                    in.createFloatArray(),
                    (Rect) in.readParcelable(Rect.class.getClassLoader()),
                    (Rect) in.readParcelable(Rect.class.getClassLoader()),
                    in.readInt(),
                    in.readInt());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(getOriginalUri(), flags);
            dest.writeParcelable(getUri(), flags);
            dest.writeSerializable(getError());
            dest.writeFloatArray(getCropPoints());
            dest.writeParcelable(getCropRect(), flags);
            dest.writeParcelable(getWholeImageRect(), flags);
            dest.writeInt(getRotation());
            dest.writeInt(getSampleSize());
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        String mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
