import java.io.File
import java.util.*

fun cmd(vararg args: String, directory: File, printToStdout: Boolean = false): Pair<Int, String?> {
//    MyProject.logger.info("running " + args.joinToString(" "))
    val p = ProcessBuilder()
            .command(*args)
            .redirectErrorStream(true)
            .directory(directory)
            .start()
    val output = p.inputStream.bufferedReader().use {
        val lines = LinkedList<String>()
        it.lines().peek(lines::add).forEach { line ->
            if (printToStdout) {
                println(line) // TODO use a logger here printToStdout=true -> lifecycle, else info (or lower)
            }
        }
        lines.joinToString(separator = "\n")
    }
    val exit = p.waitFor()
    return exit to output
}

fun ensureSuccess(cmd: Pair<Int, String?>): String? {
    val (exit, output) = cmd
    if (exit != 0) {
        println(output)
        throw IllegalStateException("Failed to run command, exit code is $exit")
    }
    return output
}

fun bytesToHex(bytes: ByteArray): String {
    val out = StringBuilder()
    bytes.forEach {
        out.append(String.format("%02X", it))
    }
    return out.toString()
}
