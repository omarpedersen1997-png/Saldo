package dk.sonderborg.borger

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NewsWorker.createChannel(this)
        if (Prefs(this).notificationsEnabled) NewsWorker.schedule(this)
        setContent { BorgerTheme { BorgerApp() } }
    }
}

// ---------- Hjælpefunktioner til at åbne links, ringe og skrive ----------

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Ingen app kan åbne linket", Toast.LENGTH_SHORT).show()
    }
}

fun Context.dial(number: String) {
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.replace(" ", ""))))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Kan ikke ringe fra denne enhed", Toast.LENGTH_SHORT).show()
    }
}

fun Context.email(address: String) {
    try {
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Ingen mail-app fundet", Toast.LENGTH_SHORT).show()
    }
}

fun Context.share(item: NewsItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, item.title + "\n" + item.link)
    }
    startActivity(Intent.createChooser(send, "Del nyhed"))
}

// ---------- App-tilstand ----------

class AppState(context: Context) {
    val prefs = Prefs(context)
    val repo = NewsRepository(prefs)
    var sources by mutableStateOf(prefs.sources)
    var items by mutableStateOf(prefs.cachedItems)
    var bookmarks by mutableStateOf(prefs.bookmarks)
    var status by mutableStateOf(prefs.sourceStatus)
    var lastUpdated by mutableStateOf(prefs.lastUpdated)
    var localOnly by mutableStateOf(prefs.localOnly)
    var notifications by mutableStateOf(prefs.notificationsEnabled)
    var refreshing by mutableStateOf(false)
    var errors by mutableStateOf<Map<String, String>>(emptyMap())

    val visibleItems: List<NewsItem> get() = repo.visible(items, sources)

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        try {
            val result = repo.refresh()
            items = result.items
            errors = result.errors
            status = prefs.sourceStatus
            lastUpdated = prefs.lastUpdated
            repo.markAllSeen(repo.visible(result.items, sources))
        } finally {
            refreshing = false
        }
    }

    fun toggleBookmark(item: NewsItem) {
        bookmarks = if (bookmarks.any { it.id == item.id }) bookmarks.filterNot { it.id == item.id }
        else listOf(item) + bookmarks
        prefs.bookmarks = bookmarks
    }

    fun updateSources(list: List<FeedSource>) {
        sources = list
        prefs.sources = list
    }

    fun setLocal(value: Boolean) {
        localOnly = value
        prefs.localOnly = value
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    News("Nyheder", Icons.Outlined.Newspaper),
    Topics("Borger", Icons.Outlined.GridView),
    Contact("Kontakt", Icons.Outlined.Phone),
    Settings("Indstillinger", Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorgerApp() {
    val context = LocalContext.current
    val state = remember { AppState(context) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openTopic by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val stale = System.currentTimeMillis() - state.lastUpdated > 15 * 60 * 1000
        if (stale || state.items.isEmpty()) state.refresh()
    }

    BackHandler(enabled = openTopic != null || tab != 0) {
        if (openTopic != null) openTopic = null else tab = 0
    }

    val topic = Content.topics.firstOrNull { it.id == openTopic }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            topic?.title ?: Tab.entries[tab].let { if (it == Tab.News) "Sønderborg Borger" else it.label },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (topic == null && tab == 0) {
                            Text(
                                "Nyt fra din kommune og dit lokalområde",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (topic != null) {
                        IconButton(onClick = { openTopic = null }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Tilbage")
                        }
                    }
                },
                colors = run {
                    val light = MaterialTheme.colorScheme.primary == Sea
                    val container = if (light) Sea else MaterialTheme.colorScheme.primaryContainer
                    val content = if (light) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = container,
                        titleContentColor = content,
                        navigationIconContentColor = content,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; openTopic = null },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                topic != null -> TopicDetail(topic)
                tab == 0 -> NewsScreen(state)
                tab == 1 -> TopicsScreen(onOpen = { openTopic = it })
                tab == 2 -> ContactScreen()
                else -> SettingsScreen(state)
            }
        }
    }
}

