package dumstuff

import cmd
import ensureSuccess
import kotlinx.serialization.toUtf8Bytes
import org.cadixdev.atlas.Atlas
import org.cadixdev.lorenz.asm.LorenzRemapper
import org.cadixdev.lorenz.io.MappingFormats
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import stuff.taskGroupPrivate
import toothpick.DebugJarEntryRemappingTransformer
import java.net.URL
import java.nio.file.Files

fun dumShitTasks(project: Project): List<Task> {
    val modFolder = project.projectDir.resolve("DyescapePaper-Server/src/main/java")
    val origFolder = project.projectDir.resolve("work/decomp")
    val workFolder = project.projectDir.resolve("work/dumShit")
    val patches = workFolder.resolve("patches")
    val vanillaClasses = workFolder.resolve("vanillaClasses")
    val vanillaDecomp = workFolder.resolve("vanillaDecomp")
    val modClasses = workFolder.resolve("modClasses")
    val modDecomp = workFolder.resolve("modDecomp")
    val patched = workFolder.resolve("patched")
    val paperWorkDir = project.projectDir.resolve("work/Paper/work")
    val serverUrl = "https://launcher.mojang.com/v1/objects/03b8fa357937d0bdb6650ec8cc74506ec2fd91a7/server.jar"
    val serverMappingUrl = "https://launcher.mojang.com/v1/objects/cd36be8de62b1a50f174b06767b49b9f79f3b807/server.txt"
    val minecraftVersion = "20w21a"

    val extractMod: Task by project.tasks.creating {
        group = taskGroupPrivate
        onlyIf {
            !modClasses.exists()
        }
        doLast {
            modClasses.mkdirs()
            ensureSuccess(cmd("jar", "xf", project.projectDir.resolve("DyescapePaper-Server/target/dyescapepaper-1.15.2.jar").toString(), "net/minecraft", directory = modClasses, printToStdout = true))

        }
    }

    val decompileMod: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(extractMod)
        onlyIf {
            !modDecomp.resolve("net/minecraft/server/MinecraftServer.java").exists()
        }
        doLast {
            if (modDecomp.exists()) {
                modDecomp.deleteRecursively()
            }
            modDecomp.mkdirs()
            logger.lifecycle("Decompiling classes...")
            try {
                ensureSuccess(cmd("java", "-jar", "$paperWorkDir/BuildData/bin/fernflower.jar", "-dgs=1", "-hdc=0", "-asc=1", "-udv=0", "-rsy=1", "-aoa=1", modClasses.absolutePath, modDecomp.absolutePath, directory = project.rootDir, printToStdout = true))
            } catch (e: IllegalStateException) {
                modDecomp.deleteRecursively()
                throw GradleException("Failed to decompile classes.", e)
            }
        }
    }

    val diffServer: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(decompileMod)
        onlyIf {
            !patches.exists()
        }
        doLast {
            if (patches.exists()) patches.deleteRecursively()
            patches.mkdirs()

            modDecomp.resolve("net/minecraft").walk(FileWalkDirection.TOP_DOWN).filter { file -> file.isFile }.forEach { mod ->
                val relative = mod.relativeTo(modDecomp).toString()
                val orig = origFolder.resolve(relative)
                val patch = patches.resolve("$relative.patch")
                patch.parentFile.mkdirs()

                val (_, output) = cmd("diff", "-Nu", "--label", "a/$relative", orig.absolutePath, "--label", "b/$relative", mod.absolutePath, directory = patches)
                Files.write(patch.toPath(), output?.toUtf8Bytes()!!)
            }
        }
    }

    val downloadVanillaSnapshot: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(diffServer)
        onlyIf {
            !workFolder.resolve("$minecraftVersion.jar").exists()
        }
        doLast {
            workFolder.resolve("$minecraftVersion.jar").writeBytes(URL(serverUrl).readBytes())
        }
    }

    val remapVanillaSnapshot: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(downloadVanillaSnapshot)
        onlyIf {
            !workFolder.resolve("$minecraftVersion-mapped.jar").exists()
        }
        doLast {
            val mojangFile = workFolder.resolve("server.txt")
            if (!mojangFile.exists()) {
                mojangFile.writeText(URL(serverMappingUrl).readText())
            }
            val mojangMappings = MappingFormats.byId("proguard").read(mojangFile.toPath()).reverse()

            val atlas = Atlas()
            atlas.install { ctx ->
                DebugJarEntryRemappingTransformer(LorenzRemapper(mojangMappings, ctx.inheritanceProvider()))
            }
            atlas.run(workFolder.resolve("$minecraftVersion.jar").toPath(), workFolder.resolve("$minecraftVersion-mapped.jar").toPath())
            atlas.close()

            ensureSuccess(cmd("mvn", "install:install-file", "-q", "-Dfile=${workFolder.resolve("$minecraftVersion-mapped.jar").absolutePath}", "-Dpackaging=jar", "-DgroupId=me.minidigger", "-DartifactId=minecraft-server", "-Dversion=\"$minecraftVersion-SNAPSHOT\"", directory = project.projectDir))
        }
    }

    val extractVanillaSnapshot: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(remapVanillaSnapshot)
        onlyIf {
            !vanillaClasses.exists()
        }
        doLast {
            vanillaClasses.mkdirs()
            ensureSuccess(cmd("jar", "xf", "$workFolder/$minecraftVersion-mapped.jar", "net/minecraft", directory = vanillaClasses, printToStdout = true))

        }
    }

    val decompileVanillaSnapshot: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(extractVanillaSnapshot)
        onlyIf {
            !vanillaDecomp.resolve("net/minecraft/server/MinecraftServer.java").exists()
        }
        doLast {
            if (vanillaDecomp.exists()) {
                vanillaDecomp.deleteRecursively()
            }
            vanillaDecomp.mkdirs()
            logger.lifecycle("Decompiling classes...")
            try {
                ensureSuccess(cmd("java", "-jar", "$paperWorkDir/BuildData/bin/fernflower.jar", "-dgs=1", "-hdc=0", "-asc=1", "-udv=0", "-rsy=1", "-aoa=1", vanillaClasses.absolutePath, vanillaDecomp.absolutePath, directory = project.rootDir, printToStdout = true))
            } catch (e: IllegalStateException) {
                vanillaDecomp.deleteRecursively()
                throw GradleException("Failed to decompile classes.", e)
            }
        }
    }

    val applyVanillaPatches: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(decompileVanillaSnapshot)
        doLast {
            if (patched.exists()) patched.deleteRecursively()
            patched.mkdirs()

            patches.resolve("net/minecraft").walk(FileWalkDirection.TOP_DOWN).filter { file -> file.isFile }.forEach { patch ->
                val relative = patch.relativeTo(patches).toString().replace(".patch", "")
                val decompFile = vanillaDecomp.resolve(relative)
                val patchedFile = patched.resolve(relative)

                if(!decompFile.exists()) return@forEach

                patchedFile.parentFile.mkdirs()

                decompFile.copyTo(patchedFile)
                ensureSuccess(cmd("patch", "-d", patch.relativeTo(patches).parent, patchedFile.absolutePath, patch.absolutePath, directory = patched))
            }
        }
    }

    return listOf(diffServer, decompileVanillaSnapshot)
}
