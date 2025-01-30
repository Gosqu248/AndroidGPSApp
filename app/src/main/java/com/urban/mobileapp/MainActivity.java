package com.urban.mobileapp;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;


public class MainActivity extends AppCompatActivity  {

    private TextView tvCoordinates, tvAddress;
    private LocationHelper locationHelper;
    private GeocoderHelper geocoderHelper;

    private final ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    locationHelper.startLocationUpdates();
                } else {
                    Toast.makeText(this, "Wymagane zezwolenie na lokalizację", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCoordinates = findViewById(R.id.tvCoordinates);
        tvAddress = findViewById(R.id.tvAddress);

        locationHelper = new LocationHelper(this);
        geocoderHelper = new GeocoderHelper(this);

        locationHelper.setLocationUpdateListener(this::updateLocation);

        //Sprawdzenie uprawnień
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates();
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
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

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
    }
}