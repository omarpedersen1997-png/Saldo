package dk.sonderborg.borger

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** En nyhedskilde (RSS eller Atom). URL'en må også være en almindelig hjemmeside med et feed-link. */
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    /** Kun vis nyheder, der nævner byer/områder i Sønderborg Kommune (bruges til regionale kilder). */
    val localFilter: Boolean = false,
    val builtIn: Boolean = false,
)

data class NewsItem(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val summary: String,
    val link: String,
    val publishedMillis: Long,
)

/** Fortolker RSS 2.0, RSS 1.0 (RDF) og Atom. Ren JVM-kode, så den kan unit-testes. */
object FeedParser {

    /** Byer og områder i Sønderborg Kommune – bruges til at filtrere regionale nyhedskilder. */
    val LOCAL_KEYWORDS = listOf(
        "sønderborg", "sonderborg", "als", "alsingerne", "nordborg", "augustenborg", "gråsten",
        "broager", "egernsund", "dybbøl", "sundeved", "nybøl", "avnbøl", "ullerup", "blans",
        "guderup", "hørup", "høruphav", "kegnæs", "sydals", "lysabild", "mommark", "fynshav",
        "havnbjerg", "svenstrup", "hjortspring", "nordals", "midtals", "rinkenæs", "kværs",
        "adsbøl", "ketting", "stevning", "dynt", "skelde", "sottrup", "ulkebøl", "vollerup",
    )

    private val localRegex = Regex(
        "(^|[^\\p{L}])(" + LOCAL_KEYWORDS.joinToString("|") { Regex.escape(it) } + ")([^\\p{L}]|$)",
        RegexOption.IGNORE_CASE,
    )

    fun isLocal(item: NewsItem): Boolean = localRegex.containsMatchIn(item.title + " " + item.summary)

    fun looksLikeHtml(bytes: ByteArray): Boolean {
        val head = String(bytes, 0, minOf(bytes.size, 2048), Charsets.UTF_8).lowercase(Locale.ROOT)
        if (head.contains("<rss") || head.contains("<feed") || head.contains("<rdf:rdf")) return false
        return head.contains("<!doctype html") || head.contains("<html")
    }

    /** Finder et RSS/Atom-link i en HTML-side (<link rel="alternate" type="application/rss+xml" href="...">). */
    fun discoverFeedUrl(html: String, baseUrl: String): String? {
        val linkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
        for (match in linkTag.findAll(html)) {
            val tag = match.value
            val type = attr(tag, "type")?.lowercase(Locale.ROOT) ?: continue
            if (!type.contains("rss+xml") && !type.contains("atom+xml")) continue
            val href = attr(tag, "href") ?: continue
            return runCatching { URL(URL(baseUrl), decodeEntities(href)).toString() }.getOrNull()
        }
        return null
    }

    private fun attr(tag: String, name: String): String? {
        val m = Regex("\\b$name\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE).find(tag)
            ?: return null
        return m.groupValues.drop(2).firstOrNull { it.isNotEmpty() }
    }

    fun parse(bytes: ByteArray, source: FeedSource): List<NewsItem> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val start = bytes.indexOfFirst { it == '<'.code.toByte() }.coerceAtLeast(0)
        val clean = if (start > 0) bytes.copyOfRange(start, bytes.size) else bytes
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(clean))
        val root = doc.documentElement ?: return emptyList()

