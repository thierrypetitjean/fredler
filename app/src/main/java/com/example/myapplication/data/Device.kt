package com.example.myapplication.data

import java.io.Serializable


data class Device(
    var model: String? = null,
    var description: String? = null,
    var devicename: String? = null,
    var ieee: String? = null,
    var access: ArrayList<Access>? = null,
    var payload: Payload? = null

    ) : Serializable