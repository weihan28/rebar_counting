package com.fyp.rebarcountingapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .readTimeout(5, TimeUnit.MINUTES)      // Time to wait for server to send full response
        .build()

    val api: RebarApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://rebar-count.onrender.com")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .create(RebarApi::class.java)
    }
}
