package com.localiza.uniforads.model

import java.io.Serializable

data class Address(
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val places: MutableList<Place> = mutableListOf()
) : Serializable