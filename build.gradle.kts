plugins {
    java
    minipaper
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}

initMiniPaperTasks()

minipaper {
    minecraftVersion = "1.15.2"
    forkName = "MiniPaper"
    groupId = "me.minidigger.MiniPaper"
    upstreamName = "Paper"

    subProjects = mapOf(
            // API project
            "$forkName-API" to listOf(
                    File(rootProject.projectDir, "${upstreamName}/${upstreamName}-API"),
                    project(":${forkName.toLowerCase()}-api").projectDir,
                    File(rootProject.projectDir, "patches/api")
            ),

            // Server project
            "$forkName-Server" to listOf(
                    File(rootProject.projectDir, "${upstreamName}/${upstreamName}-Server"),
                    project(":${forkName.toLowerCase()}-server").projectDir,
                    File(rootProject.projectDir, "patches/server")
            )
    )
}

allprojects {
    group = minipaper.groupId
    version = "${minipaper.minecraftVersion}-R0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "minipaper")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