// ---------- Nyheder ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(state: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var sourceFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showSaved by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val base = if (showSaved) state.bookmarks else state.visibleItems
    val q = query.trim().lowercase()
    val shown = base.filter { item ->
        (sourceFilter == null || item.sourceId == sourceFilter) &&
            (q.isEmpty() || item.title.lowercase().contains(q) || item.summary.lowercase().contains(q))
    }
    val bookmarkIds = state.bookmarks.map { it.id }.toSet()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { scope.launch { state.refresh() } },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Søg i nyheder") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Ryd søgning")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = showSaved,
                            onClick = { showSaved = !showSaved },
                            label = { Text("Gemte (${state.bookmarks.size})") },
                            leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, Modifier.size(18.dp)) },
                        )
                    }
                    item {
                        FilterChip(selected = sourceFilter == null, onClick = { sourceFilter = null }, label = { Text("Alle kilder") })
                    }
                    items(state.sources.filter { it.enabled }, key = { it.id }) { src ->
                        FilterChip(
                            selected = sourceFilter == src.id,
                            onClick = { sourceFilter = if (sourceFilter == src.id) null else src.id },
                            label = { Text(src.name) },
                        )
                    }
                }
            }
            if (state.errors.isNotEmpty() && !showSaved) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Nogle kilder kunne ikke hentes: " +
                                    state.errors.entries.joinToString("; ") { "${it.key} (${it.value})" } +
                                    ". Du kan rette kilderne under Indstillinger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            if (state.lastUpdated > 0 && !showSaved) {
                item {
                    Text(
                        "Opdateret " + DateUtils.getRelativeTimeSpanString(state.lastUpdated).toString().lowercase() +
                            " · træk ned for at opdatere",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            if (shown.isEmpty()) {
                item {
                    EmptyState(
                        when {
                            state.refreshing -> "Henter nyheder …"
                            showSaved -> "Du har ikke gemt nogen nyheder endnu. Tryk på bogmærket på en nyhed for at gemme den."
                            q.isNotEmpty() -> "Ingen nyheder matcher \"$query\"."
                            else -> "Ingen nyheder lige nu. Træk ned for at opdatere, eller tjek dine kilder under Indstillinger."
                        }
                    )
                }
            }
            items(shown, key = { it.id }) { item ->
                NewsCard(
                    item = item,
                    saved = item.id in bookmarkIds,
                    onOpen = { context.openUrl(item.link) },
                    onSave = { state.toggleBookmark(item) },
                    onShare = { context.share(item) },
                )
            }
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit, onShare: () -> Unit) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        item.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (item.publishedMillis > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DateUtils.getRelativeTimeSpanString(
                            item.publishedMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp),
            )
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Del") }
                IconButton(onClick = onSave) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (saved) "Fjern fra gemte" else "Gem",
                        tint = if (saved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onOpen) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Åbn") }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Newspaper,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ---------- Borgeremner ----------

@Composable
fun TopicsScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Hvad skal du have hjælp til?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Søg på kommunens hjemmeside") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (search.isNotBlank()) context.openUrl(Content.kommuneSearch(search.trim()))
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(Content.topics, key = { it.id }) { topic ->
            Card(
                onClick = { onOpen(topic.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(topic.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            topic.searchTerms.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Disclaimer() }
    }
}

@Composable
fun TopicDetail(topic: Content.Topic) {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(topic.intro, style = MaterialTheme.typography.bodyLarge)
        }
        if (topic.tips.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Godt at vide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        topic.tips.forEach {
                            Spacer(Modifier.height(6.dp))
                            Text("• $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        item { Text("Selvbetjening og links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(topic.links) { link ->
            LinkRow(link.title, link.subtitle, Icons.AutoMirrored.Outlined.OpenInNew) { context.openUrl(link.url) }
        }
        item {
            Text("Søg på kommunens hjemmeside", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topic.searchTerms) { term ->
                    OutlinedButton(onClick = { context.openUrl(Content.kommuneSearch(term)) }) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(term)
                    }
                }
            }
        }
        item {
            FilledTonalButton(onClick = { context.dial(Content.BORGERSERVICE_PHONE) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ring til Borgerservice: ${Content.BORGERSERVICE_PHONE}")
            }
        }
    }
}

@Composable
fun LinkRow(title: String, subtitle: String?, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------- Kontakt ----------

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Borgerservice Sønderborg", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(Content.BORGERSERVICE_ADDRESS, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { context.dial(Content.BORGERSERVICE_PHONE) }) {
                            Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Ring")
                        }
                        FilledTonalButton(onClick = { context.email(Content.BORGERSERVICE_EMAIL) }) {
                            Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Skriv")
                        }
                        FilledTonalButton(onClick = {
                            context.openUrl("geo:0,0?q=" + Uri.encode(Content.BORGERSERVICE_ADDRESS))
                        }) {
                            Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Kort")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Tlf. ${Content.BORGERSERVICE_PHONE} · ${Content.BORGERSERVICE_EMAIL}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Åbnings- og telefontider ændrer sig ved helligdage – tjek altid de aktuelle tider.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    TextButton(onClick = { context.openUrl(Content.kommuneSearch("borgerservice åbningstider")) }) {
                        Text("Se aktuelle åbningstider")
                    }
                }
            }
        }
        item {
            LinkRow("Sønderborg Kommunes hjemmeside", "sonderborgkommune.dk", Icons.Outlined.Public) {
                context.openUrl(Content.KOMMUNE_SITE)
            }
        }
        item {
            LinkRow("Byrådets dagsordener og referater", "Følg med i politikken", Icons.AutoMirrored.Outlined.OpenInNew) {
                context.openUrl(Content.DAGSORDENER)
            }
        }
        item {
            Text("Vigtige telefonnumre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp))
        }
        items(Content.importantNumbers) { phone ->
            Card(
                onClick = { context.dial(phone.number) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(phone.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        if (phone.note != null) {
                            Text(phone.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(phone.number, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.Call, contentDescription = "Ring ${phone.number}", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Disclaimer() }
    }
}

@Composable
fun Disclaimer() {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Uofficiel borger-app. Den er ikke udgivet af Sønderborg Kommune. Oplysninger kan ændre sig – " +
                "kommunens egen hjemmeside er altid den gældende kilde.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- Indstillinger ----------

@Composable
fun SettingsScreen(state: AppState) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Notifikationer er slået fra i Android-indstillingerne", Toast.LENGTH_LONG).show()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("Hold dig opdateret") }
        item {
            SettingSwitch(
                title = "Besked om nye nyheder",
                subtitle = "Appen tjekker dine kilder ca. hver 3. time",
                checked = state.notifications,
            ) { on ->
                state.notifications = on
                state.prefs.notificationsEnabled = on
                if (on) {
                    NewsWorker.schedule(context)
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    NewsWorker.cancel(context)
                }
            }
        }
        item {
            SettingSwitch(
                title = "Kun lokale nyheder fra regionale kilder",
                subtitle = "Viser kun nyheder fra fx DR Syd, der nævner Sønderborg, Als, Nordborg, Gråsten m.fl.",
                checked = state.localOnly,
            ) { state.setLocal(it) }
        }

        item { SectionTitle("Nyhedskilder") }
        items(state.sources, key = { it.id }) { src ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(src.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(src.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = src.enabled, onCheckedChange = { on ->
                            state.updateSources(state.sources.map { if (it.id == src.id) it.copy(enabled = on) else it })
                        })
                        if (!src.builtIn) {
                            IconButton(onClick = { state.updateSources(state.sources.filterNot { it.id == src.id }) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Fjern ${src.name}")
                            }
                        }
                    }
                    val st = state.status[src.id]
                    if (st != null && src.enabled) {
                        Text(
                            st,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (st.startsWith("Fejl")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Tilføj kilde")
                }
                TextButton(onClick = {
                    val custom = state.sources.filterNot { it.builtIn }
                    state.updateSources(Content.defaultSources + custom)
                }) { Text("Nulstil standardkilder") }
            }
        }
        item {
            Text(
                "Du kan tilføje alle RSS- eller Atom-feeds – fx din lokale avis, skole eller forening. " +
                    "Indsætter du en almindelig hjemmeside, forsøger appen selv at finde sidens feed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { SectionTitle("Om appen") }
        item {
            Text(
                "Sønderborg Borger samler nyheder, selvbetjening og kontaktoplysninger ét sted. " +
                    "Alle data gemmes kun på din telefon. Appen indsamler ingen personoplysninger.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item { Disclaimer() }
    }

    if (showAdd) {
        AddSourceDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, url ->
                val id = "custom-" + System.currentTimeMillis()
                state.updateSources(state.sources + FeedSource(id = id, name = name, url = url))
                showAdd = false
            },
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
fun AddSourceDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    val valid = name.isNotBlank() && (url.startsWith("https://") || url.startsWith("http://")) && url.length > 10
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tilføj nyhedskilde") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Navn") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text("Adresse (RSS eller hjemmeside)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onAdd(name.trim(), url.trim()) }) { Text("Tilføj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuller") } },
    )
}
