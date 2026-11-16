package dev.pandemonium;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

/** Roasts raw copper over a lit campfire, producing native copper for the bloomery stage. */
public final class CrucibleItem extends Item {
    public CrucibleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (state.getBlock() != Blocks.CAMPFIRE || !state.getValue(CampfireBlock.LIT)) return InteractionResult.PASS;
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack ore = ItemStack.EMPTY;
        for (ItemStack candidate : player.getInventory().items) {
            if (candidate.is(net.minecraft.world.item.Items.RAW_COPPER)) { ore = candidate; break; }
        }
        if (ore.isEmpty()) {
            player.displayClientMessage(Component.literal("§7Necesitas mineral de cobre en el inventario."), true);
            return InteractionResult.CONSUME;
        }
        ore.shrink(1);
        ItemStack nativeCopper = new ItemStack(ForgingItems.COPPER_CHUNK, 2);
        if (!player.getInventory().add(nativeCopper)) player.drop(nativeCopper, false, false);
        player.displayClientMessage(Component.literal("§6Tuestas el mineral y separas el cobre nativo."), true);
        level.playSound(null, pos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7f, 0.8f);
        context.getItemInHand().hurtAndBreak(1, player, context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }
}
