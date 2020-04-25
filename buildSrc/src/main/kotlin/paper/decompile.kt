package paper

import cmd
import ensureSuccess

import org.gradle.api.GradleException
import java.io.File
import java.net.URL


fun decompile() {
    downloadVersionData()
    // TODO fix libs
//    downloadLibs()
    extractClasses()
    decomp()
}

private fun downloadVersionData() {
    val file = File(versionjson)
    if (!file.exists()) {
        try {
            logger.info("Downloading $minecraftversion JSON Data")
            val escaped = minecraftversion.replace("\\-pre", " Pre-Release").replace("\\.", "\\\\.")
            val urlEncoded = escaped.replace(" ", "_")
            val manifest = URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").readText()
            val url = "https://" + manifest.substringAfter("{\"id\": \"${escaped}\",").substringBefore("$urlEncoded.json").substringAfter("https://") + "$urlEncoded.json"
            logger.info("$versionjson - $url")
            file.writeText(URL(url).readText())
        } catch (e: Exception) {
            throw GradleException("Failed to download the version.json.", e)
        }
    }
}

private fun downloadLibs() {
    val group = "com.mojang"
    val groupEsp = "com\\.mojang"
    val groupPath = "com/mojang"
    val libPath = "$decompiledir/libraries/$group/"
    val libDir = File(libPath)
    libDir.mkdirs()

    for (lib in listOf("datafixerupper", "authlib", "brigadier")) {
        val jarPath = "$libPath/${lib}-sources.jar"
        val destPath = "$libPath/${lib}"
        val jarFile = File(jarPath)
        val destFile = File(destPath)

        if (!jarFile.exists()) {
            //libesc=$(echo ${lib} | sed 's/\./\\]./g')
            //            ver=$(grep -oE "${groupesc}:${libesc}:[0-9\.]+" "$versionjson" | sed "s/${groupesc}:${libesc}://g")
            //            echo "Downloading ${group}:${lib}:${ver} Sources"
            //            curl -s -o "$jar" "https://libraries.minecraft.net/${grouppath}/${lib}/${ver}/${lib}-${ver}-sources.jar"
            //            set +e
            //            grep "<html>" "$jar" && grep -oE "<title>.*?</title>" "$jar" && rm "$jar" && echo "Failed to download $jar" && exit 1
            //            set -e
        }

        if (!destFile.exists()) {
            logger.info("Extracting $group:$lib Sources")
            destFile.mkdirs()
            ensureSuccess(cmd("jar", "xf", "\"$jarPath\"", directory = destFile))
        }
    }
}

private fun extractClasses() {
    logger.info("Extracting NMS classes...")
    val classFile = File(classDir);
    if (!classFile.exists()) {
        classFile.mkdirs()
        ensureSuccess(cmd("jar", "xf", "$decompiledir/$minecraftversion-mapped.jar", "net/minecraft/server", directory = classFile, printToStdout = true))
    }
}

private fun decomp() {
    val spigotDecompFolder = File("$spigotDecompDir/");
    spigotDecompFolder.mkdirs()

    // if we see the old net folder, copy it to spigot to avoid redecompiling
    val oldNetFolder = File("$decompiledir/net")
    if (oldNetFolder.exists()) {
        oldNetFolder.copyRecursively(spigotDecompFolder)
    }

    val newNetFolder = File("$spigotDecompDir/net");
    if(!newNetFolder.exists()) {
        logger.info("Decompiling classes...")
        try {
            ensureSuccess(cmd("java", "-jar" ,"$workdir/BuildData/bin/fernflower.jar", "-dgs=1", "-hdc=0", "-asc=1", "-udv=0", "-rsy=1", "-aoa=1", classDir, spigotDecompDir, printToStdout = true))
        } catch (e: IllegalStateException) {
            newNetFolder.deleteRecursively()
            throw GradleException("Failed to decompile classes.", e)
        }
    }
}