        val entries = elementsByLocalName(root, "item").ifEmpty { elementsByLocalName(root, "entry") }
        return entries.mapNotNull { el -> toItem(el, source) }
    }

    private fun toItem(el: Element, source: FeedSource): NewsItem? {
        val title = stripHtml(childText(el, "title") ?: "").ifBlank { return null }
        val link = findLink(el) ?: return null
        val rawSummary = childText(el, "description")
            ?: childText(el, "summary")
            ?: childText(el, "encoded")
            ?: childText(el, "content")
            ?: ""
        val summary = stripHtml(rawSummary).let { if (it.length > 400) it.take(397).trimEnd() + "…" else it }
        val date = parseDate(
            childText(el, "pubDate") ?: childText(el, "published") ?: childText(el, "updated")
                ?: childText(el, "date") ?: childText(el, "issued")
        )
        val guid = childText(el, "guid") ?: childText(el, "id") ?: link
        return NewsItem(
            id = source.id + "|" + guid.trim(),
            sourceId = source.id,
            sourceName = source.name,
            title = title,
            summary = summary,
            link = link.trim(),
            publishedMillis = date,
        )
    }

    private fun findLink(el: Element): String? {
        val links = directChildren(el).filter { it.localNameOrName() == "link" }
        // Atom: <link rel="alternate" href="..."/>
        links.firstOrNull { it.hasAttribute("href") && (it.getAttribute("rel").isEmpty() || it.getAttribute("rel") == "alternate") }
            ?.let { return it.getAttribute("href") }
        links.firstOrNull { it.hasAttribute("href") }?.let { return it.getAttribute("href") }
        // RSS: <link>https://...</link>
        links.map { it.textContent.trim() }.firstOrNull { it.startsWith("http") }?.let { return it }
        // Nogle feeds har kun en permalink-guid
        val guid = directChildren(el).firstOrNull { it.localNameOrName() == "guid" }?.textContent?.trim()
        return guid?.takeIf { it.startsWith("http") }
    }

    private fun Element.localNameOrName(): String = localName ?: nodeName.substringAfter(':')

    private fun directChildren(el: Element): List<Element> {
        val out = ArrayList<Element>()
        val nodes = el.childNodes
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) out += n as Element
        }
        return out
    }

    private fun childText(el: Element, localName: String): String? =
        directChildren(el).firstOrNull { it.localNameOrName() == localName }
            ?.textContent?.trim()?.takeIf { it.isNotEmpty() }

    private fun elementsByLocalName(root: Element, name: String): List<Element> {
        val list = root.getElementsByTagNameNS("*", name)
        val out = ArrayList<Element>()
        for (i in 0 until list.length) out += list.item(i) as Element
        if (out.isEmpty()) {
            val plain = root.getElementsByTagName(name)
            for (i in 0 until plain.length) out += plain.item(i) as Element
        }
        return out
    }

    fun parseDate(raw: String?): Long {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return 0L
        runCatching { return OffsetDateTime.parse(s).toInstant().toEpochMilli() }
        runCatching { return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm Z",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )
        for (p in patterns) {
            runCatching {
                val f = SimpleDateFormat(p, Locale.ENGLISH).apply { isLenient = false }
                return f.parse(s)!!.time
            }
        }
        return 0L
    }

    fun stripHtml(input: String): String {
        var s = input
        s = s.replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
        s = s.replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>"), " ")
        s = s.replace(Regex("(?s)<[^>]+>"), " ")
        s = decodeEntities(s)
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "aelig" to "æ", "oslash" to "ø", "aring" to "å", "AElig" to "Æ", "Oslash" to "Ø", "Aring" to "Å",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "laquo" to "«", "raquo" to "»",
        "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“", "euro" to "€", "eacute" to "é",
        "uuml" to "ü", "ouml" to "ö", "auml" to "ä",
    )

    fun decodeEntities(input: String): String =
        Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);").replace(input) { m ->
            val v = m.groupValues[1]
            when {
                v.startsWith("#x") || v.startsWith("#X") ->
                    v.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                v.startsWith("#") ->
                    v.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                else -> NAMED_ENTITIES[v] ?: m.value
            }
        }

    /** Sorterer nyeste først og fjerner dubletter (samme link fra flere kilder). */
    fun merge(items: List<NewsItem>, limit: Int = 400): List<NewsItem> =
        items.sortedByDescending { it.publishedMillis }
            .distinctBy { it.link.trimEnd('/').substringAfter("://").lowercase(Locale.ROOT) }
            .distinctBy { it.id }
            .take(limit)
}
