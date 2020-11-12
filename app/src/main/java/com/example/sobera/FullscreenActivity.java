package com.example.sobera;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.hardware.Camera;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class FullscreenActivity extends AppCompatActivity{

    private Camera mCamera;
    private CameraPreview mPreview;
    String effect = "none";
    public static final int RequestPermissionCodeCamera = 100;
    public static final int RequestPermissionCodeStorage = 200;
    File folder = new File(Environment.getExternalStorageDirectory() +
            File.separator + "SOBERA");
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private Button mSobelButton;
    private Button mPlainButton;
    private Button mCamShotButton;
    private Button mSwapCamButton;
    private Button mEmptyButton;
    private int effectIdx;
    private int maxEffectIdx;
    private List<String> allColorsEffects;
    private TextView textEffect;
    private TextView textSobel;
    private ImageView imageView;
    Camera.Size bestSize = null;
    final int numberCamera = Camera.getNumberOfCameras();
    int currentCam = 0;
    String lastJPGPath;
    String effectUnavailable = "Effect unavailable";
    String folderPic;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;


    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mSobelButton = (Button) findViewById(R.id.sobel_button);
        mPlainButton = (Button) findViewById(R.id.plain_button);
        mCamShotButton = (Button) findViewById(R.id.save_pic_button);
        mSwapCamButton = (Button) findViewById(R.id.swap_camera_button);
        textEffect = (TextView) findViewById(R.id.text_for_effects);
        textSobel = (TextView) findViewById(R.id.text_apply_sobel);
       imageView = (ImageView) findViewById(R.id.imageView1);
       mEmptyButton = (Button) findViewById(R.id.empty_button);

       mSobelButton.setVisibility(View.GONE);
       mPlainButton.setVisibility(View.GONE);
       mEmptyButton.setVisibility(View.GONE);
       textSobel.setVisibility(View.GONE);
       imageView.setVisibility(View.GONE);

       if(effect.equals("none")){
           textEffect.setText("no filter");
       }
       else{
           textEffect.setText(effect);
       }


       if(ContextCompat.checkSelfPermission(FullscreenActivity.this,Manifest.permission.CAMERA)
               != PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(FullscreenActivity.this,new String[]{ Manifest.permission.CAMERA},RequestPermissionCodeCamera);
       }
       else{
           doStuff();
       }

    }

    private void doStuff(){
        // Create an instance of Camera
        mCamera = getCameraInstance();

        Camera.Parameters params = mCamera.getParameters();
        allColorsEffects = params.getSupportedColorEffects();
        for(int i=0; i<allColorsEffects.size(); i++){
            Log.wtf("Effects tag", "Available effect: " + allColorsEffects.get(i));
        }

        setCameraSize(params);




        effectIdx = 0;
        maxEffectIdx = allColorsEffects.size();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        final FrameLayout preview = (FrameLayout) mContentView;
        preview.addView(mPreview);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mContentView.setOnTouchListener(new OnSwipeTouchListener(FullscreenActivity.this) {
            /*public boolean onSwipeTop() {
                //Toast.makeText(FullscreenActivity.this, "top", Toast.LENGTH_SHORT).show();
                return true;

             public boolean onSwipeBottom() {
                //Toast.makeText(FullscreenActivity.this, "bottom", Toast.LENGTH_SHORT).show();
                return true;
            }
            }*/
            public boolean onSwipeRight() {
                if(maxEffectIdx != 0){
                    effectIdx = effectIdx-1;
                    if(effectIdx == -1){
                        effectIdx = maxEffectIdx-1;
                    }
                    effect = allColorsEffects.get(effectIdx);
                    if(effect.equals("none")){
                        textEffect.setText("no filter");
                    }
                    else{
                        textEffect.setText(effect);
                    }
                    mPreview.refreshCamera(mCamera,effect);
                }
                else{
                    textEffect.setText(effectUnavailable);
                }

                return true;
            }
            public boolean onSwipeLeft() {
                if(maxEffectIdx != 0){
                    effectIdx = effectIdx+1;
                    if(effectIdx == maxEffectIdx){
                        effectIdx = 0;
                    }
                    effect = allColorsEffects.get(effectIdx);
                    if(effect.equals("none")){
                        textEffect.setText("no filter");
                    }
                    else{
                        textEffect.setText(effect);
                    }
                    mPreview.refreshCamera(mCamera,effect);
                }
                else{
                    textEffect.setText(effectUnavailable);
                }

                return true;
            }

            public boolean onSimpleClick() {
                toggle();
                return true;
            }

        });


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        // make button as listener to be called outside this function!
        mSobelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on sobel button");
                doSobel(true);

            }
        });
        mPlainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on plain button");
                doSobel(false);
            }

        });
        mCamShotButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on camera button");
                if(ContextCompat.checkSelfPermission(FullscreenActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(FullscreenActivity.this,new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},RequestPermissionCodeStorage);
                }
                else{
                    takeAShot();
                    //mCamera.takePicture(shutterCallback, null, jpegCallback );
                }

            }
        });
        mSwapCamButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on swap camera button");
                currentCam++;
                if(currentCam == numberCamera){
                    currentCam = 0;
                }
                mCamera.stopPreview();
                preview.removeView(mPreview);
                if (mCamera != null) {
                    mCamera.release();
                }
                mCamera = Camera.open(currentCam);
                setCameraSize(mCamera.getParameters());
                preview.addView(mPreview);
                mPreview.refreshCamera(mCamera,effect);

            }
        });
    }

    private void takeAShot(){
        mCamera.takePicture(shutterCallback, null, jpegCallback );
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            Log.wtf("Camera settings", "onShutter'd");
        }
    };

    /** Handles data for jpeg picture */
    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            FileOutputStream outStream = null;
            Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
            try {
                // write to local sandbox file system
                // outStream =
                // CameraDemo.this.openFileOutput(String.format("%d.jpg",
                // System.currentTimeMillis()), 0);
                // Or write to sdcard

                if (!folder.exists()) {
                    boolean success = folder.mkdirs();
                    if (success) {
                        //Toast.makeText(FullscreenActivity.this, "Directory Created", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FullscreenActivity.this, "Failed - Error", Toast.LENGTH_SHORT).show();
                    }
                }
                lastJPGPath = String.format("Sobera_%d", System.currentTimeMillis());
                String state = Environment.getExternalStorageState();
                if (state.equals(Environment.MEDIA_MOUNTED)) {
                    // Available to read and write
                    folderPic = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
                }
                else{
                    folderPic = "";
                    Toast.makeText(FullscreenActivity.this, "No external storage, pictures won't be saved.", Toast.LENGTH_SHORT).show();
                }
                outStream = new FileOutputStream(folderPic + "SOBERA/" + lastJPGPath + ".jpg");
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    WindowManager wi =  (WindowManager) FullscreenActivity.this.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wi.getDefaultDisplay();
                    if(display.getRotation() == Surface.ROTATION_270) {
                        //Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                        realImage= rotate(realImage, 180);
                        boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        if(bo){
                            Log.wtf("Picture changed", "Success");
                        }
                        else {
                            Log.wtf("Picture changed", "Fail");
                        }
                    }
                    else{
                        outStream.write(data);
                    }
                }
                else{
                    //Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                    if(currentCam == Camera.CameraInfo.CAMERA_FACING_FRONT){
                        realImage= rotate(realImage, -90);
                    }
                    else{
                        realImage= rotate(realImage, 90);
                    }
                    boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    if(bo){
                        Log.wtf("Picture changed", "Success");
                    }
                    else{
                        Log.wtf("Picture changed", "Fail");
                    }

                }
                outStream.close();
                mCamera.stopPreview();
                FrameLayout.LayoutParams imageViewparam = (FrameLayout.LayoutParams) imageView.getLayoutParams();
                imageViewparam.height = realImage.getHeight();
                imageView.setLayoutParams(imageViewparam);
                imageView.setImageBitmap(realImage);
                imageView.setVisibility(View.VISIBLE);
                textSobel.setVisibility(View.VISIBLE);

                Log.wtf("Camera settings", "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.wtf("Camera settings", "onPictureTaken - jpeg");
            //mPreview.refreshCamera(mCamera,effect);
            mCamShotButton.setVisibility(View.GONE);
            mSwapCamButton.setVisibility(View.GONE);
            mSobelButton.setVisibility(View.VISIBLE);
            mPlainButton.setVisibility(View.VISIBLE);
            mEmptyButton.setVisibility(View.VISIBLE);
        }
    };

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }


    private void show() {
        // Show the system bar
        /*mControlsView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);*/
        mControlsView.setVisibility(View.VISIBLE);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] result) {
        switch (requestCode) {
            case RequestPermissionCodeCamera:
                if (!Arrays.asList(result).contains(PackageManager.PERMISSION_DENIED)) {
                    Toast.makeText(FullscreenActivity.this, "Permission Camera Granted.", Toast.LENGTH_LONG).show();
                    doStuff();
                } else {
                    Toast.makeText(FullscreenActivity.this, "Permission Denied. You can change this parameter in the settings of your phone.", Toast.LENGTH_LONG).show();
                }
                break;
            case RequestPermissionCodeStorage:
                if (!Arrays.asList(result).contains(PackageManager.PERMISSION_DENIED)) {
                    Toast.makeText(FullscreenActivity.this, "Permission Storage Granted.", Toast.LENGTH_LONG).show();
                    takeAShot();
                    //mCamera.takePicture(shutterCallback, null, jpegCallback );
                } else {
                    Toast.makeText(FullscreenActivity.this, "Permission Denied. You can change this parameter in the settings of your phone.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /*
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }*/
    }


    private void doSobel(boolean makeItAppend) {
        if(makeItAppend){

            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(folderPic + "SOBERA/" + lastJPGPath + "_Sobel.jpg");
                Bitmap bitmap = BitmapFactory.decodeFile(folderPic + "SOBERA/" + lastJPGPath + ".jpg");

                Sobel.filterSobel(bitmap);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
                outStream.close();

            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        mSobelButton.setVisibility(View.GONE);
        mPlainButton.setVisibility(View.GONE);
        mEmptyButton.setVisibility(View.GONE);
        textSobel.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        mCamShotButton.setVisibility(View.VISIBLE);
        mSwapCamButton.setVisibility(View.VISIBLE);
        mCamera.startPreview();
        mPreview.refreshCamera(mCamera,effect);
    }

    public static class Sobel {
        private static Bitmap toGreyScale( Bitmap source ) {
            Bitmap greyScaleBitmap = Bitmap.createBitmap(
                    source.getWidth(), source.getHeight(),
                    Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(greyScaleBitmap);
            Paint p = new Paint();
            ColorMatrix cm = new ColorMatrix();

            cm.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
            p.setColorFilter(filter);
            c.drawBitmap(source, 0, 0, p);
            return greyScaleBitmap;
        }

        public static void filterSobel( Bitmap source) {

            Bitmap grey = toGreyScale(source);

            int w = grey.getWidth();
            int h = grey.getHeight();
            // Allocate 4 times as much data as is necessary because Android.
            int sz = w * h;

            IntBuffer buffer = IntBuffer.allocate( sz );
            grey.copyPixelsToBuffer( buffer );
            final int[] bitmapData = buffer.array();

            int[] output = new int[ w * h ];
            for( int y=1; y<h-1; y++ ) {
                for( int x=1; x<w-1; x++ ) {
                    int idx = (y * w + x );

                    // Apply Sobel filter
                    int tl = (bitmapData[idx - w - 1]) & 0xFF;
                    int tr = (bitmapData[idx - w + 1]) & 0xFF;
                    int l = (bitmapData[idx - 1]) & 0xFF;
                    int r = (bitmapData[idx + 1]) & 0xFF;
                    int t = (bitmapData[idx - w]) & 0xFF;
                    int b = (bitmapData[idx + w]) & 0xFF;
                    int bl = (bitmapData[idx + w - 1]) & 0xFF;
                    int br = (bitmapData[idx + w + 1]) & 0xFF;

                    int sx1 = (int) ( tr - tl + 2 * ( r - l ) + br - bl ); //Matrix Gx
                    int sx2 = (int) ( -tr - tl + 2 * ( b - t ) + br + bl ); //Matrix Gy
                    int sx = abs(sx1) + abs(sx2);
                    sx = sx & 0xFF;

                    // Put back into ARG and B bytes
                    output[idx] = (sx << 24) | ( sx << 16) | (sx << 8) | sx;
                }
            }
            source.copyPixelsFromBuffer( IntBuffer.wrap(output));
        }

        private static int abs(int i) {
            if(i<0){
                return -i;
            }
            return i;
        }
    }

    private void setCameraSize(Camera.Parameters params){
        List<Camera.Size> sizeList = params.getSupportedPreviewSizes();
        bestSize = sizeList.get(0);
        for(int i = 1; i < sizeList.size(); i++){
            if((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)){
                bestSize = sizeList.get(i);
            }
        }
        params.setPictureSize(bestSize.width, bestSize.height);
    }

}