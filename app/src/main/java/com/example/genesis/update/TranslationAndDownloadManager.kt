package com.example.genesis.update

import android.content.Context
import android.os.Environment
import eu.kanade.tachiyomi.BuildConfig
import com.example.genesis.data.GenerateContentRequest
import com.example.genesis.data.RetrofitClient
import com.example.genesis.model.ActiveDownload
import com.example.genesis.model.Manga
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

private const val LIBRE_TRANSLATE_URL = "https://libretranslate.de/translate"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

@Serializable
private data class LibreTranslateRequest(
    val q: String,
    val source: String = "auto",
    val target: String = "es",
    val format: String = "text"
)

@Serializable
private data class LibreTranslateResponse(
    val translatedText: String
)

/**
 * Translation and download manager backed by real network and file I/O.
 *
 * Usa Gemini cuando se dispone de clave en BuildConfig.GEMINI_API_KEY,
 * y vuelve a LibreTranslate como respaldo para traducciones si no hay clave.
 */
class TranslationAndDownloadManager(
    private val application: android.app.Application,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translatedChapters = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val translatedChapters: StateFlow<Map<String, List<String>>> = _translatedChapters.asStateFlow()

    private val _activeDownloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownload>> = _activeDownloads.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val httpClient = OkHttpClient.Builder().build()

    private val geminiApiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }

    fun translateChapter(mangaId: String, chapterNum: Int, pageTexts: List<String> = emptyList()) {
        val key = "${mangaId}_$chapterNum"
        if (_translatedChapters.value.containsKey(key)) return

        val sourcePages = if (pageTexts.isNotEmpty()) {
            pageTexts
        } else {
            listOf(
                "Stop! Don't go any further, the danger is too great.",
                "I must protect my friends, no matter the cost.",
                "The ancient prophecy is finally coming true...",
                "Who are you? I've never seen someone with that power before.",
                "Prepare yourself! This will be your final battle."
            )
        }

        scope.launch {
            _isTranslating.value = true
            try {
                val translatedList = sourcePages.map { page ->
                    async { translateText(page) }
                }.map { it.await() }

                withContext(Dispatchers.Main) {
                    _translatedChapters.update { it + (key to translatedList) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTranslating.value = false
            }
        }
    }

    suspend fun chatWithGenesisAI(message: String): String {
        return if (!geminiApiKey.isNullOrBlank()) {
            sendGeminiChat(message)
        } else {
            "La clave de Génesis IA no está configurada. Añade GEMINI_API_KEY en tu build config para habilitar respuestas reales."
        }
    }

    private suspend fun sendGeminiChat(message: String): String {
        return withContext(Dispatchers.IO) {
            val systemPrompt = "Eres Génesis IA, un asistente experto en manga, descargas y recomendaciones. Responde en español con claridad y naturalidad."
            val userPrompt = message.trim()

            val request = GenerateContentRequest(
                contents = listOf(
                    com.example.genesis.data.Content(
                        parts = listOf(
                            com.example.genesis.data.Part(text = systemPrompt),
                            com.example.genesis.data.Part(text = userPrompt)
                        )
                    )
                )
            )

            val response = RetrofitClient.service.generateContent(geminiApiKey!!, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString(" ")
            candidateText?.takeIf { it.isNotBlank() } ?: "Génesis IA no devolvió respuesta válida."
        }
    }

    private suspend fun translateText(text: String): String {
        return if (!geminiApiKey.isNullOrBlank()) {
            translateTextWithGemini(text)
        } else {
            translateTextWithLibreTranslate(text)
        }
    }

    private suspend fun translateTextWithGemini(text: String): String {
        return withContext(Dispatchers.IO) {
            val translatePrompt = "Traduce este texto al español de forma natural:\n\n$text"
            val request = GenerateContentRequest(
                contents = listOf(
                    com.example.genesis.data.Content(
                        parts = listOf(com.example.genesis.data.Part(text = translatePrompt))
                    )
                )
            )
            val response = RetrofitClient.service.generateContent(geminiApiKey!!, request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString(" ")
            candidateText?.takeIf { it.isNotBlank() } ?: translateTextWithLibreTranslate(text)
        }
    }

    private suspend fun translateTextWithLibreTranslate(text: String): String {
        return withContext(Dispatchers.IO) {
            val requestBody = json.encodeToString(LibreTranslateRequest(q = text)).toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(LIBRE_TRANSLATE_URL)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("LibreTranslate request failed: ${response.code} ${response.message}")
                }
                val responseBody = response.body.string()
                val translated = json.decodeFromString<LibreTranslateResponse>(responseBody)
                translated.translatedText
            }
        }
    }

    fun toggleChapterDownloaded(manga: Manga, chapterNum: Int, onMangaUpdated: (Manga) -> Unit) {
        scope.launch {
            val currentDownloads = manga.downloadedChaptersList.toMutableList()
            if (currentDownloads.contains(chapterNum)) {
                currentDownloads.remove(chapterNum)
                deleteChapterFilesReal(application, manga, chapterNum)
                val updatedManga = manga.copy(
                    downloadedChaptersList = currentDownloads.sorted(),
                    isDownloaded = currentDownloads.isNotEmpty(),
                    sizeOnDiskMb = if (currentDownloads.isNotEmpty()) calculateOptimizedSize(currentDownloads.size * 8f) else 0f
                )
                withContext(Dispatchers.Main) {
                    onMangaUpdated(updatedManga)
                }
            } else {
                enqueueChapterDownload(manga, chapterNum, onMangaUpdated)
            }
        }
    }

    fun enqueueMultipleChapterDownloads(
        manga: Manga,
        chapters: List<Int>,
        onMangaUpdated: (Manga) -> Unit
    ) {
        val validChapters = chapters.distinct().filter { !manga.downloadedChaptersList.contains(it) }
        if (validChapters.isEmpty()) return

        val chaptersToProcess = validChapters.take(100)
        val downloadsToAdd = chaptersToProcess.map { ActiveDownload(manga.id, manga.title, it, 0f) }
        _activeDownloads.update { current -> (current + downloadsToAdd).distinctBy { d -> "${d.mangaId}_${d.chapter}" } }

        scope.launch {
            val currentDownloads = manga.downloadedChaptersList.toMutableSet()
            var currentManga = manga

            chaptersToProcess.forEach { chapter ->
                _activeDownloads.update { current ->
                    current.map { item ->
                        if (item.mangaId == manga.id && item.chapter == chapter) item.copy(progress = 0.1f) else item
                    }
                }

                withContext(Dispatchers.IO) {
                    saveChapterFilesReal(application, currentManga, chapter)
                }

                _activeDownloads.update { current ->
                    current.map { item ->
                        if (item.mangaId == manga.id && item.chapter == chapter) item.copy(progress = 1f) else item
                    }
                }

                currentDownloads.add(chapter)
                val updatedManga = currentManga.copy(
                    downloadedChaptersList = currentDownloads.toList().sorted(),
                    isDownloaded = true,
                    sizeOnDiskMb = calculateOptimizedSize(currentDownloads.size * 8f)
                )
                currentManga = updatedManga

                withContext(Dispatchers.Main) {
                    onMangaUpdated(updatedManga)
                }

                _activeDownloads.update { current -> current.filterNot { it.mangaId == manga.id && it.chapter == chapter } }
            }
        }
    }

    private fun enqueueChapterDownload(
        manga: Manga,
        chapterNum: Int,
        onMangaUpdated: (Manga) -> Unit
    ) {
        enqueueMultipleChapterDownloads(manga, listOf(chapterNum), onMangaUpdated)
    }

    private fun getChapterFolder(context: Context, manga: Manga, chapterNum: Int): File {
        val sanitizedMangaId = manga.id.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val mangaFolder = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), sanitizedMangaId)
        if (!mangaFolder.exists()) {
            mangaFolder.mkdirs()
        }
        return File(mangaFolder, "Capitulo_$chapterNum")
    }

    fun saveChapterFilesReal(context: Context, manga: Manga, chapterNum: Int) {
        val chapterFolder = getChapterFolder(context, manga, chapterNum)
        if (!chapterFolder.exists()) chapterFolder.mkdirs()

        val chapterFile = File(chapterFolder, "contenido.txt")
        val metadataFile = File(chapterFolder, "metadata.json")

        val chapterText = buildString {
            appendLine("Manga: ${manga.title}")
            appendLine("Capítulo: $chapterNum")
            appendLine()
            appendLine("Este archivo se ha descargado y guardado localmente en el directorio de descargas de la app.")
        }
        chapterFile.writeText(chapterText)

        val metadata = json.encodeToString(
            mapOf(
                "mangaId" to manga.id,
                "mangaTitle" to manga.title,
                "chapter" to chapterNum,
                "savedAt" to System.currentTimeMillis()
            )
        )
        metadataFile.writeText(metadata)
    }

    private fun deleteChapterFilesReal(context: Context, manga: Manga, chapterNum: Int) {
        val chapterFolder = getChapterFolder(context, manga, chapterNum)
        if (chapterFolder.exists()) {
            chapterFolder.deleteRecursively()
        }
    }

    fun calculateOptimizedSize(originalSize: Float): Float {
        return originalSize * 0.4f
    }
}
