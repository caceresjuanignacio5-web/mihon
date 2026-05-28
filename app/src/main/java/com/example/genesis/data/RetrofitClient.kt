package com.example.genesis.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

import kotlinx.serialization.SerialName

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    @SerialName("generationConfig") val generationConfig: GenerationConfig? = null,
    @SerialName("safetySettings") val safetySettings: List<SafetySetting>? = null
)

@Serializable
data class SafetySetting(
    val category: String,
    val threshold: String
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null,
    @SerialName("functionCall") val functionCall: FunctionCall? = null,
    @SerialName("functionResponse") val functionResponse: FunctionResponse? = null
)

@Serializable
data class FunctionCall(
    val name: String,
    val args: JsonObject
)

@Serializable
data class FunctionResponse(
    val name: String,
    val response: JsonObject
)

@Serializable
data class GenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String? = null,
    @SerialName("responseSchema") val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    @SerialName("topP") val topP: Float? = null,
    @SerialName("topK") val topK: Int? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content,
    @SerialName("finishReason") val finishReason: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; explicitNulls = false }

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}
