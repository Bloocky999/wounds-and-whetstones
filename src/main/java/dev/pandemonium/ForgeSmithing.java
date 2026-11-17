package dev.pandemonium;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

/** Holds server-side workpieces and derives weapon statistics from the actual pixel profile. */
public final class ForgeSmithing {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final BlockPos anvil;
        final String metal;
        final Item blank;
        final InteractionHand hammerHand;
        final int availableHandles;
        final boolean[] cells = ForgeMath.billet();
        final Queue<Integer> undo = new ArrayDeque<>();
        int handle;
        int hits;
        long lastStrike = -10;
        String message = "Golpea píxeles para dibujar el perfil de la hoja.";

        Session(BlockPos anvil, String metal, Item blank, InteractionHand hammerHand, int handle, int availableHandles) {
            this.anvil = anvil.immutable();
            this.metal = metal;
            this.blank = blank;
            this.hammerHand = hammerHand;
            this.handle = handle;
            this.availableHandles = availableHandles;
        }
    }

    private ForgeSmithing() {}

    public static boolean start(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        ItemStack billet = findBillet(player);
        if (billet.isEmpty()) return false;
        if (!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, ForgePayloads.State.TYPE)) {
            player.sendSystemMessage(Component.literal("§7Para forjar el filo pixel a pixel, también necesitas instalar el mod en el cliente."));
            return true;
        }
        int handles = handleMask(player);
        if (handles == 0) {
            player.sendSystemMessage(Component.literal("§7Necesitas una empuñadura: palo, hueso, cuero o pepita de hierro."));
            return true;
        }
        int handle = (handles & 1) != 0 ? ForgeMath.WOOD : firstHandle(handles);
        String metal = billet.is(ForgingItems.COPPER_BLADE) ? "copper" : "iron";
        SESSIONS.put(player.getUUID(), new Session(pos, metal, billet.getItem(), hand, handle, handles));
        sendState(player, SESSIONS.get(player.getUUID()), false);
        return true;
    }

    public static void handle(ServerPlayer player, int action, int argument) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        if (!validContext(player, session)) {
            close(player, session, "Te has alejado del yunque; la pieza vuelve a quedar en tu inventario.");
            return;
        }
        switch (action) {
            case 0 -> strike(player, session, argument, false);
            case 1 -> strike(player, session, 0, true);
            case 2 -> selectHandle(player, session, argument);
            case 3 -> finish(player, session);
            case 4 -> close(player, session, "Forja cancelada. Conservas la pieza en bruto.");
            default -> { }
        }
    }

    public static void disconnect(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    private static void strike(ServerPlayer player, Session s, int cell, boolean undo) {
        long tick = player.server.getTickCount();
        if (tick - s.lastStrike < 2 || !hasHammer(player, s)) return;
        int target;
        if (undo) {
            Integer last = s.undo.poll();
            if (last == null || s.cells[last]) return;
            target = last;
        } else {
            if (cell < 0 || cell >= ForgeMath.PIXELS || !s.cells[cell]) return;
            target = cell;
        }
        s.lastStrike = tick;
        s.hits++;
        if (undo) s.cells[target] = true;
        else {
            s.cells[target] = false;
            s.undo.add(target);
        }
        s.message = undo ? "Recalientas y corriges el último golpe." : "Golpe " + s.hits + ": quitas un píxel de metal.";
        hurtHammer(player, s);
        player.swing(s.hammerHand);
        player.level().playSound(null, s.anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.75f, 0.85f + player.getRandom().nextFloat() * 0.2f);
        sendState(player, s, false);
    }

    private static void selectHandle(ServerPlayer player, Session s, int handle) {
        if (handle < ForgeMath.WOOD || handle > ForgeMath.IRON) return;
        if (hasItem(player, handleItem(handle))) {
            s.handle = handle;
            s.message = "Empuñadura seleccionada: " + handleName(handle) + ".";
        } else {
            s.message = "No llevas el material de esa empuñadura.";
        }
        sendState(player, s, false);
    }

    private static void finish(ServerPlayer player, Session s) {
        ForgeMath.Stats stats = ForgeMath.evaluate(s.metal, s.cells, s.handle);
        if (!stats.valid()) {
            s.message = stats.reason();
            sendState(player, s, false);
            return;
        }
        ItemStack billet = findBillet(player, s.blank);
        ItemStack grip = findItem(player, handleItem(s.handle));
        if (billet.isEmpty() || grip.isEmpty() || !hasHammer(player, s)) {
            s.message = "Falta la pieza, la empuñadura o el martillo en tu inventario.";
            sendState(player, s, false);
            return;
        }
        billet.shrink(1);
        grip.shrink(1);
        ItemStack sword = new ItemStack(s.metal.equals("copper")
                ? ForgingItems.FORGED_COPPER_SWORD : ForgingItems.FORGED_IRON_SWORD);
        sword.set(DataComponents.MAX_DAMAGE, stats.durability());
        sword.set(DataComponents.DAMAGE, 0);
        sword.set(DataComponents.ENCHANTABLE, new Enchantable(stats.enchantability()));
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "forged_damage"),
                        stats.damage(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                        Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "forged_speed"),
                        stats.attackSpeed(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        sword.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal(
                "Espada de " + (s.metal.equals("copper") ? "cobre" : "hierro") + " — " + stats.quality()
                        + " (" + handleName(s.handle).toLowerCase() + ")"));
        if (!player.getInventory().add(sword)) player.drop(sword, false, Prediction.SERVER_ONLY);
        s.message = "Forja completada: " + stats.quality() + ", empuñadura de " + handleName(s.handle) + ".";
        SESSIONS.remove(player.getUUID());
        ForgePayloads.send(player, encode(s, true));
        player.sendSystemMessage(Component.literal("§6" + s.message));
    }

    private static void close(ServerPlayer player, Session s, String message) {
        SESSIONS.remove(player.getUUID());
        s.message = message;
        ForgePayloads.send(player, encode(s, true));
    }

    private static boolean validContext(ServerPlayer player, Session s) {
        Block block = player.level().getBlockState(s.anvil).getBlock();
        boolean anvil = block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL;
        return anvil && player.distanceToSqr(s.anvil.getX() + 0.5, s.anvil.getY() + 0.5, s.anvil.getZ() + 0.5) <= 36;
    }

    private static boolean hasHammer(ServerPlayer player, Session s) {
        return player.getItemInHand(s.hammerHand).is(ForgingItems.FORGE_HAMMER);
    }

    private static void hurtHammer(ServerPlayer player, Session s) {
        EquipmentSlot slot = s.hammerHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        player.getItemInHand(s.hammerHand).hurtAndBreak(1, player, slot);
    }

    private static ItemStack findBillet(ServerPlayer player) {
        return findBillet(player, null);
    }

    private static ItemStack findBillet(ServerPlayer player, Item expected) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if ((stack.is(ForgingItems.COPPER_BLADE) || stack.is(ForgingItems.IRON_BLADE))
                    && (expected == null || stack.is(expected))) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findItem(ServerPlayer player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasItem(ServerPlayer player, Item item) { return !findItem(player, item).isEmpty(); }

    private static Item handleItem(int handle) {
        return switch (handle) {
            case ForgeMath.BONE -> Items.BONE;
            case ForgeMath.LEATHER -> Items.LEATHER;
            case ForgeMath.IRON -> Items.IRON_NUGGET;
            default -> Items.STICK;
        };
    }

    private static String handleName(int handle) {
        return switch (handle) {
            case ForgeMath.BONE -> "Hueso";
            case ForgeMath.LEATHER -> "Cuero";
            case ForgeMath.IRON -> "Hierro";
            default -> "Madera";
        };
    }

    private static int handleMask(ServerPlayer player) {
        int mask = 0;
        for (int h = 0; h <= ForgeMath.IRON; h++) if (hasItem(player, handleItem(h))) mask |= 1 << h;
        return mask;
    }

    private static int firstHandle(int mask) {
        for (int h = 0; h <= ForgeMath.IRON; h++) if ((mask & (1 << h)) != 0) return h;
        return ForgeMath.WOOD;
    }

    private static void sendState(ServerPlayer player, Session s, boolean closed) {
        ForgePayloads.send(player, encode(s, closed));
    }

    private static String encode(Session s, boolean closed) {
        StringBuilder pixels = new StringBuilder(ForgeMath.PIXELS);
        for (boolean cell : s.cells) pixels.append(cell ? '1' : '0');
        return s.metal + "|" + pixels + "|" + s.hits + "|" + s.handle + "|"
                + (closed ? 0 : s.availableHandles) + "|" + s.message.replace('|', '/') + "|" + closed;
    }
}
