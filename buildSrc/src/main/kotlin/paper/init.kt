package paper

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import java.io.File

lateinit var basedir: String
lateinit var workdir: String

lateinit var minecraftversion: String
lateinit var minecraftserverurl: String
lateinit var minecrafthash: String

lateinit var accesstransforms: String
lateinit var classmappings: String
lateinit var membermappings: String
lateinit var packagemappings: String

lateinit var decompiledir: String
lateinit var jarpath: String
lateinit var versionjson: String
lateinit var classDir: String
lateinit var spigotDecompDir: String
lateinit var cbDir: String
lateinit var spigotDir: String
lateinit var nmsDir: String

lateinit var codePath: String

var libDownloads = hashMapOf<String, String>()

@UnstableDefault
fun init(project: Project, upstreamName: String) {
    basedir = "${project.rootDir.absolutePath}/work/${upstreamName}"
    workdir = "$basedir/work"
    val info = Json.parse(BuildInfo.serializer(), File("$workdir/BuildData/info.json").readText())

    minecraftversion = info.minecraftVersion;
    minecraftserverurl = info.serverUrl;
    minecrafthash = info.minecraftHash;

    accesstransforms = "$workdir/BuildData/mappings/" + info.accessTransforms
    classmappings = "$workdir/BuildData/mappings/" + info.classMappings
    membermappings = "$workdir/BuildData/mappings/" + info.memberMappings
    packagemappings = "$workdir/BuildData/mappings/" + info.packageMappings

    decompiledir = "$workdir/Minecraft/$minecraftversion"
    jarpath = "$decompiledir/$minecraftversion"
    versionjson = "$workdir/Minecraft/$minecraftversion/$minecraftversion.json"
    classDir = "$decompiledir/classes"
    spigotDecompDir = "$decompiledir/spigot"
    cbDir = "$workdir/CraftBukkit"
    spigotDir = "$workdir/Spigot"
    nmsDir = "$spigotDecompDir/net/minecraft/server"

    codePath = "src/main/java/net/minecraft/server"

    File(decompiledir).mkdirs()
}

@Serializable
private data class BuildInfo(val minecraftVersion: String,
                             val serverUrl: String,
                             val minecraftHash: String,
                             val accessTransforms: String,
                             val classMappings: String,
                             val memberMappings: String,
                             val packageMappings: String,
                             val classMapCommand: String,
                             val memberMapCommand: String,
                             val finalMapCommand: String,
                             val decompileCommand: String,
                             val toolsVersion: Int)
