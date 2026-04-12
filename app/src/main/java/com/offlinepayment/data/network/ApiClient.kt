package com.offlinepayment.data.network

import com.offlinepayment.BuildConfig
import com.offlinepayment.data.session.AuthSessionManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.math.BigDecimal
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    private val authHeaderInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        AuthSessionManager.currentSession()?.let { session ->
            val token = session.accessToken
            if (!token.isNullOrBlank() && !token.startsWith("offline_token")) {
                builder.header("Authorization", "Bearer $token")
            }
            builder.header("x-device-fingerprint", session.deviceFingerprint)
        }
        builder.header("Accept", "application/json")
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        redactHeader("x-device-fingerprint")
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Time to establish connection
        .readTimeout(60, TimeUnit.SECONDS)    // Time to wait for response (increased for slow Render servers)
        .writeTimeout(30, TimeUnit.SECONDS)   // Time to send request
        .addInterceptor(authHeaderInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    // Custom BigDecimal adapter for Moshi
    private object BigDecimalAdapterFactory : JsonAdapter.Factory {
        override fun create(type: java.lang.reflect.Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
            if (type != BigDecimal::class.java) {
                return null
            }
            return object : JsonAdapter<BigDecimal>() {
                override fun fromJson(reader: JsonReader): BigDecimal? {
                    return when (reader.peek()) {
                        JsonReader.Token.NULL -> {
                            reader.skipValue()
                            null
                        }
                        JsonReader.Token.STRING -> {
                            val value = reader.nextString()
                            try {
                                BigDecimal(value)
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }
                        JsonReader.Token.NUMBER -> {
                            val value = reader.nextDouble()
                            BigDecimal.valueOf(value)
                        }
                        else -> {
                            reader.skipValue()
                            null
                        }
                    }
                }

                override fun toJson(writer: JsonWriter, value: BigDecimal?) {
                    if (value == null) {
                        writer.nullValue()
                    } else {
                        writer.value(value.toPlainString())
                    }
                }
            }
        }
    }

    private val moshi = Moshi.Builder()
        .add(BigDecimalAdapterFactory)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttp)
        .build()

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val walletApi: WalletApi by lazy { retrofit.create(WalletApi::class.java) }
    val syncApi: SyncApi by lazy { retrofit.create(SyncApi::class.java) }
}
