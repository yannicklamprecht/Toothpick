import org.gradle.api.Project
import java.io.File

object MyProject : Project by ProjectSingleton.project {

    var minecraftVersion: String = "1.15.2"
    var forkName: String = "MiniPaper"
    var upstreamName: String = "Paper2"

    var upstreamPatchCmd: Array<String> = arrayOf("./paper", "patch")
//    var upstreamPatchCmd: Array<String> = arrayOf("nix-shell", "-p", "maven", "--command", "./paper patch")

    val mySubProjects = mapOf(
            // API project
            "$forkName-API" to listOf(
                    File(rootProject.projectDir, "$upstreamName/$upstreamName-API"),
                    project(":${forkName.toLowerCase()}-api").projectDir,
                    File(rootProject.projectDir, "patches/api")
            ),

            // Server project
            "$forkName-Server" to listOf(
                    File(rootProject.projectDir, "$upstreamName/$upstreamName-Server"),
                    project(":${forkName.toLowerCase()}-server").projectDir,
                    File(rootProject.projectDir, "patches/server")
            )
    )
}
