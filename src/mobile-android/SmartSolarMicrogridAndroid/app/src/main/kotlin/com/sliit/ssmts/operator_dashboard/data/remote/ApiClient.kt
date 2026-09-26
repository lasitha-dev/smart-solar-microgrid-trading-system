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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
        configureDevSsl(builder)

        return builder.build()
    }

    /**
     * Configures development SSL trust manager to allow self-signed localhost/emulator certificates
     * specifically for development loopback addresses (10.0.2.2, localhost, and 127.0.0.1).
     *
     * @param builder OkHttpClient builder to configure.
     */
    private fun configureDevSsl(builder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { hostname, _ ->
                hostname == "10.0.2.2" || hostname == "localhost" || hostname == "127.0.0.1"
            }
        } catch (_: Exception) {
            // Retain default platform SSL config if custom context cannot be initialized
        }
    }

    /**
     * Resolves the active base URL, checking for custom developer overrides in SharedPreferences.
     *
     * @param context Application or activity context.
     * @return Formatted base URL string ending with a trailing slash.
     */
    fun getBaseUrl(context: android.content.Context): String {
        val prefs = context.getSharedPreferences(com.sliit.ssmts.util.Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val customUrl = prefs.getString(com.sliit.ssmts.util.Constants.KEY_CUSTOM_BASE_URL, null)
        val baseUrl = if (!customUrl.isNullOrBlank()) customUrl else com.sliit.ssmts.util.Constants.DEFAULT_BASE_URL
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    /**
     * Constructs the Retrofit service client for OperatorDashboardApi.
     *
     * @param baseUrl Base URL for the central C# Web API (defaults to Constants.DEFAULT_BASE_URL).
     * @param tokenProvider Provider supplying active bearer tokens.
     * @param enableLogging Whether to include OkHttp logging (defaults to true).
     * @param okHttpClient Optional custom OkHttpClient instance (defaults to standard builder).
     * @return Initialized OperatorDashboardApi interface.
     */
    fun createOperatorDashboardApi(
        baseUrl: String = com.sliit.ssmts.util.Constants.DEFAULT_BASE_URL,
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
