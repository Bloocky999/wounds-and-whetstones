package dev.pandemonium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Efectos de pantalla extra (todo con rectangulos; sin shaders). */
final class EffectsRenderer {
    private EffectsRenderer() {}

    private static int hash(int n) {
        n *= 0x9E3779B1;
        n ^= n >>> 15;
        n *= 0x85EBCA6B;
        n ^= n >>> 13;
        return n & 0x7fffffff;
    }

    private static float pulse(long t, int start, int len) {
        float x = (t - start) / (float) len;
        if (x < 0f || x > 1f) return 0f;
        return (float) Math.sin(x * Math.PI);
    }

    private static int alpha(float a, int rgb) {
        return (Math.max(0, Math.min(255, (int) (a * 255))) << 24) | (rgb & 0xFFFFFF);
    }

    /** Linea quebrada (grieta o vena) con ramas. */
    private static void crack(GuiGraphicsExtractor g, float x, float y, float ang, float len, int seed, int argb, int depth) {
        float step = 6f;
        int n = (int) (len / step);
        for (int i = 0; i < n; i++) {
            ang += ((hash(seed + i) % 100) / 100f - 0.5f) * 0.6f;
            x += (float) Math.cos(ang) * step;
            y += (float) Math.sin(ang) * step;
            g.fill((int) x, (int) y, (int) x + 2, (int) y + 2, argb);
            if (depth < 2 && i % 4 == 3) {
                float side = (hash(seed + i * 7) % 2 == 0) ? 0.9f : -0.9f;
                crack(g, x, y, ang + side, (len - i * step) * 0.5f, seed + i * 13, argb, depth + 1);
            }
        }
    }

