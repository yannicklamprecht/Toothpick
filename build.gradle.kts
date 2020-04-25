
plugins {
    java
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}
ProjectSingleton.project = project
logger.info("initializing project...")
initTasks()

allprojects {
    group = "eu.mikroskeem.toothpick"
//    version = "${MyProject.minecraftVersion}-R0.1-SNAPSHOT"
}

extra["settingUp"] = false

subprojects {
    apply(plugin = "java")

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
