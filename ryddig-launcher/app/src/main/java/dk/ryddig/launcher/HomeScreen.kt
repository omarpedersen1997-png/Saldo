package dk.ryddig.launcher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Danish: Locale = Locale.forLanguageTag("da-DK")

@Composable
fun HomeScreen(
    apps: List<AppEntry>?,
    overrides: Map<String, Category>,
    collapsed: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleCategory: (Category) -> Unit,
    onMoveApp: (AppEntry, Category?) -> Unit,
    onResetCategories: () -> Unit,
    resumed: Int,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var moveApp by remember { mutableStateOf<AppEntry?>(null) }
    var isDefaultHome by remember { mutableStateOf(true) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = HomeRole.isDefaultHome(context)
    }
    LaunchedEffect(resumed) { isDefaultHome = HomeRole.isDefaultHome(context) }

    fun categoryOf(app: AppEntry) = overrides[app.key] ?: app.autoCategory
    fun launch(app: AppEntry) {
        focus.clearFocus()
        onQueryChange("")
        AppRepository.launch(context, app)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = (maxWidth / 86.dp).toInt().coerceIn(4, 7)
        val all = apps.orEmpty()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Header(
                    onWallpaper = {
                        runCatching {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Vælg baggrund"))
                        }
                    },
                    onSetDefault = { runCatching { roleLauncher.launch(HomeRole.requestIntent(context)) } },
                    onPhoneSettings = { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } },
                    onResetCategories = onResetCategories,
                )
            }
            if (!isDefaultHome) {
                item(key = "default") {
                    GlassCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("Gør Ryddig Start til din startskærm", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Så åbner hjem-knappen altid din sorterede startskærm.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(10.dp))
                            FilledTonalButton(onClick = {
                                runCatching { roleLauncher.launch(HomeRole.requestIntent(context)) }
                            }) { Text("Vælg som startskærm") }
                        }
                    }
                }
            }
            item(key = "search") {
                SearchField(query, onQueryChange, focus) {
                    val first = apps.orEmpty().firstOrNull { it.label.contains(query.trim(), ignoreCase = true) }
                    if (first != null) launch(first)
                }
            }

            when {
                apps == null -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                query.isNotBlank() -> {
                    val q = query.trim()
                    val hits = all.filter { it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true) }
                    item(key = "results") {
                        GlassCard {
                            Column(Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    if (hits.isEmpty()) "Ingen apps matcher \"$q\"" else "${hits.size} resultater",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                AppGrid(hits, columns, onClick = ::launch, onLongClick = { menuApp = it })
                            }
                        }
                    }
                }

                else -> {
                    val grouped = all.groupBy(::categoryOf)
                    for (category in Category.entries) {
                        val list = grouped[category] ?: continue
                        val isCollapsed = category.name in collapsed
                        item(key = "cat-${category.name}") {
                            GlassCard {
                                Column {
                                    CategoryHeader(category, list.size, isCollapsed) { onToggleCategory(category) }
                                    if (!isCollapsed) {
                                        AppGrid(list, columns, onClick = ::launch, onLongClick = { menuApp = it })
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                    item(key = "tip") {
                        Text(
                            "Tip: Hold fingeren på en app for at flytte den til en anden kategori.",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }

    menuApp?.let { app ->
        AppMenuDialog(
            app = app,
            current = categoryOf(app),
            onDismiss = { menuApp = null },
            onMove = { menuApp = null; moveApp = app },
            onHideToggle = {
                menuApp = null
                onMoveApp(app, if (categoryOf(app) == Category.SKJULT) null else Category.SKJULT)
            },
            onInfo = { menuApp = null; AppRepository.openAppInfo(context, app.packageName) },
            onUninstall = { menuApp = null; AppRepository.uninstall(context, app.packageName) },
        )
    }
    moveApp?.let { app ->
        MoveDialog(
            app = app,
            current = categoryOf(app),
            isOverridden = overrides.containsKey(app.key),
            onDismiss = { moveApp = null },
            onPick = { cat -> moveApp = null; onMoveApp(app, cat) },
        )
    }
}

@Composable
private fun Header(
    onWallpaper: () -> Unit,
    onSetDefault: () -> Unit,
    onPhoneSettings: () -> Unit,
    onResetCategories: () -> Unit,
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val time = remember(now) { SimpleDateFormat("HH:mm", Danish).format(now) }
    val date = remember(now) {
        SimpleDateFormat("EEEE d. MMMM", Danish).format(now).replaceFirstChar { it.titlecase(Danish) }
    }

    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(time, fontSize = 56.sp, fontWeight = FontWeight.Light, color = Color.White)
            Text(date, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Skift baggrund") }, onClick = { menuOpen = false; onWallpaper() })
                DropdownMenuItem(text = { Text("Vælg standard-startskærm") }, onClick = { menuOpen = false; onSetDefault() })
                DropdownMenuItem(text = { Text("Telefonens indstillinger") }, onClick = { menuOpen = false; onPhoneSettings() })
                DropdownMenuItem(text = { Text("Nulstil mine kategorier") }, onClick = { menuOpen = false; onResetCategories() })
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, focus: FocusManager, onGo: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Søg efter apps") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange(""); focus.clearFocus() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Ryd søgning")
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.45f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.35f),
            focusedBorderColor = Color.White.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            focusedTrailingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
            cursorColor = Color.White,
        ),
    )
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.42f), contentColor = Color.White),
    ) { content() }
}

@Composable
private fun CategoryHeader(category: Category, count: Int, collapsed: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(category.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("$count", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.65f))
        Spacer(Modifier.width(6.dp))
        Icon(
            if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            contentDescription = if (collapsed) "Fold ud" else "Fold sammen",
            tint = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun AppGrid(apps: List<AppEntry>, columns: Int, onClick: (AppEntry) -> Unit, onLongClick: (AppEntry) -> Unit) {
    Column(Modifier.padding(horizontal = 6.dp)) {
        for (row in apps.chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                for (app in row) {
                    Box(Modifier.weight(1f)) { AppIcon(app, { onClick(app) }, { onLongClick(app) }) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Flere valg")
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppMenuDialog(
    app: AppEntry,
    current: Category,
    onDismiss: () -> Unit,
    onMove: () -> Unit,
    onHideToggle: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp)) },
        title = { Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text("Kategori: ${current.emoji} ${current.title}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                MenuRow("📂  Flyt til anden kategori", onMove)
                MenuRow(if (current == Category.SKJULT) "👁️  Vis appen igen" else "🙈  Skjul appen", onHideToggle)
                MenuRow("ℹ️  App-info (lager, cache, tilladelser)", onInfo)
                MenuRow("🗑️  Afinstaller", onUninstall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Luk") } },
    )
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun MoveDialog(
    app: AppEntry,
    current: Category,
    isOverridden: Boolean,
    onDismiss: () -> Unit,
    onPick: (Category?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flyt \"${app.label}\"") },
        text = {
            LazyColumn {
                if (isOverridden) {
                    item {
                        MenuRow("✨  Automatisk (${app.autoCategory.title})") { onPick(null) }
                        HorizontalDivider()
                    }
                }
                items(Category.entries) { cat ->
                    val mark = if (cat == current) "  ✓" else ""
                    MenuRow("${cat.emoji}  ${cat.title}$mark") { onPick(cat) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Annuller") } },
    )
}
