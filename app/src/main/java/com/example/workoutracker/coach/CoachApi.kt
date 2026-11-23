package com.example.workoutracker.coach

import com.example.workoutracker.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/** The coach server's endpoints, as Retrofit turns them into Kotlin functions. */
interface CoachApi {
    @POST("api/coach/review")
    suspend fun review(@Body request: ReviewRequestDto): CoachReviewDto
}

/** Shared JSON settings: ignore fields we don't know (so the server can add some safely). */
val coachJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false // don't send "previous": null etc.; leave the field out
}

object CoachClient {

    /** Whether a server address was set in local.properties (coach.baseUrl). */
    val isConfigured: Boolean get() = BuildConfig.COACH_BASE_URL.isNotBlank()

    /**
     * Builds the client. The app key is added to every request by an OkHttp "interceptor"
     * (code that sees each request on its way out), so no endpoint can forget it.
     * The long read timeout allows for a slow AI answer and, later, the free server waking up.
     */
    fun create(): CoachApi? {
        if (!isConfigured) return null
        val baseUrl = BuildConfig.COACH_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
        val http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("X-App-Key", BuildConfig.COACH_APP_KEY).build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(coachJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoachApi::class.java)
    }
}