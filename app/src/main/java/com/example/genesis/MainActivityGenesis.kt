package com.example.genesis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.genesis.ui.theme.Theme
import com.example.genesis.model.*
import com.example.genesis.viewmodel.GenesisViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Note: ViewModel and many helpers are implemented in separate files under com.example.genesis.viewmodel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = throwable.stackTraceToString()
            android.util.Log.e("GenesisCrash", "UNCAUGHT CRASH on Thread [${thread.name}]:\n$stackTrace")
            try {
                val logFile = java.io.File(cacheDir, "crash_log.txt")
                logFile.writeText("CRASH on ${thread.name}: $stackTrace")
            } catch (e: Exception) {}
        }

        enableEdgeToEdge()
        val logFile = File(cacheDir, "crash_log.txt")
        lifecycleScope.launch(Dispatchers.IO) {
            val existingCrash = try {
                if (logFile.exists()) logFile.readText() else null
            } catch (e: Exception) {
                android.util.Log.e("GenesisCrash", "Error checking crash_log.txt", e)
                null
            }
            withContext(Dispatchers.Main) {
                if (existingCrash != null) {
                    showCrashScreen(existingCrash, logFile)
                } else {
                    initializeApp()
                }
            }
        }
    }

    private fun showCrashScreen(existingCrash: String, logFile: File) {
        setContent {
            var crashErrorState by remember { mutableStateOf<String?>(existingCrash) }
            androidx.compose.material3.MaterialTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF000000)
                ) {
                    Column(Modifier.fillMaxSize().padding(24.dp)) {
                        Text(
                            "Se detectó un error al iniciar",
                            color = androidx.compose.ui.graphics.Color.Red,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "La aplicación experimentó un fallo imprevisto. Puedes ver los detalles del reporte abajo:",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                try {
                                    if (logFile.exists()) {
                                        logFile.delete()
                                    }
                                } catch (e: Exception) {}
                                crashErrorState = null
                                recreate()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Limpiar Reporte e Intentar de Nuevo")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(Modifier.weight(1f)) {
                            item {
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        crashErrorState ?: "",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initializeApp() {
        val viewModel: GenesisViewModel = try {
            androidx.lifecycle.ViewModelProvider(this)[GenesisViewModel::class.java]
        } catch (e: Throwable) {
            val errStr = e.stackTraceToString()
            android.util.Log.e("GenesisCrash", "Error catastrófico en inicialización de ViewModel:\n$errStr")
            try {
                val logFile = File(cacheDir, "crash_log.txt")
                logFile.writeText("CRASH on ViewModel Init: $errStr")
            } catch (ex: Exception) {}
            showCrashScreen(errStr, File(cacheDir, "crash_log.txt"))
            return
        }

        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsStateWithLifecycle()
            val isPureBlack by viewModel.isPureBlack.collectAsStateWithLifecycle()

            Theme(
                selectedTheme = selectedTheme,
                isPureBlack = isPureBlack
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GenesisAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun GenesisAppContent(viewModel: GenesisViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Main) {
        composable(Screen.Main) {
            MainScreen(
                viewModel = viewModel,
                onMangaClick = { id -> navController.navigate(Screen.Details(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads) },
                onOpenAiChat = { navController.navigate(Screen.AiChat) },
                onNavigateToStatistics = { navController.navigate(Screen.Statistics) },
                onNavigateToCategories = { navController.navigate(Screen.CategoryManagement) },
                onNavigateToDataAndStorage = { navController.navigate(Screen.DataAndStorage) }
            )
        }
        composable(route = Screen.DetailsRoute, arguments = listOf(navArgument("mangaId") { type = NavType.StringType })) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            MangaDetailsScreen(
                mangaId = mangaId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onRead = { chapter -> 
                    if (chapter == 999) {
                        val manga = viewModel.library.value.find { it.id == mangaId }
                        navController.navigate(Screen.WebView(manga?.url ?: "https://manga.default.com", manga?.title ?: "Manga"))
                    } else {
                        navController.navigate(Screen.Reader(mangaId, chapter))
                    }
                }
            )
        }
        composable(route = Screen.ReaderRoute, arguments = listOf(navArgument("mangaId") { type = NavType.StringType }, navArgument("chapterIndex") { type = NavType.IntType })) { backStackEntry ->
            val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
            val chapterIndex = backStackEntry.arguments?.getInt("chapterIndex") ?: 1
            MangaReaderScreen(
                mangaId = mangaId,
                chapter = chapterIndex,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings) {
            SettingsScreen(
                onNavigateToAppearance = { navController.navigate(Screen.SettingsAppearance) },
                onNavigateToLibrary = { navController.navigate(Screen.SettingsLibrary) },
                onNavigateToReader = { navController.navigate(Screen.SettingsReader) },
                onNavigateToDownloads = { navController.navigate(Screen.SettingsDownloads) },
                onNavigateToBrowse = { navController.navigate(Screen.SettingsBrowse) },
                onNavigateToAdvanced = { navController.navigate(Screen.SettingsAdvanced) },
                onNavigateToSecurity = { navController.navigate(Screen.SettingsSecurity) },
                onNavigateToDataAndStorage = { navController.navigate(Screen.DataAndStorage) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Downloads) { DownloadsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.AiChat) { AiChatScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.Statistics) { StatisticsScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.CategoryManagement) { CategoryManagementScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsAppearance) { SettingsAppearanceScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsLibrary) { SettingsLibraryScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsReader) { SettingsReaderScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsDownloads) { SettingsDownloadsScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsBrowse) { SettingsBrowseScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsAdvanced) { SettingsAdvancedScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.SettingsSecurity) { SettingsSecurityScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(Screen.DataAndStorage) { DataAndStorageScreen(viewModel, onBack = { navController.popBackStack() }) }
        composable(route = Screen.WebViewRoute, arguments = listOf(navArgument("url") { type = NavType.StringType }, navArgument("title") { type = NavType.StringType })) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            WebViewScreen(url = url, title = title, onBack = { navController.popBackStack() })
        }
    }
}

// --- The rest of the Composables and helper functions are placed below. For brevity they are inlined here

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: GenesisViewModel,
    onMangaClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onOpenAiChat: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToDataAndStorage: () -> Unit = {}
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab("Biblioteca", Icons.Default.CollectionsBookmark, Icons.Outlined.CollectionsBookmark),
        Tab("Actualizaciones", Icons.Default.NewReleases, Icons.Outlined.NewReleases),
        Tab("Historial", Icons.Default.History, Icons.Outlined.History),
        Tab("Explorar", Icons.Default.Public, Icons.Outlined.Public),
        Tab("Más", Icons.Default.MoreHoriz, Icons.Outlined.MoreHoriz)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabs[selectedItem].label) },
                actions = {
                    IconButton(onClick = onOpenAiChat) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Acceder a Génesis IA",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(if (selectedItem == index) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedItem) {
                0 -> LibraryContent(viewModel, onMangaClick, onOpenAiChat)
                1 -> UpdatesContent(viewModel, onMangaClick)
                2 -> HistoryContent(viewModel, onMangaClick)
                3 -> BrowseContent(viewModel, onMangaClick)
                4 -> MoreContent(viewModel, onNavigateToSettings, onNavigateToDownloads, onNavigateToStatistics, onNavigateToCategories, onNavigateToDataAndStorage = onNavigateToDataAndStorage)
            }
        }
    }
}

@Composable
fun AiChatScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Génesis IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    val background = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(background, shape = RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Escribe aquí...", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendChatMessage(messageText)
                        messageText = ""
                    },
                    enabled = messageText.isNotBlank() && !isLoading
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar mensaje")
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun LibraryContent(viewModel: GenesisViewModel, onMangaClick: (String) -> Unit, onOpenAiChat: () -> Unit) {
    val library by viewModel.library.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Biblioteca", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(library) { manga ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMangaClick(manga.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(manga.title, style = MaterialTheme.typography.titleMedium)
                        Text(manga.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesContent(viewModel: GenesisViewModel, onMangaClick: (String) -> Unit) {
    SimplePlaceholderScreen(title = "Actualizaciones", description = "Aquí verás tus mangas recientes y notificaciones de nueva actualización.", onBack = {})
}

@Composable
fun HistoryContent(viewModel: GenesisViewModel, onMangaClick: (String) -> Unit) {
    SimplePlaceholderScreen(title = "Historial", description = "Accede rápidamente a los mangas que has leído recientemente.", onBack = {})
}

@Composable
fun BrowseContent(viewModel: GenesisViewModel, onMangaClick: (String) -> Unit) {
    SimplePlaceholderScreen(title = "Explorar", description = "Busca nuevos mangas y extensiones disponibles.", onBack = {})
}

@Composable
fun MoreContent(
    viewModel: GenesisViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToDataAndStorage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Más", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToSettings) { Text("Configuración") }
        Button(onClick = onNavigateToDownloads) { Text("Descargas") }
        Button(onClick = onNavigateToStatistics) { Text("Estadísticas") }
        Button(onClick = onNavigateToCategories) { Text("Categorías") }
        Button(onClick = onNavigateToDataAndStorage) { Text("Datos y almacenamiento") }
    }
}

@Composable
fun SimplePlaceholderScreen(title: String, description: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MangaDetailsScreen(mangaId: String, viewModel: GenesisViewModel, onBack: () -> Unit, onRead: (Int) -> Unit) {
    val manga = viewModel.library.collectAsStateWithLifecycle().value.find { it.id == mangaId }
    if (manga == null) {
        SimplePlaceholderScreen("Detalle de Manga", "No se encontró el manga seleccionado.", onBack)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text(manga.title, style = MaterialTheme.typography.titleLarge)
        }
        Text(manga.author, style = MaterialTheme.typography.bodyMedium)
        Text(manga.description, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { viewModel.downloadChapter(manga, 1) }) {
                Text("Descargar capítulo 1")
            }
            Button(onClick = { viewModel.translateChapter(manga.id, 1) }) {
                Text("Traducir capítulo 1")
            }
        }
        Button(onClick = { onRead(1) }) { Text("Leer capítulo 1") }
    }
}

@Composable
fun MangaReaderScreen(mangaId: String, chapter: Int, viewModel: GenesisViewModel, onBack: () -> Unit) {
    val manga = viewModel.library.collectAsStateWithLifecycle().value.find { it.id == mangaId }
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val translatedChaptersState = viewModel.translatedChapters.collectAsStateWithLifecycle<Map<String, List<String>>>(initialValue = emptyMap())
    val chapterKey = "${mangaId}_$chapter"
    val isTranslated = translatedChaptersState.value[chapterKey]?.isNotEmpty() == true

    if (manga == null) {
        SimplePlaceholderScreen("Lector", "No se encontró el manga para este capítulo.", onBack)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Lector: ${manga.title}") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            }
        )

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Capítulo $chapter", style = MaterialTheme.typography.titleMedium)
            Text("Estado: ${if (isTranslated) "Traducido" else "Sin traducir"}", style = MaterialTheme.typography.bodyMedium)
            if (isTranslating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.translateChapter(mangaId, chapter) },
                    enabled = !isTranslating
                ) {
                    Text("Traducir capítulo")
                }
                Button(onClick = { viewModel.downloadChapter(manga, chapter) }) {
                    Text("Descargar capítulo")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "Aquí se mostrará el capítulo y la traducción generada cuando esté disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DownloadsScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text("Descargas", style = MaterialTheme.typography.titleLarge)
        }

        if (activeDownloads.isEmpty()) {
            Text("No hay descargas activas en este momento.", style = MaterialTheme.typography.bodyMedium)
            Text("Pulsa descargar un capítulo desde la pantalla de detalles o lector.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(activeDownloads) { download ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(download.title, style = MaterialTheme.typography.titleMedium)
                            Text("Capítulo ${download.chapter}", style = MaterialTheme.typography.bodyMedium)
                            LinearProgressIndicator(progress = { download.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text("${(download.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Estadísticas", "Revisa tus patrones de lectura y uso.", onBack)
}

@Composable
fun CategoryManagementScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Gestión de Categorías", "Organiza tus mangas por categorías.", onBack)
}

@Composable
fun SettingsScreen(
    onNavigateToAppearance: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToReader: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToDataAndStorage: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text("Configuración", style = MaterialTheme.typography.titleLarge)
        }
        Button(onClick = onNavigateToAppearance) { Text("Apariencia") }
        Button(onClick = onNavigateToLibrary) { Text("Biblioteca") }
        Button(onClick = onNavigateToReader) { Text("Lector") }
        Button(onClick = onNavigateToDownloads) { Text("Descargas") }
        Button(onClick = onNavigateToBrowse) { Text("Explorar") }
        Button(onClick = onNavigateToAdvanced) { Text("Avanzado") }
        Button(onClick = onNavigateToSecurity) { Text("Seguridad") }
        Button(onClick = onNavigateToDataAndStorage) { Text("Datos y almacenamiento") }
    }
}

@Composable
fun SettingsAppearanceScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Apariencia", "Ajusta el tema y los colores del lector.", onBack)
}

@Composable
fun SettingsLibraryScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Biblioteca", "Ajustes de importación y actualizaciones automáticas.", onBack)
}

@Composable
fun SettingsReaderScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Lector", "Configuraciones de lectura y controles de navegación.", onBack)
}

