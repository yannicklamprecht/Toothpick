import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import kotlinx.dom.elements
import kotlinx.dom.parseXml
import kotlinx.dom.search
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    java
    id("com.github.johnrengelman.shadow")
}

val minecraftVersion: String = rootProject.extra["minecraftVersion"] as String

repositories {
    loadRepositories(File(project.projectDir, "pom.xml"))
}

dependencies {
    loadDependencies(File(project.projectDir, "pom.xml"))
}

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
    val relocVersion = minecraftVersion.replace(".", "_")
    relocate("org.bukkit.craftbukkit", "org.bukkit.craftbukkit.v$relocVersion") {
        exclude("org.bukkit.craftbukkit.Main*")
    }

    relocate("net.minecraft.server", "net.minecraft.server.v$relocVersion")
}

tasks["build"].dependsOn(shadowJar)

fun RepositoryHandler.loadRepositories(pomFile: File) {
    val dom = parseXml(pomFile)
    val repositoriesBlock = dom.search("repositories").firstOrNull() ?: return

    // Load repositories
    repositoriesBlock.elements("repository").forEach { repositoryElem ->
        val url = repositoryElem.search("url").firstOrNull()?.textContent ?: return@forEach
        maven(url)
    }
}

fun DependencyHandlerScope.loadDependencies(pomFile: File) {
    val dom = parseXml(pomFile)

    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return

    // Load dependencies
    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent
        val version = dependencyElem.search("version").firstOrNull()!!.textContent.applyReplacements(mapOf(
                "project.version" to "${project.version}",
                "minecraft.version" to minecraftVersion
        ))
        val scope = dependencyElem.search("scope").firstOrNull()?.textContent
        val classifier = dependencyElem.search("classifier").firstOrNull()?.textContent

        val dependencyString = "${groupId}:${artifactId}:${version}${classifier?.run {":$this" } ?: ""}"
        logger.debug("Read dependency '{}' from '{}'", dependencyString, pomFile.absolutePath)

        // Special case API
        if (groupId == "com.destroystokyo.paper" && artifactId == "paper-api") {
            implementation(project(":toothpick-api"))
            return@forEach
        }

        when (scope) {
            "compile", null -> compile(dependencyString)
            "provided" -> {
                compileOnly(dependencyString)
                testImplementation(dependencyString) // TODO: Bukkit quirk? or Maven scope mapping? No clue
            }
            "runtime" -> runtimeOnly(dependencyString)
            "test" -> testImplementation(dependencyString)
        }
    }
}

fun String.applyReplacements(replacements: Map<String, String>): String {
    var result = this
    for ((key, value) in replacements) {
        result = result.replace("\${$key}", value)
    }
    return result
}