val kotlinxDomVersion = "0.0.10"
val kotlinxSerializationVersion = "0.20.0"

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.3.70"
}

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.dom:$kotlinxDomVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
}

gradlePlugin {
    plugins {
        register("ToothPick") {
            id = "toothpick"
            implementationClass = "ToothPick"
        }
    }
}
