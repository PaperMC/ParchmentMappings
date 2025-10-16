package org.parchmentmc.enigma

import cuchaz.enigma.analysis.index.JarIndex
import cuchaz.enigma.api.service.JarIndexerService
import cuchaz.enigma.api.service.NameProposalService
import cuchaz.enigma.classprovider.ClassProvider
import cuchaz.enigma.translation.mapping.EntryRemapper
import cuchaz.enigma.translation.representation.MethodDescriptor
import cuchaz.enigma.translation.representation.entry.ClassEntry
import cuchaz.enigma.translation.representation.entry.Entry
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry
import cuchaz.enigma.translation.representation.entry.MethodEntry
import java.util.*
import javax.lang.model.SourceVersion

class EnigmaNameProposalService() : JarIndexerService, NameProposalService {

    lateinit var indexer: JarIndex

    val suggestions = mapOf(
        // client
        "Lcom/mojang/blaze3d/vertex/VertexConsumer;" to "consumer",
        "Lnet/minecraft/client/renderer/MultiBufferSource;" to "bufferSource",
        "Lnet/minecraft/client/renderer/SubmitNodeCollector;" to "nodeCollector",
        "Lnet/minecraft/client/renderer/SubmitNodeCollection;" to "nodeCollection",
        "Lnet/minecraft/client/renderer/SubmitNodeStorage;" to "nodeStorage",
        "Lnet/minecraft/client/gui/ActiveTextCollector;" to "textCollector",
        "Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;" to "interpolator",
        "Lnet/minecraft/client/gui/TextAlignment;" to "alignment",
        "Lnet/minecraft/util/FormattedCharSequence;" to "text",
        "Lnet/minecraft/client/gui/navigation/ScreenRectangle;" to "rectangle",
        // server
        "Lcom/mojang/authlib/GameProfile;" to "profile",

        "Lnet/minecraft/world/item/ItemStack;" to "stack",
        "Lnet/minecraft/world/level/ItemLike;" to "item",
        "Lnet/minecraft/world/level/block/state/BlockState;" to "state",
        "Lnet/minecraft/core/BlockPos;" to "pos",
        $$"Lnet/minecraft/core/BlockPos$MutableBlockPos;" to "pos",
        "Lnet/minecraft/world/entity/EquipmentSlot;" to "slot",
        "Lnet/minecraft/resources/ResourceLocation;" to "location",
        "Lnet/minecraft/world/entity/ContainerUser;" to "user",
        "Lnet/minecraft/world/effect/MobEffectInstance;" to "effectInstance",

        "Lnet/minecraft/server/level/WorldGenRegion;" to "region",
        "Lnet/minecraft/server/level/ServerLevel;" to "level",
        "Lnet/minecraft/world/level/LevelReader;" to "level",
        "Lnet/minecraft/world/level/LevelHeightAccessor;" to "level",
        "Lnet/minecraft/world/level/BlockGetter;" to "level",
        // "Lnet/minecraft/world/level/CollisionGetter;" to "level",

        "Lnet/minecraft/server/level/ServerPlayer;" to "player",
        "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;" to "nautilus",
        "Lnet/minecraft/world/entity/projectile/AbstractArrow;" to "arrow",
        "Lnet/minecraft/world/entity/monster/AbstractSkeleton;" to "skeleton",
        "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;" to "horse",
        "Lnet/minecraft/world/entity/HumanoidArm;" to "arm",
        "Lnet/minecraft/util/debug/DebugValueAccess;" to "valueAccess",
    )

    override fun acceptJar(scope: Set<String>, classProvider: ClassProvider, jarIndex: JarIndex) {
        indexer = jarIndex
    }

    private fun paramCanConflict(startIndex: Int, methodDesc: MethodDescriptor, entry: ClassEntry): Boolean {
        var alreadyFound = false
        for (index in startIndex..methodDesc.argumentDescs.lastIndex) {
            val desc = methodDesc.argumentDescs[index]
            if (desc.containsType() && desc.typeEntry.equals(entry)) {
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

        return indexer.entryIndex.getClassAccess(method.parent)?.isEnum ?: false
    }

    override fun proposeName(obfEntry: Entry<*>, remapper: EntryRemapper): Optional<String> {
        if (obfEntry is LocalVariableEntry && obfEntry.isArgument) {
            val parent = obfEntry.parent
            if (parent != null && !parent.name.startsWith("lambda$")) { // todo handle lambda
                val isStatic = indexer.entryIndex.getMethodAccess(parent)?.isStatic ?: false

                var offsetLvtIndex = 0
                if (!isStatic) {
                    offsetLvtIndex++ // (this, ...)
                }
                var descStartIndex = 0 // ignore implicit argument in descriptors for conflict check
                if (isEnumConstructor(parent)) {
                    descStartIndex += 2 // (name, ordinal, ...)
                }

                val paramIndex = fromLvtToParamIndex(obfEntry.index, parent, offsetLvtIndex)
                if (paramIndex == -1) {
                    return Optional.empty() // happens for faulty param detection (like Player#actuallyHurt)
                }

                val desc = parent.desc.argumentDescs[paramIndex]
                if (desc.isPrimitive) {
                    return Optional.empty()
                }

                val strDesc = desc.toString()
                var name = suggestions[strDesc] ?: strDesc.substringAfterLast('/').substringAfterLast('$').dropLast(1).replaceFirstChar { it.lowercase() } // relevant type
                if (paramCanConflict(descStartIndex, parent.desc, desc.typeEntry)) {
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

    /*
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
