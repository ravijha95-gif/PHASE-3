# PROJECT R — Phase 4 (Winlator Host & Reskinned Container Environment)

PROJECT R is a personal Android launcher and reskinned Winlator host environment for Windows PC games.

## What's New in Phase 4 (`v0.4.0`)

- **Phase 3 Discrepancies Resolved:**
  - Dual-syncs both `project_r_v2` and `project_r` SharedPreferences stores so Phase 2 and Phase 3 game libraries are seamlessly preserved across upgrades.
  - Dynamic status indicators across `Home`, `Library`, and `Game Details` reflecting linked `.exe` status, bound Container ID, and active GPU driver.
  - Proper hardware/gesture `BackHandler` navigation across all detail and XServer session screens.
- **Winlator Container & `.desktop` Shortcut Architecture:**
  - Create, configure, and persist isolated Wine x86_64 containers (`files/imagefs/home/xuser/containers/xuser-.wine-<id>/.container`).
  - Automatic generation of Winlator-compatible `.desktop` shortcut files (`files/imagefs/home/xuser/Desktop/<Game>.desktop`) with per-game resolution, DirectX wrapper, launch arguments, and DirectInput8 profile overrides.
- **Real-Time Device GPU & Vulkan Capability Inspector (`GpuCapabilityDetector`):**
  - Queries live `EGL14` / `GLES20` (`GL_RENDERER`, `GL_VENDOR`, `GL_VERSION`), SoC model (`Build.SOC_MODEL`), and Android Vulkan hardware features at startup.
  - Detects **Mali GPUs** (including **Mali-G610 MC6** on the **Redmi K50i / Dimensity 8100**) and guards against selecting Adreno-only Turnip drivers, recommending **VirGL (Mesa OpenGL)** or **Vortek (System Vulkan)** + **WineD3D / dgVoodoo2 (Direct3D 8)**.
- **1-Click IGI 2: Covert Strike (Redmi K50i / Mali-G610) Preset:**
  - Pre-configured container and game shortcut profile tuned for Direct3D 8 rendering, Cortex-A78 performance core affinity (`CPU 4–7`), and `BOX64_DYNAREC_SAFEFLAGS=1`.
- **Reskinned XServer Session Host & External APK Hand-off:**
  - Built-in reskinned Winlator XServer `:0` session view with interactive X11 mouse touchpad, customizable IGI 2 WASD + Action overlay, Wine Task Manager, `dxdiag` inspector, and live boot/execution logs.
  - Direct `com.winlator.XServerDisplayActivity` intent hand-off (`container_id` + `shortcut_path`) when an external Winlator APK (`com.winlator`, `com.winlator.cmod`, `com.winlator.glibc`) is installed.
- **Third-Party & Open-Source Licence Compliance:**
  - Built-in Open-Source Notices modal in Settings documenting `Winlator` (LGPL-2.1), `Wine` (LGPL-2.1+), `Box86/Box64` (MIT), `Mesa/VirGL` (MIT), `DXVK` (zlib), `VKD3D` (LGPL-2.1), and `PRoot` (GPL-2.0).

## Build

```bash
gradle --no-daemon assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
