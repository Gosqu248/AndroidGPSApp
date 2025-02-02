package com.urban.mobileapp.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.urban.mobileapp.db.entity.Stop;

import java.util.List;

@Dao
public interface StopDao {
    @Insert
    void insert(Stop stop);

    @Query("SELECT * FROM stops")
    List<Stop> getAllStops();
}
