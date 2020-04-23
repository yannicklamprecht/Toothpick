rootProject.name = "toothpick"

setupSubproject("toothpick-api") {
    projectDir = File("Toothpick-API")
    buildFileName = "../subprojects/api.gradle.kts"
}
setupSubproject("toothpick-server") {
    projectDir = File("Toothpick-Server")
    buildFileName = "../subprojects/server.gradle.kts"
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}