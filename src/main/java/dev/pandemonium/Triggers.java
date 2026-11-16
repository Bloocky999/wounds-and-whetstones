package dev.pandemonium;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
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
                Conditions.ocdUse(p, level.getBlockState(hit.getBlockPos()));
                onUseBlock(p, hit.getBlockPos(), level.getGameTime(), level.getBlockState(hit.getBlockPos()));
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (player instanceof ServerPlayer p) Conditions.ocdBreak(p, state);
        });
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (ConditionStore.has(sender, Conditions.MUTISM)) {
                Fx.bar(sender, "§8Intentas hablar, pero no te sale la voz...");
                return false;
            }
            if (ConditionStore.has(sender, Conditions.STUTTER)) {
                String text = Conditions.stutter(message.decoratedContent().getString(), sender);
                Fx.server(sender).getPlayerList().broadcastSystemMessage(
                        Component.literal("<" + sender.getName().getString() + "> " + text), false);
                return false;
            }
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(Triggers::onTick);
    }

    /** Intenta dar una condicion con cierta probabilidad (0..1). */
    private static void maybe(ServerPlayer p, float chance, Condition c) {
        if (!Settings.naturalOn()) return;
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
            if (Conditions.spreads(other, p, 0.35f)) ConditionStore.give(p, Conditions.ZOMBIE_INFECTION);
        }
        if (attacker instanceof ServerPlayer other && ConditionStore.has(other, Conditions.FLU)) {
            if (Conditions.spreads(other, p, 0.15f)) ConditionStore.give(p, Conditions.FLU);
        }
        if (attacker instanceof ServerPlayer other && ConditionStore.has(other, Conditions.RABIES)) {
            if (Conditions.spreads(other, p, 0.5f)) ConditionStore.give(p, Conditions.RABIES);
        }
        if (attacker != null && (attacker.getType() == EntityTypes.WOLF || attacker.getType() == EntityTypes.FOX)) {
            maybe(p, 0.04f, Conditions.RABIES);
        }
        if (attacker != null && attacker.getType() == EntityTypes.PHANTOM) maybe(p, 0.25f, Conditions.INSOMNIA);
        if (attacker != null && (attacker.getType() == EntityTypes.SPIDER || attacker.getType() == EntityTypes.CAVE_SPIDER)) {
            maybe(p, 0.10f, Conditions.ARACHNOPHOBIA);
        }
        if (attacker != null && attacker.getType().getCategory() == MobCategory.MONSTER) maybe(p, 0.03f, Conditions.PANIC);
        if (source.is(DamageTypes.IN_WALL)) maybe(p, 0.40f, Conditions.CLAUSTROPHOBIA);
        if (attacker != null && !source.is(DamageTypes.FALL) && taken >= 5f) maybe(p, 0.12f, Conditions.BLEEDING);
        if (taken >= 8f) {
            maybe(p, 0.03f, Conditions.EPILEPSY);
            if (ConditionStore.has(p, Conditions.EPILEPSY) && p.getRandom().nextFloat() < 0.15f) Conditions.startSeizure(p);
        }
        if (attacker != null && attacker.getType().getCategory() == MobCategory.MONSTER) maybe(p, 0.02f, Conditions.STUTTER);
        if (attacker != null && attacker.getType() == EntityTypes.ENDERMAN) maybe(p, 0.20f, Conditions.SCHIZOPHRENIA);
        if (attacker != null && (attacker.getType() == EntityTypes.PILLAGER || attacker.getType() == EntityTypes.VINDICATOR)) {
            maybe(p, 0.08f, Conditions.KLEPTOMANIA);
        }
        if (taken >= 9f) maybe(p, 0.05f, Conditions.LAME_ARM);
        for (Conditions.Spread sp : Conditions.SPREADS) {
            if (attacker instanceof ServerPlayer o && ConditionStore.has(o, sp.virus()) && Conditions.spreads(o, p, sp.hitChance())) {
                ConditionStore.give(p, sp.virus());
            }
        }
        if (!source.is(DamageTypes.FALL) && taken >= 7f) maybe(p, 0.15f, Conditions.CONCUSSION);
        if (taken >= 10f) maybe(p, 0.06f, Conditions.TREMORS);
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
            if (taken >= 4f) maybe(p, 0.15f, Conditions.SPRAIN);
            if (taken >= 8f) maybe(p, 0.10f, Conditions.ARTHROSIS);
            if (taken >= 8f) maybe(p, 0.12f, Conditions.BAD_LEG);
            if (taken >= 6f) maybe(p, 0.05f, Conditions.VERTIGO);
            if (ConditionStore.has(p, Conditions.BRITTLE_BONES)) {
                // Huesos fragiles: se repite el mismo dano una vez mas (sin llegar a matar por si solo)
                p.setHealth(Math.max(0.5f, p.getHealth() - taken));
            }
        }
        if (source.is(DamageTypes.DROWN)) {
            maybe(p, 0.30f, Conditions.ASTHMA);
            maybe(p, 0.08f, Conditions.MUTISM);
        }
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            maybe(p, 0.60f, Conditions.TOURETTE);
            maybe(p, 0.25f, Conditions.TREMORS);
            maybe(p, 0.10f, Conditions.COLORBLIND);
            maybe(p, 0.15f, Conditions.EPILEPSY);
            if (ConditionStore.has(p, Conditions.EPILEPSY)) Conditions.startSeizure(p);
            maybe(p, 0.20f, Conditions.MIGRAINE);
            maybe(p, 0.10f, Conditions.SCHIZOPHRENIA);
            maybe(p, 0.08f, Conditions.NIGHT_BLINDNESS);
        }
        if (source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION)) {
            maybe(p, 0.35f, Conditions.DEMENTIA);
            maybe(p, 0.20f, Conditions.BLINDNESS);
            maybe(p, 0.30f, Conditions.TINNITUS);
        }
    }

    /** TOC: abrir el mismo cofre 3 veces seguidas. */
    private static void onUseBlock(ServerPlayer p, BlockPos pos, long now, BlockState state) {
        if (!(state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL))) return;
        ChestHistory h = CHESTS.get(p.getUUID());
        if (h != null && h.pos().equals(pos) && now - h.lastTick() < 600) {
            int count = h.count() + 1;
            CHESTS.put(p.getUUID(), new ChestHistory(pos, count, now));
            if (count >= 3 && Settings.naturalOn() && ConditionStore.give(p, Conditions.OCD)) {
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
        if (h[1] >= 3 && Settings.naturalOn() && ConditionStore.give(p, Conditions.HYPOCHONDRIA)) {
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
            // Congelacion total, calor del Nether, migrana por cansancio, leche
            if (p.getTicksFrozen() >= p.getTicksRequiredToFreeze()) maybe(p, 0.06f, Conditions.FROSTBITE);
            if (p.level().dimension() == Level.NETHER && !p.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                maybe(p, 0.0025f, Conditions.HEATSTROKE);
            }
            if (p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) > 48000) maybe(p, 0.002f, Conditions.MIGRAINE);
            maybe(p, 1f / 5000f, Conditions.POX);
            String raw = Conditions.rawEaten(p);
            if (raw != null) rawFood(p, raw);
            if (p.hasEffect(MobEffects.WITHER)) maybe(p, 0.02f, Conditions.NIGHT_BLINDNESS);
            if (p.isFreezing()) maybe(p, 0.012f, Conditions.COLD);
            else if (p.isInWaterOrRain() && !p.isInWater()) maybe(p, 0.004f, Conditions.COLD);
            maybe(p, 1f / 12000f, Conditions.MEASLES);
            maybe(p, 1f / 12000f, Conditions.PINKEYE);
            maybe(p, 1f / 10000f, Conditions.GASTRO);
            maybe(p, 1f / 20000f, Conditions.PLAGUE);
            maybe(p, 1f / 9000f, Conditions.KLEPTOMANIA);
            maybe(p, 1f / 20000f, Conditions.NIGHT_BLINDNESS);
            if (p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) > 144000) maybe(p, 0.001f, Conditions.SCHIZOPHRENIA);
            if (p.getFoodData().getFoodLevel() <= 4) maybe(p, 0.02f, Conditions.ANEMIA);
            if (p.isPassenger()) maybe(p, 0.01f, Conditions.MOTION_SICKNESS);
            if (Conditions.nearType(p, 3, EntityTypes.CAT, EntityTypes.OCELOT)) maybe(p, 0.01f, Conditions.CAT_ALLERGY);
            if (p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) > 120000) maybe(p, 0.001f, Conditions.INSOMNIA);
            if (Conditions.milkDrank(p)) {
                if (ConditionStore.has(p, Conditions.LACTOSE)) {
                    Fx.startVomit(p, 28);
                    p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 4));
                    Fx.effect(p, MobEffects.NAUSEA, 120, 0);
                    Fx.bar(p, "§eLa leche te ha sentado fatal...");
                } else {
                    maybe(p, 0.05f, Conditions.LACTOSE);
                }
            }
            // Gripe: frio o lluvia
            if (p.isFreezing()) maybe(p, 0.015f, Conditions.FLU);
            else if (p.isInWaterOrRain() && !p.isInWater()) maybe(p, 0.003f, Conditions.FLU);
            // Frio extremo: artrosis
            if (p.isFreezing()) maybe(p, 0.02f, Conditions.ARTHROSIS);
            // Hipo espontaneo
            maybe(p, 1f / 1500f, Conditions.HICCUPS);
            // Narcolepsia: llevar mucho sin dormir
            int sinceRest = p.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            if (sinceRest > 72000) {
                maybe(p, 0.003f, Conditions.NARCOLEPSY);
            }
        }
    }

    /** Carne cruda: probabilidad de salmonela y de parasitos segun el tipo. */
    private static void rawFood(ServerPlayer p, String raw) {
        float sal;
        float par;
        switch (raw) {
            case "pork" -> { sal = 0.30f; par = 0.20f; }
            case "chicken" -> { sal = 0.30f; par = 0.10f; }
            case "beef", "mutton" -> { sal = 0.12f; par = 0.06f; }
            case "rabbit" -> { sal = 0.20f; par = 0.10f; }
            case "cod", "salmon" -> { sal = 0.06f; par = 0.15f; }
            default -> { sal = 0.50f; par = 0.20f; }
        }
        maybe(p, sal, Conditions.SALMONELLA);
        maybe(p, par, Conditions.PARASITES);
    }
}
