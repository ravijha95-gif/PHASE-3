# PROJECT R — Phase 3 Preview

PROJECT R is a personal Android launcher for a Windows-PC-game compatibility environment.

## Phase 3 features

- Custom PROJECT R target / gaming launcher icon (adaptive and legacy densities).
- Premium dark gaming interface retained from Phase 2.
- Game library and game profiles.
- Import game folders using Android's Storage Access Framework.
- Select and persist a Windows executable URI for each imported game.
- Detect the official Winlator Android package (`com.winlator`).
- Open installed Winlator, or open the official GitHub releases page if it is not installed.
- Engine Center shows the runtime detection state.
- GitHub Actions supports Gradle files at repository root or under `project_r/`.

## Important runtime limitation

This milestone adds a **runtime hand-off bridge**, not a bundled/embedded Wine + Box86/Box64 environment. PROJECT R remembers the selected `.exe` document URI, but it cannot yet send that file directly to a specific Winlator container/shortcut. The public Winlator source does not document a stable public intent/API for this hand-off, so the app opens Winlator and the user must select/configure the game inside Winlator.

The next milestone is to fork/port the actual upstream Winlator app source as the runtime host and integrate the PROJECT R UI into it. This requires its native libraries, root filesystem, PRoot setup, runtime assets, build system and third-party licence compliance; it is not a one-file Gradle dependency.

## Upstream source references

- Winlator main project: https://github.com/brunodev85/winlator
- Winlator Android app source: https://github.com/brunodev85/winlator-app
- Current app source declares application ID `com.winlator` and LGPL-2.1.
- Main README credits Wine, Box86/Box64, Mesa, DXVK, VKD3D and other third-party projects.

Pin an exact upstream commit and audit each dependency's licence before integrating or distributing the runtime.

## Redmi K50i target

The device uses a MediaTek Dimensity 8100 and Mali GPU. Do not select Adreno-only Turnip drivers. The eventual runtime port must detect Vulkan/OpenGL capabilities and use a backend compatible with the actual Mali driver. Performance and IGI 2 compatibility must be tested on-device.

## Build

Requirements: JDK 17, Android SDK 35, Android Gradle Plugin 8.7.3, Kotlin 2.0.21.

```bash
gradle --no-daemon assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

The workflow detects a Gradle project at either repository root or `project_r/`. Upload the contents of the `project_r` folder to your repository (or keep the folder nested), then run **Actions → Build PROJECT R APK**.

## Version

- Application ID: `com.projectr.launcher` (unchanged)
- Version code: 3
- Version name: 0.3.0

## Licensing

The original PROJECT R launcher files are for this personal project. No Windows game files or third-party runtime binaries are included. If upstream Winlator code or runtime assets are added, retain all applicable source notices, licence texts and obligations. Winlator app source is LGPL-2.1; Wine, Box86/Box64, Mesa, DXVK, VKD3D and other components have their own licences.
