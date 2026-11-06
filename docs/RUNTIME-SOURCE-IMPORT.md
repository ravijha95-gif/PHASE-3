# Runtime source import plan

The project currently contains the PROJECT R Android launcher UI and an external Winlator app hand-off. It does **not** contain Wine, Box86/Box64, a Windows container/root filesystem, graphics translation components, or a game execution surface. The Android UI alone cannot execute a Windows `.exe`.

## User-provided archive audit

The Drive file named `Apk.zip` (41,449,823 bytes) was inspected. It is **not an Android source project**: it contains APK internals (`classes.dex`, `AndroidManifest.xml`, compiled native `.so` libraries), web assets, and bundled emulator/game data, but no Kotlin/Java sources or Gradle build files. Its assets describe a different product (Clocked Daemon), including EmulatorJS 4.2.3 / arcade-emulation and cricket-specific components. It does not provide Wine, Box86/Box64, Windows containers, or an IGI 2-compatible execution engine. Repackaging its compiled APK pieces would be unsupported and would not add a Windows runtime to PROJECT R.

The archive's own notice identifies GPL-3.0 EmulatorJS and other component licences, and says upstream source is separately distributed. Any port of its UI/store code would need the real source, licence review, and separate review of its game/catalog data and service configuration. Its bundled server defaults also contain an access key; do not copy that configuration into PROJECT R, and rotate the key if it is still active. The archive was used only for inspection and is not included in this repository.

## Approved runtime source and port plan

The chosen upstream is [`brunodev85/winlator-app`](https://github.com/brunodev85/winlator-app), pinned for this port plan to commit `2c33741d50c5daab0521a098be5c969f63b9e887` (the repository HEAD inspected on 2026-11-06). The source declares LGPL-2.1, compile/target SDK 35/28, min SDK 26, and an arm64-v8a native build. Its Android module uses AGP 8.4.2 and includes a CMake project, a large root filesystem and graphics/runtime assets; the source is an Android application, not a drop-in Gradle library.

A quick upstream tree audit found assets including a 65 MB compressed rootfs, Direct3D components, Wine/Box64/graphics driver payloads, native libraries and extensive Android lifecycle, container, X server, and input-control code. Pulling all assets directly into this repository is inappropriate for the current patch-size budget. The port must preserve the pinned upstream source as a separately fetched, reproducible upstream component and explicitly manage its asset distribution rather than silently copying a built APK.

Port architecture:

1. Build the pinned Winlator app unchanged first, then retain its runtime lifecycle as the host. The intended integration is to rehost PROJECT R's launcher/library UI in that host and connect profiles to Winlator's own container and shortcut APIs; do not pretend the existing URI picker or an undocumented intent can launch an EXE.
2. Resolve application ID / namespace and package assumptions, merge manifest activities/services/providers and permissions, adapt native CMake/ABI packaging, and retain required runtime assets. Keep `com.projectr.launcher` only if the merged host can safely support it.
3. Implement a user-owned game profile flow that imports legally owned game files, creates/chooses a container, configures the game, and stores Winlator shortcut/container identifiers. Keep store/catalog separate; do not reuse the unrelated archive's backend, access key, catalog or game data.
4. Wire the native runtime's touchscreen-control editor, mouse/keyboard injection, and physical controller binding systems into the launcher/game session. Validate hot-plug, lifecycle, and input with a real controller.
5. For Redmi K50i, query actual GPU/driver capabilities and choose a supported Mali-compatible renderer. Never force Adreno-only Turnip. Run on-device tests before claiming IGI 2 compatibility.
6. Retain all applicable Winlator, Wine, Box86/Box64, Mesa, DXVK/VKD3D and other dependency notices. Audit the exact pinned tree before distributing builds.

Windows game installers and game assets are not part of this repository. Users supply legally obtained media/files themselves. The `Apk.zip` archive is not included.

## Runtime integration milestones

- [x] Inspect the user-provided `Apk.zip`; it is a compiled Clocked Daemon APK payload, not runtime source, and is not suitable for this Windows-game project.
- [x] Select and pin upstream Winlator app source revision `2c33741d50c5daab0521a098be5c969f63b9e887` for the port plan.
- [ ] Fetch the pinned source reproducibly and produce a complete file/dependency/license inventory.
- [ ] Build the original runtime unchanged first and document toolchain/results.
- [ ] Confirm supported device ABI and GPU backend on the Redmi K50i (MediaTek/Mali; do not assume Adreno/Turnip).
- [ ] Adapt runtime installation/setup and persistent container management.
- [ ] Add a native/Android game-render surface with lifecycle, audio, storage, and error/log handling.
- [ ] Wire input events from touchscreen overlay and Android game controllers into runtime-supported mouse/keyboard/gamepad interfaces.
- [ ] Add IGI 2 profile only; user supplies its installation/media. Validate setup, Direct3D 8 rendering, audio, and controls on the target device.
- [ ] Keep the external Winlator bridge clearly labelled until in-process execution and tests succeed.

## Current capabilities and limitations

PROJECT R can import a game folder, remember a selected executable URI, and open the separately installed Winlator app. The executable is not passed to Winlator; there is no in-app Windows execution, touchscreen control overlay, or controller-to-runtime mapping in this phase. Never advertise these as working until connected to and verified against the integrated runtime.
