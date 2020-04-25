import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.kotlin.dsl.*

class MiniPaper : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.create<MiniPaperExtension>("minipaper", target.objects)
    }
}


