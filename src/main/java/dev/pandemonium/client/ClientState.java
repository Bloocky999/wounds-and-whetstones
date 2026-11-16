package dev.pandemonium.client;

import dev.pandemonium.Condition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Estado que el cliente recibe del servidor. */
public final class ClientState {
    public record Entry(String id, String label, boolean fake) {}

    public static List<Entry> entries = new ArrayList<>();
    public static final Map<String, Long> FX = new HashMap<>();
    public static final Map<String, Integer> FX_LEN = new HashMap<>();
    public static String taskText = "";
    public static long taskEnd = 0;
    public static Map<UUID, Integer> pose = new HashMap<>();
    public static long poseTime = 0;
    public static boolean timerOn = false;
    public static long timerLeftMs = 0;
    public static long timerIntervalMs = 1;
    public static long timerRecv = 0;

    public static void setTimer(boolean on, int remainingTicks, int intervalTicks) {
        timerOn = on;
        timerLeftMs = remainingTicks * 50L;
        timerIntervalMs = Math.max(1, intervalTicks * 50L);
        timerRecv = System.currentTimeMillis();
    }

    /** Milisegundos que quedan (interpolado entre paquetes del servidor). */
    public static long timerRemainingMs() {
        return Math.max(0, timerLeftMs - (System.currentTimeMillis() - timerRecv));
    }
    /** Ultima vez que los moodles se dibujaron encima de una pantalla (chat/inventario). */
    public static long screenDrawMs = 0;
    public static final Map<String, Long> SEEN = new HashMap<>();
    private static float roll = 0f;

    private ClientState() {}

