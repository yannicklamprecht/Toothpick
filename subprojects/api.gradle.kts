plugins {
    java
}

repositories {
    loadRepositories(File(project.projectDir, "pom.xml"), project)
}

dependencies {
    loadDependencies(File(project.projectDir, "pom.xml"), project, false)
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes("Automatic-Module-Name" to "org.bukkit")
    }
}

val testTask : Task by tasks.creating {
    doLast {
        logger.lifecycle("Initializing ${minipaper.forkName} with minecraft version ${minipaper.minecraftVersion}. Upstream is ${minipaper.upstreamName}" )
    }
}
