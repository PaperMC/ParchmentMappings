package org.parchmentmc.util

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.name

object JarProcessing {
    interface ClassProcessor {
        fun shouldProcess(file: Path): Boolean = true

        interface NodeBased : ClassProcessor {
            fun processClass(node: ClassNode, classNodeCache: ClassNodeCache)
        }

        interface VisitorBased : ClassProcessor {
            fun processClass(node: ClassNode, parent: ClassVisitor, classNodeCache: ClassNodeCache): ClassVisitor?
        }
    }

    fun processJar(
        jarFile: FileSystem,
        processor: ClassProcessor
    ) {
        val classNodeCache = ClassNodeCache.create(jarFile)

        jarFile.walk().use { stream ->
            stream.forEach { file ->
                processFile(file, classNodeCache, processor)
            }
        }
    }

    private fun processFile(file: Path, classNodeCache: ClassNodeCache, processor: ClassProcessor) {
        if (!file.name.endsWith(".class")) {
            return
        }

        if (processor.shouldProcess(file)) {
            processClass(file, classNodeCache, processor)
        }
    }

    private fun processClass(file: Path, classNodeCache: ClassNodeCache, processor: ClassProcessor) {
        val node = classNodeCache.findClass(file.toString()) ?: return

        val writer = ClassWriter(0)
        val visitor = when (processor) {
            is ClassProcessor.VisitorBased -> processor.processClass(node, writer, classNodeCache) ?: writer
            is ClassProcessor.NodeBased -> {
                processor.processClass(node, classNodeCache)
                writer
            }
            else -> error("Unknown class processor type: ${processor::class.java.name}")
        }
        node.accept(visitor)
    }
}
