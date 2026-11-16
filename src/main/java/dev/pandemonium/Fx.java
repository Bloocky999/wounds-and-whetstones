package dev.pandemonium;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

/** Utilidades para temporizadores, mensajes y efectos. */
public final class Fx {
    private static final Map<String, Long> NEXT = new HashMap<>();

    private Fx() {}

    /**
     * Devuelve true cuando toca disparar un evento. La primera vez solo programa el siguiente
     * (entre min y max ticks) y devuelve false.
     */
    public static boolean due(ServerPlayer p, String key, long now, int min, int max) {
        String k = p.getUUID() + ":" + key;
        Long next = NEXT.get(k);
        if (next == null) {
            NEXT.put(k, now + range(p, min, max));
            return false;
        }
        if (now >= next) {
            NEXT.put(k, now + range(p, min, max));
            return true;
        }
        return false;
    }

    /** Borra los temporizadores de un jugador (al morir, por ejemplo). */
    public static void clear(UUID id) {
        String prefix = id.toString() + ":";
        NEXT.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private static int range(ServerPlayer p, int min, int max) {
        return min + p.getRandom().nextInt(Math.max(1, max - min + 1));
    }

    /** Numero aleatorio entre -range y +range. */
    public static float rf(ServerPlayer p, float range) {
        return (p.getRandom().nextFloat() * 2f - 1f) * range;
    }

    public static void msg(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text));
    }

    /** Mensaje en la barra encima de la barra de objetos. */
    public static void bar(ServerPlayer p, String text) {
        p.sendSystemMessage(Component.literal(text), true);
    }

    public static void effect(ServerPlayer p, Holder<MobEffect> effect, int ticks, int amplifier) {
        p.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    public static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        p.level().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Sacude la camara del jugador: gira yaw grados a los lados y pitch grados arriba/abajo. */
    public static void jerk(ServerPlayer p, float yaw, float pitch) {
        p.connection.send(new ClientboundPlayerRotationPacket(yaw, true, pitch, true));
    }

    public static ServerLevel level(ServerPlayer p) {
        return (ServerLevel) p.level();
    }

    public static MinecraftServer server(ServerPlayer p) {
        return level(p).getServer();
    }

    public static boolean nearFlowers(ServerPlayer p, int radius) {
        BlockPos c = p.blockPosition();
        Level level = p.level();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(BlockTags.FLOWERS)) {
                return true;
            }
        }
        return false;
    }
}
