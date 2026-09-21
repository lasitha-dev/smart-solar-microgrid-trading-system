/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Factory object configuring Retrofit client, OkHttp pipeline, and JSON converters.
 */

package com.sliit.ssmts.data.remote

import android.content.Context
import com.google.gson.GsonBuilder
import com.sliit.ssmts.data.remote.interceptor.AuthHeaderInterceptor
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory managing Retrofit instances and network pipeline setup.
 */
object RetrofitClient {

    @Volatile
    private var authApiInstance: AuthApi? = null

    /**
     * Obtains the configured [AuthApi] instance for making remote REST calls.
     */
    fun getAuthApi(context: Context): AuthApi {
        return authApiInstance ?: synchronized(this) {
            authApiInstance ?: buildRetrofit(context).create(AuthApi::class.java).also {
                authApiInstance = it
            }
        }
    }

    /**
     * Resolves the active base URL, checking for custom developer overrides in SharedPreferences.
     */
    private fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val customUrl = prefs.getString(Constants.KEY_CUSTOM_BASE_URL, null)
        val baseUrl = if (!customUrl.isNullOrBlank()) customUrl else Constants.DEFAULT_BASE_URL
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val sessionManager = SessionManager(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(getBaseUrl(context))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