    private static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int th, int argb) {
        g.fill(cx - r, cy - r, cx + r, cy - r + th, argb);
        g.fill(cx - r, cy + r - th, cx + r, cy + r, argb);
        g.fill(cx - r, cy - r, cx - r + th, cy + r, argb);
        g.fill(cx + r - th, cy - r, cx + r, cy + r, argb);
    }

    static void draw(GuiGraphicsExtractor g, Minecraft mc, int w, int h, long now) {
        // Latido: ataques de panico / ansiedad (doble latido con vineta)
        float anx = ClientState.fxLeft("anxiety");
        if (anx > 0) {
            long ph = now % 900;
            float beat = Math.max(pulse(ph, 0, 140), 0.7f * pulse(ph, 220, 140));
            HudRenderer.vignette(g, w, h, 0x330044, anx * (0.3f + 1.0f * beat));
        }

        // Grietas de hielo creciendo desde las esquinas (congelacion)
        if (ClientState.has("frostbite")) {
            float f = ClientState.fade("frostbite");
            int col = alpha(0.75f * f, 0xDDF4FF);
            float len = 40f + 110f * f;
            crack(g, 0, 0, 0.8f, len, 11, col, 0);
            crack(g, w, 0, 2.3f, len, 23, col, 0);
            crack(g, 0, h, -0.8f, len, 37, col, 0);
            crack(g, w, h, -2.3f, len, 41, col, 0);
        }

        // Ondulacion de calor (golpe de calor)
        if (ClientState.has("heatstroke")) {
            float f = ClientState.fade("heatstroke");
            for (int k = 0; k < 6; k++) {
                int baseY = (int) ((now / 18 + k * h / 6) % h);
                for (int x = 0; x < w; x += 20) {
                    int dy = (int) (Math.sin(x / 30.0 + now / 200.0 + k) * 3);
                    g.fill(x, baseY + dy, x + 20, baseY + dy + 3, alpha(0.10f * f, 0xFF9944));
                }
            }
        }

        // Esporas flotando (infeccion zombi, etapa 2+)
        if (ClientState.has("zombie_infection") && ClientState.severity("zombie_infection") >= 2) {
            int sev = ClientState.severity("zombie_infection");
            int count = 10 + sev * 6;
            for (int i = 0; i < count; i++) {
                int hx = hash(i * 31) % Math.max(1, w);
                int hy = hash(i * 17) % Math.max(1, h);
                int x = hx + (int) (Math.sin(now / 700.0 + i) * 10);
                int y = h - (int) ((now / 15 + hy) % h);
                g.fill(x, y, x + 2, y + 2, alpha(0.5f, 0x66CC44));
            }
        }

        // Venas oscuras que laten desde los bordes (zombi 3+ / rabia)
        boolean veins = (ClientState.has("zombie_infection") && ClientState.severity("zombie_infection") >= 3)
                || (ClientState.has("rabies") && ClientState.severity("rabies") >= 2);
        if (veins) {
            long ph = now % 1000;
            float beat = Math.max(pulse(ph, 0, 160), 0.6f * pulse(ph, 240, 160));
            int col = alpha(0.35f + 0.4f * beat, ClientState.has("rabies") ? 0x990000 : 0x225511);
            for (int i = 0; i < 6; i++) {
                int y = h * (i + 1) / 7;
                crack(g, 0, y, 0.15f, 70f, 100 + i, col, 1);
                crack(g, w, y, 3.0f, 70f, 200 + i, col, 1);
            }
        }

        // Glitch: tics, convulsiones, conmocion
        boolean glitch = ClientState.fxLeft("tic") > 0 || ClientState.fxLeft("seizure") > 0 || ClientState.fxLeft("ringing") > 0.6f;
        if (glitch) {
            long bucket = now / 50;
            for (int i = 0; i < 7; i++) {
                int hh = hash((int) bucket * 7 + i);
                int y = hh % Math.max(1, h);
                int bh = 2 + (hh >> 8) % 9;
                int off = ((hh >> 4) % 31) - 15;
                int rgb = (i % 2 == 0) ? 0x00FFFF : 0xFF00FF;
                g.fill(off, y, w + off, y + bh, alpha(0.28f, rgb));
            }
        }

        // Aberracion cromatica en los bordes (conmocion / migrana)
        if (ClientState.has("concussion") || ClientState.has("migraine")) {
            float p = 0.5f + 0.5f * (float) Math.sin(now / 260.0);
            int e = 4 + (int) (6 * p);
            g.fill(0, 0, e, h, alpha(0.22f, 0xFF0000));
            g.fill(w - e, 0, w, h, alpha(0.22f, 0x0000FF));
        }

        // Gotas de sudor (gripe / golpe de calor)
        if (ClientState.has("flu") || ClientState.has("heatstroke")) {
            for (int i = 0; i < 4; i++) {
                int x = w * (2 * i + 1) / 9 + hash(i) % 30;
                int y = (int) ((now / 25 + i * 211) % (h + 40)) - 20;
                g.fill(x, y, x + 3, y + 8, alpha(0.30f, 0xBBE6FF));
                g.fill(x - 1, y + 8, x + 4, y + 11, alpha(0.35f, 0xBBE6FF));
            }
        }

        // Vomito: chorretes verdes cayendo desde arriba
        float sick = ClientState.fxLeft("sick");
        if (sick > 0) {
            float grow = 1f - sick;
            for (int i = 0; i < 7; i++) {
                int x = w * (i + 1) / 8 + hash(i + 5) % 20;
                int len = (int) ((h * 0.25f + hash(i + 9) % 40) * Math.min(1f, grow * 2.5f));
                g.fill(x, 0, x + 4, len, alpha(0.55f * sick + 0.15f, 0x88AA22));
                g.fill(x - 1, len, x + 5, len + 5, alpha(0.55f * sick + 0.15f, 0x99BB33));
            }
        }

        // Ondas del tinnitus saliendo del centro
        float tin = ClientState.fxLeft("tinnitus");
        if (tin > 0) {
            int r = (int) ((1f - tin) * Math.min(w, h) * 0.6f) + 4;
            ring(g, w / 2, h / 2, r, 2, alpha(tin * 0.5f, 0xFFFFFF));
        }

        // Eco/ping de la ceguera
        if (ClientState.has("blindness")) {
            long ph = now % 2600;
            if (ph < 1400) {
                float x = ph / 1400f;
                int r = (int) (x * Math.min(w, h) * 0.55f) + 4;
                ring(g, w / 2, h / 2, r, 2, alpha((1f - x) * 0.45f, 0x88CCFF));
            }
        }

        // Respiracion del asma: tunel que se cierra y abre
        float gasp = ClientState.fxLeft("gasp");
        if (gasp > 0) {
            float breath = 0.5f + 0.5f * (float) Math.sin(now / 450.0);
            HudRenderer.vignette(g, w, h, 0x000000, gasp * breath * 1.3f);
        }

        // "Zzz" flotando (insomnio / narcolepsia)
        if (ClientState.has("insomnia") || ClientState.fxLeft("sleep") > 0) {
            for (int i = 0; i < 3; i++) {
                int x = w / 2 - 40 + i * 30 + (int) (Math.sin(now / 500.0 + i) * 6);
                int y = h / 2 - 10 - (int) ((now / 20 + i * 40) % 90);
                g.text(mc.font, "z", x, y, alpha(0.6f, 0xCCCCFF), true);
            }
        }

        // Cristales rotos al recibir la dosis del modo programado
        float dose = ClientState.fxLeft("dose");
        if (dose > 0) {
            float grow = Math.min(1f, (1f - dose) * 6f);
            int col = alpha(dose, 0xFFFFFF);
            for (int i = 0; i < 8; i++) {
                crack(g, w / 2f, h / 2f, i * 0.785f, (60f + 200f * grow), 300 + i, col, 0);
            }
        }
    }
}
