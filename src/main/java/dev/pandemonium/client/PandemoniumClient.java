package dev.pandemonium.client;

import dev.pandemonium.Pandemonium;
import dev.pandemonium.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class PandemoniumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.SyncPayload.TYPE,
                (payload, context) -> ClientState.setActive(payload.ids()));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.FxPayload.TYPE,
                (payload, context) -> ClientState.startFx(payload.id(), payload.ticks()));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.TaskPayload.TYPE,
                (payload, context) -> ClientState.setTask(payload.text(), payload.ticks()));

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "hud"),
                HudRenderer::extract);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.reset());
    }
}
