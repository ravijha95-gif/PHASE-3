package com.projectr.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Phase 3 bridge to the separately installed official Winlator application.
 *
 * This is deliberately an external-app handoff, not an in-process runtime.
 * No undocumented Winlator activity extras or private APIs are assumed.
 */
object RuntimeBridge {
    private const val WINLATOR_PACKAGE = "com.winlator"
    private const val RELEASES_URL = "https://github.com/brunodev85/winlator/releases"

    fun isInstalled(context: Context): Boolean {
        return context.packageManager.getLaunchIntentForPackage(WINLATOR_PACKAGE) != null
    }

    fun openOrInstall(context: Context): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(WINLATOR_PACKAGE)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            "Winlator opened. Select the game inside Winlator; direct hand-off is not available yet."
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
            context.startActivity(browserIntent)
            "Winlator is not installed. Opening the official releases page."
        }
    }
}
