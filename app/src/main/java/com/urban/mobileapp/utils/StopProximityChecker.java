package com.urban.mobileapp.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

import com.urban.mobileapp.db.AppDatabase;
import com.urban.mobileapp.db.dao.StopDao;
import com.urban.mobileapp.db.entity.StopDB;


public class StopProximityChecker {
    private final Context context;
    private final TextView tvCurrentStop;
    private final AudioPlayerManager audioPlayerManager;
    private String currentStopName = null;

    public StopProximityChecker(Context context, TextView tvCurrentStop) {
        this.context = context;
        this.tvCurrentStop = tvCurrentStop;
        this.audioPlayerManager = new AudioPlayerManager(context);
    }

    @SuppressLint("SetTextI18n")
    public void checkNearbyStops(final Location currentLocation) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            StopDao stopDao = db.stopDao();
            List<StopDB> stops = stopDao.getAllStops();

            mainHandler.post(() -> {
                boolean found = false;

                for (StopDB stop : stops) {
                    Location stopLocation = new Location("");
                    stopLocation.setLatitude(stop.getLat());
                    stopLocation.setLongitude(stop.getLon());

                    float distance = currentLocation.distanceTo(stopLocation);
                    float bearing = currentLocation.getBearing() - stop.getBearing();

                    if (distance <= 10 && Math.abs(bearing) <= 10) {
                        if (!stop.getName().equals(currentStopName)) {
                            tvCurrentStop.setText(stop.getName());
                            audioPlayerManager.playStopAnnouncement(stop.getName());
                            currentStopName = stop.getName();
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    currentStopName = "Brak przystanku w pobliżu";
                    tvCurrentStop.setText("Brak przystanku w pobliżu");
                }
        });
    });
}
}
