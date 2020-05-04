val kotlinxDomVersion = "0.0.10"
val kotlinxSerializationVersion = "0.20.0"
val mercuryVersion = "0.1.0-SNAPSHOT"
val lorenzVersion = "0.6.0-SNAPSHOT"
val atlasVersion = "0.2.1-SNAPSHOT"
val bombeVersion = "0.4.0"
val asmVersion = "7.3.1"
val lorenzTinyVersion = "1.0.0+build.1"
val tinyMappingsParserVersion = "0.2.1.13"
val guavaVersion = "29.0-jre"

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.3.70"
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://maven.fabricmc.net/")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.dom:$kotlinxDomVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
    implementation("org.cadixdev:mercury:$mercuryVersion")
    implementation("org.cadixdev:lorenz:$lorenzVersion")
    implementation("org.cadixdev:lorenz-io-proguard:$lorenzVersion")
    implementation("org.cadixdev:atlas:$atlasVersion")
    implementation("org.cadixdev:bombe:$bombeVersion")
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")
//    implementation("net.fabricmc:lorenz-tiny:$lorenzTinyVersion")
//    implementation("net.fabricmc:tiny-mappings-parser:$tinyMappingsParserVersion")
//    implementation("com.google.guava:guava:$guavaVersion")
}

gradlePlugin {
    plugins {
        register("ToothPick") {
            id = "toothpick"
            implementationClass = "ToothPick"
        }
    }
}
