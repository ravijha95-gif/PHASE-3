# PROJECT R — Phase 4 + embedded Winlator runtime

PROJECT R is an Android launcher with the Phase 4 game-library, container-profile, and GPU-inspector UI, plus the pinned upstream Winlator app/runtime embedded in the APK.

## Phase 4 interface

- Preserves the Phase 4 library, settings, controller-profile, GPU-inspector, and shortcut-configuration screens.
- Includes the IGI 2: Covert Strike profile preset and Mali/Turnip warning logic.
- Keeps the existing PROJECT R application ID and UI.

The Phase 4 profile editor and XServer session presentation are PROJECT R UI layers; actual Windows execution is provided by the embedded upstream Winlator runtime, not by the UI simulation itself.

## Embedded runtime and game launch

- Winlator app source is pinned as a submodule at `runtime/winlator-app`, commit `2c33741d50c5daab0521a098be5c969f63b9e887`.
- The APK build includes its Java source, resources, runtime assets, arm64 native libraries, and CMake project.
- Open **Engine → Open Runtime**, complete first-run root filesystem setup, and create/initialize a container in Winlator.
- In a game's details, link an EXE located inside its imported game folder and tap **LAUNCH GAME IN BUNDLED WINLATOR**. PROJECT R copies the folder into the selected Winlator container and starts the EXE using Winlator's `exec_path` handoff.
- The direct launch path is newly integrated and still needs a clean CI build and device validation. It does not automate installer dialogs or choose a game-specific graphics profile. IGI 2 gameplay compatibility is not yet confirmed.

See [`docs/IGI2-LAUNCH.md`](docs/IGI2-LAUNCH.md) for setup, launch steps, and limitations. The unrelated Drive `Apk.zip` was inspected only as a reference and is not bundled.

## Build

```bash
git submodule update --init --recursive
gradle --no-daemon assembleDebug
```

The Android build uses JDK 17, Android SDK 35, NDK `24.0.8215888`, and CMake `3.22.1`. The resulting debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.
