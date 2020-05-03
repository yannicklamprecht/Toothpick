var forkName = "MiniPaper"

rootProject.name = forkName

setupSubproject("${forkName.toLowerCase()}-api") {
    projectDir = File("$forkName-API")
    buildFileName = "../subprojects/api.gradle.kts"
}
setupSubproject("${forkName.toLowerCase()}-server") {
//    projectDir = File("$forkName-Server")
    projectDir = File(rootProject.projectDir,"work/Paper-Server-Remapped-Patched")
    buildFileName =  File(rootProject.projectDir, "subprojects/server.gradle.kts").toRelativeString(projectDir)
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
