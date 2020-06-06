plugins {
    java
}

// This project is used to resolve dependencies, used by mercury in the mapping process

val serverPomFile = project.parent?.projectDir?.resolve("work/Paper/Paper-Server/pom.xml")
val apiPomFile = project.parent?.projectDir?.resolve("work/Paper/Paper-API/pom.xml")
if (serverPomFile != null) {
    repositories {
        loadRepositories(serverPomFile, project)
    }

    dependencies {
        loadDependencies(serverPomFile, project, true)
    }
}
if (apiPomFile != null) {
    repositories {
        loadRepositories(apiPomFile, project)
    }

    dependencies {
        loadDependencies(apiPomFile, project, true)
    }
}
