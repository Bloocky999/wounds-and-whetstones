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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Paquetes servidor -> cliente. Solo se envian si el cliente tiene el mod. */
public final class Payloads {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Pandemonium.MOD_ID, path);
    }

    /** Entradas: "id", "id|etiqueta" o "fake:Nombre". */
    public record SyncPayload(List<String> entries) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(id("sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncPayload::entries, SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FxPayload(String name, int ticks) implements CustomPacketPayload {
        public static final Type<FxPayload> TYPE = new Type<>(id("fx"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, FxPayload::name, ByteBufCodecs.VAR_INT, FxPayload::ticks, FxPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TaskPayload(String text, int ticks) implements CustomPacketPayload {
        public static final Type<TaskPayload> TYPE = new Type<>(id("task"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TaskPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TaskPayload::text, ByteBufCodecs.VAR_INT, TaskPayload::ticks, TaskPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Entradas "uuid:flags". */
    public record PosePayload(List<String> entries) implements CustomPacketPayload {
        public static final Type<PosePayload> TYPE = new Type<>(id("pose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PosePayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PosePayload::entries, PosePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Estado del temporizador global (modo programado). */
    public record TimerPayload(boolean on, int remaining, int interval) implements CustomPacketPayload {
        public static final Type<TimerPayload> TYPE = new Type<>(id("timer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TimerPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TimerPayload::on, ByteBufCodecs.VAR_INT, TimerPayload::remaining,
                ByteBufCodecs.VAR_INT, TimerPayload::interval, TimerPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Payloads() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FxPayload.TYPE, FxPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TaskPayload.TYPE, TaskPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PosePayload.TYPE, PosePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TimerPayload.TYPE, TimerPayload.CODEC);
    }

    private static void send(ServerPlayer p, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(p, payload.type())) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    public static void sync(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        for (Condition c : ConditionStore.list(p)) {
            String label = c.label(p);
            out.add(label.isEmpty() ? c.id : c.id + "|" + label);
        }
        for (String f : Conditions.fakeNames(p)) out.add("fake:" + f);
        send(p, new SyncPayload(out));
    }

    public static void fx(ServerPlayer p, String name, int ticks) {
        send(p, new FxPayload(name, ticks));
    }

    public static void task(ServerPlayer p, String text, int ticks) {
        send(p, new TaskPayload(text, ticks));
    }

    /** Envia a todos la pose (brazos/inclinacion) de cada jugador con alguna. */
    public static void poseAll(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int flags = Conditions.poseFlags(p);
            if (flags != 0) out.add(p.getUUID() + ":" + flags);
        }
        PosePayload payload = new PosePayload(out);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p, payload);
    }

    public static void timer(ServerPlayer p) {
        send(p, new TimerPayload(Settings.scheduled(), Settings.remainingTicks(), Settings.intervalMinutes() * 1200));
    }

    public static void timerAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) timer(p);
    }
}
