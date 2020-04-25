package paper

import cmd
import ensureSuccess
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.time.LocalDateTime

fun applyPatches(project: Project): Task {
    val date = LocalDateTime.now().toString()

    val vanilla: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        doLast {
            val cb = File(cbDir)
            ensureSuccess(runGitCmd("checkout", "-B", "patched", "HEAD", directory = cb))
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

            runGitCmd("add", "src", directory = cb)
            runGitCmd("commit", "-m", "Minecraft $ ($date)", "--author=\"Vanilla <auto@mated.null>\"", directory = cb)
        }
    }

    val cb: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(vanilla)
        doLast {
            val cb = File(cbDir)
            val nmsPatches = File(cb, "nms-patches")

            for (patch in nmsPatches.list()) {
                val file = patch.substringBeforeLast(".") + ".java"
                logger.lifecycle("Patching $file < $patch")
                ensureSuccess(cmd("patch", "-s", "-d", "src/main/java/", "net/minecraft/server/$file", "<", "$nmsPatches/$patch", directory = cb, printToStdout = true))
            }

            ensureSuccess(runGitCmd("add", "src", directory = cb, printToStdout = true))
            ensureSuccess(runGitCmd("commit", "-m", "Craftbukkit $ $date", "--author=\"CraftBukkit <auto@mated.null>\"", directory = cb, printToStdout = true))
            ensureSuccess(runGitCmd("checkout", "-f", "HEAD~2", directory = cb, printToStdout = true))
        }
    }

    val spigot: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(cb)
        doLast {

        }
    }

    val paper: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(spigot)
        doLast {

        }
    }

    return paper;
}


private fun runGitCmd(vararg args: String, directory: File, printToStdout: Boolean = false): Pair<Int, String?> {
    return cmd("git", "-c", "commit.gpgsign=false", *args, directory = directory, printToStdout = printToStdout)
}
