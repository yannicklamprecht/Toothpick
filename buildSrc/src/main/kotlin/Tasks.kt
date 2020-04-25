import MyProject.mySubProjects
import MyProject.upstreamName
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import paper.applyPatches
import paper.decompile
import paper.init
import paper.remap
import java.io.File

fun initTasks() {
    MyProject.logger.info("Creating tasks...")

    val initGitSubmodules: Task by MyProject.tasks.creating {
        onlyIf {
            !File("$upstreamName/.git").exists()
        }
        doLast {
            val (exit, _) = cmd("git", "submodule", "update", "--init", printToStdout = true)
            if (exit != 0) {
                throw IllegalStateException("Failed to checkout git submodules: git exited with code $exit")
            }
        }
    }

    val setupUpstream: Task by MyProject.tasks.creating {
        dependsOn(initGitSubmodules)
        doLast {
//            val upstreamDir = File(MyProject.rootProject.projectDir, upstreamName)
//            val (exit, _) = cmd(*upstreamPatchCmd, directory = upstreamDir, printToStdout = true)
//            if (exit != 0) {
//                throw IllegalStateException("Failed to apply $upstreamName patches: script exited with code $exit")
//            }
            @Suppress("EXPERIMENTAL_API_USAGE")
            init()
            remap()
            decompile()
            applyPatches()
        }
    }

    val applyPatches: Task by MyProject.tasks.creating {
//    dependsOn(setupUpstream)
        doLast {
            for ((name, stuff) in mySubProjects) {
                val (sourceRepo, projectDir, patchesDir) = stuff

                // Reset or initialize subproject
                println(">>> Resetting subproject $name")
                if (projectDir.exists()) {
                    ensureSuccess(cmd("git", "reset", "--hard", "origin/master", directory = projectDir))
                } else {
                    ensureSuccess(cmd("git", "clone", sourceRepo.absolutePath, projectDir.absolutePath))
                }

                // Apply patches
                val patches = patchesDir.listFiles()
                        ?.filter { it.name.endsWith(".patch") }
                        ?.takeIf { it.isNotEmpty() } ?: continue

                println(">>> Applying patches to $name")
                val gitCommand = arrayListOf("git", "am", "--3way")
                gitCommand.addAll(patches.map { it.absolutePath })
                ensureSuccess(cmd(*gitCommand.toTypedArray(), directory = projectDir, printToStdout = true))
                println(">>> Done")
            }
        }
    }

    val rebuildPatches: Task by MyProject.tasks.creating {
        doLast {
            for ((name, stuff) in mySubProjects) {
                val (_, projectDir, patchesDir) = stuff

                if (!patchesDir.exists()) {
                    patchesDir.mkdirs()
                }

                println(">>> Rebuilding patches for $name")

                // Nuke old patches
                patchesDir.listFiles()
                        ?.filter { it.name.endsWith(".patch") }
                        ?.forEach { it.delete() }

                // And generate new
                ensureSuccess(cmd("git", "format-patch",
                        "--no-stat", "--zero-commit", "--full-index", "--no-signature", "-N",
                        "-o", patchesDir.absolutePath, "origin/master",
                        directory = projectDir,
                        printToStdout = true
                ))
            }
        }
    }

    val cleanUp: Task by MyProject.tasks.creating {
        doLast {
            for ((_, stuff) in mySubProjects) {
                val (_, projectDir, _) = stuff
                logger.info("Deleating $projectDir...")
                projectDir.deleteRecursively()
            }
            val upstreamDir = File(MyProject.rootProject.projectDir, upstreamName)
            logger.info("Deleating $upstreamDir...")
            upstreamDir.deleteRecursively()
        }
    }
}
