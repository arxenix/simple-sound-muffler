package dev.arxenix.simplesoundmuffler

import dev.arxenix.simplesoundmuffler.mixin.AbstractSoundInstanceAccessor
import drawer.getFrom
import drawer.put
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.PositionImpl
import net.minecraft.world.chunk.WorldChunk

class SoundMufflerBlockEntity : BlockEntity(SimpleSoundMuffler.BLOCK_ENTITY), BlockEntityClientSerializable {

    companion object {

        @Environment(EnvType.CLIENT)
        fun handleSound(original: PositionedSoundInstance): PositionedSoundInstance {
            val soundPos = PositionImpl(original.x, original.y, original.z)
            // TODO - optimize more
            // runs in O(#blockentities in 3x3 chunk region)
            // if we track soundmufflers only, we can do it in O(#soundmufflers in 3x3 chunk region)
            val chunkX = original.x.toInt() shr 4
            val chunkZ = original.z.toInt() shr 4

            val offsets = arrayOf(
                intArrayOf(1, -1),
                intArrayOf(1, 0),
                intArrayOf(1, 1),
                intArrayOf(0, -1),
                intArrayOf(0, 0),
                intArrayOf(0, 1),
                intArrayOf(-1, -1),
                intArrayOf(-1, 0),
                intArrayOf(-1, 1)
            )

            val soundMufflers = mutableListOf<SoundMufflerBlockEntity>()

            // iterate through all chunks in 3x3 region around sound
            for (offset in offsets) {
                val ncX = chunkX + offset[0]
                val ncZ = chunkZ + offset[1]

                // get existing chunks only
                val chunk = MinecraftClient.getInstance().world!!
                    .getExistingChunk(ncX, ncZ) as? WorldChunk
                if (chunk != null) {
                    // get list of sound muffler blockentities in chunk
                    val inChunk = chunk.blockEntities.values.mapNotNull { it as? SoundMufflerBlockEntity }
                    // check to see if its in range of noise
                    // +0.1 to help w/ precision errors
                    val valid = inChunk.filter { it.pos.isWithinDistance(soundPos, it.data.radius.toDouble() + 0.1) }
                    soundMufflers.addAll(valid)
                }
            }
            // TODO - better handling for case when there are multiple soundMufflers
            // right now we just grab the first and pass the sound to it
            return soundMufflers.firstOrNull()?.handleSound(original) ?: original
        }
    }
    var data: SoundMufflerData = SoundMufflerData()

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        SoundMufflerData.serializer().put(data, tag)
        return tag
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag) {
        super.fromTag(state, tag)
        data = SoundMufflerData.serializer().getFrom(tag)
    }

    override fun fromClientTag(p0: CompoundTag) {
        fromTag(cachedState, p0)
    }

    override fun toClientTag(p0: CompoundTag): CompoundTag {
        return toTag(p0);
    }

    @Environment(EnvType.CLIENT)
    fun handleSound(original: PositionedSoundInstance): PositionedSoundInstance? {
        //println("muffler handling sound ${original.id}")
        val contains = data.soundEvents.contains(original.id)
        var muffleAmount = 0;
        if (data.mode == SoundMufflerMode.ALLOW && !contains) {
            muffleAmount = this.data.muffleAmount
        }
        else if (data.mode == SoundMufflerMode.DENY && contains) {
            muffleAmount = this.data.muffleAmount
        }
        val volumeMultiplier = (100 - muffleAmount)/100.0f
        //println("muffleAmount $muffleAmount multiplier $volumeMultiplier")
        return PositionedSoundInstance(
            SoundEvent(original.id),
            original.category,
            (original as AbstractSoundInstanceAccessor).volume * volumeMultiplier,
            (original as AbstractSoundInstanceAccessor).pitch,
            original.x,
            original.y,
            original.z)
    }
}