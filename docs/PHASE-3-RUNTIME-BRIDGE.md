# PROJECT R — Phase 4 and embedded Winlator integration

## Integrated components

- Phase 4 library, profile, GPU-inspector, and settings screens are retained.
- Upstream Winlator app source is pinned at `runtime/winlator-app` commit `2c33741d50c5daab0521a098be5c969f63b9e887`.
- The Android build includes upstream Java, resources, runtime assets, arm64 JNI libraries, and native CMake sources. PROJECT R keeps application ID `com.projectr.launcher`; Winlator's host and XServer activities are internal routes in the same APK.
- Engine opens the bundled Winlator host; external Winlator APK detection and dispatch remain optional.
- Game details can copy a SAF-imported directory into a real Winlator container and pass the copied EXE to XServer via `exec_path`.
- PROJECT R's Phase 4 profile editor/session presentation remains a separate UI layer; it should not be confused with the actual container managed by Winlator.

## Validation status

The upstream integration had a successful CI build before the Phase 4 reconciliation. **The combined Phase 4 + runtime merge has not yet been built or tested.** The first-run rootfs setup, real container creation, selected-EXE handoff, Redmi K50i/Mali graphics behavior, and IGI 2 gameplay all remain unverified. Direct launch does not automate installer prompts or set a game-specific Winlator profile.

## Build

```bash
git submodule update --init --recursive
gradle --no-daemon assembleDebug
```

The build targets min API 26 / target API 28 and uses NDK `24.0.8215888` plus CMake `3.22.1`. CI initializes the submodule and installs the native toolchain.

## User-provided archive

The Drive `Apk.zip` was inspected as an unrelated compiled emulator/catalog payload, not source for a Windows runtime. It is not included. See [`RUNTIME-SOURCE-IMPORT.md`](RUNTIME-SOURCE-IMPORT.md).
