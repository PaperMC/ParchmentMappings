package org.parchmentmc.enigma

import cuchaz.enigma.api.service.JarIndexerService
import cuchaz.enigma.api.service.NameProposalService
import cuchaz.enigma.api.view.index.JarIndexView
import cuchaz.enigma.classprovider.ClassProvider
import cuchaz.enigma.translation.mapping.EntryRemapper
import cuchaz.enigma.translation.representation.MethodDescriptor
import cuchaz.enigma.translation.representation.entry.ClassEntry
import cuchaz.enigma.translation.representation.entry.Entry
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry
import cuchaz.enigma.translation.representation.entry.MethodEntry
import java.util.*
import javax.lang.model.SourceVersion

const val ACC_STATIC = 0x0008
const val ACC_ENUM = 0x4000

class EnigmaNameProposalService() : JarIndexerService, NameProposalService {

    lateinit var indexer: JarIndexView

    val suggestions = mapOf(
        // client
        "Lcom/mojang/blaze3d/vertex/VertexConsumer;" to "consumer",
        "Lcom/mojang/blaze3d/platform/NativeImage;" to "image",
        "Lcom/mojang/blaze3d/pipeline/RenderPipeline;" to "pipeline",
        "Lnet/minecraft/client/renderer/MultiBufferSource;" to "bufferSource",
        "Lnet/minecraft/client/renderer/SubmitNodeCollector;" to "nodeCollector",
        "Lnet/minecraft/client/renderer/SubmitNodeCollection;" to "nodeCollection",
        "Lnet/minecraft/client/renderer/SubmitNodeStorage;" to "nodeStorage",
        "Lnet/minecraft/client/gui/ActiveTextCollector;" to "textCollector",
        "Lnet/minecraft/client/gui/TextAlignment;" to "alignment",
        "Lnet/minecraft/util/FormattedCharSequence;" to "text",
        "Lnet/minecraft/client/gui/navigation/ScreenRectangle;" to "rectangle",
        $$"Lnet/minecraft/network/chat/FontDescription$AtlasSprite;" to "sprite",
        "Lnet/minecraft/client/resources/model/MaterialSet;" to "materials",
        "Lnet/minecraft/client/particle/SpriteSet;" to "sprites",
        "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;" to "sprite",
        "Lnet/minecraft/client/renderer/texture/MipmapStrategy;" to "strategy",
        "Lnet/minecraft/client/multiplayer/ClientLevel;" to "level",
        "Lnet/minecraft/client/player/AbstractClientPlayer;" to "player",
        "Lnet/minecraft/client/input/KeyEvent;" to "event",
        "Lnet/minecraft/client/input/CharacterEvent;" to "event",
        "Lnet/minecraft/client/input/MouseButtonEvent;" to "event",
        "Lnet/minecraft/client/input/MouseButtonInfo;" to "buttonInfo",

        // server
        "Lcom/mojang/authlib/GameProfile;" to "profile",
        "Lnet/minecraft/world/item/component/ResolvableProfile;" to "profile",
        "Lcom/mojang/brigadier/CommandDispatcher;" to "dispatcher",
        "Lcom/mojang/brigadier/context/CommandContext;" to "context",
        "Lnet/minecraft/commands/CommandBuildContext;" to "buildContext",
        "Lnet/minecraft/commands/CommandSourceStack;" to "source",
        "Lcom/mojang/brigadier/StringReader;" to "reader",
        "Lcom/mojang/serialization/DynamicOps;" to "ops",
        "Lnet/minecraft/resources/RegistryOps;" to "ops",

        "Lnet/minecraft/world/item/ItemStack;" to "stack",
        "Lnet/minecraft/world/level/ItemLike;" to "item",
        "Lnet/minecraft/world/level/block/state/BlockState;" to "state",

        "Lnet/minecraft/core/BlockPos;" to "pos",
        $$"Lnet/minecraft/core/BlockPos$MutableBlockPos;" to "pos",

        "Lnet/minecraft/world/entity/EquipmentSlot;" to "slot",
        "Lnet/minecraft/resources/ResourceKey;" to "key",
        "Lnet/minecraft/resources/ResourceLocation;" to "location",
        "Lnet/minecraft/world/entity/ContainerUser;" to "user",
        "Lnet/minecraft/world/effect/MobEffectInstance;" to "effectInstance",
        "Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;" to "interpolator",
        "Lnet/minecraft/network/chat/FormattedText;" to "text",
        "Lnet/minecraft/sounds/SoundSource;" to "source",
        "Lnet/minecraft/nbt/CompoundTag;" to "tag",
        "Lnet/minecraft/nbt/NbtAccounter;" to "accounter",
        "Lnet/minecraft/sounds/SoundEvent;" to "sound",
        "Lnet/minecraft/util/profiling/ProfilerFiller;" to "profiler",
        "Lnet/minecraft/server/permissions/PermissionSet;" to "permissions",
        "Lnet/minecraft/world/phys/shapes/CollisionContext;" to "context",
        "Lnet/minecraft/world/phys/shapes/VoxelShape;" to "shape",
        "Lnet/minecraft/util/RandomSource;" to "random",
        "Lnet/minecraft/world/entity/MobCategory;" to "category",
        "Lnet/minecraft/world/level/block/state/pattern/BlockInWorld;" to "block",
        "Lnet/minecraft/core/component/DataComponentMap;" to "components",
        "Lnet/minecraft/core/component/DataComponentType;" to "component",
        "Lnet/minecraft/core/component/DataComponentGetter;" to "componentGetter",
        "Lnet/minecraft/world/level/storage/loot/LootContext;" to "context",
        "Lnet/minecraft/world/attribute/EnvironmentAttribute;" to "attribute",
        "Lnet/minecraft/world/attribute/EnvironmentAttributeMap;" to "attributes",
        "Lnet/minecraft/world/attribute/modifier/AttributeModifier;" to "modifier",
        "Lnet/minecraft/world/level/storage/ValueInput;" to "input",
        "Lnet/minecraft/world/level/storage/ValueOutput;" to "output",
        "Lnet/minecraft/world/entity/EntityDimensions;" to "dimensions",
        "Lnet/minecraft/world/scores/PlayerTeam;" to "team",
        "Lnet/minecraft/world/InteractionHand;" to "hand",
        "Lnet/minecraft/CrashReportCategory;" to "category",
        "Lnet/minecraft/world/entity/EntitySpawnReason;" to "spawnReason",

        "Lnet/minecraft/world/level/chunk/LevelChunk;" to "chunk",
        "Lnet/minecraft/world/level/chunk/ChunkAccess;" to "chunk",

        "Lnet/minecraft/server/level/WorldGenRegion;" to "region",
        "Lnet/minecraft/world/level/LevelReader;" to "level",
        "Lnet/minecraft/world/level/LevelAccessor;" to "level",
        "Lnet/minecraft/world/level/LevelHeightAccessor;" to "level",
        "Lnet/minecraft/world/level/BlockGetter;" to "level",
        "Lnet/minecraft/world/level/BlockAndTintGetter;" to "level",
        // "Lnet/minecraft/world/level/CollisionGetter;" to "level",
        "Lnet/minecraft/server/level/ServerLevel;" to "level",
        "Lnet/minecraft/world/level/ServerLevelAccessor;" to "level",

        "Lnet/minecraft/server/level/ServerPlayer;" to "player",
        "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;" to "nautilus",
        "Lnet/minecraft/world/entity/projectile/AbstractArrow;" to "arrow",
        "Lnet/minecraft/world/entity/monster/AbstractSkeleton;" to "skeleton",
        "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;" to "horse",
        "Lnet/minecraft/world/entity/player/PlayerSkin;" to "skin",
        $$"Lnet/minecraft/world/entity/player/PlayerSkin$Patch;" to "skinPatch",
        "Lnet/minecraft/world/entity/HumanoidArm;" to "arm",
        "Lnet/minecraft/util/debug/DebugValueAccess;" to "valueAccess",

        // libraries
        "Lcom/google/gson/JsonDeserializationContext;" to "context",
        "Lcom/google/gson/JsonSerializationContext;" to "context",
        "Lcom/google/gson/JsonObject;" to "json",
        "Lio/netty/channel/ChannelHandlerContext;" to "ctx",
        "Lio/netty/channel/ChannelPromise;" to "promise",
        "Lio/netty/buffer/ByteBuf;" to "buffer",
        "Lnet/minecraft/network/FriendlyByteBuf;" to "buffer",
        "Lnet/minecraft/network/RegistryFriendlyByteBuf;" to "buffer",
    )

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

