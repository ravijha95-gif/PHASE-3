# Launching IGI 2 from PROJECT R

PROJECT R now has an in-process launch path for the bundled Winlator runtime:

1. Open Winlator once and finish its first-run root filesystem/Wine setup.
2. Create and initialize a container (opening the container once is recommended).
3. Add the IGI 2 game directory to PROJECT R's library using the folder picker.
4. In the game details, link the game's Windows executable. Select an EXE from inside that imported directory, preferably the installed game's `igi2.exe` rather than a setup launcher.
5. Tap **LAUNCH GAME IN WINLATOR** and choose a container if prompted.

On the first launch, PROJECT R copies the selected directory tree into `C:\PROJECT R\<game name>` in the container's Wine prefix. It then starts Winlator's `XServerDisplayActivity` with the existing container ID and the copied executable path using Winlator's `exec_path` intent extra. This avoids reliance on launching a separate package or passing a content-provider URI into Wine. The saved source files remain in their original SAF folder.

The copy runs on a worker thread and reports inaccessible files, missing permissions, unavailable containers, and storage/setup errors in the app. Large game folders may take several minutes and require enough free internal storage. Re-launching copies the folder again so changed source files are reflected; users should avoid editing the game source during a copy.

## Scope and troubleshooting

- The selected EXE must be inside the imported folder tree; selecting an EXE elsewhere is rejected because its data files would not be copied with it.
- The imported folder should include the complete game files. Selecting an installer is supported as an executable, but this first implementation does not automate installer prompts or apply a game-specific graphics profile.
- If the game exits or displays a black screen after the Winlator window opens, confirm the copied executable is the installed game EXE, then try Winlator's container graphics/driver settings. IGI 2/Direct3D compatibility and device GPU behavior are not asserted as verified by this code change.
- No user device, APK build, or gameplay test has been run in this environment. Build and test on the target Android device before treating IGI 2 compatibility as confirmed.
