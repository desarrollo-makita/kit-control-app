package com.makita.ubiapp

import retrofit2.http.GET
import retrofit2.http.Path

data class KitItem(
    val tipoitem: String? = null,
    val item: String,
    val Clasif7: String? = null,
    val Clasif9: String? = null
)

interface ApiService {
    @GET("api/get-lista-kit/{item}")
    suspend fun obtenerListaKit(@Path("item") item: String): List<KitItem>
}
