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

    public static void extract(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Efectos de pantalla
        vignette(g, w, h, 0x226611, ClientState.fxLeft("sick") * 0.8f);
        vignette(g, w, h, 0x771111, ClientState.fxLeft("spasm"));
        vignette(g, w, h, 0x112200, ClientState.fxLeft("stage") * 1.2f);
        vignette(g, w, h, 0x000000, ClientState.fxLeft("vertigo"));
        vignette(g, w, h, 0x220033, ClientState.fxLeft("anxiety") * 0.9f);
        vignette(g, w, h, 0x333344, ClientState.fxLeft("gasp") * 0.7f);
        float sneeze = ClientState.fxLeft("sneeze");
        if (sneeze > 0) g.fill(0, 0, w, h, ((int) (sneeze * 120) << 24) | 0xFFFFFF);
        float cgh = ClientState.fxLeft("cough");
        if (cgh > 0) vignette(g, w, h, 0xCCCCBB, cgh * 0.5f);
        float hic = ClientState.fxLeft("hiccup");
        if (hic > 0) g.fill(0, 0, w, h, ((int) (hic * 60) << 24) | 0xFFFFAA);
        float tic = ClientState.fxLeft("tic");
        if (tic > 0) g.fill(0, (int) (h * 0.4f), w, (int) (h * 0.4f) + 6, ((int) (tic * 160) << 24) | 0x00FFFF);
        float sleep = ClientState.fxLeft("sleep");
        if (sleep > 0) {
            float lid = Math.min(1f, sleep * 2f);
            int half = (int) (h * 0.5f * lid);
            g.fill(0, 0, w, half, 0xFF000000);
            g.fill(0, h - half, w, h, 0xFF000000);
        }

        long now = System.currentTimeMillis();
        if (ClientState.has("frostbite")) {
            vignette(g, w, h, 0xAADDFF, 0.55f);
            for (int i = 0; i < 10; i++) {
                int cx = (i % 2 == 0) ? 2 + i * 3 : w - 8 - i * 3;
                int cy = (i < 5) ? 2 + i * 6 : h - 8 - (i - 5) * 6;
                g.fill(cx, cy, cx + 5, cy + 5, 0xAAEEFFFF);
            }
        }
        if (ClientState.has("heatstroke")) vignette(g, w, h, 0xFF6600, 0.4f + 0.25f * (float) Math.sin(now / 400.0));
        if (ClientState.has("rabies")) vignette(g, w, h, 0x660000, 0.25f + 0.15f * (float) Math.sin(now / 300.0));
        if (ClientState.has("migraine")) {
            float s = 0.5f + 0.5f * (float) Math.sin(now / 180.0);
            vignette(g, w, h, 0x000000, 0.6f);
            g.fill(0, 0, w, h, ((int) (s * 45) << 24) | 0xFFFFFF);
        }
        if (ClientState.has("concussion")) vignette(g, w, h, 0xFFFFFF, 0.12f + 0.08f * (float) Math.sin(now / 350.0));
        float ring = ClientState.fxLeft("ringing");
        if (ring > 0) g.fill(0, 0, w, h, ((int) (ring * 90) << 24) | 0xFFFFFF);
        vignette(g, w, h, 0xFFFFFF, ClientState.fxLeft("tinnitus") * 0.18f);
        float frost = ClientState.fxLeft("frost");
        if (frost > 0) vignette(g, w, h, 0xFFFFFF, frost * 0.6f);
        if (ClientState.has("pox")) {
            long seed = 12345;
            for (int i = 0; i < 32; i++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int rx = (int) ((seed >>> 33) % 1000);
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int ry = (int) ((seed >>> 33) % 1000);
                int x = (i % 2 == 0) ? rx * w / 1000 / 6 : w - rx * w / 1000 / 6 - 4;
                int y = ry * h / 1000;
                g.fill(x, y, x + 4, y + 4, 0xCCCC3344);
                g.fill(x + 1, y + 1, x + 3, y + 3, 0xFFFF8899);
            }
        }

        if (ClientState.has("colorblind")) g.fill(0, 0, w, h, 0x70808080);
        if (ClientState.has("bleeding")) {
            vignette(g, w, h, 0x880000, 0.3f + 0.1f * (float) Math.sin(now / 200.0));
            for (int i = 0; i < 5; i++) {
                int x = w * (i + 1) / 6;
                int len = (int) ((now / 12 + i * 37) % 120);
                g.fill(x, 0, x + 3, len, 0xAAAA0000);
                g.fill(x - 1, len, x + 4, len + 4, 0xCCAA0000);
            }
        }
        float seizure = ClientState.fxLeft("seizure");
        if (seizure > 0) g.fill(0, 0, w, h, ((now / 60) % 2 == 0) ? 0xB0FFFFFF : 0xB0000000);
        float faint = ClientState.fxLeft("faint");
        if (faint > 0) vignette(g, w, h, 0x000000, Math.min(2.2f, faint * 3f));
        float ins = ClientState.fxLeft("insomnia");
        if (ins > 0) {
            int hb = (int) (h * 0.08f * (0.5f + 0.5f * Math.sin(now / 90.0)) * ins);
            g.fill(0, 0, w, hb, 0xFF000000);
            g.fill(0, h - hb, w, h, 0xFF000000);
        }

        if (ClientState.has("blindness")) sonar(g, mc, w, h);

        EffectsRenderer.draw(g, mc, w, h, now);

        // Temporizador global (modo programado) y aviso de dosis
        float dose = ClientState.fxLeft("dose");
        if (dose > 0) {
            vignette(g, w, h, 0xCC0000, dose * 1.6f);
            g.fill(0, 0, w, h, ((int) (dose * dose * 110) << 24) | 0xFFFFFF);
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

        // Tarea del TOC
        if (!ClientState.taskText.isEmpty()) {
            long left = ClientState.taskEnd - System.currentTimeMillis();
            if (left > 0) {
                String text = ClientState.taskText + " (" + (left / 1000) + " s)";
                int tw = mc.font.width(text);
                g.fill((w - tw) / 2 - 6, 30, (w + tw) / 2 + 6, 46, 0xAA220033);
                g.text(mc.font, text, (w - tw) / 2, 34, 0xFFE8D0FF, true);
            }
        }

        // Moodles (con tooltip al pasar el raton con el chat o el inventario abiertos)
        boolean interactiveScreen = MoodleRenderer.isInteractiveScreen(mc);
        boolean mixinHandles = interactiveScreen && System.currentTimeMillis() - ClientState.screenDrawMs < 300;
        if (!mixinHandles) {
            int mx = -1, my = -1;
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
