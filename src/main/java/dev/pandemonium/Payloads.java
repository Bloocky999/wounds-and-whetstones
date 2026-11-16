package dev.pandemonium;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mensajes servidor -> cliente. Solo se envian a jugadores que tengan el mod en su cliente,
 * asi que los que no lo tengan juegan igual (sin efectos visuales).
 */
public final class Payloads {
    private Payloads() {}

    /** Lista de condiciones activas del jugador. */
    public record SyncPayload(List<String> ids) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncPayload::ids,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Un efecto visual puntual en pantalla (estornudo, tic, sueno...). */
    public record FxPayload(String id, int ticks) implements CustomPacketPayload {
        public static final Type<FxPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "fx"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, FxPayload::id,
                ByteBufCodecs.VAR_INT, FxPayload::ticks,
                FxPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Tarea de TOC en curso (ticks = 0 la oculta). */
    public record TaskPayload(String text, int ticks) implements CustomPacketPayload {
        public static final Type<TaskPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, "task"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TaskPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TaskPayload::text,
                ByteBufCodecs.VAR_INT, TaskPayload::ticks,
                TaskPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FxPayload.TYPE, FxPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TaskPayload.TYPE, TaskPayload.CODEC);
    }

    public static void sync(ServerPlayer p) {
        List<String> ids = new ArrayList<>();
        for (Condition c : ConditionStore.list(p)) ids.add(c.id);
        send(p, new SyncPayload(ids));
    }

    public static void fx(ServerPlayer p, String id, int ticks) {
        send(p, new FxPayload(id, ticks));
    }

    public static void task(ServerPlayer p, String text, int ticks) {
        send(p, new TaskPayload(text, ticks));
    }

    private static void send(ServerPlayer p, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
