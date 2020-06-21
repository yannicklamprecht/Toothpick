val kotlinxDomVersion = "0.0.10"
val kotlinxSerializationVersion = "0.20.0"
val mercuryVersion = "0.1.1-SNAPSHOT"
val lorenzVersion = "0.6.0-SNAPSHOT"
val atlasVersion = "0.3.0-SNAPSHOT"
val bombeVersion = "0.4.0"
val at = "0.1.0-SNAPSHOT"
val eclipsejdt = "3.22.0"

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.3.70"
    id("net.linguica.maven-settings") version "0.5"
}

repositories {
    mavenCentral()
    // mavenLocal()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://nexus.endrealm.net/repository/maven-snapshots/") {
        name = "01"
    }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.dom:$kotlinxDomVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
    implementation("org.cadixdev:mercury:$mercuryVersion")
    implementation("org.cadixdev:lorenz:$lorenzVersion")
    implementation("org.cadixdev:lorenz-io-proguard:$lorenzVersion")
    implementation("org.cadixdev:lorenz-asm:$lorenzVersion")
    implementation("org.cadixdev:at:$at")
    implementation("org.cadixdev:atlas:$atlasVersion")
    implementation("org.cadixdev:bombe:$bombeVersion")
    implementation("org.eclipse.jdt:org.eclipse.jdt.core:$eclipsejdt")


}

gradlePlugin {
    plugins {
        register("ToothPick") {
            id = "toothpick"
            implementationClass = "ToothPick"
        }
    }
}
