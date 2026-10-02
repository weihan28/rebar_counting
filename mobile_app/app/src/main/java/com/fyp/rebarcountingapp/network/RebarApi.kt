package com.fyp.rebarcountingapp.network

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class CountResponse(
    val rebarCount: Int,
    val annotated_image: String
)


interface RebarApi {
    @Multipart
    @POST("/count/")
    suspend fun countRebars(
        @Part image: MultipartBody.Part
    ): CountResponse
}
