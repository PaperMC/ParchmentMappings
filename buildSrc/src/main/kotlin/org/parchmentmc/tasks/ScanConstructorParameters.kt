package org.parchmentmc.tasks

import org.cadixdev.lorenz.MappingSet
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.parchmentmc.util.*
import org.parchmentmc.util.lorenz.FabricEnigmaReader
import java.lang.constant.ConstantDescs

abstract class ScanInitParamsJar : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputJar: RegularFileProperty

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputMapping: DirectoryProperty

    @TaskAction
    fun run() {
        val set: MappingSet?
        if (inputMapping.isPresent) {
            set = MappingSet.create()
            inputMapping.asFileTree.filter { file -> file.name.endsWith(".mapping") }
                .forEach { file ->
                    FabricEnigmaReader(file.bufferedReader(Charsets.UTF_8)).use { reader ->
                        reader.read(set)
                    }
                }
        } else {
            set = null
        }

        inputJar.path.openZip().use { jar ->
            JarProcessing.processJar(
                jar,
                ScanParameterProcessor(set)
            )
        }
    }

    private class ScanParameterProcessor(private val mapping: MappingSet?) : JarProcessing.ClassProcessor.NodeBased, AsmUtil {

        override fun processClass(node: ClassNode, classNodeCache: ClassNodeCache) {
            if (Opcodes.ACC_RECORD in node.access || Opcodes.ACC_SYNTHETIC in node.access) {
                return
            }

            node.methods.forEach { method ->
                if (method.name == ConstantDescs.INIT_NAME) {
                    if (method.parameters == null && Type.getArgumentTypes(method.desc).size == 0) {
                        return@forEach
                    }

                    val expectedNames = sortedMapOf<Int, String>()
                    val insns = method.instructions.iterator()
                    val locals = mutableSetOf<Int>()
                    while (insns.hasNext()) {
                        val insn = insns.next()
                        if (insn.opcode >= Opcodes.ISTORE && insn.opcode <= Opcodes.ASTORE) {
                            locals.add((insn as VarInsnNode).`var`)
                            continue
                        }

                        if (insn.previous != null && (insn.previous.opcode >= Opcodes.ILOAD && insn.previous.opcode <= Opcodes.ALOAD)) {
                            val index = (insn.previous as VarInsnNode).`var`
                            if (index != 0 && !locals.contains(index)) { // skip this and lvt assignment
                                if (insn.opcode == Opcodes.PUTFIELD && (insn as FieldInsnNode).owner == node.name) {
                                    if (insn.name.startsWith("this$") || insn.name.startsWith("val$")) { // skip outer class reference from nested class/method
                                        continue
                                    }
                                    expectedNames[index] = insn.name
                                }
                            }
                        }
                    }

                    val currentNames: Map<Int, String>
                    if (mapping == null && method.parameters != null) {
                        currentNames =
                            method.parameters
                                .filter { node -> !(Opcodes.ACC_MANDATED in node.access || Opcodes.ACC_SYNTHETIC in node.access) }
                                .mapIndexed { index, node ->
                                    fromParamToLvtIndex(index, method) to node.name
                                }
                                .toMap()
                    } else if (mapping != null) {
                        currentNames = mapping.getClassMapping(node.name)
                            .flatMap { it.getMethodMapping(method.name, method.desc) }
                            .map { method ->
                                method.parameterMappings
                                    .associate { it.index to it.deobfuscatedName }
                            }.orElseGet { mapOf() }
                    } else {
                        currentNames = mapOf()
                    }
                    if (currentNames != expectedNames && currentNames.size <= expectedNames.size) {
                        println(node.name)
                        println(method.name + " " + method.desc)
                        println("current : $currentNames")
                        println("expected: $expectedNames")
                        expectedNames.forEach { entry ->
                            println("ARG ${entry.key} ${entry.value}")
                        }
                    }
                }
            }
        }
    }
}
