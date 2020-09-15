package dev.arxenix.simplesoundmuffler.mixin;

import dev.arxenix.simplesoundmuffler.SoundMufflerBlockEntity;
import dev.arxenix.simplesoundmuffler.SoundMufflerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @ModifyVariable(
            method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
            at = @At(value = "STORE", opcode = Opcodes.ASTORE, ordinal = 0)
    )
    private PositionedSoundInstance modifyPlaySound(PositionedSoundInstance original) {
        assert MinecraftClient.getInstance().world != null;
        assert MinecraftClient.getInstance().player != null;
        // send observe sound event to our screen
        if (MinecraftClient.getInstance().currentScreen instanceof SoundMufflerScreen) {
            SoundMufflerScreen screen = (SoundMufflerScreen) MinecraftClient.getInstance().currentScreen;
            screen.getDescription().observeSound(original.getId());
        }

        return SoundMufflerBlockEntity.Companion.handleSound(original);
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
