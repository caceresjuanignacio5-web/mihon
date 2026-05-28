package com.example.genesis.model

import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.Calendar

@Serializable
data class ActiveDownload(
    val mangaId: String,
    val title: String,
    val chapter: Int,
    val progress: Float
)

@Serializable
data class Manga(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String,
    val source: String,
    val type: String = "Manga",
    val status: String = "En publicación",
    val description: String = "Sin descripción disponible.",
    val coverUrl: String = "",
    val category: String = "Librería",
    val unreadCount: Int = 0,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis(),
    val chapters: Int = 0,
    val genres: List<String> = emptyList(),
    val releaseDay: Int = Calendar.MONDAY,
    val url: String = "https://manga.default.com",
    val notes: String = "",
    val sizeOnDiskMb: Float = 0f,
    val bookmarkedChapters: List<Int> = emptyList(),
    val downloadedChaptersList: List<Int> = emptyList(),
    val translatedChaptersList: List<Int> = emptyList()
)

data class Extension(
    val name: String,
    val lang: String,
    val version: String,
    val isInstalled: Boolean,
    val iconUrl: String = "",
    val isPinned: Boolean = false
)

@Serializable
data class UpdateLog(
    val id: String = UUID.randomUUID().toString(),
    val mangaId: String,
    val mangaTitle: String,
    val chapter: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Tab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

object Screen {
    const val Main = "main"
    fun Details(mangaId: String) = "details/$mangaId"
    const val DetailsRoute = "details/{mangaId}"
    
    fun Reader(mangaId: String, chapterIndex: Int) = "reader/$mangaId/$chapterIndex"
    const val ReaderRoute = "reader/{mangaId}/{chapterIndex}"
    
    const val Settings = "settings"
    const val Downloads = "downloads"
    const val AiChat = "ai_chat"
    const val Statistics = "statistics"
    const val CategoryManagement = "category_management"
    const val SettingsAppearance = "settings_appearance"
    const val SettingsLibrary = "settings_library"
    const val SettingsReader = "settings_reader"
    const val SettingsDownloads = "settings_downloads"
    const val SettingsBrowse = "settings_browse"
    const val SettingsAdvanced = "settings_advanced"
    const val SettingsSecurity = "settings_security"
    const val DataAndStorage = "data_storage"
    
    fun WebView(url: String, title: String) = "webview/${Uri.encode(url)}/${Uri.encode(title)}"
    const val WebViewRoute = "webview/{url}/{title}"
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
