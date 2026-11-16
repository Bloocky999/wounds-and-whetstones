package dev.pandemonium.client;

import dev.pandemonium.Condition;
import dev.pandemonium.Conditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Moodles: casillas pequenas a la izquierda con aspecto de hueco de inventario de Minecraft.
 * Con el chat o el inventario abiertos, al pasar el raton sale un tooltip.
 */
public final class MoodleRenderer {
    private static final Map<String, String[]> ICONS = new HashMap<>();
    private static final Map<String, String> SHAPE = new HashMap<>();

    static {
        ICONS.put("skull", new String[] {".oooooo.", "o#h####o", "o#o##o#o", "o######o", ".o####o.", "..o##o..", "..o#o#o.", "...ooo.."});
        ICONS.put("drop", new String[] {"...oo...", "..o##o..", ".o#hh#o.", ".o#h##o.", "o##h###o", "o######o", ".o####o.", "..oooo.."});
        ICONS.put("flame", new String[] {"...o....", "..oo.o..", "..o#oo..", ".o##o#o.", ".o#hh#o.", "o##hh##o", ".o####o.", "..oooo.."});
        ICONS.put("snow", new String[] {"...oo...", "o..oo..o", ".o.##.o.", "..o##o..", "oo#hh#oo", "..o##o..", ".o.oo.o.", "o..oo..o"});
        ICONS.put("bolt", new String[] {"....oo..", "...o##o.", "..o##o..", ".o#hh#o.", "..oo#o..", "...o#o..", "..o#o...", "..oo...."});
        ICONS.put("eye", new String[] {"........", "..oooo..", ".o####o.", "o##oo##o", "o#ohho#o", ".o####o.", "..oooo..", "........"});
        ICONS.put("cloud", new String[] {"........", "........", "..ooo...", ".o###oo.", "o#h####o", "o######o", ".oooooo.", "........"});
        ICONS.put("zzz", new String[] {".oooooo.", ".o####o.", "..oo#o..", "..o#oo..", ".o#oo...", ".o####o.", ".oooooo.", "........"});
        ICONS.put("question", new String[] {"..oooo..", ".o####o.", "oo.oo#o.", "...o#o..", "..o#o...", "..o#o...", "........", "..o#o..."});
        ICONS.put("cross", new String[] {"..oooo..", "..o##o..", "ooo##ooo", "o#h####o", "o######o", "ooo##ooo", "..o##o..", "..oooo.."});
        ICONS.put("heart", new String[] {".oo..oo.", "o##oo##o", "o#h####o", "o######o", ".o####o.", "..o##o..", "...oo...", "........"});
        ICONS.put("bug", new String[] {"o..oo..o", ".o.oo.o.", "..o##o..", ".o#hh#o.", "oo####oo", ".o####o.", "o.o##o.o", "...oo..."});
        ICONS.put("sound", new String[] {"...o....", "..oo..o.", "ooo#..o.", "o###o.o.", "o###o.o.", "ooo#..o.", "..oo..o.", "...o...."});
        SHAPE.put("zombie_infection", "skull");
        SHAPE.put("tetanus", "bolt");
        SHAPE.put("flu", "cloud");
        SHAPE.put("hiccups", "sound");
        SHAPE.put("food_poisoning", "drop");
        SHAPE.put("pollen_allergy", "cloud");
        SHAPE.put("acrophobia", "question");
        SHAPE.put("ocd", "question");
        SHAPE.put("adhd", "bolt");
        SHAPE.put("dementia", "question");
        SHAPE.put("narcolepsy", "zzz");
        SHAPE.put("tourette", "bolt");
        SHAPE.put("asthma", "cloud");
        SHAPE.put("brittle_bones", "cross");
        SHAPE.put("arthrosis", "cross");
        SHAPE.put("blindness", "eye");
        SHAPE.put("hypochondria", "cross");
        SHAPE.put("frostbite", "snow");
        SHAPE.put("heatstroke", "flame");
        SHAPE.put("rabies", "skull");
        SHAPE.put("pox", "bug");
        SHAPE.put("concussion", "question");
        SHAPE.put("lactose_intolerance", "drop");
        SHAPE.put("migraine", "bolt");
        SHAPE.put("tremors", "sound");
        SHAPE.put("tinnitus", "sound");
        SHAPE.put("insomnia", "zzz");
        SHAPE.put("colorblind", "eye");
        SHAPE.put("sprain", "cross");
        SHAPE.put("motion_sickness", "question");
        SHAPE.put("anemia", "drop");
        SHAPE.put("panic", "heart");
        SHAPE.put("arachnophobia", "bug");
        SHAPE.put("claustrophobia", "question");
        SHAPE.put("bleeding", "drop");
        SHAPE.put("cat_allergy", "cloud");
        SHAPE.put("epilepsy", "bolt");
        SHAPE.put("cold", "cloud");
        SHAPE.put("gastroenteritis", "drop");
        SHAPE.put("measles", "bug");
        SHAPE.put("pinkeye", "eye");
        SHAPE.put("plague", "skull");
        SHAPE.put("salmonella", "drop");
        SHAPE.put("parasites", "bug");
        SHAPE.put("schizophrenia", "question");
        SHAPE.put("kleptomania", "bolt");
        SHAPE.put("lame_arm", "cross");
        SHAPE.put("bad_leg", "cross");
        SHAPE.put("night_blindness", "eye");
        SHAPE.put("mutism", "sound");
        SHAPE.put("stutter", "sound");
        SHAPE.put("vertigo", "question");
    }

