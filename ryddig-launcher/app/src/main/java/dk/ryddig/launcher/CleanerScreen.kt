package dk.ryddig.launcher

import android.Manifest
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Tilstand for oprydningen – overlever skift mellem fanerne. */
class CleanerState {
    var scanning by mutableStateOf(false)
    var progress by mutableStateOf("")
    var results by mutableStateOf<List<JunkFile>?>(null)
    val selected = mutableStateMapOf<String, Boolean>()
    val expanded = mutableStateListOf<JunkType>()
    var lastResult by mutableStateOf<DeleteResult?>(null)
    var deleting by mutableStateOf(false)
    var job: Job? = null
}

@Composable
fun CleanerScreen(state: CleanerState, scope: CoroutineScope, resumed: Int, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(Cleaner.hasAccess(context)) }
    var storage by remember { mutableStateOf(Cleaner.storageInfo()) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(resumed) {
        hasAccess = Cleaner.hasAccess(context)
        storage = Cleaner.storageInfo()
    }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasAccess = Cleaner.hasAccess(context)
    }
    fun size(bytes: Long) = Formatter.formatShortFileSize(context, bytes)

    fun startScan() {
        state.job?.cancel()
        state.lastResult = null
        state.scanning = true
        state.progress = ""
        state.job = scope.launch {
            val found = runCatching { Cleaner.scan { state.progress = it } }.getOrDefault(emptyList())
            state.selected.clear()
            found.filter { it.type.preselected }.forEach { state.selected[it.path] = true }
            state.results = found
            state.scanning = false
        }
    }

    val results = state.results
    val selectedFiles = results.orEmpty().filter { state.selected[it.path] == true }
    val selectedBytes = selectedFiles.sumOf { it.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp, end = 12.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Ryd op", fontSize = 34.sp, fontWeight = FontWeight.Light, color = Color.White,
                modifier = Modifier.padding(start = 8.dp))
        }
        item {
            GlassCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Lagerplads", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (storage.total > 0) storage.used.toFloat() / storage.total else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${size(storage.used)} brugt · ${size(storage.free)} fri af ${size(storage.total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        if (!hasAccess) {
            item {
                GlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("Giv adgang til dine filer", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "For at finde unødvendige filer skal Ryddig Start have adgang til telefonens lager. " +
                                "Intet slettes, før du selv har set listen og trykket \"Slet\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            if (!Cleaner.openAllFilesAccessSettings(context)) {
                                legacyPermission.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    )
                                )
                            }
                        }) { Text("Giv adgang") }
                    }
                }
            }
        } else {
            item {
                GlassCard {
                    Column(Modifier.padding(16.dp)) {
                        if (state.scanning) {
                            Text("Scanner telefonen …", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = Color.White.copy(alpha = 0.15f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(state.progress, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.7f))
                        } else {
                            Text(
                                if (results == null) "Find unødvendige filer" else "Scan igen",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Finder installationsfiler, midlertidige filer, miniaturebilleder, tomme mapper, " +
                                    "papirkurv, gamle downloads og store filer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = ::startScan) { Text(if (results == null) "Start scanning" else "Scan igen") }
                        }
                    }
                }
            }
        }

        state.lastResult?.let { r ->
            item {
                GlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("✅ Frigjort ${size(r.freedBytes)}", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(
                            "${r.deleted} elementer slettet" + if (r.failed > 0) " · ${r.failed} kunne ikke slettes" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        if (results != null && !state.scanning) {
            if (results.isEmpty()) {
                item {
                    GlassCard {
                        Text("🎉 Ingen unødvendige filer fundet. Telefonen er ryddig!", modifier = Modifier.padding(16.dp))
                    }
                }
            }
            val byType = results.groupBy { it.type }
            for (type in JunkType.entries) {
                val files = byType[type] ?: continue
                val isExpanded = type in state.expanded
                val selCount = files.count { state.selected[it.path] == true }
                item(key = "type-${type.name}") {
                    GlassCard {
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { if (isExpanded) state.expanded.remove(type) else state.expanded.add(type) }
                                    .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selCount == files.size,
                                    onCheckedChange = { on ->
                                        files.forEach { if (on) state.selected[it.path] = true else state.selected.remove(it.path) }
                                    },
                                    colors = checkboxColors(),
                                )
                                Text(type.emoji, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(type.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${files.size} stk · ${size(files.sumOf { it.size })}" +
                                            if (selCount in 1 until files.size) " · $selCount valgt" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.75f),
                                    )
                                }
                                Icon(
                                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null, tint = Color.White.copy(alpha = 0.8f),
                                )
                            }
                            if (isExpanded) {
                                Text(
                                    type.description,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
                if (isExpanded) {
                    items(files.take(300), key = { it.path }) { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp)
                                .clickable {
                                    if (state.selected[f.path] == true) state.selected.remove(f.path) else state.selected[f.path] = true
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.selected[f.path] == true,
                                onCheckedChange = { on -> if (on) state.selected[f.path] = true else state.selected.remove(f.path) },
                                colors = checkboxColors(),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                Text(
                                    f.path.substringBeforeLast('/').substringAfter("/emulated/0").ifEmpty { "/" },
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                            if (!f.isDirectory) {
                                Text(size(f.size), style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    }
                    if (files.size > 300) {
                        item {
                            Text("… og ${files.size - 300} mere (vælges med afkrydsningen ovenfor)",
                                modifier = Modifier.padding(start = 24.dp),
                                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            if (results.isNotEmpty()) {
                item {
                    Button(
                        onClick = { confirm = true },
                        enabled = selectedFiles.isNotEmpty() && !state.deleting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(
                            if (state.deleting) "Sletter …"
                            else "Slet ${selectedFiles.size} valgte (${size(selectedBytes)})",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        item {
            GlassCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Andre apps' cache", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Android tillader ikke, at apps sletter hinandens cache. Brug telefonens egen oprydning, " +
                            "eller hold fingeren på en app på startskærmen → App-info → Lager → Ryd cache.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { Cleaner.openSystemCleanup(context) }) {
                        Text("Åbn telefonens oprydning", color = Color.White)
                    }
                }
            }
        }
        item { Box(Modifier.height(8.dp)) }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Slet ${selectedFiles.size} elementer?") },
            text = { Text("Du frigør ca. ${size(selectedBytes)}. Slettede filer kan ikke gendannes.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    val toDelete = selectedFiles
                    state.deleting = true
                    scope.launch {
                        val r = Cleaner.delete(context, toDelete)
                        val gone = toDelete.map { it.path }.toSet()
                        state.results = state.results?.filterNot { it.path in gone && !java.io.File(it.path).exists() }
                        gone.forEach { state.selected.remove(it) }
                        state.lastResult = r
                        state.deleting = false
                        storage = Cleaner.storageInfo()
                    }
                }) { Text("Slet", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuller") } },
        )
    }
}

@Composable
private fun checkboxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colorScheme.primary,
    uncheckedColor = Color.White.copy(alpha = 0.7f),
    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
)
