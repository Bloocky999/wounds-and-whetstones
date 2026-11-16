package dev.pandemonium.mixin;

import dev.pandemonium.client.ClientState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inclina la camara (artrosis). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    public abstract Quaternionf rotation();

    @Inject(method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("TAIL"))
    private void pandemonium$roll(Level level, Entity entity, boolean detached, boolean mirror, float partial, CallbackInfo ci) {
        float roll = ClientState.cameraRoll();
        if (Math.abs(roll) > 0.001f) {
            this.rotation().rotateZ(roll);
        }
    }
}
