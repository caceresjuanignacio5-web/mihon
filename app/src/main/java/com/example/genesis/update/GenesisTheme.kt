package com.example.genesis.update

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ARCHIVO DE REFERENCIA: Apariencia y Diseño (UI) de Génesis Manga.
 * Contiene los esquemas de colores, el tema personalizado (incluyendo modo OLED)
 * y los componentes visuales que definen la estética de la aplicación.
 */

// --- 1. DEFINICIÓN DE COLORES (COSMIC / OLED DARK) ---

val MihonBlue = Color(0xFF8AB4F8)
val MihonDark = Color(0xFF0F1011)
val MihonSurface = Color(0xFF1C1D1F)
val MihonAccent = Color(0xFF8AB4F8)
val MihonBackground = Color(0xFF000000) // Negro puro para pantallas AMOLED

// --- 2. TEMA DE LA APLICACIÓN (THEME ENGINE) ---

@Composable
fun GenesisTheme(
    selectedTheme: String = "Original",
    isPureBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        "Miaupuchino" -> darkColorScheme(
            primary = Color(0xFFD7CCC8),
            background = if (isPureBlack) Color.Black else Color(0xFF1F1816),
            surface = Color(0xFF2D2421),
            onPrimary = Color(0xFF1F1816)
        )
        else -> darkColorScheme(
            primary = MihonBlue,
            background = if (isPureBlack) Color.Black else MihonSurface,
            surface = MihonDark,
            onBackground = Color.White,
            onSurface = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// --- 3. COMPONENTES VISUALES CORE ---

/**
 * Tarjeta de Manga (MangaCard)
 * Implementa el diseño de grilla con superposición de degradado y badges.
 */
@Composable
fun MangaCardPreview(title: String, chapters: Int, isFavorite: Boolean) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
        ) {
            // Fondo simulado (en la app real es AsyncImage)
            Box(Modifier.fillMaxSize().background(Color.Gray))
            
            // Degradado inferior para legibilidad
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 150f
                        )
                    )
            )

            if (isFavorite) {
                Icon(
                    Icons.Default.Favorite, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(6.dp).size(18.dp).align(Alignment.TopStart)
                )
            }

            Text(
                "$chapters caps",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp).align(Alignment.BottomStart)
            )
        }
        
        Text(
            text = title,
            modifier = Modifier.padding(8.dp),
            maxLines = 2,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Estructura de Navegación Principal (Navegación por pestañas)
 */
@Composable
fun MainBottomNavigation(selectedItem: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    ) {
        val items = listOf("Biblioteca", "Actualizaciones", "Historial", "Explorar", "Más")
        val icons = listOf(Icons.Default.CollectionsBookmark, Icons.Default.NewReleases, Icons.Default.History, Icons.Default.Public, Icons.Default.MoreHoriz)
        
        items.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(icons[index], contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) }
            )
        }
    }
}

/**
 * Botón de Acción de Detalles con soporte para texto de apoyo (Countdown)
 */
@Composable
fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    supportingText: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray)
        if (supportingText != null) {
            Text(supportingText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp)
        }
    }
}

/**
 * Visualización del Lector (MangaReaderScreen)
 * Muestra el diseño de páginas verticales y la superposición de traducción AI.
 */
@Composable
fun ReaderPreview(
    isTranslated: Boolean,
    onToggleTranslate: () -> Unit,
    pages: List<String> = emptyList()
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Lector Génesis") },
                actions = {
                    IconButton(onClick = onToggleTranslate) {
                        Icon(
                            Icons.Default.Translate, 
                            contentDescription = "Traducir",
                            tint = if (isTranslated) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
            items(5) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .background(Color(0xFF0F0F0F)),
                    contentAlignment = Alignment.Center
                ) {
                    // Contenedor de la página
                    Box(Modifier.fillMaxSize()) {
                        // En la app real aquí va la imagen (Coil)
                        Text("PÁGINA ${i + 1}", color = Color.DarkGray)
                        
                        // Superposición de traducción (Balloon overlay)
                        if (isTranslated) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                                    .widthIn(max = 240.dp)
                            ) {
                                Text(
                                    "Texto traducido por la IA de Génesis...",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 8.dp, color = Color.Black)
            }
        }
    }
}
