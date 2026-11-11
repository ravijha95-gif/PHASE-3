# Runtime integration checklist

Status distinguishes PROJECT R's Phase 4 profile/UI work from the actual bundled Winlator execution path. Nothing here implies IGI 2 gameplay has been validated.

- [x] Preserve the Phase 4 library, profile editor, GPU inspector, and controller-oriented UI.
- [x] Pin upstream Winlator app source as `runtime/winlator-app` at `2c33741d50c5daab0521a098be5c969f63b9e887`.
- [x] Wire upstream Winlator Java source, resources, runtime assets, JNI libraries, and CMake build into PROJECT R.
- [x] Merge Winlator internal activities, provider, service, and required permissions while preserving PROJECT R's application ID.
- [x] Open bundled Winlator from Engine; retain optional external APK detection/dispatch.
- [x] Copy a user-selected imported game directory into an actual Winlator container and hand off the copied EXE using Winlator's `exec_path` extra.
- [x] Build workflow initializes the runtime submodule and installs the required NDK/CMake toolchain.
- [ ] Run CI on the resolved Phase 4 + bundled-runtime merge and fix any build errors.
- [ ] Validate first-run rootfs setup, actual Winlator container creation and lifecycle on a physical device.
- [ ] Confirm SAF folder/EXE permissions, large game copy behavior, free-space errors, and actual EXE-in-folder handoff.
- [ ] Confirm the Redmi K50i/Mali-compatible graphics backend from device logs; do not assume Adreno-only Turnip support.
- [ ] Test IGI 2 installation/gameplay using user-provided licensed files, including rendering, audio, controls, suspend/resume, and crashes.
- [ ] Complete licence and notice review for all bundled native/runtime components before distribution.
- [ ] Do not bundle copyrighted installers or reuse the unrelated Drive archive's service key, catalog, or game payload.
