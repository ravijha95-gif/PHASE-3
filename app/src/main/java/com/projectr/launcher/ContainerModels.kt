package com.projectr.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WinlatorContainer(
    val id: Int,
    val name: String,
    val screenSize: String = "1280x720",
    val graphicsDriver: String = "VirGL (Mesa OpenGL)",
    val dxWrapper: String = "WineD3D (OpenGL / D3D8-D3D11)",
    val d3d8Override: String = "dgVoodoo2 + WineD3D (IGI 2 Direct3D 8)",
    val audioDriver: String = "ALSA (Low Latency)",
    val wineVersion: String = "wine-9.2-x86_64",
    val box64Preset: String = "Compatibility (SafeFlags)",
    val cpuList: String = "4,5,6,7", // Dimensity 8100 Cortex-A78 performance cores default
    val envVars: String = "ZINK_DESCRIPTORS=lazy MESA_GL_VERSION_OVERRIDE=4.3COMPAT BOX64_DYNAREC_SAFEFLAGS=1 BOX64_DYNAREC_BIGBLOCK=1 WINEESYNC=1",
    val drives: String = "D:/storage/emulated/0/Download;E:/storage/emulated/0/Games",
    val showFps: Boolean = true,
    val startupSelection: String = "Essential Services Only",
    val desktopTheme: String = "PROJECT R Dark Carbon",
    val createdDate: String = currentFormattedDate()
)

data class GameEntry(
    val name: String,
    val uri: String,
    val added: String,
    val exeUri: String? = null,
    val exeFileName: String? = null,
    val containerId: Int = 1,
    val execArgs: String = "",
    val overrideScreenSize: String? = null,
    val overrideDxWrapper: String? = null,
    val dinputMapperType: String = "Standard XInput + Mouse Look",
    val shortcutPath: String? = null,
    val lastLaunched: String? = null
)

object WinlatorPresets {
    val SCREEN_SIZES = listOf(
        "800x600 (4:3 Classic)",
        "1024x768 (4:3 XGA)",
        "1280x720 (16:9 HD)",
        "1366x768 (16:9 WXGA)",
        "1600x900 (16:9 HD+)",
        "1920x1080 (16:9 FHD)"
    )

    val GRAPHICS_DRIVERS = listOf(
        "VirGL (Mesa OpenGL)",
        "Vortek (System Vulkan)",
        "LLVMpipe (Software CPU)",
        "Turnip (Adreno Vulkan)"
    )

    val DX_WRAPPERS = listOf(
        "WineD3D (OpenGL / D3D8-D3D11)",
        "dgVoodoo2 (Direct3D 8 -> D3D11)",
        "DXVK 1.10.3 (Vulkan D3D9-11)",
        "VKD3D 2.12 (Direct3D 12)",
        "CNC DDraw (Classic 2D/DirectDraw)"
    )

    val AUDIO_DRIVERS = listOf(
        "ALSA (Low Latency)",
        "PulseAudio (Compatibility)"
    )

    val BOX64_PRESETS = listOf(
        "Stability (Conservative)",
        "Compatibility (SafeFlags)",
        "Intermediate (Balanced)",
        "Performance (FastMath + BigBlock)",
        "IGI 2 Covert Strike Tuned"
    )

    val CPU_TOPOLOGIES = listOf(
        "4,5,6,7" to "Performance Cores (CPU 4–7 • Cortex-A78)",
        "0,1,2,3,4,5,6,7" to "All 8 Cores (CPU 0–7 • Big.LITTLE)",
        "2,3,4,5,6,7" to "6 Hybrid Cores (CPU 2–7)",
        "6,7" to "Dual Prime Cores (CPU 6–7 • Classic Single-Thread)"
    )

