package dev.pandemonium.mixin;

import dev.pandemonium.Conditions;
import dev.pandemonium.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cleptomania: lo que intentas tirar vuelve a tu inventario. */
@Mixin(Player.class)
public abstract class PlayerDropMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void pandemonium$klepto(ItemStack stack, boolean randomly, boolean thrown, CallbackInfoReturnable<ItemEntity> cir) {
        if (!thrown || stack.isEmpty()) return;
        if (!((Object) this instanceof ServerPlayer sp)) return;
        if (!Conditions.keepsItems(sp)) return;
        if (sp.getInventory().add(stack.copy())) {
            Fx.bar(sp, "§8Es MÍO...");
            cir.setReturnValue(null);
        }
    }
}
