package com.urban.mobileapp;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.urban.mobileapp.db.AppDatabase;
import com.urban.mobileapp.db.dao.StopDao;
import com.urban.mobileapp.db.entity.Stop;
import com.urban.mobileapp.model.Bus;
import com.urban.mobileapp.service.BusApi;
import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;
import com.urban.mobileapp.utils.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvTilt, tvTime, tvCurrentStop, tvAddress, tvAccuracy;
    private LocationHelper locationHelper;
    private GeocoderHelper geocoderHelper;
    private final Handler handler = new Handler();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            setHour();
            handler.postDelayed(this, 1000);
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
        try {
            setContentView(R.layout.activity_main);

            tvTilt = findViewById(R.id.tvTilt);
            tvTime = findViewById(R.id.tvTime);
            tvCurrentStop = findViewById(R.id.tvNow);
            tvAddress = findViewById(R.id.tvAddress);
            tvAccuracy = findViewById(R.id.tvAccuracy);

            checkNotificationPermission();
            setHour();
            getApiData();
            getDB();

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


    private void checkNearbyStops(Location currentLocation) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            StopDao stopDao = db.stopDao();
            List<Stop> stops = stopDao.getAllStops();

            handler.post(() -> {
                checkStopsProximity(currentLocation, stops);
            });
        });

    }

    @SuppressLint("SetTextI18n")
    private void checkStopsProximity(Location currentLocation, List<Stop> stops) {
     for (Stop stop : stops) {
         Location stopLocation = new Location("");
         stopLocation.setLatitude(stop.getLat());
         stopLocation.setLongitude(stop.getLon());

         float distance = currentLocation.distanceTo(stopLocation);

         if (distance - currentLocation.getAccuracy() <= 10) {
             tvCurrentStop.setText(stop.getName());
             return;
         }
     }
     tvCurrentStop.setText("Brak przystanku w pobliżu");
    }

    private void updateLocation(Location location) {
        if (location == null) return;

        String coordinates = "Szerokość: " + location.getLatitude() + "\nDługość: " + location.getLongitude();
        Log.d("Coordinates", coordinates);

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
                Log.d("UpdatedBearing", "Aktualny kąt " + bearing);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

        });
        checkNearbyStops(location);
    }

    private void setHour() {
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        tvTime.setText(currentTime);
    }

    private void getApiData() {
        BusApi busApi = RetrofitClient.getRetrofitInstance().create(BusApi.class);

        busApi.getBusById(1L).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(@NonNull Call<Bus> call, @NonNull Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bus bus = response.body();
                    Log.d("BUS_DETAILS", "Bus: " + bus.getId() + " Line: " + bus.getLineNumber());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Bus> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Error: " + t.getMessage());
            }
        });
    }

    private void getDB() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            // Get database instance
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            StopDao stopDao = db.stopDao();

            // Insert stops
            stopDao.insert(new Stop("Stacja wujek Łukasz", 49.84942067562159, 20.74343013571693));
            stopDao.insert(new Stop("Stacja wujek Wojtek", 49.84926291735035, 20.74384359629674));
            stopDao.insert(new Stop("Stacja wujek Władek", 49.84943412785344, 20.744145156994847));
            stopDao.insert(new Stop("Stacja krzyżówka", 49.849587299658324, 20.745886310070112));
            stopDao.insert(new Stop("Stacja wały", 49.85008277540994, 20.746635381059967));

            // Fetch and log all stops
            List<Stop> stops = stopDao.getAllStops();
            for (Stop stop : stops) {
                Log.d("DATABASE", "Stop: ID=" + stop.getId() +
                        ", Name=" + stop.getName() +
                        ", Lat=" + stop.getLat() +
                        ", Lon=" + stop.getLon());
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
    protected void onResume() {
        super.onResume();
        handler.post(timeUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(timeUpdater);
        locationHelper.stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
