var forkName = "MiniPaper"

rootProject.name = forkName

setupSubproject("${forkName.toLowerCase()}-api") {
    projectDir = File("$forkName-API")
    buildFileName = "../subprojects/api.gradle.kts"
}
setupSubproject("${forkName.toLowerCase()}-server") {
    projectDir = File("$forkName-Server")
    buildFileName = "../subprojects/server.gradle.kts"
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
