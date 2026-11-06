# PROJECT R

PROJECT R is an Android launcher being integrated with the open-source Winlator Windows compatibility runtime so users can run their own Windows PC games on supported Android devices.

## Current integration status

- Winlator app source is pinned as a Git submodule at `runtime/winlator-app` (`2c33741d50c5daab0521a098be5c969f63b9e887`).
- The Android build is wired to include Winlator's Java source, resources, runtime assets, arm64 native libraries, and CMake project in the PROJECT R APK.
- PROJECT R keeps its existing application ID (`com.projectr.launcher`) and opens Winlator's in-process container UI from Engine / game details.
- Winlator supplies its existing container setup, touchscreen controls, mouse/keyboard input, and physical-controller configuration screens.
- PROJECT R still does **not** automatically copy its saved `.exe` URI into a container, create a game shortcut, or launch IGI 2. Configure/import the game inside the embedded Winlator UI for now.
- GitHub Actions has successfully built the merged debug APK (about 174 MB). The first-run rootfs setup and Redmi K50i on-device run are still untested. Do not treat IGI 2 compatibility as confirmed.

The Drive archive `Apk.zip` was inspected and is not the requested runtime source. It contains compiled APK payloads for an unrelated emulator/catalog app, not buildable Winlator/Wine source. It was not included. The audit and port notes are in [`docs/RUNTIME-SOURCE-IMPORT.md`](docs/RUNTIME-SOURCE-IMPORT.md).

## Build

Requirements: JDK 17, Gradle 8.9, Android SDK 35, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Android NDK `24.0.8215888`, and CMake `3.22.1`.

Fetch the pinned runtime source before building:

```bash
git submodule update --init --recursive
gradle --no-daemon assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

The first runtime launch initializes the Winlator root filesystem. Allow setup to finish before creating a container. Use a compatible graphics backend for the handset GPU; do not assume Adreno-only Turnip support on Mali devices.

## GitHub Actions

**Actions → Build PROJECT R APK** builds the app and uploads `PROJECT-R-debug-apk`. The workflow checks out the pinned submodule and installs the NDK/CMake toolchain. It runs for pull requests to `main`/`master`, pushes to those branches, or manual dispatch.

## Version

- Application ID: `com.projectr.launcher` (unchanged)
- Version code: 4
- Version name: 0.4.0

## Licensing and game files

Winlator app source is LGPL-2.1. Wine, Box86/Box64, Mesa, DXVK/VKD3D, native libraries and other bundled components have their own licences and notices. Review the pinned source and third-party terms before distributing builds; preserve required notices and source availability. No Windows game installers or game assets are included. Users must supply legally obtained game media/files themselves.
