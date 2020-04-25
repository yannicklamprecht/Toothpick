package paper

import bytesToHex
import cmd
import ensureSuccess
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import java.io.File
import java.net.URL
import java.security.MessageDigest
import stuff.taskGroupPrivate

fun remap(project: Project): Task {
    val downloadVanilla: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            val jar = File("$jarpath.jar")
            if (!jar.exists()) {
                try {
                    val content = URL(minecraftserverurl).readBytes()
                    val hash = MessageDigest.getInstance("MD5").digest(content)
                    val hex = bytesToHex(hash)
                    if (hex.toLowerCase() == minecrafthash) {
                        jar.writeBytes(content)
                    } else {
                        throw GradleException("The MD5 checksum of the downloaded server jar does not match the BuildData hash.")
                    }
                } catch (e: Exception) {
                    throw GradleException("Failed to download the vanilla server jar. Check connectivity or try again later.", e)
                }
            }
        }
    }

    // TODO maybe we can avoid spawning new jvms here by launching special source directly?
    val mapClasses: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(downloadVanilla)
        doLast {
            val jar = File("$jarpath-cl.jar")
            if (!jar.exists()) {
                ensureSuccess(cmd("java", "-jar", "$workdir/BuildData/bin/SpecialSource-2.jar", "map", "--only", ".", "--only", "net/minecraft", "--auto-lvt", "BASIC", "--auto-member", "SYNTHETIC", "-i", "$jarpath.jar", "-m", classmappings, "-o", "$jarpath-cl.jar", directory = project.rootDir));
            }
        }
    }

    val mapMembers: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(mapClasses)
        doLast {
            val jar = File("$jarpath-m.jar")
            if (!jar.exists()) {
                ensureSuccess(cmd("java", "-jar", "$workdir/BuildData/bin/SpecialSource-2.jar", "map", "--only", ".", "--only", "net/minecraft", "--auto-member", "LOGGER", "--auto-member", "TOKENS", "-i", "$jarpath-cl.jar", "-m", membermappings, "-o", "$jarpath-m.jar", directory = project.rootDir))
            }
        }
    }

    val remapJar: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(mapMembers)
        doLast {
            val jar = File("$jarpath-mapped.jar")
            if (!jar.exists()) {
                ensureSuccess(cmd("java", "-jar", "$workdir/BuildData/bin/SpecialSource.jar", "--only", ".", "--only", "net/minecraft", "-i", "$jarpath-m.jar", "--access-transformer", accesstransforms, "-m", packagemappings, "-o", "$jarpath-mapped.jar", directory = project.rootDir))
            }
        }
    }

    val install: Task by project.tasks.creating {
        group = taskGroupPrivate
        dependsOn(remapJar)
        doLast {
            val cb = File("$workdir/CraftBukkit")
            ensureSuccess(cmd("mvn", "install:install-file", "-q", "-Dfile=../Minecraft/$minecraftversion/$minecraftversion-mapped.jar", "-Dpackaging=jar", "-DgroupId=org.spigotmc", "-DartifactId=minecraft-server", "-Dversion=\"$minecraftversion-SNAPSHOT\"", directory = cb))
        }
    }

    return install
}