    fun createDefaultMaliK50iContainer(id: Int = 1): WinlatorContainer {
        return WinlatorContainer(
            id = id,
            name = "Mali-G610 • Direct3D 8/9 Host",
            screenSize = "1280x720 (16:9 HD)",
            graphicsDriver = "VirGL (Mesa OpenGL)",
            dxWrapper = "WineD3D (OpenGL / D3D8-D3D11)",
            d3d8Override = "dgVoodoo2 + WineD3D (IGI 2 Direct3D 8)",
            audioDriver = "ALSA (Low Latency)",
            wineVersion = "wine-9.2-x86_64",
            box64Preset = "IGI 2 Covert Strike Tuned",
            cpuList = "4,5,6,7",
            envVars = "MESA_GL_VERSION_OVERRIDE=4.3COMPAT MESA_GLSL_VERSION_OVERRIDE=430 BOX64_DYNAREC_SAFEFLAGS=1 BOX64_DYNAREC_BIGBLOCK=1 BOX64_DYNAREC_STRONGMEM=1 WINEESYNC=1",
            drives = "D:/storage/emulated/0/Download;E:/storage/emulated/0/Games",
            showFps = true,
            startupSelection = "Essential Services Only",
            desktopTheme = "PROJECT R Dark Carbon"
        )
    }

    fun createVortekVulkanContainer(id: Int): WinlatorContainer {
        return WinlatorContainer(
            id = id,
            name = "Vortek Vulkan • DXVK Container",
            screenSize = "1280x720 (16:9 HD)",
            graphicsDriver = "Vortek (System Vulkan)",
            dxWrapper = "DXVK 1.10.3 (Vulkan D3D9-11)",
            d3d8Override = "d3d8to9 + DXVK 1.10.3",
            audioDriver = "PulseAudio (Compatibility)",
            wineVersion = "wine-9.2-x86_64",
            box64Preset = "Intermediate (Balanced)",
            cpuList = "0,1,2,3,4,5,6,7",
            envVars = "DXVK_ASYNC=1 BOX64_DYNAREC_FASTNAN=1 BOX64_DYNAREC_BIGBLOCK=2 WINEESYNC=1",
            drives = "D:/storage/emulated/0/Download;E:/storage/emulated/0/Games",
            showFps = true,
            startupSelection = "Normal Windows Services",
            desktopTheme = "PROJECT R Dark Carbon"
        )
    }
}

class ProjectRRepository(private val context: Context) {
    // Syncs both Phase 2 ("project_r_v2") and Phase 3 ("project_r") preference stores
    private val prefsV3 = context.getSharedPreferences("project_r", Context.MODE_PRIVATE)
    private val prefsV2 = context.getSharedPreferences("project_r_v2", Context.MODE_PRIVATE)

    val imageFsRoot: File = File(context.filesDir, "imagefs")
    val containersRoot: File = File(imageFsRoot, "home/xuser/containers")
    val shortcutsRoot: File = File(imageFsRoot, "home/xuser/Desktop")

    init {
        ensureImageFsStructure()
    }

    fun ensureImageFsStructure() {
        listOf(
            File(imageFsRoot, "usr/bin"),
            File(imageFsRoot, "usr/lib"),
            File(imageFsRoot, "opt/wine/bin"),
            File(imageFsRoot, "tmp/.X11-unix"),
            File(imageFsRoot, "home/xuser/.wine/drive_c/windows/system32"),
            File(imageFsRoot, "home/xuser/.wine/dosdevices"),
            containersRoot,
            shortcutsRoot
        ).forEach { it.mkdirs() }
    }

