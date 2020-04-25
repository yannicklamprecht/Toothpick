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
