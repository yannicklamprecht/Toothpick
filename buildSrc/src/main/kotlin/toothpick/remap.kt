package toothpick

import cmd
import kotlinx.dom.elements
import kotlinx.dom.parseXml
import kotlinx.dom.search
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import stuff.taskGroupPrivate
import java.io.File

fun remap2(project: Project): Task {
    val createRet: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            cmd("java", "-jar", "srg2source.jar", "--extract", "--out", "test_ret.txt", "--sc", "JAVA_10",
                    "--in", "../MiniPaper-Server/src/main/java",
                    "--lib", "../MiniPaper-API/src/main/java",
                    "--lib", "../Paper/work/Minecraft/1.15.2/spigot",
                    *loadPom(File("/home/martin/.m2/repository/"), project.rootProject.projectDir.resolve("MiniPaper-API/pom.xml")),
                    *loadPom(File("/home/martin/.m2/repository/"), project.rootProject.projectDir.resolve("MiniPaper-Server/pom.xml")),
                    "--batch", "true",
                    directory = File(project.rootProject.projectDir, "work"), printToStdout = true)
        }
    }
    val sortRet: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            cmd("java", "-jar", "srg2source.jar", "--sort", "--in", "test_ret.txt", "--out", "test_ret_sorted.txt",
                    directory = File(project.rootProject.projectDir, "work"), printToStdout = true)
        }
    }

    val mapStuff: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            cmd("java", "-jar", "srg2source.jar", "--apply", "--in", "../MiniPaper-Server/src/main/java", "--out", "test", "--map", "spigotToMojang.srg", "--range", "test_ret.txt",
                    directory = File(project.rootProject.projectDir, "work"), printToStdout = false)
        }
    }

    return mapStuff
}

fun loadPom(repo: File, pomFile: File): Array<String> {
    val dom = parseXml(pomFile)

    val dependenciesBlock = dom.search("dependencies").firstOrNull() ?: return arrayOf()

    val libList = arrayListOf<String>()

    // Load dependencies
    dependenciesBlock.elements("dependency").forEach { dependencyElem ->
        val groupId = dependencyElem.search("groupId").firstOrNull()!!.textContent
        val artifactId = dependencyElem.search("artifactId").firstOrNull()!!.textContent
        val version = dependencyElem.search("version").firstOrNull()!!.textContent

        if (version.contains("project.version") || version.contains("minecraft.version")) {
            return@forEach
        }

        var lib = repo.resolve(groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-sources.jar")
        if (!lib.exists()) {
            lib = repo.resolve(groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar")
            if (!lib.exists()) {
                println("reeee, ${lib.absolutePath} doesnt exist, not even jar?!!")
                throw GradleException("reeee, ${lib.absolutePath} doesnt exist, not even jar?!!")
                return@forEach
            } else {
                println("reeee, ${lib.absolutePath} doesnt exist!")
//                throw GradleException("reeee, ${lib.absolutePath} doesnt exist!")
                return@forEach
            }
        }

        libList.add("--lib")
        libList.add(lib.absolutePath)
    }

    return libList.toTypedArray()
}
