package dev.arxenix.simplesoundmuffler.mixin;

import dev.arxenix.simplesoundmuffler.SoundMufflerBlockEntity;
import dev.arxenix.simplesoundmuffler.SoundMufflerScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Unique
    private int SEARCH_RADIUS = 3;

    @ModifyVariable(
            method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
            at = @At(value = "STORE", opcode = Opcodes.ASTORE, ordinal = 0)
    )
    private PositionedSoundInstance modifyPlaySound(PositionedSoundInstance original) {
        assert MinecraftClient.getInstance().world != null;
        // send observe sound event to our screen
        if (MinecraftClient.getInstance().currentScreen instanceof SoundMufflerScreen) {
            SoundMufflerScreen screen = (SoundMufflerScreen) MinecraftClient.getInstance().currentScreen;
            screen.getDescription().observeSound(original.getId());
        }


        // scan a 6x6x6 cube around sound for a sound muffler
        // TODO - optimize!
        BlockBox blockBox = new BlockBox(
                (int) original.getX() - SEARCH_RADIUS, (int) original.getY() - SEARCH_RADIUS, (int) original.getZ() - SEARCH_RADIUS,
                (int) original.getX() + SEARCH_RADIUS, (int) original.getY() + SEARCH_RADIUS, (int) original.getZ() + SEARCH_RADIUS
        );
        for (int x=blockBox.minX; x<=blockBox.maxX; x++) {
            for (int y=blockBox.minY; y<=blockBox.maxY;y++) {
                for (int z=blockBox.minZ; z<=blockBox.maxZ;z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = MinecraftClient.getInstance().world.getBlockEntity(pos);
                    if (be instanceof SoundMufflerBlockEntity) {
                        // send to the blockentity for handling and return the altered sound
                        PositionedSoundInstance newSound = ((SoundMufflerBlockEntity) be).handleSound(original);
                        if (newSound != null)
                            return newSound;
                    }
                }
            }
        }

        return original;
    }

    /*
    @Inject(
            at=@At(value="HEAD"),
            method= "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"
    )
    public void playSound(double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        assert MinecraftClient.getInstance().world != null;
        if (MinecraftClient.getInstance().currentScreen instanceof SoundMufflerScreen) {
            SoundMufflerScreen screen = (SoundMufflerScreen) MinecraftClient.getInstance().currentScreen;
            screen.getDescription().observeSound(sound);
        }
    }
    */
}
