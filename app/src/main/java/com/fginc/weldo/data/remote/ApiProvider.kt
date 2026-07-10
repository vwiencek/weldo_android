package com.fginc.weldo.data.remote

import com.fginc.weldo.data.local.WeldoSession
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds [WeldoApi] against the session's current base URL and rebuilds transparently when
 * the user changes it in Settings. The bearer + Apple-user headers are added per request from
 * [WeldoSession], so a fresh token takes effect without rebuilding.
 */
class ApiProvider(private val session: WeldoSession) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false      // drop null fields (server owns id/createdAt/...)
        encodeDefaults = true      // still send non-null defaults (completed=false, detail="")
        coerceInputValues = true   // tolerate server null for a non-null-with-default field
    }

    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedApi: WeldoApi? = null

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        session.token.value?.let { builder.header("Authorization", "Bearer $it") }
        session.appleUserId.value?.let { builder.header("X-Apple-User-Identifier", it) }
        chain.proceed(builder.build())
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // /capture can be slow (LLM round-trip)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun api(): WeldoApi {
        val base = session.baseUrl.value.trimEnd('/') + "/"
        val current = cachedApi
        if (current != null && base == cachedBaseUrl) return current
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        val api = retrofit.create(WeldoApi::class.java)
        cachedBaseUrl = base
        cachedApi = api
        return api
    }
}
