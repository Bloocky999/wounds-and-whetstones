package dev.pandemonium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Catalogo de todas las condiciones. Para anadir una nueva: crea otra aqui y ponla en ALL. */
public final class Conditions {
    private static final Map<UUID, OcdTask> OCD_TASKS = new HashMap<>();
    private static final Map<UUID, Integer> SPRINT = new HashMap<>();

    private static final String[] SHOUTS = {
        "¡PATATA!", "¡Biiip bop!", "¡Me gustan los pepinillos!", "¡AAAH!", "¡Tortilla con cebolla!",
        "¡Bu!", "¡Yo no he sido!", "¡Dame un abrazo!", "¡Wiiiii!", "¡Calabacín!"
    };

    // ------------------------------------------------------------- ENFERMEDADES

    public static final Condition ZOMBIE_INFECTION = new Condition(
            "zombie_infection", "Infección zombi",
            "Fiebre, hambre y debilidad. Se contagia a los que estén cerca. Dura 10 minutos.", 12000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 500, 900)) {
                Fx.bar(p, "§2Grrrr... §7(la fiebre sube)");
                Payloads.fx(p, "sick", 60);
                Fx.effect(p, MobEffects.HUNGER, 200, 1);
                Fx.effect(p, MobEffects.WEAKNESS, 200, 0);
                Fx.sound(p, SoundEvents.ZOMBIE_AMBIENT, 1.0f, 0.8f);
            }
            if (now % 20 == 0) {
                // contagio a jugadores a menos de 2,5 bloques
                for (ServerPlayer other : Fx.level(p).players()) {
                    if (other == p || other.distanceTo(p) > 2.5f) continue;
                    if (p.getRandom().nextFloat() < 0.06f) {
                        ConditionStore.give(other, this);
                    }
                }
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            Fx.msg(p, "§aTu fiebre ha bajado. Ya no estás infectado.");
        }
    };

    public static final Condition TETANUS = new Condition(
            "tetanus", "Tétanos",
            "Músculos agarrotados: minas y caminas peor, y te dan espasmos. Dura 8 minutos.", 9600) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id + ":t", now, 300, 500)) {
                Fx.effect(p, MobEffects.MINING_FATIGUE, 240, 1);
                Fx.effect(p, MobEffects.SLOWNESS, 240, 0);
                Fx.bar(p, "§cTus músculos se tensan...");
            }
            if (Fx.due(p, id + ":s", now, 900, 1800)) {
                Fx.jerk(p, Fx.rf(p, 20f), Fx.rf(p, 10f));
                Fx.bar(p, "§c¡Espasmo!");
                Payloads.fx(p, "spasm", 14);
            }
        }
    };

    public static final Condition HICCUPS = new Condition(
            "hiccups", "Hipo",
            "Cada pocos segundos pegas un saltito y la cámara da un tirón. Dura 3 minutos.", 3600) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 300, 700)) {
                p.push(0.0, 0.3, 0.0);
                Fx.jerk(p, Fx.rf(p, 4f), -6f);
                Fx.bar(p, "§e¡Hip!");
                Payloads.fx(p, "hiccup", 10);
                Fx.sound(p, SoundEvents.PLAYER_BURP, 0.6f, 1.6f);
            }
        }
    };

    // ------------------------------------------------------------ ALERGIA / FOBIAS

    public static final Condition POLLEN_ALLERGY = new Condition(
            "pollen_allergy", "Alergia al polen",
            "Cerca de las flores estornudas: la cámara da un tirón y te mareas.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 60, 160) && Fx.nearFlowers(p, 4)) {
                Fx.jerk(p, Fx.rf(p, 8f), 22f);
                Fx.effect(p, MobEffects.NAUSEA, 60, 0);
                Fx.sound(p, SoundEvents.PANDA_SNEEZE, 1.0f, 1.2f);
                Fx.bar(p, "§e¡ACHÚS!");
                Payloads.fx(p, "sneeze", 24);
            }
        }
    };

    public static final Condition ACROPHOBIA = new Condition(
            "acrophobia", "Miedo a las alturas",
            "Si estás muy alto sobre el suelo te mareas, te ralentizas y se te oscurece la vista.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 20 != 0 || p.isInWater() || p.getAbilities().flying) return;
            int air = 0;
            net.minecraft.core.BlockPos pos = p.blockPosition();
            for (int i = 1; i <= 10; i++) {
                if (p.level().getBlockState(pos.below(i)).isAir()) air++;
                else break;
            }
            if (air >= 9) {
                Fx.effect(p, MobEffects.NAUSEA, 100, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 60, 1);
                Fx.effect(p, MobEffects.DARKNESS, 60, 0);
                Fx.bar(p, "§8¡Qué altura!");
                Payloads.fx(p, "vertigo", 40);
            }
        }
    };

    // ------------------------------------------------------------- DISCAPACIDADES

    public static final Condition OCD = new Condition(
            "ocd", "TOC",
            "De vez en cuando te entra una compulsión absurda (saltar N veces, mirar al suelo...). Si no la cumples, te da ansiedad.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            OcdTask t = OCD_TASKS.get(p.getUUID());
            if (t == null) {
                if (Fx.due(p, id, now, 3600, 7200)) {
                    startOcd(p, now);
                }
                return;
            }
            boolean done = false;
            if (t.type == 0) {
                done = jumps(p) - t.startJumps >= t.target;
            } else {
                t.lookTicks = p.getXRot() > 70f ? t.lookTicks + 1 : 0;
                done = t.lookTicks >= 80;
            }
            if (done) {
                OCD_TASKS.remove(p.getUUID());
                Fx.bar(p, "§aAhhh... qué alivio.");
                Payloads.task(p, "", 0);
                Fx.effect(p, MobEffects.SPEED, 200, 0);
            } else if (now >= t.deadline) {
                OCD_TASKS.remove(p.getUUID());
                Fx.bar(p, "§cNo lo has cumplido... ¡ansiedad!");
                Payloads.task(p, "", 0);
                Payloads.fx(p, "anxiety", 160);
                Fx.effect(p, MobEffects.NAUSEA, 200, 0);
                Fx.effect(p, MobEffects.BLINDNESS, 80, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 200, 1);
            } else if (now % 200 == 0) {
                Fx.bar(p, "§e" + t.text + " §7(" + ((t.deadline - now) / 20) + " s)");
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            OCD_TASKS.remove(p.getUUID());
            Payloads.task(p, "", 0);
        }
    };

    public static final Condition ADHD = new Condition(
            "adhd", "TDAH",
            "La ranura seleccionada de la barra de objetos cambia sola de vez en cuando.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 400, 1200)) {
                int slot = p.getRandom().nextInt(9);
                p.getInventory().setSelectedSlot(slot);
                p.connection.send(new ClientboundSetHeldSlotPacket(slot));
                Fx.bar(p, "§e¡Una ardilla!");
            }
        }
    };

    public static final Condition DEMENTIA = new Condition(
            "dementia", "Demencia",
            "Dos objetos de tu inventario se cambian de sitio de vez en cuando. ¿Dónde lo dejé?", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 900, 1800)) {
                Inventory inv = p.getInventory();
                int a = p.getRandom().nextInt(36);
                int b = p.getRandom().nextInt(36);
                if (a == b) return;
                ItemStack first = inv.getItem(a);
                ItemStack second = inv.getItem(b);
                if (first.isEmpty() && second.isEmpty()) return;
                inv.setItem(a, second);
                inv.setItem(b, first);
                p.containerMenu.broadcastChanges();
                Fx.bar(p, "§7¿Dónde dejé eso...?");
            }
        }
    };

    public static final Condition NARCOLEPSY = new Condition(
            "narcolepsy", "Narcolepsia",
            "De repente te quedas dormido unos segundos, en mitad de lo que estés haciendo.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 1800, 4800)) {
                Fx.effect(p, MobEffects.BLINDNESS, 100, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 100, 6);
                Fx.msg(p, "§8Zzzz... te has quedado dormido.");
                Payloads.fx(p, "sleep", 100);
            }
        }
    };

    public static final Condition TOURETTE = new Condition(
            "tourette", "Tics (Tourette)",
            "Tirones de cámara y gritos aleatorios en el chat para todos.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 1200, 3600)) {
                Fx.jerk(p, Fx.rf(p, 30f), Fx.rf(p, 15f));
                Payloads.fx(p, "tic", 10);
                String shout = SHOUTS[p.getRandom().nextInt(SHOUTS.length)];
                Fx.server(p).getPlayerList().broadcastSystemMessage(
                        Component.literal("<" + p.getName().getString() + "> " + shout), false);
            }
        }
    };

    public static final Condition ASTHMA = new Condition(
            "asthma", "Asma",
            "Si corres demasiado tiempo seguido te quedas sin aire y te frenas.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (!p.isSprinting()) {
                SPRINT.remove(p.getUUID());
                return;
            }
            int ticks = SPRINT.merge(p.getUUID(), 1, Integer::sum);
            if (ticks > 200) {
                SPRINT.remove(p.getUUID());
                p.setSprinting(false);
                Fx.effect(p, MobEffects.SLOWNESS, 100, 2);
                Fx.effect(p, MobEffects.HUNGER, 100, 0);
                Fx.bar(p, "§c*cof cof* ¡No puedo respirar!");
                Payloads.fx(p, "gasp", 100);
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            SPRINT.remove(p.getUUID());
        }
    };

    public static final Condition BRITTLE_BONES = new Condition(
            "brittle_bones", "Huesos frágiles",
            "Recibes el doble de daño por caídas.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            // El efecto se aplica en Triggers, al recibir daño por caida
        }
    };

    public static final Condition HYPOCHONDRIA = new Condition(
            "hypochondria", "Hipocondría",
            "Te crees enfermo todo el rato: de vez en cuando te salta un aviso falso de una enfermedad que no tienes.", -1) {
        private final String[] fake = {
            "Peste bubónica", "Ántrax", "Gripe aviar cuadrada", "Escorbuto", "Mal de creeper",
            "Resfriado de Enderman", "Viruela de ovejas", "Fiebre del diamante"
        };

        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 1200, 3600)) {
                String name = fake[p.getRandom().nextInt(fake.length)];
                Fx.msg(p, "§c¡Has contraído: §f" + name + "§c! §7(o eso crees...)");
                Fx.sound(p, SoundEvents.PLAYER_HURT, 0.8f, 0.9f);
                Payloads.fx(p, "sick", 40);
            }
        }
    };

    public static final Condition FOOD_POISONING = new Condition(
            "food_poisoning", "Intoxicación",
            "Náuseas y vómitos: pierdes comida de golpe cada poco. Dura 3 minutos.", 3600) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 300, 600)) {
                Fx.effect(p, MobEffects.NAUSEA, 160, 0);
                int food = p.getFoodData().getFoodLevel();
                p.getFoodData().setFoodLevel(Math.max(0, food - 3));
                Fx.bar(p, "§2*blergh*");
                Fx.sound(p, SoundEvents.PLAYER_BURP, 0.9f, 0.7f);
                Payloads.fx(p, "sick", 50);
            }
        }
    };

    // ------------------------------------------------------------------ REGISTRO

    public static final List<Condition> ALL = List.of(
            ZOMBIE_INFECTION, TETANUS, HICCUPS,
            POLLEN_ALLERGY, ACROPHOBIA,
            OCD, ADHD, DEMENTIA, NARCOLEPSY, TOURETTE, ASTHMA, BRITTLE_BONES,
            HYPOCHONDRIA, FOOD_POISONING);

    private Conditions() {}

    public static Condition get(String id) {
        for (Condition c : ALL) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Condition c : ALL) ids.add(c.id);
        return ids;
    }

    /** Limpia el estado temporal de un jugador. */
    public static void resetState(UUID id) {
        OCD_TASKS.remove(id);
        SPRINT.remove(id);
        Fx.clear(id);
    }

    // ---------------------------------------------------------------------- TOC

    private static final class OcdTask {
        int type;          // 0 = saltar N veces, 1 = mirar al suelo
        int target;
        int startJumps;
        int lookTicks;
        long deadline;
        String text;
    }

    private static int jumps(ServerPlayer p) {
        return p.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
    }

    private static void startOcd(ServerPlayer p, long now) {
        OcdTask t = new OcdTask();
        t.type = p.getRandom().nextInt(2);
        t.deadline = now + 800;
        if (t.type == 0) {
            t.target = 5 + p.getRandom().nextInt(6);
            t.startJumps = jumps(p);
            t.text = "TOC: salta " + t.target + " veces";
        } else {
            t.text = "TOC: mira al suelo 4 segundos sin parar";
        }
        OCD_TASKS.put(p.getUUID(), t);
        Fx.msg(p, "§e" + t.text + " §7(tienes 40 segundos)");
        Payloads.task(p, t.text, 800);
    }
}
