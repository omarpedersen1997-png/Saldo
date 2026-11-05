package dk.ryddig.launcher

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking

/** Husker hvilken kategori hver widget på startskærmen viser. */
object WidgetStore {
    private fun prefs(c: Context) = c.getSharedPreferences("ryddig_widgets", Context.MODE_PRIVATE)

    fun get(c: Context, widgetId: Int): Category? =
        prefs(c).getString("w$widgetId", null)?.let { runCatching { Category.valueOf(it) }.getOrNull() }

    fun set(c: Context, widgetId: Int, category: Category) {
        prefs(c).edit().putString("w$widgetId", category.name).apply()
    }

    fun remove(c: Context, widgetId: Int) {
        prefs(c).edit().remove("w$widgetId").apply()
    }
}

private val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

/** En boks på den nuværende startskærm, der viser alle apps i én kategori. */
class CategoryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, manager, it) }
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_grid)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetStore.remove(context, it) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val category = WidgetStore.get(context, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_category)

            if (category == null) {
                views.setTextViewText(R.id.widget_title, "Tryk for at vælge kategori")
                views.setViewVisibility(R.id.widget_grid, View.GONE)
                views.setViewVisibility(R.id.widget_empty, View.GONE)
                val config = Intent(context, WidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                views.setOnClickPendingIntent(
                    R.id.widget_title,
                    PendingIntent.getActivity(context, widgetId, config, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                )
            } else {
                views.setTextViewText(R.id.widget_title, "${category.emoji}  ${category.title}")
                views.setViewVisibility(R.id.widget_grid, View.VISIBLE)
                val openApp = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                views.setOnClickPendingIntent(
                    R.id.widget_title,
                    PendingIntent.getActivity(context, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                )

                val service = Intent(context, CategoryWidgetService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                service.data = Uri.parse(service.toUri(Intent.URI_INTENT_SCHEME))
                @Suppress("DEPRECATION")
                views.setRemoteAdapter(R.id.widget_grid, service)
                views.setEmptyView(R.id.widget_grid, R.id.widget_empty)

                val template = Intent(context, WidgetLaunchActivity::class.java)
                views.setPendingIntentTemplate(
                    R.id.widget_grid,
                    PendingIntent.getActivity(context, widgetId, template, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag),
                )
            }
            manager.updateAppWidget(widgetId, views)
        }

        fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, CategoryWidgetProvider::class.java))

        /** Opdatér alle bokse på startskærmen (efter flytning af apps, installationer m.m.). */
        fun refreshAll(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val ids = widgetIds(context)
                if (ids.isEmpty()) return
                ids.forEach { updateWidget(context, manager, it) }
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_grid)
            }
        }

        /** Beder den nuværende startskærm om at tilføje en boks for kategorien. */
        fun requestPin(context: Context, category: Category): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val callback = Intent(context, WidgetPinnedReceiver::class.java).putExtra(EXTRA_CATEGORY, category.name)
            val pending = PendingIntent.getBroadcast(
                context, category.ordinal, callback, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
            )
            return runCatching {
                manager.requestPinAppWidget(ComponentName(context, CategoryWidgetProvider::class.java), null, pending)
            }.getOrDefault(false)
        }

        const val EXTRA_CATEGORY = "dk.ryddig.launcher.CATEGORY"
    }
}

/** Kaldes af startskærmen, når brugeren har accepteret at tilføje en boks. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val category = intent.getStringExtra(CategoryWidgetProvider.EXTRA_CATEGORY)
            ?.let { runCatching { Category.valueOf(it) }.getOrNull() }
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || category == null) return
        WidgetStore.set(context, id, category)
        val manager = AppWidgetManager.getInstance(context)
        CategoryWidgetProvider.updateWidget(context, manager, id)
        manager.notifyAppWidgetViewDataChanged(intArrayOf(id), R.id.widget_grid)
    }
}

class CategoryWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(
        applicationContext,
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
    )

    private class Factory(private val context: Context, private val widgetId: Int) : RemoteViewsFactory {
        private var apps: List<AppEntry> = emptyList()

        override fun onCreate() {}
        override fun onDestroy() {}

        override fun onDataSetChanged() {
            val category = WidgetStore.get(context, widgetId)
            apps = if (category == null) {
                emptyList()
            } else {
                runCatching { runBlocking { AppRepository.loadApps(context, only = category, iconSizeDp = 44) } }
                    .getOrDefault(emptyList())
            }
        }

        override fun getCount(): Int = apps.size

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_app_item)
            val app = apps.getOrNull(position) ?: return views
            views.setImageViewBitmap(R.id.app_icon, app.bitmap)
            views.setTextViewText(R.id.app_label, app.label)
            views.setOnClickFillInIntent(
                R.id.app_item,
                Intent()
                    .putExtra(WidgetLaunchActivity.EXTRA_PACKAGE, app.packageName)
                    .putExtra(WidgetLaunchActivity.EXTRA_ACTIVITY, app.activityName),
            )
            return views
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount(): Int = 1
        override fun getItemId(position: Int): Long = apps.getOrNull(position)?.key?.hashCode()?.toLong() ?: position.toLong()
        override fun hasStableIds(): Boolean = true
    }
}

/** Usynlig mellemstation: åbner den app, der blev trykket på i en boks. */
class WidgetLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        val activity = intent.getStringExtra(EXTRA_ACTIVITY)
        if (pkg != null && activity != null && !AppRepository.launch(this, pkg, activity)) {
            Toast.makeText(this, "Appen findes ikke længere", Toast.LENGTH_SHORT).show()
            CategoryWidgetProvider.refreshAll(this)
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "dk.ryddig.launcher.PACKAGE"
        const val EXTRA_ACTIVITY = "dk.ryddig.launcher.ACTIVITY"
    }
}

/** Vises når en boks tilføjes via startskærmens egen widget-liste: vælg kategori. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            RyddigTheme {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Text(
                            "Hvilken kategori skal boksen vise?",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LazyColumn {
                            items(Category.entries.filter { it != Category.SKJULT }) { cat ->
                                Text(
                                    "${cat.emoji}  ${cat.title}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { choose(widgetId, cat) }
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun choose(widgetId: Int, category: Category) {
        WidgetStore.set(this, widgetId, category)
        val manager = AppWidgetManager.getInstance(this)
        CategoryWidgetProvider.updateWidget(this, manager, widgetId)
        manager.notifyAppWidgetViewDataChanged(intArrayOf(widgetId), R.id.widget_grid)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
