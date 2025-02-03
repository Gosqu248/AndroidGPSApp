package com.urban.mobileapp.service;
import retrofit2.converter.gson.GsonConverterFactory;

import retrofit2.Retrofit;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "http://192.168.0.31:8080/api/";

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
