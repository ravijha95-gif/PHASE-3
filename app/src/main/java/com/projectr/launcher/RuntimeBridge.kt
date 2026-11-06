package com.projectr.launcher

import android.content.Context
import android.content.Intent
import com.winlator.MainActivity as WinlatorMainActivity

/**
 * In-process entry point to the pinned Winlator runtime host bundled in this APK.
 * The selected PROJECT R executable is not yet converted into a Winlator shortcut;
 * users still configure/import it from Winlator's own container UI.
 */
object RuntimeBridge {
    fun isInstalled(@Suppress("UNUSED_PARAMETER") context: Context): Boolean = true

    fun openOrInstall(context: Context): String {
        return try {
            context.startActivity(
                Intent(context, WinlatorMainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "PROJECT R runtime opened. Create a container, then select or install your game inside Winlator."
        } catch (error: Exception) {
            "Could not open the bundled Winlator runtime: ${error.message ?: "unknown error"}"
        }
    }
}
