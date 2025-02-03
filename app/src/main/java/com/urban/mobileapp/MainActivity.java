package com.urban.mobileapp;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.urban.mobileapp.db.entity.StopDB;
import com.urban.mobileapp.model.Bus;
import com.urban.mobileapp.model.Stop;
import com.urban.mobileapp.service.BusApi;
import com.urban.mobileapp.utils.GeocoderHelper;
import com.urban.mobileapp.utils.LocationHelper;
import com.urban.mobileapp.service.RetrofitClient;

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

    private TextView tvTilt, tvTime, tvCurrentStop, tvAddress, tvAccuracy, tvId, tvVariant;
    private LocationHelper locationHelper;
    private GeocoderHelper geocoderHelper;
    private final Handler handler = new Handler();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaPlayer mediaPlayer;
    private String currentStopName = null;
    private String variantName = "";

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
            tvId = findViewById(R.id.tvId);
            tvVariant = findViewById(R.id.tvVariant);


            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );

            // Utrzymywanie ekranu włączonego
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            setAndroidId(this);
            checkNotificationPermission();
            setHour();

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
            List<StopDB> stopDBS = stopDao.getAllStops();

            handler.post(() -> {
                checkStopsProximity(currentLocation, stopDBS);
            });
        });

    }

    @SuppressLint("SetTextI18n")
    private void checkStopsProximity(Location currentLocation, List<StopDB> stopDBS) {
     for (StopDB stopDB : stopDBS) {
         Location stopLocation = new Location("");
         stopLocation.setLatitude(stopDB.getLat());
         stopLocation.setLongitude(stopDB.getLon());

         float distance = currentLocation.distanceTo(stopLocation);

         if (distance <= 10) {

             String stopName = stopDB.getName();

             if (!stopName.equals(currentStopName)) {
                 tvCurrentStop.setText(stopDB.getName());
                 String mp3Name = getAudioFileName(stopName);
                 Log.d("MP3", mp3Name);

                 int resourceId = getResources().getIdentifier(
                         mp3Name,
                         "raw",
                         getPackageName()
                 );


                 if (resourceId != 0) {
                     if (mediaPlayer != null) {
                         mediaPlayer.release();
                     }
                     mediaPlayer = MediaPlayer.create(this, resourceId);
                     mediaPlayer.start();
                 } else {
                     Log.e("MP3", "Brak pliku: " + mp3Name);
                 }
                 currentStopName = stopName;
             }
             return;
         }
     }
     currentStopName = "Brak przystanku w pobliżu";
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

    private void getApiData(String androidId) {
        if (androidId != null && androidId.isEmpty()) {
            Log.e("API_ERROR", "Android ID is null or empty");
            return;
        }

        BusApi busApi = RetrofitClient.getRetrofitInstance().create(BusApi.class);
        busApi.getBusById(androidId).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(@NonNull Call<Bus> call, @NonNull Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bus bus = response.body();
                    Log.d("BUS_DETAILS", "Bus: " + bus.getId() + " Model: " + bus.getModel());

                    if (bus.getStops() != null && !bus.getStops().isEmpty()) {
                        updateStopsInDB(bus.getStops());
                    }
                } else {
                    Log.e("API_ERROR", "Kod błędu: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Bus> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Error: " + t.getMessage());
            }
        });
    }

    private void updateStopsInDB(List<Stop> stops) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            StopDao stopDao = db.stopDao();

            stopDao.deleteAllStops();

            for (Stop stop: stops) {
                if (!variantName.equals(stop.getLine())) {
                    variantName = stop.getLine();
                    tvVariant.setText(variantName);
                }
                stopDao.insert(stop.toStopDB());
            }

            List<StopDB> updatedStops = stopDao.getAllStops();
            Log.d("DATABASE", "Zaktualizowane przystanki:");
            for (StopDB stop : updatedStops) {
                Log.d("DATABASE", String.format(
                        "Przystanek: %s (Linia %s) @ %.6f, %.6f",
                        stop.getName(),
                        stop.getLine(),
                        stop.getLat(),
                        stop.getLon()
                ));
            }
        });
    }

    private static String getAudioFileName(String stopName) {
        String name = stopName.replace("Stacja ", "").replace(" ", "");
        name = replacePolishCharacters(name);
        return name.toLowerCase();
    }

    private static String replacePolishCharacters(String input) {
     return input
             .replace("ł", "l")
             .replace("Ł", "L")
             .replace("ą", "a")
             .replace("ę", "e")
             .replace("ć", "c")
             .replace("ś", "s")
             .replace("ź", "z")
             .replace("ż", "z")
             .replace("ń", "n")
             .replace("ó", "o");
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Wymagane zezwolenie na nakładkę!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setAndroidId(Context context) {
        @SuppressLint("HardwareIds") String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        getApiData(id);
        tvId.setText(id);
        Log.d("AndroidId", "Android ID: " + id);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(timeUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.moveTaskToFront(getTaskId(), 0);
        handler.removeCallbacks(timeUpdater);
        locationHelper.stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        if(mediaPlayer != null) {
            mediaPlayer.release(); // Zwolnij zasoby
            mediaPlayer = null;
        }
    }
}
