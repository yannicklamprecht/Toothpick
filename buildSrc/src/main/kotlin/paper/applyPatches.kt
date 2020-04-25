package paper

import cmd
import ensureSuccess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
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
                ensureSuccess(cmd("patch", "-s", "-d", "src/main/java/", "net/minecraft/server/$file", "$nmsPatches/$patch", directory = cb, printToStdout = true))
            }

            ensureSuccess(runGitCmd("add", "src", directory = cb))
            ensureSuccess(runGitCmd("commit", "-m", "Craftbukkit $ $date", "--author=\"CraftBukkit <auto@mated.null>\"", directory = cb))
            ensureSuccess(runGitCmd("checkout", "-f", "HEAD~2", directory = cb))
        }
    }

    val spigot: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(cb)
        doLast {
            applyPatch(File(workdir, "Bukkit"), File(spigotDir, "Spigot-API"), "HEAD", logger)
            applyPatch(File(workdir, "CraftBukkit"), File(spigotDir, "Spigot-Server"), "patched", logger)
        }
    }

    val importMcDev: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(spigot)
        doLast {
            // ./scripts/importmcdev.sh "$basedir" || exit 1

            // nuke shit
            val server = File(spigotDir, "Spigot-Server")
            File(server, "nms-patches").deleteRecursively()
            File(server, "applyPatches.sh").deleteRecursively()
            File(server, "makePatches.sh").deleteRecursively()
            runGitCmd("add", ".", "-A", directory = server)
            runGitCmd("commit", "-m", "mc-dev Imports", directory = server)
        }
    }

    val paper: Task by project.tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(importMcDev)
        doLast {
            applyPatch(File(spigotDir, "Spigot-API"), File(basedir, "Paper-API"), "HEAD", logger)
            applyPatch(File(spigotDir, "Spigot-Server"), File(basedir, "Paper-Server"), "HEAD", logger)

            // TODO do we really need this?
            //    # if we have previously ran ./paper mcdev, update it
            //    if [ -d "$workdir/Minecraft/$minecraftversion/src" ]; then
            //        $basedir/scripts/makemcdevsrc.sh $basedir
            //    fi
        }
    }

    return paper
}

private fun applyPatch(what: File, target: File, branch: String, logger: Logger) {
    ensureSuccess(runGitCmd("fetch", directory = what))
    ensureSuccess(runGitCmd("branch", "-f", "upstream", branch, directory = what))

    if (!target.exists()) {
        ensureSuccess(runGitCmd("clone", what.absolutePath, target.absolutePath, directory = what.parentFile))
    }

    logger.lifecycle("Resetting ${target.name} to ${what.name}...")
    runGitCmd("remote", "rm", "upstream", directory = target)
    ensureSuccess(runGitCmd("remote", "add", "upstream", what.absolutePath, directory = target))
    runGitCmd("checkout", "master", directory = target)
    runGitCmd("checkout", "-b", "master", directory = target)
    ensureSuccess(runGitCmd("fetch", "upstream", directory = target))
    ensureSuccess(runGitCmd("reset", "--hard", "upstream/upstream", directory = target))

    logger.lifecycle("  Applying patches to $target...")
    val statusFile = File(target, ".git/patch-apply-failed")
    statusFile.delete()
    runGitCmd("am", "--abort", directory = target)

    val patchesDir = File("${target.parent}/${what.name}-Patches/")
    val patches = patchesDir.listFiles()
            ?.filter { it.name.endsWith(".patch") }
            ?.takeIf { it.isNotEmpty() } ?: return
    val gitCommand = arrayListOf("git", "-c", "commit.gpgsign=false", "am", "--3way", "--ignore-whitespace")
    gitCommand.addAll(patches.map { it.absolutePath })

    val (exit, _) = cmd(*gitCommand.toTypedArray(), directory = target, printToStdout = true)
    if (exit != 0) {
        statusFile.writeText("1")
        throw GradleException("Something did not apply cleanly to ${target.name}!")
    } else {
        statusFile.delete()
        logger.lifecycle("  Patches applied cleanly to ${target.name}")
    }
}

private fun runGitCmd(vararg args: String, directory: File, printToStdout: Boolean = false): Pair<Int, String?> {
    return cmd("git", "-c", "commit.gpgsign=false", *args, directory = directory, printToStdout = printToStdout)
}
