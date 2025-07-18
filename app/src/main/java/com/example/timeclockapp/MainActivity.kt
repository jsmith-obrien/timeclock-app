package com.example.timeclockapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/* ============================================================ */
/*  Activity bootstrap                                          */
/* ============================================================ */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (currentUser == null) {
                LoginScreen { user ->
                    currentUser = user
                    userPunchLogs.getOrPut(user.username) { mutableStateListOf() }
                }
            } else {
                TimeClockTabbedUI()
            }
        }
    }
}

/* ============================================================ */
/*  Shared app state                                            */
/* ============================================================ */
var currentUser by mutableStateOf<User?>(null)
val userPunchLogs = mutableStateMapOf<String, SnapshotStateList<Punch>>()

/* ============================================================ */
/*  Tab scaffold                                                */
/* ============================================================ */
@Composable
fun TimeClockTabbedUI() {
    val user = currentUser ?: return
    val punchLog = userPunchLogs.getOrPut(user.username) { mutableStateListOf() }

    var selectedTab by remember { mutableStateOf(0) }
    val titles = listOf("Punch", "Hours")

    Scaffold(
        topBar = {
            Column(Modifier.background(Color(0xFF512DA8)).fillMaxWidth()) {
                Spacer(Modifier.height(32.dp)) // status‑bar buffer
                TabRow(selectedTab, containerColor = Color(0xFF512DA8)) {
                    titles.forEachIndexed { i, t ->
                        Tab(selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(t, color = Color.White, fontWeight = FontWeight.SemiBold) })
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> TimeClockScreen(punchLog)
                1 -> HoursScreen(punchLog)
            }

            // logout text
            Text(
                "Logout", color = Color(0xFF8B0000), fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 385.dp)
                    .padding(end = 16.dp)
                    .clickable { currentUser = null }
            )
        }
    }
}


//  PUNCH TAB

