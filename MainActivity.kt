package dk.pengeoverblik.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import java.time.LocalDate
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

private val Danish = Locale("da", "DK")

private data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val recurring: Boolean = false,
    val owner: String = "Mit budget",
    val payFrequency: String = "Månedligt"
)

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Danish).format(value)
private fun String.csvSafe(): String = "\"" + replace("\"", "\"\"") + "\""

private data class BillImportDraft(val title: String, val amount: Double, val dueDay: Int, val rawText: String)

private data class Bill(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dueDay: Int,
    val owner: String = "Fælles"
)

private fun daysUntilDue(dueDay: Int): Int {
    val today = LocalDate.now()
    val thisMonthDay = dueDay.coerceAtMost(today.lengthOfMonth())
    var due = today.withDayOfMonth(thisMonthDay)
    if (due.isBefore(today)) {
        val next = today.plusMonths(1)
        due = next.withDayOfMonth(dueDay.coerceAtMost(next.lengthOfMonth()))
    }
    return java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillReminderNotifications.createChannel(this)
        BillReminderNotifications.scheduleDailyCheck(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
        setContent { PengeOverblikApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PengeOverblikApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("pengeoverblik", Context.MODE_PRIVATE) }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var incomeMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var bills by remember { mutableStateOf(loadBills(prefs)) }
    var showAddBill by remember { mutableStateOf(false) }
    var billImportDraft by remember { mutableStateOf<BillImportDraft?>(null) }
    var isReadingScreenshot by remember { mutableStateOf(false) }
    var screenshotError by remember { mutableStateOf<String?>(null) }
    val screenshotPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isReadingScreenshot = true
            screenshotError = null
            try {
                val image = InputImage.fromFilePath(context, uri)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener { result ->
                        isReadingScreenshot = false
                        val extracted = result.text
                        val explicitAmounts = Regex("""(?i)([0-9][0-9. ]*(?:,[0-9]{2})?)\s*(?:kr\.?|dkk)""").findAll(extracted)
                            .mapNotNull { it.groupValues[1].replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull() }
                            .filter { it in 1.0..10000000.0 }.toList()
                        val amountMatch = explicitAmounts.maxOrNull() ?: Regex("""(?<![0-9])([0-9]{1,3}(?:\.[0-9]{3})*(?:,[0-9]{2})?)(?![0-9])""").findAll(extracted)
                            .mapNotNull { it.groupValues[1].replace(".", "").replace(",", ".").toDoubleOrNull() }
                            .filter { it in 1.0..10000000.0 && it < 1000000 }.maxOrNull()
                        val dayMatch = Regex("""(?i)(?:forfald(?:sdato)?|betalingsdato|betales senest|sidste betalingsdag)[^0-9]{0,20}([0-3]?\d)[./-]([01]?\d)(?:[./-]\d{2,4})?""").find(extracted)
                            ?: Regex("""\b([0-3]?\d)[./-]([01]?\d)(?:[./-]\d{2,4})?\b""").find(extracted)
                        val dueDay = dayMatch?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..31 } ?: 1
                        val lines = extracted.lines().map { it.trim() }.filter { it.length in 3..55 }
                        val title = lines.firstOrNull { line ->
                            !line.contains(Regex("""(?i)betalingsservice|oversigt|betalingsdato|forfald|beløb|konto|ocr|pbs|kr\.?|dkk|ændre betaling""")) &&
                            !line.any(Char::isDigit)
                        } ?: lines.firstOrNull { !it.any(Char::isDigit) } ?: "Regning fra Betalingsservice"
                        if (amountMatch != null) billImportDraft = BillImportDraft(title.take(50), amountMatch, dueDay, extracted)
                        else screenshotError = "Jeg kunne læse teksten, men fandt ikke et sikkert beløb. Prøv et skarpere skærmbillede, eller indtast regningen manuelt."
                    }
                    .addOnFailureListener { isReadingScreenshot = false; screenshotError = "Skærmbilledet kunne ikke læses. Prøv igen med et tydeligere billede." }
            } catch (_: Exception) { isReadingScreenshot = false; screenshotError = "Billedet kunne ikke åbnes. Vælg et skærmbillede fra telefonens galleri." }
        }
    }
    var cashBalanceText by remember { mutableStateOf(prefs.getString("cash_balance", "") ?: "") }
    var daysToPaydayText by remember { mutableStateOf(prefs.getString("days_to_payday", "14") ?: "14") }

    fun persist(next: List<Entry>) {
        entries = next
        prefs.edit().putString("entries", JSONArray().apply {
            next.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("title", e.title); put("amount", e.amount)
                put("income", e.isIncome); put("category", e.category); put("recurring", e.recurring)
                put("owner", e.owner); put("payFrequency", e.payFrequency)
            }) }
        }.toString()).apply()
    }

    fun persistBills(next: List<Bill>) {
        bills = next
        prefs.edit().putString("bills", JSONArray().apply {
            next.forEach { b -> put(JSONObject().apply {
                put("id", b.id); put("title", b.title); put("amount", b.amount)
                put("dueDay", b.dueDay); put("owner", b.owner)
            }) }
        }.toString()).apply()
    }

    val totalIncome = entries.filter { it.isIncome }.sumOf { it.amount }
    val totalExpenses = entries.filterNot { it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpenses
    val daysToPayday = (daysToPaydayText.toIntOrNull() ?: 14).coerceIn(1, 60)
    val upcomingBills = bills.filter { daysUntilDue(it.dueDay) <= daysToPayday }
    val upcomingBillTotal = upcomingBills.sumOf { it.amount }
    val cashBalance = cashBalanceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val availableUntilPayday = cashBalance - upcomingBillTotal

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Color(0xFF6D63F5), onPrimary = Color.White,
            background = Color(0xFF10111A), surface = Color(0xFF191B27),
            onSurface = Color(0xFFF6F7FB), onSurfaceVariant = Color(0xFFA7ABBD),
            secondary = Color(0xFFB9B4FF), secondaryContainer = Color(0xFF29283F),
            tertiary = Color(0xFF68DCC4), tertiaryContainer = Color(0xFF1E3938),
            outline = Color(0xFF454858), outlineVariant = Color(0xFF2A2D3A)
        ) else lightColorScheme(
            primary = Color(0xFF5B55E7), onPrimary = Color.White,
            background = Color(0xFFF6F7FB), surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF171A2B), onSurfaceVariant = Color(0xFF777D91),
            secondary = Color(0xFFB8B4FF), secondaryContainer = Color(0xFFEEEDFF),
            tertiary = Color(0xFF168F7C), tertiaryContainer = Color(0xFFE0F7F1),
            outline = Color(0xFFD8DCE7), outlineVariant = Color(0xFFEAECF2)
        )
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Partners Budget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Indstillinger", tint = MaterialTheme.colorScheme.onSurface) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Overblik") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ListAlt, null) }, label = { Text("Poster") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Regninger") })
                    NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Statistik") })
                    NavigationBarItem(selected = tab == 4, onClick = { tab = 4 }, icon = { Icon(Icons.Default.Savings, null) }, label = { Text("Mål") })
                }
            },
            floatingActionButton = {
                if (tab == 2) FloatingActionButton(onClick = { showAddBill = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "Tilføj regning")
                } else if (tab == 0 || tab == 1) FloatingActionButton(onClick = { incomeMode = false; showAdd = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "Tilføj post")
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(Modifier.padding(padding), totalIncome, totalExpenses, balance, entries,
                    cashBalance = cashBalance, daysToPayday = daysToPayday, upcomingBillTotal = upcomingBillTotal,
                    availableUntilPayday = availableUntilPayday,
                    onAddIncome = { incomeMode = true; showAdd = true },
                    onAddExpense = { incomeMode = false; showAdd = true },
                    onDelete = { id -> persist(entries.filterNot { it.id == id }) })
                1 -> EntriesScreen(Modifier.padding(padding), entries,
                    onDelete = { id -> persist(entries.filterNot { it.id == id }) },
                    onAdd = { isIncome -> incomeMode = isIncome; showAdd = true })
                2 -> BillsScreen(Modifier.padding(padding), bills, onDelete = { id -> persistBills(bills.filterNot { it.id == id }) }, onAdd = { showAddBill = true }, onImportScreenshot = { screenshotPicker.launch("image/*") }, isReadingScreenshot = isReadingScreenshot)
                3 -> StatisticsScreen(Modifier.padding(padding), entries)
                else -> GoalsScreen(Modifier.padding(padding), balance)
            }
        }

        if (showAdd) AddEntryDialog(
            initialIncome = incomeMode,
            onDismiss = { showAdd = false },
            onSave = { entry -> persist(entries + entry); showAdd = false }
        )
        if (showAddBill) AddBillDialog(onDismiss = { showAddBill = false }, onSave = { bill -> persistBills(bills + bill); showAddBill = false })
        billImportDraft?.let { draft ->
            AddBillDialog(
                initialTitle = draft.title,
                initialAmount = String.format(Locale.US, "%.2f", draft.amount),
                initialDueDay = draft.dueDay.toString(),
                importNote = "Vælg budget nedenfor: Mit budget, Partners budget eller Fælles. Kontrollér navn, beløb og forfaldsdag før du gemmer.",
                onDismiss = { billImportDraft = null },
                onSave = { bill -> persistBills(bills + bill); billImportDraft = null }
            )
        }
        screenshotError?.let { message ->
            AlertDialog(onDismissRequest = { screenshotError = null }, title = { Text("Kunne ikke aflæse regningen") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { screenshotError = null }) { Text("OK") } })
        }
        if (isReadingScreenshot) AlertDialog(onDismissRequest = {}, title = { Text("Læser skærmbilledet") }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { CircularProgressIndicator(modifier = Modifier.size(24.dp)); Text("Finder regningsnavn, beløb og forfaldsdag …") } }, confirmButton = {})
        if (showSettings) AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Indstillinger", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mørkt tema", fontWeight = FontWeight.SemiBold)
                            Text("Skift mellem lyst og mørkt udtryk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = darkMode, onCheckedChange = { darkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() })
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OutlinedTextField(value = cashBalanceText, onValueChange = { cashBalanceText = it; prefs.edit().putString("cash_balance", it).apply() }, label = { Text("Saldo på kontoen lige nu (kr.)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = daysToPaydayText, onValueChange = { daysToPaydayText = it.filter(Char::isDigit).take(2); prefs.edit().putString("days_to_payday", it.filter(Char::isDigit).take(2)).apply() }, label = { Text("Dage til næste løn") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Text("Saldo og dage til løn bruges til et estimat frem til næste løn. Regninger trækkes fra, hvis de forfalder inden da.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val csv = buildString {
                                appendLine("Navn;Beløb;Type;Kategori;Budget;Hyppighed")
                                entries.forEach { e -> appendLine("${e.title.csvSafe()};${e.amount};${if (e.isIncome) "Indtægt" else "Udgift"};${e.category.csvSafe()};${e.owner.csvSafe()};${e.payFrequency.csvSafe()}") }
                            }
                            val send = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "Partners Budget – budgetposter.csv"); putExtra(Intent.EXTRA_TEXT, csv) }
                            context.startActivity(Intent.createChooser(send, "Eksportér budgetposter"))
                        }, modifier = Modifier.weight(1f)) { Text("Eksportér CSV") }
                        OutlinedButton(onClick = {
                            val backup = JSONObject().apply {
                                put("version", "3.2.0")
                                put("entries", JSONArray().apply { entries.forEach { e -> put(JSONObject().apply { put("id",e.id); put("title",e.title); put("amount",e.amount); put("income",e.isIncome); put("category",e.category); put("recurring",e.recurring); put("owner",e.owner); put("payFrequency",e.payFrequency) }) } })
                                put("bills", JSONArray().apply { bills.forEach { b -> put(JSONObject().apply { put("id",b.id); put("title",b.title); put("amount",b.amount); put("dueDay",b.dueDay); put("owner",b.owner) }) } })
                            }.toString(2)
                            val send = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_SUBJECT, "Partners Budget – backup.json"); putExtra(Intent.EXTRA_TEXT, backup) }
                            context.startActivity(Intent.createChooser(send, "Sikkerhedskopiér data"))
                        }, modifier = Modifier.weight(1f)) { Text("Backup") }
                    }
                    Text("Partners Budget · Version 3.2.0\n\nDine budgetposter og regninger gemmes lokalt på telefonen. Appen forbinder ikke til din bank og sender ikke budgetdata til en server. Eksport sker kun, når du selv vælger at dele filens indhold.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Færdig") } }
        )
    }
}

