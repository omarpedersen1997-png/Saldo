package dk.ryddig.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager

private val colors = darkColorScheme(
    primary = Color(0xFF7FD3FF),
    onPrimary = Color(0xFF00344A),
    secondary = Color(0xFFFFD166),
    secondaryContainer = Color(0xFF2E4A66),
    onSecondaryContainer = Color.White,
    surface = Color(0xFF1B2230),
    onSurface = Color.White,
    error = Color(0xFFFF8FA3),
)

@Composable
fun RyddigTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun RyddigApp(packagesChanged: Int, resumed: Int, homePressed: Int) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val prefs = remember { LauncherPrefs(context) }
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    val overrides = remember { mutableStateMapOf<String, Category>().apply { putAll(prefs.overrides()) } }
    var collapsed by remember { mutableStateOf(prefs.collapsed()) }
    val cleaner = remember { CleanerState() }

    LaunchedEffect(packagesChanged) {
        apps = runCatching { AppRepository.loadApps(context) }.getOrDefault(apps ?: emptyList())
    }
    // Hjem-knap: tilbage til startskærmen og ryd søgningen.
    LaunchedEffect(homePressed) {
        if (homePressed > 0) {
            tab = 0
            query = ""
            focus.clearFocus()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0x55000000), Color(0x22000000), Color(0x88000000))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            bottomBar = {
                NavigationBar(containerColor = Color.Black.copy(alpha = 0.55f)) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                        indicatorColor = Color.White.copy(alpha = 0.18f),
                    )
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                        label = { Text("Startskærm") }, colors = itemColors,
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1; focus.clearFocus() },
                        icon = { Icon(Icons.Filled.CleaningServices, contentDescription = null) },
                        label = { Text("Ryd op") }, colors = itemColors,
                    )
                }
            },
        ) { padding ->
            when (tab) {
                0 -> HomeScreen(
                    apps = apps,
                    overrides = overrides,
                    collapsed = collapsed,
                    query = query,
                    onQueryChange = { query = it },
                    onToggleCategory = { cat ->
                        collapsed = if (cat.name in collapsed) collapsed - cat.name else collapsed + cat.name
                        prefs.setCollapsed(collapsed)
                    },
                    onMoveApp = { app, cat ->
                        if (cat == null || cat == app.autoCategory) overrides.remove(app.key) else overrides[app.key] = cat
                        prefs.setOverride(app.key, if (cat == app.autoCategory) null else cat)
                    },
                    onResetCategories = {
                        overrides.clear()
                        prefs.clearOverrides()
                    },
                    resumed = resumed,
                    contentPadding = padding,
                )

                else -> CleanerScreen(state = cleaner, scope = scope, resumed = resumed, contentPadding = padding)
            }
        }
    }

}
