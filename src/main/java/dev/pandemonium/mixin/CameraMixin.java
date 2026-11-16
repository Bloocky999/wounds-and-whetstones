package dev.pandemonium.mixin;

import dev.pandemonium.client.ClientState;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inclina la camara (artrosis: 15 grados, igual que el torso). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    public abstract Quaternionf rotation();

    @Inject(method = {"setup", "update"}, at = @At("TAIL"), require = 0)
    private void pandemonium$roll(CallbackInfo ci) {
        if (!ClientState.claimCameraFrame()) return;
        float roll = ClientState.cameraRoll();
        if (Math.abs(roll) > 0.001f) {
            this.rotation().rotateZ(roll);
        }
    }
}
