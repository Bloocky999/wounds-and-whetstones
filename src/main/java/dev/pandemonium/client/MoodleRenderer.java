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

/** Moodles al estilo Project Zomboid: iconos a la derecha con tooltip al pasar el raton. */
public final class MoodleRenderer {
    private static final Map<String, String[]> ICONS = new HashMap<>();
    private static final Map<String, String> SHAPE = new HashMap<>();

    static {
        ICONS.put("skull", new String[] {"..#####..", ".#######.", "##.###.##", "##.###.##", ".#######.", "..#####..", "..#.#.#..", "..#####..", "........."});
        ICONS.put("drop", new String[] {"....#....", "....#....", "...###...", "..#####..", ".#######.", ".#######.", ".#######.", "..#####..", "...###..."});
        ICONS.put("flame", new String[] {"....#....", "...##....", "...###...", "..####.#.", ".#######.", ".#######.", ".#######.", "..#####..", "...###..."});
        ICONS.put("snow", new String[] {"....#....", "#...#...#", ".#..#..#.", "..#.#.#..", "#########", "..#.#.#..", ".#..#..#.", "#...#...#", "....#...."});
        ICONS.put("bolt", new String[] {"......##.", ".....##..", "....##...", "...#####.", ".....##..", "....##...", "...##....", "..##.....", ".#......."});
        ICONS.put("eye", new String[] {".........", "..#####..", ".#######.", "##..#..##", "##.###.##", "##..#..##", ".#######.", "..#####..", "........."});
        ICONS.put("sound", new String[] {"....#..#.", "...##...#", "#####.#.#", "#####.#.#", "#####.#.#", "...##...#", "....#..#.", ".........", "........."});
        ICONS.put("cloud", new String[] {".........", "...###...", "..#####.#", "#########", "#########", ".#######.", ".........", ".........", "........."});
        ICONS.put("zzz", new String[] {"#####....", "...#.....", "..#......", ".#####...", "....#####", "......#..", ".....#...", "....#####", "........."});
        ICONS.put("spiral", new String[] {"#########", "#.......#", "#.#####.#", "#.#...#.#", "#.#.#.#.#", "#.#.###.#", "#.#.....#", "#.#######", "#........"});
        ICONS.put("cross", new String[] {"...###...", "...###...", "...###...", "#########", "#########", "#########", "...###...", "...###...", "...###..."});
        ICONS.put("heart", new String[] {".##...##.", "####.####", "#########", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."});
        ICONS.put("bug", new String[] {"..#...#..", "...#.#...", "..#####..", ".#######.", "#.#####.#", "..#####..", ".#.###.#.", "#..###..#", "....#...."});
        SHAPE.put("zombie_infection", "skull");
        SHAPE.put("tetanus", "bolt");
        SHAPE.put("flu", "cloud");
        SHAPE.put("hiccups", "sound");
        SHAPE.put("food_poisoning", "drop");
        SHAPE.put("pollen_allergy", "cloud");
        SHAPE.put("acrophobia", "spiral");
        SHAPE.put("ocd", "spiral");
        SHAPE.put("adhd", "bolt");
        SHAPE.put("dementia", "spiral");
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
        SHAPE.put("concussion", "spiral");
        SHAPE.put("lactose_intolerance", "drop");
        SHAPE.put("migraine", "bolt");
        SHAPE.put("tremors", "sound");
        SHAPE.put("tinnitus", "sound");
        SHAPE.put("insomnia", "zzz");
        SHAPE.put("colorblind", "eye");
        SHAPE.put("sprain", "cross");
        SHAPE.put("motion_sickness", "spiral");
        SHAPE.put("anemia", "drop");
        SHAPE.put("panic", "heart");
        SHAPE.put("arachnophobia", "bug");
        SHAPE.put("claustrophobia", "spiral");
        SHAPE.put("bleeding", "drop");
        SHAPE.put("cat_allergy", "cloud");
        SHAPE.put("epilepsy", "bolt");
    }

    private MoodleRenderer() {}

    /** True si hay chat o inventario abierto (se puede pasar el raton). */
    public static boolean isInteractiveScreen(Object screen) {
        return screen instanceof ChatScreen || screen instanceof AbstractContainerScreen<?>;
    }

    private static int severity(ClientState.Entry e) {
        String l = e.label();
        if (l.isEmpty()) return 1;
        char c = l.charAt(l.length() - 1);
        return Character.isDigit(c) ? Math.max(1, Math.min(4, c - '0')) : 1;
    }

    private static void icon(GuiGraphicsExtractor g, String shape, int x, int y, int argb) {
        String[] rows = ICONS.getOrDefault(shape, ICONS.get("cross"));
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (rows[r].charAt(c) == '#') g.fill(x + c * 2, y + r * 2, x + c * 2 + 2, y + r * 2 + 2, argb);
            }
        }
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, int mx, int my, boolean interactive) {
        if (ClientState.entries.isEmpty()) return;
        int w = mc.getWindow().getGuiScaledWidth();
        long now = System.currentTimeMillis();
        int size = 24;
        int gap = 4;
        int x = w - size - 6;
        int y = 6;
        ClientState.Entry hover = null;
        int hoverY = 0;
        for (ClientState.Entry e : ClientState.entries) {
            int color = e.fake() ? 0xDD4444 : HudRenderer.colorOf(e.id());
            int sev = severity(e);
            Long seen = ClientState.SEEN.get((e.fake() ? "fake:" : "") + e.id());
            float t = seen == null ? 1f : Math.min(1f, (now - seen) / 450f);
            float ease = 1f - (1f - t) * (1f - t) * (1f - t);
            int ix = x + (int) ((1f - ease) * (size + 12));
            int pulse = sev >= 3 ? (int) (40 + 70 * (0.5 + 0.5 * Math.sin(now / 220.0))) : 0;
            g.fill(ix - 1, y - 1, ix + size + 1, y + size + 1, 0xFF000000 | color);
            g.fill(ix, y, ix + size, y + size, 0xE0101018);
            if (pulse > 0) g.fill(ix, y, ix + size, y + size, (pulse << 24) | color);
            if (t < 1f) g.fill(ix, y, ix + size, y + size, ((int) ((1f - t) * 170) << 24) | 0xFFFFFF);
            String shape = e.fake() ? "cross" : SHAPE.getOrDefault(e.id(), "cross");
            icon(g, shape, ix + 3, y + 2, 0xFF000000 | color);
            // barritas de gravedad (etapa/fase)
            for (int i = 0; i < sev; i++) {
                g.fill(ix + 3 + i * 5, y + size - 3, ix + 6 + i * 5, y + size - 1, 0xFFFFFFFF);
            }
            if (interactive && mx >= ix && mx <= ix + size && my >= y && my <= y + size) {
                hover = e;
                hoverY = y;
            }
            y += size + gap;
        }
        if (hover != null) tooltip(g, mc, hover, x, hoverY);
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

    private static void tooltip(GuiGraphicsExtractor g, Minecraft mc, ClientState.Entry e, int iconX, int iconY) {
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
        boxW += 12;
        int boxH = 10 + 12 + lines.size() * 10 + 12;
        int bx = iconX - boxW - 8;
        int by = Math.max(4, iconY);
        g.fill(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, 0xFF000000 | color);
        g.fill(bx, by, bx + boxW, by + boxH, 0xF0101018);
        g.text(mc.font, title, bx + 6, by + 5, 0xFF000000 | color, true);
        int ty = by + 19;
        for (String l : lines) {
            g.text(mc.font, l, bx + 6, ty, 0xFFDDDDDD, true);
            ty += 10;
        }
        g.text(mc.font, info, bx + 6, ty + 2, 0xFF999999, true);
    }
}
