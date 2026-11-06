# Winlator runtime source integration

PROJECT R now has a first in-process source integration of Winlator. This is a substantial native/runtime merge and remains **unverified until CI and on-device tests pass**.

## User-provided archive audit

The Drive file named `Apk.zip` (41,449,823 bytes) was inspected. It is **not an Android source project**: it contains APK internals (`classes.dex`, `AndroidManifest.xml`, compiled native `.so` libraries), web assets, and bundled emulator/game data, but no Kotlin/Java sources or Gradle build files. Its assets describe a different product (Clocked Daemon), including EmulatorJS 4.2.3 / arcade-emulation and cricket-specific components. It does not provide Wine, Box86/Box64, Windows containers, or an IGI 2-compatible execution engine. Repackaging its compiled APK pieces would be unsupported and would not add a Windows runtime to PROJECT R.

The archive's own notice identifies GPL-3.0 EmulatorJS and other component licences, and says upstream source is separately distributed. Its bundled server defaults contain an access key; do not copy that configuration into PROJECT R, and rotate the key if it is still active. The archive and its catalog/game data are not included in this repository.

## Pinned Winlator source

- Source: [`brunodev85/winlator-app`](https://github.com/brunodev85/winlator-app)
- Pinned commit: `2c33741d50c5daab0521a098be5c969f63b9e887`
- Upstream app license: LGPL-2.1; component dependencies have separate licences/notices.
- Upstream app settings inspected: min SDK 26, target SDK 28, arm64-v8a, NDK `24.0.8215888`, CMake `3.22.1`.
- The root project tracks this source as submodule `runtime/winlator-app`; the full runtime tree is intentionally not copied into PROJECT R's Git history.

The source includes Android runtime/container/X server/input code, native CMake code, JNI libraries, and large compressed assets (including rootfs, Wine components, graphics backends and control profiles). The app Gradle configuration wires these source trees into the PROJECT R APK. The existing PROJECT R application ID stays `com.projectr.launcher`; the Android namespace is `com.winlator` so upstream code uses its expected generated `R` class. A build-generated Java copy adapts Winlator's hard-coded FileProvider authority without editing the pinned upstream submodule.

## Current integration behavior

- Engine and game-details buttons open Winlator's internal `com.winlator.MainActivity` within the PROJECT R APK.
- Winlator's own screens handle initial rootfs setup, containers, touchscreen controls, mouse/keyboard, and physical gamepad bindings.
- The selected EXE URI is still only saved in PROJECT R. No automatic document copy, container selection, shortcut creation, or direct game launch is implemented. Users must configure/import the game from Winlator's UI for now.
- The Redmi K50i uses Mali graphics. Do not force Adreno-only Turnip. The correct renderer and IGI 2 behavior require device tests.

## Build

```bash
git submodule update --init --recursive
gradle --no-daemon assembleDebug
```

The GitHub Actions workflow checks out submodules recursively and installs the pinned NDK/CMake versions. The combined debug APK build passed in [run 42207461515](https://github.com/ravijha95-gif/PHASE-3/actions/runs/42207461515); its artifact is about 174 MB. First-run rootfs setup, storage needs, graphics, audio, touchscreen controls, and physical controllers still require device validation.

## Remaining integration work

- [ ] Confirm GitHub Actions build and resolve compile/packaging issues.
- [ ] Install/run on Redmi K50i; record GPU/renderer and first-run setup results.
- [ ] Integrate PROJECT R profiles with Winlator container IDs / shortcut records so the saved EXE URI can be imported and launched in one flow.
- [ ] Verify touch mapping, mouse/keyboard, Bluetooth/USB controller bindings, audio, pause/resume, and process cleanup.
- [ ] Test IGI 2 installation and gameplay with user-supplied licensed files.
- [ ] Complete third-party licence inventory and ship required notices/source links before distributing builds.
- [ ] Decide whether a separate, rights-reviewed store/catalog is needed. Do not reuse the unrelated archive's backend, key, catalog, or game content.
