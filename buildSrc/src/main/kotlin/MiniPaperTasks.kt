import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import paper.applyPatches
import paper.decompile
import paper.init
import paper.remap
import java.io.File

fun Project.initMiniPaperTasks() = run {
    val initGitSubmodules: Task by project.tasks.creating {
        group = "MiniPaper"
        onlyIf {
            !File("${minipaper.upstreamName}/.git").exists() || !File("${minipaper.upstreamName}/work/BuldData.git").exists()
        }
        doLast {
            val (exit, _) = cmd("git", "submodule", "update", "--init", "--recursive", directory = rootDir, printToStdout = true)
            if (exit != 0) {
                throw IllegalStateException("Failed to checkout git submodules: git exited with code $exit")
            }
        }
    }
    initGitSubmodules.name

    val loadData: Task by tasks.creating {
        group = "MiniPaperInternal"
        dependsOn(initGitSubmodules)
        doLast {
            @Suppress("EXPERIMENTAL_API_USAGE") init(project, minipaper.upstreamName)
        }
    }

    val remap = remap(project)
    remap.mustRunAfter(loadData)
    val decompile = decompile(project)
    decompile.mustRunAfter(remap)
    val applySpigotPatches = applyPatches(project)
    applySpigotPatches.mustRunAfter(decompile)
    val setupUpstream: Task by tasks.creating {
        group = "MiniPaper"
        dependsOn(loadData, remap, decompile, applySpigotPatches)
    }
    setupUpstream.name

    val applyPatches: Task by tasks.creating {
        group = "MiniPaper"
        doLast {
            for ((name, stuff) in minipaper.subProjects) {
                val (sourceRepo, projectDir, patchesDir) = stuff

                // Reset or initialize subproject
                logger.lifecycle(">>> Resetting subproject $name")
                if (projectDir.exists()) {
                    ensureSuccess(cmd("git", "reset", "--hard", "origin/master", directory = projectDir))
                } else {
                    ensureSuccess(cmd("git", "clone", sourceRepo.absolutePath, projectDir.absolutePath, directory = rootDir))
                }

                // Apply patches
                val patches = patchesDir.listFiles()
                        ?.filter { it.name.endsWith(".patch") }
                        ?.takeIf { it.isNotEmpty() } ?: continue

                logger.lifecycle(">>> Applying patches to $name")
                val gitCommand = arrayListOf("git", "am", "--3way")
                gitCommand.addAll(patches.map { it.absolutePath })
                ensureSuccess(cmd(*gitCommand.toTypedArray(), directory = projectDir, printToStdout = true))
                logger.lifecycle(">>> Done")
            }
        }
    }
    applyPatches.name

    val rebuildPatches: Task by tasks.creating {
        group = "MiniPaper"
        doLast {
            for ((name, stuff) in minipaper.subProjects) {
                val (_, projectDir, patchesDir) = stuff

                if (!patchesDir.exists()) {
                    patchesDir.mkdirs()
                }

                logger.lifecycle(">>> Rebuilding patches for $name")

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
    rebuildPatches.name

    val cleanUp: Task by tasks.creating {
        group = "MiniPaper"
        doLast {
            for ((_, stuff) in minipaper.subProjects) {
                val (_, projectDir, _) = stuff
                logger.lifecycle("Deleating $projectDir...")
                projectDir.deleteRecursively()
            }
            val upstreamDir = File(rootProject.projectDir, minipaper.upstreamName)
            logger.lifecycle("Deleating $upstreamDir...")
            upstreamDir.deleteRecursively()
        }
    }
    cleanUp.name
}
