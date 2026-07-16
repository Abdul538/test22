package id.myapp.progresshubkt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Full screen: hide the status bar (and nav bar) entirely. A swipe
        // from the edge reveals them temporarily (transient), rather than
        // requiring the user to dig into a menu to get them back.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val state = AppState(applicationContext)
        state.load()
        setContent {
            val particles = remember { generateParticles(46) }
            val time = rememberParticleTime()
            var rootSizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

            MaterialTheme(colorScheme = AppDarkColorScheme) {
                CompositionLocalProvider(
                    LocalParticleField provides ParticleFieldState(particles, time, rootSizePx)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgDark)
                            .onGloballyPositioned { coords ->
                                rootSizePx = androidx.compose.ui.geometry.Size(
                                    coords.size.width.toFloat(),
                                    coords.size.height.toFloat()
                                )
                            }
                    ) {
                        ParticleBackground(modifier = Modifier.fillMaxSize(), particles = particles, time = time)
                        AppRoot(state)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRoot(state: AppState) {
    if (!state.hasCompletedSetup) {
        NewProgramScreen(state)
        return
    }
    var tab by remember { mutableIntStateOf(0) }
    val phase = remember(state.week, state.settings) { phaseFor(state.week, state.settings.totalWeeks) }
    val (streak, _) = state.computeStreak()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Color(0xCC0B0F15)) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.CheckCircle, null) }, label = { Text("Hari Ini") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.DateRange, null) }, label = { Text("Progres") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Pengaturan") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AppHeader(
                phaseName = phase.name,
                phaseColor = Color(phase.colorHex),
                week = state.week,
                totalWeeks = state.settings.totalWeeks,
                streak = streak,
                currentWeight = state.currentWeight,
                startWeight = state.settings.startWeight,
                goalWeight = state.settings.goalWeight
            )
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> TodayScreen(state)
                    1 -> ProgressScreen(state)
                    else -> SettingsScreen(state)
                }
            }
        }
    }
}

@Composable
fun TodayScreen(state: AppState) {
    val days = remember(state.week, state.settings) { weekPlan(state.week, state.settings) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { state.goToWeek(state.week - 1) }, enabled = state.week > 1) { Text("‹", color = Color.White) }
            Text("Minggu ${state.week}", fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { state.goToWeek(state.week + 1) }, enabled = state.week < state.settings.totalWeeks) { Text("›", color = Color.White) }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(days) { day -> DayRow(state, day) }
        }
    }
}

@Composable
fun DayRow(state: AppState, day: DayPlan) {
    val key = "${state.week}-${day.key}"
    val done = state.completed[key] == true
    var kmText by remember(key, state.actualKm[key]) {
        mutableStateOf((state.actualKm[key] ?: day.km).let { if (it == it.roundToInt().toDouble()) it.roundToInt().toString() else it.toString() })
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (done) AccentTeal.copy(alpha = 0.5f) else GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${day.label} · ${formatDateId(day.date)}", fontWeight = FontWeight.Medium, color = Color.White)
                if (day.rest) {
                    Text("Istirahat", color = TextDim, style = MaterialTheme.typography.bodySmall)
                } else {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = kmText,
                        onValueChange = {
                            kmText = it
                            it.toDoubleOrNull()?.let { v -> state.setActualKm(state.week, day.key, v) }
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        label = { Text("km") }
                    )
                }
            }
            if (!day.rest) {
                Checkbox(checked = done, onCheckedChange = {
                    state.toggleDayDone(state.week, day.key, day.km)
                }, colors = CheckboxDefaults.colors(checkedColor = AccentTeal))
            }
        }
    }
}

@Composable
fun ProgressScreen(state: AppState) {
    val (streak, best) = state.computeStreak()
    val (done, total) = state.totalSessionsAll
    var newWeight by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ProgressRing(
                        fraction = state.progressFraction.toFloat(),
                        color = AccentTeal,
                        centerText = "${(state.progressFraction * 100).roundToInt()}%",
                        centerLabel = "MENUJU TARGET"
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Berat sekarang: ${"%.1f".format(state.currentWeight)} kg", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Target: ${state.settings.startWeight.toInt()} kg → ${state.settings.goalWeight.toInt()} kg", color = TextDim)
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Statistik", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Sesi selesai: $done / $total", color = Color.White)
                Text("Total jarak: ${"%.1f".format(state.totalKmAll)} km", color = Color.White)
                Text("Total kalori: ${state.totalCaloriesAll} kkal", color = Color.White)
                Text("Streak saat ini: $streak hari (terbaik: $best)", color = Color.White)
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Catat berat badan", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newWeight, onValueChange = { newWeight = it }, label = { Text("kg") }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        newWeight.toDoubleOrNull()?.let { state.logWeight(it); newWeight = "" }
                    }) { Text("Simpan") }
                }
            }
        }
        item {
            Text("Riwayat berat", fontWeight = FontWeight.Bold, color = Color.White)
        }
        items(state.weightHistorySorted.reversed()) { (offset, w) ->
            val date = state.settings.startDate + offset
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDateId(date), color = TextDim)
                    Text("${"%.1f".format(w)} kg", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(state: AppState) {
    var startWeight by remember { mutableStateOf(state.settings.startWeight.toString()) }
    var goalWeight by remember { mutableStateOf(state.settings.goalWeight.toString()) }
    var totalWeeks by remember { mutableStateOf(state.settings.totalWeeks.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pengaturan Program", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = startWeight, onValueChange = { startWeight = it }, label = { Text("Berat awal (kg)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = goalWeight, onValueChange = { goalWeight = it }, label = { Text("Berat target (kg)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = totalWeeks, onValueChange = { totalWeeks = it }, label = { Text("Jumlah minggu") }, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = {
            state.updateSettings(
                state.settings.copy(
                    startWeight = startWeight.toDoubleOrNull() ?: state.settings.startWeight,
                    goalWeight = goalWeight.toDoubleOrNull() ?: state.settings.goalWeight,
                    totalWeeks = totalWeeks.toIntOrNull() ?: state.settings.totalWeeks
                )
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Pengaturan")
        }
    }
}
