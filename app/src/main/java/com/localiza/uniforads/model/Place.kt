package com.localiza.uniforads.model

import java.io.Serializable

data class Place(
    val name: String = "",
    val type: String = ""
) : Serializable