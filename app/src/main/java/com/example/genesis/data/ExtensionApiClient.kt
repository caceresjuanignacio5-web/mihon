package com.example.genesis.data

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

@Serializable
data class MangaDexResponse(
    val data: List<MangaDexData>
)

@Serializable
data class MangaDexData(
    val id: String,
    val attributes: MangaDexAttributes,
    val relationships: List<MangaDexRelationship>
)

@Serializable
data class MangaDexAttributes(
    val title: Map<String, String>,
    val description: Map<String, String>? = null,
    val status: String,
    val lastChapter: String? = null
)

@Serializable
data class MangaDexRelationship(
    val id: String,
    val type: String
)

interface RealExtensionService {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String?,
        @Query("limit") limit: Int = 30,
        @Query("includes[]") includes: String = "cover_art"
    ): MangaDexResponse
}

object ExtensionApiClient {
    private var currentBaseUrl = "https://api.mangadex.org/"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private var retrofit = Retrofit.Builder()
        .baseUrl(currentBaseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    var service: RealExtensionService = retrofit.create(RealExtensionService::class.java)
        private set

    fun setBaseUrl(url: String) {
        try {
            val newUrl = if (url.endsWith("/")) url else "$url/"
            if (newUrl != currentBaseUrl) {
                currentBaseUrl = newUrl
                retrofit = Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                service = retrofit.create(RealExtensionService::class.java)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
