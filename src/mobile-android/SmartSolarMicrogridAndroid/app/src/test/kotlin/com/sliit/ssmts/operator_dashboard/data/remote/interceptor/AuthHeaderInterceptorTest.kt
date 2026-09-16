/**
 * Description: Unit test suite for AuthHeaderInterceptor verifying dynamic injection of
 * Authorization Bearer JWT tokens and graceful omission for unauthenticated requests.
 */
package com.sliit.ssmts.operator_dashboard.data.remote.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Unit test verifying AuthHeaderInterceptor behavior against MockWebServer.
 */
class AuthHeaderInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    /**
     * Starts MockWebServer before each test execution.
     */
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    /**
     * Shuts down MockWebServer after each test execution.
     */
    @After
    @Throws(IOException::class)
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Verifies that when a valid token is provided, the Authorization header with Bearer prefix is attached.
     */
    @Test
    fun intercept_withValidToken_attachesBearerHeader() {
        val expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-payload"
        val interceptor = AuthHeaderInterceptor { expectedToken }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertEquals("Bearer $expectedToken", authHeader)
    }

    /**
     * Verifies that when token is null, no Authorization header is attached.
     */
    @Test
    fun intercept_withNullToken_omitsAuthorizationHeader() {
        val interceptor = AuthHeaderInterceptor { null }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertNull(authHeader)
    }

    /**
     * Verifies that when token is blank, no Authorization header is attached.
     */
    @Test
    fun intercept_withBlankToken_omitsAuthorizationHeader() {
        val interceptor = AuthHeaderInterceptor { "   " }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/api/test"))
            .build()

        client.newCall(request).execute().close()

        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertNull(authHeader)
    }
}
