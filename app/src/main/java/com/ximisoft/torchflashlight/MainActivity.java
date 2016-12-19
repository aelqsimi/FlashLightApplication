package com.ximisoft.torchflashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linroid.filtermenu.library.FilterMenu;
import com.linroid.filtermenu.library.FilterMenuLayout;


/**
 * Created by XimiSoft
 * Date : 08/10/2015
 * Email : ximisoft44@gmail.com
 */

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements SensorEventListener, Constants {

    public static String PACKAGE_NAME;
    static int freq;
    private static int batteryPct;
    ImageButton btnSwitch;
    Camera.Parameters params;
    MediaPlayer mp;
    // define the display assembly compass picture
    private ImageView image;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private SwitchCompat strobo;
    // record the compass picture angle turned
    private float currentDegree = 0f;
    // device sensor manager
    private SensorManager mSensorManager;
    // Admob ads
    private InterstitialAd interstitialAd = null;
    private AdView adView = null;
    private Camera camera;
    private boolean isFlashOn;
    private boolean isCameraOn = false;
    private boolean hasFlash;
    private boolean batteryOk = true;
    private StroboRunner sr;
    private Thread t;
    private Thread tr;
    private ImageButton btnCamera;
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            batteryPct = level * 100 / scale;
            Log.d("Batery", "=========================>" + String.valueOf(batteryPct));
            if (batteryPct < 15) {
                if (batteryOk) {
                    turnOffFlash();
                    showBatteryLowDialog();
                    batteryOk = false;
                }
            } else {
                batteryOk = true;
            }
        }
    };
    private Tracker mTracker;

    public static boolean isOnline(Context mContext) {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        PACKAGE_NAME = getApplicationContext().getPackageName();

        // our compass image
        image = (ImageView) findViewById(R.id.imageViewCompass);

        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // flash switch button
        btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);

        btnCamera = (ImageButton) findViewById(R.id.btnCam);

        preview = (FrameLayout) findViewById(R.id.camera_preview);

        SeekBar skBar = (SeekBar) findViewById(R.id.seekBar);
        strobo = (SwitchCompat) findViewById(R.id.activeStrobo);
        strobo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SeekBar bar = (SeekBar) findViewById(R.id.seekBar);
                TextView stroboText = (TextView) findViewById(R.id.textView2);
                if (isChecked) {
                    bar.setVisibility(View.VISIBLE);
                    bar.setProgress(90);
                    if (isFlashOn) {
                        turnOffFlash();
                    }
                    turnOnFlash();
                    stroboText.setVisibility(View.VISIBLE);
                    toggleButtonImageSwitch();

                } else {
                    isFlashOn = true;
                    turnOffFlash();
                    bar.setProgress(0);
                    bar.setVisibility(View.GONE);
                    stroboText.setVisibility(View.GONE);
                    toggleButtonImageSwitch();
                }

            }
        });
        skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freq = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });
        FilterMenuLayout layout = (FilterMenuLayout) findViewById(R.id.filter_menu);
        FilterMenu menu = new FilterMenu.Builder(this)
                .addItem(R.drawable.store)
                .addItem(R.drawable.rate)
                .addItem(R.drawable.info)

                .attach(layout)
                .withListener(new FilterMenu.OnMenuChangeListener() {
                    @Override
                    public void onMenuItemClick(View view, int position) {
                        if (isCameraOn) {
                            turnOffCamera();
                        }
                        if (position == 0) {
                            Uri uri = Uri.parse(STORE_URL);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);

                            mTracker.setScreenName("Store Menu");
                            mTracker.send(new HitBuilders.ScreenViewBuilder().build());

                        } else if (position == 1) {
                            Uri uri = Uri.parse(APP_URL + PACKAGE_NAME);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                            mTracker.setScreenName("App Menu");
                            mTracker.send(new HitBuilders.ScreenViewBuilder().build());


                        } else if (position == 2) {
                            Intent i = new Intent(MainActivity.this, WebViewActivity.class);
                            startActivity(i);
                            mTracker.setScreenName("Licences Menu");
                            mTracker.send(new HitBuilders.ScreenViewBuilder().build());

                        }
                    }

                    @Override
                    public void onMenuCollapse() {
                    }

                    @Override
                    public void onMenuExpand() {
                    }
                })
                .build();

		/*
         * First check if device is supporting flashlight or not
		 */
        hasFlash = this.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash) {
            // device doesn't support flash
            // Show alert message and close the application
            AlertDialog alert = new AlertDialog.Builder(this)
                    .create();
            alert.setTitle(getString(R.string.FlashError));
            alert.setMessage(getString(R.string.FlashErrorMsg));
            alert.setButton(Dialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // closing the application
                    finish();
                }
            });
            alert.show();
            return;
        }
        // get the camera
        getCamera();

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCameraOn) {
                    turnOnCamera();
                } else {
                    turnOffCamera();
                }
            }
        });

        // displaying button image switch
        toggleButtonImageSwitch();

		/*
         * Switch button click event to toggle flash on/off
		 */
        btnSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    // turn off flash
                    turnOffFlash();
                } else {
                    // turn on flash
                    turnOnFlash();
                }
            }
        });
        setupLayoutAdmob();
        // showInterstitialAd();
    }

    /*
    * Preview the camera
     */

    /*
     * Get the camera
     */
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {

            }
        }
    }

    private void previewCamera() {
        mPreview = new CameraPreview(getApplicationContext(), camera);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } catch (Exception e) {

            }

        }
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        preview.addView(mPreview);
    }

    /*
     * Turning On flash
     */
    private void turnOnFlash() {
        if (!batteryOk) {
            showBatteryLowDialog();
            return;
        }
        getCamera();
        Log.d("Flash on?", "============+>" + String.valueOf(isFlashOn));
        if (!isFlashOn) {
            if (camera == null || params == null) {
                isFlashOn = false;
                return;
            }
            // play sound
            playSound();
            try {
                if (freq != 0) {
                    sr = new StroboRunner();
                    t = new Thread(sr);
                    t.start();
                } else {
                    params = camera.getParameters();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    camera.setParameters(params);
                    camera.startPreview();
                }
            } catch (Exception e) {
                // Do nothing;
            }

            isFlashOn = true;
            // changing button/switch image
            toggleButtonImageSwitch();
        }

    }

    /*
     * Turning Off flash
     */
    private void turnOffFlash() {
        getCamera();
        Log.d("Flash on?", "============+>" + String.valueOf(isFlashOn));
        if (isFlashOn) {
            if (camera == null || params == null) {
                isFlashOn = false;
                return;
            }
            // play sound
            playSound();
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            if (!isCameraOn) {
                mPreview = null;
                camera.stopPreview();
            }
            if (t != null) {
                sr.stopRunning = true;
                t = null;
            }
            isFlashOn = false;
            // changing button/switch image
            toggleButtonImageSwitch();
        }
    }

    /*
    Turning on camera preview
     */
    private void turnOnCamera() {
        // play sound
        playSound();
        isCameraOn = true;
        turnOffFlash();
        strobo.setChecked(false);
        previewCamera();
        toggleButtonImageCamera();
    }

    /*
   Turning on camera preview
    */
    private void turnOffCamera() {
        playSound();
        preview.removeAllViews();
        params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(params);
        camera.stopPreview();
        camera.release();
        camera = null;
        isCameraOn = false;
        strobo.setChecked(false);
        turnOffFlash();
        toggleButtonImageCamera();

    }

    /*
     * Playing sound
     * will play button toggle sound on flash on / off
     * */
    private void playSound() {
        if (isFlashOn) {
            mp = MediaPlayer.create(this, R.raw.light_switch_off);
        } else {
            mp = MediaPlayer.create(this, R.raw.light_switch_on);
        }
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        });
        mp.start();
    }

    /*
     * Toggle switch button images
     * changing image states to on / off
     * */
    public void toggleButtonImageSwitch() {
        if (isFlashOn) {
            btnSwitch.setImageResource(R.drawable.btn_on);
        } else {
            btnSwitch.setImageResource(R.drawable.btn_off);
        }
    }

    /*
     * Toggle switch button images Camera
     * changing image states to on / off
     * */
    public void toggleButtonImageCamera() {
        if (isCameraOn) {
            btnCamera.setImageResource(R.drawable.cam_on);
        } else {
            btnCamera.setImageResource(R.drawable.cam_off);
        }
    }

    public void showInterstitialAd() {
        boolean showAd = SHOW_AD;
        if (showAd && isOnline(this)) {
            interstitialAd = new InterstitialAd(this);
            interstitialAd.setAdUnitId(AD_INTERSTITIAL_UNIT_ID);
            AdRequest adRequest = new AdRequest.Builder().build();
            interstitialAd.loadAd(adRequest);
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    interstitialAd.show();
                }
            });

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setupLayoutAdmob() {
        RelativeLayout adLayout = (RelativeLayout) findViewById(R.id.adView);
        if (isOnline(this)) {
            boolean showAd = Constants.SHOW_AD;
            if (showAd) {
                adView = new AdView(this);
                adView.setAdUnitId(AD_BANNER_UNIT_ID);
                adView.setAdSize(AdSize.SMART_BANNER);
                adLayout.addView(adView);
                AdRequest mAdRequest = new AdRequest.Builder().build();
                adView.loadAd(mAdRequest);
                return;

            }
        }
        adLayout.setVisibility(View.INVISIBLE);
    }

    public void showBatteryLowDialog() {

        AlertDialog alert = new AlertDialog.Builder(this)
                .create();
        alert.setTitle(getString(R.string.BatteryLow));
        alert.setMessage(getString(R.string.CantUseFlash));
        alert.setButton(Dialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // on starting the app get the camera params
        getCamera();
        mTracker.setScreenName(MainActivity.class.getName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onResume() {
        super.onResume();
        mTracker.setScreenName(MainActivity.class.getName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // on resume turn on the flash
        if (hasFlash)
            turnOnFlash();
        if (isCameraOn)
            turnOnCamera();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(this.mBatInfoReceiver);
        if (isCameraOn)
            turnOffCamera();
        if (camera != null) {
            camera.release();
            camera = null;
        }
        // on pause turn off the flash
        turnOffFlash();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isCameraOn)
            turnOffCamera();
        // on stop release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class StroboRunner implements Runnable {
        boolean stopRunning = false;
        int freqn;

        @Override
        public void run() {
            Camera.Parameters paramsOn = camera.getParameters();
            Camera.Parameters paramsOff = params;
            paramsOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            paramsOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            try {
                while (!stopRunning) {
                    this.freqn = freq;
                    camera.setParameters(paramsOn);
                    camera.startPreview();
                    // We make the thread sleeping
                    Thread.sleep(100 - freq / 100);
                    camera.setParameters(paramsOff);
                    camera.startPreview();
                    Thread.sleep(freq);
                }
            } catch (Throwable t) {
            }
        }
    }
}