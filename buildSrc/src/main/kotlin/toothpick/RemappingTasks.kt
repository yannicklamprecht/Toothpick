package toothpick

import cmd
import ensureSuccess
import org.cadixdev.at.io.AccessTransformFormats
import org.cadixdev.atlas.Atlas
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
                    klassWith.innerClassMappings.forEach { innerKlass ->
                        val topKlass = classes.topLevelClassMappings.find { topKlass -> topKlass.deobfuscatedName == klass.deobfuscatedName}
                        val topInnerClass = topKlass?.innerClassMappings?.find { topInnerKlass -> topInnerKlass.deobfuscatedName == innerKlass.deobfuscatedName }
                        innerKlass.deobfuscatedName = topInnerClass?.obfuscatedName
                    }
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
            val mojangFile = project.projectDir.resolve("work/server.txt")
            if(!mojangFile.exists()) {
                mojangFile.writeText(URL("https://launcher.mojang.com/v1/objects/59c55ae6c2a7c28c8ec449824d9194ff21dc7ff1/server.txt").readText())
            }
            val mojangMappings = MappingFormats.byId("proguard").read(mojangFile.toPath())

            project.logger.info("Combining...")
            val mojangToSpigot = mojangMappings.merge(nmsToSpigot)
            val spigotToMojang = mojangToSpigot.reverse()

            project.logger.info("Writing...")
            MappingFormats.SRG.write(mojangToSpigot, project.projectDir.toPath().resolve("work/mojangToSpigot.srg"))
            MappingFormats.SRG.write(spigotToMojang, project.projectDir.toPath().resolve("work/spigotToMojang.srg"))
            project.logger.info("Done!")

//            project.logger.info("Reading yarn...")
//            val tiny = LorenzTiny.readTinyMappings(projectDir.resolve("work/yarn-tiny-1.15.2+build.local").toAbsolutePath())
//            val yarnMappings = LorenzTiny.readMappings(tiny, "named", "official").read(MappingSet())
//
//            project.logger.info("Combining...")
//            val yarnToSpigot = yarnMappings.merge(nmsToSpigot)
//            val spigotToYarn = yarnToSpigot.reverse()
//
//            project.logger.info("Writing...")
//            MappingFormats.SRG.write(yarnToSpigot, project.projectDir.toPath().resolve("work/yarnToSpigot.srg"))
//            MappingFormats.SRG.write(spigotToYarn, project.projectDir.toPath().resolve("work/spigotToYarn.srg"))
//            project.logger.info("Done!")
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

            val mappings = MappingFormats.SRG.read(projectDir.resolve("work/spigotToMojang.srg"))
            val combined = MappingFormats.SRG.read(mappings, projectDir.resolve("toothpick/toothpick.srg"))
            MappingFormats.SRG.write(combined, project.projectDir.toPath().resolve("work/spigotToMojangPlusToothpick.srg"))

            val ats = AccessTransformFormats.FML.read(projectDir.resolve("toothpick/toothpick.at"))
            val mercury = Mercury()

            mercury.sourceCompatibility = "1.8"
            mercury.encoding = StandardCharsets.UTF_8

            mercury.classPath.add(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2-mapped.jar"))
            mercury.classPath.add(projectDir.resolve("work/Paper/Paper-API/src/main/java"))

            project.subprojects.forEach { p ->
                p.configurations.filter { config -> config.isCanBeResolved }.forEach { config ->
                    config.resolvedConfiguration.files.filter { file -> !file.absolutePath.contains("build\\classes") }.forEach { file ->
                        mercury.classPath.add(file.toPath())
                    }
                }
            }

            mercury.processors.add(MercuryRemapper.create(combined))
            mercury.processors.add(AccessTransformerRewriter.create(ats))
            mercury.processors.add(BridgeMethodRewriter.create())

            mercury.rewrite(projectDir.resolve("work/Paper/Paper-Server/src/main/java"), outputDir)

            runGitCmd("add", ".", "-A", directory = outputDir.toFile())
            runGitCmd("commit", "-m", "Mojang Mappings", directory = outputDir.toFile())
        }
    }

    val applyAtlas: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(applyMercury)
        doLast {
            val projectDir = project.projectDir.toPath()
            val mappings = MappingFormats.SRG.read(projectDir.resolve("work/spigotToMojang.srg"))
            val combined = MappingFormats.SRG.read(mappings, projectDir.resolve("toothpick/toothpick.srg"))
            MappingFormats.SRG.write(combined, project.projectDir.toPath().resolve("work/spigotToMojangPlusToothpick.srg"))

            val ats = AccessTransformFormats.FML.read(projectDir.resolve("toothpick/toothpick.at"))
            val atlas = Atlas()
            atlas.install {
                AccessTransformingJarEntryTransformer(ats)
            }
            atlas.install { ctx ->
                DebugJarEntryRemappingTransformer(LorenzRemapper(combined, ctx.inheritanceProvider()))
            }
            atlas.run(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2-mapped.jar"), projectDir.resolve("work/1.15.2-mojang-mapped.jar"))
            atlas.close()
        }
    }

    return applyAtlas
}
