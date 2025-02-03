package com.urban.mobileapp.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.urban.mobileapp.db.entity.StopDB;

import java.util.List;

@Dao
public interface StopDao {
    @Insert
    void insert(StopDB stopDB);

    @Query("SELECT * FROM stops")
    List<StopDB> getAllStops();

    @Query("DELETE FROM stops")
    void deleteAllStops();
}
