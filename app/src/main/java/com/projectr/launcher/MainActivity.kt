package com.projectr.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF080B12)
private val Surface1 = Color(0xFF111827)
private val Surface2 = Color(0xFF182235)
private val Surface3 = Color(0xFF202D42)
private val Cyan = Color(0xFF65E6F2)
private val Violet = Color(0xFF9B8CFF)
private val TextMain = Color(0xFFF4F7FB)
private val TextMuted = Color(0xFF9AA9BC)
private val Green = Color(0xFF8BE28B)
private val Orange = Color(0xFFFFB86B)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("project_r", MODE_PRIVATE) }
    private var onFolderPicked: ((Uri, String) -> Unit)? = null
    private var onExecutablePicked: ((Uri) -> Unit)? = null

    private val executablePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            onExecutablePicked?.invoke(uri)
        }
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            val folderName = uri.lastPathSegment?.substringAfterLast(':')
                ?.ifBlank { "Imported game folder" } ?: "Imported game folder"
            onFolderPicked?.invoke(uri, folderName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var games by remember { mutableStateOf(loadGames()) }
            var tab by remember { mutableStateOf(AppTab.HOME) }
            var selected by remember { mutableStateOf<GameEntry?>(null) }
            var toastMessage by remember { mutableStateOf<String?>(null) }

            fun persist(updated: List<GameEntry>) {
                games = updated
                val array = JSONArray()
                updated.forEach {
                    array.put(JSONObject().put("name", it.name).put("uri", it.uri).put("added", it.added).put("exeUri", it.exeUri ?: ""))
                }
                prefs.edit().putString("games", array.toString()).apply()
            }

            onFolderPicked = { uri, name ->
                if (games.none { it.uri == uri.toString() }) {
                    persist(games + GameEntry(name, uri.toString(), "Added ${dateNow()}"))
                    toastMessage = "$name added to your library"
                } else {
                    toastMessage = "This folder is already in your library"
                }
                tab = AppTab.LIBRARY
            }

            onExecutablePicked = { uri ->
                val current = selected
                if (current != null) {
                    val updated = games.map { if (it.uri == current.uri) it.copy(exeUri = uri.toString()) else it }
                    persist(updated)
                    selected = updated.firstOrNull { it.uri == current.uri }
                    toastMessage = "Windows executable linked to ${current.name}"
                }
            }

            ProjectRTheme {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    when {
                        selected != null -> GameDetailsScreen(
                            game = selected!!,
                            onBack = { selected = null },
                            onConfigure = { selected = null; tab = AppTab.SETTINGS },
                            onRemove = {
                                persist(games.filterNot { it.uri == selected!!.uri })
                                selected = null
                                toastMessage = "Game removed from library"
                            },
                            onChooseExecutable = { executablePicker.launch(arrayOf("*/*")) },
                            onOpenRuntime = { toastMessage = RuntimeBridge.openOrInstall(this@MainActivity) },
                            runtimeInstalled = RuntimeBridge.isInstalled(this@MainActivity)
                        )
                        else -> Scaffold(
                            containerColor = Ink,
                            bottomBar = { ProjectBottomBar(tab) { tab = it } }
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(padding)) {
                                when (tab) {
                                    AppTab.HOME -> HomeScreen(
                                        games = games,
                                        onImport = { folderPicker.launch(null) },
                                        onLibrary = { tab = AppTab.LIBRARY },
                                        onGame = { selected = it },
                                        onEngine = { tab = AppTab.ENGINE }
                                    )
                                    AppTab.LIBRARY -> LibraryScreen(
                                        games = games,
                                        onImport = { folderPicker.launch(null) },
                                        onGame = { selected = it }
                                    )
                                    AppTab.ENGINE -> EngineScreen(
                                        runtimeInstalled = RuntimeBridge.isInstalled(this@MainActivity),
                                        onOpenRuntime = {
                                            toastMessage = RuntimeBridge.openOrInstall(this@MainActivity)
                                        }
                                    )
                                    AppTab.SETTINGS -> SettingsScreen()
                                }
                                AnimatedVisibility(
                                    visible = toastMessage != null,
                                    enter = fadeIn(), exit = fadeOut(),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                                ) {
                                    Surface(
                                        color = Surface3,
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(9.dp))
                                            Text(toastMessage.orEmpty(), color = TextMain, fontSize = 12.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Text("OK", color = Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { toastMessage = null })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadGames(): List<GameEntry> {
        val raw = prefs.getString("games", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                GameEntry(
                    obj.optString("name", "Imported game"),
                    obj.optString("uri", ""),
                    obj.optString("added", "Imported"),
                    obj.optString("exeUri").takeIf { it.isNotBlank() }
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}

private fun dateNow(): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

data class GameEntry(val name: String, val uri: String, val added: String, val exeUri: String? = null)
enum class AppTab { HOME, LIBRARY, ENGINE, SETTINGS }

@Composable
private fun ProjectRTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            onPrimary = Ink,
            secondary = Violet,
            background = Ink,
            surface = Surface1,
            onSurface = TextMain,
            onBackground = TextMain
        ),
        content = content
    )
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Cyan, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Text("R", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
            Text("PROJECT R", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Text("PC GAMING • ANDROID", color = TextMuted, fontSize = 8.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopHeader(title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BrandMark()
            if (subtitle != null) Text(subtitle.uppercase(), color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(Surface2)
                .border(1.dp, Color.White.copy(alpha = .07f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = Cyan, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun HomeScreen(
    games: List<GameEntry>,
    onImport: () -> Unit,
    onLibrary: () -> Unit,
    onGame: (GameEntry) -> Unit,
    onEngine: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { TopHeader("Home", "Your PC collection, reimagined") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("YOUR NEXT\nPC GAMING SESSION.", color = TextMain, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, letterSpacing = (-.8).sp)
                Text("A home for the Windows games you grew up with.", color = TextMuted, fontSize = 12.sp)
            }
        }
        item {
            FeaturedPanel(
                title = if (games.isEmpty()) "BUILD YOUR\nGAME LIBRARY" else games.first().name.uppercase(),
                eyebrow = if (games.isEmpty()) "WELCOME TO PROJECT R" else "YOUR COLLECTION",
                detail = if (games.isEmpty()) "Import a Windows game folder to get started." else "Your recently added game is ready to configure.",
                buttonText = if (games.isEmpty()) "IMPORT YOUR FIRST GAME" else "OPEN GAME DETAILS",
                onClick = { if (games.isEmpty()) onImport() else onGame(games.first()) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("LIBRARY", games.size.toString(), Icons.Default.SportsEsports, Modifier.weight(1f))
                MetricCard("ENGINE", "SETUP PENDING", Icons.Default.Memory, Modifier.weight(1f), Orange)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeading("RECENTLY ADDED", "Your collection")
                Text("SEE ALL", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable { onLibrary() })
            }
        }
        if (games.isEmpty()) {
            item { EmptyLibraryCard(onImport) }
        } else {
            items(games.take(4)) { game -> GameTile(game, onClick = { onGame(game) }) }
        }
        item {
            EngineTeaser(onClick = onEngine)
        }
    }
}

@Composable
private fun FeaturedPanel(
    title: String,
    eyebrow: String,
    detail: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(244.dp).clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF253B59), Color(0xFF26274D), Color(0xFF111827))))
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .92f)))))
        Box(Modifier.align(Alignment.TopEnd).padding(18.dp).size(112.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Cyan.copy(alpha = .30f), Violet.copy(alpha = .06f), Color.Transparent))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(eyebrow, color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(title, color = TextMain, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(detail, color = TextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(if (buttonText.contains("IMPORT")) Icons.Default.Add else Icons.Default.ArrowForward, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
            }
        }
        Text("R", modifier = Modifier.align(Alignment.TopEnd).padding(end = 25.dp, top = 20.dp), color = Color.White.copy(alpha = .09f), fontSize = 100.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, tint: Color = Cyan) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(Surface1).border(1.dp, Color.White.copy(alpha = .05f), RoundedCornerShape(17.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        Text(value, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        if (subtitle != null) Text(subtitle, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun GameTile(game: GameEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Surface1)
            .border(1.dp, Color.White.copy(alpha = .05f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(66.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF244B65), Color(0xFF38315E)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsEsports, null, tint = Cyan, modifier = Modifier.size(29.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(game.name, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(game.added, color = TextMuted, fontSize = 10.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Orange))
                Spacer(Modifier.width(5.dp))
                Text("IMPORTED • ENGINE PENDING", color = Orange, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .5.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted)
    }
}

@Composable
private fun EmptyLibraryCard(onImport: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1)
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(18.dp)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.FolderOpen, null, tint = Cyan, modifier = Modifier.size(32.dp))
        Text("YOUR LIBRARY IS WAITING", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Add a folder containing your own Windows game files.", color = TextMuted, fontSize = 10.sp)
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text("IMPORT GAME", fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EngineTeaser(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF18263B), Color(0xFF211F3D))))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Memory, null, tint = Violet, modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("COMPATIBILITY ENGINE", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Bundled runtime • first-run setup still required.", color = TextMuted, fontSize = 10.sp)
        }
        Icon(Icons.Default.ArrowForward, null, tint = Cyan)
    }
}

@Composable
private fun LibraryScreen(games: List<GameEntry>, onImport: () -> Unit, onGame: (GameEntry) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 20.dp)) {
        TopHeader("Library", "Your imported Windows games")
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MY COLLECTION", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${games.size} game folder${if (games.size == 1) "" else "s"} imported", color = TextMuted, fontSize = 11.sp)
            }
            Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink), shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Text(" ADD", fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (games.isEmpty()) {
            EmptyLibraryCard(onImport)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(games) { GameTile(it) { onGame(it) } }
            }
        }
    }
}

@Composable
private fun GameDetailsScreen(
    game: GameEntry,
    onBack: () -> Unit,
    onConfigure: () -> Unit,
    onRemove: () -> Unit,
    onChooseExecutable: () -> Unit,
    onOpenRuntime: () -> Unit,
    runtimeInstalled: Boolean
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextMain) }
            Text("GAME OVERVIEW", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        }
        FeaturedPanel(
            title = game.name.uppercase(),
            eyebrow = "YOUR GAME",
            detail = "Imported folder • Windows runtime not connected",
            buttonText = "CONFIGURE GAME",
            onClick = onConfigure
        )
        Text(game.name, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Black)
        StatusLine("LIBRARY STATUS", "Folder imported", Green)
        StatusLine("RUNTIME STATUS", if (runtimeInstalled) "Winlator detected (external runtime)" else "Winlator not detected", if (runtimeInstalled) Green else Orange)
        StatusLine("EXECUTABLE", if (game.exeUri.isNullOrBlank()) "Not linked" else "Windows file linked", if (game.exeUri.isNullOrBlank()) Orange else Green)
        StatusLine("ADDED", game.added, TextMuted)
        InfoCard("GAME FOLDER URI", game.uri)
        InfoCard("WINDOWS EXECUTABLE", game.exeUri ?: "No .exe selected yet")
        OutlinedButton(onClick = onChooseExecutable, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(8.dp)); Text(if (game.exeUri == null) "SELECT GAME EXE" else "CHANGE GAME EXE")
        }
        Button(
            onClick = onOpenRuntime, modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(if (runtimeInstalled) Icons.Default.OpenInNew else Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text(if (runtimeInstalled) "OPEN WINLATOR" else "GET WINLATOR", fontWeight = FontWeight.Black)
        }
        OutlinedButton(onClick = onConfigure, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Tune, null); Spacer(Modifier.width(8.dp)); Text("GAME SETTINGS")
        }
        OutlinedButton(onClick = onRemove, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8C8C))) {
            Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(7.dp)); Text("REMOVE FROM LIBRARY")
        }
        Text(
            "PROJECT R opens the bundled Winlator runtime. The saved executable is not yet passed into a Winlator container or launched automatically; configure the game from the runtime UI for now.",
            color = TextMuted, fontSize = 10.sp
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String, tint: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Surface1).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Surface1).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(value, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun EngineScreen(runtimeInstalled: Boolean, onOpenRuntime: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TopHeader("Engine", "Windows compatibility runtime")
        Spacer(Modifier.height(5.dp))
        Text("RUNTIME CENTER", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text("The pinned Winlator host source, native libraries, and runtime assets are included; the GitHub APK build now passes. First-run root filesystem setup and device testing are pending, and PROJECT R does not yet auto-create a container or launch its saved EXE directly.", color = TextMuted, fontSize = 11.sp)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Surface1).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("WINLATOR RUNTIME", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (runtimeInstalled) "Bundled • first-run setup required" else "Runtime unavailable", color = if (runtimeInstalled) Orange else Orange, fontSize = 10.sp)
            }
            Button(onClick = onOpenRuntime, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)) {
                Text(if (runtimeInstalled) "OPEN RUNTIME" else "UNAVAILABLE", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF263A58), Color(0xFF211F3E)))).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Memory, null, tint = Cyan, modifier = Modifier.size(30.dp))
            Text("WINLATOR-BASED RUNTIME", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Orange))
                Spacer(Modifier.width(7.dp))
                Text("SOURCE BUNDLED • DEVICE TEST PENDING", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .4.sp)
            }
            Text("Wine • Box86/Box64 • graphics • PRoot • audio • touch controls • gamepad", color = TextMuted, fontSize = 10.sp)
        }
        EngineComponent("Wine", "Windows API compatibility layer", false)
        EngineComponent("Box86 / Box64", "x86 and x64 instruction translation", false)
        EngineComponent("Mesa graphics", "OpenGL / Vulkan translation for device GPU", false)
        EngineComponent("Container & root filesystem", "Isolated Windows-compatible environment", false)
        EngineComponent("Input & audio", "Mouse, keyboard, gamepad and sound routing", false)
        Text("Device target", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        InfoCard("ANDROID DEVICE", "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown ABI"}")
        InfoCard("GRAPHICS NOTE", "This project must detect the actual GPU and Vulkan/OpenGL capabilities at runtime. Do not assume Adreno drivers on Mali devices.")
        Text("Pinned Winlator source revision 2c33741d50c5daab0521a098be5c969f63b9e887 is bundled and the CI APK build passed. Redmi K50i validation and direct game EXE hand-off remain follow-up work.", color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun EngineComponent(name: String, description: String, installed: Boolean) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Surface1).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Extension, null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextMuted, fontSize = 9.sp)
        }
        Text(if (installed) "TESTED" else "UNVERIFIED", color = if (installed) Green else Orange, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        TopHeader("Settings", "Personalise your gaming setup")
        Spacer(Modifier.height(5.dp))
        Text("PREFERENCES", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Black)
        SettingRow("Application", "PROJECT R • Preview 0.4.0", Icons.Default.Apps)
        SettingRow("Game library", "Folders are selected with Android's document picker", Icons.Default.FolderOpen)
        SettingRow("Compatibility engine", "Winlator runtime bundled • first-run setup required", Icons.Default.Memory)
        SettingRow("Graphics", "GPU backend needs device validation", Icons.Default.Tune)
        SettingRow("Controls", "Winlator touchscreen and gamepad settings", Icons.Default.Gamepad)
        SettingRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}", Icons.Default.Smartphone)
        Spacer(Modifier.height(4.dp))
        Text("PROJECT R IS A PERSONAL PROJECT. WINDOWS GAME FILES AND GAME LICENSES ARE NOT INCLUDED. THE BUNDLED WINLATOR COMPONENTS HAVE SEPARATE OPEN-SOURCE LICENSES.", color = TextMuted, fontSize = 8.sp, lineHeight = 13.sp, letterSpacing = .5.sp)
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Surface1).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 9.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ProjectBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = Surface1, contentColor = TextMain, tonalElevation = 0.dp) {
        NavigationBarItem(selected == AppTab.HOME, { onSelect(AppTab.HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("HOME", fontSize = 8.sp, fontWeight = FontWeight.Bold) }, colors = navColors())
        NavigationBarItem(selected == AppTab.LIBRARY, { onSelect(AppTab.LIBRARY) }, icon = { Icon(Icons.Default.ViewModule, null) }, label = { Text("LIBRARY", fontSize = 8.sp, fontWeight = FontWeight.Bold) }, colors = navColors())
        NavigationBarItem(selected == AppTab.ENGINE, { onSelect(AppTab.ENGINE) }, icon = { Icon(Icons.Default.Memory, null) }, label = { Text("ENGINE", fontSize = 8.sp, fontWeight = FontWeight.Bold) }, colors = navColors())
        NavigationBarItem(selected == AppTab.SETTINGS, { onSelect(AppTab.SETTINGS) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("SETTINGS", fontSize = 8.sp, fontWeight = FontWeight.Bold) }, colors = navColors())
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Cyan,
    selectedTextColor = Cyan,
    indicatorColor = Cyan.copy(alpha = .12f),
    unselectedIconColor = TextMuted,
    unselectedTextColor = TextMuted
)
