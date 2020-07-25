import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.kotlin.dsl.*

class ToothPick : Plugin<Project> {

    override fun apply(target: Project) {
        val toothPickExtension = target.extensions.create<ToothPickExtension>("toothpick", target.objects)
        target.initToothPickTasks(toothPickExtension)
    }
}