    private MoodleRenderer() {}

    private static java.lang.reflect.Field screenField;
    private static boolean screenFieldSearched = false;

    /** Pantalla abierta ahora (se busca por reflexion el campo de tipo Screen para no depender del nombre). */
    public static Object currentScreen(Minecraft mc) {
        if (!screenFieldSearched) {
            screenFieldSearched = true;
            try {
                for (java.lang.reflect.Field f : Minecraft.class.getDeclaredFields()) {
                    if (f.getType() == net.minecraft.client.gui.screens.Screen.class) {
                        f.setAccessible(true);
                        screenField = f;
                        break;
                    }
                }
            } catch (Throwable ignored) {
                screenField = null;
            }
        }
        try {
            return screenField == null ? null : screenField.get(mc);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isInteractive(Object screen) {
        return screen instanceof ChatScreen || screen instanceof AbstractContainerScreen<?>;
    }

    /** True si hay chat o inventario abierto (se puede pasar el raton). */
    public static boolean isInteractiveScreen(Minecraft mc) {
        return isInteractive(currentScreen(mc));
    }

    private static int severity(ClientState.Entry e) {
        String l = e.label();
        if (l.isEmpty()) return 1;
        char c = l.charAt(l.length() - 1);
        return Character.isDigit(c) ? Math.max(1, Math.min(4, c - '0')) : 1;
    }

    private static int mix(int rgb, int with, float t) {
        int r = (int) (((rgb >> 16) & 255) * (1 - t) + ((with >> 16) & 255) * t);
        int g = (int) (((rgb >> 8) & 255) * (1 - t) + ((with >> 8) & 255) * t);
        int b = (int) ((rgb & 255) * (1 - t) + (with & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Icono de 8x8 celdas de 2 px: contorno oscuro, color principal y brillo. */
    private static void icon(GuiGraphicsExtractor g, String shape, int x, int y, int rgb) {
        String[] rows = ICONS.getOrDefault(shape, ICONS.get("cross"));
        int main = 0xFF000000 | rgb;
        int dark = mix(rgb, 0x000000, 0.7f);
        int light = mix(rgb, 0xFFFFFF, 0.6f);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char ch = rows[r].charAt(c);
                if (ch == '.') continue;
                int col = ch == 'o' ? dark : (ch == 'h' ? light : main);
                g.fill(x + c * 2, y + r * 2, x + c * 2 + 2, y + r * 2 + 2, col);
            }
        }
    }

    /** Hueco al estilo inventario de Minecraft. */
    private static void slot(GuiGraphicsExtractor g, int x, int y, int size, int rgb) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.fill(x, y, x + size, y + size, 0xFF8B8B8B);
        g.fill(x, y, x + size - 1, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + size - 1, 0xFF373737);
        g.fill(x + 1, y + size - 1, x + size, y + size, 0xFFFFFFFF);
        g.fill(x + size - 1, y + 1, x + size, y + size, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, (0x40 << 24) | rgb);
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, int mx, int my, boolean interactive) {
        if (ClientState.entries.isEmpty()) return;
        long now = System.currentTimeMillis();
        int size = 20;
        int gap = 2;
        int x = 5;
        int y = 5;
        ClientState.Entry hover = null;
        int hoverY = 0;
        for (ClientState.Entry e : ClientState.entries) {
            int color = e.fake() ? 0xDD4444 : HudRenderer.colorOf(e.id());
            int sev = severity(e);
            Long seen = ClientState.SEEN.get((e.fake() ? "fake:" : "") + e.id());
            float t = seen == null ? 1f : Math.min(1f, (now - seen) / 450f);
            float ease = 1f - (1f - t) * (1f - t) * (1f - t);
            int ix = x - (int) ((1f - ease) * (size + 12));
            slot(g, ix, y, size, color);
            if (sev >= 3) {
                int pulse = (int) (30 + 60 * (0.5 + 0.5 * Math.sin(now / 220.0)));
                g.fill(ix + 1, y + 1, ix + size - 1, y + size - 1, (pulse << 24) | color);
            }
            if (t < 1f) g.fill(ix, y, ix + size, y + size, ((int) ((1f - t) * 170) << 24) | 0xFFFFFF);
            String shape = e.fake() ? "cross" : SHAPE.getOrDefault(e.id(), "cross");
            icon(g, shape, ix + 2, y + 2, color);
            // gravedad: barritas bajo el icono
            for (int i = 0; i < sev; i++) {
                g.fill(ix + 3 + i * 4, y + size - 3, ix + 6 + i * 4, y + size - 1, 0xFF000000);
                g.fill(ix + 3 + i * 4, y + size - 3, ix + 5 + i * 4, y + size - 2, 0xFF000000 | color);
            }
            if (interactive && mx >= ix && mx <= ix + size && my >= y && my <= y + size) {
                hover = e;
                hoverY = y;
            }
            y += size + gap;
        }
        if (hover != null) tooltip(g, mc, hover, x + size, hoverY);
    }

    private static List<String> wrap(Minecraft mc, String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String tryLine = line.isEmpty() ? word : line + " " + word;
            if (mc.font.width(tryLine) > width && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tryLine);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    /** Tooltip con el aspecto de los de Minecraft (fondo morado oscuro y borde violeta). */
    private static void tooltip(GuiGraphicsExtractor g, Minecraft mc, ClientState.Entry e, int rightEdge, int iconY) {
        int color = e.fake() ? 0xDD4444 : HudRenderer.colorOf(e.id());
        String title;
        String desc;
        String info;
        Condition c = e.fake() ? null : Conditions.get(e.id());
        if (c == null) {
            title = e.id();
            desc = "Síntomas preocupantes. Te notas raro y no sabes por qué.";
            info = (Math.abs(e.id().hashCode()) % 2 == 0) ? "Dura unos minutos" : "Hasta que mueras";
        } else {
            title = c.name;
            desc = c.description;
            if (c.isTemporary()) info = "Dura unos " + Math.max(1, c.duration / 1200) + " min";
            else info = c.clearedOnDeath() ? "Hasta que mueras" : "Permanente";
        }
        if (!e.label().isEmpty()) title += " - " + e.label();
        List<String> lines = wrap(mc, desc, 190);
        int boxW = Math.max(mc.font.width(title), mc.font.width(info));
        for (String l : lines) boxW = Math.max(boxW, mc.font.width(l));
        int boxH = 10 + 4 + lines.size() * 10 + 12;
        int bx = rightEdge + 10;
        int by = Math.max(6, iconY - 4);
        // fondo y borde estilo vanilla
        g.fill(bx - 4, by - 3, bx + boxW + 4, by + boxH + 3, 0xF0100010);
        g.fill(bx - 3, by - 4, bx + boxW + 3, by - 3, 0xF0100010);
        g.fill(bx - 3, by + boxH + 3, bx + boxW + 3, by + boxH + 4, 0xF0100010);
        g.fill(bx - 3, by - 3, bx + boxW + 3, by - 2, 0x505000FF);
        g.fill(bx - 3, by + boxH + 2, bx + boxW + 3, by + boxH + 3, 0x5028007F);
        g.fill(bx - 3, by - 2, bx - 2, by + boxH + 2, 0x505000FF);
        g.fill(bx + boxW + 2, by - 2, bx + boxW + 3, by + boxH + 2, 0x5028007F);
        g.text(mc.font, title, bx, by, 0xFF000000 | color, true);
        int ty = by + 14;
        for (String l : lines) {
            g.text(mc.font, l, bx, ty, 0xFFDDDDDD, true);
            ty += 10;
        }
        g.text(mc.font, info, bx, ty + 2, 0xFF888888, true);
    }
}
