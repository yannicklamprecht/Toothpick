package toothpick

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import stuff.taskGroupPrivate
import java.lang.StringBuilder
import java.net.URL

fun srg(project: Project): Task {
    val srg2: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            val mojangMappings = readMojang(project)
            val spigotMappings = readSpigot(project)
            val merged = hashMapOf<String, ClassInfo>() // obfname, info

            mojangMappings.forEach { classInfo ->
                // search for info in merged
                val info = merged.computeIfAbsent(classInfo.obfName) {
                    ClassInfo("", classInfo.mojangName, classInfo.obfName)
                }

                // complete info
                if (info.mojangName == "") {
                    info.mojangName = classInfo.mojangName
                }

                // iterate fields
                classInfo.fields.forEach {
                    // TODO fields
                }

                // iterate methods
                // TODO methods
            }

            spigotMappings.forEach { classInfo ->
                // search for info in merged
                val info = merged.computeIfAbsent(classInfo.obfName) {
                    ClassInfo(classInfo.spigotName, "", classInfo.obfName)
                }

                // complete info
                if (info.spigotName == "") {
                    info.spigotName = classInfo.spigotName
                }

                // iterate fields
                classInfo.fields.forEach {
                    // TODO fields
                }

                // iterate methods
                // TODO methods
            }

            merged.forEach { (_, classInfo) ->
                // fix missing class name
                if (classInfo.mojangName == "") {
                    classInfo.mojangName = classInfo.obfName
                }
                if (classInfo.spigotName == "") {
                    classInfo.spigotName = classInfo.obfName
                }
                // fix missing method names
                classInfo.fields.forEach { fieldInfo ->
                    if (fieldInfo.mojangName == "") {
                        fieldInfo.mojangName = fieldInfo.obfName
                    }
                    if (fieldInfo.spigotName == "") {
                        fieldInfo.spigotName = fieldInfo.obfName
                    }
                }
                // fix missing field names
                classInfo.methods.forEach { methodInfo ->
                    if (methodInfo.mojangName == "") {
                        methodInfo.mojangName = methodInfo.obfName
                    }
                    if (methodInfo.spigotName == "") {
                        methodInfo.spigotName = methodInfo.obfName
                    }
                }
            }

            val cl = merged.map { (_, info) ->
                val result = StringBuilder()
                result.append("CL: ").append(info.spigotName).append(" ").append(info.mojangName).append("\n")
                info.fields.forEach {
                    // TODO fields
                }
                info.methods.forEach {
                    // TODO methods
                }
                result.toString()
            }.sorted()
            project.projectDir.resolve("work/spigotToMojang.srg").writeText(cl.joinToString(""))
        }
    }
    return srg2
}

fun readMojang(project: Project): MutableCollection<ClassInfo> {
    val file = URL("https://launcher.mojang.com/v1/objects/59c55ae6c2a7c28c8ec449824d9194ff21dc7ff1/server.txt").readText()

    val classes = arrayListOf<ClassInfo>()
    var currentClass: ClassInfo? = null
    file.split("\n").forEach { line ->
        try {
            // comment
            if (line.startsWith("#") || line.contains("package-info") || line == "") {
                return@forEach
            }
            // class
            else if (line.endsWith(":")) {
                if (currentClass != null) {
                    classes.add(currentClass!!)
                }

                val middle = line.indexOf(" -> ")
                val orig = line.substring(0, middle).replace(".", "/")
                val obf = line.substring(middle + 4, line.length - 1).replace(".", "/")
                currentClass = ClassInfo("", orig, obf)
            }
            // method
            else if (line.contains("(")) {
                if (line.contains("init>")) {
                    return@forEach
                }
                val split = line.split(" ");
                val returnType = split[0].substringAfterLast(":")
                val orig = split[1].substringBefore("(")
                val params = split[1].substringAfter("(").substringBefore(")")
                val obf = split[3]

                currentClass?.methods?.add(MethodInfo("", orig, obf, returnType, params))
            }
            // field
            else {
                val split = line.split(" ")
                val mojangName = split[1]
                val obf = split[3]
                currentClass?.fields?.add(FieldInfo("", mojangName, obf))
            }
        } catch (ex: Exception) {
            project.logger.warn("Error while parsing line '$line': ", ex)
        }
    }
    return classes
}

fun readSpigot(project: Project): MutableCollection<ClassInfo> {
    val classesFile = project.projectDir.resolve("Paper/work/BuildData/mappings/bukkit-1.15.2-cl.csrg").readLines()

    val classes = hashMapOf<String, ClassInfo>()
    classesFile.forEach { line ->
        try {
            // comment
            if (line.startsWith("#")) {
                return@forEach
            }

            val split = line.split(" ")
            val obf = split[0].replace(".", "/")
            val spigot = split[1].replace(".", "/")
            classes[spigot] = (ClassInfo("net/minecraft/server/$spigot", "", obf))
        } catch (ex: Exception) {
            project.logger.warn("Error while parsing line '$line': ", ex)
        }
    }

    classes["net/minecraft/server/MinecraftServer"] = ClassInfo("net/minecraft/server/MinecraftServer", "net/minecraft/server/MinecraftServer", "net/minecraft/server/MinecraftServer")

    val memberFile = project.projectDir.resolve("Paper/work/BuildData/mappings/bukkit-1.15.2-members.csrg").readLines()
    memberFile.forEach { line ->
        try {
            // comment
            when {
                line.startsWith("#") -> return@forEach
                // method
                line.contains("(") -> {
                    val split = line.split(" ")
                    val className = split[0]
                    val obf = split[1]
                    val type = split[2]
                    val spigot = split[3]
                    val params = type.substring(1, type.indexOf(")"))
                    val returnType = type.substring(type.indexOf(")") + 1, type.length - 1)
                    classes[className]!!.methods.add(MethodInfo(spigot, "", obf, returnType, params))
                }
                // field
                else -> {
                    val split = line.split(" ")
                    val className = split[0]
                    val obf = split[1]
                    val spigot = split[2]
                    classes[className]!!.fields.add(FieldInfo(spigot, "", obf))
                }
            }
        } catch (ex: Exception) {
            project.logger.warn("Error while parsing line '$line': ", ex)
        }
    }

    return classes.values
}

data class ClassInfo(
        var spigotName: String,
        var mojangName: String,
        val obfName: String,
        val fields: ArrayList<FieldInfo> = arrayListOf(),
        val methods: ArrayList<MethodInfo> = arrayListOf()
)

data class FieldInfo(
        var spigotName: String,
        var mojangName: String,
        val obfName: String
)

data class MethodInfo(
        var spigotName: String,
        var mojangName: String,
        val obfName: String,
        val returnType: String,
        val params: String
)
