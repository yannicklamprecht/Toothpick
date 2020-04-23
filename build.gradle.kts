import java.io.File
import java.util.LinkedList
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatusType
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.transport.RefSpec

buildscript {
    val kotlinxDomVersion = "0.0.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx.dom:${kotlinxDomVersion}")
    }
}

plugins {
    java
    id("org.ajoberstar.grgit") version "4.0.2"
}

extra["minecraftVersion"] = "1.15.2"

allprojects {
    group = "eu.mikroskeem.toothpick"
    version = "${rootProject.extra["minecraftVersion"]}-R0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val initGitSubmodules by tasks.creating {
    onlyIf {
        !File("Paper/.git").exists()
    }
    doLast {
        /*
        Git.open(rootProject.projectDir).use {
            it.submoduleInit().call()
            it.submoduleUpdate().call()
        }
        */
        val (exit, _) = cmd("git", "submodule", "update", "--init", printToStdout = true)
        if (exit != 0) {
            throw IllegalStateException("Failed to checkout git submodules: git exited with code $exit")
        }
    }
}

val setupPaper by tasks.creating {
    dependsOn(initGitSubmodules)
    doLast {
        val paperDir = File(rootProject.projectDir, "Paper")
        val (exit, _) = if (true) { // TODO: make this configurable, not everybody uses nix lol
            cmd("nix-shell", "-p", "maven", "--command", "./paper patch", directory = paperDir, printToStdout = true)
        } else {
            cmd("./paper", "patch", directory = paperDir, printToStdout = true)
        }
        if (exit != 0) {
            throw IllegalStateException("Failed to apply Paper patches: script exited with code $exit")
        }
    }
}

val applyPaperPatches by tasks.creating {
    //dependsOn(setupPaper)
    doLast {
        val subprojects = mapOf(
                // API project
                "Toothpick-API" to listOf(
                        File(rootProject.projectDir, "Paper/Paper-API"),
                        project(":toothpick-api").projectDir,
                        File(rootProject.projectDir, "patches/api")
                ),

                // Server project
                "Toothpick-Server" to listOf(
                        File(rootProject.projectDir, "Paper/Paper-Server"),
                        project(":toothpick-server").projectDir,
                        File(rootProject.projectDir, "patches/server")
                )
        )

        // TODO: this task is lazy af
        for ((name, stuff) in subprojects) {
            val (sourceRepo, projectDir, patchesDir) = stuff

            // Reset or initialize subproject
            logger.info("Resetting subproject {}", name)
            if (projectDir.exists()) {
                ensureSuccess(cmd("git", "reset", "--hard", "origin/master", directory = projectDir))
            } else {
                ensureSuccess(cmd("git", "clone", sourceRepo.absolutePath, projectDir.absolutePath))
            }

            // Apply patches
            logger.info("Applying patches to {}", name)
            val patches = patchesDir.listFiles()
                    ?.filter { it.name.endsWith(".patch") }
                    ?.takeIf { it.isNotEmpty() } ?: continue
            val gitCommand = arrayListOf("git", "am", "--3way")
            gitCommand.addAll(patches.map { it.absolutePath })
            ensureSuccess(cmd(*gitCommand.toTypedArray(), directory = projectDir, printToStdout = true))
            logger.info("Done")
        }
    }
}

fun cmd(vararg args: String, directory: File = rootProject.projectDir, printToStdout: Boolean = false): Pair<Int, String?> {
    val p = ProcessBuilder()
            .command(*args)
            .redirectErrorStream(true)
            .directory(directory)
            .start()
    val output = p.inputStream.bufferedReader().use {
        val lines = LinkedList<String>()
        it.lines().peek(lines::add).forEach { line ->
            if (printToStdout) {
                println(line)
            }
        }
        lines.joinToString(separator = "\n")
    }
    val exit = p.waitFor()
    return exit to output
}

fun ensureSuccess(cmd: Pair<Int, String?>): String? {
    val (exit, output) = cmd
    if (exit != 0) {
        throw IllegalStateException("Failed to run command, exit code is $exit")
    }
    return output
}