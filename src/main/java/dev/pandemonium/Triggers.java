package dev.pandemonium;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

/** Aqui estan las formas de pillar cada enfermedad: golpes, pinchazos, caidas, cofres... */
public final class Triggers {
    private static final Set<EntityType<?>> ZOMBIES = Set.of(
            EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER, EntityTypes.HUSK, EntityTypes.DROWNED);

    private static final Map<UUID, ChestHistory> CHESTS = new HashMap<>();

    private record ChestHistory(BlockPos pos, int count, long lastTick) {}

    private static final Map<UUID, long[]> LIST_USES = new HashMap<>();

    private Triggers() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(Triggers::onDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) ConditionStore.onDeath(p);
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand == InteractionHand.MAIN_HAND && !level.isClientSide() && player instanceof ServerPlayer p) {
                onUseBlock(p, hit.getBlockPos(), level.getGameTime(), level.getBlockState(hit.getBlockPos()));
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(Triggers::onTick);
    }

    /** Intenta dar una condicion con cierta probabilidad (0..1). */
    private static void maybe(ServerPlayer p, float chance, Condition c) {
        if (p.getRandom().nextFloat() < chance) {
            ConditionStore.give(p, c);
        }
    }

    private static void onDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
        if (!(entity instanceof ServerPlayer p)) return;
        Entity attacker = source.getEntity();

        if (attacker != null && ZOMBIES.contains(attacker.getType())) {
            maybe(p, 0.30f, Conditions.ZOMBIE_INFECTION);
        }
        if (attacker instanceof ServerPlayer other && ConditionStore.has(other, Conditions.ZOMBIE_INFECTION)) {
            maybe(p, 0.35f, Conditions.ZOMBIE_INFECTION);
        }
        if (attacker != null && attacker.getType() == EntityTypes.BEE) {
            maybe(p, 0.20f, Conditions.ADHD);
        }
        if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            maybe(p, 0.15f, Conditions.TETANUS);
        }
        if (source.is(DamageTypes.CACTUS)) {
            maybe(p, 0.10f, Conditions.TETANUS);
        }
        if (source.is(DamageTypes.FALL)) {
            if (taken >= 6f) maybe(p, 0.25f, Conditions.BRITTLE_BONES);
            if (taken >= 4f) maybe(p, 0.15f, Conditions.ACROPHOBIA);
            if (ConditionStore.has(p, Conditions.BRITTLE_BONES)) {
                // Huesos fragiles: se repite el mismo dano una vez mas (sin llegar a matar por si solo)
                p.setHealth(Math.max(0.5f, p.getHealth() - taken));
            }
        }
        if (source.is(DamageTypes.DROWN)) {
            maybe(p, 0.30f, Conditions.ASTHMA);
        }
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            maybe(p, 0.60f, Conditions.TOURETTE);
        }
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            maybe(p, 0.35f, Conditions.DEMENTIA);
        }
    }

    /** TOC: abrir el mismo cofre 3 veces seguidas. */
    private static void onUseBlock(ServerPlayer p, BlockPos pos, long now, BlockState state) {
        if (!(state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL))) return;
        ChestHistory h = CHESTS.get(p.getUUID());
        if (h != null && h.pos().equals(pos) && now - h.lastTick() < 600) {
            int count = h.count() + 1;
            CHESTS.put(p.getUUID(), new ChestHistory(pos, count, now));
            if (count >= 3 && ConditionStore.give(p, Conditions.OCD)) {
                Fx.msg(p, "§7Abrir el mismo cofre tres veces... algo no va bien en tu cabeza.");
            }
        } else {
            CHESTS.put(p.getUUID(), new ChestHistory(pos, 1, now));
        }
    }

    /** Hipocondria: mirarte las condiciones 3 veces en 5 minutos. */
    public static void onList(ServerPlayer p) {
        long now = Fx.server(p).getTickCount();
        long[] h = LIST_USES.get(p.getUUID());
        if (h == null || now - h[0] > 6000) {
            LIST_USES.put(p.getUUID(), new long[] {now, 1});
            return;
        }
        h[1]++;
        if (h[1] >= 3 && ConditionStore.give(p, Conditions.HYPOCHONDRIA)) {
            Fx.msg(p, "§7Mirarte tanto los síntomas no es sano...");
        }
    }

    /** Disparadores que se comprueban una vez por segundo. */
    private static void onTick(MinecraftServer server) {
        long now = server.getTickCount();
        if (now % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator()) continue;

            // Alergia: pasar rato entre flores
            if (!ConditionStore.has(p, Conditions.POLLEN_ALLERGY) && Fx.nearFlowers(p, 3)) {
                maybe(p, 0.012f, Conditions.POLLEN_ALLERGY);
            }
            // Comida en mal estado (hambre de carne podrida, etc.), salvo si es por la infeccion zombi
            if (p.hasEffect(MobEffects.HUNGER) && !ConditionStore.has(p, Conditions.ZOMBIE_INFECTION)) {
                maybe(p, 0.04f, Conditions.FOOD_POISONING);
            }
            // Hipo espontaneo
            maybe(p, 1f / 1500f, Conditions.HICCUPS);
            // Narcolepsia: llevar mucho sin dormir
            int sinceRest = p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            if (sinceRest > 72000) {
                maybe(p, 0.003f, Conditions.NARCOLEPSY);
            }
        }
    }
}
