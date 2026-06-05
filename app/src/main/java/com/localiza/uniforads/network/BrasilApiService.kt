package com.localiza.uniforads.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface BrasilApiService {
    @GET("cep/v2/{cep}")
    fun getCep(@Path("cep") cep: String): Call<CepResponse>
}