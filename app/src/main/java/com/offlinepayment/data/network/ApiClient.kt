package com.offlinepayment.data.network

import com.offlinepayment.BuildConfig
import com.offlinepayment.data.session.AuthSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    private val authHeaderInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        AuthSessionManager.currentSession()?.let { session ->
            builder.header("Authorization", "Bearer ${session.accessToken}")
            builder.header("x-device-fingerprint", session.deviceFingerprint)
        }
        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authHeaderInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttp)
        .build()

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val walletApi: WalletApi by lazy { retrofit.create(WalletApi::class.java) }
}

