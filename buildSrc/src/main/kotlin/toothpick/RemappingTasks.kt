package toothpick

import cmd
import ensureSuccess
import org.cadixdev.at.io.AccessTransformFormats
import org.cadixdev.atlas.Atlas
import org.cadixdev.bombe.jar.asm.JarEntryRemappingTransformer
import org.cadixdev.lorenz.asm.LorenzRemapper
import org.cadixdev.lorenz.io.MappingFormats
import org.cadixdev.mercury.Mercury
import org.cadixdev.mercury.at.AccessTransformerRewriter
import org.cadixdev.mercury.extra.BridgeMethodRewriter
import org.cadixdev.mercury.remapper.MercuryRemapper
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import paper.runGitCmd
import stuff.taskGroupPrivate
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files

fun initRemappingTasks(project: Project): Task {
    val createMappings: Task by project.tasks.creating {
        group = taskGroupPrivate
        onlyIf {
            !Files.exists(project.projectDir.toPath().resolve("work/spigotToMojang.srg"))
        }
        doLast {
            val projectDir = project.projectDir.toPath()

            val buildData = projectDir.resolve("work/Paper/work/BuildData/mappings")

            project.logger.info("Reading...")
            val classes = MappingFormats.CSRG.read(buildData.resolve("bukkit-1.15.2-cl.csrg"))
            val members = MappingFormats.CSRG.read(buildData.resolve("bukkit-1.15.2-members.csrg"))

            project.logger.info("Fixing packages...")
            classes.topLevelClassMappings.forEach { klass ->
                if (!klass.deobfuscatedName.contains("/")) {
                    klass.deobfuscatedName = "net/minecraft/server/" + klass.deobfuscatedName
                }
            }
            members.topLevelClassMappings.forEach { klass ->
                if (!klass.deobfuscatedName.contains("/")) {
                    klass.deobfuscatedName = "net/minecraft/server/" + klass.deobfuscatedName
                }
            }

            project.logger.info("Combining...")
            val spigotToNms = members.reverse()
            spigotToNms.topLevelClassMappings.forEach { klassWith ->
                val klass = classes.topLevelClassMappings.find { k -> k.deobfuscatedName == klassWith.obfuscatedName }
                if (klass != null) {
                    klassWith.deobfuscatedName = klass.obfuscatedName
                }
            }
            classes.topLevelClassMappings.forEach { klass ->
                val klassWith = spigotToNms.getOrCreateTopLevelClassMapping(klass.deobfuscatedName)
                klassWith.deobfuscatedName = klass.obfuscatedName
            }

            val nmsToSpigot = spigotToNms.reverse()

            project.logger.info("Writing...")
            MappingFormats.SRG.write(spigotToNms, project.projectDir.toPath().resolve("work/spigotToNms.srg"))
            MappingFormats.SRG.write(nmsToSpigot, project.projectDir.toPath().resolve("work/nmsToSpigot.srg"))
            project.logger.info("Done!")


            project.logger.info("Reading mojang...")
            val mojangMappings = MappingFormats.byId("proguard").createReader(URL("https://launcher.mojang.com/v1/objects/59c55ae6c2a7c28c8ec449824d9194ff21dc7ff1/server.txt").openStream()).read()

            project.logger.info("Combining...")
            val mojangToSpigot = mojangMappings.merge(nmsToSpigot)
            val spigotToMojang = mojangToSpigot.reverse()

            project.logger.info("Writing...")
            MappingFormats.SRG.write(mojangToSpigot, project.projectDir.toPath().resolve("work/mojangToSpigot.srg"))
            MappingFormats.SRG.write(spigotToMojang, project.projectDir.toPath().resolve("work/spigotToMojang.srg"))
            project.logger.info("Done!")
        }
    }

    val applyMercury: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(createMappings)
        doLast {
            val projectDir = project.projectDir.toPath()
            val outputDir = projectDir.resolve("work/Paper-Server-Remapped/src/main/java")

            val paper = project.projectDir.resolve("work/Paper/Paper-Server")
            val remapped = project.projectDir.resolve("work/Paper-Server-Remapped")

            if (remapped.exists()) {
                ensureSuccess(cmd("git", "reset", "--hard", "origin/master", directory = remapped))
            } else {
                ensureSuccess(cmd("git", "clone", paper.absolutePath, remapped.absolutePath, directory = project.projectDir))
            }

            if (Files.isDirectory(outputDir)) {
                outputDir.toFile().deleteRecursively()
            }
            Files.createDirectory(outputDir)

            val mercury = Mercury()

            val mappings = MappingFormats.SRG.read(projectDir.resolve("work/spigotToMojang.srg"))
            val mappings2 = MappingFormats.SRG.read(projectDir.resolve("work/toothpick.srg"))
            val ats = AccessTransformFormats.FML.read(projectDir.resolve("work/toothpick.at"))

            mercury.sourceCompatibility = "1.8"
            mercury.encoding = StandardCharsets.UTF_8

            mercury.classPath.add(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2-mapped.jar"))
            mercury.classPath.add(projectDir.resolve("work/Paper/Paper-API/src/main/java"))

            project.subprojects.forEach { p ->
                p.configurations.forEach { config ->
                    if (config.isCanBeResolved) {
                        config.resolvedConfiguration.files.forEach { file ->
                            mercury.classPath.add(file.toPath())
                        }
                    }
                }
            }

            mercury.processors.add(MercuryRemapper.create(mappings2))
            mercury.processors.add(MercuryRemapper.create(mappings))
            mercury.processors.add(AccessTransformerRewriter.create(ats))
            mercury.processors.add(BridgeMethodRewriter.create())

            mercury.rewrite(projectDir.resolve("work/Paper/Paper-Server/src/main/java"), outputDir)
        }
    }

    val applyAtlas: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(applyMercury)
        doLast {
            val projectDir = project.projectDir.toPath()
            val mappings = MappingFormats.SRG.read(projectDir.resolve("work/spigotToMojang.srg"))
            val mappings2 = MappingFormats.SRG.read(projectDir.resolve("work/toothpick.srg"))
            val ats = AccessTransformFormats.FML.read(projectDir.resolve("work/toothpick.at"))
            val atlas = Atlas()
            atlas.install {
                AccessTransformingJarEntryTransformer(ats)
            }
            atlas.install { ctx ->
                JarEntryRemappingTransformer(LorenzRemapper(mappings2, ctx.inheritanceProvider()))
            }
            atlas.install { ctx ->
                DebugJarEntryRemappingTransformer(LorenzRemapper(mappings, ctx.inheritanceProvider()))
            }
            atlas.run(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2-mapped.jar"), projectDir.resolve("work/1.15.2-mojang-mapped.jar"))
            atlas.close()
        }
    }

    val applyPostMappingPatches by project.tasks.creating {
        group = taskGroupPrivate
//        dependsOn(applyAtlas)
        doLast {
            val patched = project.projectDir.resolve("work/Paper-Server-Remapped-Patched")
            val input = project.projectDir.resolve("work/Paper-Server-Remapped")

            runGitCmd("add", ".", "-A", directory = input)
            runGitCmd("commit", "-m", "Mojang Mappings", directory = input)

            if (patched.exists()) {
                ensureSuccess(cmd("git", "reset", "--hard", "origin/master", directory = patched))
            } else {
                ensureSuccess(cmd("git", "clone", input.absolutePath, patched.absolutePath, directory = project.projectDir))
            }

            val patches = project.projectDir.resolve("patches/postmapping").listFiles()
                    ?.filter { it.name.endsWith(".patch") }
                    ?.takeIf { it.isNotEmpty() } ?: return@doLast

            logger.lifecycle(">>> Applying patches to $name")
            val gitCommand = arrayListOf("git", "am", "--3way")
            gitCommand.addAll(patches.map { it.absolutePath })
            ensureSuccess(cmd(*gitCommand.toTypedArray(), directory = patched, printToStdout = true))
        }
    }

    return applyPostMappingPatches
}

fun createPostMappingPatchesTask(project: Project) {
    val createPostMappingPatches by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            val patched = project.projectDir.resolve("work/Paper-Server-Remapped-Patched")
            val patchesDir = project.projectDir.resolve("patches/postmapping")
            if (!patchesDir.exists()) {
                patchesDir.mkdirs()
            }

            // Nuke old patches
            patchesDir.listFiles()
                    ?.filter { it.name.endsWith(".patch") }
                    ?.forEach { it.delete() }

            // And generate new
            ensureSuccess(cmd("git", "format-patch",
                    "--no-stat", "--zero-commit", "--full-index", "--no-signature", "-N",
                    "-o", patchesDir.absolutePath, "origin/master",
                    directory = patched,
                    printToStdout = true
            ))
        }
    }
    createPostMappingPatches.name
}
