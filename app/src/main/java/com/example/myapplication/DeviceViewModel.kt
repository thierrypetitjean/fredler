package com.example.myapplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.Device

class DeviceViewModel : ViewModel() {

    // Create a LiveData with a String
    val currentDev: MutableLiveData<Device> by lazy {
        MutableLiveData<Device>()
    }

    // Rest of the ViewModel...
}