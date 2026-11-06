# Runtime integration checklist

Status reflects source/build wiring only. The merged APK and runtime must pass CI and on-device validation before advertising a working Windows gaming experience.

- [x] Pin Winlator app source revision `2c33741d50c5daab0521a098be5c969f63b9e887` as `runtime/winlator-app` submodule.
- [ ] Review upstream and every bundled component's licence; add complete in-app / distribution notices.
- [x] Wire upstream Java source, resources, runtime assets, and JNI libraries into PROJECT R's app build.
- [x] Merge Winlator runtime activities, service, provider, permissions, and internal runtime entry point while preserving PROJECT R application ID.
- [x] Wire upstream CMake project and arm64-v8a ABI/NDK configuration.
- [x] Include Winlator's Wine/Box86/Box64/rootfs and graphics/input runtime assets in the APK build configuration.
- [x] Include Winlator's container lifecycle, X server game surface, on-screen controls, mouse/keyboard, and physical gamepad configuration code.
- [ ] Confirm the full Android/NDK build in GitHub Actions; resolve any compile or packaging failures.
- [ ] Test initial rootfs setup, create/start/stop containers, logs, and cleanup on the Redmi K50i.
- [ ] Confirm the Mali-compatible renderer and record actual Vulkan/OpenGL details; never assume Adreno-only Turnip.
- [ ] Connect PROJECT R game profiles and saved EXE document URIs to Winlator container IDs / shortcut records.
- [ ] Implement a guided, permission-safe import/copy flow and direct game launch from PROJECT R.
- [ ] Test touch controls, mouse/keyboard, USB/Bluetooth controller hot-plug and bindings, audio, pause/resume, and lifecycle.
- [ ] Test IGI 2 installation and gameplay with user-supplied licensed game files; verify rendering, audio, and input.
- [ ] Decide whether PROJECT R needs a separate, rights-reviewed store/catalog. Do not bundle copyrighted game installers or reuse the unrelated archive's backend/key/catalog.
