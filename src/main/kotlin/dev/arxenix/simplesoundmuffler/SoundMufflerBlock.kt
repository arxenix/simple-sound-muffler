package dev.arxenix.simplesoundmuffler

import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class SoundMufflerBlock(settings: Settings?) : BlockWithEntity(settings), BlockEntityProvider {
    override fun createBlockEntity(blockView: BlockView?): BlockEntity? {
        return SoundMufflerBlockEntity()
    }

    override fun getRenderType(state: BlockState): BlockRenderType {
        return BlockRenderType.MODEL
    }

    override fun onUse(
        blockState: BlockState,
        world: World,
        pos: BlockPos?,
        player: PlayerEntity,
        hand: Hand?,
        blockHitResult: BlockHitResult?
    ): ActionResult {
        val blockEntity = world.getBlockEntity(pos)
        if (blockEntity is SoundMufflerBlockEntity) {
            if (world.isClient) {
                MinecraftClient.getInstance().openScreen(
                    SoundMufflerScreen(SoundMufflerGui(blockEntity))
                )
                return ActionResult.SUCCESS
            }
            else {
                return ActionResult.CONSUME
            }
        } else {
            return ActionResult.PASS
        }
    }
}

