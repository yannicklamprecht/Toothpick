plugins {
    java
    toothpick
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}


toothpick {
    minecraftVersion = "1.16.1"
    forkName = "MiniPaper"
    groupId = "me.minidigger.MiniPaper"
    upstreamName = "Paper"

    subProjects = mapOf(
            // API project
            "$forkName-API" to listOf(
                    File(rootProject.projectDir, "work/${upstreamName}/${upstreamName}-API"),
                    project(":${forkName.toLowerCase()}-api").projectDir,
                    File(rootProject.projectDir, "patches/api")
            ),

            // Server project
            "$forkName-Server" to listOf(
                    File(rootProject.projectDir, "work/${upstreamName}-Server-Remapped"),
                    project(":${forkName.toLowerCase()}-server").projectDir,
                    File(rootProject.projectDir, "patches/server")
            )
    )
}

allprojects {
    group = toothPick.groupId
    version = "${toothPick.minecraftVersion}-R0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
//    apply(plugin = "toothpick")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.getting(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xmaxerrs")
        options.compilerArgs.add("400")
    }
}
