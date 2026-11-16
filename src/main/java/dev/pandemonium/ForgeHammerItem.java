package dev.pandemonium;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;

/** A real anvil interaction: blooms consolidate and shaped blanks become forged weapons. */
public final class ForgeHammerItem extends Item {
    public ForgeHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var block = level.getBlockState(pos).getBlock();
        if (block != Blocks.ANVIL && block != Blocks.CHIPPED_ANVIL && block != Blocks.DAMAGED_ANVIL) {
            return InteractionResult.PASS;
        }
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack output = null;
        String message = null;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ForgingItems.COPPER_BLOOM)) {
                stack.shrink(1);
                output = new ItemStack(ForgingItems.COPPER_INGOT);
                message = "El martilleo expulsa la escoria y consolida el cobre.";
                break;
            }
            if (stack.is(ForgingItems.IRON_BLOOM)) {
                stack.shrink(1);
                output = new ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
                message = "Expulsas la escoria y sueldas la esponja de hierro.";
                break;
            }
            if (stack.is(ForgingItems.COPPER_BLADE)) {
                stack.shrink(1);
                output = new ItemStack(ForgingItems.FORGED_COPPER_SWORD);
                message = "Templas la hoja de cobre. Es ligera, blanda y se desafila pronto.";
                break;
            }
            if (stack.is(ForgingItems.IRON_BLADE)) {
                stack.shrink(1);
                output = new ItemStack(ForgingItems.FORGED_IRON_SWORD);
                message = "La hoja de hierro toma forma bajo el martillo.";
                break;
            }
        }
        if (output == null) {
            player.displayClientMessage(Component.literal("§7El yunque necesita una esponja de cobre o una hoja en bruto."), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getInventory().add(output)) player.drop(output, false, false);
        context.getItemInHand().hurtAndBreak(1, player,
                context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.displayClientMessage(Component.literal("§6" + message), true);
        level.levelEvent(1021, pos, 0);
        return InteractionResult.SUCCESS;
    }
}
