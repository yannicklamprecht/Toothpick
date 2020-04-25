import org.gradle.api.*
import org.gradle.kotlin.dsl.findByType

val Project.toothPick get() = rootProject.extensions.findByType(ToothPickExtension::class)!!
