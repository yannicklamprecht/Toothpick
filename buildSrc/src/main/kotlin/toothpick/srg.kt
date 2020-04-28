package toothpick

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import stuff.taskGroupPrivate
import java.net.URL
import java.util.function.BiConsumer
import java.util.function.Function

fun srg(project: Project): Task {
    val srg2: Task by project.tasks.creating {
        group = taskGroupPrivate
        doLast {
            val mojangMappings = readMojang(project)
            val spigotMappings = readSpigot(project)
            val merged = hashMapOf<String, ClassInfo>() // obfname, info

            // move mojang mappings into  merged
            mojangMappings.forEach { classInfo ->
                fix(classInfo, merged, Function { it.mojangName }, BiConsumer { i, n -> i.mojangName = n }, true)
            }

            // move spigot mappings into merged
            spigotMappings.forEach { classInfo ->
                fix(classInfo, merged, Function { it.spigotName }, BiConsumer { i, n -> i.spigotName = n }, false)
            }

            // fix missing names by writing obf names
            merged.forEach { (_, classInfo) ->
                // fix missing class name
                if (classInfo.mojangName == "") {
                    classInfo.mojangName = classInfo.obfName
                }
                if (classInfo.spigotName == "") {
                    if(classInfo.obfName.contains("$") && merged.containsKey(classInfo.obfName.substringBefore("$"))) {
                        classInfo.spigotName = merged[classInfo.obfName.substringBefore("$")]!!.spigotName + "$" + classInfo.obfName.substringAfter("$")
                    } else {
                        classInfo.spigotName = "net/minecraft/server/" + classInfo.obfName
                    }
                }
                // fix missing field names
                classInfo.fields.forEach { fieldInfo ->
                    if (fieldInfo.mojangName == "") {
                        fieldInfo.mojangName = fieldInfo.obfName
                    }
                    if (fieldInfo.spigotName == "") {
                        fieldInfo.spigotName = fieldInfo.obfName
                    }
                }
                // fix missing method names
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
                info.fields.forEach { field ->
                    result.append("FD: ").append(info.spigotName).append("/").append(field.spigotName).append(" ").append(info.mojangName).append("/").append(field.mojangName).append("\n")
                }
                info.methods.forEach { method ->
                    // TODO params
                    // MD: net/minecraft/server/Advancement/c ()Lnet/minecraft/server/AdvancementDisplay; net/minecraft/server/Advancement/testMethod
//                    result.append("MD: ").append(info.spigotName).append("/").append(method.spigotName).append(" ").append(method.params).append(" ").append(info.mojangName).append("/").append(method.mojangName).append("\n")
                }
                result.toString()
            }
            project.projectDir.resolve("work/spigotToMojang.srg").writeText(cl.joinToString(""))
        }
    }
    return srg2
}

private fun fix(classInfo: ClassInfo, merged: HashMap<String, ClassInfo>, nameExtractor: Function<Info, String>, nameConsumer: BiConsumer<Info, String>, mojang: Boolean) {
    // search for info in merged
    val info = merged.computeIfAbsent(classInfo.obfName) {
        if (mojang) {
            ClassInfo("", classInfo.mojangName, classInfo.obfName)
        } else {
            ClassInfo(classInfo.spigotName, "", classInfo.obfName)
        }
    }

    // complete info
    if (nameExtractor.apply(info) == "") {
        nameConsumer.accept(info, nameExtractor.apply(classInfo))
    }

    // iterate fields
    classInfo.fields.forEach { newInfo ->
        // try to found existing field, apply name
        var found = false
        info.fields.forEach { oldInfo ->
            if (oldInfo.obfName == newInfo.obfName) {
                if (nameExtractor.apply(oldInfo) == "") {
                    nameConsumer.accept(oldInfo, nameExtractor.apply(newInfo))
                    found = true
                }
            }
        }

        // if not found, create new field
        if (!found) {
            if (mojang) {
                info.fields.add(FieldInfo("", newInfo.mojangName, newInfo.obfName))
            } else {
                info.fields.add(FieldInfo(newInfo.spigotName, "", newInfo.obfName))
            }
        }
    }

    // iterate methods
    classInfo.methods.forEach { newInfo ->
        // try to found existing field, apply name
        var found = false
        info.methods.forEach { oldInfo ->
            // todo compare args too
            if (oldInfo.obfName == newInfo.obfName) {
                if (nameExtractor.apply(oldInfo) == "") {
                    nameConsumer.accept(oldInfo, nameExtractor.apply(newInfo))
                    found = true
                }
            }
        }

        // if not found, create new method
        if (!found) {
            if (mojang) {
                info.methods.add(MethodInfo("", newInfo.mojangName, newInfo.obfName, newInfo.returnType, newInfo.params))
            } else {
                info.methods.add(MethodInfo(newInfo.spigotName, "", newInfo.obfName, newInfo.returnType, newInfo.params))
            }
        }
    }
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
                if(orig.contains("$")) {
                    println("found inner class $orig")
                }
            }
            // method
            else if (line.contains("(")) {
                if (line.contains("init>")) {
                    return@forEach
                }
                val split = line.split(" ");
                val returnType = split[4+0].substringAfterLast(":")
                val orig = split[4+1].substringBefore("(")
                val params = split[4+1].substringAfter("(").substringBefore(")")
                val obf = split[4+3]

                currentClass?.methods?.add(MethodInfo("", orig, obf, returnType, params))
            }
            // field
            else {
                val split = line.split(" ")
                val mojangName = split[4 + 1]
                val obf = split[4 + 3]
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

    // special case
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

open class Info(
        var spigotName: String,
        var mojangName: String,
        val obfName: String
)

class ClassInfo(
        spigotName: String, mojangName: String, obfName: String,
        val fields: ArrayList<FieldInfo> = arrayListOf(),
        val methods: ArrayList<MethodInfo> = arrayListOf()
) : Info(spigotName, mojangName, obfName)

class FieldInfo(spigotName: String, mojangName: String, obfName: String

) : Info(spigotName, mojangName, obfName)

class MethodInfo(
        spigotName: String, mojangName: String, obfName: String,
        val returnType: String,
        val params: String
) : Info(spigotName, mojangName, obfName)
