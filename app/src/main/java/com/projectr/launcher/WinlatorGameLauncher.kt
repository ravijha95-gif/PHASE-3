package com.projectr.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.winlator.XServerDisplayActivity
import com.winlator.container.ContainerManager
import com.winlator.xenvironment.RootFS
import java.io.File
import java.io.FileOutputStream

/** Imports a SAF-selected game folder into a chosen Winlator C: drive and starts its EXE. */
object WinlatorGameLauncher {
    data class ContainerOption(val id: Int, val name: String)

    fun isRuntimeReady(context: Context): Boolean = RootFS.find(context).isValid()

    fun listContainers(context: Context): List<ContainerOption> =
        ContainerManager(context).containers.map { ContainerOption(it.id, it.name) }

    /**
     * This must be called from a worker thread. Copying a full game directory can take a while.
     * The EXE URI must refer to a file inside the imported folder tree.
     */
    fun prepareLaunch(context: Context, game: GameEntry, containerId: Int): Intent {
        check(isRuntimeReady(context)) { "Winlator system files are not set up yet. Open the runtime once first." }
        val exeUri = Uri.parse(game.exeUri ?: error("Select the game's EXE first."))
        val gameTree = DocumentFile.fromTreeUri(context, Uri.parse(game.uri))
            ?: error("The imported game folder is no longer available. Re-import it and grant access.")
        check(gameTree.isDirectory) { "The imported game location is not a folder." }

        val manager = ContainerManager(context)
        val container = manager.getContainerById(containerId)
            ?: error("That Winlator container no longer exists. Open the runtime and choose a container.")
        val containerRoot = container.rootDir ?: error("Winlator returned an invalid container path.")
        val driveC = File(containerRoot, ".wine/drive_c")
        check(driveC.isDirectory) { "This container is not initialized yet. Open it once in Winlator and retry." }

        val destination = File(driveC, "PROJECT R/${safePathSegment(game.name)}")
        check(destination.canonicalPath.startsWith(driveC.canonicalPath + File.separator)) {
            "Invalid game folder name."
        }
        if (!destination.exists() && !destination.mkdirs()) {
            error("Could not create the game folder inside this container. Check free storage.")
        }

        var copiedExe: File? = null
        copyChildren(context, gameTree, destination, exeUri) { file -> copiedExe = file }
        val executable = copiedExe
            ?: error("The selected EXE is outside the imported folder. Import the folder containing both the EXE and its game data, then select the EXE again.")
        check(executable.isFile && executable.canRead()) { "The copied EXE could not be read." }

        return Intent(context, XServerDisplayActivity::class.java)
            .putExtra("container_id", container.id)
            .putExtra("exec_path", executable.absolutePath)
    }

    private fun copyChildren(
        context: Context,
        sourceDirectory: DocumentFile,
        destinationDirectory: File,
        selectedExe: Uri,
        onExecutableCopied: (File) -> Unit
    ) {
        for (source in sourceDirectory.listFiles()) {
            val name = safePathSegment(source.name ?: continue)
            val destination = File(destinationDirectory, name)
            check(destination.canonicalPath.startsWith(destinationDirectory.canonicalPath + File.separator)) {
                "The game folder contains an invalid file name."
            }

            if (source.isDirectory) {
                if (!destination.exists() && !destination.mkdirs()) {
                    error("Could not create ${destination.name}. Check free storage.")
                }
                copyChildren(context, source, destination, selectedExe, onExecutableCopied)
            } else if (source.isFile) {
                source.uri.let { uri ->
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Android could not read ${source.name}.")
                    input.use { inputStream ->
                        FileOutputStream(destination).use { output ->
                            inputStream.copyTo(output, DEFAULT_BUFFER_SIZE)
                        }
                    }
                    if (sameDocument(source.uri, selectedExe)) onExecutableCopied(destination)
                }
            }
        }
    }

    private fun sameDocument(left: Uri, right: Uri): Boolean {
        if (left == right) return true
        val leftId = runCatching { DocumentsContract.getDocumentId(left) }.getOrNull()
        val rightId = runCatching { DocumentsContract.getDocumentId(right) }.getOrNull()
        return leftId != null && leftId == rightId
    }

    private fun safePathSegment(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .filterNot { it.isISOControl() }
        .trim()
        .take(100)
        .ifBlank { "Imported game" }
}
