package com.urban.mobileapp;

import android.annotation.SuppressLint;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urban.mobileapp.model.Bus;
import com.urban.mobileapp.service.BusApi;
import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;
import com.urban.mobileapp.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvCoordinates, tvAddress, tvBus, tvBearing;
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
        try {
            setContentView(R.layout.activity_main);

            tvCoordinates = findViewById(R.id.tvCoordinates);
            tvAddress = findViewById(R.id.tvAddress);
            tvBus = findViewById(R.id.tvBus);
            tvBearing = findViewById(R.id.tvBearing);

            checkNotificationPermission();
            BusApi busApi = RetrofitClient.getRetrofitInstance().create(BusApi.class);

            busApi.getBusById(1L).enqueue(new Callback<Bus>() {
                @Override
                public void onResponse(@NonNull Call<Bus> call, @NonNull Response<Bus> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Bus bus = response.body();
                        Log.d("BUS_DETAILS", "Bus: " + bus.getId() + " Line: " + bus.getLineNumber());
                        tvBus.setText("Dane autobusu: \nLinia: " + bus.getLineNumber() + " \nModel: " + bus.getModel() + " \nIlość miejsc: " + bus.getCapacity());
                    } else {
                        tvBus.setText("Brak danych autobusu");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Bus> call, @NonNull Throwable t) {
                    Log.e("API_ERROR", "Error: " + t.getMessage());
                    tvBus.setText("Błąd pobierania danych");
                }
            });

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
        if (location == null) return;

        String coordinates = "Szerokość: " + location.getLatitude() + "\nDługość: " + location.getLongitude();
        tvCoordinates.setText(coordinates);

        float bearing = location.hasBearing() ? location.getBearing() : 0.0f;

        geocoderHelper.getAddressFromLocation(location.getLatitude(), location.getLongitude(), new GeocoderHelper.GeocoderListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onAddressFound(String address) {
                tvAddress.setText("Aktualny address: " + address);
                Log.d("UpdatedAddress", "Aktualny address " + address);
                tvBearing.setText("Aktualny kąt: " + bearing);
                Log.d("UpdatedBearing", "Aktualny kąt " + bearing);
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

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
    }
}
