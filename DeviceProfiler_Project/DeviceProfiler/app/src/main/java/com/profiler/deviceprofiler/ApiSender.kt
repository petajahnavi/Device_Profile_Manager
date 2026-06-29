package com.profiler.deviceprofiler

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// -------------------- DATA CLASSES --------------------

data class ProfilePayload(
    @SerializedName("deviceModel")     val deviceModel: String,
    @SerializedName("manufacturer")    val manufacturer: String,
    @SerializedName("androidVersion")  val androidVersion: String,
    @SerializedName("cpuCores")        val cpuCores: Int,
    @SerializedName("totalRamMB")      val totalRamMB: Long,
    @SerializedName("batteryPercent")  val batteryPercent: Int,
    @SerializedName("isCharging")      val isCharging: Boolean,
    @SerializedName("benchmarkTimeMs") val benchmarkTimeMs: Long,
    @SerializedName("cpuScore")        val cpuScore: Int,
    @SerializedName("memoryScore")     val memoryScore: Int,
    @SerializedName("batteryScore")    val batteryScore: Int,
    @SerializedName("gpuName")         val gpuName: String,     // ✅ NEW
    @SerializedName("gpuScore")        val gpuScore: Int,       // ✅ NEW
    @SerializedName("finalScore")      val finalScore: Int,
    @SerializedName("grade")           val grade: String
)

data class ApiResponse(
    @SerializedName("message") val message: String,
    @SerializedName("id")      val id: String?
)

// -------------------- API INTERFACE --------------------

interface ProfileApi {

    @POST("/profile")
    suspend fun saveProfile(@Body payload: ProfilePayload): ApiResponse

    @GET("/profile")
    suspend fun getAllProfiles(): List<ProfilePayload>
}

// -------------------- MAIN CLASS --------------------

class ApiSender(private val context: Context) {

    companion object {
        // ⚠️ IMPORTANT: trailing slash required
        private const val BASE_URL = "http://10.1.169.123:3000/"
    }

    private val api: ProfileApi by lazy {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfileApi::class.java)
    }

    // -------------------- SEND FUNCTION --------------------

    suspend fun sendProfile(
        deviceInfo: DeviceInfoData,
        liveStatus: LiveStatusData,
        benchmark: BenchmarkResult,
        scores: ScoreResult
    ): Result<String> {

        return withContext(Dispatchers.IO) {
            try {

                val payload = ProfilePayload(
                    deviceModel     = "${deviceInfo.manufacturer} ${deviceInfo.deviceModel}",
                    manufacturer    = deviceInfo.manufacturer,
                    androidVersion  = deviceInfo.androidVersion,
                    cpuCores        = deviceInfo.cpuCores,
                    totalRamMB      = deviceInfo.totalRamMB,
                    batteryPercent  = liveStatus.batteryPercent,
                    isCharging      = liveStatus.isCharging,
                    benchmarkTimeMs = benchmark.totalTimeMs,
                    cpuScore        = scores.cpuScore,
                    memoryScore     = scores.memoryScore,
                    batteryScore    = scores.batteryScore,
                    gpuName         = deviceInfo.gpuName,     // ✅ NEW
                    gpuScore        = scores.gpuScore,        // ✅ NEW
                    finalScore      = scores.finalScore,
                    grade           = scores.grade
                )

                val response = api.saveProfile(payload)

                Result.success("Saved! ${response.message}")

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}