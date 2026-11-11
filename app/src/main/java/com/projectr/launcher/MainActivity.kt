package com.projectr.launcher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

enum class AppTab { HOME, LIBRARY, CONTAINERS, ENGINE, SETTINGS }

private data class ActiveXServerSession(
    val game: GameEntry?,
    val container: WinlatorContainer
)

class MainActivity : ComponentActivity() {
    private val repository by lazy { ProjectRRepository(this) }
    private var onFolderPicked: ((Uri, String) -> Unit)? = null
    private var onExecutablePicked: ((Uri, String) -> Unit)? = null
    private var onRootFsArchivePicked: ((Uri, String) -> Unit)? = null

    private val executablePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "game.exe"
            onExecutablePicked?.invoke(uri, fileName)
        }
    }

    private val rootFsPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "main.obb"
            onRootFsArchivePicked?.invoke(uri, fileName)
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
        enableEdgeToEdge()
        val gpuReport = GpuCapabilityDetector.inspect(this)

        setContent {
            var games by remember { mutableStateOf(repository.loadGames()) }
            var containers by remember { mutableStateOf(repository.loadContainers()) }
            var rootFsArchive by remember { mutableStateOf(repository.getRootFsArchiveName()) }
            var tab by remember { mutableStateOf(AppTab.HOME) }
            var selectedGame by remember { mutableStateOf<GameEntry?>(null) }
            var activeSession by remember { mutableStateOf<ActiveXServerSession?>(null) }
            var editingShortcutGame by remember { mutableStateOf<GameEntry?>(null) }
            var showLicencesDialog by remember { mutableStateOf(false) }
            var toastMessage by remember { mutableStateOf<String?>(null) }
            var bundledContainerChoices by remember { mutableStateOf<List<WinlatorGameLauncher.ContainerOption>>(emptyList()) }
            var pendingBundledGame by remember { mutableStateOf<GameEntry?>(null) }
            var bundledLaunchError by remember { mutableStateOf<String?>(null) }
            var bundledLaunchNeedsSetup by remember { mutableStateOf(false) }
            var preparingBundledGame by remember { mutableStateOf(false) }

            val externalInstalled = RuntimeBridge.isInstalled(this@MainActivity)
            val detectedPkg = RuntimeBridge.detectInstalledPackage(this@MainActivity)

            fun persistGames(updated: List<GameEntry>) {
                repository.saveGames(updated)
                games = repository.loadGames()
            }

            fun persistContainers(updated: List<WinlatorContainer>) {
                repository.saveContainers(updated)
                containers = repository.loadContainers()
            }

            fun launchGameInBundledContainer(game: GameEntry, containerId: Int) {
                pendingBundledGame = null
                preparingBundledGame = true
                lifecycleScope.launch {
                    try {
                        val intent = withContext(Dispatchers.IO) {
                            WinlatorGameLauncher.prepareLaunch(this@MainActivity, game, containerId)
                        }
                        preparingBundledGame = false
                        startActivity(intent)
                    } catch (error: Exception) {
                        preparingBundledGame = false
                        bundledLaunchNeedsSetup = false
                        bundledLaunchError = error.message ?: "The game could not be prepared for the bundled runtime."
                    }
                }
            }

            fun requestBundledGameLaunch(game: GameEntry) {
                if (game.exeUri.isNullOrBlank()) {
                    bundledLaunchNeedsSetup = false
                    bundledLaunchError = "Link the game's EXE first. Choose an EXE from inside the imported folder so its data files can be copied with it."
                    return
                }
                if (!WinlatorGameLauncher.isRuntimeReady(this@MainActivity)) {
                    bundledLaunchNeedsSetup = true
                    bundledLaunchError = "The bundled Winlator runtime has not completed its first-run setup. Open it, wait for the root filesystem setup, and create a container."
                    return
                }
                val options = runCatching {
                    WinlatorGameLauncher.listContainers(this@MainActivity)
                }.getOrDefault(emptyList())
                if (options.isEmpty()) {
                    bundledLaunchNeedsSetup = true
                    bundledLaunchError = "Create and initialize a container in the bundled Winlator runtime before launching this game."
                } else if (options.size == 1) {
                    launchGameInBundledContainer(game, options.first().id)
                } else {
                    bundledContainerChoices = options
                    pendingBundledGame = game
                }
            }

            onFolderPicked = { uri, name ->
                if (games.none { it.uri == uri.toString() }) {
                    val defaultContainerId = containers.firstOrNull()?.id ?: 1
                    val newEntry = GameEntry(
                        name = name,
                        uri = uri.toString(),
                        added = "Added ${currentFormattedDate()}",
                        containerId = defaultContainerId
                    )
                    persistGames(games + newEntry)
                    toastMessage = "$name added & bound to Container #$defaultContainerId"
                } else {
                    toastMessage = "This folder is already in your library"
                }
                tab = AppTab.LIBRARY
            }

            onExecutablePicked = { uri, fileName ->
                val current = selectedGame
                if (current != null) {
                    val updated = games.map {
                        if (it.uri == current.uri) {
                            it.copy(exeUri = uri.toString(), exeFileName = fileName)
                        } else {
                            it
                        }
                    }
                    persistGames(updated)
                    selectedGame = games.firstOrNull { it.uri == current.uri }
                    toastMessage = "Linked $fileName to ${current.name} (.desktop shortcut updated)"
                }
            }

            onRootFsArchivePicked = { uri, fileName ->
                repository.setRootFsArchive(uri.toString(), fileName)
                rootFsArchive = fileName
                toastMessage = "Mounted Winlator RootFS archive: $fileName"
            }

            ProjectRTheme {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    when {
                        activeSession != null -> {
                            val session = activeSession!!
                            XServerSessionScreen(
                                game = session.game,
                                container = session.container,
                                gpuReport = gpuReport,
                                rootFsArchive = rootFsArchive,
                                externalRuntimeInstalled = externalInstalled,
                                onLaunchExternalWinlator = {
                                    toastMessage = if (session.game != null) {
                                        RuntimeBridge.launchExternalShortcutIfAvailable(
                                            this@MainActivity,
                                            session.game,
                                            session.container
                                        )
                                    } else {
                                        RuntimeBridge.openOrInstall(this@MainActivity)
                                    }
                                },
                                onExitSession = { activeSession = null }
                            )
                        }

                        selectedGame != null -> {
                            val game = selectedGame!!
                            val boundContainer = containers.firstOrNull { it.id == game.containerId }
                                ?: containers.first()
                            GameDetailsScreen(
                                game = game,
                                container = boundContainer,
                                gpuReport = gpuReport,
                                runtimeInstalled = externalInstalled,
                                onBack = { selectedGame = null },
                                onConfigureShortcut = { editingShortcutGame = game },
                                onRemove = {
                                    persistGames(games.filterNot { it.uri == game.uri })
                                    selectedGame = null
                                    toastMessage = "Game removed from library"
                                },
                                onChooseExecutable = { executablePicker.launch(arrayOf("*/*")) },
                                onLaunchXServerSession = {
                                    val updated = games.map {
                                        if (it.uri == game.uri) it.copy(lastLaunched = currentFormattedDate()) else it
                                    }
                                    persistGames(updated)
                                    selectedGame = games.firstOrNull { it.uri == game.uri }
                                    activeSession = ActiveXServerSession(game = selectedGame, container = boundContainer)
                                },
                                onOpenExternalRuntime = {
                                    toastMessage = RuntimeBridge.launchExternalShortcutIfAvailable(
                                        this@MainActivity,
                                        game,
                                        boundContainer
                                    )
                                },
                                onLaunchBundledRuntime = { requestBundledGameLaunch(game) }
                            )
                        }

                        else -> Scaffold(
                            containerColor = Ink,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = { ProjectBottomBar(tab) { tab = it } }
                        ) { padding ->
                            Box(Modifier.fillMaxSize().padding(padding)) {
                                when (tab) {
                                    AppTab.HOME -> HomeScreen(
                                        games = games,
                                        containers = containers,
                                        gpuReport = gpuReport,
                                        runtimeInstalled = externalInstalled,
                                        onImport = { folderPicker.launch(null) },
                                        onLibrary = { tab = AppTab.LIBRARY },
                                        onContainers = { tab = AppTab.CONTAINERS },
                                        onGame = { selectedGame = it },
                                        onEngine = { tab = AppTab.ENGINE }
                                    )

                                    AppTab.LIBRARY -> LibraryScreen(
                                        games = games,
                                        containers = containers,
                                        onImport = { folderPicker.launch(null) },
                                        onAddIgi2Preset = {
                                            val igiUri = "content://com.projectr.launcher/presets/igi2_covert_strike"
                                            if (games.none { it.uri == igiUri }) {
                                                val targetContainer = containers.firstOrNull()?.id ?: 1
                                                val presetGame = GameEntry(
                                                    name = "IGI 2: Covert Strike",
                                                    uri = igiUri,
                                                    added = "Added ${currentFormattedDate()}",
                                                    exeUri = "$igiUri/pc/igi2.exe",
                                                    exeFileName = "igi2.exe",
                                                    containerId = targetContainer,
                                                    execArgs = "-windowed",
                                                    overrideDxWrapper = "dgVoodoo2 (Direct3D 8 -> D3D11)",
                                                    dinputMapperType = "IGI 2 Tactical FPS (Mouse + WASD Overlay)"
                                                )
                                                persistGames(games + presetGame)
                                                toastMessage = "IGI 2: Covert Strike profile created for Container #$targetContainer"
                                            } else {
                                                toastMessage = "IGI 2 profile is already in your library"
                                            }
                                        },
                                        onGame = { selectedGame = it }
                                    )

                                    AppTab.CONTAINERS -> ContainersScreen(
                                        containers = containers,
                                        gpuReport = gpuReport,
                                        onCreateContainer = { newContainer ->
                                            persistContainers(containers + newContainer)
                                            toastMessage = "Created ${newContainer.name}"
                                        },
                                        onUpdateContainer = { updated ->
                                            persistContainers(containers.map { if (it.id == updated.id) updated else it })
                                            toastMessage = "Saved Container #${updated.id}"
                                        },
                                        onDeleteContainer = { toDelete ->
                                            persistContainers(containers.filterNot { it.id == toDelete.id })
                                            toastMessage = "Removed Container #${toDelete.id}"
                                        },
                                        onBootContainer = { containerToBoot ->
                                            activeSession = ActiveXServerSession(game = null, container = containerToBoot)
                                        }
                                    )

                                    AppTab.ENGINE -> EngineScreen(
                                        gpuReport = gpuReport,
                                        containers = containers,
                                        rootFsArchive = rootFsArchive,
                                        runtimeInstalled = externalInstalled,
                                        detectedPkg = detectedPkg,
                                        onMountRootFs = { rootFsPicker.launch(arrayOf("*/*")) },
                                        onBootDefaultContainer = {
                                            activeSession = ActiveXServerSession(
                                                game = null,
                                                container = containers.first()
                                            )
                                        },
                                        onOpenRuntime = {
                                            toastMessage = RuntimeBridge.openOrInstall(this@MainActivity)
                                        }
                                    )

                                    AppTab.SETTINGS -> SettingsScreen(
                                        gpuReport = gpuReport,
                                        containersCount = containers.size,
                                        gamesCount = games.size,
                                        onOpenContainers = { tab = AppTab.CONTAINERS },
                                        onOpenLicences = { showLicencesDialog = true }
                                    )
                                }

                                AnimatedVisibility(
                                    visible = toastMessage != null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                ) {
                                    Surface(
                                        color = Surface3,
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Green,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(9.dp))
                                            Text(
                                                toastMessage.orEmpty(),
                                                color = TextMain,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "OK",
                                                color = Cyan,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { toastMessage = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (editingShortcutGame != null) {
                        GameShortcutConfigDialog(
                            game = editingShortcutGame!!,
                            containers = containers,
                            onDismiss = { editingShortcutGame = null },
                            onSave = { updatedGame ->
                                val updatedList = games.map {
                                    if (it.uri == updatedGame.uri) updatedGame else it
                                }
                                persistGames(updatedList)
                                if (selectedGame?.uri == updatedGame.uri) {
                                    selectedGame = games.firstOrNull { it.uri == updatedGame.uri }
                                }
                                editingShortcutGame = null
                                toastMessage = "Updated Winlator shortcut for ${updatedGame.name}"
                            }
                        )
                    }

                    if (showLicencesDialog) {
                        OpenSourceNoticesDialog(onDismiss = { showLicencesDialog = false })
                    }

                    if (pendingBundledGame != null) {
                        AlertDialog(
                            onDismissRequest = { pendingBundledGame = null },
                            title = { Text("Choose bundled Winlator container", color = TextMain) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    bundledContainerChoices.forEach { option ->
                                        OutlinedButton(
                                            onClick = { launchGameInBundledContainer(pendingBundledGame!!, option.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(option.name, color = TextMain) }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = { TextButton(onClick = { pendingBundledGame = null }) { Text("CANCEL") } },
                            containerColor = Surface2
                        )
                    }

                    if (bundledLaunchError != null) {
                        AlertDialog(
                            onDismissRequest = { bundledLaunchError = null },
                            title = { Text(if (bundledLaunchNeedsSetup) "Prepare Winlator" else "Can't launch game", color = TextMain) },
                            text = { Text(bundledLaunchError.orEmpty(), color = TextMuted) },
                            confirmButton = {
                                TextButton(onClick = {
                                    val openRuntime = bundledLaunchNeedsSetup
                                    bundledLaunchError = null
                                    if (openRuntime) toastMessage = RuntimeBridge.openOrInstall(this@MainActivity)
                                }) {
                                    Text(if (bundledLaunchNeedsSetup) "OPEN WINLATOR" else "GOT IT", color = Cyan)
                                }
                            },
                            containerColor = Surface2
                        )
                    }

                    if (preparingBundledGame) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Preparing game", color = TextMain) },
                            text = { Text("Copying the imported game folder into the selected Winlator container. Large games may take several minutes.", color = TextMuted) },
                            confirmButton = {},
                            containerColor = Surface2
                        )
                    }
                }
            }
        }
    }
}

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
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Cyan, Violet))),
            contentAlignment = Alignment.Center
        ) {
            Text("R", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
            Text("PROJECT R", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Text("WINLATOR HOST • PHASE 4", color = TextMuted, fontSize = 8.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopHeader(subtitle: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BrandMark()
            if (subtitle != null) {
                Text(subtitle.uppercase(), color = TextMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Surface2)
                .border(1.dp, Color.White.copy(alpha = .07f), CircleShape)
                .then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Memory, contentDescription = "Engine Status", tint = Cyan, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun HomeScreen(
    games: List<GameEntry>,
    containers: List<WinlatorContainer>,
    gpuReport: GpuCapabilityReport,
    runtimeInstalled: Boolean,
    onImport: () -> Unit,
    onLibrary: () -> Unit,
    onContainers: () -> Unit,
    onGame: (GameEntry) -> Unit,
    onEngine: () -> Unit
) {
    val firstGame = games.firstOrNull()
    val firstGameContainer = containers.firstOrNull { it.id == firstGame?.containerId } ?: containers.first()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { TopHeader("Your PC collection & Winlator host", onActionClick = onEngine) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "YOUR NEXT\nPC GAMING SESSION.",
                    color = TextMain,
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-.8).sp
                )
                Text(
                    "Reskinned Winlator container host • ${gpuReport.glRenderer} (${gpuReport.recommendedDriver.substringBefore(" ")})",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
        item {
            FeaturedPanel(
                title = if (firstGame == null) "BUILD YOUR\nGAME LIBRARY" else firstGame.name.uppercase(),
                eyebrow = if (firstGame == null) "WELCOME TO PROJECT R" else "CONTAINER #${firstGameContainer.id} • ${firstGameContainer.graphicsDriver.substringBefore(" ").uppercase()}",
                detail = if (firstGame == null) {
                    "Import a Windows game folder or create a Direct3D 8 container profile."
                } else if (!firstGame.exeUri.isNullOrBlank()) {
                    "Linked: ${firstGame.exeFileName ?: "Executable ready"} • ${firstGame.overrideDxWrapper ?: firstGameContainer.dxWrapper}"
                } else {
                    "Folder imported • Select a Windows .exe to enable direct XServer launch."
                },
                buttonText = if (firstGame == null) "IMPORT YOUR FIRST GAME" else "OPEN GAME & LAUNCHER",
                onClick = { if (firstGame == null) onImport() else onGame(firstGame) }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("LIBRARY", "${games.size} GAMES", Icons.Default.SportsEsports, Modifier.weight(1f), Cyan)
                MetricCard("CONTAINERS", "${containers.size} ACTIVE", Icons.Default.Dns, Modifier.weight(1f).clickable { onContainers() }, Violet)
                MetricCard(
                    label = "ENGINE",
                    value = if (runtimeInstalled) "APK + HOST" else "HOST READY",
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f).clickable { onEngine() },
                    tint = Green
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading("RECENTLY ADDED", "Game shortcuts & containers")
                Text(
                    "SEE ALL",
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { onLibrary() }
                )
            }
        }
        if (games.isEmpty()) {
            item { EmptyLibraryCard(onImport) }
        } else {
            items(games.take(4)) { game ->
                val c = containers.firstOrNull { it.id == game.containerId } ?: containers.first()
                GameTile(game = game, container = c, onClick = { onGame(game) })
            }
        }
        item {
            EngineTeaser(gpuReport = gpuReport, containersCount = containers.size, onClick = onEngine)
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
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF253B59), Color(0xFF26274D), Color(0xFF111827))))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .92f))))
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Cyan.copy(alpha = .30f), Violet.copy(alpha = .06f), Color.Transparent)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(eyebrow, color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
            Text(
                title,
                color = TextMain,
                fontSize = 24.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(detail, color = TextMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(42.dp).testTag("featured_action_button")
            ) {
                Icon(
                    if (buttonText.contains("IMPORT")) Icons.Default.Add else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
            }
        }
        Text(
            "R",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 25.dp, top = 20.dp),
            color = Color.White.copy(alpha = .09f),
            fontSize = 100.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Cyan
) {
    Column(
        modifier
            .clip(RoundedCornerShape(17.dp))
            .background(Surface1)
            .border(1.dp, Color.White.copy(alpha = .05f), RoundedCornerShape(17.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(value, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
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
private fun GameTile(game: GameEntry, container: WinlatorContainer, onClick: () -> Unit) {
    val hasExe = !game.exeUri.isNullOrBlank()
    val statusColor = if (hasExe) Green else Orange
    val statusLabel = if (hasExe) {
        "READY • CONTAINER #${container.id} (${container.graphicsDriver.substringBefore(" ").uppercase()})"
    } else {
        "IMPORTED • SELECT .EXE TO LINK SHORTCUT"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Surface1)
            .border(1.dp, Color.White.copy(alpha = .05f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF244B65), Color(0xFF38315E)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                game.name,
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hasExe) "${game.exeFileName ?: "Linked .exe"} • ${game.added}" else game.added,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(5.dp))
                Text(
                    statusLabel,
                    color = statusColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = .5.sp
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun EmptyLibraryCard(onImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, Color.White.copy(alpha = .06f), RoundedCornerShape(18.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Cyan, modifier = Modifier.size(32.dp))
        Text("YOUR LIBRARY IS WAITING", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Add a folder containing your Windows game files or load the IGI 2 preset.", color = TextMuted, fontSize = 10.sp)
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("IMPORT GAME FOLDER", fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EngineTeaser(gpuReport: GpuCapabilityReport, containersCount: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF18263B), Color(0xFF211F3D))))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Memory, contentDescription = null, tint = Cyan, modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("WINLATOR HOST • ${gpuReport.glRenderer.uppercase()}", color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                "$containersCount container(s) configured • ${gpuReport.recommendedDriver} • ${gpuReport.vulkanVersionLabel}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Cyan)
    }
}

@Composable
private fun LibraryScreen(
    games: List<GameEntry>,
    containers: List<WinlatorContainer>,
    onImport: () -> Unit,
    onAddIgi2Preset: () -> Unit,
    onGame: (GameEntry) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).padding(top = 20.dp)) {
        TopHeader("Your imported Windows games & shortcuts")
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MY COLLECTION", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("${games.size} game profile${if (games.size == 1) "" else "s"} configured", color = TextMuted, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddIgi2Preset,
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("IGI 2 PRESET", fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                    shape = RoundedCornerShape(11.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text(" ADD", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (games.isEmpty()) {
            EmptyLibraryCard(onImport)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(games) { game ->
                    val c = containers.firstOrNull { it.id == game.containerId } ?: containers.first()
                    GameTile(game = game, container = c) { onGame(game) }
                }
            }
        }
    }
}

@Composable
private fun GameDetailsScreen(
    game: GameEntry,
    container: WinlatorContainer,
    gpuReport: GpuCapabilityReport,
    runtimeInstalled: Boolean,
    onBack: () -> Unit,
    onConfigureShortcut: () -> Unit,
    onRemove: () -> Unit,
    onChooseExecutable: () -> Unit,
    onLaunchXServerSession: () -> Unit,
    onOpenExternalRuntime: () -> Unit,
    onLaunchBundledRuntime: () -> Unit
) {
    BackHandler { onBack() }

    val hasExe = !game.exeUri.isNullOrBlank()
    val effectiveResolution = game.overrideScreenSize ?: container.screenSize
    val effectiveDx = game.overrideDxWrapper ?: container.dxWrapper

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("details_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextMain)
            }
            Text("GAME & SHORTCUT OVERVIEW", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        }

        FeaturedPanel(
            title = game.name.uppercase(),
            eyebrow = "CONTAINER #${container.id} • ${container.name.uppercase()}",
            detail = if (hasExe) {
                "Ready to launch (${game.exeFileName ?: "Linked .exe"}) • ${container.graphicsDriver} • $effectiveDx"
            } else {
                "Imported folder • Link a Windows .exe to enable direct container boot"
            },
            buttonText = "OPEN PHASE 4 SESSION VIEW",
            onClick = onLaunchXServerSession
        )

        StatusLine("CONTAINER BINDING", "#${container.id} • ${container.name} ($effectiveResolution)", Cyan)
        StatusLine(
            "GRAPHICS & DIRECTX",
            "${container.graphicsDriver} • $effectiveDx",
            if (!gpuReport.isAdrenoGpu && container.graphicsDriver.contains("Turnip", true)) Orange else Green
        )
        StatusLine(
            "WINDOWS EXECUTABLE",
            if (hasExe) game.exeFileName ?: "Windows .exe linked" else "Not linked yet",
            if (hasExe) Green else Orange
        )
        StatusLine(
            "EXTERNAL WINLATOR APK",
            if (runtimeInstalled) "External APK detected; bundled runtime is also available" else "Bundled Winlator runtime available; no separate APK detected",
            if (runtimeInstalled) Green else Cyan
        )

        InfoCard("GAME FOLDER URI", game.uri)
        InfoCard("WINDOWS EXECUTABLE URI", game.exeUri ?: "No .exe selected yet")
        InfoCard(
            "GENERATED WINLATOR .DESKTOP SHORTCUT",
            game.shortcutPath ?: "/data/user/0/com.projectr.launcher/files/imagefs/home/xuser/Desktop/${game.name}.desktop"
        )

        Button(
            onClick = onLaunchXServerSession,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("launch_xserver_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("OPEN PHASE 4 SESSION VIEW", fontWeight = FontWeight.Black)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChooseExecutable, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (game.exeUri == null) "LINK .EXE" else "CHANGE .EXE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onConfigureShortcut, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SHORTCUT CONFIG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onLaunchBundledRuntime,
            enabled = hasExe,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Ink),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("LAUNCH GAME IN BUNDLED WINLATOR", fontWeight = FontWeight.Black)
        }

        OutlinedButton(
            onClick = onOpenExternalRuntime,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (runtimeInstalled) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (runtimeInstalled) "DISPATCH TO EXTERNAL WINLATOR APK" else "DOWNLOAD OFFICIAL WINLATOR APK",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8C8C))
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("REMOVE FROM LIBRARY")
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String, tint: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface1)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface1)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text(value, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EngineScreen(
    gpuReport: GpuCapabilityReport,
    containers: List<WinlatorContainer>,
    rootFsArchive: String?,
    runtimeInstalled: Boolean,
    detectedPkg: String?,
    onMountRootFs: () -> Unit,
    onBootDefaultContainer: () -> Unit,
    onOpenRuntime: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TopHeader("Winlator compatibility runtime & GPU inspector")
        Text("RUNTIME & GPU CENTER", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)

        // Hero Runtime Card
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF263A58), Color(0xFF211F3E))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Green))
                    Spacer(Modifier.width(6.dp))
                    Text("PHASE 4 HOST ACTIVE", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
            Text("PROJECT R • WINLATOR ARCHITECTURE", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                "Upstream baseline: ${RuntimeBridge.PINNED_UPSTREAM_REF} • ${containers.size} Container(s) initialized in /files/imagefs",
                color = TextMuted,
                fontSize = 10.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBootDefaultContainer,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("OPEN XSERVER DESKTOP", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onMountRootFs,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (rootFsArchive == null) "LINK OBB / ROOTFS" else "CHANGE OBB", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live GPU / Vulkan / Mali-G610 Inspector
        Text("REAL-TIME DEVICE GPU & VULKAN INSPECTOR", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        InfoCard(
            "OPENGL ES RENDERER & VENDOR",
            "${gpuReport.glRenderer} • ${gpuReport.glVendor}\n${gpuReport.glVersion}"
        )
        InfoCard(
            "SOC / VULKAN / ABI TELEMETRY",
            "${Build.MANUFACTURER} ${Build.MODEL} • SoC: ${gpuReport.socModel} • ${gpuReport.vulkanVersionLabel} • ABI: ${gpuReport.primaryAbi}"
        )
        InfoCard(
            "HARDWARE DRIVER POLICY (REDMI K50I / MALI GUARD)",
            "Recommended Driver: ${gpuReport.recommendedDriver}\nRecommended DirectX/D3D8 Layer: ${gpuReport.recommendedDxWrapper}\n${gpuReport.turnipBlockedReason ?: "Adreno Turnip driver supported on this GPU."}"
        )

        Text("RUNTIME COMPONENTS", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        EngineComponent("Bundled upstream Winlator runtime", "Pinned source: ${RuntimeBridge.PINNED_UPSTREAM_REF}", true)
        EngineComponent("Phase 4 profile & GPU tools", "PROJECT R container profiles and ${gpuReport.glRenderer} hardware inspection", true)
        EngineComponent("Runtime initialization", if (rootFsArchive != null) "Optional archive linked: $rootFsArchive" else "Open Winlator, finish first-run setup, then create a runtime container", false)

        // Optional external Winlator APK row
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Surface1)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("OPTIONAL EXTERNAL WINLATOR APK", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (runtimeInstalled) "Detected package: $detectedPkg" else "External APK not installed (Optional)",
                    color = if (runtimeInstalled) Green else TextMuted,
                    fontSize = 10.sp
                )
            }
            Button(
                onClick = onOpenRuntime,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
            ) {
                Text("OPEN BUNDLED", fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun EngineComponent(name: String, description: String, ready: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface1)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (ready) Icons.Default.CheckCircle else Icons.Default.Extension,
            contentDescription = null,
            tint = if (ready) Green else Orange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextMuted, fontSize = 9.sp)
        }
        Text(
            if (ready) "ACTIVE" else "PENDING",
            color = if (ready) Green else Orange,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SettingsScreen(
    gpuReport: GpuCapabilityReport,
    containersCount: Int,
    gamesCount: Int,
    onOpenContainers: () -> Unit,
    onOpenLicences: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TopHeader("Personalise your Winlator host setup")
        Text("PREFERENCES & COMPLIANCE", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
        SettingRow("Application", "PROJECT R • Phase 4 Host (v0.4.0)", Icons.Default.Apps)
        SettingRow("Upstream Baseline", RuntimeBridge.PINNED_UPSTREAM_REF, Icons.Default.Verified)
        SettingRow(
            "Preference Store Sync",
            "Synced across project_r_v2 & project_r ($gamesCount games, $containersCount containers)",
            Icons.Default.Storage
        )
        SettingRow(
            "Detected Graphics Hardware",
            "${gpuReport.glRenderer} • Default: ${gpuReport.recommendedDriver}",
            Icons.Default.DeveloperBoard
        )
        SettingRow(
            "Container Manager",
            "Configure Wine, Box64, DXVK/WineD3D, and CPU affinity",
            Icons.Default.Dns,
            onClick = onOpenContainers
        )
        SettingRow(
            "Open-Source Notices & Licences",
            "View LGPL-2.1, Wine, Box64, Mesa, DXVK, VKD3D & PRoot licences",
            Icons.Default.Policy,
            onClick = onOpenLicences
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "PROJECT R IS A PERSONAL LAUNCHER & RESKINNED WINLATOR HOST ENVIRONMENT. WINDOWS GAME BINARIES ARE USER-SUPPLIED VIA ANDROID SAF. RETAINS ALL UPSTREAM LGPL-2.1 / MIT / GPL-2.0 THIRD-PARTY NOTICES.",
            color = TextMuted,
            fontSize = 8.sp,
            lineHeight = 13.sp,
            letterSpacing = .5.sp
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Surface1)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 9.sp)
        }
        if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProjectBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = Surface1, contentColor = TextMain, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = selected == AppTab.HOME,
            onClick = { onSelect(AppTab.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("HOME", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
            colors = navColors()
        )
        NavigationBarItem(
            selected = selected == AppTab.LIBRARY,
            onClick = { onSelect(AppTab.LIBRARY) },
            icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Library") },
            label = { Text("LIBRARY", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
            colors = navColors()
        )
        NavigationBarItem(
            selected = selected == AppTab.CONTAINERS,
            onClick = { onSelect(AppTab.CONTAINERS) },
            icon = { Icon(Icons.Default.Dns, contentDescription = "Containers") },
            label = { Text("CONTAINERS", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
            colors = navColors()
        )
        NavigationBarItem(
            selected = selected == AppTab.ENGINE,
            onClick = { onSelect(AppTab.ENGINE) },
            icon = { Icon(Icons.Default.Memory, contentDescription = "Engine") },
            label = { Text("ENGINE", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
            colors = navColors()
        )
        NavigationBarItem(
            selected = selected == AppTab.SETTINGS,
            onClick = { onSelect(AppTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("SETTINGS", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
            colors = navColors()
        )
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
