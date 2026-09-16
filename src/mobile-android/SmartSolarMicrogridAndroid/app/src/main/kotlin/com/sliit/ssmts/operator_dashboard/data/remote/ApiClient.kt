/**
 * Description: Factory and builder for creating secure OkHttpClient and Retrofit API instances
 * configured with authentication, header redaction, and timeout policies for Member 4.
 */
package com.sliit.ssmts.operator_dashboard.data.remote

import com.sliit.ssmts.operator_dashboard.data.remote.interceptor.AuthHeaderInterceptor
import com.sliit.ssmts.operator_dashboard.data.remote.interceptor.AuthTokenProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Networking client factory creating production-grade Retrofit and OkHttpClient instances.
 */
object ApiClient {

    private const val DEFAULT_TIMEOUT_SECONDS = 15L

    /**
     * Constructs a configured OkHttpClient with dynamic token injection and header redaction.
     *
     * @param tokenProvider Provider supplying active bearer tokens.
     * @param enableLogging Whether to include OkHttp logging (defaults to true).
     * @return Configured OkHttpClient instance.
     */
    fun createOkHttpClient(
        tokenProvider: AuthTokenProvider = AuthTokenProvider { null },
        enableLogging: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))

        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    /**
     * Constructs the Retrofit service client for OperatorDashboardApi.
     *
     * @param baseUrl Base URL for the central C# Web API (e.g., "https://10.0.2.2:7143/").
     * @param tokenProvider Provider supplying active bearer tokens.
     * @param enableLogging Whether to include OkHttp logging (defaults to true).
     * @param okHttpClient Optional custom OkHttpClient instance (defaults to standard builder).
     * @return Initialized OperatorDashboardApi interface.
     */
    fun createOperatorDashboardApi(
        baseUrl: String,
        tokenProvider: AuthTokenProvider = AuthTokenProvider { null },
        enableLogging: Boolean = true,
        okHttpClient: OkHttpClient? = null
    ): OperatorDashboardApi {
        val client = okHttpClient ?: createOkHttpClient(tokenProvider, enableLogging)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(OperatorDashboardApi::class.java)
    }
}
