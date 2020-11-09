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
import android.hardware.Camera;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class FullscreenActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    String effect;
    public static final int RequestPermissionCode = 1;
    File folder = new File(Environment.getExternalStorageDirectory() +
            File.separator + "SOBERA");

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private Button mSobelButton;
    private Button mNextButton;
    private Button mPlainButton;
    private Button mCamShotButton;
    private int effectIdx;
    private int maxEffectIdx;
    private List<String> allColorsEffects;
    private List<String> possibleColorsEffects = Arrays.asList("mono", "negative", "sepia", "aqua", "whiteboard","blackboard", "posterize", "solarize");
    private TextView textEffect;
    Camera.Size bestSize = null;



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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.wtf("Effects tag", "Click down on button");
                    effect = allColorsEffects.get(effectIdx);
                    mPreview.refreshCamera(mCamera,effect);
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    effectIdx = effectIdx+1;
                    if(effectIdx == maxEffectIdx){
                        effectIdx = 0;
                    }
                    Log.wtf("Effects tag", "Click up on button");
                    effect = "none";
                    mPreview.refreshCamera(mCamera,effect);
                    break;
                default:
                    break;
            }
            return false;
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
        mNextButton = (Button) findViewById(R.id.next_button);
        textEffect = (TextView) findViewById(R.id.text_for_effects);
       //EnableRuntimePermission();
       if(ContextCompat.checkSelfPermission(FullscreenActivity.this,Manifest.permission.CAMERA)
               != PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(FullscreenActivity.this,new String[]{ Manifest.permission.CAMERA},100);
       }

        // Create an instance of Camera
        mCamera = getCameraInstance();

        Camera.Parameters params = mCamera.getParameters();
        allColorsEffects = params.getSupportedColorEffects();
        for(int i=0; i<allColorsEffects.size(); i++){
            Log.wtf("Effects tag", "Available effect: " + allColorsEffects.get(i));
            /*if(!possibleColorsEffects.contains(allColorsEffects.get(i))){
                Log.wtf("Remove effects tag", "Remove effect: " + allColorsEffects.get(i));
                allColorsEffects.remove(i);
                i--;
            }*/
            if(allColorsEffects.get(i).equals("none")){
                Log.wtf("Remove effects tag", "Remove effect: " + allColorsEffects.get(i));
                allColorsEffects.remove(i);
                i--;
            }
        }
       List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
       bestSize = sizeList.get(0);
       for(int i = 1; i < sizeList.size(); i++){
           if((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)){
               bestSize = sizeList.get(i);
           }
       }
       params.setPictureSize(bestSize.width, bestSize.height);




        effectIdx = 0;
        maxEffectIdx = allColorsEffects.size();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) mContentView;
        preview.addView(mPreview);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        // make button as listener to be called outside this function!
        mSobelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on sobel button");
                if(allColorsEffects.contains("blackboard")){
                    effect = "blackboard";
                    textEffect.setText(effect);
                    mPreview.refreshCamera(mCamera,effect);
                }
                else{
                    textEffect.setText("Effect indisponible");
                }

                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }

            }
        });
        mPlainButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on plain button");
                effect = "none";
                textEffect.setText(effect);
                mPreview.refreshCamera(mCamera,effect);
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }

        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.wtf("Effects tag", "Click on next button");
                if(maxEffectIdx != 0){
                    effect = allColorsEffects.get(effectIdx);
                    effectIdx = effectIdx+1;
                    if(effectIdx == maxEffectIdx){
                        effectIdx = 0;
                    }
                    textEffect.setText(effect);
                    mPreview.refreshCamera(mCamera,effect);
                }
                else{
                    textEffect.setText("Effect indisponible");
                }

                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }

            }
        });
       mCamShotButton.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               Log.wtf("Effects tag", "Click on camera button");
               if(ContextCompat.checkSelfPermission(FullscreenActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
               != PackageManager.PERMISSION_GRANTED){
                   ActivityCompat.requestPermissions(FullscreenActivity.this,new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
               }
               mCamera.takePicture(shutterCallback, null, jpegCallback );
           }
       });

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
            try {
                // write to local sandbox file system
                // outStream =
                // CameraDemo.this.openFileOutput(String.format("%d.jpg",
                // System.currentTimeMillis()), 0);
                // Or write to sdcard


                if (!folder.exists()) {
                    boolean success = folder.mkdirs();
                    if (success) {
                        Toast.makeText(FullscreenActivity.this, "Directory Created", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(FullscreenActivity.this, "Failed - Error", Toast.LENGTH_SHORT).show();
                    }
                }

                outStream = new FileOutputStream(String.format(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/SOBERA/Sobera_%d.jpg", System.currentTimeMillis()));
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    WindowManager wi =  (WindowManager) FullscreenActivity.this.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wi.getDefaultDisplay();
                    if(display.getRotation() == Surface.ROTATION_270) {
                        Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                        realImage= rotate(realImage, 180);
                        boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        if(bo){
                            Log.wtf("Picture changed", "Success");
                        }
                        else{
                            Log.wtf("Picture changed", "Fail");
                        }
                    }
                    else{
                        outStream.write(data);
                    }
                }
                else{
                    Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                    realImage= rotate(realImage, 90);
                    boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    if(bo){
                        Log.wtf("Picture changed", "Success");
                    }
                    else{
                        Log.wtf("Picture changed", "Fail");
                    }
                }
                outStream.close();
                Log.wtf("Camera settings", "onPictureTaken - wrote bytes: " + data.length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            Log.wtf("Camera settings", "onPictureTaken - jpeg");
            mPreview.refreshCamera(mCamera,effect);
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
            case RequestPermissionCode:
                if (result.length > 0 && result[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(FullscreenActivity.this, "Permission Granted.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(FullscreenActivity.this, "Permission Denied. You can change this parameter in the settings of your phone.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

}