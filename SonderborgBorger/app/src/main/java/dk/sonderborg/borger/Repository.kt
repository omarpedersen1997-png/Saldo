package dk.sonderborg.borger

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Lokal lagring i SharedPreferences. Alt bliver på telefonen. */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("borger", Context.MODE_PRIVATE)

    var sources: List<FeedSource>
        get() {
            val raw = sp.getString("sources", null) ?: return Content.defaultSources
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    FeedSource(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        url = o.getString("url"),
                        enabled = o.optBoolean("enabled", true),
                        localFilter = o.optBoolean("localFilter", false),
                        builtIn = o.optBoolean("builtIn", false),
                    )
                }
            }.getOrDefault(Content.defaultSources)
        }
        set(value) {
            val arr = JSONArray()
            value.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.id).put("name", it.name).put("url", it.url)
                        .put("enabled", it.enabled).put("localFilter", it.localFilter).put("builtIn", it.builtIn)
                )
            }
            sp.edit().putString("sources", arr.toString()).apply()
        }

    var cachedItems: List<NewsItem>
        get() = itemsFromJson(sp.getString("items", null))
        set(value) = sp.edit().putString("items", itemsToJson(value)).apply()

    var bookmarks: List<NewsItem>
        get() = itemsFromJson(sp.getString("bookmarks", null))
        set(value) = sp.edit().putString("bookmarks", itemsToJson(value)).apply()

    var seenIds: Set<String>
        get() = sp.getStringSet("seen", emptySet())!!.toSet()
        set(value) = sp.edit().putStringSet("seen", value.toSet()).apply()

    var notificationsEnabled: Boolean
        get() = sp.getBoolean("notify", true)
        set(value) = sp.edit().putBoolean("notify", value).apply()

    var localOnly: Boolean
        get() = sp.getBoolean("localOnly", true)
        set(value) = sp.edit().putBoolean("localOnly", value).apply()

    var lastUpdated: Long
        get() = sp.getLong("lastUpdated", 0L)
        set(value) = sp.edit().putLong("lastUpdated", value).apply()

    var sourceStatus: Map<String, String>
        get() = runCatching {
            val o = JSONObject(sp.getString("status", "{}")!!)
            o.keys().asSequence().associateWith { o.getString(it) }
        }.getOrDefault(emptyMap())
        set(value) = sp.edit().putString("status", JSONObject(value as Map<*, *>).toString()).apply()

    private fun itemsToJson(items: List<NewsItem>): String {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id).put("sourceId", it.sourceId).put("sourceName", it.sourceName)
                    .put("title", it.title).put("summary", it.summary).put("link", it.link)
                    .put("published", it.publishedMillis)
            )
        }
        return arr.toString()
    }

    private fun itemsFromJson(raw: String?): List<NewsItem> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NewsItem(
                    id = o.getString("id"),
                    sourceId = o.getString("sourceId"),
                    sourceName = o.getString("sourceName"),
                    title = o.getString("title"),
                    summary = o.optString("summary"),
                    link = o.getString("link"),
                    publishedMillis = o.optLong("published"),
                )
            }
        }.getOrDefault(emptyList())
    }
}

data class RefreshResult(val items: List<NewsItem>, val newItems: List<NewsItem>, val errors: Map<String, String>)

class NewsRepository(private val prefs: Prefs) {

    /** Nyheder som brugeren skal se, efter filtrering af regionale kilder. */
    fun visible(items: List<NewsItem>, sources: List<FeedSource> = prefs.sources): List<NewsItem> {
        val byId = sources.associateBy { it.id }
        return items.filter { item ->
            val src = byId[item.sourceId] ?: return@filter false
            src.enabled && (!prefs.localOnly || !src.localFilter || FeedParser.isLocal(item))
        }
    }

    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        val sources = prefs.sources.filter { it.enabled }
        val previous = prefs.cachedItems
        val results = coroutineScope {
            sources.map { src -> async { src to runCatching { fetch(src) } } }.awaitAll()
        }
        val status = prefs.sourceStatus.toMutableMap()
        val errors = mutableMapOf<String, String>()
        val fresh = mutableListOf<NewsItem>()
        val failedIds = mutableSetOf<String>()
        for ((src, result) in results) {
            result.onSuccess { list ->
                fresh += list
                status[src.id] = "OK · ${list.size} nyheder"
            }.onFailure { e ->
                failedIds += src.id
                val msg = friendlyError(e)
                errors[src.name] = msg
                status[src.id] = "Fejl: $msg"
            }
        }
        // Behold gamle nyheder fra kilder, der fejlede denne gang, så listen ikke tømmes ved netværksfejl.
        val kept = previous.filter { it.sourceId in failedIds }
        val merged = FeedParser.merge(fresh + kept)
        val seen = prefs.seenIds
        val newItems = if (seen.isEmpty()) emptyList() else visible(merged).filter { it.id !in seen }

        prefs.cachedItems = merged
        prefs.sourceStatus = status
        prefs.lastUpdated = System.currentTimeMillis()
        RefreshResult(merged, newItems, errors)
    }

    fun markAllSeen(items: List<NewsItem>) {
        // Begræns størrelsen, så listen ikke vokser for evigt.
        prefs.seenIds = (items.map { it.id } + prefs.seenIds).take(1500).toSet()
    }

    private fun fetch(source: FeedSource): List<NewsItem> {
        var bytes = download(source.url)
        if (FeedParser.looksLikeHtml(bytes)) {
            val feedUrl = FeedParser.discoverFeedUrl(String(bytes, Charsets.UTF_8), source.url)
                ?: throw IllegalStateException("Siden har ikke noget RSS-feed")
            bytes = download(feedUrl)
        }
        return FeedParser.parse(bytes, source)
    }

    private fun download(start: String): ByteArray {
        var url = start
        repeat(6) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "SonderborgBorger/1.0 (Android; nyhedslæser)")
                setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, text/html;q=0.8")
            }
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: throw IllegalStateException("Omdirigering uden adresse")
                    url = URL(URL(url), loc).toString()
                    return@repeat
                }
                if (code !in 200..299) throw IllegalStateException("Serveren svarede $code")
                conn.inputStream.use { input ->
                    val out = ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    var total = 0
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > 5_000_000) throw IllegalStateException("Feedet er for stort")
                        out.write(buf, 0, n)
                    }
                    return out.toByteArray()
                }
            } finally {
                conn.disconnect()
            }
        }
        throw IllegalStateException("For mange omdirigeringer")
    }

    private fun friendlyError(e: Throwable): String = when (e) {
        is java.net.UnknownHostException -> "Ingen forbindelse til serveren"
        is java.net.SocketTimeoutException -> "Serveren svarede ikke i tide"
        is org.xml.sax.SAXException -> "Kunne ikke læse feedet"
        else -> e.message ?: e.javaClass.simpleName
    }
}
