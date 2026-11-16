package dev.pandemonium.client;

import dev.pandemonium.Condition;
import dev.pandemonium.Conditions;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Dibuja el panel de condiciones y los efectos de pantalla. Todo con rectangulos y degradados, sin shaders. */
public final class HudRenderer {
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("zombie_infection", 0x4CAF50),
            Map.entry("tetanus", 0xC0392B),
            Map.entry("hiccups", 0xF1C40F),
            Map.entry("pollen_allergy", 0xE67E22),
            Map.entry("acrophobia", 0x95A5A6),
            Map.entry("ocd", 0x9B59B6),
            Map.entry("adhd", 0x3498DB),
            Map.entry("dementia", 0x7F8C8D),
            Map.entry("narcolepsy", 0x5D6D7E),
            Map.entry("tourette", 0xE91E63),
            Map.entry("asthma", 0x1ABC9C),
            Map.entry("brittle_bones", 0xECF0F1),
            Map.entry("hypochondria", 0xFF8A80),
            Map.entry("food_poisoning", 0x9ACD32));

    private HudRenderer() {}

    public static void extract(GuiGraphicsExtractor g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        long now = System.currentTimeMillis();

        screenEffects(g, w, h, now);
        if (!mc.options.hideGui) {
            conditionPanel(g, mc, now);
            ocdTask(g, mc, w, now);
        }
    }

    // ------------------------------------------------------------ EFECTOS DE PANTALLA

    private static void screenEffects(GuiGraphicsExtractor g, int w, int h, long now) {
        // Ambiente suave mientras la enfermedad esta activa
        if (ClientState.has("zombie_infection")) {
            vignette(g, w, h, 0x3FA03A, 0.10f + 0.07f * (float) Math.sin(now / 650.0), 0.30f);
        }
        if (ClientState.has("tetanus")) {
            vignette(g, w, h, 0xA02020, 0.06f, 0.22f);
        }
        if (ClientState.has("food_poisoning")) {
            vignette(g, w, h, 0x7FA830, 0.09f + 0.05f * (float) Math.sin(now / 500.0), 0.28f);
        }

        float p;
        if ((p = ClientState.progress("sneeze", now)) >= 0) {
            float k = (1f - p) * (1f - p);
            g.fill(0, 0, w, h, argb(0.55f * k, 0xFFFFFF));
            vignette(g, w, h, 0xBFE3FF, 0.5f * k, 0.35f);
        }
        if ((p = ClientState.progress("hiccup", now)) >= 0) {
            vignette(g, w, h, 0xFFF2A0, 0.35f * (1f - p), 0.25f);
        }
        if ((p = ClientState.progress("spasm", now)) >= 0) {
            g.fill(0, 0, w, h, argb(0.18f * (1f - p), 0xFF0000));
            vignette(g, w, h, 0xC00000, 0.65f * (1f - p), 0.4f);
        }
        if ((p = ClientState.progress("sick", now)) >= 0) {
            float k = (float) Math.sin(Math.PI * p);
            vignette(g, w, h, 0x4CAF50, 0.65f * k, 0.5f);
        }
        if ((p = ClientState.progress("vertigo", now)) >= 0) {
            vignette(g, w, h, 0x000000, 0.85f * (1f - p * 0.4f), 0.45f);
        }
        if ((p = ClientState.progress("gasp", now)) >= 0) {
            vignette(g, w, h, 0x9ADCFF, 0.55f * (1f - p), 0.5f);
        }
        if ((p = ClientState.progress("anxiety", now)) >= 0) {
            float pulse = 0.75f + 0.25f * (float) Math.sin(now / 90.0);
            vignette(g, w, h, 0x2A0A3A, 0.85f * (1f - p) * pulse, 0.6f);
        }
        if ((p = ClientState.progress("tic", now)) >= 0) {
            glitch(g, w, h, now, 1f - p);
        }
        if ((p = ClientState.progress("sleep", now)) >= 0) {
            eyelids(g, w, h, p);
        }
    }

    /** Marco oscuro/coloreado que se desvanece hacia el centro. size = fraccion de pantalla que ocupa. */
    private static void vignette(GuiGraphicsExtractor g, int w, int h, int rgb, float alpha, float size) {
        if (alpha <= 0.01f) return;
        int steps = 28;
        int band = Math.max(1, (int) (Math.min(w, h) * size / steps));
        for (int i = 0; i < steps; i++) {
            float t = 1f - i / (float) steps;
            float a = Math.min(1f, alpha) * t * t;
            if (a <= 0.004f) continue;
            int col = argb(a, rgb);
            int o = i * band;
            if (w - 2 * o <= 0 || h - 2 * (o + band) <= 0) break;
            g.fill(o, o, w - o, o + band, col);
            g.fill(o, h - o - band, w - o, h - o, col);
            g.fill(o, o + band, o + band, h - o - band, col);
            g.fill(w - o - band, o + band, w - o, h - o - band, col);
        }
    }

    /** Parpados que se cierran y se abren (narcolepsia). p va de 0 a 1. */
    private static void eyelids(GuiGraphicsExtractor g, int w, int h, float p) {
        float close;
        if (p < 0.25f) close = p / 0.25f;
        else if (p > 0.80f) close = (1f - p) / 0.20f;
        else close = 1f;
        close = Math.max(0f, Math.min(1f, close));
        int lid = (int) (close * (h / 2f));
        if (lid <= 0) return;
        int soft = Math.min(36, h / 10);
        g.fill(0, 0, w, lid, 0xFF000000);
        g.fill(0, h - lid, w, h, 0xFF000000);
        g.fillGradient(0, lid, w, lid + soft, 0xFF000000, 0x00000000);
        g.fillGradient(0, h - lid - soft, w, h - lid, 0x00000000, 0xFF000000);
    }

    /** Franjas de colores desplazadas, como una senal de video rota (tics). */
    private static void glitch(GuiGraphicsExtractor g, int w, int h, long now, float strength) {
        Random rnd = new Random(now / 45);
        int bars = 4 + rnd.nextInt(6);
        for (int i = 0; i < bars; i++) {
            int y = rnd.nextInt(Math.max(1, h));
            int bh = 2 + rnd.nextInt(10);
            int shift = rnd.nextInt(Math.max(1, w / 6));
            int rgb = switch (rnd.nextInt(3)) {
                case 0 -> 0x00FFFF;
                case 1 -> 0xFF00FF;
                default -> 0xFFFFFF;
            };
            g.fill(shift, y, w, y + bh, argb(0.35f * strength, rgb));
        }
    }

    // --------------------------------------------------------------------------- HUD

    private static void conditionPanel(GuiGraphicsExtractor g, Minecraft mc, long now) {
        if (ClientState.ACTIVE.isEmpty()) return;
        int x = 6;
        int y = 6;
        int rowH = 15;
        for (String id : ClientState.ACTIVE) {
            Condition c = Conditions.get(id);
            if (c == null) continue;
            int color = COLORS.getOrDefault(id, 0xFFFFFF);
            int textW = mc.font.width(c.name);
            float pulse = 0.65f + 0.35f * (float) Math.sin(now / 420.0 + id.hashCode());
            g.fill(x, y, x + textW + 14, y + rowH - 2, 0xB0101418);
            g.fill(x, y, x + 3, y + rowH - 2, argb(pulse, color));
            g.text(mc.font, c.name, x + 8, y + 3, 0xFF000000 | color, true);
            y += rowH;
        }
    }

    private static void ocdTask(GuiGraphicsExtractor g, Minecraft mc, int w, long now) {
        if (ClientState.taskText.isEmpty()) return;
        float left = 1f - (now - ClientState.taskStart) / (float) ClientState.taskDuration;
        if (left <= 0f) {
            ClientState.taskText = "";
            return;
        }
        String text = ClientState.taskText;
        int tw = mc.font.width(text);
        int boxW = Math.max(tw + 24, 170);
        int x = (w - boxW) / 2;
        int y = 22;
        g.fill(x, y, x + boxW, y + 26, 0xC0141018);
        g.fill(x, y, x + 3, y + 26, 0xFF9B59B6);
        g.text(mc.font, text, (w - tw) / 2, y + 5, 0xFFE8D0FF, true);
        int barW = boxW - 16;
        int color = left > 0.25f ? 0xFF9B59B6 : 0xFFE74C3C;
        g.fill(x + 8, y + 17, x + 8 + barW, y + 21, 0xFF2A2230);
        g.fill(x + 8, y + 17, x + 8 + (int) (barW * left), y + 21, color);
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
