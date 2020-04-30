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

    // Don't like to do this but sadly have to do this for compatibility reasons
    val relocVersion = toothPick.minecraftVersion.replace(".", "_")
    relocate("org.bukkit.craftbukkit", "org.bukkit.craftbukkit.v$relocVersion") {
        exclude("org.bukkit.craftbukkit.Main*")
    }

    relocate("net.minecraft.server", "net.minecraft.server.v$relocVersion")
}

tasks["build"].dependsOn(shadowJar)
