package com.urban.mobileapp.service;

import com.urban.mobileapp.model.Bus;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BusApi {

    @GET("busses/{androidId}")
    Call<Bus> getBusById(@Path("androidId") String androidId);
}
