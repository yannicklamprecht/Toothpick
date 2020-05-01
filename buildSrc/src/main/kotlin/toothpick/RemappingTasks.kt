package toothpick

import org.cadixdev.atlas.Atlas
import org.cadixdev.bombe.analysis.CachingInheritanceProvider
import org.cadixdev.bombe.analysis.ReflectionInheritanceProvider
import org.cadixdev.bombe.jar.asm.JarEntryRemappingTransformer
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.asm.LorenzRemapper
import org.cadixdev.lorenz.io.MappingFormats
import org.cadixdev.mercury.Mercury
import org.cadixdev.mercury.extra.BridgeMethodRewriter
import org.cadixdev.mercury.remapper.MercuryRemapper
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
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
                klass.deobfuscatedName = "net/minecraft/server/" + klass.deobfuscatedName
            }
            members.topLevelClassMappings.forEach { klass ->
                klass.deobfuscatedName = "net/minecraft/server/" + klass.deobfuscatedName
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
                klassWith.deobfuscatedName = klass.obfuscatedName;
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
            val outputDir = projectDir.resolve("Minipaper-Server/src/main/java")

            if (Files.isDirectory(outputDir)) {
                outputDir.toFile().deleteRecursively()
            }
            Files.createDirectory(outputDir)

            val mercury = Mercury()

            val mappings = MappingFormats.SRG.read(projectDir.resolve("work/spigotToMojang.srg"))

            mercury.sourceCompatibility = "1.8"
            mercury.encoding = StandardCharsets.UTF_8

//            mercury.classPath.add(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2.jar"))
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

            mercury.processors.add(MercuryRemapper.create(mappings))
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
            val atlas = Atlas()
            atlas.install { ctx ->
                JarEntryRemappingTransformer(LorenzRemapper(mappings, ctx.inheritanceProvider()))
            }
            atlas.run(projectDir.resolve("work/Paper/work/Minecraft/1.15.2/1.15.2-mapped.jar"), projectDir.resolve("work/1.15.2-mojang-mapped.jar"))
            project.projectDir.resolve("work/1.15.2-mojang-mapped.jar").copyTo(project.projectDir.resolve("work/1.15.2-mojang-mapped-copied.jar"))
        }
    }

    return applyAtlas
}
