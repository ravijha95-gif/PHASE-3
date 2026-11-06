package com.projectr.launcher

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val Ink = Color(0xFF080B12)
private val Surface1 = Color(0xFF111827)
private val Surface2 = Color(0xFF182235)
private val Cyan = Color(0xFF65E6F2)
private val Violet = Color(0xFF9B8CFF)
private val TextMain = Color(0xFFF4F7FB)
private val TextMuted = Color(0xFF9AA9BC)
private val Green = Color(0xFF8BE28B)
private val Orange = Color(0xFFFFB86B)

@Composable
fun ContainersScreen(
    containers: List<WinlatorContainer>,
    gpuReport: GpuCapabilityReport,
    onCreateContainer: (WinlatorContainer) -> Unit,
    onUpdateContainer: (WinlatorContainer) -> Unit,
    onDeleteContainer: (WinlatorContainer) -> Unit,
    onBootContainer: (WinlatorContainer) -> Unit
) {
    var editingContainer by remember { mutableStateOf<WinlatorContainer?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("WINLATOR CONTAINERS", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${containers.size} isolated Wine x86_64 environment${if (containers.size == 1) "" else "s"}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        val nextId = (containers.maxOfOrNull { it.id } ?: 0) + 1
                        val fresh = WinlatorPresets.createDefaultMaliK50iContainer(nextId)
                            .copy(name = "Container #$nextId")
                        onCreateContainer(fresh)
                        editingContainer = fresh
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.testTag("create_container_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // Quick 1-Click Preset Banner for Redmi K50i / IGI 2
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1B2E4B), Color(0xFF231F3E))))
                    .border(1.dp, Cyan.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "HARDWARE PRESET • ${gpuReport.glRenderer.uppercase()}",
                        color = Cyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                }
                Text(
                    text = "IGI 2: Covert Strike & Direct3D 8 Mali-G610 Profile",
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Pre-tuned with ${gpuReport.recommendedDriver}, ${gpuReport.recommendedDxWrapper}, dgVoodoo2 D3D8 bridge, and Cortex-A78 CPU 4–7 affinity.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                OutlinedButton(
                    onClick = {
                        val nextId = (containers.maxOfOrNull { it.id } ?: 0) + 1
                        val preset = WinlatorPresets.createDefaultMaliK50iContainer(nextId)
                            .copy(name = "IGI 2 • Mali-G610 Direct3D 8 (#$nextId)")
                        onCreateContainer(preset)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADD IGI 2 MALI-G610 CONTAINER", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        items(containers, key = { it.id }) { container ->
            val turnipConflict = !gpuReport.isAdrenoGpu && container.graphicsDriver.contains("Turnip", ignoreCase = true)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Surface1)
                    .border(
                        1.dp,
                        if (turnipConflict) Orange.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(17.dp)
                    )
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = container.name,
                                color = TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Container #${container.id} • ${container.wineVersion} • ${container.screenSize}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (turnipConflict) Orange.copy(alpha = 0.15f) else Green.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (turnipConflict) "DRIVER WARN" else "READY",
                            color = if (turnipConflict) Orange else Green,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = "GPU: ${container.graphicsDriver} • DX: ${container.dxWrapper}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Box64: ${container.box64Preset} • Audio: ${container.audioDriver} • CPU [${container.cpuList}]",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (turnipConflict && gpuReport.turnipBlockedReason != null) {
                    Text(
                        text = gpuReport.turnipBlockedReason,
                        color = Orange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBootContainer(container) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("BOOT DESKTOP", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(
                        onClick = { editingContainer = container },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Container", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (containers.size > 1) {
                        IconButton(onClick = { onDeleteContainer(container) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Container", tint = Color(0xFFFF8C8C))
                        }
                    }
                }
            }
        }
    }

    if (editingContainer != null) {
        ContainerEditorDialog(
            initial = editingContainer!!,
            gpuReport = gpuReport,
            onDismiss = { editingContainer = null },
            onSave = { updated ->
                onUpdateContainer(updated)
                editingContainer = null
            }
        )
    }
}

@Composable
fun ContainerEditorDialog(
    initial: WinlatorContainer,
    gpuReport: GpuCapabilityReport,
    onDismiss: () -> Unit,
    onSave: (WinlatorContainer) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var screenSize by remember { mutableStateOf(initial.screenSize) }
    var graphicsDriver by remember { mutableStateOf(initial.graphicsDriver) }
    var dxWrapper by remember { mutableStateOf(initial.dxWrapper) }
    var audioDriver by remember { mutableStateOf(initial.audioDriver) }
    var box64Preset by remember { mutableStateOf(initial.box64Preset) }
    var cpuList by remember { mutableStateOf(initial.cpuList) }
    var envVars by remember { mutableStateOf(initial.envVars) }
    var showFps by remember { mutableStateOf(initial.showFps) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Surface1,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "EDIT CONTAINER #${initial.id}",
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Container Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OptionSelectorSection(
                    title = "SCREEN RESOLUTION",
                    options = WinlatorPresets.SCREEN_SIZES,
                    selected = screenSize,
                    onSelect = { screenSize = it }
                )

                OptionSelectorSection(
                    title = "GRAPHICS DRIVER (HOST: ${gpuReport.glRenderer})",
                    options = WinlatorPresets.GRAPHICS_DRIVERS,
                    selected = graphicsDriver,
                    onSelect = { graphicsDriver = it }
                )

                if (!gpuReport.isAdrenoGpu && graphicsDriver.contains("Turnip", ignoreCase = true)) {
                    Text(
                        text = gpuReport.turnipBlockedReason ?: "Turnip is for Adreno GPUs only.",
                        color = Orange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OptionSelectorSection(
                    title = "DIRECTX / D3D8 WRAPPER",
                    options = WinlatorPresets.DX_WRAPPERS,
                    selected = dxWrapper,
                    onSelect = { dxWrapper = it }
                )

                OptionSelectorSection(
                    title = "BOX64 DYNAREC PRESET",
                    options = WinlatorPresets.BOX64_PRESETS,
                    selected = box64Preset,
                    onSelect = { box64Preset = it }
                )

                OptionSelectorSection(
                    title = "AUDIO BACKEND",
                    options = WinlatorPresets.AUDIO_DRIVERS,
                    selected = audioDriver,
                    onSelect = { audioDriver = it }
                )

                OptionSelectorSection(
                    title = "CPU AFFINITY MASK",
                    options = WinlatorPresets.CPU_TOPOLOGIES.map { it.first },
                    selected = cpuList,
                    optionLabel = { code ->
                        WinlatorPresets.CPU_TOPOLOGIES.firstOrNull { it.first == code }?.second ?: code
                    },
                    onSelect = { cpuList = it }
                )

                OutlinedTextField(
                    value = envVars,
                    onValueChange = { envVars = it },
                    label = { Text("Environment Variables") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show XServer FPS / GPU HUD", color = TextMain, fontSize = 12.sp)
                    Switch(checked = showFps, onCheckedChange = { showFps = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name.ifBlank { "Container #${initial.id}" },
                                    screenSize = screenSize,
                                    graphicsDriver = graphicsDriver,
                                    dxWrapper = dxWrapper,
                                    audioDriver = audioDriver,
                                    box64Preset = box64Preset,
                                    cpuList = cpuList,
                                    envVars = envVars,
                                    showFps = showFps
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun GameShortcutConfigDialog(
    game: GameEntry,
    containers: List<WinlatorContainer>,
    onDismiss: () -> Unit,
    onSave: (GameEntry) -> Unit
) {
    var selectedContainerId by remember { mutableIntStateOf(game.containerId) }
    var execArgs by remember { mutableStateOf(game.execArgs) }
    var overrideResolution by remember { mutableStateOf(game.overrideScreenSize ?: "") }
    var overrideDx by remember { mutableStateOf(game.overrideDxWrapper ?: "") }
    var dinputMode by remember { mutableStateOf(game.dinputMapperType) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Surface1,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "WINLATOR SHORTCUT PROFILE",
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
                Text(game.name, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OptionSelectorSection(
                    title = "TARGET CONTAINER",
                    options = containers.map { it.id.toString() },
                    selected = selectedContainerId.toString(),
                    optionLabel = { idStr ->
                        val c = containers.firstOrNull { it.id.toString() == idStr }
                        if (c != null) "#${c.id} • ${c.name} (${c.graphicsDriver.substringBefore(" ")})" else "Container #$idStr"
                    },
                    onSelect = { selectedContainerId = it.toIntOrNull() ?: 1 }
                )

                OutlinedTextField(
                    value = execArgs,
                    onValueChange = { execArgs = it },
                    label = { Text("Executable Arguments (e.g. -windowed -novideo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OptionSelectorSection(
                    title = "SHORTCUT RESOLUTION OVERRIDE",
                    options = listOf("") + WinlatorPresets.SCREEN_SIZES,
                    selected = overrideResolution,
                    optionLabel = { if (it.isBlank()) "Use Container Default" else it },
                    onSelect = { overrideResolution = it }
                )

                OptionSelectorSection(
                    title = "SHORTCUT DIRECTX OVERRIDE",
                    options = listOf("") + WinlatorPresets.DX_WRAPPERS,
                    selected = overrideDx,
                    optionLabel = { if (it.isBlank()) "Use Container Default" else it },
                    onSelect = { overrideDx = it }
                )

                OptionSelectorSection(
                    title = "INPUT / DIRECTINPUT8 PROFILE",
                    options = listOf(
                        "Standard XInput + Mouse Look",
                        "IGI 2 Tactical FPS (Mouse + WASD Overlay)",
                        "Classic DirectInput8 Joystick"
                    ),
                    selected = dinputMode,
                    onSelect = { dinputMode = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            onSave(
                                game.copy(
                                    containerId = selectedContainerId,
                                    execArgs = execArgs.trim(),
                                    overrideScreenSize = overrideResolution.takeIf { it.isNotBlank() },
                                    overrideDxWrapper = overrideDx.takeIf { it.isNotBlank() },
                                    dinputMapperType = dinputMode
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                    ) {
                        Text("SAVE SHORTCUT", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSelectorSection(
    title: String,
    options: List<String>,
    selected: String,
    optionLabel: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        options.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Cyan.copy(alpha = 0.14f) else Surface2)
                    .border(
                        1.dp,
                        if (isSelected) Cyan else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = optionLabel(option),
                    color = if (isSelected) Cyan else TextMain,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun OpenSourceNoticesDialog(onDismiss: () -> Unit) {
    val notices = listOf(
        Triple("Winlator (brunodev85/winlator-app)", "GNU Lesser General Public License v2.1 (LGPL-2.1)", "Upstream Android container/shortcut architecture, XEnvironment host structure, and XServer display pipeline."),
        Triple("Wine Project (winehq.org)", "GNU Lesser General Public License v2.1+ (LGPL-2.1+)", "Windows API compatibility layer (ntdll, kernel32, user32, gdi32, dinput8, wined3d)."),
        Triple("Box86 & Box64 (ptitSeb)", "MIT License", "Linux Userspace x86 and x86_64 Emulator with ARM64 Dynamic Recompiler (Dynarec)."),
        Triple("Mesa 3D / VirGL / Zink", "MIT / Khronos Free Software License", "OpenGL and Vulkan userland drivers including VirGL renderer for Mali GPUs and Turnip for Adreno."),
        Triple("DXVK (doitsujin) & VKD3D-Proton", "zlib/libpng License & LGPL-2.1", "Vulkan-based translation layers for Direct3D 9, 10, 11, and Direct3D 12."),
        Triple("PRoot (proot-me)", "GNU General Public License v2.0 (GPL-2.0)", "User-space chroot, mount --bind, and binfmt_misc implementation for unprivileged Android containers.")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Surface1,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("OPEN-SOURCE & RUNTIME LICENCES", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(
                    "Pinned upstream baseline: ${RuntimeBridge.PINNED_UPSTREAM_REF}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notices) { (project, license, desc) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Surface2)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(project, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(license, color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
