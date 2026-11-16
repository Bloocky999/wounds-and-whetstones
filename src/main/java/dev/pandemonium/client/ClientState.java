package dev.pandemonium.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lo que el cliente sabe: condiciones activas, efectos de pantalla en curso y tarea de TOC. */
public final class ClientState {
    public static final List<String> ACTIVE = new ArrayList<>();
    /** id del efecto -> {inicio en ms, duracion en ms} */
    private static final Map<String, long[]> FX = new HashMap<>();

    public static String taskText = "";
    public static long taskStart;
    public static long taskDuration;

    private ClientState() {}

    public static void setActive(List<String> ids) {
        ACTIVE.clear();
        ACTIVE.addAll(ids);
    }

    public static boolean has(String id) {
        return ACTIVE.contains(id);
    }

    public static void startFx(String id, int ticks) {
        FX.put(id, new long[] {System.currentTimeMillis(), Math.max(1, ticks) * 50L});
    }

    /** Progreso 0..1 del efecto, o -1 si no esta activo. */
    public static float progress(String id, long now) {
        long[] f = FX.get(id);
        if (f == null) return -1f;
        float p = (now - f[0]) / (float) f[1];
        if (p >= 1f) {
            FX.remove(id);
            return -1f;
        }
        return Math.max(0f, p);
    }

    public static void setTask(String text, int ticks) {
        if (ticks <= 0) {
            taskText = "";
            return;
        }
        taskText = text;
        taskStart = System.currentTimeMillis();
        taskDuration = ticks * 50L;
    }

    public static void reset() {
        ACTIVE.clear();
        FX.clear();
        taskText = "";
    }
}
