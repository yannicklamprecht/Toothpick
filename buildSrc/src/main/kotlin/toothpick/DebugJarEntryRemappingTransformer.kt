package toothpick

import org.cadixdev.bombe.jar.JarClassEntry
import org.cadixdev.bombe.jar.asm.JarEntryRemappingTransformer
import org.cadixdev.lorenz.asm.LorenzRemapper

class DebugJarEntryRemappingTransformer(private val lorenzRemapper: LorenzRemapper) : JarEntryRemappingTransformer(lorenzRemapper) {

    override fun transform(entry: JarClassEntry?): JarClassEntry {
        return try {
            synchronized(lorenzRemapper) {
                super.transform(entry)
            }
        } catch (ex : Throwable) {
            println("error for " + entry?.name)
            ex.printStackTrace()
            entry!!
        }
    }
}
