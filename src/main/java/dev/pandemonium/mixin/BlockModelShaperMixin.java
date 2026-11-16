package dev.pandemonium.mixin;

import dev.pandemonium.client.ClientState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Daltonismo: cambia el modelo que se dibuja para un bloque (la roca como obsidiana, etc.). */
@Mixin(targets = "net.minecraft.client.renderer.block.BlockModelShaper")
public abstract class BlockModelShaperMixin {
    @ModifyVariable(method = "getBlockModel", at = @At("HEAD"), argsOnly = true)
    private BlockState pandemonium$remap(BlockState state) {
        return ClientState.remap(state);
    }
}
