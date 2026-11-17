package dev.hammerandhearth;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Standalone entry point for the vintage-inspired smithing mod. */
public final class HammerAndHearth implements ModInitializer {
    public static final String MOD_ID = "hammerandhearth";

    @Override
    public void onInitialize() {
        ForgingItems.register();
        ForgePayloads.register();
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ForgeSmithing.disconnect(handler.getPlayer().getUUID()));
    }
}
