package org.parchmentmc.enigma.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.nio.file.FileSystem
import kotlin.io.path.exists
import kotlin.io.path.readBytes

class ClassNodeCacheImpl(
    private val jarFile: FileSystem
) : ClassNodeCache {

    private val classNodeMap = hashMapOf<String, ClassNode?>()

    override fun findClass(name: String?): ClassNode? { // todo handle concurrent call
        if (name == null) {
            return null
        }
        return classNodeMap.computeIfAbsent(normalize(name)) { fileName ->
            val classData = findClassData(fileName) ?: return@computeIfAbsent null
            val classReader = ClassReader(classData)
            val node = ClassNode(Opcodes.ASM9)
            classReader.accept(node, ClassReader.SKIP_FRAMES)
            return@computeIfAbsent node
        }
    }

    private fun findClassData(className: String): ByteArray? {
        jarFile.getPath(className).let { remappedClass ->
            if (remappedClass.exists()) {
                return remappedClass.readBytes()
            }
        }
        return null
    }

    private fun normalize(name: String): String {
        val workingName = name.removeSuffix(".class")

        var startIndex = 0
        var endIndex = workingName.length
        if (workingName.startsWith('L')) {
            startIndex = 1
        }
        if (workingName.endsWith(';')) {
            endIndex--
        }

        return workingName.substring(startIndex, endIndex).replace('.', '/') + ".class"
    }

    override fun close() {
        jarFile.close()
    }
}
