/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Manual Dependency Injection container for providing singletons.
 */

package com.sliit.ssmts.reservation_workflow.di

import android.content.Context
import com.sliit.ssmts.reservation_workflow.data.local.SsmtsDatabase
import com.sliit.ssmts.reservation_workflow.data.remote.ReservationApi
import com.sliit.ssmts.reservation_workflow.data.remote.interceptor.AuthHeaderInterceptor
import com.sliit.ssmts.reservation_workflow.data.repository.ReservationRepositoryImpl
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.util.Constants
import com.sliit.ssmts.util.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DependencyProvider {

    private var retrofit: Retrofit? = null
    private var repository: IReservationRepository? = null

    private fun getBaseUrl(context: Context?): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val customUrl = prefs.getString(Constants.KEY_CUSTOM_BASE_URL, null)
            if (!customUrl.isNullOrBlank()) {
                return if (customUrl.endsWith("/")) customUrl else "$customUrl/"
            }
        }
        return Constants.DEFAULT_BASE_URL
    }

    private fun getRetrofit(context: Context? = null): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthHeaderInterceptor {
                val token = context?.let { SessionManager(it).getAuthToken() }
                token ?: "mock_token_123"
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(getBaseUrl(context))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getReservationApi(context: Context? = null): ReservationApi {
        return getRetrofit(context).create(ReservationApi::class.java)
    }

    fun getReservationRepository(context: Context): IReservationRepository {
        if (repository == null) {
            val db = SsmtsDatabase.getDatabase(context)
            repository = ReservationRepositoryImpl(
                api = getReservationApi(context),
                dao = db.reservationDao()
            )
        }
        return repository!!
    }
}