@Composable
fun TimeClockScreen(punchLog: SnapshotStateList<Punch>) {
    val ctx   = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("prefs_${currentUser!!.username}", Context.MODE_PRIVATE) }

    /* last punch = list tail OR stored pref on cold start */
    val lastPunch by remember(punchLog) {
        derivedStateOf { punchLog.lastOrNull()?.label ?: prefs.getString("lastPunch", null) }
    }

    /* enable / disable matrix */
    val canClockIn  = lastPunch == null || lastPunch == "Clock Out"
    val canClockOut = lastPunch in listOf("Clock In", "End Lunch", "End Break")
    val onLunch     = lastPunch == "Start Lunch"
    val onBreak     = lastPunch == "Start Break"
    val canStartLB  = lastPunch in listOf("Clock In", "End Lunch", "End Break")

    fun punch(lbl: String) = punchLog.add(Punch(System.currentTimeMillis(), lbl))

    /* ticking clock */
    var now by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            now = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            delay(1_000)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEDE7F6), Color(0xFFF3E5F5))))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.rick), "avatar", Modifier.size(80.dp).clip(CircleShape))
        Spacer(Modifier.height(16.dp))
        Text("${currentUser?.preferredFirstName}, please select a function.",
            style = MaterialTheme.typography.titleMedium)
        Text(now, style = MaterialTheme.typography.headlineMedium)
        Divider(Modifier.padding(vertical = 8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PunchBtn("Clock In",  canClockIn)  { punch("Clock In") }
                PunchBtn("Clock Out", canClockOut) { punch("Clock Out") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    onLunch  -> PunchBtn("End Lunch",   true)  { punch("End Lunch") }
                    canStartLB -> PunchBtn("Start Lunch", true)  { punch("Start Lunch") }
                    else     -> PunchBtn("Start Lunch", false) {}
                }
                when {
                    onBreak  -> PunchBtn("End Break",   true)  { punch("End Break") }
                    canStartLB-> PunchBtn("Start Break", true) { punch("Start Break") }
                    else     -> PunchBtn("Start Break", false) {}
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Last punch: ${lastPunch ?: "None"}")
    }
}

@Composable
private fun PunchBtn(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(140.dp).height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF7E57C2) else Color.LightGray,
            contentColor   = if (enabled) Color.White       else Color.DarkGray
        )
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/* ============================================================ */
/*  HOURS TAB                                                   */
/* ============================================================ */
@Composable
fun HoursScreen(punchLog: SnapshotStateList<Punch>) {
    val ctx  = LocalContext.current
    val user = currentUser ?: return

    /* pay‑period window */
    val today       = remember { Date() }
    val startPeriod = remember { getStartOfPayPeriod(today) }
    val endPeriod   = remember {
        Calendar.getInstance().apply { time = startPeriod; add(Calendar.DAY_OF_MONTH, 13) }.time
    }

    /* punches, oldest→newest, within period */
    val punches by remember(punchLog) {
        derivedStateOf {
            punchLog.filter {
                Date(it.timestamp).let { d -> d.after(startPeriod) && d.before(endPeriod) }
            }.sortedBy { it.timestamp }
        }
    }

    /* detect invalid pairs */
    val badPunches = remember(punches) {
        val err = mutableSetOf<Punch>()
        var inSession = false; var lunchOpen = false; var breakOpen = false

        punches.forEachIndexed { i, p ->
            val next = punches.getOrNull(i + 1)
            when (p.label) {
                "Clock In" -> {
                    if (inSession) err += p
                    inSession = true; lunchOpen = false; breakOpen = false
                    if (next != null && next.label in listOf("End Break", "End Lunch")) { err += p; err += next }
                }
                "Clock Out" -> { if (!inSession) err += p else inSession = false }
                "Start Lunch" -> {
                    if (!inSession || lunchOpen) err += p else lunchOpen = true
                    if (next?.label != "End Lunch") { err += p; next?.let { err += it } }
                }
                "End Lunch"   -> if (!lunchOpen)  err += p else lunchOpen  = false
                "Start Break" -> {
                    if (!inSession || breakOpen) err += p else breakOpen = true
                    if (next?.label != "End Break") { err += p; next?.let { err += it } }
                }
                "End Break"   -> if (!breakOpen)  err += p else breakOpen = false
            }
        }
        punches.lastOrNull()?.let { err.remove(it) } // never flag newest
        err
    }

    val totalHours = remember(punches) { computeHours(punches) }
    var deleteMe by remember { mutableStateOf<Punch?>(null) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF3E5F5)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Summary for Pay Period", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF4A148C),
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("Total Hours: $totalHours", fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Divider(Modifier.fillMaxWidth(), 1.dp, Color.Gray)
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(punches) { p ->
                val timeStr = SimpleDateFormat("EEE hh:mm a", Locale.getDefault())
                    .format(Date(p.timestamp))
                Card(Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {

                        if (p in badPunches) {
                            Icon(Icons.Default.Warning, "invalid", tint = Color(0xFFD32F2F),
                                modifier = Modifier.padding(end = 8.dp))
                        }

                        Column(Modifier.weight(1f)) {
                            Text(p.label, fontWeight = FontWeight.Bold,
                                fontSize = 16.sp, color = Color(0xFF6A1B9A))
                            Spacer(Modifier.height(4.dp))
                            Text(timeStr, fontSize = 14.sp)
                        }

                        if (user.isAdmin) {
                            IconButton({ deleteMe = p }) {
                                Icon(Icons.Default.Close, "Delete", tint = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    /* confirm delete */
    if (deleteMe != null) {
        AlertDialog(
            onDismissRequest = { deleteMe = null },
            title = { Text("Delete this punch?") },
            text  = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    punchLog.remove(deleteMe)
                    StorageUtils.savePunches(ctx, user.username, punchLog)
                    // refresh last‑punch pref so Punch tab is correct
                    ctx.getSharedPreferences("prefs_${user.username}", Context.MODE_PRIVATE)
                        .edit().putString("lastPunch", punchLog.lastOrNull()?.label).apply()
                    deleteMe = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ deleteMe = null }) { Text("Cancel") } }
        )
    }
}

/* ============================================================ */
/*  LOGIN screen                                                */
/* ============================================================ */
@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit) {
    val ctx   = LocalContext.current
    val users = remember { StorageUtils.loadUsers(ctx) }

    var uname by remember { mutableStateOf("") }
    var pass  by remember { mutableStateOf("") }
    var err   by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text("☕ JSO Time and Attendance", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(uname, { uname = it }, label = { Text("Username") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") },
            singleLine = true, visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(16.dp))

        err?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }

        Button(onClick = {
            val user = users.find {
                it.username.equals(uname.trim(), true) && it.password == pass
            }
            if (user != null) {
                err = null
                onLoginSuccess(user)
            } else err = "Invalid username or password"
        }) { Text("Login") }
    }
}

/* ============================================================ */
/*  Utilities                                                   */
/* ============================================================ */

/* set up pay‑period anchor  (2025‑01‑05 00:00)  */
fun getStartOfPayPeriod(date: Date): Date {
    val cal = Calendar.getInstance().apply { time = date }
    val anchor = Calendar.getInstance().apply {
        set(2025, Calendar.JANUARY, 5, 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }
    while (cal.before(anchor)) cal.add(Calendar.WEEK_OF_YEAR, 2)
    val diff = ((cal.timeInMillis - anchor.timeInMillis) / 86_400_000L).toInt() % 14
    cal.add(Calendar.DAY_OF_YEAR, -diff)
    return cal.time
}

/* ms → hours string */
private fun computeHours(punches: List<Punch>): String {
    var total = 0L
    var clockIn: Long? = null
    var lunchStart: Long? = null; var lunchSum = 0L
    var breakStart: Long? = null; var breakSum = 0L

    punches.sortedBy { it.timestamp }.forEach {
        when (it.label) {
            "Clock In"   -> { clockIn = it.timestamp; lunchSum = 0; breakSum = 0 }
            "Clock Out"  -> clockIn?.let { ci ->
                total += it.timestamp - ci - lunchSum + breakSum; clockIn = null
            }
            "Start Lunch"-> lunchStart = it.timestamp
            "End Lunch"  -> lunchStart?.let { ls -> lunchSum += it.timestamp - ls; lunchStart = null }
            "Start Break"-> breakStart = it.timestamp
            "End Break"  -> breakStart?.let { bs -> breakSum += it.timestamp - bs; breakStart = null }
        }
    }
    return "%.2f".format(total / 3_600_000f)
}

/* ---------------- SHA‑256 one‑way hash ---------------- */
fun hashPassword(pw: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(pw.toByteArray())
        .joinToString("") { "%02x".format(it) }