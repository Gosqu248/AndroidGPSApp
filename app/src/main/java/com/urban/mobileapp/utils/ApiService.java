package com.urban.mobileapp.utils;

import android.annotation.SuppressLint;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService  extends Service {

    private final IBinder binder = new LocalBinder();
    private ScheduledExecutorService executor;
    private OkHttpClient client;

    public class LocalBinder extends Binder {
        ApiService getService() {
            return ApiService.this;
        }
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        startPooling();
    }

    private void startPooling() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::fetchData, 0, 5, TimeUnit.MINUTES);
    }

    private void fetchData() {
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/hello")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String result = response.body().string();
                DataRepository.getInstance().setApiResponse(result);
                Log.d("ApiService", "Odpowiedź serwera: " + result);
            }
        } catch (IOException e) {
            Log.e("ApiService", "Błąd podczas pobierania danych ", e);
        }
    }

    @Override
    public void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
