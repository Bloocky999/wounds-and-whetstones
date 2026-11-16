package dev.pandemonium;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Catalogo de todas las condiciones. Para anadir una nueva: crea otra aqui y ponla en ALL. */
public final class Conditions {
    private static final Map<UUID, OcdTask> OCD_TASKS = new HashMap<>();
    private static final Map<UUID, Integer> SPRINT = new HashMap<>();
    private static final Map<UUID, Integer> ASTHMA_ATTACK = new HashMap<>();
    private static final Map<String, Integer> LAST_STAGE = new HashMap<>();
    private static final Map<UUID, List<Fake>> FAKES = new HashMap<>();

    private record Fake(String name, long expires) {}

    private static final String[] SHOUTS = {
        "¡PATATA!", "¡Biiip bop!", "¡Me gustan los pepinillos!", "¡AAAH!", "¡Tortilla con cebolla!",
        "¡Bu!", "¡Yo no he sido!", "¡Dame un abrazo!", "¡Wiiiii!", "¡Calabacín!"
    };

    private static final String[] ZOMBIE_STAGES = {
        "",
        "§2Tus brazos se tensan hacia delante... §7(etapa 2)",
        "§6El sol empieza a hacerte daño. §7(etapa 3)",
        "§4Ya casi no te controlas: ¡quieres pegar a los que tienes cerca! §7(etapa 4)"
    };

    private static final String[] TETANUS_STAGES = {
        "",
        "§cLos espasmos son cada vez más fuertes. §7(fase 2)",
        "§4Tu mandíbula se bloquea y casi no puedes moverte. §7(fase 3)"
    };

    // ------------------------------------------------------------- ENFERMEDADES

