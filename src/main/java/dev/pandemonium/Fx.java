package dev.pandemonium;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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

    public static void effectHidden(ServerPlayer p, Holder<MobEffect> effect, int ticks, int amplifier) {
        p.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false));
    }

    /** Lanza particulas desde la cara hacia donde mira el jugador. */
    public static void spray(ServerPlayer p, ParticleOptions type, int count, double speed, double spread) {
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition().add(look.scale(0.35)).add(0, -0.12, 0);
        ServerLevel level = level(p);
        for (int i = 0; i < count; i++) {
            double vx = look.x + (p.getRandom().nextDouble() - 0.5) * spread;
            double vy = look.y + (p.getRandom().nextDouble() - 0.5) * spread - 0.05;
            double vz = look.z + (p.getRandom().nextDouble() - 0.5) * spread;
            level.sendParticles(type, eye.x, eye.y, eye.z, 0, vx, vy, vz, speed);
        }
    }

    private static final Map<String, Long> POSE_UNTIL = new HashMap<>();

    /** Activa una animacion corta de cuerpo (visible para todos) durante unos ticks. */
    public static void pose(ServerPlayer p, int flag, int ticks) {
        MinecraftServer s = server(p);
        POSE_UNTIL.put(p.getUUID() + ":" + flag, s.getTickCount() + (long) ticks);
        Payloads.poseAll(s);
    }

    /** True si algun jugador tiene una animacion corta activa (para enviar la pose mas a menudo). */
    public static boolean hasTempPose(MinecraftServer s) {
        long now = s.getTickCount();
        POSE_UNTIL.values().removeIf(until -> until <= now);
        return !POSE_UNTIL.isEmpty();
    }

    /** Animaciones cortas activas ahora mismo para este jugador. */
    public static int tempPose(ServerPlayer p) {
        long now = server(p).getTickCount();
        int flags = 0;
        for (int flag : new int[] {Condition.POSE_COUGH, Condition.POSE_VOMIT, Condition.POSE_SHIVER, Condition.POSE_HUNCH,
                Condition.POSE_TWITCH, Condition.POSE_SCRATCH, Condition.POSE_COVER, Condition.POSE_SLUMP}) {
            Long until = POSE_UNTIL.get(p.getUUID() + ":" + flag);
            if (until != null && until > now) flags |= flag;
        }
        return flags;
    }

    private static final Map<UUID, Integer> VOMIT = new HashMap<>();

    /** Vomito: chorro de particulas verdes desde la cara durante unos ticks. */
    public static void startVomit(ServerPlayer p, int ticks) {
        VOMIT.put(p.getUUID(), ticks);
        pose(p, Condition.POSE_VOMIT, ticks);
        Payloads.fx(p, "sick", 50);
        jerk(p, 0f, 18f);
        sound(p, SoundEvents.PLAYER_BURP, 1.0f, 0.5f);
    }

    public static void tickEffects(MinecraftServer server) {
        if (VOMIT.isEmpty()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Integer left = VOMIT.get(p.getUUID());
            if (left == null) continue;
            if (!p.isAlive()) {
                VOMIT.remove(p.getUUID());
                continue;
            }
            spray(p, ParticleTypes.ITEM_SLIME, 6, 0.35, 0.3);
            if (left % 3 == 0) spray(p, ParticleTypes.SNEEZE, 1, 0.3, 0.2);
            if (left % 6 == 0) sound(p, SoundEvents.SLIME_SQUISH, 0.8f, 0.7f);
            if (left <= 1) VOMIT.remove(p.getUUID());
            else VOMIT.put(p.getUUID(), left - 1);
        }
    }

    /** Gira la camara del jugador hacia una entidad. */
    public static void lookAt(ServerPlayer p, Entity e) {
        Vec3 d = e.getEyePosition().subtract(p.getEyePosition());
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * 180.0 / Math.PI);
        float pitch = (float) (-(Mth.atan2(d.y, horiz) * 180.0 / Math.PI));
        jerk(p, Mth.wrapDegrees(yaw - p.getYRot()), Mth.wrapDegrees(pitch - p.getXRot()));
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
