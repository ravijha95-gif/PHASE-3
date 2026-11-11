package com.projectr.launcher

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build

data class GpuCapabilityReport(
    val glVendor: String,
    val glRenderer: String,
    val glVersion: String,
    val socModel: String,
    val board: String,
    val hardware: String,
    val primaryAbi: String,
    val vulkanSupported: Boolean,
    val vulkanVersionLabel: String,
    val isMaliGpu: Boolean,
    val isAdrenoGpu: Boolean,
    val isRedmiK50iClass: Boolean,
    val recommendedDriver: String,
    val recommendedDxWrapper: String,
    val turnipBlockedReason: String?
)

object GpuCapabilityDetector {

    fun inspect(context: Context): GpuCapabilityReport {
        var vendor = "Unknown Vendor"
        var renderer = "Unknown GPU Renderer"
        var version = "OpenGL ES"

        try {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display != EGL14.EGL_NO_DISPLAY) {
                val vers = IntArray(2)
                if (EGL14.eglInitialize(display, vers, 0, vers, 1)) {
                    val configAttribs = intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                        EGL14.EGL_NONE
                    )
                    val configs = arrayOfNulls<EGLConfig>(1)
                    val numConfigs = IntArray(1)
                    EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)
                    if (numConfigs[0] > 0 && configs[0] != null) {
                        val surfaceAttribs = intArrayOf(
                            EGL14.EGL_WIDTH, 1,
                            EGL14.EGL_HEIGHT, 1,
                            EGL14.EGL_NONE
                        )
                        val surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
                        val ctxAttribs = intArrayOf(
                            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                            EGL14.EGL_NONE
                        )
                        val eglCtx = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                        if (EGL14.eglMakeCurrent(display, surface, surface, eglCtx)) {
                            GLES20.glGetString(GLES20.GL_VENDOR)?.let { vendor = it }
                            GLES20.glGetString(GLES20.GL_RENDERER)?.let { renderer = it }
                            GLES20.glGetString(GLES20.GL_VERSION)?.let { version = it }
                            EGL14.eglMakeCurrent(
                                display,
                                EGL14.EGL_NO_SURFACE,
                                EGL14.EGL_NO_SURFACE,
                                EGL14.EGL_NO_CONTEXT
                            )
                        }
                        EGL14.eglDestroySurface(display, surface)
                        EGL14.eglDestroyContext(display, eglCtx)
                    }
                    EGL14.eglTerminate(display)
                }
            }
        } catch (_: Exception) {
            // Fallback to system build tags if offscreen EGL query is restricted
        }

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        val pm = context.packageManager
        val hasVulkan = pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        val vulkanVersionInt = pm.systemAvailableFeatures
            .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?.version ?: 0

        val vulkanLabel = if (hasVulkan && vulkanVersionInt > 0) {
            val major = (vulkanVersionInt shr 22) and 0x3FF
            val minor = (vulkanVersionInt shr 12) and 0x3FF
            "Vulkan $major.$minor"
        } else if (hasVulkan) {
            "Vulkan 1.1+"
        } else {
            "OpenGL ES Only"
        }

        val combined = "$vendor $renderer $socModel ${Build.HARDWARE} ${Build.BOARD}".lowercase()
        val isMali = combined.contains("mali") || combined.contains("mt6895") || combined.contains("dimensity")
        val isAdreno = combined.contains("adreno") || combined.contains("qcom") || combined.contains("snapdragon")
        val isK50i = combined.contains("22041216i") || combined.contains("xaga") ||
            combined.contains("mt6895") || combined.contains("g610") ||
            Build.MODEL.contains("22041216", ignoreCase = true) ||
            Build.MODEL.contains("K50i", ignoreCase = true)

        val recommendedDriver = when {
            isMali -> "VirGL (Mesa OpenGL)"
            isAdreno -> "Turnip (Adreno Vulkan)"
            hasVulkan -> "Vortek (System Vulkan)"
            else -> "VirGL (Mesa OpenGL)"
        }

        val recommendedDxWrapper = when {
            isMali -> "WineD3D (OpenGL / D3D8-D3D11)"
            isAdreno -> "DXVK 1.10.3 + dgVoodoo2 (D3D8)"
            else -> "WineD3D (OpenGL / D3D8-D3D11)"
        }

        val turnipBlocked = if (!isAdreno) {
            "Turnip requires a Qualcomm Adreno GPU. Detected $renderer ($socModel) — use VirGL or Vortek instead."
        } else {
            null
        }

        return GpuCapabilityReport(
            glVendor = vendor,
            glRenderer = renderer,
            glVersion = version,
            socModel = socModel,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            vulkanSupported = hasVulkan,
            vulkanVersionLabel = vulkanLabel,
            isMaliGpu = isMali,
            isAdrenoGpu = isAdreno,
            isRedmiK50iClass = isK50i,
            recommendedDriver = recommendedDriver,
            recommendedDxWrapper = recommendedDxWrapper,
            turnipBlockedReason = turnipBlocked
        )
    }
}
