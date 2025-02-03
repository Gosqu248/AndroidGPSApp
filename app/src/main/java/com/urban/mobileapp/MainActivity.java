package com.urban.mobileapp;

import android.annotation.SuppressLint;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urban.mobileapp.utils.AudioPlayerManager;
import com.urban.mobileapp.utils.BusDataManager;
import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;
import com.urban.mobileapp.utils.StopProximityChecker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextView tvTilt;
    private TextView tvTime;
    private TextView tvAddress;
    private TextView tvAccuracy;
    private TextView tvId;
    private LocationHelper locationHelper;
    private GeocoderHelper geocoderHelper;
    private AudioPlayerManager audioPlayerManager;
    private StopProximityChecker stopProximityChecker;
    private BusDataManager busDataManager;

    private final Handler timeHandler = new Handler();
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            setHour();
            timeHandler.postDelayed(this, 1000);
        }
    };


    private final ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    locationHelper.startLocationUpdates();
                } else {
                    Toast.makeText(this, "Wymagane zezwolenie na lokalizację", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Powiadomienia będą wyłączone", Toast.LENGTH_SHORT).show();
                }
            });

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTilt = findViewById(R.id.tvTilt);
        tvTime = findViewById(R.id.tvTime);
        TextView tvCurrentStop = findViewById(R.id.tvNow);
        tvAddress = findViewById(R.id.tvAddress);
        tvAccuracy = findViewById(R.id.tvAccuracy);
        tvId = findViewById(R.id.tvId);
        TextView tvVariant = findViewById(R.id.tvVariant);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        audioPlayerManager = new AudioPlayerManager(this);
        stopProximityChecker = new StopProximityChecker(this, tvCurrentStop);
        busDataManager = new BusDataManager(this, tvVariant);
        locationHelper = new LocationHelper(this);
        geocoderHelper = new GeocoderHelper(this);

        locationHelper.setLocationUpdateListener(this::updateLocation);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates();
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        setAndroidId();
        setHour();
        checkNotificationPermission();
    }

    private void updateLocation(Location location) {
        if (location == null) return;

        float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
        float accuracy = location.hasAccuracy() ? location.getAccuracy() : 0.0f;

        geocoderHelper.getAddressFromLocation(location.getLatitude(), location.getLongitude(), new GeocoderHelper.GeocoderListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onAddressFound(String address) {
                tvAddress.setText(address);
                tvTilt.setText(String.valueOf(bearing));
                tvAccuracy.setText(accuracy + " m");
                Log.d("UpdatedAddress", "Aktualny address " + address);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        stopProximityChecker.checkNearbyStops(location);
    }

    private void setHour() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvTime.setText(currentTime);
    }

    private void setAndroidId() {
        @SuppressLint("HardwareIds") String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvId.setText(id);
        Log.d("AndroidId", "Android ID: " + id);
        busDataManager.fetchBusData(id);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timeHandler.post(timeUpdater);
        locationHelper.startLocationUpdates();

    }

    @Override
    protected void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeUpdater);
        locationHelper.stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioPlayerManager.release();
    }
}