    fun loadContainers(): List<WinlatorContainer> {
        val raw = prefsV3.getString("containers", null)
            ?: prefsV2.getString("containers", null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                WinlatorPresets.createDefaultMaliK50iContainer(1),
                WinlatorPresets.createVortekVulkanContainer(2)
            )
            saveContainers(defaults)
            return defaults
        }
        return try {
            val array = JSONArray(raw)
            val parsed = (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                WinlatorContainer(
                    id = obj.optInt("id", i + 1),
                    name = obj.optString("name", "Container #${i + 1}"),
                    screenSize = obj.optString("screenSize", "1280x720 (16:9 HD)"),
                    graphicsDriver = obj.optString("graphicsDriver", "VirGL (Mesa OpenGL)"),
                    dxWrapper = obj.optString("dxWrapper", "WineD3D (OpenGL / D3D8-D3D11)"),
                    d3d8Override = obj.optString("d3d8Override", "dgVoodoo2 + WineD3D (IGI 2 Direct3D 8)"),
                    audioDriver = obj.optString("audioDriver", "ALSA (Low Latency)"),
                    wineVersion = obj.optString("wineVersion", "wine-9.2-x86_64"),
                    box64Preset = obj.optString("box64Preset", "Compatibility (SafeFlags)"),
                    cpuList = obj.optString("cpuList", "4,5,6,7"),
                    envVars = obj.optString("envVars", "MESA_GL_VERSION_OVERRIDE=4.3COMPAT BOX64_DYNAREC_SAFEFLAGS=1"),
                    drives = obj.optString("drives", "D:/storage/emulated/0/Download;E:/storage/emulated/0/Games"),
                    showFps = obj.optBoolean("showFps", true),
                    startupSelection = obj.optString("startupSelection", "Essential Services Only"),
                    desktopTheme = obj.optString("desktopTheme", "PROJECT R Dark Carbon"),
                    createdDate = obj.optString("createdDate", currentFormattedDate())
                )
            }
            if (parsed.isEmpty()) listOf(WinlatorPresets.createDefaultMaliK50iContainer(1)) else parsed
        } catch (_: Exception) {
            listOf(WinlatorPresets.createDefaultMaliK50iContainer(1))
        }
    }

    fun saveContainers(containers: List<WinlatorContainer>) {
        val array = JSONArray()
        containers.forEach { c ->
            val obj = JSONObject()
                .put("id", c.id)
                .put("name", c.name)
                .put("screenSize", c.screenSize)
                .put("graphicsDriver", c.graphicsDriver)
                .put("dxWrapper", c.dxWrapper)
                .put("d3d8Override", c.d3d8Override)
                .put("audioDriver", c.audioDriver)
                .put("wineVersion", c.wineVersion)
                .put("box64Preset", c.box64Preset)
                .put("cpuList", c.cpuList)
                .put("envVars", c.envVars)
                .put("drives", c.drives)
                .put("showFps", c.showFps)
                .put("startupSelection", c.startupSelection)
                .put("desktopTheme", c.desktopTheme)
                .put("createdDate", c.createdDate)
            array.put(obj)

            // Also write Winlator-compatible .container JSON file on disk
            try {
                val cDir = File(containersRoot, "xuser-.wine-${c.id}")
                File(cDir, "drive_c/Program Files").mkdirs()
                File(cDir, "dosdevices").mkdirs()
                File(cDir, ".container").writeText(obj.toString(2))
            } catch (_: Exception) { }
        }
        val json = array.toString()
        prefsV3.edit().putString("containers", json).apply()
        prefsV2.edit().putString("containers", json).apply()
    }

    fun loadGames(): List<GameEntry> {
        val rawV3 = prefsV3.getString("games", null)
        val rawV2 = prefsV2.getString("games", null)
        val mergedByUri = linkedMapOf<String, GameEntry>()

        listOfNotNull(rawV2, rawV3).forEach { raw ->
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uri = obj.optString("uri", "")
                    if (uri.isNotBlank()) {
                        val exeUri = obj.optString("exeUri").takeIf { it.isNotBlank() }
                        val exeFileName = obj.optString("exeFileName").takeIf { it.isNotBlank() }
                            ?: exeUri?.substringAfterLast('/')?.substringAfterLast(':')
                        val entry = GameEntry(
                            name = obj.optString("name", "Imported game"),
                            uri = uri,
                            added = obj.optString("added", "Imported"),
                            exeUri = exeUri,
                            exeFileName = exeFileName,
                            containerId = obj.optInt("containerId", 1),
                            execArgs = obj.optString("execArgs", ""),
                            overrideScreenSize = obj.optString("overrideScreenSize").takeIf { it.isNotBlank() },
                            overrideDxWrapper = obj.optString("overrideDxWrapper").takeIf { it.isNotBlank() },
                            dinputMapperType = obj.optString("dinputMapperType", "Standard XInput + Mouse Look"),
                            shortcutPath = obj.optString("shortcutPath").takeIf { it.isNotBlank() },
                            lastLaunched = obj.optString("lastLaunched").takeIf { it.isNotBlank() }
                        )
                        mergedByUri[uri] = entry
                    }
                }
            } catch (_: Exception) { }
        }
        val list = mergedByUri.values.toList()
        if (list.isNotEmpty()) {
            saveGames(list)
        }
        return list
    }

    fun saveGames(games: List<GameEntry>) {
        val array = JSONArray()
        games.forEach { g ->
            val shortcutFile = writeWinlatorDesktopShortcut(g)
            val obj = JSONObject()
                .put("name", g.name)
                .put("uri", g.uri)
                .put("added", g.added)
                .put("exeUri", g.exeUri ?: "")
                .put("exeFileName", g.exeFileName ?: "")
                .put("containerId", g.containerId)
                .put("execArgs", g.execArgs)
                .put("overrideScreenSize", g.overrideScreenSize ?: "")
                .put("overrideDxWrapper", g.overrideDxWrapper ?: "")
                .put("dinputMapperType", g.dinputMapperType)
                .put("shortcutPath", shortcutFile?.absolutePath ?: g.shortcutPath ?: "")
                .put("lastLaunched", g.lastLaunched ?: "")
            array.put(obj)
        }
        val json = array.toString()
        prefsV3.edit().putString("games", json).apply()
        prefsV2.edit().putString("games", json).apply()
    }

    fun writeWinlatorDesktopShortcut(game: GameEntry): File? {
        return try {
            shortcutsRoot.mkdirs()
            val safeName = game.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val file = File(shortcutsRoot, "$safeName.desktop")
            val exeName = game.exeFileName ?: game.exeUri?.substringAfterLast(':')?.substringAfterLast('/') ?: "game.exe"
            val content = buildString {
                appendLine("[Desktop Entry]")
                appendLine("Name=${game.name}")
                appendLine("Exec=env WINEPREFIX=\"/home/xuser/.wine-${game.containerId}\" wine \"D:\\\\${exeName}\" ${game.execArgs}".trim())
                appendLine("Type=Application")
                appendLine("StartupNotify=true")
                appendLine("Comment=Created by PROJECT R Phase 4 Winlator Host")
                appendLine("")
                appendLine("[Extra Data]")
                appendLine("container_id:${game.containerId}")
                appendLine("target_uri:${game.uri}")
                appendLine("exe_uri:${game.exeUri.orEmpty()}")
                if (!game.overrideScreenSize.isNullOrBlank()) {
                    appendLine("screenSize:${game.overrideScreenSize}")
                }
                if (!game.overrideDxWrapper.isNullOrBlank()) {
                    appendLine("dxwrapper:${game.overrideDxWrapper}")
                }
                appendLine("dinputMapperType:${game.dinputMapperType}")
            }
            file.writeText(content)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun getRootFsArchiveName(): String? {
        return prefsV3.getString("rootfs_archive_name", null)
            ?: prefsV2.getString("rootfs_archive_name", null)
    }

    fun setRootFsArchive(uriString: String, displayName: String) {
        prefsV3.edit()
            .putString("rootfs_archive_uri", uriString)
            .putString("rootfs_archive_name", displayName)
            .apply()
        prefsV2.edit()
            .putString("rootfs_archive_uri", uriString)
            .putString("rootfs_archive_name", displayName)
            .apply()
    }
}

fun currentFormattedDate(): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
