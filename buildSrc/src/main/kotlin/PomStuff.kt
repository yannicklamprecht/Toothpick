import kotlinx.dom.elements
import kotlinx.dom.parseXml
import kotlinx.dom.search
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.project
import java.io.File

fun DependencyHandlerScope.loadDependencies(pomFile: File, project: Project, addAPI: Boolean = false) {
    if (!pomFile.exists()) {
        project.logger.warn("$pomFile doesn't exist, please run `setupUpstream` and then `applyPatches`!")
        return
    }

    project.logger.info("Loading dependencies from pom $pomFile")

    val dom = parseXml(pomFile)

    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return

    // Load dependencies
    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent
        val version = dependencyElem.search("version").firstOrNull()!!.textContent.applyReplacements(mapOf(
                "project.version" to "${project.version}",
                "minecraft.version" to project.toothPick.minecraftVersion
        ))
        val scope = dependencyElem.search("scope").firstOrNull()?.textContent
        val classifier = dependencyElem.search("classifier").firstOrNull()?.textContent

        val dependencyString = "${groupId}:${artifactId}:${version}${classifier?.run { ":$this" } ?: ""}"
        project.logger.debug("Read dependency '{}' from '{}'", dependencyString, pomFile.absolutePath)

        // Special case API
        if (artifactId == "${project.toothPick.upstreamName.toLowerCase()}-api") {
            if (addAPI) {
                add("implementation", project(":${project.toothPick.forkName.toLowerCase()}-api"))
            }
            return@forEach
        } else if(artifactId == "minecraft-server") {
            return@forEach
        }

        when (scope) {
            "compile", null -> add("api", dependencyString)
            "provided" -> {
                add("api", dependencyString)
                add("testImplementation", dependencyString) // TODO: Bukkit quirk? or Maven scope mapping? No clue
            }
            "runtime" -> add("runtimeOnly", dependencyString)
            "test" -> add("testImplementation", dependencyString)
        }
    }
}

fun RepositoryHandler.loadRepositories(pomFile: File, project: Project) {
    if (!pomFile.exists()) {
        project.logger.warn("$pomFile doesn't exist, please run `setupUpstream` and then `applyPatches`!")
        return
    }

    project.logger.info("Loading repositories from pom $pomFile")

    val dom = parseXml(pomFile)
    val repositoriesBlock = dom.search("repositories").firstOrNull() ?: return

    // Load repositories
    repositoriesBlock.elements("repository").forEach { repositoryElem ->
        val url = repositoryElem.search("url").firstOrNull()?.textContent ?: return@forEach
        maven(url)
    }
    mavenLocal()
}

private fun String.applyReplacements(replacements: Map<String, String>): String {
    var result = this
    for ((key, value) in replacements) {
        result = result.replace("\${$key}", value)
    }
    return result
}
