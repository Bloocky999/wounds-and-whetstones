package dev.hammerandhearth.client;

import dev.hammerandhearth.ForgeMath;
import dev.hammerandhearth.ForgePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Pixel-for-pixel smithing bench: each removed square is one server-validated hammer strike. */
public final class ForgeScreen extends Screen {
    private static final int CELL = 10;
    private static final int PANEL_W = 430;
    private static final int PANEL_H = 282;
    private static final String[] HANDLES = {"Madera", "Hueso", "Cuero", "Hierro"};
    private static ForgeScreen current;

    private String metal = "iron";
    private boolean[] cells = ForgeMath.billet();
    private int hits;
    private int handle = ForgeMath.WOOD;
    private int availableHandles;
    private String status = "Golpea píxeles para dibujar el perfil de la hoja.";
    private boolean closed;
    private int left, top, gridX, gridY, infoX;

    private ForgeScreen() {
        super(Component.literal("Forja de precisión"));
    }

    public static void receive(String encoded) {
        String[] fields = encoded.split("\\|", 7);
        if (fields.length != 7) return;
        Minecraft client = Minecraft.getInstance();
        if (Boolean.parseBoolean(fields[6])) {
            if (current != null) current.closed = true;
            current = null;
            client.gui.setScreen(null);
            return;
        }
        if (current == null) {
            current = new ForgeScreen();
            client.gui.setScreen(current);
        }
        current.metal = fields[0];
        current.cells = parsePixels(fields[1]);
        try {
            current.hits = Integer.parseInt(fields[2]);
            current.handle = Integer.parseInt(fields[3]);
            current.availableHandles = Integer.parseInt(fields[4]);
        } catch (NumberFormatException ignored) {
            current.availableHandles = 0;
        }
        current.status = fields[5];
    }

    private static boolean[] parsePixels(String encoded) {
        boolean[] result = new boolean[ForgeMath.PIXELS];
        for (int i = 0; i < Math.min(encoded.length(), result.length); i++) result[i] = encoded.charAt(i) == '1';
        return result;
    }

