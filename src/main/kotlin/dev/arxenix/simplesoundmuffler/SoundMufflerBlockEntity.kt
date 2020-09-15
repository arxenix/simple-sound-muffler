package dev.arxenix.simplesoundmuffler

import dev.arxenix.simplesoundmuffler.mixin.AbstractSoundInstanceAccessor
import drawer.getFrom
import drawer.put
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.sound.SoundEvent

class SoundMufflerBlockEntity : BlockEntity(SimpleSoundMuffler.BLOCK_ENTITY), BlockEntityClientSerializable {
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