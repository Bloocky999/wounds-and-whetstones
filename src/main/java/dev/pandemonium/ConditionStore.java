package dev.pandemonium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Guarda quien tiene que condicion. Se guarda en pandemonium.json dentro del mundo.
 * Por cada condicion hay dos entradas: "id" = ticks restantes (-1 = permanente)
 * y "id#age" = ticks que lleva el jugador con ella (para las etapas).
 */
public final class ConditionStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
    private static final Map<UUID, Map<String, Integer>> DATA = new HashMap<>();

    private ConditionStore() {}

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("pandemonium.json");
    }

    public static void load(MinecraftServer server) {
        DATA.clear();
        Path file = file(server);
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, Map<String, Integer>> raw = GSON.fromJson(reader, TYPE);
            if (raw != null) {
                raw.forEach((k, v) -> DATA.put(UUID.fromString(k), new HashMap<>(v)));
            }
        } catch (Exception e) {
            Pandemonium.LOGGER.warn("No se pudo leer pandemonium.json", e);
        }
    }

    public static void save(MinecraftServer server) {
        Map<String, Map<String, Integer>> raw = new HashMap<>();
        DATA.forEach((k, v) -> {
            if (!v.isEmpty()) raw.put(k.toString(), v);
        });
        try (Writer writer = Files.newBufferedWriter(file(server))) {
            GSON.toJson(raw, TYPE, writer);
        } catch (IOException e) {
            Pandemonium.LOGGER.warn("No se pudo guardar pandemonium.json", e);
        }
    }

    public static boolean has(ServerPlayer p, Condition c) {
        Map<String, Integer> m = DATA.get(p.getUUID());
        return m != null && m.containsKey(c.id);
    }

    public static List<Condition> list(ServerPlayer p) {
        List<Condition> out = new ArrayList<>();
        Map<String, Integer> m = DATA.get(p.getUUID());
        if (m == null) return out;
        for (String id : m.keySet()) {
            if (id.indexOf('#') >= 0) continue;
            Condition c = Conditions.get(id);
            if (c != null) out.add(c);
        }
        return out;
    }

    /** Ticks que lleva el jugador con la condicion. */
    public static int age(ServerPlayer p, Condition c) {
        Map<String, Integer> m = DATA.get(p.getUUID());
        if (m == null) return 0;
        return m.getOrDefault(c.id + "#age", 0);
    }

    /** Da la condicion al jugador. Devuelve false si ya la tenia. */
    public static boolean give(ServerPlayer p, Condition c) {
        Map<String, Integer> m = DATA.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        if (m.containsKey(c.id)) return false;
        m.put(c.id, c.duration);
        m.put(c.id + "#age", 0);
        Fx.msg(p, "§c¡Has contraído: §f" + c.name + "§c! §7" + c.description);
        c.onGained(p);
        Payloads.sync(p);
        return true;
    }

    public static boolean remove(ServerPlayer p, Condition c) {
        Map<String, Integer> m = DATA.get(p.getUUID());
        if (m == null || m.remove(c.id) == null) return false;
        m.remove(c.id + "#age");
        c.onLost(p);
        Payloads.sync(p);
        return true;
    }

    public static void removeAll(ServerPlayer p) {
        for (Condition c : list(p)) remove(p, c);
    }

    /** Al morir se curan las condiciones marcadas (enfermedades); las demas se quedan. */
    public static void onDeath(ServerPlayer p) {
        Map<String, Integer> m = DATA.get(p.getUUID());
        if (m != null) {
            for (Condition c : list(p)) {
                if (c.clearedOnDeath()) {
                    m.remove(c.id);
                    m.remove(c.id + "#age");
                }
            }
        }
        Conditions.resetState(p.getUUID());
    }

    public static void tickPlayer(ServerPlayer p, long now) {
        Map<String, Integer> m = DATA.get(p.getUUID());
        if (m == null || m.isEmpty()) return;
        for (String id : new ArrayList<>(m.keySet())) {
            if (id.indexOf('#') >= 0) continue;
            Condition c = Conditions.get(id);
            if (c == null) {
                m.remove(id);
                m.remove(id + "#age");
                continue;
            }
            c.tick(p, now);
            Integer left = m.get(id);
            if (left == null) continue;
            m.merge(id + "#age", 1, Integer::sum);
            if (left > 0) {
                if (left == 1) {
                    m.remove(id);
                    m.remove(id + "#age");
                    c.onLost(p);
                    Payloads.sync(p);
                } else {
                    m.put(id, left - 1);
                }
            }
        }
    }
}
