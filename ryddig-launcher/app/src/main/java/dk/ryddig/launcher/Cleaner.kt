package dk.ryddig.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class JunkType(val title: String, val emoji: String, val description: String, val preselected: Boolean) {
    APK(
        "Installationsfiler (APK)", "📦",
        "Downloadede installationsfiler. Appen er typisk allerede installeret, så filen fylder bare.", true,
    ),
    TEMP(
        "Midlertidige filer og logs", "🧾",
        "Filer som .tmp, .log, .bak og afbrudte downloads.", true,
    ),
    THUMBS(
        "Miniaturebilleder", "🖼️",
        "Små kopier af billeder i .thumbnails-mapper. Telefonen laver dem igen efter behov.", true,
    ),
    EMPTY_DIRS(
        "Tomme mapper", "📁",
        "Mapper uden indhold, som apps har efterladt.", true,
    ),
    TRASH(
        "Papirkurv", "🗑️",
        "Filer som allerede ligger i papirkurven i galleri- eller fil-appen.", false,
    ),
    OLD_DOWNLOADS(
        "Gamle downloads", "⏳",
        "Filer i Download, som ikke er ændret i over 90 dage. Gennemse dem før du sletter.", false,
    ),
    LARGE(
        "Store filer (over 100 MB)", "🐘",
        "Kan være vigtige videoer eller backups – vælg selv, hvad der skal væk.", false,
    ),
}

data class JunkFile(
    val path: String,
    val name: String,
    val size: Long,
    val type: JunkType,
    val isDirectory: Boolean,
)

data class DeleteResult(val deleted: Int, val freedBytes: Long, val failed: Int)

data class StorageInfo(val total: Long, val free: Long) {
    val used: Long get() = total - free
}

object Cleaner {
    private val tempExtensions = setOf("tmp", "temp", "log", "bak", "old", "part", "crdownload", "partial", "dmp")
    private val apkExtensions = setOf("apk", "xapk", "apks", "apkm")
    private val protectedTopLevel = setOf(
        "android", "alarms", "audiobooks", "dcim", "documents", "download", "movies", "music",
        "notifications", "pictures", "podcasts", "recordings", "ringtones", "screenshots",
    )
    private const val LARGE_FILE = 100L * 1024 * 1024
    private val OLD_AGE = TimeUnit.DAYS.toMillis(90)

    fun hasAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** Android 11+: åbner indstillingen "Adgang til alle filer". Returnerer false på ældre Android. */
    fun openAllFilesAccessSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val specific = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        val generic = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        return runCatching { context.startActivity(specific) }.isSuccess ||
            runCatching { context.startActivity(generic) }.isSuccess
    }

    /** Androids egen "Frigør plads"-skærm (kan bl.a. rydde andre apps' cache). */
    fun openSystemCleanup(context: Context) {
        val intents = listOf(
            Intent(StorageManager.ACTION_MANAGE_STORAGE),
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        )
        for (intent in intents) {
            if (runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    fun storageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(stat.totalBytes, stat.availableBytes)
    }

    suspend fun scan(onProgress: (String) -> Unit): List<JunkFile> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val rootPath = root.absolutePath
        val now = System.currentTimeMillis()
        val result = ArrayList<JunkFile>()
        val stack = ArrayDeque<Pair<File, Int>>()
        stack.addLast(root to 0)
        var visited = 0

        while (stack.isNotEmpty()) {
            ensureActive()
            val (dir, depth) = stack.removeLast()
            val isTopLevel = dir.parentFile?.absolutePath == rootPath
            if (isTopLevel && dir.name.equals("Android", ignoreCase = true)) continue // app-data: rør ikke

            val children = dir.listFiles() ?: continue
            visited++
            if (visited % 50 == 0) onProgress(dir.absolutePath.removePrefix(rootPath).ifEmpty { "/" })

            if (children.isEmpty()) {
                val protected = dir.absolutePath == rootPath ||
                    (isTopLevel && dir.name.lowercase() in protectedTopLevel)
                if (!protected) result += JunkFile(dir.absolutePath, dir.name, 0, JunkType.EMPTY_DIRS, true)
                continue
            }

            val inThumbs = dir.absolutePath.split('/').any { it.equals(".thumbnails", ignoreCase = true) }
            val inDownload = dir.absolutePath.removePrefix(rootPath).trimStart('/')
                .startsWith("Download", ignoreCase = true)

            for (f in children) {
                if (f.isDirectory) {
                    if (depth < 15) stack.addLast(f to depth + 1)
                    continue
                }
                val name = f.name
                val ext = name.substringAfterLast('.', "").lowercase()
                val size = f.length()
                val type = when {
                    inThumbs -> JunkType.THUMBS
                    name.startsWith(".trashed-") -> JunkType.TRASH
                    ext in apkExtensions -> JunkType.APK
                    ext in tempExtensions || name.startsWith("~$") -> JunkType.TEMP
                    inDownload && now - f.lastModified() > OLD_AGE -> JunkType.OLD_DOWNLOADS
                    size >= LARGE_FILE -> JunkType.LARGE
                    else -> null
                }
                if (type != null) result += JunkFile(f.absolutePath, name, size, type, false)
            }
        }
        result.sortedByDescending { it.size }
    }

    suspend fun delete(context: Context, files: List<JunkFile>): DeleteResult = withContext(Dispatchers.IO) {
        var deleted = 0
        var failed = 0
        var freed = 0L
        val removedPaths = ArrayList<String>()
        // Filer først, derefter mapper (dybeste først), så mapper er tomme når de slettes.
        val ordered = files.sortedWith(compareBy<JunkFile> { it.isDirectory }.thenByDescending { it.path.length })
        for (item in ordered) {
            val f = File(item.path)
            val ok = when {
                !f.exists() -> true
                item.isDirectory -> (f.listFiles()?.isEmpty() ?: false) && f.delete()
                else -> f.delete()
            }
            if (ok) {
                deleted++
                freed += item.size
                removedPaths += item.path
            } else {
                failed++
            }
        }
        // Fortæl telefonens mediedatabase, at filerne er væk (så galleriet ikke viser spøgelsesfiler).
        if (removedPaths.isNotEmpty()) {
            runCatching {
                MediaScannerConnection.scanFile(context, removedPaths.toTypedArray(), null, null)
            }
        }
        DeleteResult(deleted, freed, failed)
    }
}