    public static final Condition ZOMBIE_INFECTION = new Condition(
            "zombie_infection", "Infección zombi",
            "Avanza por etapas hasta que mueras: fiebre, brazos de zombi, el sol te quema y te entran ganas de pegar.", -1) {
        private int stage(ServerPlayer p) {
            int age = ConditionStore.age(p, this);
            if (age < 2400) return 1;
            if (age < 6000) return 2;
            if (age < 10800) return 3;
            return 4;
        }

        @Override
        public boolean clearedOnDeath() {
            return true;
        }

        @Override
        public String label(ServerPlayer p) {
            return "Etapa " + stage(p);
        }

        @Override
        public int poseFlags(ServerPlayer p) {
            int st = stage(p);
            int flags = st >= 2 ? POSE_ARMS : 0;
            if (st >= 3) flags |= POSE_HUNCH;
            if (st >= 4) flags |= POSE_LIMP;
            return flags;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            int stage = stage(p);
            announceStage(p, id, stage, ZOMBIE_STAGES);

            if (Fx.due(p, id + ":g", now, 500, 900)) {
                Fx.bar(p, "§2Grrrr...");
                Fx.effect(p, MobEffects.HUNGER, 100, 0);
                Fx.sound(p, SoundEvents.ZOMBIE_AMBIENT, 1.0f, 0.8f);
                Payloads.fx(p, "sick", 60);
            }
            if (stage >= 2 && now % 40 == 0) {
                Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
            }
            if (stage >= 2 && now % 20 == 0) {
                for (ServerPlayer other : Fx.level(p).players()) {
                    if (other == p || other.distanceTo(p) > 2.5f) continue;
                    if (spreads(p, other, 0.06f)) {
                        ConditionStore.give(other, this);
                    }
                }
            }
            if (stage >= 2 && Fx.due(p, id + ":c", now, 400, 900)) {
                cough(p, this, 0.45f);
            }
            if (stage >= 3 && now % 20 == 0) {
                sunBurn(p);
            }
            if (stage >= 4 && Fx.due(p, id + ":u", now, 600, 1200)) {
                attackUrge(p);
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            LAST_STAGE.remove(p.getUUID() + id);
            Fx.msg(p, "§aLa infección ha desaparecido.");
        }
    };

    public static final Condition TETANUS = new Condition(
            "tetanus", "Tétanos",
            "Músculos cada vez más agarrotados, con espasmos. Va por fases y dura hasta que mueras.", -1) {
        private int stage(ServerPlayer p) {
            int age = ConditionStore.age(p, this);
            if (age < 3600) return 1;
            if (age < 8400) return 2;
            return 3;
        }

        @Override
        public boolean clearedOnDeath() {
            return true;
        }

        @Override
        public String label(ServerPlayer p) {
            return "Fase " + stage(p);
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            int stage = stage(p);
            announceStage(p, id, stage, TETANUS_STAGES);

            if (stage == 1 && Fx.due(p, id + ":t", now, 300, 500)) {
                Fx.effect(p, MobEffects.MINING_FATIGUE, 240, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 240, 0);
                Fx.bar(p, "§cTus músculos se tensan...");
            }
            if (stage >= 2 && now % 40 == 0) {
                Fx.effectHidden(p, MobEffects.MINING_FATIGUE, 60, stage - 1);
                Fx.effectHidden(p, MobEffects.SLOWNESS, 60, stage - 2);
            }
            int minSpasm = stage == 1 ? 900 : (stage == 2 ? 500 : 250);
            if (Fx.due(p, id + ":s", now, minSpasm, minSpasm * 2)) {
                Fx.jerk(p, Fx.rf(p, 12f + 8f * stage), Fx.rf(p, 8f + 4f * stage));
                Fx.bar(p, "§c¡Espasmo!");
                Payloads.fx(p, "spasm", 14);
            }
            if (stage == 3 && Fx.due(p, id + ":j", now, 600, 1200)) {
                Fx.bar(p, "§4¡Mandíbula bloqueada!");
                Fx.effect(p, MobEffects.HUNGER, 200, 1);
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            LAST_STAGE.remove(p.getUUID() + id);
        }
    };

    public static final Condition FLU = new Condition(
            "flu", "Gripe",
            "Tosiendo y con fiebre durante 10 minutos. Si toses a alguien en la cara sin mascarilla (casco puesto), se la pegas.", 12000) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_HUNCH;
        }

        @Override
        public String label(ServerPlayer p) {
            return "tos";
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id + ":c", now, 300, 700)) {
                cough(p, this, 0.6f);
            }
            if (now % 20 == 0) {
                // contagio por cercania constante
                for (ServerPlayer other : Fx.level(p).players()) {
                    if (other == p || other.distanceTo(p) > 2.0f) continue;
                    if (spreads(p, other, 0.03f)) ConditionStore.give(other, this);
                }
            }
            if (Fx.due(p, id + ":f", now, 1200, 2400)) {
                Fx.effect(p, MobEffects.WEAKNESS, 200, 0);
                Fx.bar(p, "§eTienes fiebre...");
                Payloads.fx(p, "sick", 60);
            }
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
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
                Fx.spray(p, ParticleTypes.BUBBLE_POP, 4, 0.12, 0.3);
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
                Fx.startVomit(p, 28);
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
                Fx.sound(p, SoundEvents.PANDA_SNEEZE, 1.0f, 0.72f);
                Fx.bar(p, "§e¡ACHÚS!");
                Payloads.fx(p, "sneeze", 24);
                Fx.spray(p, ParticleTypes.SNEEZE, 10, 0.25, 0.5);
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
            BlockPos pos = p.blockPosition();
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
            "Compulsiones según lo que tengas cerca: saltar N veces, romper N bloques, mirar a un bicho, tocar un cofre... Con tiempo límite; si te pasas o no llegas, te haces daño.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            OcdTask t = OCD_TASKS.get(p.getUUID());
            if (t == null) {
                if (Fx.due(p, id, now, 3600, 7200)) startOcd(p, now);
                return;
            }
            switch (t.kind) {
                case "jumps" -> {
                    int c = jumps(p) - t.startJumps;
                    if (c != t.count) ocdProgress(p, t, c, now);
                }
                case "sneak" -> {
                    boolean s = p.isShiftKeyDown();
                    if (s && !t.lastSneak) ocdProgress(p, t, t.count + 1, now);
                    t.lastSneak = s;
                }
                case "lookdown" -> {
                    t.lookTicks = p.getXRot() > 70f ? t.lookTicks + 1 : 0;
                    if (t.lookTicks >= 80) {
                        ocdSuccess(p);
                        return;
                    }
                }
                case "stare" -> {
                    Entity e = Fx.level(p).getEntity(t.entity);
                    if (e == null || !e.isAlive()) {
                        OCD_TASKS.remove(p.getUUID());
                        Payloads.task(p, "", 0);
                        return;
                    }
                    Vec3 d = e.getEyePosition().subtract(p.getEyePosition());
                    boolean looking = d.length() < 16 && d.normalize().dot(p.getLookAngle()) > 0.96;
                    t.lookTicks = looking ? t.lookTicks + 1 : Math.max(0, t.lookTicks - 1);
                    if (t.lookTicks >= 100) {
                        ocdSuccess(p);
                        return;
                    }
                }
                default -> { }
            }
            if (OCD_TASKS.get(p.getUUID()) == null) return;
            // tareas de contar: al llegar justo al numero hay que quedarse quieto 2,5 s sin pasarse
            if (t.counting() && t.count == t.target) {
                t.settle++;
                if (t.settle >= 50) {
                    ocdSuccess(p);
                    return;
                }
            }
            if (now >= t.deadline) {
                ocdFail(p, "§c¡Se acabó el tiempo!", 4f);
            } else if (now % 200 == 0) {
                Fx.bar(p, "§e" + t.text + " §7(" + t.count + "/" + t.target + ", " + ((t.deadline - now) / 20) + " s)");
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
                Fx.pose(p, POSE_SLUMP, 100);
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
                Fx.pose(p, POSE_TWITCH, 10);
                String shout = SHOUTS[p.getRandom().nextInt(SHOUTS.length)];
                Fx.server(p).getPlayerList().broadcastSystemMessage(
                        Component.literal("<" + p.getName().getString() + "> " + shout), false);
            }
        }
    };

    public static final Condition ASTHMA = new Condition(
            "asthma", "Asma",
            "Si corres demasiado tiempo seguido te da un ataque de 20 segundos: no puedes correr, toses y te ahogas.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            UUID uuid = p.getUUID();
            Integer attack = ASTHMA_ATTACK.get(uuid);
            if (attack != null) {
                // ataque en curso: no se puede correr, toses con particulas y sacudidas
                if (p.isSprinting()) p.setSprinting(false);
                if (now % 10 == 0) {
                    Fx.effectHidden(p, MobEffects.SLOWNESS, 25, 1);
                }
                if (attack % 25 == 0) {
                    Fx.jerk(p, Fx.rf(p, 3f), -5f);
                    Fx.spray(p, ParticleTypes.CLOUD, 6, 0.12, 0.4);
                    Fx.sound(p, SoundEvents.PLAYER_BREATH, 1.0f, 0.8f);
                    Fx.bar(p, "§c*cof cof*");
                }
                if (attack <= 1) {
                    ASTHMA_ATTACK.remove(uuid);
                    Fx.bar(p, "§aTe recuperas poco a poco.");
                } else {
                    ASTHMA_ATTACK.put(uuid, attack - 1);
                }
                return;
            }
            if (!p.isSprinting()) {
                SPRINT.remove(uuid);
                return;
            }
            int ticks = SPRINT.merge(uuid, 1, Integer::sum);
            if (ticks > 200) {
                SPRINT.remove(uuid);
                ASTHMA_ATTACK.put(uuid, 400);
                p.setSprinting(false);
                Fx.effect(p, MobEffects.HUNGER, 200, 0);
                Fx.bar(p, "§c¡No puedo respirar!");
                Payloads.fx(p, "gasp", 400);
                Fx.pose(p, POSE_HUNCH, 400);
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            SPRINT.remove(p.getUUID());
            ASTHMA_ATTACK.remove(p.getUUID());
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

    public static final Condition ARTHROSIS = new Condition(
            "arthrosis", "Artrosis",
            "Tu torso queda doblado hacia un lado (15 grados) y la cámara también. Te mueves peor y te crujen los huesos.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return (p.getUUID().hashCode() & 1) == 0 ? POSE_LEAN_R : POSE_LEAN_L;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) {
                Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
            }
            if (Fx.due(p, id, now, 900, 1800)) {
                Fx.effect(p, MobEffects.MINING_FATIGUE, 120, 0);
                Fx.sound(p, SoundEvents.BONE_MEAL_USE, 1.0f, 0.5f);
                Fx.bar(p, "§7*crac*");
            }
        }
    };

    public static final Condition BLINDNESS = new Condition(
            "blindness", "Ceguera",
            "Solo ves lo que tienes pegado. Los ruidos y movimientos cercanos aparecen como destellos en pantalla.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_FEEL;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 20 == 0) {
                Fx.effectHidden(p, MobEffects.BLINDNESS, 100, 0);
            }
        }
    };

    public static final Condition HYPOCHONDRIA = new Condition(
            "hypochondria", "Hipocondría",
            "Te crees enfermo todo el rato: te salen en el panel enfermedades falsas que no tienes.", -1) {
        private final String[] fake = {
            "Peste bubónica", "Ántrax", "Gripe aviar cuadrada", "Escorbuto", "Mal de creeper",
            "Resfriado de Enderman", "Viruela de ovejas", "Fiebre del diamante", "Tos de piglin"
        };

        @Override
        public void tick(ServerPlayer p, long now) {
            List<Fake> list = FAKES.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
            boolean changed = false;
            if (now % 20 == 0) {
                Iterator<Fake> it = list.iterator();
                while (it.hasNext()) {
                    if (it.next().expires() <= now) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (Fx.due(p, id, now, 900, 2400)) {
                String name = fake[p.getRandom().nextInt(fake.length)];
                list.add(new Fake(name, now + 4800));
                Fx.msg(p, "§c¡Has contraído: §f" + name + "§c! §7(o eso crees...)");
                Fx.sound(p, SoundEvents.PLAYER_HURT, 0.8f, 0.9f);
                Payloads.fx(p, "sick", 40);
                changed = true;
            }
            if (changed) Payloads.sync(p);
        }

        @Override
        public void onLost(ServerPlayer p) {
            FAKES.remove(p.getUUID());
        }
    };

    // ------------------------------------------------------------ MAS ENFERMEDADES

    public static final Condition FROSTBITE = new Condition(
            "frostbite", "Congelación",
            "Tiritas, vas lento y los copos de nieve te rodean. Se pilla al congelarte del todo. Dura 4 minutos.", 4800) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_SHIVER | POSE_HUNCH;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 1);
            if (now % 8 == 0) {
                Fx.level(p).sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
            }
            if (Fx.due(p, id, now, 60, 140)) {
                Fx.jerk(p, Fx.rf(p, 2.5f), Fx.rf(p, 1.5f));
                Fx.sound(p, SoundEvents.PLAYER_HURT_FREEZE, 0.6f, 1.4f);
                Payloads.fx(p, "frost", 30);
            }
        }
    };

    public static final Condition HEATSTROKE = new Condition(
            "heatstroke", "Golpe de calor",
            "Sudas, te mareas y te debilitas. Se pilla pasando mucho rato en el Nether. Mojarte te alivia. Dura 3 minutos.", 3600) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_HUNCH;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.WEAKNESS, 60, 0);
            if (now % 6 == 0) {
                Fx.level(p).sendParticles(ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 1.9, p.getZ(), 2, 0.2, 0.0, 0.2, 0.0);
            }
            if (Fx.due(p, id, now, 200, 400)) {
                Fx.effect(p, MobEffects.NAUSEA, 100, 0);
                Fx.bar(p, "§6Qué calor...");
            }
            if (p.isInWater() && ConditionStore.age(p, this) > 100) {
                Fx.msg(p, "§bTe has refrescado y se te pasa el golpe de calor.");
                ConditionStore.remove(p, this);
            }
        }
    };

    public static final Condition RABIES = new Condition(
            "rabies", "Rabia",
            "Mordida de lobo o zorro. Fases: fiebre, miedo al agua con espuma en la boca, y ganas de morder a todo el mundo. Se contagia por golpes. Hasta que mueras.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return stage(p) >= 3 ? (POSE_ARMS | POSE_HUNCH) : 0;
        }

        private int stage(ServerPlayer p) {
            int age = ConditionStore.age(p, this);
            if (age < 3600) return 1;
            if (age < 9000) return 2;
            return 3;
        }

        @Override
        public boolean clearedOnDeath() {
            return true;
        }

        @Override
        public String label(ServerPlayer p) {
            return "Fase " + stage(p);
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            int stage = stage(p);
            announceStage(p, id, stage, new String[] {
                "", "§cTe da miedo el agua y echas espuma por la boca. §7(fase 2)",
                "§4La rabia te domina: quieres morder a todo el mundo. §7(fase 3)"});
            if (stage == 1 && Fx.due(p, id + ":f", now, 600, 1200)) {
                Fx.effect(p, MobEffects.WEAKNESS, 200, 0);
                Fx.bar(p, "§cTienes fiebre y te duele la herida...");
                Payloads.fx(p, "sick", 50);
            }
            if (stage >= 2) {
                if (now % 30 == 0) {
                    Fx.spray(p, ParticleTypes.BUBBLE_POP, 2, 0.04, 0.15);
                    Fx.spray(p, ParticleTypes.CLOUD, 1, 0.02, 0.1);
                }
                if (now % 10 == 0 && p.isInWaterOrRain()) {
                    Fx.jerk(p, Fx.rf(p, 20f), Fx.rf(p, 10f));
                    Fx.effect(p, MobEffects.NAUSEA, 100, 0);
                    Fx.effect(p, MobEffects.BLINDNESS, 40, 0);
                    Fx.bar(p, "§4¡AGUA! ¡NO!");
                    Fx.sound(p, SoundEvents.ZOMBIE_AMBIENT, 0.8f, 0.5f);
                }
            }
            if (stage >= 3 && Fx.due(p, id + ":u", now, 400, 800)) {
                attackUrge(p);
            }
        }
    };

    public static final Condition POX = new Condition(
            "pox", "Varicela",
            "Granitos rojos y picores. Muy contagiosa por cercanía. Dura 10 minutos.", 12000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 400, 900)) {
                Fx.jerk(p, Fx.rf(p, 3f), Fx.rf(p, 2f));
                Fx.sound(p, SoundEvents.PLAYER_HURT, 0.3f, 1.8f);
                Fx.bar(p, "§c*te rascas*");
                Fx.pose(p, POSE_SCRATCH, 40);
            }
            if (now % 20 == 0) {
                for (ServerPlayer other : Fx.level(p).players()) {
                    if (other == p || other.distanceTo(p) > 2.5f) continue;
                    if (spreads(p, other, 0.04f)) ConditionStore.give(other, this);
                }
            }
        }
    };

    public static final Condition CONCUSSION = new Condition(
            "concussion", "Conmoción",
            "Tras un golpe fuerte: pitido en los oídos, tropiezas y la vista se tambalea. Dura 2 minutos.", 2400) {
        @Override
        public void onGained(ServerPlayer p) {
            Payloads.fx(p, "ringing", 120);
            Fx.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id, now, 160, 360)) {
                Fx.jerk(p, Fx.rf(p, 12f), Fx.rf(p, 6f));
                Fx.effect(p, MobEffects.NAUSEA, 80, 0);
                p.push(Fx.rf(p, 0.25f), 0.0, Fx.rf(p, 0.25f));
                Payloads.fx(p, "ringing", 50);
                Fx.bar(p, "§7*tropiezas*");
                Fx.pose(p, POSE_TWITCH, 20);
            }
        }
    };

    public static final Condition LACTOSE = new Condition(
            "lactose_intolerance", "Intolerancia a la lactosa",
            "Cada vez que bebes leche te sienta fatal: vómitos y hambre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            // el disparador esta en Triggers (al beber leche)
        }
    };

    public static final Condition MIGRAINE = new Condition(
            "migraine", "Migraña",
            "Luces que molestan, latidos en la cabeza y náuseas. Dura 3 minutos.", 3600) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 30 == 0) Fx.sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.5f, 1.0f);
            if (Fx.due(p, id, now, 100, 220)) {
                Fx.effect(p, MobEffects.NAUSEA, 100, 0);
                Fx.effect(p, MobEffects.DARKNESS, 60, 0);
                Payloads.fx(p, "migraine", 60);
            }
        }
    };

    public static final Condition TREMORS = new Condition(
            "tremors", "Temblores",
            "Te tiemblan las manos: la cámara vibra siempre y minas peor. Para siempre.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_SHIVER;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.MINING_FATIGUE, 50, 0);
        }
    };


    public static final Condition TINNITUS = new Condition(
            "tinnitus", "Tinnitus",
            "Un pitido agudo y constante en los oídos, que a ratos sube de volumen. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id + ":b", now, 50, 110)) {
                Fx.sound(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.55f, 2.4f);
                Payloads.fx(p, "tinnitus", 80);
            }
            if (Fx.due(p, id + ":l", now, 1200, 3000)) {
                Fx.effect(p, MobEffects.NAUSEA, 60, 0);
                Fx.sound(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 2.5f);
                Payloads.fx(p, "ringing", 90);
                Fx.bar(p, "§7iiiiiiiiiiiii...");
            }
        }
    };

    // ------------------------------------------------------- OTRA TANDA DE CONDICIONES

    public static final Condition INSOMNIA = new Condition(
            "insomnia", "Insomnio",
            "Te despiertas a los pocos segundos de meterte en la cama. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 20 == 0 && p.isSleeping() && p.getRandom().nextFloat() < 0.35f) {
                p.stopSleeping();
                Fx.msg(p, "§8No consigues dormir...");
                Payloads.fx(p, "insomnia", 80);
            }
        }
    };

    public static final Condition COLORBLIND = new Condition(
            "colorblind", "Daltonismo",
            "Los bloques cambian de aspecto al azar (según tu semilla): la roca como obsidiana, los troncos como piedra... Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            // solo visual (cliente)
        }
    };

    public static final Condition SPRAIN = new Condition(
            "sprain", "Esguince",
            "Te duele el tobillo: no puedes correr y cojeas. Dura 5 minutos.", 6000) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_LIMP;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (p.isSprinting()) p.setSprinting(false);
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
            if (Fx.due(p, id, now, 300, 700)) {
                Fx.jerk(p, 0f, 5f);
                Fx.sound(p, SoundEvents.PLAYER_HURT, 0.4f, 1.3f);
                Fx.bar(p, "§e¡Ay, el tobillo!");
                Payloads.fx(p, "sick", 15);
            }
        }
    };

    public static final Condition MOTION_SICKNESS = new Condition(
            "motion_sickness", "Cinetosis",
            "Montar en barco, vagoneta o caballo te marea y te hace vomitar. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (!p.isPassenger()) return;
            if (now % 40 == 0) Fx.effect(p, MobEffects.NAUSEA, 120, 0);
            if (Fx.due(p, id, now, 200, 500)) {
                Fx.startVomit(p, 28);
                p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 2));
                Fx.bar(p, "§2Qué mareo...");
            }
        }
    };

    public static final Condition ANEMIA = new Condition(
            "anemia", "Anemia",
            "Por pasar hambre: te debilitas y a veces se te va la vista del todo. Dura 5 minutos.", 6000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.WEAKNESS, 60, 0);
            if (Fx.due(p, id, now, 400, 900)) {
                Fx.effect(p, MobEffects.BLINDNESS, 60, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 60, 3);
                Payloads.fx(p, "faint", 60);
                Fx.bar(p, "§8Se te va la vista...");
                Fx.pose(p, POSE_SLUMP, 60);
            }
        }
    };

    public static final Condition PANIC = new Condition(
            "panic", "Trastorno de pánico",
            "Si hay monstruos cerca te late el corazón a mil y te dan ataques de pánico. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 10 != 0) return;
            AABB box = p.getBoundingBox().inflate(8);
            boolean near = !Fx.level(p).getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e.getType().getCategory() == MobCategory.MONSTER).isEmpty();
            if (!near) return;
            if (now % 20 == 0) Fx.sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.6f, 1.2f);
            if (Fx.due(p, id, now, 300, 900)) {
                Payloads.fx(p, "anxiety", 120);
                Fx.effect(p, MobEffects.SLOWNESS, 100, 1);
                Fx.effect(p, MobEffects.WEAKNESS, 100, 0);
                Fx.jerk(p, Fx.rf(p, 8f), Fx.rf(p, 5f));
                Fx.level(p).sendParticles(ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 1.9, p.getZ(), 4, 0.2, 0, 0.2, 0);
                Fx.bar(p, "§8¡El corazón a mil!");
                Fx.pose(p, POSE_COVER, 60);
            }
        }
    };

    public static final Condition ARACHNOPHOBIA = new Condition(
            "arachnophobia", "Aracnofobia",
            "Las arañas cerca te paralizan de miedo. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 10 != 0) return;
            if (!nearType(p, 10, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER)) return;
            if (now % 20 == 0) Fx.sound(p, SoundEvents.WARDEN_HEARTBEAT, 0.7f, 1.3f);
            if (Fx.due(p, id, now, 100, 200)) {
                Fx.jerk(p, Fx.rf(p, 10f), Fx.rf(p, 6f));
                Fx.effect(p, MobEffects.NAUSEA, 80, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 60, 1);
                Payloads.fx(p, "anxiety", 60);
                Fx.bar(p, "§8¡ARAÑA!");
                Fx.pose(p, POSE_COVER, 40);
            }
        }
    };

    public static final Condition CLAUSTROPHOBIA = new Condition(
            "claustrophobia", "Claustrofobia",
            "En sitios muy estrechos te agobias y te mareas. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 20 != 0) return;
            BlockPos c = p.blockPosition().above();
            int solid = 0;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                var st = p.level().getBlockState(pos);
                if (!st.isAir() && st.getFluidState().isEmpty()) solid++;
            }
            if (solid >= 18) {
                Fx.effect(p, MobEffects.NAUSEA, 80, 0);
                Fx.effect(p, MobEffects.SLOWNESS, 60, 0);
                Payloads.fx(p, "anxiety", 40);
                Fx.bar(p, "§8Las paredes se cierran...");
                Fx.pose(p, POSE_COVER, 30);
            }
        }
    };

    public static final Condition BLEEDING = new Condition(
            "bleeding", "Sangrado",
            "Una herida abierta: pierdes vida poco a poco y dejas gotas de sangre. Regeneración o 30 s lo paran.", 600) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (p.hasEffect(MobEffects.REGENERATION)) {
                ConditionStore.remove(p, this);
                return;
            }
            if (now % 4 == 0) {
                Fx.level(p).sendParticles(new DustParticleOptions(0xAA0000, 1.0f),
                        p.getX(), p.getY() + 0.8, p.getZ(), 2, 0.2, 0.3, 0.2, 0.0);
            }
            if (now % 40 == 0) {
                p.setHealth(Math.max(1.0f, p.getHealth() - 1.0f));
                Fx.sound(p, SoundEvents.PLAYER_HURT, 0.3f, 1.5f);
            }
        }
    };

    public static final Condition CAT_ALLERGY = new Condition(
            "cat_allergy", "Alergia a los gatos",
            "Cerca de gatos y ocelotes estornudas sin parar. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 10 != 0) return;
            if (Fx.due(p, id, now, 60, 160) && nearType(p, 4, EntityTypes.CAT, EntityTypes.OCELOT)) {
                Fx.jerk(p, Fx.rf(p, 8f), 22f);
                Fx.effect(p, MobEffects.NAUSEA, 60, 0);
                Fx.sound(p, SoundEvents.PANDA_SNEEZE, 1.0f, 0.72f);
                Fx.bar(p, "§e¡ACHÚS! (gatos)");
                Payloads.fx(p, "sneeze", 24);
                Fx.spray(p, ParticleTypes.SNEEZE, 10, 0.25, 0.5);
            }
        }
    };

    public static final Condition EPILEPSY = new Condition(
            "epilepsy", "Epilepsia",
            "De vez en cuando (o tras un golpe o un rayo) sufres una convulsión: la cámara se sacude con luces fuertes. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            Integer left = SEIZURE.get(p.getUUID());
            if (left != null) {
                if (left % 3 == 0) Fx.jerk(p, Fx.rf(p, 25f), Fx.rf(p, 15f));
                if (left % 6 == 0) {
                    Fx.level(p).sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.3, 0.5, 0.3, 0.1);
                }
                if (left <= 1) {
                    SEIZURE.remove(p.getUUID());
                    Fx.msg(p, "§7Recuperas el control poco a poco.");
                } else {
                    SEIZURE.put(p.getUUID(), left - 1);
                }
                return;
            }
            if (Fx.due(p, id, now, 6000, 12000)) startSeizure(p);
        }

        @Override
        public void onLost(ServerPlayer p) {
            SEIZURE.remove(p.getUUID());
        }
    };

    private static final Map<UUID, Integer> SEIZURE = new HashMap<>();

    public static void startSeizure(ServerPlayer p) {
        if (SEIZURE.containsKey(p.getUUID())) return;
        SEIZURE.put(p.getUUID(), 70);
        Fx.effectHidden(p, MobEffects.SLOWNESS, 80, 6);
        Fx.effectHidden(p, MobEffects.WEAKNESS, 80, 2);
        Payloads.fx(p, "seizure", 70);
        Fx.pose(p, Condition.POSE_SHIVER, 70);
        Fx.pose(p, Condition.POSE_TWITCH, 70);
        Fx.sound(p, SoundEvents.PLAYER_HURT, 0.8f, 0.6f);
        Fx.msg(p, "§c¡Convulsión!");
    }

    public static boolean nearType(ServerPlayer p, double radius, EntityType<?>... types) {
        AABB box = p.getBoundingBox().inflate(radius);
        return !Fx.level(p).getEntitiesOfClass(LivingEntity.class, box, e -> {
            for (EntityType<?> t : types) if (e.getType() == t) return true;
            return false;
        }).isEmpty();
    }

    /** Particulas alrededor de los enfermos, visibles para todos. */
    public static void auras(MinecraftServer server, long now) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            var level = Fx.level(p);
            for (Condition c : ConditionStore.list(p)) {
                switch (c.id) {
                    case "zombie_infection" -> level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                            p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.6, 0.4, 0.0);
                    case "pox" -> level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.4, 0.6, 0.4, 0.0);
                    case "concussion" -> {
                        for (int k = 0; k < 3; k++) {
                            double a = now * 0.3 + k * 2.09;
                            level.sendParticles(ParticleTypes.CRIT, p.getX() + Math.cos(a) * 0.4, p.getY() + 2.05,
                                    p.getZ() + Math.sin(a) * 0.4, 1, 0, 0, 0, 0.0);
                        }
                    }
                    case "migraine" -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            p.getX(), p.getY() + 2.1, p.getZ(), 2, 0.25, 0.1, 0.25, 0.0);
                    case "tinnitus" -> {
                        if (p.getRandom().nextFloat() < 0.2f) {
                            level.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.1, p.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
                        }
                    }
                    default -> { }
                }
            }
        }
    }

    // ------------------------------------------------ VIRUS, DISCAPACIDADES Y COMIDA CRUDA

    public static final Condition COLD = new Condition(
            "cold", "Resfriado",
            "Estornudos y tos suave. Muy contagioso por cercanía y estornudo. Dura 5 minutos.", 6000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (Fx.due(p, id + ":s", now, 400, 900)) {
                Fx.jerk(p, Fx.rf(p, 8f), 18f);
                Fx.sound(p, SoundEvents.PANDA_SNEEZE, 1.0f, 0.72f);
                Fx.spray(p, ParticleTypes.SNEEZE, 8, 0.25, 0.5);
                Fx.bar(p, "§e¡Achís!");
                Payloads.fx(p, "sneeze", 20);
                spreadFront(p, this, 0.45f);
            }
            if (Fx.due(p, id + ":c", now, 500, 1100)) cough(p, this, 0.3f);
            spreadNear(p, this, 2.0f, 0.02f, now);
        }
    };

    public static final Condition GASTRO = new Condition(
            "gastroenteritis", "Gastroenteritis",
            "Vómitos fuertes (¡y contagiosos si vomitas a alguien!). Dura 4 minutos.", 4800) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.WEAKNESS, 60, 0);
            if (Fx.due(p, id, now, 300, 700)) {
                Fx.startVomit(p, 28);
                p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 3));
                Fx.effect(p, MobEffects.NAUSEA, 100, 0);
                Fx.bar(p, "§2*blergh*");
                spreadFront(p, this, 0.7f);
            }
        }
    };

    public static final Condition MEASLES = new Condition(
            "measles", "Sarampión",
            "Fiebre, erupciones y tos. Se contagia muchísimo por cercanía. Dura 7 minutos.", 9000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            spreadNear(p, this, 3.0f, 0.06f, now);
            if (Fx.due(p, id + ":c", now, 500, 1100)) cough(p, this, 0.3f);
            if (Fx.due(p, id + ":f", now, 1200, 2400)) {
                Fx.effect(p, MobEffects.WEAKNESS, 200, 0);
                Fx.bar(p, "§cTienes fiebre y la piel te arde...");
                Payloads.fx(p, "sick", 50);
            }
        }
    };

    public static final Condition PINKEYE = new Condition(
            "pinkeye", "Conjuntivitis",
            "Ojos rojos y pegajosos: a veces no ves nada. Se pega por contacto cercano. Dura 5 minutos.", 6000) {
        @Override
        public void tick(ServerPlayer p, long now) {
            spreadNear(p, this, 1.5f, 0.05f, now);
            if (Fx.due(p, id, now, 400, 900)) {
                Fx.effect(p, MobEffects.BLINDNESS, 30, 0);
                Payloads.fx(p, "insomnia", 60);
                Fx.bar(p, "§dSe te pegan los ojos...");
            }
        }
    };

    public static final Condition PLAGUE = new Condition(
            "plague", "Peste negra",
            "Fiebre, bubones y tos con sangre. Cada fase es peor, te quita vida y se contagia fuerte. Hasta que mueras.", -1) {
        private int stage(ServerPlayer p) {
            int age = ConditionStore.age(p, this);
            if (age < 3600) return 1;
            if (age < 9000) return 2;
            return 3;
        }

        @Override
        public boolean clearedOnDeath() {
            return true;
        }

        @Override
        public String label(ServerPlayer p) {
            return "Fase " + stage(p);
        }

        @Override
        public int poseFlags(ServerPlayer p) {
            int st = stage(p);
            if (st >= 3) return POSE_HUNCH | POSE_LIMP;
            return st >= 2 ? POSE_HUNCH : 0;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            int st = stage(p);
            announceStage(p, id, st, new String[] {
                "", "§8Te salen bubones negros y duelen. §7(fase 2)",
                "§4Toses sangre y te quedas sin fuerzas. §7(fase 3)"});
            spreadNear(p, this, 2.5f, 0.05f, now);
            if (st == 1 && Fx.due(p, id + ":f", now, 600, 1200)) {
                Fx.effect(p, MobEffects.WEAKNESS, 200, 0);
                Fx.bar(p, "§cFiebre alta...");
                Payloads.fx(p, "sick", 50);
            }
            if (st >= 2) {
                if (now % 40 == 0) Fx.effectHidden(p, MobEffects.SLOWNESS, 60, st - 2);
                if (now % (st == 3 ? 300 : 600) == 0) {
                    p.setHealth(Math.max(1.0f, p.getHealth() - 1.0f));
                    Fx.sound(p, SoundEvents.PLAYER_HURT, 0.4f, 0.7f);
                }
            }
            if (st >= 3 && Fx.due(p, id + ":c", now, 300, 700)) {
                cough(p, this, 0.4f);
                Fx.level(p).sendParticles(new DustParticleOptions(0x880000, 1.0f),
                        p.getX(), p.getEyeY() - 0.1, p.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
            }
        }
    };

    public static final Condition SALMONELLA = new Condition(
            "salmonella", "Salmonela",
            "Por carne cruda mal cocinada: calambres, fiebre y vómitos. Dura 4 minutos.", 4800) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 40 == 0) {
                Fx.effectHidden(p, MobEffects.WEAKNESS, 60, 0);
                Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
            }
            if (Fx.due(p, id, now, 400, 800)) {
                Fx.startVomit(p, 28);
                p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - 2));
                Fx.bar(p, "§2Calambres horribles...");
            }
            if (Fx.due(p, id + ":f", now, 1200, 2400)) {
                Fx.bar(p, "§cTienes fiebre...");
                Payloads.fx(p, "sick", 50);
            }
        }
    };

    public static final Condition PARASITES = new Condition(
            "parasites", "Parásitos",
            "Una tenia te roba la comida: pasas hambre aunque comas. Empeora con el tiempo. Hasta que mueras.", -1) {
        private int stage(ServerPlayer p) {
            return ConditionStore.age(p, this) < 12000 ? 1 : 2;
        }

        @Override
        public boolean clearedOnDeath() {
            return true;
        }

        @Override
        public String label(ServerPlayer p) {
            return "Fase " + stage(p);
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            int st = stage(p);
            announceStage(p, id, st, new String[] {"", "§2Los parásitos se multiplican. §7(fase 2)"});
            if (now % 600 == 0) {
                p.getFoodData().setFoodLevel(Math.max(0, p.getFoodData().getFoodLevel() - st));
            }
            if (st >= 2 && Fx.due(p, id + ":h", now, 600, 1200)) Fx.effect(p, MobEffects.HUNGER, 200, 0);
            if (Fx.due(p, id + ":g", now, 800, 1600)) {
                Fx.sound(p, SoundEvents.PLAYER_BURP, 0.7f, 0.5f);
                Fx.bar(p, "§2*gruñe tu estómago*");
            }
            if (Fx.due(p, id + ":v", now, 4000, 8000)) Fx.startVomit(p, 28);
        }
    };

    private static final String[] WHISPERS = {
        "Te están mirando.", "No confíes en nadie.", "Detrás de ti.", "Ya vienen.", "Lo saben todo.",
        "No estás solo.", "Escucha.", "Hay alguien en la pared."
    };
    private static final String[] FAKE_DISEASES = {
        "Voces", "Ojos en la pared", "Mal de ojo", "Eco mental", "Sombra persistente"
    };

    public static final Condition SCHIZOPHRENIA = new Condition(
            "schizophrenia", "Esquizofrenia",
            "Oyes pasos y susurros que no existen, ves sombras en el rabillo del ojo y te salen enfermedades falsas. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            List<Fake> list = FAKES.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
            boolean changed = false;
            if (now % 20 == 0) {
                Iterator<Fake> it = list.iterator();
                while (it.hasNext()) {
                    if (it.next().expires() <= now) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (Fx.due(p, id + ":f", now, 1500, 3000)) {
                list.add(new Fake(FAKE_DISEASES[p.getRandom().nextInt(FAKE_DISEASES.length)], now + 3600));
                changed = true;
            }
            if (changed) Payloads.sync(p);
            if (Fx.due(p, id, now, 300, 900)) hallucinate(p);
        }
    };

    private static void hallucinate(ServerPlayer p) {
        switch (p.getRandom().nextInt(5)) {
            case 0 -> {
                Fx.sound(p, SoundEvents.ZOMBIE_AMBIENT, 1.0f, 0.8f);
                Fx.bar(p, "§8...pasos detrás de ti");
            }
            case 1 -> {
                Fx.sound(p, SoundEvents.CREEPER_PRIMED, 1.0f, 1.0f);
                Fx.jerk(p, Fx.rf(p, 20f), 0f);
            }
            case 2 -> {
                Fx.msg(p, "§8§o" + WHISPERS[p.getRandom().nextInt(WHISPERS.length)]);
                Fx.sound(p, SoundEvents.ENDERMAN_STARE, 0.5f, 0.6f);
            }
            case 3 -> Payloads.fx(p, "shadow", 50);
            default -> {
                Payloads.fx(p, "shadow", 50);
                Fx.sound(p, SoundEvents.SKELETON_AMBIENT, 0.8f, 0.7f);
            }
        }
    }

    public static final Condition KLEPTOMANIA = new Condition(
            "kleptomania", "Cleptomanía",
            "No puedes soltar lo que has cogido (si lo tiras, vuelve a tu inventario) y los objetos cercanos te atraen. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 5 != 0) return;
            AABB box = p.getBoundingBox().inflate(5);
            List<ItemEntity> items = Fx.level(p).getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive());
            if (items.isEmpty()) return;
            for (ItemEntity e : items) {
                Vec3 d = p.position().add(0, 0.5, 0).subtract(e.position());
                if (d.length() > 0.5) e.setDeltaMovement(d.normalize().scale(0.3));
            }
            if (Fx.due(p, id, now, 1500, 3000)) Fx.bar(p, "§8No puedo dejar de coger cosas...");
        }
    };

    public static final Condition LAME_ARM = new Condition(
            "lame_arm", "Brazo inutilizado",
            "No puedes usar la mano izquierda (el objeto de la segunda mano vuelve a tu inventario) y el brazo cuelga. Para siempre.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_LAMEARM;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 10 == 0) {
                ItemStack off = p.getOffhandItem();
                if (!off.isEmpty()) {
                    ItemStack dropped = off.copy();
                    p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    p.drop(dropped, true, net.minecraft.util.Prediction.SERVER_ONLY);
                    Fx.bar(p, "§8El brazo no responde: se te cae el objeto.");
                }
            }
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.MINING_FATIGUE, 50, 0);
        }
    };

    public static final Condition BAD_LEG = new Condition(
            "bad_leg", "Pierna lesionada",
            "Cojeas siempre, vas más lento y no puedes correr. Para siempre.", -1) {
        @Override
        public int poseFlags(ServerPlayer p) {
            return POSE_LIMP;
        }

        @Override
        public void tick(ServerPlayer p, long now) {
            if (p.isSprinting()) p.setSprinting(false);
            if (now % 40 == 0) Fx.effectHidden(p, MobEffects.SLOWNESS, 60, 0);
        }
    };

    public static final Condition NIGHT_BLINDNESS = new Condition(
            "night_blindness", "Ceguera nocturna",
            "De noche, al aire libre, casi no ves nada. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            if (now % 20 != 0) return;
            Level level = p.level();
            if (!level.isBrightOutside() && level.canSeeSky(p.blockPosition())) Payloads.fx(p, "night", 40);
        }
    };

    public static final Condition MUTISM = new Condition(
            "mutism", "Mudez",
            "No puedes hablar: tus mensajes del chat no salen. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            // el bloqueo del chat esta en Triggers
        }
    };

    public static final Condition STUTTER = new Condition(
            "stutter", "Tartamudez",
            "Tus mensajes del chat salen tartamudeados (h-hola...). Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            // el tartamudeo del chat esta en Triggers
        }
    };

    private static final Map<UUID, Integer> SPIN = new HashMap<>();

    public static final Condition VERTIGO = new Condition(
            "vertigo", "Vértigo",
            "De pronto todo gira: la cámara da vueltas unos segundos. Para siempre.", -1) {
        @Override
        public void tick(ServerPlayer p, long now) {
            Integer left = SPIN.get(p.getUUID());
            if (left != null) {
                Fx.jerk(p, 7f, 0f);
                if (left <= 1) SPIN.remove(p.getUUID());
                else SPIN.put(p.getUUID(), left - 1);
                return;
            }
            if (Fx.due(p, id, now, 4000, 9000)) {
                SPIN.put(p.getUUID(), 50);
                Fx.effect(p, MobEffects.NAUSEA, 160, 0);
                Payloads.fx(p, "vertigo", 60);
                Fx.bar(p, "§8Todo da vueltas...");
            }
        }

        @Override
        public void onLost(ServerPlayer p) {
            SPIN.remove(p.getUUID());
        }
    };

    // ------------------------------------------------------------------ REGISTRO

    public static final List<Condition> ALL = List.of(
            ZOMBIE_INFECTION, TETANUS, FLU, HICCUPS, FOOD_POISONING,
            POLLEN_ALLERGY, ACROPHOBIA,
            OCD, ADHD, DEMENTIA, NARCOLEPSY, TOURETTE, ASTHMA, BRITTLE_BONES,
            ARTHROSIS, BLINDNESS, HYPOCHONDRIA,
            FROSTBITE, HEATSTROKE, RABIES, POX, CONCUSSION, LACTOSE, MIGRAINE, TREMORS, TINNITUS,
            INSOMNIA, COLORBLIND, SPRAIN, MOTION_SICKNESS, ANEMIA, PANIC, ARACHNOPHOBIA, CLAUSTROPHOBIA,
            BLEEDING, CAT_ALLERGY, EPILEPSY,
            COLD, GASTRO, MEASLES, PINKEYE, PLAGUE, SALMONELLA, PARASITES, SCHIZOPHRENIA, KLEPTOMANIA,
            LAME_ARM, BAD_LEG, NIGHT_BLINDNESS, MUTISM, STUTTER, VERTIGO);

    /** Virus que tambien se pasan al golpear a otro jugador. */
    public record Spread(Condition virus, float hitChance) {}

    public static final List<Spread> SPREADS = List.of(
            new Spread(COLD, 0.10f), new Spread(GASTRO, 0.12f), new Spread(MEASLES, 0.30f),
            new Spread(PINKEYE, 0.20f), new Spread(PLAGUE, 0.35f), new Spread(POX, 0.10f));

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

    /** Pose del cuerpo de un jugador: combina la de todas sus condiciones. */
    public static int poseFlags(ServerPlayer p) {
        int flags = 0;
        for (Condition c : ConditionStore.list(p)) flags |= c.poseFlags(p);
        flags |= Fx.tempPose(p);
        return flags;
    }

    /** Enfermedades falsas que se le muestran a un hipocondriaco en el panel. */
    public static List<String> fakeNames(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        List<Fake> list = FAKES.get(p.getUUID());
        if (list != null) {
            for (Fake f : list) out.add(f.name());
        }
        return out;
    }

    /** Limpia el estado temporal de un jugador. */
    public static void resetState(UUID id) {
        OCD_TASKS.remove(id);
        SPRINT.remove(id);
        ASTHMA_ATTACK.remove(id);
        FAKES.remove(id);
        LAST_STAGE.keySet().removeIf(k -> k.startsWith(id.toString()));
        Fx.clear(id);
    }

    // ------------------------------------------------------------ AYUDAS DE ETAPAS

    private static void announceStage(ServerPlayer p, String id, int stage, String[] messages) {
        String key = p.getUUID() + id;
        Integer last = LAST_STAGE.get(key);
        if (last != null && last == stage) return;
        LAST_STAGE.put(key, stage);
        if (last != null && stage - 1 < messages.length) {
            Fx.msg(p, messages[stage - 1]);
            Fx.sound(p, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.7f, 0.6f);
            Payloads.fx(p, "stage", 50);
            Payloads.sync(p);
        }
    }

    private static final Map<UUID, Integer> FILTER_LEFT = new HashMap<>();
    private static final Map<UUID, Integer> MILK = new HashMap<>();

    public static boolean hasTag(ItemStack s, String key) {
        if (s == null || s.isEmpty()) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().contains(key);
    }

    /** Cuanto deja pasar el virus lo que llevas en la cabeza (1 = nada de proteccion). */
    public static float protection(ServerPlayer p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return 1.0f;
        if (hasTag(head, "pmask")) {
            return FILTER_LEFT.getOrDefault(p.getUUID(), 0) > 0 ? 0.12f : 0.45f;
        }
        return 0.8f;
    }

    /** Tirada de contagio: la probabilidad baja con la proteccion del que contagia y del que recibe. */
    public static boolean spreads(ServerPlayer from, ServerPlayer to, float chance) {
        if (!Settings.contagion()) return false;
        return from.getRandom().nextFloat() < chance * protection(from) * protection(to);
    }

    /** Cada segundo: gasta y cambia los filtros de las mascarillas puestas. */
    public static void tickMasks(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!hasTag(p.getItemBySlot(EquipmentSlot.HEAD), "pmask")) continue;
            int left = FILTER_LEFT.getOrDefault(p.getUUID(), 0);
            if (left > 0) {
                FILTER_LEFT.put(p.getUUID(), left - 1);
                if (left == 1) Fx.msg(p, "§7El filtro de la mascarilla se ha agotado.");
                else continue;
            }
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (hasTag(st, "pfilter")) {
                    st.shrink(1);
                    FILTER_LEFT.put(p.getUUID(), 600);
                    Fx.msg(p, "§aHas puesto un filtro nuevo en la mascarilla (10 min).");
                    Fx.sound(p, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.3f);
                    break;
                }
            }
        }
    }

    /** True si el jugador ha bebido leche desde la ultima vez que se comprobo. */
    public static boolean milkDrank(ServerPlayer p) {
        int n = p.getStats().getValue(Stats.ITEM_USED.get(Items.MILK_BUCKET));
        Integer last = MILK.put(p.getUUID(), n);
        return last != null && n > last;
    }

    /** Tos: particulas hacia delante; si das en la cara a alguien le pasas el virus (la mascarilla ayuda). */
    public static void cough(ServerPlayer p, Condition virus, float chance) {
        boolean covered = protection(p) < 0.5f;
        Fx.spray(p, ParticleTypes.CLOUD, covered ? 2 : 8, 0.15, 0.35);
        Fx.sound(p, SoundEvents.PLAYER_HURT, covered ? 0.45f : 0.8f, 0.68f);
        Fx.jerk(p, Fx.rf(p, 3f), 6f);
        Fx.bar(p, covered ? "§7*cof* (con mascarilla)" : "§e*COF COF*");
        Payloads.fx(p, "cough", 24);
        Fx.pose(p, Condition.POSE_COUGH, 24);
        spreadFront(p, virus, chance);
    }

    /** Contagio hacia delante: a quien tengas delante a menos de 3 bloques (tos, estornudo, vomito). */
    public static void spreadFront(ServerPlayer p, Condition virus, float chance) {
        net.minecraft.world.phys.Vec3 look = p.getLookAngle();
        for (ServerPlayer other : Fx.level(p).players()) {
            if (other == p || ConditionStore.has(other, virus)) continue;
            net.minecraft.world.phys.Vec3 d = other.getEyePosition().subtract(p.getEyePosition());
            double dist = d.length();
            if (dist > 3.0 || dist < 0.01) continue;
            if (d.normalize().dot(look) < 0.7) continue;
            if (spreads(p, other, chance)) {
                ConditionStore.give(other, virus);
                Fx.msg(p, "§7Le has pasado algo a " + other.getName().getString() + "...");
            }
        }
    }

    /** Contagio por cercania constante (se llama cada tick; actua una vez por segundo). */
    private static void spreadNear(ServerPlayer p, Condition virus, float radius, float chance, long now) {
        if (now % 20 != 0) return;
        for (ServerPlayer other : Fx.level(p).players()) {
            if (other == p || other.distanceTo(p) > radius || ConditionStore.has(other, virus)) continue;
            if (spreads(p, other, chance)) ConditionStore.give(other, virus);
        }
    }

    private static final Map<String, Integer> RAW_SEEN = new HashMap<>();

    /** Nombre de la carne cruda que el jugador ha comido desde la ultima comprobacion (o null). */
    public static String rawEaten(ServerPlayer p) {
        Item[] raws = {Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON, Items.ROTTEN_FLESH};
        String[] names = {"beef", "pork", "chicken", "mutton", "rabbit", "cod", "salmon", "rotten"};
        String found = null;
        for (int i = 0; i < raws.length; i++) {
            int n = p.getStats().getValue(Stats.ITEM_USED.get(raws[i]));
            Integer last = RAW_SEEN.put(p.getUUID() + names[i], n);
            if (last != null && n > last) found = names[i];
        }
        return found;
    }

    public static boolean keepsItems(ServerPlayer p) {
        return ConditionStore.has(p, KLEPTOMANIA);
    }

    /** Tartamudeo: repite la primera letra de algunas palabras. */
    public static String stutter(String text, ServerPlayer p) {
        StringBuilder sb = new StringBuilder();
        for (String w : text.split(" ")) {
            if (w.length() > 1 && p.getRandom().nextFloat() < 0.4f) {
                int rep = 1 + p.getRandom().nextInt(2);
                for (int r = 0; r < rep; r++) sb.append(w.charAt(0)).append('-');
            }
            sb.append(w).append(' ');
            if (p.getRandom().nextFloat() < 0.08f) sb.append("... ");
        }
        return sb.toString().trim();
    }

    /** Etapa 3 de la infeccion: el sol te quema como a un zombi (el casco te protege). */
    private static void sunBurn(ServerPlayer p) {
        Level level = p.level();
        BlockPos pos = p.blockPosition();
        if (!level.isBrightOutside() || !level.canSeeSky(pos) || p.isInWaterOrRain()) return;
        if (!p.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) return;
        p.igniteForSeconds(6.0f);
        Fx.bar(p, "§6¡El sol te quema!");
    }

    /** Etapa 4 de la infeccion: te giras hacia el jugador o aldeano mas cercano y le pegas. */
    private static void attackUrge(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(3.5);
        List<LivingEntity> targets = Fx.level(p).getEntitiesOfClass(LivingEntity.class, box,
                e -> e != p && e.isAlive() && (e instanceof ServerPlayer || e.getType() == EntityTypes.VILLAGER));
        if (targets.isEmpty()) return;
        LivingEntity nearest = targets.get(0);
        for (LivingEntity e : targets) {
            if (e.distanceTo(p) < nearest.distanceTo(p)) nearest = e;
        }
        Fx.lookAt(p, nearest);
        p.attack(nearest);
        Fx.msg(p, "§4¡No puedes controlarte!");
        Fx.sound(p, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 0.8f);
    }

    // ---------------------------------------------------------------------- TOC

    private static final class OcdTask {
        String kind;       // jumps, sneak, break, touch, lookdown, stare
        int target;
        int count;
        int startJumps;
        int lookTicks;
        int settle;
        boolean lastSneak;
        int category = -1;
        UUID entity;
        long deadline;
        String text;

        boolean counting() {
            return kind.equals("jumps") || kind.equals("sneak") || kind.equals("break") || kind.equals("touch");
        }
    }

    private record Cat(String name, java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> test) {}

    private static final Cat[] BREAK_CATS = {
        new Cat("hierba", s -> s.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS) || s.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)),
        new Cat("flores", s -> s.is(net.minecraft.tags.BlockTags.FLOWERS)),
        new Cat("hojas", s -> s.is(net.minecraft.tags.BlockTags.LEAVES)),
        new Cat("troncos", s -> s.is(net.minecraft.tags.BlockTags.LOGS)),
        new Cat("tierra", s -> s.is(net.minecraft.tags.BlockTags.DIRT)),
        new Cat("arena", s -> s.is(net.minecraft.tags.BlockTags.SAND)),
        new Cat("piedra", s -> s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD))
    };

    private static final Cat[] TOUCH_CATS = {
        new Cat("cofre", s -> s.is(net.minecraft.world.level.block.Blocks.CHEST) || s.is(net.minecraft.world.level.block.Blocks.BARREL)
                || s.is(net.minecraft.world.level.block.Blocks.TRAPPED_CHEST)),
        new Cat("puerta", s -> s.is(net.minecraft.tags.BlockTags.DOORS)),
        new Cat("mesa de crafteo", s -> s.is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)),
        new Cat("cama", s -> s.is(net.minecraft.tags.BlockTags.BEDS)),
        new Cat("palanca", s -> s.is(net.minecraft.world.level.block.Blocks.LEVER))
    };

    private static int jumps(ServerPlayer p) {
        return p.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
    }

    private static String mobName(Entity e) {
        var t = e.getType();
        if (e instanceof ServerPlayer sp) return "el jugador " + sp.getName().getString();
        if (t == EntityTypes.COW) return "la vaca";
        if (t == EntityTypes.PIG) return "el cerdo";
        if (t == EntityTypes.SHEEP) return "la oveja";
        if (t == EntityTypes.CHICKEN) return "la gallina";
        if (t == EntityTypes.ZOMBIE) return "el zombi";
        if (t == EntityTypes.SKELETON) return "el esqueleto";
        if (t == EntityTypes.CREEPER) return "el creeper";
        if (t == EntityTypes.VILLAGER) return "el aldeano";
        if (t == EntityTypes.WOLF) return "el lobo";
        if (t == EntityTypes.CAT) return "el gato";
        if (t == EntityTypes.HORSE) return "el caballo";
        if (t == EntityTypes.SPIDER) return "la araña";
        return "esa criatura";
    }

    private static void startOcd(ServerPlayer p, long now) {
        var rnd = p.getRandom();
        OcdTask t = new OcdTask();
        List<String> kinds = new ArrayList<>(List.of("jumps", "sneak", "lookdown"));

        // que hay cerca? (bloques en radio 5 para romper, en radio 8 para tocar)
        BlockPos c = p.blockPosition();
        int[] breakCount = new int[BREAK_CATS.length];
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-5, -3, -5), c.offset(5, 3, 5))) {
            var st = p.level().getBlockState(pos);
            if (st.isAir()) continue;
            for (int i = 0; i < BREAK_CATS.length; i++) {
                if (BREAK_CATS[i].test().test(st)) breakCount[i]++;
            }
        }
        List<Integer> breakOptions = new ArrayList<>();
        for (int i = 0; i < breakCount.length; i++) if (breakCount[i] >= 5) breakOptions.add(i);
        if (!breakOptions.isEmpty()) kinds.add("break");

        boolean[] touchHas = new boolean[TOUCH_CATS.length];
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -3, -8), c.offset(8, 3, 8))) {
            var st = p.level().getBlockState(pos);
            if (st.isAir()) continue;
            for (int i = 0; i < TOUCH_CATS.length; i++) {
                if (TOUCH_CATS[i].test().test(st)) touchHas[i] = true;
            }
        }
        List<Integer> touchOptions = new ArrayList<>();
        for (int i = 0; i < touchHas.length; i++) if (touchHas[i]) touchOptions.add(i);
        if (!touchOptions.isEmpty()) kinds.add("touch");

        List<LivingEntity> near = Fx.level(p).getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10),
                e -> e != p && e.isAlive() && !e.isInvisible());
        if (!near.isEmpty()) kinds.add("stare");

        t.kind = kinds.get(rnd.nextInt(kinds.size()));
        int seconds = 30;
        switch (t.kind) {
            case "jumps" -> {
                t.target = 3 + rnd.nextInt(6);
                t.startJumps = jumps(p);
                t.text = "TOC: salta exactamente " + t.target + " veces";
            }
            case "sneak" -> {
                t.target = 3 + rnd.nextInt(5);
                t.text = "TOC: agáchate exactamente " + t.target + " veces";
                seconds = 25;
            }
            case "lookdown" -> {
                t.target = 4;
                t.text = "TOC: mira al suelo 4 segundos sin parar";
            }
            case "break" -> {
                t.category = breakOptions.get(rnd.nextInt(breakOptions.size()));
                t.target = 3 + rnd.nextInt(2);
                t.text = "TOC: rompe exactamente " + t.target + " bloques de " + BREAK_CATS[t.category].name() + " cercanos";
                seconds = 45;
            }
            case "touch" -> {
                t.category = touchOptions.get(rnd.nextInt(touchOptions.size()));
                t.target = 3;
                t.text = "TOC: toca (clic derecho) exactamente 3 veces " + TOUCH_CATS[t.category].name();
                seconds = 40;
            }
            default -> {
                LivingEntity e = near.get(rnd.nextInt(near.size()));
                t.entity = e.getUUID();
                t.target = 5;
                t.text = "TOC: mira fijamente a " + mobName(e) + " 5 segundos";
            }
        }
        t.deadline = now + seconds * 20L;
        OCD_TASKS.put(p.getUUID(), t);
        Fx.msg(p, "§e" + t.text + " §7(" + seconds + " s; si te pasas o no llegas, te haces daño)");
        Fx.sound(p, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.6f);
        Payloads.task(p, t.text, seconds * 20);
    }

    private static void ocdProgress(ServerPlayer p, OcdTask t, int count, long now) {
        t.count = count;
        t.settle = 0;
        if (count > t.target) {
            ocdFail(p, "§c¡Te has pasado!", 3f);
            return;
        }
        Fx.sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f, 0.8f + 0.15f * count);
        Payloads.task(p, t.text + " (" + count + "/" + t.target + ")", (int) Math.max(1, t.deadline - now));
    }

    private static void ocdSuccess(ServerPlayer p) {
        OCD_TASKS.remove(p.getUUID());
        Payloads.task(p, "", 0);
        Fx.bar(p, "§aAhhh... qué alivio.");
        Fx.sound(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.5f);
        Fx.effect(p, MobEffects.SPEED, 200, 0);
    }

    private static void ocdFail(ServerPlayer p, String reason, float damage) {
        OCD_TASKS.remove(p.getUUID());
        Payloads.task(p, "", 0);
        Fx.bar(p, reason + " §7¡Ansiedad!");
        p.setHealth(Math.max(1.0f, p.getHealth() - damage));
        Fx.sound(p, SoundEvents.PLAYER_HURT, 1.0f, 0.8f);
        Fx.jerk(p, Fx.rf(p, 10f), 8f);
        Payloads.fx(p, "anxiety", 160);
        Fx.effect(p, MobEffects.NAUSEA, 200, 0);
        Fx.effect(p, MobEffects.SLOWNESS, 200, 1);
    }

    /** Llamado al romper un bloque (Triggers). */
    public static void ocdBreak(ServerPlayer p, net.minecraft.world.level.block.state.BlockState state) {
        OcdTask t = OCD_TASKS.get(p.getUUID());
        if (t == null || !t.kind.equals("break") || !BREAK_CATS[t.category].test().test(state)) return;
        ocdProgress(p, t, t.count + 1, Fx.server(p).getTickCount());
    }

    /** Llamado al hacer clic derecho en un bloque (Triggers). */
    public static void ocdUse(ServerPlayer p, net.minecraft.world.level.block.state.BlockState state) {
        OcdTask t = OCD_TASKS.get(p.getUUID());
        if (t == null || !t.kind.equals("touch") || !TOUCH_CATS[t.category].test().test(state)) return;
        ocdProgress(p, t, t.count + 1, Fx.server(p).getTickCount());
    }
}
