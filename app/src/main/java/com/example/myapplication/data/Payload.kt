package com.example.myapplication.data

import java.io.Serializable

data class Payload (

    var brightness      : Int?    = null,
    var color : Colors? = null,
    var color_mode : String? = null,
    var color_temp: Int? = null,
    var linkquality: Int? = null,
    var state : String? = null


    ) : Serializable