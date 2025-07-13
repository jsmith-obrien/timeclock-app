package com.example.timeclockapp

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeClockTabbedUI()
        }
    }
}

data class Punch(val timestamp: Long, val label: String)

// Shared punch log across tabs
val punchLog = mutableStateListOf<Punch>()

@Composable
fun TimeClockTabbedUI() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Punch", "Hours")

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> TimeClockApp(punchLog)
            1 -> HoursTab(punchLog)
        }
    }
}

@Composable
fun TimeClockApp(punchLog: SnapshotStateList<Punch>) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var lastPunch by remember { mutableStateOf(prefs.getString("lastPunch", null)) }
    var currentTime by remember { mutableStateOf("") }

    // Live clock
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    fun punch(action: String) {
        val now = System.currentTimeMillis()
        punchLog.add(Punch(now, action))
        prefs.edit().putString("lastPunch", action).apply()
        lastPunch = action
    }

    // Determine button states
    val canClockIn = lastPunch == null || lastPunch == "Clock Out"
    val canClockOut = lastPunch in listOf("Clock In", "End Lunch", "End Break")
    val onLunch = lastPunch == "Start Lunch"
    val onBreak = lastPunch == "Start Break"
    val canStartLunchOrBreak = lastPunch in listOf("Clock In", "End Lunch", "End Break")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBBDEFB), // Medium light blue (top)
                        Color(0xFFE3F2FD)  // Even lighter blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.rick), // make sure rick.png is in res/drawable
                contentDescription = "Rick Sanchez",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Miles, please select a function.", style = MaterialTheme.typography.bodyLarge)
            Text(currentTime, style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeButton("Clock In", canClockIn) { punch("Clock In") }
                    TimeButton("Clock Out", canClockOut) { punch("Clock Out") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        onLunch -> TimeButton("End Lunch", true) { punch("End Lunch") }
                        canStartLunchOrBreak -> TimeButton("Start Lunch", true) { punch("Start Lunch") }
                        else -> TimeButton("Start Lunch", false) {}
                    }

                    when {
                        onBreak -> TimeButton("End Break", true) { punch("End Break") }
                        canStartLunchOrBreak -> TimeButton("Start Break", true) { punch("Start Break") }
                        else -> TimeButton("Start Break", false) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Last punch: ${lastPunch ?: "None"}")
        }
    }
}

@Composable
fun TimeButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(140.dp)
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (enabled) Color.White else Color.DarkGray
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun HoursTab(punchLog: List<Punch>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Punch History (Session Only):", style = MaterialTheme.typography.titleMedium)
        if (punchLog.isEmpty()) {
            Text("No punches recorded.")
        } else {
            punchLog
                .sortedByDescending { it.timestamp }
                .forEach {
                    val time = SimpleDateFormat("EEE hh:mm a", Locale.getDefault()).format(Date(it.timestamp))
                    Text("• $time – ${it.label}")
                }
        }
    }
}