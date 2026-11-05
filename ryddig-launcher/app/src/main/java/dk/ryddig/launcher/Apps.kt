package dk.ryddig.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

enum class Category(val title: String, val emoji: String) {
    KOMMUNIKATION("Kommunikation", "💬"),
    SOCIALE("Sociale medier", "👥"),
    INTERNET("Internet", "🌐"),
    FOTO("Foto & video", "📷"),
    MUSIK("Musik & lyd", "🎵"),
    SPIL("Spil", "🎮"),
    OEKONOMI("Økonomi", "💳"),
    SHOPPING("Shopping & mad", "🛒"),
    REJSER("Kort & rejser", "🗺️"),
    NYHEDER("Nyheder & læsning", "📰"),
    PRODUKTIVITET("Produktivitet", "📋"),
    SUNDHED("Sundhed & træning", "❤️"),
    VAERKTOEJ("Værktøjer", "🧰"),
    SYSTEM("System", "⚙️"),
    ANDET("Andet", "📦"),
    SKJULT("Skjulte apps", "🙈"),
}

data class AppEntry(
    val key: String,
    val packageName: String,
    val activityName: String,
    val label: String,
    val bitmap: Bitmap,
    val autoCategory: Category,
) {
    val icon: ImageBitmap by lazy { bitmap.asImageBitmap() }
}

/** Automatisk sortering ud fra pakkenavn, appnavn og Androids egen kategori. */
object Categorizer {
    // Rækkefølgen betyder noget: første match vinder.
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.SYSTEM to listOf(
            "com.android.settings", "packageinstaller", "android.vending", "galaxy.store",
            "samsungapps", "devicecare", "=lool", "securitysettings", "themestore", "simtoolkit", "=stk",
        ),
        Category.KOMMUNIKATION to listOf(
            "dialer", "=phone", "contacts", "messag", "messenger", "orca", "=mms", "=sms",
            "whatsapp", "telegram", "signal", "viber", "skype", "gmail", "outlook", "mail",
            "teams", "zoom", "=meet", "=duo", "telefon", "beskeder",
        ),
        Category.SOCIALE to listOf(
            "facebook", "instagram", "snapchat", "tiktok", "musically", "twitter", "reddit",
            "linkedin", "pinterest", "tumblr", "discord", "threads", "bereal", "mastodon",
            "bsky", "bluesky", "tinder", "hinge", "bumble",
        ),
        Category.INTERNET to listOf(
            "chrome", "browser", "firefox", "opera", "brave", "duckduckgo", "=edge",
            "vivaldi", "sbrowser", "webview",
        ),
        Category.MUSIK to listOf(
            "music", "musik", "spotify", "soundcloud", "podcast", "audible", "=radio", "deezer",
            "tidal", "shazam", "recorder", "optager", "storytel", "mofibo", "=nota",
        ),
        Category.FOTO to listOf(
            "camera", "kamera", "gallery", "galleri", "photo", "=foto", "=video", "youtube",
            "netflix", "hbo", "disney", "primevideo", "avod", "viaplay", "tv2play", "drtv",
            "dr.tv", "twitch", "vlc", "capcut", "lightroom", "snapseed", "picsart", "max.",
        ),
        Category.OEKONOMI to listOf(
            "bank", "mobilepay", "paypal", "wallet", "walletnfc", "mitid", "nemid", "eboks",
            "e-boks", "=skat", "=saldo", "vipps", "nykredit", "nordea", "danske", "jyske",
            "sydbank", "lunar", "revolut", "spiir", "aktie", "nordnet", "saxo", "=invest",
            "crypto", "coinbase", "binance", "=borger",
        ),
        Category.SHOPPING to listOf(
            "amazon", "=shop", "ebay", "temu", "shein", "aliexpress", "dba", "zalando",
            "coop", "netto", "salling", "lidl", "rema", "ikea", "vinted", "bilka", "foetex",
            "tjek", "etilbudsavis", "justeat", "just-eat", "wolt", "hungry", "toogoodtogo",
            "mcdonald", "burgerking", "sunset", "matas", "elgiganten", "power.", "proshop",
        ),
        Category.REJSER to listOf(
            "maps", "=kort", "waze", "navigation", "rejseplan", "rejsekort", "dsb", "uber",
            "=bolt", "airbnb", "booking", "=trip", "flight", "sas.", "norwegian", "ryanair",
            "parkering", "easypark", "parkman", "apcoa", "taxa", "=dott", "=lime", "=tier",
            "midttrafik", "movia", "flixbus",
        ),
        Category.NYHEDER to listOf(
            "=news", "nyhed", "politiken", "berlingske", "jyllands", "ekstrabladet", "dr.dk",
            "tv2.", "bt.", "zetland", "kindle", "=books", "=reader", "readly", "wattpad",
            "flipboard", "feedly",
        ),
        Category.PRODUKTIVITET to listOf(
            "=docs", "sheets", "slides", "=office", "=word", "excel", "powerpoint", "=drive",
            "dropbox", "onedrive", "=notes", "=noter", "=keep", "calendar", "kalender", "todo",
            "=task", "notion", "evernote", "onenote", "trello", "slack", "pdf", "scanner",
            "aula", "lectio", "minuddannelse", "forældreintra", "translate", "oversæt",
        ),
        Category.SUNDHED to listOf(
            "health", "sundhed", "=fit", "steps", "workout", "strava", "garmin", "medicin",
            "minlaege", "min.laege", "laege", "sleep", "=calm", "headspace", "fitbit",
            "runkeeper", "=nike", "adidas", "=polar", "suunto", "samsung.android.app.shealth",
            "lifesum", "myfitnesspal", "=sats", "fitnessworld",
        ),
        Category.VAERKTOEJ to listOf(
            "calculator", "lommeregner", "=clock", "=ur", "=alarm", "flashlight", "torch",
            "lommelygte", "weather", "vejr", "dmi", "yr.", "=files", "=filer", "myfiles",
            "filemanager", "compass", "kompas", "authenticator", "vpn", "=qr", "ruler",
            "=lens", "smartthings", "=home", "=remote", "=wear", "=buds", "bluetooth", "backup",
        ),
    )

    fun categorize(packageName: String, label: String, info: ApplicationInfo?): Category {
        if (info != null && isGame(info)) return Category.SPIL
        val haystack = "$packageName ${label.lowercase(Locale.ROOT)}"
        // Korte ord (f.eks. "=ur", "kort") skal matche et helt ord, ikke en del af et ord.
        for ((category, words) in rules) {
            if (words.any { matches(haystack, it) }) return category
        }
        if (info != null) {
            when (info.category) {
                ApplicationInfo.CATEGORY_AUDIO -> return Category.MUSIK
                ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> return Category.FOTO
                ApplicationInfo.CATEGORY_SOCIAL -> return Category.SOCIALE
                ApplicationInfo.CATEGORY_NEWS -> return Category.NYHEDER
                ApplicationInfo.CATEGORY_MAPS -> return Category.REJSER
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return Category.PRODUKTIVITET
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> return Category.VAERKTOEJ
            }
            if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) return Category.SYSTEM
        }
        return Category.ANDET
    }

    @Suppress("DEPRECATION")
    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME || (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0

    private fun matches(haystack: String, word: String): Boolean {
        if (!word.startsWith("=")) return haystack.contains(word)
        val w = Regex.escape(word.substring(1))
        return Regex("(^|[^a-z0-9æøå])$w($|[^a-z0-9æøå])").containsMatchIn(haystack)
    }
}

