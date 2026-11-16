package dev.pandemonium.client;

import dev.pandemonium.Conditions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;

/** Panel de condiciones y efectos visuales (todo con rectangulos, sin shaders). */
public final class HudRenderer {
    private HudRenderer() {}

    static int colorOf(String id) {
        return switch (id) {
            case "zombie_infection" -> 0x55AA33;
            case "tetanus" -> 0xCC5533;
            case "arthrosis" -> 0xB0A090;
            case "blindness" -> 0x888888;
            case "hypochondria" -> 0xDD88AA;
            case "asthma" -> 0x66BBEE;
            case "flu" -> 0x99DD99;
            case "frostbite" -> 0xAADDFF;
            case "heatstroke" -> 0xFF8833;
            case "rabies" -> 0xAA3333;
            case "pox" -> 0xEE6677;
            case "concussion" -> 0xCCCCEE;
            case "lactose_intolerance" -> 0xEEEECC;
            case "migraine" -> 0xBBAAEE;
            case "tremors" -> 0xAAAAAA;
            case "tinnitus" -> 0xDDDDFF;
            case "insomnia" -> 0x7788AA;
            case "colorblind" -> 0xBBBBBB;
            case "sprain" -> 0xDDBB66;
            case "motion_sickness" -> 0x77BB88;
            case "anemia" -> 0xCC8888;
            case "panic" -> 0x9966CC;
            case "arachnophobia" -> 0x886644;
            case "claustrophobia" -> 0x778877;
            case "bleeding" -> 0xDD2222;
            case "cat_allergy" -> 0xFFCC88;
            case "epilepsy" -> 0xFFFF66;
            case "cold" -> 0xAADDEE;
            case "gastroenteritis" -> 0x99BB44;
            case "measles" -> 0xFF8899;
            case "pinkeye" -> 0xFF77AA;
            case "plague" -> 0x665544;
            case "salmonella" -> 0xBBCC55;
            case "parasites" -> 0xCC9966;
            case "schizophrenia" -> 0xAA88DD;
            case "kleptomania" -> 0xDDAA33;
            case "lame_arm" -> 0xBBAA99;
            case "bad_leg" -> 0xBBAA99;
            case "night_blindness" -> 0x556699;
            case "mutism" -> 0x999999;
            case "stutter" -> 0xCCCCAA;
            case "vertigo" -> 0x88AACC;
            case "food_poisoning" -> 0x88AA22;
            default -> 0xE0C050;
        };
    }

    static void vignette(GuiGraphicsExtractor g, int w, int h, int rgb, float strength) {
        if (strength <= 0f) return;
        int rings = 14;
        for (int i = 0; i < rings; i++) {
            float k = 1f - i / (float) rings;
            int a = (int) (strength * 150 * k * k);
            if (a <= 0) continue;
            int col = (a << 24) | rgb;
            int t = i * Math.max(2, h / 40);
            g.fill(0, t, w, t + Math.max(2, h / 40), col);
            g.fill(0, h - t - Math.max(2, h / 40), w, h - t, col);
            int s = i * Math.max(2, w / 60);
            g.fill(s, 0, s + Math.max(2, w / 60), h, col);
            g.fill(w - s - Math.max(2, w / 60), 0, w - s, h, col);
        }
    }

    /** Doble latido 0..1 (ms dentro de un ciclo de 900). */
    private static float heartbeat(long now) {
        long ph = now % 900;
        return Math.max(bump(ph, 0, 140), 0.7f * bump(ph, 220, 140));
    }

    private static float bump(long t, int start, int len) {
        float x = (t - start) / (float) len;
        if (x < 0f || x > 1f) return 0f;
        return (float) Math.sin(x * Math.PI);
    }

    private static void spots(GuiGraphicsExtractor g, int w, int h, int seedBase, int count, int outer, int inner) {
        long seed = 12345 + seedBase;
        for (int i = 0; i < count; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int rx = (int) ((seed >>> 33) % 1000);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int ry = (int) ((seed >>> 33) % 1000);
            int x = (i % 2 == 0) ? rx * w / 1000 / 6 : w - rx * w / 1000 / 6 - 4;
            int y = ry * h / 1000;
            g.fill(x, y, x + 4, y + 4, 0xCC000000 | outer);
            g.fill(x + 1, y + 1, x + 3, y + 3, 0xFF000000 | inner);
        }
    }

