import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import java.text.SimpleDateFormat
import java.util.*

plugins {
    java
    id("com.github.johnrengelman.shadow")
}

repositories {
    loadRepositories(File(project.projectDir, "pom.xml"), project)
}

dependencies {
    loadDependencies(File(project.projectDir, "pom.xml"), project, true)

    val projectDir = project.parent!!.projectDir
    val og = project.parent!!.projectDir.resolve("work/${toothPick.minecraftVersion}-mojang-mapped.jar")
    if (!og.exists()) {
        logger.warn("work/${toothPick.minecraftVersion}-mojang-mapped.jar doesn't exist (yet)")
        return@dependencies
    }
    for (i in 0..100) {
        val file = projectDir.resolve("work/${toothPick.minecraftVersion}-mojang-mapped-copied-$i.jar")
        if (file.exists()) {
            if (file.delete()) {
                og.copyTo(file, true)
                api(files(file))
                break
            }
        } else {
            og.copyTo(file, true)
            api(files(file))
            break
        }
    }
}

// ignore server tests as they infinitely loop right now
val test by tasks.getting(Test::class) {
    onlyIf { false } // NEVER
}

val shadowJar by tasks.getting(ShadowJar::class) {
    transform(Log4j2PluginsCacheFileTransformer::class.java)

    manifest {
        attributes(
                "Main-Class" to "org.bukkit.craftbukkit.Main",
                "Implementation-Title" to "CraftBukkit",
                //"Implementation-Version" to "${describe}",
                "Implementation-Vendor" to SimpleDateFormat("yyyyMMdd-HHmm").format(Date()),
                "Specification-Title" to "Bukkit",
                "Specification-Version" to "${project.version}",
                "Specification-Vendor" to "Bukkit Team"
        )
    }
}

tasks["build"].dependsOn(shadowJar)
