package dev.pandemonium;

import net.minecraft.server.level.ServerPlayer;

/** Una enfermedad, fobia o discapacidad que puede tener un jugador. */
public abstract class Condition {
    /** Brazos estirados hacia delante, como un zombi. */
    public static final int POSE_ARMS = 1;
    /** Torso doblado hacia un lado (artrosis). */
    public static final int POSE_LEAN_R = 2;
    public static final int POSE_LEAN_L = 4;
    /** Animaciones cortas: toser, temblar de frio, vomitar. */
    public static final int POSE_COUGH = 8;
    public static final int POSE_SHIVER = 16;
    public static final int POSE_VOMIT = 32;
    /** Encorvado (fiebre, frio, asma, zombi avanzado). */
    public static final int POSE_HUNCH = 64;
    /** Cojera. */
    public static final int POSE_LIMP = 128;
    /** Sacudidas bruscas (tics, convulsiones). */
    public static final int POSE_TWITCH = 256;
    /** Rascarse (varicela). */
    public static final int POSE_SCRATCH = 512;
    /** Taparse la cara (panico, miedo). */
    public static final int POSE_COVER = 1024;
    /** Tantear con los brazos (ceguera). */
    public static final int POSE_FEEL = 2048;
    /** Desplomarse (narcolepsia, desmayo). */
    public static final int POSE_SLUMP = 4096;
    /** Brazo izquierdo inutilizado, colgando. */
    public static final int POSE_LAMEARM = 8192;

    public final String id;
    public final String name;
    public final String description;
    /** Duracion en ticks (20 = 1 segundo). -1 = permanente. */
    public final int duration;

    protected Condition(String id, String name, String description, int duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
    }

    public boolean isTemporary() {
        return duration > 0;
    }

    /** Si al morir se cura. Por defecto, las temporales si; las permanentes se pueden marcar aparte. */
    public boolean clearedOnDeath() {
        return isTemporary();
    }

    /** Texto extra que sale junto al nombre en el panel (por ejemplo, "Etapa 2"). */
    public String label(ServerPlayer player) {
        return "";
    }

    /** Pose del cuerpo que ven los demas jugadores (POSE_*). */
    public int poseFlags(ServerPlayer player) {
        return 0;
    }

    /** Se llama cada tick mientras el jugador la tiene. */
    public abstract void tick(ServerPlayer player, long now);

    public void onGained(ServerPlayer player) {}

    public void onLost(ServerPlayer player) {}
}
