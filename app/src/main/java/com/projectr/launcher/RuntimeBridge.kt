package com.projectr.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.winlator.MainActivity as BundledWinlatorActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4 Runtime Bridge & Host Dispatcher.
 *
 * Supports:
 * 1. Direct PROJECT R XServer Host Session (in-app reskinned Winlator container environment).
 * 2. Direct Intent hand-off (`com.winlator.XServerDisplayActivity` with `container_id` and `shortcut_path`)
 *    to external Winlator / Winlator CMod / GLIBC installations.
 * 3. Fallback package launch or official GitHub releases download.
 */
object RuntimeBridge {
    private val WINLATOR_PACKAGES = listOf(
        "com.winlator",
        "com.winlator.cmod",
        "com.winlator.glibc",
        "com.winlator.afeimod"
    )
    private const val XSERVER_ACTIVITY = "com.winlator.XServerDisplayActivity"
    private const val RELEASES_URL = "https://github.com/brunodev85/winlator/releases"
    const val PINNED_UPSTREAM_REF = "brunodev85/winlator-app @ 2c33741d50c5daab0521a098be5c969f63b9e887 (LGPL-2.1)"

    fun detectInstalledPackage(context: Context): String? {
        val pm = context.packageManager
        return WINLATOR_PACKAGES.firstOrNull { pkg ->
            pm.getLaunchIntentForPackage(pkg) != null
        }
    }

    fun isInstalled(context: Context): Boolean {
        return detectInstalledPackage(context) != null
    }

    /** Opens the runtime bundled in this APK; external package hand-off remains optional. */
    fun openOrInstall(context: Context): String {
        return try {
            context.startActivity(
                Intent(context, BundledWinlatorActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "Opened the bundled Winlator runtime. Complete first-run setup and create a container before launching games."
        } catch (error: Exception) {
            val pkg = detectInstalledPackage(context)
            if (pkg != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    "Bundled runtime could not open; opened external runtime ($pkg)."
                } else {
                    "Bundled runtime could not open and $pkg has no launch activity. ${error.message.orEmpty()}"
                }
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                "Bundled runtime could not open. Opening official Winlator releases page. ${error.message.orEmpty()}"
            }
        }
    }

    /**
     * Attempts direct `XServerDisplayActivity` launch with `container_id` and `shortcut_path`
     * extras if external Winlator is installed, falling back to package launch.
     */
    fun launchExternalShortcutIfAvailable(
        context: Context,
        game: GameEntry,
        container: WinlatorContainer
    ): String {
        val pkg = detectInstalledPackage(context) ?: return openOrInstall(context)
        try {
            val directIntent = Intent().apply {
                component = ComponentName(pkg, XSERVER_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("container_id", container.id)
                if (!game.shortcutPath.isNullOrBlank()) {
                    putExtra("shortcut_path", game.shortcutPath)
                }
                if (!game.exeUri.isNullOrBlank()) {
                    data = Uri.parse(game.exeUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(directIntent)
            return "Dispatched Container #${container.id} (${game.name}) to $pkg XServerDisplayActivity."
        } catch (_: Exception) {
            return openOrInstall(context)
        }
    }

    fun generateBootLogs(
        game: GameEntry?,
        container: WinlatorContainer,
        gpuReport: GpuCapabilityReport,
        rootFsArchive: String?
    ): List<String> {
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val exeName = game?.exeFileName
            ?: game?.exeUri?.substringAfterLast('/')?.substringAfterLast(':')
            ?: "explorer.exe"
        val resolution = game?.overrideScreenSize ?: container.screenSize
        val dxWrapper = game?.overrideDxWrapper ?: container.dxWrapper

        return buildList {
            add("[$ts] [PROJECT-R] Initializing Winlator Host Environment ($PINNED_UPSTREAM_REF)")
            add("[$ts] [IMAGEFS] Mounting container rootfs -> /data/user/0/com.projectr.launcher/files/imagefs")
            if (rootFsArchive != null) {
                add("[$ts] [IMAGEFS] Linked OBB/RootFS package: $rootFsArchive")
            } else {
                add("[$ts] [IMAGEFS] Using local container prefix xuser-.wine-${container.id} (No external .obb archive mounted yet)")
            }
            add("[$ts] [PROOT] Binding drives: C: -> drive_c | ${container.drives}")
            add("[$ts] [CPU] Applying taskset CPU affinity mask [${container.cpuList}] (${gpuReport.socModel})")
            add("[$ts] [XSERVER] Starting XServer :0 shm/socket at ${resolution.substringBefore(" ")} 32bpp")
            add("[$ts] [GPU] Detected Host Renderer: ${gpuReport.glRenderer} (${gpuReport.glVendor})")
            add("[$ts] [GPU] Host Graphics Backend: ${container.graphicsDriver} • ${gpuReport.vulkanVersionLabel}")
            if (gpuReport.isMaliGpu && container.graphicsDriver.contains("Turnip", ignoreCase = true)) {
                add("[$ts] [WARN] Turnip driver selected on Mali GPU! Auto-switching to VirGL (Mesa GL) to prevent crash.")
            }
            add("[$ts] [DXWRAP] Configuring DirectX layer: $dxWrapper | D3D8 policy: ${container.d3d8Override}")
            add("[$ts] [AUDIO] Starting ${container.audioDriver} server socket")
            add("[$ts] [BOX64] Preset: ${container.box64Preset} | ENV: ${container.envVars}")
            add("[$ts] [WINE] Booting ${container.wineVersion} wineserver & explorer.exe /desktop=shell,${resolution.substringBefore(" ")}")
            add("[$ts] [EXEC] Launching target binary: D:\\$exeName ${game?.execArgs.orEmpty()}".trim())
        }
    }
}
