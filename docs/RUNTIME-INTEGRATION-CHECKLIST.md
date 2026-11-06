# Runtime integration checklist

Do not mark the runtime as installed until all required assets are packaged and validated on-device.

- [x] Select and pin upstream Winlator app revision `2c33741d50c5daab0521a098be5c969f63b9e887` (port source/asset audit still pending).
- [ ] Review upstream licence and all submodule licences.
- [ ] Import required Android Java/Kotlin application components.
- [ ] Integrate native ARM64 libraries and their ABI packaging rules.
- [ ] Integrate Wine and Box86/Box64 userland components.
- [ ] Integrate root filesystem/PRoot setup and extraction lifecycle.
- [ ] Integrate Mesa/Vulkan/OpenGL components compatible with Mali-G610.
- [ ] Implement container creation, persistence, start/stop and logs.
- [ ] Implement shortcut/executable launch API.
- [ ] Add a dedicated game-render screen and connect a configurable on-screen keyboard/mouse overlay to the runtime's input API.
- [ ] Route Android `KeyEvent` / `MotionEvent` and physical game-controller axes/buttons through a configurable gamepad-to-Windows input mapper; test controller hot-plug and dead zones.
- [ ] Decide whether PROJECT R needs a catalog/store. If added, keep it separate from the runtime, verify all content rights, and never bundle Windows game installers or copyrighted game assets without authorization.
- [ ] Connect PROJECT R game profiles to container/shortcut identifiers.
- [ ] Test on Redmi K50i; record actual Vulkan/OpenGL renderer and logs.
- [ ] Test IGI 2 installation, Direct3D 8 rendering, audio and input.
- [ ] Add all required licence texts and notices to About / open-source notices.

## Architecture decision

Preferred approach: fork/port the upstream Winlator application as the runtime host and adapt its Android presentation and launcher flows. Do not attempt to package the entire runtime as a normal Maven dependency; the runtime includes native binaries, rootfs assets, setup scripts and lifecycle code.
