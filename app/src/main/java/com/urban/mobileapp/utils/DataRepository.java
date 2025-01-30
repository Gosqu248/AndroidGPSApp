package com.urban.mobileapp.utils;

import androidx.lifecycle.MutableLiveData;

public class DataRepository {
    private static DataRepository instance;
    private final MutableLiveData<String> apiResponse = new MutableLiveData<>();

    public static DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public MutableLiveData<String> getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(String response) {
        apiResponse.postValue(response);
    }
}
