package com.urban.mobileapp.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.urban.mobileapp.db.dao.StopDao;
import com.urban.mobileapp.db.entity.Stop;


@Database(entities = {Stop.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract StopDao stopDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "stops_database"
            ).build();
        }
        return instance;
    }
}