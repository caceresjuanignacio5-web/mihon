package com.example.genesis.update

import android.util.Log
import eu.kanade.tachiyomi.BuildConfig
import com.example.genesis.data.*
import com.example.genesis.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object GeminiIARetrofit {
    val service: GeminiApiService by lazy {
        RetrofitClient.service
    }
}

class GenesisTranslator(private val apiKey: String = BuildConfig.GEMINI_API_KEY) {
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun translatePage(originalLines: List<String>): List<String>? {
        val activeKey = apiKey.takeIf { it.isNotBlank() } ?: return null
        _isTranslating.value = true

        return try {
            val systemPrompt = """
                Actúa como un traductor experto de Manga y Manhwa (Génesis IA v3.5).
                Tu tarea es traducir los siguientes textos del INGLÉS al ESPAÑOL.
                REGLAS:
                - Usa un lenguaje coloquial y natural.
                - Mantén el impacto emocional de las escenas de acción.
                - Devuelve el resultado como un JSON plano con el formato: {"translations": ["traduccion1", "traduccion2", ...]}.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(
                        Part(text = systemPrompt),
                        Part(text = originalLines.joinToString("\n"))
                    ))
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val response = GeminiIARetrofit.service.generateContent(activeKey, request)
            val jsonResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            jsonResponse?.let { parseTranslationResponse(it) }
        } catch (e: Exception) {
            Log.e("GenesisIA", "Error en traducción contextual con Gemini 3-Flash", e)
            null
        } finally {
            _isTranslating.value = false
        }
    }

    private fun parseTranslationResponse(jsonResponse: String): List<String>? {
        return try {
            val element = json.parseToJsonElement(jsonResponse)
            element.jsonObject["translations"]?.jsonArray?.map { it.jsonPrimitive.content }
        } catch (e: Exception) {
            Log.e("GenesisIA", "Error parseando traducción JSON", e)
            null
        }
    }
}

class GenesisAIAssistant {
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    private val apiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun processUserQuery(query: String) {
        if (query.isBlank() || apiKey.isNullOrBlank()) return

        scope.launch {
            withContext(Dispatchers.Main) {
                _chatHistory.update { it + ChatMessage("Génesis IA procesando: $query...", false) }
            }

            val responseText = try {
                sendGeminiChat(query)
            } catch (e: Exception) {
                Log.e("GenesisIA", "Error en chat con Gemini 3-Flash", e)
                "Lo siento, no pude procesar tu solicitud en este momento."
            }

            withContext(Dispatchers.Main) {
                _chatHistory.update { it + ChatMessage(responseText, false) }
            }
        }
    }

    private suspend fun sendGeminiChat(query: String): String {
        val activeKey = apiKey!!
        return withContext(Dispatchers.IO) {
            val systemPrompt = "Eres Génesis IA, un asistente experto en manga, descargas y recomendaciones. Responde en español con claridad y naturalidad."
            val userPrompt = query.trim()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(
                        Part(text = systemPrompt),
                        Part(text = userPrompt)
                    ))
                )
            )

            val response = GeminiIARetrofit.service.generateContent(activeKey, request)
            response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.mapNotNull { it.text }
                ?.joinToString(" ")
                ?.takeIf { it.isNotBlank() }
                ?: "Génesis IA no devolvió respuesta válida."
        }
    }
}

object GenesisIA {
    fun createTranslator(): GenesisTranslator = GenesisTranslator()
    fun createAssistant(): GenesisAIAssistant = GenesisAIAssistant()
}
