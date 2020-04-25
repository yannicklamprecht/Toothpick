import org.gradle.api.*
import org.gradle.kotlin.dsl.findByType

val Project.minipaper get() = rootProject.extensions.findByType(MiniPaperExtension::class)!!
