# Runtime integration checklist

Updated for **Phase 4 (v0.4.0)** — Reskinned Winlator Container Host & GPU Inspector.

- [x] Select and pin an upstream Winlator revision (`brunodev85/winlator-app @ v11.2`, LGPL-2.1).
- [x] Review upstream licence and all submodule licences (added in-app Open-Source Notices dialog for Winlator, Wine, Box86/Box64, Mesa/VirGL, DXVK, VKD3D, PRoot).
- [x] Import/port Android container, shortcut (`.desktop`), and `imagefs` prefix manager components (`ContainerModels.kt`).
- [x] Implement real-time EGL14/GLES20 & Vulkan GPU capability detection (`GpuCapabilityDetector.kt`) with Mali-G610 / Redmi K50i hardware guard (blocks Adreno-only Turnip on Mali).
- [x] Implement container creation, persistence, start/stop, and live boot/session logs (`ContainerScreens.kt`, `XServerSessionScreen.kt`).
- [x] Implement shortcut/executable launch API (both built-in reskinned `XServerSessionScreen` and direct `com.winlator.XServerDisplayActivity` intent hand-off with `container_id` and `shortcut_path`).
- [x] Connect PROJECT R game profiles to container/shortcut identifiers and dual-sync `project_r_v2` + `project_r` preference stores.
- [x] Add 1-click **IGI 2: Covert Strike (Redmi K50i / Mali-G610)** Direct3D 8 container & game shortcut presets.
- [x] Add all required licence texts and notices to Settings -> Open-Source Notices.
- [ ] Optional on-device validation: Mount full `main.obb` / `imagefs.txz` userland archive on physical Redmi K50i handset and benchmark IGI 2 Direct3D 8 frame pacing.
