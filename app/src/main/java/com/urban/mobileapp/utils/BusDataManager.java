package com.urban.mobileapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.urban.mobileapp.db.AppDatabase;
import com.urban.mobileapp.db.dao.StopDao;
import com.urban.mobileapp.db.entity.StopDB;
import com.urban.mobileapp.model.Bus;
import com.urban.mobileapp.model.Stop;
import com.urban.mobileapp.service.BusApi;
import com.urban.mobileapp.service.RetrofitClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class BusDataManager {
    private final Context context;
    private final TextView tvVariant;

    public BusDataManager(Context context, TextView tvVariant) {
        this.context = context;
        this.tvVariant = tvVariant;
    }

    public void fetchBusData(String androidId) {
        if (androidId == null || androidId.isEmpty()) {
            Log.e("BusDataManager", "Android ID jest pusty lub null");
            return;
        }

        BusApi busApi = RetrofitClient.getRetrofitInstance().create(BusApi.class);
        busApi.getBusById(androidId).enqueue(new Callback<Bus>() {
            @Override
            public void onResponse(@NonNull Call<Bus> call, @NonNull Response<Bus> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bus bus = response.body();
                    Log.d("BusDataManager", "Dane autobusu: " + bus.getId() + " Model: " + bus.getModel());

                    if (bus.getStops() != null && !bus.getStops().isEmpty()) {
                        updateStopsInDB(bus.getStops());
                    }
                } else {
                    Log.e("BusDataManager", "Błąd API, kod: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Bus> call, @NonNull Throwable t) {
                Log.e("BusDataManager", "Błąd API: " + t.getMessage());

            }
        });
    }

    private void updateStopsInDB(List<Stop> stops) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            StopDao stopDao = db.stopDao();

            stopDao.deleteAllStops();

            String variantName = "";
            Handler mainHandler = new Handler(Looper.getMainLooper());
            for (Stop stop : stops) {
                if (!variantName.equals(stop.getLine())) {
                    variantName = stop.getLine();
                    String finalVariantName = variantName;
                    mainHandler.post(() -> tvVariant.setText(finalVariantName));
                }
                stopDao.insert(stop.toStopDB());
            }

            List<StopDB> updatedStops = stopDao.getAllStops();
            Log.d("BusDataManager", "Zaktualizowane przystanki:");
            for (StopDB stop : updatedStops) {
                Log.d("BusDataManager", String.format("Przystanek: %s (Linia %s) @ %.6f, %.6f",
                        stop.getName(), stop.getLine(), stop.getLat(), stop.getLon()));
            }
        });
    }
}
