plugins {
    java
}
val pomFile = project.parent?.projectDir?.resolve("work/Paper/Paper-Server/pom.xml")
if (pomFile != null) {
    repositories {
        loadRepositories(pomFile, project)
    }

    dependencies {
        loadDependencies(pomFile, project, true)
    }
}
