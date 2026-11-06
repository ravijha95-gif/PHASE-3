# PROJECT R — Runtime integration status

## What is wired into the app

- PROJECT R source has the Winlator app source as a pinned submodule at `runtime/winlator-app` (`2c33741d50c5daab0521a098be5c969f63b9e887`).
- The PROJECT R Android application build is configured to consume the upstream Java source, resources, runtime assets, arm64 JNI libraries, and native CMake project.
- The app retains its application ID `com.projectr.launcher`. Winlator's own `MainActivity` is an internal Activity and the Engine / game-details actions open it from the same APK.
- Winlator's own UI provides container creation, rootfs initialization, touchscreen control editing, and physical game-controller bindings.

## Not yet completed or verified

- The combined Android/native debug APK build passed in GitHub Actions run [42207461515](https://github.com/ravijha95-gif/PHASE-3/actions/runs/42207461515). The uploaded artifact is about 174 MB.
- PROJECT R persists the selected EXE document URI, but does not copy it into a Winlator container, create a Winlator shortcut, or launch that game automatically. Users must use Winlator's UI to configure/import their game.
- The first-run rootfs install and runtime startup have not been tested on-device.
- Graphics backend selection for the Redmi K50i's Mali GPU and actual IGI 2 compatibility remain unverified.
- Complete third-party licence/notice review and in-app notices remain required before redistributing builds.

## Build prerequisites

Initialize the submodule before running Gradle locally:

```bash
git submodule update --init --recursive
gradle --no-daemon assembleDebug
```

The runtime app currently targets Android API 28 / min API 26, uses NDK `24.0.8215888`, and requires CMake `3.22.1`. The CI workflow initializes the submodule and installs this native toolchain.

## User-provided archive

The Drive `Apk.zip` is a compiled APK payload for an unrelated emulator/catalog app, not Winlator source. It was inspected but is not included. See [`RUNTIME-SOURCE-IMPORT.md`](RUNTIME-SOURCE-IMPORT.md) for details.
