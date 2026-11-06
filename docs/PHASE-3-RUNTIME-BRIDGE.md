# PROJECT R — Phase 3: Runtime bridge

## Included in this phase

1. Custom PROJECT R launcher icon (adaptive and legacy density assets).
2. Android package visibility for the official Winlator package (`com.winlator`).
3. Runtime detection using Android's package manager.
4. Open installed Winlator, or open the official Winlator GitHub releases page if it is absent.
5. Link a Windows executable document URI to an imported game profile.
6. Persist the executable URI and its Android read permission.
7. Preserve the Phase 2 app ID and `project_r_v2` preference store so existing library data is retained during an in-place upgrade.

## Critical limitation

This is an **external runtime bridge**, not an embedded Winlator runtime. The selected executable URI is saved in PROJECT R, but the current public Winlator app source does not document a stable public Android intent/API for receiving a document URI and starting a specific container shortcut. Therefore PROJECT R does not pretend to auto-launch the selected EXE. The user opens Winlator and selects/configures the game there.

## Verified upstream facts

The upstream `winlator-app` Android application declares:
- Namespace/application ID: `com.winlator`
- `minSdkVersion 26`
- `targetSdkVersion 28`
- `versionName 11.2` in the current source snapshot inspected for this phase.

The app repository identifies its license as LGPL-2.1 and describes itself as the Android application source. The main Winlator repository documents its runtime components and third-party credits. Pin a specific upstream commit before any embedded fork/port.

## Next step for a true in-process runtime

The correct long-term route is to fork/port the Winlator application source as the runtime host and merge PROJECT R's UI into that application, preserving the upstream runtime lifecycle, native library packaging, root filesystem assets, setup scripts and all third-party notices. Simply adding Wine or Box86 as a Maven dependency is not sufficient.

## Redmi K50i

The Redmi K50i uses a MediaTek Dimensity platform with Mali graphics. Do not force Adreno-only Turnip components. Query Vulkan/OpenGL capabilities on-device and select a supported Mali-compatible backend. Actual IGI 2 rendering must be validated on the handset.
