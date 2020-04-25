package paper

import MyProject
import cmd
import ensureSuccess
import java.io.File
import java.time.LocalDateTime

fun applyPatches() {
    vanilla()
    cb()
    spigot()
    paper()
}

val date = LocalDateTime.now().toString()

private fun vanilla() {
    logger.info("Setting up nms...")

    val cb = File(cbDir)
    ensureSuccess(runGitCmd("checkout", "-B", "patched", "HEAD", directory = cb, printToStdout = true))
    val code = File(cb, codePath)
    code.deleteRecursively()
    code.mkdirs()

    val nmsPatches = File(cb, "nms-patches")
    for (patch in nmsPatches.list()) {
        val file = patch.substringBeforeLast(".") + ".java"
        val inF = File(nmsDir, file)
        val outF = File(code, file)
        inF.copyTo(outF, true)
        logger.info("Copying $inF => $outF")
    }

    runGitCmd("add", "src", directory = cb, printToStdout = true)
    runGitCmd("commit", "-m", "Minecraft $ ($date)", "--author=\"Vanilla <auto@mated.null>\"", directory = cb, printToStdout = true)
}

private fun cb() {
    logger.info("Applying CraftBukkit patches to NMS...")
    val cb = File(cbDir)
    val nmsPatches = File(cb, "nms-patches")

    for (patch in nmsPatches.list()) {
        val file = patch.substringBeforeLast(".") + ".java"
        logger.info("Patching $file < $patch")
        ensureSuccess(cmd("patch", "-s", "-d", "src/main/java/", "net/minecraft/server/$file", "<", "$nmsPatches/$patch", directory = cb, printToStdout = true))
    }

    ensureSuccess(runGitCmd("add", "src", directory = cb, printToStdout = true))
    ensureSuccess(runGitCmd("commit", "-m", "Craftbukkit $ $date", "--author=\"CraftBukkit <auto@mated.null>\"", directory = cb, printToStdout = true))
    ensureSuccess(runGitCmd("checkout", "-f", "HEAD~2", directory = cb, printToStdout = true))
}

private fun spigot() {

}

private fun paper() {

}

private fun runGitCmd(vararg args: String, directory: File = MyProject.project.rootProject.projectDir, printToStdout: Boolean = false): Pair<Int, String?> {
    return cmd("git", "-c", "commit.gpgsign=false", *args, directory = directory, printToStdout = printToStdout)
}
