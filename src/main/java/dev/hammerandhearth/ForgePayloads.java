package dev.hammerandhearth;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Small, validated play payloads for the pixel-by-pixel forge screen. */
public final class ForgePayloads {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HammerAndHearth.MOD_ID, path);
    }

    public record Action(int action, int argument) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(id("forge_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::action,
                ByteBufCodecs.VAR_INT, Action::argument,
                Action::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(String encoded) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(id("forge_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, State::encoded, State::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private ForgePayloads() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE, State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,
                (payload, context) -> ForgeSmithing.handle(context.player(), payload.action(), payload.argument()));
    }

    public static void send(ServerPlayer player, String encoded) {
        if (ServerPlayNetworking.canSend(player, State.TYPE)) {
            ServerPlayNetworking.send(player, new State(encoded));
        }
    }
}
