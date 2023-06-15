package com.example.myapplication.data

import java.io.Serializable

data class Access (

    var access      : Int?    = null,
    var description : String? = null,
    var features    : ArrayList<Feutures>? = null,
    var name        : String? = null,
    var presets     : ArrayList<Presets>? = null,
    var property    : String? = null,
    var type        : String? = null,
    var unit        : String? = null,
    var valueMax    : Int?    = null,
    var valueMin    : Int?    = null,
    var value_off   : String? = null,
    var value_on    : String? = null,
    var value_toggle : String? = null

) : Serializable