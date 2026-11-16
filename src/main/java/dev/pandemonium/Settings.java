package dev.pandemonium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Ajustes globales del mundo. Modo "scheduled": nada se pilla de forma natural; cada X minutos
 * (igual para todos y sincronizado) el servidor reparte una condicion aleatoria.
 */
public final class Settings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        boolean scheduled = false;
        int intervalMinutes = 10;
        int remaining = 12000;
        boolean contagion = true;
        boolean targetAll = true;
    }

    private static Data d = new Data();

    private Settings() {}

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("pandemonium_settings.json");
    }

    public static void load(MinecraftServer s) {
        d = new Data();
        Path f = file(s);
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            Data read = GSON.fromJson(r, Data.class);
            if (read != null) d = read;
        } catch (Exception e) {
            Pandemonium.LOGGER.warn("No se pudo leer pandemonium_settings.json", e);
        }
    }

    public static void save(MinecraftServer s) {
        try (Writer w = Files.newBufferedWriter(file(s))) {
            GSON.toJson(d, w);
        } catch (IOException e) {
            Pandemonium.LOGGER.warn("No se pudo guardar pandemonium_settings.json", e);
        }
    }

    public static boolean scheduled() {
        return d.scheduled;
    }

    /** Disparadores naturales (golpes, caidas, cofres...) permitidos. */
    public static boolean naturalOn() {
        return !d.scheduled;
    }

    public static boolean contagion() {
        return d.contagion;
    }

    public static boolean targetAll() {
        return d.targetAll;
    }

    public static int intervalMinutes() {
        return d.intervalMinutes;
    }

    public static int remainingTicks() {
        return d.remaining;
    }

    private static int intervalTicks() {
        return d.intervalMinutes * 1200;
    }

    public static void setScheduled(MinecraftServer s, boolean on) {
        d.scheduled = on;
        d.remaining = intervalTicks();
        save(s);
        broadcast(s, on ? "§eModo programado: nada se pilla de forma natural. Una condición nueva cada "
                + d.intervalMinutes + " min." : "§eModo natural: las condiciones se pillan por situaciones.");
        Payloads.timerAll(s);
    }

    public static void setInterval(MinecraftServer s, int minutes) {
        d.intervalMinutes = minutes;
        d.remaining = intervalTicks();
        save(s);
        Payloads.timerAll(s);
    }

    public static void setContagion(MinecraftServer s, boolean on) {
        d.contagion = on;
        save(s);
    }

    public static void setTargetAll(MinecraftServer s, boolean all) {
        d.targetAll = all;
        save(s);
    }

    private static void broadcast(MinecraftServer s, String text) {
        s.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    /** Cuenta atras: solo avanza si hay jugadores conectados. */
    public static void tick(MinecraftServer s) {
        if (!d.scheduled) return;
        List<ServerPlayer> players = eligible(s);
        if (players.isEmpty()) return;
        d.remaining--;
        if (d.remaining == 1200) broadcast(s, "§eNueva condición en 1 minuto...");
        if (d.remaining == 600) broadcast(s, "§eNueva condición en 30 segundos...");
        if (d.remaining == 200) broadcast(s, "§6Nueva condición en 10 segundos...");
        if (d.remaining <= 0) {
            dispense(s);
            d.remaining = intervalTicks();
            Payloads.timerAll(s);
        } else if (d.remaining % 20 == 0) {
            Payloads.timerAll(s);
        }
    }

    private static List<ServerPlayer> eligible(MinecraftServer s) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.isAlive() && !p.isSpectator()) out.add(p);
        }
        return out;
    }

    /** Reparte ya una condicion aleatoria (a todos, o a uno solo segun el ajuste). */
    public static void dispense(MinecraftServer s) {
        List<ServerPlayer> players = eligible(s);
        if (players.isEmpty()) return;
        broadcast(s, "§c§l¡HORA DE LA CONDICIÓN!");
        List<ServerPlayer> chosen = new ArrayList<>(players);
        if (!d.targetAll) {
            ServerPlayer one = players.get(s.overworld().getRandom().nextInt(players.size()));
            chosen = List.of(one);
        }
        for (ServerPlayer p : chosen) {
            List<Condition> pool = new ArrayList<>();
            for (Condition c : Conditions.ALL) {
                if (!ConditionStore.has(p, c)) pool.add(c);
            }
            Payloads.fx(p, "dose", 60);
            Fx.sound(p, SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);
            if (pool.isEmpty()) continue;
            ConditionStore.give(p, pool.get(p.getRandom().nextInt(pool.size())));
        }
    }
}
