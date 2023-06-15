package com.example.myapplication.data

import java.io.Serializable

data class Presets (

    var description      : String?    = null,
    var name      : String?    = null,
    var value     : Int? = null

) : Serializable