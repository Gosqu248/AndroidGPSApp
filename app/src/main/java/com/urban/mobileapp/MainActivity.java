package com.urban.mobileapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urban.mobileapp.utils.ApiService;
import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;


public class MainActivity extends AppCompatActivity  {

    private TextView tvCoordinates, tvAddress, tvHello;
    private LocationHelper locationHelper;
    private GeocoderHelper geocoderHelper;
    private BroadcastReceiver dataReceiver;

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);

            tvCoordinates = findViewById(R.id.tvCoordinates);
            tvAddress = findViewById(R.id.tvAddress);
            tvHello = findViewById(R.id.tvHello);

            checkNotificationPermission();

            setUpDataReceiver(); // Register receiver first
            startService(new Intent(this, ApiService.class)); // Then start service

            locationHelper = new LocationHelper(this);
            geocoderHelper = new GeocoderHelper(this);

            locationHelper.setLocationUpdateListener(this::updateLocation);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationHelper.startLocationUpdates();
            } else {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Wystąpił błąd", e);
        }
    }

    private void updateLocation(Location location) {
        String coordinates = "Szerokość: " + location.getLatitude() + "\nDługość: " + location.getLongitude();
        tvCoordinates.setText(coordinates);

        geocoderHelper.getAddressFromLocation(location.getLatitude(), location.getLongitude(), new GeocoderHelper.GeocoderListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onAddressFound(String address) {
                tvAddress.setText("Aktualny address: " + address);
                Log.d("UpdatedAddress", "Aktualny address " + address);

            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setUpDataReceiver() {
        dataReceiver = new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = intent.getStringExtra("result");
                runOnUiThread(() -> {
                    tvHello.setText("Odpowiedź serwera: " + result);
                    Log.d("API", "Odpowiedź serwera: " + result);
                });
            }
        };
        registerReceiver(dataReceiver, new IntentFilter("DATA_UPDATE_ACTION"), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
        unregisterReceiver(dataReceiver);
    }
}