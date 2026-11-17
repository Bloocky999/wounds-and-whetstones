package dev.hammerandhearth.client;

import dev.hammerandhearth.ForgePayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entry point for the smithing minigame only. */
public final class HammerAndHearthClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ForgePayloads.State.TYPE,
                (payload, context) -> context.client().execute(() -> ForgeScreen.receive(payload.encoded())));
    }
}