    operator fun Int.contains(value: Int): Boolean {
        return value and this != 0
    }

    private fun isEnumConstructor(method: MethodEntry): Boolean {
        if (!method.isConstructor) {
            return false
        }

        return ACC_ENUM in indexer.entryIndex.getAccess(method.parent)
    }

    override fun proposeName(obfEntry: Entry<*>, remapper: EntryRemapper): Optional<String> {
        if (obfEntry is LocalVariableEntry && obfEntry.isArgument) {
            val parent = obfEntry.parent
            if (parent != null) {
                val isStatic = ACC_STATIC in indexer.entryIndex.getAccess(parent)

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

                val paramDesc = parent.desc.argumentDescs[paramIndex]
                if (!paramDesc.containsType()) { // primitive / array of primitive
                    return Optional.empty()
                }

                var paramDescStr = paramDesc.toString()
                if (paramDesc.isArray) {
                    paramDescStr = paramDescStr.drop(paramDesc.arrayDimension) // for array, the element type is often more relevant than the array itself
                }

                var name = suggestions[paramDescStr] ?: paramDescStr.substringAfterLast('/').substringAfterLast('$').dropLast(1).replaceFirstChar { it.lowercase() } // relevant type
                if (paramCanConflict(descStartIndex, parent.desc, paramDesc.typeEntry)) { // not completely accurate for lambda/inner classes
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
