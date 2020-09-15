package dev.arxenix.simplesoundmuffler

import drawer.getFrom
import drawer.put
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.function.Supplier


class SimpleSoundMuffler: ModInitializer {
    companion object {
        val ID = Identifier("simplesoundmuffler", "sound_muffler")
        var BLOCK: Block? = null
        var BLOCK_ITEM: BlockItem? = null
        var BLOCK_ENTITY: BlockEntityType<SoundMufflerBlockEntity>? = null
        val UPDATE_SOUND_MUFFLER_PACKET = Identifier("simplesoundmuffler", "update_sound_muffler")
    }

    override fun onInitialize() {
        println("SimpleSoundMuffler - fabric mod initialized")

        // validation test
        val t1 = SoundMufflerData(SoundMufflerMode.DENY, mutableSetOf(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS.id), 2)
        val tag = CompoundTag()
        SoundMufflerData.serializer().put(t1, tag)
        val t2 = SoundMufflerData.serializer().getFrom(tag)
        assert(t1 == t2)

        BLOCK = Registry.register(
            Registry.BLOCK,
            ID,
            SoundMufflerBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL))
        )

        BLOCK_ITEM = Registry.register(
            Registry.ITEM,
            ID,
            BlockItem(BLOCK, Item.Settings().group(ItemGroup.REDSTONE))
        )

        BLOCK_ENTITY = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            ID,
            BlockEntityType.Builder.create(Supplier { SoundMufflerBlockEntity() }, BLOCK).build(null)
        )

        ServerSidePacketRegistry.INSTANCE.register(
            UPDATE_SOUND_MUFFLER_PACKET
        ) { ctx: PacketContext, buf: PacketByteBuf ->
            val pos = buf.readBlockPos()
            val tag = buf.readCompoundTag()!!
            val data = SoundMufflerData.serializer().getFrom(tag)
            ctx.taskQueue.execute {
                // Execute on the main thread
                // ALWAYS validate that the information received is valid in a C2S packet!
                val blockEntity = ctx.player.world.getBlockEntity(pos)
                if (blockEntity is SoundMufflerBlockEntity) {
                    if (ctx.player.world.canPlayerModifyAt(ctx.player, pos)) {
                        if (data.muffleAmount in 0..100
                            && data.radius in 1..16) {
                            blockEntity.data = data
                            blockEntity.sync()
                            blockEntity.markDirty()
                        }
                    }
                }
            }
        }
    }
}





