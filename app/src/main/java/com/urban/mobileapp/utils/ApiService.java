package com.urban.mobileapp.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService  extends Service {
    private static final int NOTIFICATION_ID = 1;
    private final IBinder binder = new LocalBinder();
    private ScheduledExecutorService executor;
    private OkHttpClient client;

    public class LocalBinder extends Binder {
        AppService getService() {
            return ApiService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        startForeground(NOTIFICATION_ID, createNotification());
        startPooling();
    }

    private void startPooling() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::fetchData, 0, 5, TimeUnit.MINUTES);
    }

    private void fetchData
}
