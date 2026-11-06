# Runtime integration checklist

Do not mark the runtime as installed until all required assets are packaged and validated on-device.

- [ ] Select and pin an upstream Winlator revision.
- [ ] Review upstream licence and all submodule licences.
- [ ] Import required Android Java/Kotlin application components.
- [ ] Integrate native ARM64 libraries and their ABI packaging rules.
- [ ] Integrate Wine and Box86/Box64 userland components.
- [ ] Integrate root filesystem/PRoot setup and extraction lifecycle.
- [ ] Integrate Mesa/Vulkan/OpenGL components compatible with Mali-G610.
- [ ] Implement container creation, persistence, start/stop and logs.
- [ ] Implement shortcut/executable launch API.
- [ ] Connect PROJECT R game profiles to container/shortcut identifiers.
- [ ] Test on Redmi K50i; record actual Vulkan/OpenGL renderer and logs.
- [ ] Test IGI 2 installation, Direct3D 8 rendering, audio and input.
- [ ] Add all required licence texts and notices to About / open-source notices.

## Architecture decision

Preferred approach: fork/port the upstream Winlator application as the runtime host and adapt its Android presentation and launcher flows. Do not attempt to package the entire runtime as a normal Maven dependency; the runtime includes native binaries, rootfs assets, setup scripts and lifecycle code.