    public static void setSync(List<String> raw) {
        List<Entry> list = new ArrayList<>();
        for (String s : raw) {
            if (s.startsWith("fake:")) {
                list.add(new Entry(s.substring(5), "", true));
            } else {
                int i = s.indexOf('|');
                list.add(i < 0 ? new Entry(s, "", false) : new Entry(s.substring(0, i), s.substring(i + 1), false));
            }
        }
        long now = System.currentTimeMillis();
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Entry e : list) {
            String k = (e.fake() ? "fake:" : "") + e.id();
            keys.add(k);
            SEEN.putIfAbsent(k, now);
        }
        SEEN.keySet().retainAll(keys);
        entries = list;
    }

    /** Etapa/fase (1-4) de una condicion segun su etiqueta; 1 si no tiene. */
    public static int severity(String id) {
        for (Entry e : entries) {
            if (e.fake() || !e.id().equals(id) || e.label().isEmpty()) continue;
            char c = e.label().charAt(e.label().length() - 1);
            return Character.isDigit(c) ? c - '0' : 1;
        }
        return 1;
    }

    /** 0..1: entrada suave (1,5 s) de un efecto persistente al contraer la condicion. */
    public static float fade(String id) {
        Long seen = SEEN.get(id);
        if (seen == null) return 1f;
        return Math.min(1f, (System.currentTimeMillis() - seen) / 1500f);
    }

    public static boolean has(String id) {
        for (Entry e : entries) if (!e.fake() && e.id().equals(id)) return true;
        return false;
    }

    public static void fx(String name, int ticks) {
        FX.put(name, System.currentTimeMillis() + ticks * 50L);
        FX_LEN.put(name, Math.max(1, ticks * 50));
    }

    /** 0..1: cuanto queda del efecto. */
    public static float fxLeft(String name) {
        Long end = FX.get(name);
        if (end == null) return 0f;
        long left = end - System.currentTimeMillis();
        if (left <= 0) return 0f;
        return Math.min(1f, left / (float) FX_LEN.getOrDefault(name, 1));
    }

    public static void task(String text, int ticks) {
        taskText = text;
        taskEnd = text.isEmpty() ? 0 : System.currentTimeMillis() + ticks * 50L;
    }

    public static void setPose(List<String> raw) {
        Map<UUID, Integer> m = new HashMap<>();
        for (String s : raw) {
            int i = s.lastIndexOf(':');
            try {
                m.put(UUID.fromString(s.substring(0, i)), Integer.parseInt(s.substring(i + 1)));
            } catch (Exception ignored) {
            }
        }
        pose = m;
        poseTime = System.currentTimeMillis();
    }

    /** Pesos de pose con muelle amortiguado: dan el efecto "bouncy" (se pasan un poco y rebotan). */
    public static final class Blend {
        final float[] w = new float[13];
        final float[] v = new float[13];
        long last;

        public float w(int flag) {
            return w[Integer.numberOfTrailingZeros(flag)];
        }
    }

    private static final Map<UUID, Blend> BLENDS = new HashMap<>();

    /** Mezcla de poses (con muelle) del jugador que esta en esa posicion, o null si no tiene ninguna. */
    public static Blend blendAt(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Player found = null;
        for (Player p : mc.level.players()) {
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            if (dx * dx + dy * dy + dz * dz < 2.25) {
                found = p;
                break;
            }
        }
        if (found == null) return null;
        UUID id = found.getUUID();
        long now = System.currentTimeMillis();
        int flags = pose.getOrDefault(id, 0);
        if (now - poseTime > 1500) {
            flags &= ~(Condition.POSE_COUGH | Condition.POSE_VOMIT | Condition.POSE_SCRATCH
                    | Condition.POSE_COVER | Condition.POSE_SLUMP | Condition.POSE_TWITCH);
        }
        Blend b = BLENDS.get(id);
        if (b == null) {
            if (flags == 0) return null;
            b = new Blend();
            b.last = now;
            BLENDS.put(id, b);
        }
        float dt = Math.min(0.05f, (now - b.last) / 1000f);
        if (dt > 0.001f) {
            b.last = now;
            float h = dt / 3f;
            for (int s = 0; s < 3; s++) {
                for (int i = 0; i < 13; i++) {
                    float target = (flags >> i) & 1;
                    float a = 140f * (target - b.w[i]) - 9f * b.v[i];
                    b.v[i] += a * h;
                    b.w[i] += b.v[i] * h;
                }
            }
        }
        if (flags == 0) {
            boolean settled = true;
            for (int i = 0; i < 13; i++) {
                if (Math.abs(b.w[i]) > 0.01f || Math.abs(b.v[i]) > 0.02f) settled = false;
            }
            if (settled) {
                BLENDS.remove(id);
                return null;
            }
        }
        return b;
    }

    /** Pose del jugador mas cercano a esa posicion (el renderer no nos dice el UUID). */
    public static int poseFlagsAt(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || pose.isEmpty()) return 0;
        for (Player p : mc.level.players()) {
            Integer flags = pose.get(p.getUUID());
            if (flags == null) continue;
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            if (dx * dx + dy * dy + dz * dz < 2.25) {
                if (System.currentTimeMillis() - poseTime > 1500) {
                    flags &= ~(Condition.POSE_COUGH | Condition.POSE_VOMIT | Condition.POSE_SCRATCH
                            | Condition.POSE_COVER | Condition.POSE_SLUMP | Condition.POSE_TWITCH);
                }
                return flags;
            }
        }
        return 0;
    }

    /** Inclinacion de la camara en radianes (suavizada). */
    public static float cameraRoll() {
        Minecraft mc = Minecraft.getInstance();
        float target = 0f;
        if (mc.player != null) {
            Integer f = pose.get(mc.player.getUUID());
            if (f != null) {
                if ((f & Condition.POSE_LEAN_R) != 0) target = 0.26f;
                if ((f & Condition.POSE_LEAN_L) != 0) target = -0.26f;
            }
        }
        roll += (target - roll) * 0.1f;
        double t = System.currentTimeMillis();
        float extra = 0f;
        if (has("tremors")) extra += 0.015f * (float) Math.sin(t * 0.045) + 0.01f * (float) Math.sin(t * 0.11);
        if (has("frostbite")) extra += 0.012f * (float) Math.sin(t * 0.06);
        if (has("concussion")) extra += 0.05f * (float) Math.sin(t / 350.0);
        if (has("heatstroke")) extra += 0.02f * (float) Math.sin(t / 500.0);
        if (has("migraine")) extra += 0.02f * (float) Math.sin(t / 250.0);
        return roll + extra;
    }
}
