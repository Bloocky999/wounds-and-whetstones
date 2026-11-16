package dev.pandemonium;

import net.minecraft.server.level.ServerPlayer;

/** Una enfermedad, fobia o discapacidad que puede tener un jugador. */
public abstract class Condition {
    public final String id;
    public final String name;
    public final String description;
    /** Duracion en ticks (20 = 1 segundo). -1 = permanente hasta que se quite con un comando. */
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

    /** Se llama cada tick mientras el jugador la tiene. */
    public abstract void tick(ServerPlayer player, long now);

    public void onGained(ServerPlayer player) {}

    public void onLost(ServerPlayer player) {}
}