    public static void extract(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        // ---- Efectos puntuales: todos en el borde de la pantalla (vineta)
        vignette(g, w, h, 0x226611, ClientState.fxLeft("sick") * 0.8f);
        vignette(g, w, h, 0x771111, ClientState.fxLeft("spasm"));
        vignette(g, w, h, 0x112200, ClientState.fxLeft("stage") * 1.2f);
        vignette(g, w, h, 0x000000, ClientState.fxLeft("vertigo"));
        vignette(g, w, h, 0xCCCCBB, ClientState.fxLeft("cough") * 0.5f);
        vignette(g, w, h, 0xFFFFFF, ClientState.fxLeft("sneeze") * 0.7f);
        vignette(g, w, h, 0xFFFFAA, ClientState.fxLeft("hiccup") * 0.5f);
        vignette(g, w, h, 0x00CCCC, ClientState.fxLeft("tic") * 0.6f);
        vignette(g, w, h, 0xFFFFFF, ClientState.fxLeft("ringing") * 0.6f);
        vignette(g, w, h, 0xFFFFFF, ClientState.fxLeft("tinnitus") * 0.18f);
        vignette(g, w, h, 0xCCEEFF, ClientState.fxLeft("frost") * 0.6f);
        vignette(g, w, h, 0x000000, Math.min(2.2f, ClientState.fxLeft("faint") * 3f));
        vignette(g, w, h, 0x000011, ClientState.fxLeft("night") * 2.0f);

        float anx = ClientState.fxLeft("anxiety");
        if (anx > 0) vignette(g, w, h, 0x330044, anx * (0.3f + heartbeat(now)));
        float gasp = ClientState.fxLeft("gasp");
        if (gasp > 0) vignette(g, w, h, 0x000000, gasp * (0.4f + 0.5f * (float) Math.sin(now / 450.0)) * 1.2f);
        if (ClientState.fxLeft("seizure") > 0) {
            boolean on = (now / 70) % 2 == 0;
            vignette(g, w, h, on ? 0xFFFFFF : 0x000000, 1.4f);
        }

        // Parpados: narcolepsia (cierra del todo) e insomnio / conjuntivitis (parpadeos)
        float sleep = ClientState.fxLeft("sleep");
        if (sleep > 0) {
            int half = (int) (h * 0.5f * Math.min(1f, sleep * 2f));
            g.fill(0, 0, w, half, 0xFF000000);
            g.fill(0, h - half, w, h, 0xFF000000);
        }
        float ins = ClientState.fxLeft("insomnia");
        if (ins > 0) {
            int hb = (int) (h * 0.08f * (0.5f + 0.5f * Math.sin(now / 90.0)) * ins);
            g.fill(0, 0, w, hb, 0xFF000000);
            g.fill(0, h - hb, w, h, 0xFF000000);
        }

        // ---- Efectos mientras tienes la condicion (bordes, con entrada suave)
        if (ClientState.has("frostbite")) vignette(g, w, h, 0xAADDFF, 0.55f * ClientState.fade("frostbite"));
        if (ClientState.has("heatstroke")) {
            vignette(g, w, h, 0xFF6600, (0.4f + 0.25f * (float) Math.sin(now / 400.0)) * ClientState.fade("heatstroke"));
        }
        if (ClientState.has("rabies")) vignette(g, w, h, 0x660000, 0.25f + 0.15f * (float) Math.sin(now / 300.0));
        if (ClientState.has("bleeding")) vignette(g, w, h, 0x880000, 0.3f + 0.1f * (float) Math.sin(now / 200.0));
        if (ClientState.has("concussion")) vignette(g, w, h, 0xFFFFFF, 0.12f + 0.08f * (float) Math.sin(now / 350.0));
        if (ClientState.has("migraine")) {
            vignette(g, w, h, 0x000000, 0.6f);
            vignette(g, w, h, 0xFFFFFF, 0.5f * (0.5f + 0.5f * (float) Math.sin(now / 180.0)));
        }
        if (ClientState.has("pinkeye")) vignette(g, w, h, 0xFF3355, 0.4f * ClientState.fade("pinkeye"));
        if (ClientState.has("plague")) vignette(g, w, h, 0x221100, 0.5f);

        // Granitos en los bordes (varicela, sarampion, peste)
        if (ClientState.has("pox")) spots(g, w, h, 0, 32, 0xCC3344, 0xFF8899);
        if (ClientState.has("measles")) spots(g, w, h, 777, 40, 0xCC3355, 0xFF99AA);
        if (ClientState.has("plague") && ClientState.severity("plague") >= 2) {
            spots(g, w, h, 999, 14 + ClientState.severity("plague") * 8, 0x221122, 0x553344);
        }

        if (ClientState.has("blindness")) sonar(g, mc, w, h);

        // ---- Aviso de dosis y temporizador global (modo programado)
        float dose = ClientState.fxLeft("dose");
        if (dose > 0) {
            vignette(g, w, h, 0xCC0000, dose * 1.6f);
            String title = "¡NUEVA CONDICIÓN!";
            g.text(mc.font, title, (w - mc.font.width(title)) / 2, h / 3, 0xFFFF4444, true);
        }
        if (ClientState.timerOn) {
            long left = ClientState.timerRemainingMs();
            long secs = left / 1000;
            boolean urgent = secs < 10;
            String text = "Próxima condición  " + (secs / 60) + ":" + String.format("%02d", secs % 60);
            int tw = mc.font.width(text);
            int bx = (w - tw) / 2 - 8;
            int bw = tw + 16;
            float frac = 1f - Math.min(1f, left / (float) ClientState.timerIntervalMs);
            int col = urgent ? 0xFF4444 : (frac > 0.75f ? 0xFFAA33 : 0x66CC88);
            int a = urgent ? (int) (120 + 100 * (0.5 + 0.5 * Math.sin(now / 90.0))) : 0xAA;
            g.fill(bx, 4, bx + bw, 24, (a << 24) | 0x101018);
            g.fill(bx, 4, bx + bw, 5, 0xFF000000 | col);
            g.text(mc.font, text, (w - tw) / 2, 8, 0xFF000000 | col, true);
            g.fill(bx + 4, 19, bx + bw - 4, 21, 0xFF333344);
            g.fill(bx + 4, 19, bx + 4 + (int) ((bw - 8) * frac), 21, 0xFF000000 | col);
        }

        // ---- Tarea del TOC
        if (!ClientState.taskText.isEmpty()) {
            long left = ClientState.taskEnd - System.currentTimeMillis();
            if (left > 0) {
                String text = ClientState.taskText + " (" + (left / 1000) + " s)";
                int tw = mc.font.width(text);
                g.fill((w - tw) / 2 - 6, 30, (w + tw) / 2 + 6, 46, 0xAA220033);
                g.text(mc.font, text, (w - tw) / 2, 34, 0xFFE8D0FF, true);
            }
        }

        // ---- Moodles (con tooltip al pasar el raton con el chat o el inventario abiertos)
        boolean interactiveScreen = MoodleRenderer.isInteractiveScreen(mc);
        boolean mixinHandles = interactiveScreen && System.currentTimeMillis() - ClientState.screenDrawMs < 300;
        if (!mixinHandles) {
            int mx = -1;
            int my = -1;
            if (interactiveScreen) {
                mx = (int) (mc.mouseHandler.xpos() * w / mc.getWindow().getScreenWidth());
                my = (int) (mc.mouseHandler.ypos() * h / mc.getWindow().getScreenHeight());
            }
            MoodleRenderer.draw(g, mc, mx, my, interactiveScreen);
        }
    }

