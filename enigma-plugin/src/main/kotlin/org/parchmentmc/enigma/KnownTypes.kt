package org.parchmentmc.enigma

object KnownTypes {
    // applies before unobfuscated names
    // sometimes parchment standard doesn't match
    // unobfuscated names
    val FIXED_NAMES = mapOf(
        // client
        "Lcom/mojang/blaze3d/vertex/VertexConsumer;" to "consumer",
        "Lcom/mojang/blaze3d/pipeline/RenderPipeline;" to "pipeline",
        "Lcom/mojang/blaze3d/systems/GpuDevice;" to "device",
        "Lnet/minecraft/client/gui/GuiGraphics;" to "guiGraphics",
        "Lnet/minecraft/client/renderer/texture/MipmapStrategy;" to "strategy",

        // server
        $$"Lcom/mojang/serialization/codecs/RecordCodecBuilder$Instance;" to "instance",
        "Lcom/mojang/brigadier/context/CommandContext;" to "context",
        "Lnet/minecraft/world/attribute/EnvironmentAttributeMap;" to "attributes",
        "Lnet/minecraft/world/level/gamerules/GameRule;" to "rule",

        // libraries
        "Lio/netty/buffer/ByteBuf;" to "buffer",
        "Lnet/minecraft/network/FriendlyByteBuf;" to "buffer",
        "Lnet/minecraft/network/RegistryFriendlyByteBuf;" to "buffer",
    )

    // applies after unobfuscated names if nothing found
    private val NAMES = mapOf(
        // client
        "Lcom/mojang/blaze3d/platform/NativeImage;" to "image",
        "Lcom/mojang/blaze3d/textures/GpuTexture;" to "texture",
        "Lcom/mojang/blaze3d/textures/GpuTextureView;" to "texture",
        $$"Lcom/mojang/realmsclient/gui/screens/RealmsDownloadLatestWorldScreen$DownloadStatus;" to "status",
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
        "Lnet/minecraft/client/multiplayer/ClientLevel;" to "level",
        "Lnet/minecraft/client/player/AbstractClientPlayer;" to "player",
        "Lnet/minecraft/client/input/KeyEvent;" to "event",
        "Lnet/minecraft/client/input/CharacterEvent;" to "event",
        "Lnet/minecraft/client/input/MouseButtonEvent;" to "event",
        "Lnet/minecraft/client/input/MouseButtonInfo;" to "buttonInfo",
        "Lnet/minecraft/client/OptionInstance;" to "option",

        // server
        "Lcom/mojang/authlib/GameProfile;" to "profile",
        "Lnet/minecraft/world/item/component/ResolvableProfile;" to "profile",
        "Lcom/mojang/brigadier/CommandDispatcher;" to "dispatcher",
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
        "Lnet/minecraft/resources/Identifier;" to "id",
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
        "Lnet/minecraft/world/attribute/modifier/AttributeModifier;" to "modifier",
        "Lnet/minecraft/world/level/storage/ValueInput;" to "input",
        "Lnet/minecraft/world/level/storage/ValueOutput;" to "output",
        "Lnet/minecraft/world/entity/EntityDimensions;" to "dimensions",
        "Lnet/minecraft/world/scores/PlayerTeam;" to "team",
        "Lnet/minecraft/world/InteractionHand;" to "hand",
        "Lnet/minecraft/CrashReportCategory;" to "category",
        "Lnet/minecraft/world/entity/EntityProcessor;" to "processor",
        "Lnet/minecraft/world/entity/EntitySpawnReason;" to "spawnReason",
        "Lnet/minecraft/world/level/gamerules/GameRuleMap;" to "rules",
        "Lnet/minecraft/world/level/gamerules/GameRuleCategory;" to "category",
        "Lnet/minecraft/world/level/gamerules/GameRuleType;" to "type",
        "Lnet/minecraft/server/jsonrpc/internalapi/MinecraftApi;" to "api",
        "Lnet/minecraft/server/jsonrpc/methods/ClientInfo;" to "client",
        "Lnet/minecraft/world/entity/InsideBlockEffectApplier;" to "effectApplier",
        "Lnet/minecraft/advancements/critereon/CriterionValidator;" to "validator",

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
        "Lio/netty/handler/codec/http/HttpRequest;" to "request",
        "Ljava/net/http/HttpResponse;" to "response",
    )

    fun getName(descriptor: String): String? {
        return NAMES[descriptor]
    }

    fun getBestName(descriptor: String): String {
        return getName(descriptor) ?: fallbackName(descriptor)
    }

    private fun fallbackName(descriptor: String): String {
        return descriptor
            .substringAfterLast('/')
            .substringAfterLast('$')
            .trimStart { it.isDigit() } // for local class
            .dropLast(1)
            .replaceFirstChar { it.lowercase() } // relevant type
    }
}
