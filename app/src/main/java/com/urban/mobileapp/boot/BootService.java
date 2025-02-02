package com.urban.mobileapp.boot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.urban.mobileapp.MainActivity;
import com.urban.mobileapp.R;

public class BootService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "boot_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Boot Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Uruchamianie aplikacji")
                .setContentText("Proszę czekać...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMainActivity();
        scheduleSelfDestruction();
        return START_NOT_STICKY;
    }

    private void startMainActivity() {
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);
    }

    private void scheduleSelfDestruction() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopForeground(true);
            stopSelf();
        }, 3000);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