    /** Ceguera: oscuridad y destellos donde hay ruido o movimiento cerca. */
    private static void sonar(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
        vignette(g, w, h, 0x000000, 1.6f);
        long t = System.currentTimeMillis();
        float pulse = 0.5f + 0.5f * (float) Math.sin(t / 150.0);
        float yawRad = (float) Math.toRadians(mc.player.getYRot());
        float pitchRad = (float) Math.toRadians(mc.player.getXRot());
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le)) continue;
            double dist = e.distanceTo(mc.player);
            if (dist > 24) continue;
            boolean noisy = e.isSprinting() || le.hurtTime > 0 || e.getDeltaMovement().horizontalDistanceSqr() > 0.004;
            if (!noisy) continue;
            Vec3 d = e.getEyePosition().subtract(mc.player.getEyePosition());
            float yaw = (float) Math.atan2(-d.x, d.z);
            float pitch = (float) -Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
            float dyaw = wrap(yaw - yawRad);
            float dpitch = wrap(pitch - pitchRad);
            if (Math.abs(dyaw) > 0.9f || Math.abs(dpitch) > 0.6f) continue;
            int sx = (int) (w / 2f - dyaw / 0.9f * (w / 2f));
            int sy = (int) (h / 2f + dpitch / 0.6f * (h / 2f));
            int size = (int) (6 + 8 * pulse + (24 - dist) / 4);
            int rgb = le.getType().getCategory() == MobCategory.MONSTER ? 0xFF3333
                    : (e instanceof net.minecraft.world.entity.player.Player ? 0x55AAFF : 0xFFFFFF);
            int a = (int) (110 + 90 * pulse);
            g.fill(sx - size, sy - size, sx + size, sy + size, (a / 3 << 24) | rgb);
            g.fill(sx - size / 2, sy - size / 2, sx + size / 2, sy + size / 2, (a << 24) | rgb);
        }
    }

    private static float wrap(float a) {
        while (a > Math.PI) a -= (float) (2 * Math.PI);
        while (a < -Math.PI) a += (float) (2 * Math.PI);
        return a;
    }
}
