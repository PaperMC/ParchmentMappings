package org.parchmentmc.enigma.util

import org.objectweb.asm.tree.ClassNode
import java.nio.file.FileSystem

interface ClassNodeCache : AutoCloseable {
    fun findClass(name: String?): ClassNode?

    companion object {
        fun create(
            jarFile: FileSystem
        ): ClassNodeCache {
            return ClassNodeCacheImpl(jarFile)
        }
    }
}
