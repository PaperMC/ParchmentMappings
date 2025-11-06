package org.parchmentmc.enigma

import cuchaz.enigma.api.service.JarIndexerService
import cuchaz.enigma.api.service.NameProposalService
import cuchaz.enigma.api.service.ProjectService
import cuchaz.enigma.api.view.ProjectView
import cuchaz.enigma.api.view.index.JarIndexView
import cuchaz.enigma.classprovider.ClassProvider
import cuchaz.enigma.translation.mapping.EntryRemapper
import cuchaz.enigma.translation.representation.MethodDescriptor
import cuchaz.enigma.translation.representation.entry.ClassEntry
import cuchaz.enigma.translation.representation.entry.Entry
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry
import cuchaz.enigma.translation.representation.entry.MethodEntry
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.parchmentmc.enigma.util.ClassNodeCache
import org.parchmentmc.enigma.util.contains
import org.parchmentmc.enigma.util.openZip
import java.nio.file.Path
import java.util.*
import javax.lang.model.SourceVersion

class EnigmaNameProposalService(
    private val unobfuscatedJar: Path?
) : JarIndexerService, NameProposalService, ProjectService {

    lateinit var indexer: JarIndexView

    // todo cleanup
    private var unobfuscatedClasses: ClassNodeCache? = null

    fun getUnobfuscatedNodes(): ClassNodeCache? {
        if (unobfuscatedJar != null && unobfuscatedClasses == null) {
            unobfuscatedClasses = ClassNodeCache.create(unobfuscatedJar.openZip())
        }
        return unobfuscatedClasses
    }

    override fun onProjectClose(project: ProjectView) {
        unobfuscatedClasses?.close()
    }

    override fun acceptJar(scope: Set<String>, classProvider: ClassProvider, jarIndex: JarIndexView) {
        indexer = jarIndex
    }

    private fun paramCanConflict(startIndex: Int, methodDesc: MethodDescriptor, param: ClassEntry): Boolean {
        var alreadyFound = false
        for (index in startIndex..methodDesc.argumentDescs.lastIndex) {
            val desc = methodDesc.argumentDescs[index]
            if (desc.containsType() && desc.typeEntry.equals(param)) {
                if (alreadyFound) {
                    return true
                }
                alreadyFound = true
            }
        }
        return false
    }

    private fun isEnumConstructor(method: MethodEntry): Boolean {
        if (!method.isConstructor) {
            return false
        }

        return Opcodes.ACC_ENUM in indexer.entryIndex.getAccess(method.parent)
    }

    private fun extractNameFromUnobfuscatedNode(
        param: LocalVariableEntry, paramIndex: Int,
        method: MethodEntry,
        node: ClassNode
    ): String? {
        for (mojmapMethod in node.methods) {
            if (mojmapMethod.name == method.name && mojmapMethod.desc == method.descriptor) {
                if (mojmapMethod.parameters == null) { // for lambda search in the lvt directly
                    for (lvt in mojmapMethod.localVariables) {
                        if (lvt.index == param.index) {
                            return lvt.name
                        }
                    }
                } else {
                    val paramNode = mojmapMethod.parameters[paramIndex]
                    if (paramNode != null) {
                        return paramNode.name
                    }
                }
                break
            }
        }

        return null
    }

    override fun proposeName(obfEntry: Entry<*>, remapper: EntryRemapper): Optional<String> {
        if (obfEntry is LocalVariableEntry && obfEntry.isArgument) {
            val method = obfEntry.parent
            if (method != null) {
                val isStatic = Opcodes.ACC_STATIC in indexer.entryIndex.getAccess(method)

                var offsetLvtIndex = 0
                if (!isStatic) {
                    offsetLvtIndex++ // (this, ...)
                }
                var descStartIndex = 0 // ignore implicit argument in descriptors for conflict check
                if (isEnumConstructor(method)) {
                    descStartIndex += 2 // (name, ordinal, ...)
                }

                val paramIndex = fromLvtToParamIndex(obfEntry.index, method, offsetLvtIndex)
                if (paramIndex == -1) {
                    return Optional.empty() // happens for faulty param detection (like Player#actuallyHurt)
                }

                val paramDesc = method.desc.argumentDescs[paramIndex]
                var paramDescStr = paramDesc.toString()

                val standardName = KnownTypes.FIXED_NAMES[paramDescStr]
                if (standardName != null) {
                    return Optional.of(standardName)
                }

                val unobfuscatedNodes = getUnobfuscatedNodes()
                val enclosingClass = method.parent
                if (enclosingClass != null && unobfuscatedNodes != null) { // todo cleanup
                    val node = unobfuscatedNodes.findClass(enclosingClass.fullName) ?: error("Cannot find ${enclosingClass.fullName}")
                    extractNameFromUnobfuscatedNode(
                        obfEntry, paramIndex,
                        method,
                        node
                    )?.let { name -> return Optional.of(name) }
                }

                if (!paramDesc.containsType()) { // primitive / array of primitive
                    return Optional.empty()
                }

                if (paramDesc.isArray) {
                    paramDescStr = paramDescStr.drop(paramDesc.arrayDimension) // for array, the element type is often more relevant than the array itself
                }

                var name = KnownTypes.getBestName(paramDescStr)
                if (paramCanConflict(descStartIndex, method.desc, paramDesc.typeEntry)) { // not completely accurate for lambda/inner classes
                    name += (paramIndex + 1)
                }
                if (SourceVersion.isKeyword(name)) {
                    name += '_'
                }

                return Optional.of(name)
            }
        }
        return Optional.empty()
    }

    /**
     * Transform the given LVT index into a parameter index.
     */
    fun fromLvtToParamIndex(lvtIndex: Int, method: MethodEntry, offsetLvtIndex: Int): Int {
        var currentParamIndex = 0
        var currentLvtIndex = offsetLvtIndex

        for (param in method.desc.argumentDescs) {
            if (currentLvtIndex == lvtIndex) {
                return currentParamIndex
            }

            currentParamIndex++
            currentLvtIndex++

            if (param.toString() == "J" || param.toString() == "D") { // long / double take two slots
                currentLvtIndex++
            }
        }
        return -1
    }
}
