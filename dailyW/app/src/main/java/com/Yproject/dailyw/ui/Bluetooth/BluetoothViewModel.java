package com.Yproject.dailyw.ui.Bluetooth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BluetoothViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public BluetoothViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
