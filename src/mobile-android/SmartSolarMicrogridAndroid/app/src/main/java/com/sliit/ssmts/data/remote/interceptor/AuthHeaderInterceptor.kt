/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: OkHttp interceptor dynamically appending the stored JWT token to authorized API requests.
 */

package com.sliit.ssmts.data.remote.interceptor

import com.sliit.ssmts.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that injects the Authorization Bearer header into outgoing HTTP requests.
 */
class AuthHeaderInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding header if already present (e.g. custom auth)
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = sessionManager.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        requestBuilder.addHeader("Accept", "application/json")
        requestBuilder.addHeader("Content-Type", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
