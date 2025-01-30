package com.urban.mobileapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Looper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class GeocoderHelper {
    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new android.os.Handler(Looper.getMainLooper());

    public interface GeocoderListener {
        void onAddressFound(String address);
        void onError(String errorMessage);
    }

    public GeocoderHelper(Context context) {
        this.context = context;
    }

    public void getAddressFromLocation(double latitude, double longitude, GeocoderListener listener) {
        executorService.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    String address = addresses.get(0).getAddressLine(0);
                    mainHandler.post(() -> listener.onAddressFound(address));
                } else {
                    mainHandler.post(() -> listener.onError("Nie znaleziono adresu"));
                }

            } catch (IOException e ) {
                mainHandler.post(() -> listener.onError("Błąd geokodowania"));
            }
        });
    }


}
