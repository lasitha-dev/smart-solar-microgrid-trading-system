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
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DependencyProvider {

    // Connected via USB using `adb reverse`
    private const val BASE_URL = "http://localhost:5000/" 

    private var retrofit: Retrofit? = null
    private var repository: IReservationRepository? = null

    private fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthHeaderInterceptor {
                // Mock JWT token for development
                "mock_token_123" 
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    private fun getReservationApi(): ReservationApi {
        return getRetrofit().create(ReservationApi::class.java)
    }

    fun getReservationRepository(context: Context): IReservationRepository {
        if (repository == null) {
            val db = SsmtsDatabase.getDatabase(context)
            repository = ReservationRepositoryImpl(
                api = getReservationApi(),
                dao = db.reservationDao()
            )
        }
        return repository!!
    }
}