private fun loadBills(prefs: android.content.SharedPreferences): List<Bill> {
    val raw = prefs.getString("bills", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Bill(o.optString("id", UUID.randomUUID().toString()), o.optString("title"), o.optDouble("amount"), o.optInt("dueDay", 1).coerceIn(1, 31), o.optString("owner", "Fælles"))
        }
    } catch (_: Exception) { emptyList() }
}

private fun loadEntries(prefs: android.content.SharedPreferences): List<Entry> {
    val raw = prefs.getString("entries", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Entry(o.optString("id", UUID.randomUUID().toString()), o.optString("title"), o.optDouble("amount"), o.optBoolean("income"), o.optString("category"), o.optBoolean("recurring"), o.optString("owner", "Mit budget"), o.optString("payFrequency", "Månedligt"))
        }
    } catch (_: Exception) { emptyList() }
}

@Composable
private fun Dashboard(
    modifier: Modifier,
    income: Double,
    expenses: Double,
    balance: Double,
    entries: List<Entry>,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onDelete: (String) -> Unit,
    cashBalance: Double,
    daysToPayday: Int,
    upcomingBillTotal: Double,
    availableUntilPayday: Double
) {
    val expenseShare = if (income > 0.0) (expenses / income).coerceIn(0.0, 1.0).toFloat() else 0f
    val mineIncome = entries.filter { it.isIncome && it.owner == "Mit budget" }.sumOf { it.amount }
    val mineExpenses = entries.filter { !it.isIncome && it.owner == "Mit budget" }.sumOf { it.amount }
    val partnerIncome = entries.filter { it.isIncome && it.owner == "Partners budget" }.sumOf { it.amount }
    val partnerExpenses = entries.filter { !it.isIncome && it.owner == "Partners budget" }.sumOf { it.amount }
    val sharedIncome = entries.filter { it.isIncome && it.owner == "Fælles" }.sumOf { it.amount }
    val sharedExpenses = entries.filter { !it.isIncome && it.owner == "Fælles" }.sumOf { it.amount }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("MANDAG · ØKONOMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = androidx.compose.ui.unit.TextUnit(1.1f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text("Dit overblik", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text("RÅDERUM", color = Color(0xFFE4E3FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Text(money(balance), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).height(5.dp).background(Color(0x55FFFFFF), RoundedCornerShape(8.dp))) {
                            Box(Modifier.fillMaxWidth(expenseShare).fillMaxHeight().background(Color(0xFF8BE2D0), RoundedCornerShape(8.dp)))
                        }
                        Text("${(expenseShare * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Indtægter", color = Color(0xFFDAD9FF), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(money(income), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Udgifter", color = Color(0xFFDAD9FF), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(money(expenses), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Frem til næste løn", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${daysToPayday} dage tilbage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(availableUntilPayday), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (availableUntilPayday < 0) Color(0xFFD9485F) else MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Kontosaldo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(cashBalance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Regninger før løn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(upcomingBillTotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Budgetter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("3 oversigter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    BudgetMiniRow("Mit budget", "Personligt", mineIncome, mineExpenses, MaterialTheme.colorScheme.secondaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BudgetMiniRow("Partners budget", "Personligt", partnerIncome, partnerExpenses, MaterialTheme.colorScheme.tertiaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BudgetMiniRow("Fælles", "Jeres fælles økonomi", sharedIncome, sharedExpenses, Color(0xFFFFE9F0))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seneste poster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                TextButton(onClick = onAddExpense) { Text("+ Post") }
            }
            if (entries.isEmpty()) EmptyState("Ingen poster endnu", "Tilføj en indtægt eller udgift for at komme i gang.")
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.takeLast(3).reversed().forEach { entry -> EntryRow(entry, onDelete) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddIncome, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Indtægt", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onAddExpense, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Udgift", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BudgetMiniRow(title: String, subtitle: String, income: Double, expenses: Double, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).background(tint, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tilbage: ${money(income - expenses)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable
private fun MetricCard(title: String, amount: Double, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(9.dp))
            Text(money(amount), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntriesScreen(modifier: Modifier, entries: List<Entry>, onDelete: (String) -> Unit, onAdd: (Boolean) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Dine budgetposter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Beløb vises som månedlige beløb. Du kan slette poster med papirkurven.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAdd(true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("+ Indtægt") }
            OutlinedButton(onClick = { onAdd(false) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("+ Udgift") }
        }
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) EmptyState("Ingen budgetposter", "Tilføj en indtægt eller udgift for at komme i gang.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(entries, key = { it.id }) { EntryRow(it, onDelete) } }
    }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: (String) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(if (entry.isIncome) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (entry.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (entry.isIncome) MaterialTheme.colorScheme.primary else Color(0xFFB64C32))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(listOf(entry.category, entry.owner, if (entry.recurring) "Fast post" else "").filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                if (entry.payFrequency != "Månedligt") Text("Indtastet: ${entry.payFrequency}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (entry.isIncome) "+ " else "− ") + money(entry.amount), fontWeight = FontWeight.Bold, color = if (entry.isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { onDelete(entry.id) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Slet ${entry.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun BillsScreen(modifier: Modifier, bills: List<Bill>, onDelete: (String) -> Unit, onAdd: () -> Unit, onImportScreenshot: () -> Unit, isReadingScreenshot: Boolean) {
    val sorted = bills.sortedBy { daysUntilDue(it.dueDay) }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Regningskalender", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Få overblik over faste regninger og deres forfaldsdag.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.padding(18.dp)) {
                Text("KOMMENDE REGNINGER", color = Color(0xFFE4EDE4), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(money(sorted.sumOf { it.amount }), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("Samlet beløb for de registrerede månedlige regninger", color = Color(0xFFD0DED2), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onImportScreenshot, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), enabled = !isReadingScreenshot) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Importér fra Betalingsservice-skærmbillede") }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tilføj manuelt") }
        Spacer(Modifier.height(8.dp))
        if (sorted.isEmpty()) EmptyState("Ingen regninger endnu", "Tilføj fx husleje, internet, forsikring eller andre faste regninger. De medregnes i estimatet frem til næste løn.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(sorted, key = { it.id }) { bill ->
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text(bill.dueDay.toString(), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(bill.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Forfalder dag ${bill.dueDay} · ${bill.owner}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(when (daysUntilDue(bill.dueDay)) { 0 -> "Forfalder i dag"; 1 -> "Forfalder i morgen"; else -> "Om ${daysUntilDue(bill.dueDay)} dage" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(bill.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { onDelete(bill.id) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Slet ${bill.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(initialTitle: String = "", initialAmount: String = "", initialDueDay: String = "1", importNote: String? = null, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    var title by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var dueDay by remember { mutableStateOf(initialDueDay) }
    var owner by remember { mutableStateOf("Fælles") }
    var ownerExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tilføj fast regning", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (importNote != null) Text(importNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Navn, fx husleje") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } }, label = { Text("Beløb pr. måned (kr.)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = dueDay, onValueChange = { dueDay = it.filter(Char::isDigit).take(2) }, label = { Text("Forfaldsdag (1–31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                ExposedDropdownMenuBox(expanded = ownerExpanded, onExpandedChange = { ownerExpanded = !ownerExpanded }) {
                    OutlinedTextField(value = owner, onValueChange = {}, readOnly = true, label = { Text("Budget") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ownerExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = ownerExpanded, onDismissRequest = { ownerExpanded = false }) {
                        listOf("Mit budget", "Partners budget", "Fælles").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { owner = option; ownerExpanded = false }) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { val parsed = amount.toDoubleOrNull(); val day = dueDay.toIntOrNull(); if (title.isNotBlank() && parsed != null && parsed > 0 && day != null && day in 1..31) onSave(Bill(title = title.trim(), amount = parsed, dueDay = day, owner = owner)) }, enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && (dueDay.toIntOrNull() ?: 0) in 1..31) { Text("Gem regning") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuller") } }
    )
}

@Composable
private fun StatisticsScreen(modifier: Modifier, entries: List<Entry>) {
    val income = entries.filter { it.isIncome }.sumOf { it.amount }
    val expenses = entries.filterNot { it.isIncome }.sumOf { it.amount }
    val net = income - expenses
    val categories = entries.filterNot { it.isIncome }.groupBy { it.category.ifBlank { "Andet" } }
        .mapValues { (_, list) -> list.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Statistik", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Et enkelt overblik over dine registrerede tal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Indtægter", income, Color(0xFFE2F7EF), Modifier.weight(1f))
                MetricCard("Udgifter", expenses, Color(0xFFFFE9EC), Modifier.weight(1f))
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Månedligt resultat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(money(net), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = if (net < 0) Color(0xFFD9485F) else MaterialTheme.colorScheme.primary)
                    val ratio = if (income > 0) (expenses / income).coerceIn(0.0, 1.0).toFloat() else 0f
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (ratio > .85f) Color(0xFFE46D78) else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text("${(ratio * 100).toInt()} % af indtægterne er registreret som udgifter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Text("Udgifter fordelt på kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (categories.isEmpty()) item { EmptyState("Ingen udgifter endnu", "Når du tilføjer udgifter, vises fordelingen her.") }
        items(categories) { (category, amount) ->
            val share = if (expenses > 0) (amount / expenses).toFloat().coerceIn(0f, 1f) else 0f
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(money(amount), fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Text("${(share * 100).toInt()} % af udgifterne", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GoalsScreen(modifier: Modifier, balance: Double) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Opsparing og mål", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp)) {
                Box(Modifier.size(48.dp).background(Color(0xFFE2F4E9), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(14.dp))
                Text("Muligt månedligt råderum", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(money(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Dette er forskellen mellem dine registrerede indtægter og udgifter – ikke en garanti for, at beløbet kan spares op.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        EmptyState("Opsparingsmål kommer snart", "I en senere version kan du oprette mål, følge fremskridt og registrere gældsafbetaling.")
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(initialIncome: Boolean, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(initialIncome) }
    var category by remember { mutableStateOf(if (initialIncome) "Indkomst" else "Andet") }
    var recurring by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf("Månedligt") }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var owner by remember { mutableStateOf("Mit budget") }
    var ownerExpanded by remember { mutableStateOf(false) }
    val categories = if (isIncome) listOf("Indkomst", "Løn", "Børnepenge", "Andet") else listOf("Bolig", "Mad", "Transport", "Forsikring", "Regninger", "Gæld", "Opsparing", "Andet")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tilføj budgetpost", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isIncome, onClick = { isIncome = true; category = "Indkomst" }, label = { Text("Indtægt") })
                    FilterChip(selected = !isIncome, onClick = { isIncome = false; category = "Andet" }, label = { Text("Udgift") })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Navn, fx husleje") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } }, label = { Text(if (isIncome && frequency == "Hver 14. dag") "Løn i kr. hver 14. dag" else if (isIncome && frequency == "Ugentligt") "Indtægt i kr. pr. uge" else "Beløb i kr. pr. måned") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (isIncome) {
                    ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = { frequencyExpanded = !frequencyExpanded }) {
                        OutlinedTextField(value = frequency, onValueChange = {}, readOnly = true, label = { Text("Udbetaling") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { frequencyExpanded = false }) {
                            listOf("Månedligt", "Hver 14. dag", "Ugentligt").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { frequency = option; frequencyExpanded = false }) }
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = ownerExpanded, onExpandedChange = { ownerExpanded = !ownerExpanded }) {
                    OutlinedTextField(value = owner, onValueChange = {}, readOnly = true, label = { Text("Hvem tilhører posten?") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ownerExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = ownerExpanded, onDismissRequest = { ownerExpanded = false }) {
                        listOf("Mit budget", "Partners budget", "Fælles").forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { owner = option; ownerExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = recurring, onCheckedChange = { recurring = it }); Text("Fast månedlig post") }
            }
        },
        confirmButton = { Button(onClick = { val parsed = amount.toDoubleOrNull(); if (title.isNotBlank() && parsed != null && parsed > 0) onSave(Entry(title = title.trim(), amount = parsed * when (if (isIncome) frequency else "Månedligt") { "Hver 14. dag" -> 26.0 / 12.0; "Ugentligt" -> 52.0 / 12.0; else -> 1.0 }, isIncome = isIncome, category = category, recurring = recurring, owner = owner, payFrequency = if (isIncome) frequency else "Månedligt")) }, enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0) { Text("Gem") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuller") } }
    )
}
