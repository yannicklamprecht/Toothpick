import java.io.File
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
    id("org.ajoberstar.grgit") version "4.0.2"
}

val upstreamRepository: Grgit by lazy {
    val upstreamGitDir = File(rootProject.projectDir, "Paper/.git")
    if (!upstreamGitDir.exists()) {
        val repo = Git.open(upstreamGitDir)
        repo.pull().call()
        println("should be checked out tbfh")
    }
    Grgit.open(mapOf("dir" to upstreamGitDir.absolutePath))
}

extra["minecraftVersion"] = "1.15.2"

allprojects {
    group = "eu.mikroskeem.toothpick"
    version = "${rootProject.extra["minecraftVersion"]}-R0.1-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
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
    onlyIf { false } // TODO: manually change atm
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
    dependsOn(setupPaper)
    onlyIf { false } // TODO: manually change atm
    doLast {
        val paperApiDir = File(rootProject.projectDir, "Paper/Paper-API")
        val paperServerDir = File(rootProject.projectDir, "Paper/Paper-Server")
        val apiDir = project(":toothpick-api").projectDir
        val serverDir = project(":toothpick-server").projectDir

        // TODO: this task is lazy af
        ensureSuccess(cmd("git", "clone", paperApiDir.absolutePath, apiDir.absolutePath))
        ensureSuccess(cmd("git", "clone", paperServerDir.absolutePath, serverDir.absolutePath))
    }
}

fun cmd(vararg args: String, directory: File = rootProject.projectDir, printToStdout: Boolean = false): Pair<Int, String?> {
    val p = ProcessBuilder()
            .command(*args)
            .redirectErrorStream(true)
            .directory(directory)
            .start()
    val output = p.inputStream.bufferedReader().use {
        val lines = java.util.LinkedList<String>()
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