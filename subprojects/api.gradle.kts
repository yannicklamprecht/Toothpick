plugins {
    java
}

val setup: Task by tasks.creating {
    if (project.parent?.extra?.get("settingUp") as Boolean) {
        logger.info("Setting up ${project.name}")

        repositories {
            loadRepositories(File(project.projectDir, "pom.xml"))
        }

        dependencies {
            loadDependencies(File(project.projectDir, "pom.xml"), false)
        }

        val jar by tasks.getting(Jar::class) {
            manifest {
                attributes("Automatic-Module-Name" to "org.bukkit")
            }
        }
    }
}
