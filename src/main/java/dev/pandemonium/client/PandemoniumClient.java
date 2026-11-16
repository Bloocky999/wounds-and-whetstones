package dev.pandemonium.client;

import dev.pandemonium.Payloads;
import dev.pandemonium.Pandemonium;
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
                (payload, context) -> context.client().execute(() -> ClientState.setSync(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.FxPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientState.fx(payload.name(), payload.ticks())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.TaskPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientState.task(payload.text(), payload.ticks())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.PosePayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientState.setPose(payload.entries())));

        ClientPlayNetworking.registerGlobalReceiver(Payloads.TimerPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientState.setTimer(payload.on(), payload.remaining(), payload.interval())));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "hud"), HudRenderer::extract);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientState.entries = new java.util.ArrayList<>();
            ClientState.FX.clear();
            ClientState.pose = new java.util.HashMap<>();
            ClientState.task("", 0);
            ClientState.timerOn = false;
            ClientState.colorblindOn = false;
        });
    }
}
