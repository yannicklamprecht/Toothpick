package toothpick

import org.cadixdev.at.AccessChange
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.ModifierChange
import org.cadixdev.bombe.jar.JarClassEntry
import org.cadixdev.bombe.jar.JarEntryTransformer
import org.cadixdev.bombe.type.signature.MethodSignature
import org.objectweb.asm.*
import java.lang.reflect.Modifier


class AccessTransformingJarEntryTransformer(private val ats: AccessTransformSet) : JarEntryTransformer {

    override fun transform(entry: JarClassEntry): JarClassEntry? {
        val reader = ClassReader(entry.contents)
        val writer = ClassWriter(reader, 0)
        try{
            reader.accept(ATClassVisitor(ats, writer), 0)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return JarClassEntry(entry.name, entry.time, writer.toByteArray())
    }

    class ATClassVisitor(private val ats: AccessTransformSet, writer: ClassWriter) : ClassVisitor(Opcodes.ASM7, writer) {

        private lateinit var className: String

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            try {
                className = name!!

                synchronized(ats) {
                    val klass = ats.getOrCreateClass(className)

                    var newAccess = visit(access, klass.get().access, klass.get().final, null);
//                    if (className.contains("PacketPlayOutPlayerInfo\$PlayerInfoData")) {
//                        newAccess = newAccess or Modifier.STATIC
//                    }

                    return super.visit(version, newAccess, name, signature, superName, interfaces)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            return super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitField(access: Int, name: String?, descriptor: String?, signature: String?, value: Any?): FieldVisitor {
            try {
                synchronized(ats) {
                    val klass = ats.getOrCreateClass(className)
                    val field = klass.getField(name)
                    return super.visitField(visit(access, field.access, field.final, name), name, descriptor, signature, value)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            try {
                synchronized(ats) {
                    val klass = ats.getOrCreateClass(className)
                    val method = klass.getMethod(MethodSignature.of(name, descriptor))
                    return super.visitMethod(visit(access, method.access, method.final, null), name, descriptor, signature, exceptions)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        private fun visit(access: Int, accessChange: AccessChange, modifier: ModifierChange, field: String?): Int {
            var newAccess = access
            val packetOverride = (field != null && className.substringAfterLast("/").startsWith("Packet"))
            if (accessChange == AccessChange.PUBLIC || packetOverride) {
                newAccess = access or Modifier.PUBLIC
                if (newAccess and Modifier.PROTECTED != 0) {
                    newAccess = newAccess and Modifier.PROTECTED.inv()
                }
                if (newAccess and Modifier.PRIVATE != 0) {
                    newAccess = newAccess and Modifier.PRIVATE.inv()
                }
            }
            if (modifier == ModifierChange.REMOVE) {
                newAccess = newAccess and Modifier.FINAL.inv()
            }
            return newAccess
        }
    }
}
