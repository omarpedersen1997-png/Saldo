package dk.ryddig.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    /** Tælles op når apps installeres/fjernes, så listen genindlæses. */
    private val packagesChanged = mutableIntStateOf(0)
    /** Tælles op hver gang skærmen vises igen (f.eks. efter at have givet tilladelse). */
    private val resumed = mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            packagesChanged.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        setContent {
            RyddigTheme {
                RyddigApp(
                    packagesChanged = packagesChanged.intValue,
                    resumed = resumed.intValue,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed.intValue++
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(packageReceiver) }
        super.onDestroy()
    }
}
