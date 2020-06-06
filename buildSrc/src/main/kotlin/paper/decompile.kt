package paper

import cmd
import ensureSuccess
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.net.URL
import stuff.taskGroupPrivate

fun decompile(project: Project): Task {
    val downloadVersionData: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            val file = File(versionjson)
            if (!file.exists()) {
                try {
                    val escaped = minecraftversion.replace("\\-pre", " Pre-Release").replace("\\.", "\\\\.")
                    val urlEncoded = escaped.replace(" ", "_")
                    val manifest = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").readText()
                    val url = "https://" + manifest.substringAfter("{\"id\": \"${escaped}\",").substringBefore("$urlEncoded.json").substringAfter("https://") + "$urlEncoded.json"
                    logger.lifecycle("$versionjson - $url")
                    val content = URL(url).readText();
                    file.writeText(content)

                    libDownloads = hashMapOf()

                    @Suppress("EXPERIMENTAL_API_USAGE")
                    Json.parseJson(content).jsonObject["libraries"]?.jsonArray?.forEach {
                        val o = it.jsonObject
                        val name = o["name"]!!.primitive.content.substringBeforeLast(":")
                        val downloadUrl = o["downloads"]?.jsonObject?.get("artifact")?.jsonObject?.get("url")!!.primitive.content.replace(".jar", "-sources.jar")
                        libDownloads[name] = downloadUrl
                    }

                } catch (e: Exception) {
                    throw GradleException("Failed to download the version.json.", e)
                }
            }
        }
    }

    val downloadLibs: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(downloadVersionData)
        doLast {
            val group = "com.mojang"
            val libPath = "$decompiledir/libraries/$group/"
            val libDir = File(libPath)
            libDir.mkdirs()

            for (lib in listOf("datafixerupper", "authlib", "brigadier")) {
                val jarPath = "$libPath/${lib}-sources.jar"
                val destPath = "$libPath/${lib}"
                val jarFile = File(jarPath)
                val destFile = File(destPath)

                if (!jarFile.exists()) {
                    logger.lifecycle("Downloading $group:$lib Sources")
                    val url = libDownloads["$group:$lib"]
                    if (url == null) {
                        logger.warn("Couldn't download $group:$lib, couln't find url in $versionjson")
                        continue
                    }
                    jarFile.writeBytes(URL(url).readBytes())
                    val content = jarFile.readText()
                    if (content.contains("<html>")) {
                        val error = content.substringAfter("<title>").substringBefore("</title>")
                        logger.warn("Couldn't download $jarPath: $error")
                        jarFile.delete()
                        continue
                    }
                }

                if (!destFile.exists()) {
                    logger.lifecycle("Extracting $group:$lib Sources")
                    destFile.mkdirs()
                    ensureSuccess(cmd("jar", "xf", jarPath, directory = destFile, printToStdout = true))
                }
            }
        }
    }

    val extractClasses: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(downloadLibs)
        doLast {
            val classFile = File(classDir);
            if (!classFile.exists()) {
                classFile.mkdirs()
                ensureSuccess(cmd("jar", "xf", "$decompiledir/$minecraftversion-mapped.jar", "net/minecraft/server", directory = classFile, printToStdout = true))
            }
        }
    }

    val decomp: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(extractClasses)
        doLast {
            val spigotDecompFolder = File("$spigotDecompDir/");
            spigotDecompFolder.mkdirs()

            // if we see the old net folder, copy it to spigot to avoid redecompiling
            val oldNetFolder = File("$decompiledir/net")
            if (oldNetFolder.exists()) {
                oldNetFolder.copyRecursively(spigotDecompFolder)
            }

            val newNetFolder = File("$spigotDecompDir/net");
            if (!newNetFolder.exists()) {
                logger.lifecycle("Decompiling classes...")
                try {
                    ensureSuccess(cmd("java", "-jar", "$workdir/BuildData/bin/fernflower.jar", "-dgs=1", "-hdc=0", "-asc=1", "-udv=0", "-rsy=1", "-aoa=1", classDir, spigotDecompDir, directory = project.rootDir, printToStdout = true))
                } catch (e: IllegalStateException) {
                    newNetFolder.deleteRecursively()
                    throw GradleException("Failed to decompile classes.", e)

                }
            }
        }
    }

    return decomp
}
