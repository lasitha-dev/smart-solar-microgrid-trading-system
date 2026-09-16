/**
 * Description: Dynamic OkHttp interceptor that securely injects Authorization Bearer JWT tokens
 * into outgoing HTTP requests for Member 4 authenticated operations.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Functional interface contract for providing the active authentication token.
 */
fun interface AuthTokenProvider {
    /**
     * Supplies the active JWT bearer token string.
     *
     * @return Active token string or null if unauthenticated.
     */
    fun getToken(): String?
}

/**
 * Interceptor responsible for dynamically appending authorization bearer headers.
 *
 * @property tokenProvider Provider supplying current user or operator authentication tokens.
 */
class AuthHeaderInterceptor(
    private val tokenProvider: AuthTokenProvider
) : Interceptor {

    /**
     * Intercepts outgoing HTTP requests and injects the Authorization Bearer token header if available.
     *
     * @param chain OkHttp execution interceptor chain.
     * @return Executed HTTP response.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider.getToken()

        val request = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer"
    }
}