    private void send(int action, int argument) {
        if (Minecraft.getInstance().player != null) ClientPlayNetworking.send(new ForgePayloads.Action(action, argument));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        gridX = left + 14;
        gridY = top + 53;
        infoX = left + 192;

        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0211B16);
        g.fill(left + 2, top + 2, left + PANEL_W - 2, top + PANEL_H - 2, 0xFF3A3026);
        g.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xFF211F1C);
        g.fill(left + 7, top + 7, left + PANEL_W - 7, top + 25, 0xFF56432E);
        text(g, "FORJA DE PRECISIÓN", left + 14, top + 12, 0xFFFFD58A, true);
        text(g, "Yunque · " + (metal.equals("copper") ? "Cobre" : "Hierro") + " · golpe " + hits,
                gridX, top + 35, 0xFFE5D6B8, false);

        drawAnvilGrid(g, mouseX, mouseY);
        drawHandles(g, mouseX, mouseY);
        drawStats(g);
        text(g, fit(status, 48), left + 14, top + 224, 0xFFFFD58A, false);
        button(g, left + 193, top + 250, 112, "Forjar espada", true, mouseX, mouseY);
        button(g, left + 312, top + 250, 104, "Cancelar", false, mouseX, mouseY);
        text(g, "Clic: quitar píxel · clic derecho: deshacer golpe", left + 14, top + 268, 0xFFB9AA92, false);
    }

    private void drawAnvilGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int y = 0; y < ForgeMath.SIDE; y++) for (int x = 0; x < ForgeMath.SIDE; x++) {
            int px = gridX + x * CELL, py = gridY + y * CELL;
            g.fill(px, py, px + CELL - 1, py + CELL - 1, 0xFF100F0D);
            if (cells[ForgeMath.index(x, y)]) {
                int color = metal.equals("copper") ? 0xFFB76635 : 0xFF9B9A95;
                g.fill(px + 1, py + 1, px + CELL - 2, py + CELL - 2, color);
                g.fill(px + 1, py + 1, px + CELL - 2, py + 3, metal.equals("copper") ? 0xFFE59B58 : 0xFFD5D1C9);
                if (x >= 4 && y == 5) g.fill(px + 1, py + 1, px + CELL - 2, py + CELL - 2, 0xFF4A3628);
            }
            if (mouseX >= px && mouseX < px + CELL && mouseY >= py && mouseY < py + CELL) {
                g.fill(px, py, px + CELL - 1, py + CELL - 1, 0x80FFF0A0);
            }
        }
        g.fill(gridX - 2, gridY - 2, gridX + ForgeMath.SIDE * CELL + 2, gridY, 0xFF9E8462);
        g.fill(gridX - 2, gridY + ForgeMath.SIDE * CELL, gridX + ForgeMath.SIDE * CELL + 2, gridY + ForgeMath.SIDE * CELL + 2, 0xFF4D4030);
        g.fill(gridX - 2, gridY - 2, gridX, gridY + ForgeMath.SIDE * CELL + 2, 0xFF4D4030);
        g.fill(gridX + ForgeMath.SIDE * CELL, gridY - 2, gridX + ForgeMath.SIDE * CELL + 2, gridY + ForgeMath.SIDE * CELL + 2, 0xFF9E8462);
    }

    private void drawHandles(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        text(g, "EMPUÑADURA", infoX, top + 39, 0xFFE5D6B8, true);
        int y = top + 58;
        for (int i = 0; i < HANDLES.length; i++) {
            int x = infoX + i * 56;
            boolean selected = handle == i;
            boolean available = (availableHandles & (1 << i)) != 0;
            button(g, x, y, 52, HANDLES[i], selected, mouseX, mouseY, available);
        }
        ForgeMath.Stats s = ForgeMath.evaluate(metal, cells, handle);
        int color = s.valid() ? 0xFFFFD58A : 0xFFE5D6B8;
        text(g, "PERFIL: " + s.quality(), infoX, top + 91, color, true);
        text(g, "Daño: " + format(s.damage()), infoX, top + 111, 0xFFFFFFFF, false);
        text(g, "Ataque: " + format(4.0 + s.attackSpeed()) + "/s", infoX, top + 128, 0xFFFFFFFF, false);
        text(g, "Durabilidad: " + s.durability(), infoX, top + 145, 0xFFFFFFFF, false);
        text(g, "Encantabilidad: " + s.enchantability(), infoX, top + 162, 0xFFFFFFFF, false);
        text(g, "Filo: " + s.edgePixels() + " px · Punta: " + s.tipWidth() + " px", infoX, top + 179, 0xFFCAB99E, false);
        text(g, "Hoja: " + s.bladeLength() + " columnas · " + s.metalPixels() + " px", infoX, top + 196, 0xFFCAB99E, false);
        if (!s.valid()) text(g, fit(s.reason(), 35), infoX, top + 211, 0xFFFF8D70, false);
    }

    private void drawStats(GuiGraphicsExtractor g) {
        text(g, "Un píxel tallado", left + 14, top + 38, 0xFFFFD58A, true);
        text(g, "equivale a un golpe real.", left + 14, top + 216, 0xFFB9AA92, false);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, String label,
                        boolean selected, int mouseX, int mouseY) {
        button(g, x, y, w, label, selected, mouseX, mouseY, true);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, String label,
                        boolean selected, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 18;
        int bg = !enabled ? 0xFF302B25 : selected ? 0xFF795735 : hovered ? 0xFF67533C : 0xFF494035;
        g.fill(x, y, x + w, y + 18, bg);
        g.fill(x, y, x + w, y + 1, selected ? 0xFFFFD58A : 0xFF9D886A);
        text(g, label, x + 4, y + 5, enabled ? 0xFFFFF0D4 : 0xFF756F66, false);
    }

    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color, boolean shadow) {
        g.text(font, value, x, y, color, shadow);
    }

    private String fit(String value, int maxChars) {
        if (value.length() <= maxChars) return value;
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();
        if (button == 3) {
            send(1, 0);
            return true;
        }
        if (button != 1) return super.mouseClicked(event, doubleClick);
        int gx = (int) mouseX - gridX, gy = (int) mouseY - gridY;
        if (gx >= 0 && gy >= 0 && gx < ForgeMath.SIDE * CELL && gy < ForgeMath.SIDE * CELL) {
            send(0, ForgeMath.index(gx / CELL, gy / CELL));
            return true;
        }
        int handleY = top + 58;
        if (mouseY >= handleY && mouseY < handleY + 18 && mouseX >= infoX && mouseX < infoX + 4 * 56) {
            int selected = ((int) mouseX - infoX) / 56;
            send(2, selected);
            return true;
        }
        if (inside(mouseX, mouseY, left + 193, top + 250, 112, 18)) {
            send(3, 0);
            return true;
        }
        if (inside(mouseX, mouseY, left + 312, top + 250, 104, 18)) {
            send(4, 0);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public void onClose() {
        if (!closed) send(4, 0);
        current = null;
        super.onClose();
    }
}
