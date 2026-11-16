package dev.pandemonium.mixin;

import dev.pandemonium.client.ClientState;
import dev.pandemonium.client.MoodleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dibuja los moodles encima del chat y del inventario para poder pasar el raton. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"))
    private void pandemonium$moodles(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        if (!MoodleRenderer.isInteractiveScreen(this)) return;
        Minecraft mc = Minecraft.getInstance();
        ClientState.screenDrawMs = System.currentTimeMillis();
        MoodleRenderer.draw(g, mc, mouseX, mouseY, true);
    }
}
