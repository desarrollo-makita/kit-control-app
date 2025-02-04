package com.makita.ubiapp

import com.makita.kitcontrolapp.ui.component.CodigoData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class KitItem(
    val tipoitem: String? = null,
    val item: String,
    val Clasif7: String? = null,
    val Clasif9: String? = null
)

data class EnvioDatosRequest(
    val selectedItem: String,
    val listaCodigos: List<CodigoData>
)

data class EnvioCabeceraKitRequest(
    val ItemKitID : String,
    val ean : String,
)

data class ResponseCabeceraKit(
    val status: String,
    val message: String,
    val serieDesde: String,
    val serieHasta: String
)

data class ResponseApi(
    val itemEncontrado: ResponseDetalleKit
)
data class ResponseDetalleKit(
    val item: String,
    val serieInicio: String,
    val serieHasta: String,
    val letraFabrica: String,
    val ean: String,
    val proceso: Int
)

interface ApiService {
    @GET("api/get-lista-kit/{item}")
    suspend fun obtenerListaKit(@Path("item") item: String): List<KitItem>


    @POST("api/inserta-data-kit-detalle")  // Endpoint de la API
    suspend fun insertaDataDetalle(@Body data: EnvioDatosRequest): ResponseApi

    @POST("api/inserta-data-cabecera-kit")  // Endpoint de la API
    suspend fun insertaDataCabecera(@Body data: EnvioCabeceraKitRequest): ResponseCabeceraKit
}
