val kotlinxDomVersion = "0.0.10"
val kotlinxSerializationVersion = "0.20.0"
val mercuryVersion = "0.1.0-SNAPSHOT"
val lorenzVersion = "0.6.1-SNAPSHOT"
val atlasVersion = "0.3.0-SNAPSHOT"
val bombeVersion = "0.4.0"

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.3.70"
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.dom:$kotlinxDomVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
    implementation("org.cadixdev:mercury:$mercuryVersion")
    implementation("org.cadixdev:lorenz:$lorenzVersion")
    implementation("org.cadixdev:lorenz-io-proguard:$lorenzVersion")
    implementation("org.cadixdev:atlas:$atlasVersion")
    implementation("org.cadixdev:bombe:$bombeVersion")
}

gradlePlugin {
    plugins {
        register("ToothPick") {
            id = "toothpick"
            implementationClass = "ToothPick"
        }
    }
}
