package com.urban.mobileapp.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class AudioPlayerManager {
    private MediaPlayer mediaPlayer;
    private final Context context;

    public AudioPlayerManager(Context context) {
        this.context = context;
    }

    public void playStopAnnouncement(String stopName) {
        String mp3Name = getAudioFileName(stopName);
        Log.d("AudioPlayerManager", "Próba odtworzenia pliku: " + mp3Name);

        int resourceId = context.getResources().getIdentifier(
                mp3Name,
                "raw",
                context.getPackageName()
        );

        if (resourceId != 0) {
            release();
            mediaPlayer = MediaPlayer.create(context, resourceId);
            mediaPlayer.start();
        } else {
            Log.e("AudioPlayerManager", "Brak pliku audio: " + mp3Name);
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private String getAudioFileName(String stopName) {
        String name = stopName.replace("Stacja ", "").replace(" ", "");
        name = replacePolishCharacters(name);
        return name.toLowerCase();
    }

    private String replacePolishCharacters(String input) {
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
}
