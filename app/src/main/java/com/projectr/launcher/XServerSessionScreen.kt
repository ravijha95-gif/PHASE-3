package com.projectr.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val InkBg = Color(0xFF080B12)
private val PanelSurface = Color(0xFF111827)
private val PanelSurface2 = Color(0xFF182235)
private val AccentCyan = Color(0xFF65E6F2)
private val AccentViolet = Color(0xFF9B8CFF)
private val AccentGreen = Color(0xFF8BE28B)
private val AccentOrange = Color(0xFFFFB86B)
private val TextPrimary = Color(0xFFF4F7FB)
private val TextSecondary = Color(0xFF9AA9BC)

private enum class SessionDrawerTab { DESKTOP, LOGS, PROCESSES, CONTROLS }

@Composable
fun XServerSessionScreen(
    game: GameEntry?,
    container: WinlatorContainer,
    gpuReport: GpuCapabilityReport,
    rootFsArchive: String?,
    externalRuntimeInstalled: Boolean,
    onLaunchExternalWinlator: () -> Unit,
    onExitSession: () -> Unit
) {
    BackHandler { onExitSession() }

    val exeName = remember(game) {
        game?.exeFileName
            ?: game?.exeUri?.substringAfterLast('/')?.substringAfterLast(':')
            ?: "explorer.exe"
    }
    val activeResolution = game?.overrideScreenSize ?: container.screenSize
    val activeDxWrapper = game?.overrideDxWrapper ?: container.dxWrapper

    var bootStep by remember { mutableIntStateOf(0) }
    var showTouchControls by remember { mutableStateOf(true) }
    var showHudOverlay by remember { mutableStateOf(container.showFps) }
    var activeTab by remember { mutableStateOf(SessionDrawerTab.DESKTOP) }
    var cursorOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var lastInputEvent by remember { mutableStateOf("XInput Mouse Ready • DirectInput8 Hook Active") }
    var selectedDesktopWindow by remember { mutableStateOf("DXDIAG") }

    val bootLogs = remember(game, container, gpuReport, rootFsArchive) {
        RuntimeBridge.generateBootLogs(game, container, gpuReport, rootFsArchive)
    }
    var runtimeLogs by remember { mutableStateOf(bootLogs) }

    LaunchedEffect(Unit) {
        for (i in 1..4) {
            delay(180)
            bootStep = i
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Winlator XServer Status Bar (Reskinned PROJECT R Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onExitSession,
                    modifier = Modifier.size(36.dp).testTag("exit_xserver_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Exit XServer Session",
                        tint = TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (bootStep >= 4) AccentGreen else AccentOrange)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = game?.name?.uppercase() ?: container.name.uppercase(),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "XServer :0 • ${activeResolution.substringBefore(" ")} • ${container.graphicsDriver.substringBefore(" ")}",
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = showTouchControls,
                    onClick = { showTouchControls = !showTouchControls },
                    label = { Text("PAD", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.Gamepad, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = showHudOverlay,
                    onClick = { showHudOverlay = !showHudOverlay },
                    label = { Text("HUD", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // XServer Display Viewport + Touchpad / Virtual Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0D1726), Color(0xFF141B2D), Color(0xFF090D16))
                    )
                )
                .border(1.dp, AccentCyan.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val nx = (cursorOffset.x + dragAmount.x / size.width.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                        val ny = (cursorOffset.y + dragAmount.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
                        cursorOffset = Offset(nx, ny)
                        lastInputEvent = "Mouse Move (${(nx * 1280).toInt()}, ${(ny * 720).toInt()})"
                    }
                }
        ) {
            // Subtle grid & XServer cursor canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 36.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.03f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }

                // Draw virtual X11 mouse pointer
                val cx = cursorOffset.x * size.width
                val cy = cursorOffset.y * size.height
                drawCircle(color = AccentCyan.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = AccentCyan, radius = 4.dp.toPx(), center = Offset(cx, cy))
            }

            // Simulated Wine Desktop / Active Game Container Window inside XServer :0
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1525).copy(alpha = 0.92f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "WINE DESKTOP • C:\\windows\\system32\\$exeName",
                            color = AccentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = container.wineVersion,
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                when (selectedDesktopWindow) {
                    "DXDIAG" -> {
                        Text(
                            text = "DirectX Diagnostic & Renderer Binding",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Host GPU: ${gpuReport.glRenderer} (${gpuReport.socModel})",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Driver: ${container.graphicsDriver} • Wrapper: $activeDxWrapper",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "D3D8 Layer: ${container.d3d8Override} • CPU Mask: [${container.cpuList}]",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    "WINEPREFIX" -> {
                        Text(
                            text = "Drive Mappings (xuser-.wine-${container.id})",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "C:\\ -> /data/user/0/com.projectr.launcher/files/imagefs/home/xuser/.wine/drive_c",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "D:\\ -> ${game?.uri ?: "/storage/emulated/0/Games"} ($exeName)",
                            color = AccentCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { selectedDesktopWindow = "DXDIAG" },
                        label = { Text("dxdiag.exe", fontSize = 9.sp) }
                    )
                    AssistChip(
                        onClick = { selectedDesktopWindow = "WINEPREFIX" },
                        label = { Text("winecfg Drives", fontSize = 9.sp) }
                    )
                    if (externalRuntimeInstalled) {
                        AssistChip(
                            onClick = onLaunchExternalWinlator,
                            label = { Text("Bridge -> Winlator", fontSize = 9.sp, color = AccentCyan) }
                        )
                    }
                }
            }

            // Winlator FPS & Telemetry HUD Overlay
            if (showHudOverlay) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(
                            text = "${container.graphicsDriver.substringBefore(" ").uppercase()} • ${activeDxWrapper.substringBefore(" ")} • 60.0 FPS",
                            color = AccentGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${gpuReport.glRenderer} • CPU [${container.cpuList}]",
                            color = AccentCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Virtual Gamepad / IGI 2 PC Controls Overlay
            if (showTouchControls) {
                // Left WASD Cluster
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("W", "A", "S", "D", "C", "SPACE").forEach { key ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(min = 32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable {
                                    lastInputEvent = "Key Press: [$key] -> X11 KeySym Dispatched"
                                }
                                .padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(key, color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Right Mouse / Action Cluster (LMB, RMB, Reload, Map)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("LMB" to AccentCyan, "RMB" to AccentViolet, "RELOAD" to AccentGreen, "MAP" to AccentOrange).forEach { (btn, tint) ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.68f))
                                .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                .clickable {
                                    lastInputEvent = "Action: [$btn] -> Wine DirectInput8 Event"
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(btn, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Input telemetry bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lastInputEvent,
                color = AccentCyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Drag viewport to move X11 cursor",
                color = TextSecondary,
                fontSize = 9.sp
            )
        }

        // Reskinned Winlator Bottom Control Drawer Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SessionDrawerTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) AccentCyan.copy(alpha = 0.16f) else PanelSurface)
                        .border(
                            1.dp,
                            if (selected) AccentCyan else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeTab = tab }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.name,
                        color = if (selected) AccentCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            when (activeTab) {
                SessionDrawerTab.DESKTOP -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Surface(
                                color = PanelSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "CONTAINER #${container.id}: ${container.name.uppercase()}",
                                        color = AccentCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Executable: $exeName • Box64 Preset: ${container.box64Preset}",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Env: ${container.envVars}",
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onLaunchExternalWinlator,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentCyan,
                                                contentColor = InkBg
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (externalRuntimeInstalled) "HAND OFF TO WINLATOR APK" else "GET WINLATOR NATIVE APK",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onExitSession,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("STOP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SessionDrawerTab.LOGS -> {
                    Surface(
                        color = PanelSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(runtimeLogs) { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.contains("[WARN]") -> AccentOrange
                                        line.contains("[GPU]") || line.contains("[EXEC]") -> AccentGreen
                                        else -> TextSecondary
                                    },
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                SessionDrawerTab.PROCESSES -> {
                    val processes = listOf(
                        Triple("wineserver", "PID 1042 • Wine 9.2 Server", "14.2 MB"),
                        Triple("services.exe", "PID 1058 • Windows Service Host", "8.6 MB"),
                        Triple("explorer.exe", "PID 1084 • X11 Desktop Shell (:0)", "24.1 MB"),
                        Triple(exeName, "PID 1120 • Box64 Dynarec [CPU ${container.cpuList}]", "312.4 MB")
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(processes) { (proc, desc, mem) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PanelSurface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(proc, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(desc, color = TextSecondary, fontSize = 9.sp)
                                }
                                Text(mem, color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                SessionDrawerTab.CONTROLS -> {
                    Surface(
                        color = PanelSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("INPUT PROFILE: ${game?.dinputMapperType ?: "Standard XInput + Mouse Look"}", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("• Touchpad Drag: Relative Win32 Mouse Look (DirectInput8 compatible)", color = TextSecondary, fontSize = 10.sp)
                            Text("• Left Overlay: WASD Movement + Crouch (C) + Jump (Space)", color = TextSecondary, fontSize = 10.sp)
                            Text("• Right Overlay: Fire (LMB), Aim/Scope (RMB), Reload (R), Tactical Map (M)", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