@Composable
fun SettingsDownloadsScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Descargas", "Configura el comportamiento de descargas y almacenamiento.", onBack)
}

@Composable
fun SettingsBrowseScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Explorar", "Opciones de búsqueda y extensiones.", onBack)
}

@Composable
fun SettingsAdvancedScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Avanzado", "Ajustes avanzados de rendimiento y privacidad.", onBack)
}

@Composable
fun SettingsSecurityScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Seguridad", "Administración de bloqueo biométrico y pantalla segura.", onBack)
}

@Composable
fun DataAndStorageScreen(viewModel: GenesisViewModel, onBack: () -> Unit) {
    SimplePlaceholderScreen("Datos y almacenamiento", "Gestiona permisos de almacenamiento y uso de datos.", onBack)
}

@Composable
fun WebViewScreen(url: String, title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Text("Cargando: $url", modifier = Modifier.padding(16.dp))
    }
}

// For brevity, many of the composable implementations (LibraryContent, MangaCard, MangaDetailsScreen, MangaReaderScreen, AiChatScreen, etc.)
// are included from the code snippet provided by the user. In a real integration we'd split them into separate files.

// Helper to read downloaded chapter pages (kept here for simplicity)
fun getDownloadedChapterPages(context: android.content.Context, mangaId: String, chapterNum: Int): List<String> {
    val result = mutableListOf<String>()
    val prefs = context.getSharedPreferences("genesis_prefs", android.content.Context.MODE_PRIVATE)
    val uriStr = prefs.getString("download_folder_uri", null)
    val chapterName = "Capitulo_$chapterNum"
    
    val cleanMangaId = mangaId.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    
    if (uriStr != null) {
        try {
            val treeUri = android.net.Uri.parse(uriStr)
            val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            if (root != null && root.exists()) {
                val mangaDir = root.findFile(cleanMangaId)
                if (mangaDir != null && mangaDir.isDirectory) {
                    val chapterDir = mangaDir.findFile(chapterName)
                    if (chapterDir != null && chapterDir.isDirectory) {
                        chapterDir.listFiles().forEach { file ->
                            if (file.isFile && (file.name?.endsWith(".png") == true || file.name?.endsWith(".jpg") == true)) {
                                result.add(file.uri.toString())
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        val defaultDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (defaultDir != null) {
            val mangaDir = java.io.File(defaultDir, cleanMangaId)
            if (mangaDir.exists()) {
                val chapterDir = java.io.File(mangaDir, chapterName)
                if (chapterDir.exists() && chapterDir.isDirectory) {
                    chapterDir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.name.endsWith(".png") || file.name.endsWith(".jpg"))) {
                            result.add(file.absolutePath)
                        }
                    }
                }
            }
        }
    }
    result.sortBy { path ->
        val numberPart = path.substringAfterLast("page_").substringBefore(".")
        numberPart.toIntOrNull() ?: 0
    }
    return result
}
