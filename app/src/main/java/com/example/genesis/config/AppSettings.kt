package com.example.genesis.config

// Configuraciones de Genesis IA y optimizaciones de descarga
data class AppSettings(
    var isPureBlack: Boolean = true,
    var isIncognitoMode: Boolean = false,
    var onlyDownloaded: Boolean = false,
    var showAdultContent: Boolean = true,
    var selectedTheme: String = "Original",
    var appLanguage: String = "Español",
    var libraryUpdateFrequency: String = "Cada 12 horas",
    var showUpdateNotifications: Boolean = true,
    var readerReadingMode: String = "Vertical",
    var readerTapToTurn: Boolean = true,
    var readerRotation: String = "Cualquier dirección",
    var readerBackgroundColor: String = "Negro",
    var downloadWifiOnly: Boolean = false,
    var downloadAutomatic: Boolean = true,
    var optimizeDownloads: Boolean = true,
    var genesisHighPrecision: Boolean = true,
    var securityBiometricLock: Boolean = false,
    var securitySecureScreen: Boolean = false,
    var userName: String = "Génesis User",
    var userBio: String = "Amante del manga y manhwa",
    var extensionLanguageFilter: String = "Todas",
    var saveAsCbz: Boolean = false,
    var splitTallImages: Boolean = true,
    var readerShowReadingMode: Boolean = true,
    var readerFullScreen: Boolean = true,
    var libraryDefaultCategory: String = "Preguntar siempre"
)