object AppRepository {
    /** @param only hvis sat: kun apps i denne kategori (inkl. brugerens egne flytninger). */
    suspend fun loadApps(context: Context, only: Category? = null, iconSizeDp: Int = 52): List<AppEntry> =
        withContext(Dispatchers.IO) {
        val overrides = if (only != null) LauncherPrefs(context).overrides() else emptyMap()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        val iconPx = (iconSizeDp * context.resources.displayMetrics.density).toInt().coerceIn(72, 192)
        val collator = Collator.getInstance(Locale.forLanguageTag("da-DK"))

        resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { ri ->
                runCatching {
                    val ai = ri.activityInfo
                    val label = ri.loadLabel(pm).toString().ifBlank { ai.packageName }
                    val key = "${ai.packageName}/${ai.name}"
                    val auto = Categorizer.categorize(ai.packageName, label, ai.applicationInfo)
                    if (only != null && (overrides[key] ?: auto) != only) return@runCatching null
                    AppEntry(
                        key = key,
                        packageName = ai.packageName,
                        activityName = ai.name,
                        label = label,
                        bitmap = ri.loadIcon(pm).toBitmap(iconPx, iconPx),
                        autoCategory = auto,
                    )
                }.getOrNull()
            }
            .distinctBy { it.key }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launch(context: Context, app: AppEntry) {
        launch(context, app.packageName, app.activityName)
    }

    fun launch(context: Context, packageName: String, activityName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(packageName, activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openAppInfo(context: Context, packageName: String) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun uninstall(context: Context, packageName: String) {
        runCatching {
            @Suppress("DEPRECATION")
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

/** Gemmer brugerens egne valg: flyttede apps og sammenklappede kategorier. */
class LauncherPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ryddig_start", Context.MODE_PRIVATE)

    fun overrides(): Map<String, Category> =
        prefs.all.mapNotNull { (k, v) ->
            if (!k.startsWith(PREFIX) || v !is String) return@mapNotNull null
            val cat = runCatching { Category.valueOf(v) }.getOrNull() ?: return@mapNotNull null
            k.removePrefix(PREFIX) to cat
        }.toMap()

    fun setOverride(appKey: String, category: Category?) {
        prefs.edit().apply {
            if (category == null) remove(PREFIX + appKey) else putString(PREFIX + appKey, category.name)
        }.apply()
    }

    fun clearOverrides() {
        prefs.edit().apply { prefs.all.keys.filter { it.startsWith(PREFIX) }.forEach { remove(it) } }.apply()
    }

    fun collapsed(): Set<String> = prefs.getStringSet(COLLAPSED, null) ?: setOf(Category.SKJULT.name)

    fun setCollapsed(value: Set<String>) {
        prefs.edit().putStringSet(COLLAPSED, value).apply()
    }

    private companion object {
        const val PREFIX = "cat:"
        const val COLLAPSED = "collapsed"
    }
}
