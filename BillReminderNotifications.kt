package dk.pengeoverblik.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

internal object BillReminderNotifications {
    const val CHANNEL_ID = "bill_reminders"
    private const val ALARM_REQUEST = 8214

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Regningspåmindelser", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Påmindelser om registrerede regninger i PengeOverblik"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BillReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, ALARM_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        }
    }

    fun notifyDueBills(context: Context) {
        val prefs = context.getSharedPreferences("pengeoverblik", Context.MODE_PRIVATE)
        val raw = prefs.getString("bills", null) ?: return
        val bills = runCatching { JSONArray(raw) }.getOrNull() ?: return
        val today = LocalDate.now()
        val dueSoon = mutableListOf<String>()
        var total = 0.0
        for (i in 0 until bills.length()) {
            val bill = bills.optJSONObject(i) ?: continue
            val dueDay = bill.optInt("dueDay", 1).coerceIn(1, 31)
            val dueThisMonth = today.withDayOfMonth(dueDay.coerceAtMost(today.lengthOfMonth()))
            val due = if (dueThisMonth.isBefore(today)) {
                val next = today.plusMonths(1)
                next.withDayOfMonth(dueDay.coerceAtMost(next.lengthOfMonth()))
            } else dueThisMonth
            val days = ChronoUnit.DAYS.between(today, due).toInt()
            if (days in 0..1) {
                dueSoon.add(bill.optString("title", "Regning"))
                total += bill.optDouble("amount", 0.0)
            }
        }
        if (dueSoon.isNotEmpty()) {
            val title = if (dueSoon.size == 1) "Regning forfalder i dag eller i morgen" else "Regninger forfalder snart"
            val amount = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("da", "DK")).format(total)
            val body = dueSoon.take(3).joinToString(", ") + (if (dueSoon.size > 3) " og ${dueSoon.size - 3} flere" else "") + " · I alt $amount"
            val openApp = PendingIntent.getActivity(context, 8215, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText("$body · I alt $amount")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$body. Samlet beløb: $amount."))
                .setContentIntent(openApp).setAutoCancel(true).build()
            if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(8216, notification)
            }
        }
    }
}

class BillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        BillReminderNotifications.createChannel(context)
        BillReminderNotifications.notifyDueBills(context)
        BillReminderNotifications.scheduleDailyCheck(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) BillReminderNotifications.scheduleDailyCheck(context)
    }
}
