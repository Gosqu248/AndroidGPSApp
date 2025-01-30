package com.urban.mobileapp.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final DataRepository repository = DataRepository.getInstance();

    public LiveData<String> getApiResponse() {
        return repository.getApiResponse();
    }
}
