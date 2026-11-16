package dev.pandemonium;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pandemonium implements ModInitializer {
    public static final String MOD_ID = "pandemonium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ForgingItems.register();
        ServerLifecycleEvents.SERVER_STARTED.register(ConditionStore::load);
        ServerLifecycleEvents.SERVER_STARTED.register(Settings::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(ConditionStore::save);
        ServerLifecycleEvents.SERVER_STOPPING.register(Settings::save);
        ServerTickEvents.END_SERVER_TICK.register(Pandemonium::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            Payloads.sync(p);
            Payloads.timer(p);
            int n = ConditionStore.list(p).size();
            if (n > 0) {
                Fx.msg(p, "§7Tienes " + n + " condición(es). Escribe §f/pandemonium list§7 para verlas.");
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Payloads.sync(newPlayer));

        Payloads.register();
        Triggers.register();
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> PandemoniumCommands.register(dispatcher));
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            ConditionStore.tickPlayer(p, now);
        }
        Fx.tickEffects(server);
        Settings.tick(server);
        if (now % 8 == 0) Conditions.auras(server, now);
        if (now % 20 == 0 || (now % 4 == 0 && Fx.hasTempPose(server))) Payloads.poseAll(server);
        if (now % 20 == 0) Conditions.tickMasks(server);
        if (now % 6000 == 0) {
            ConditionStore.save(server);
            Settings.save(server);
        }
    }
}
