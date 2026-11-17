package dev.pandemonium;

import java.util.ArrayDeque;
import java.util.Queue;

/** Shared geometry rules for the server-authoritative pixel smithing game. */
public final class ForgeMath {
    public static final int SIDE = 16;
    public static final int PIXELS = SIDE * SIDE;
    public static final int WOOD = 0;
    public static final int BONE = 1;
    public static final int LEATHER = 2;
    public static final int IRON = 3;

    public record Stats(boolean valid, boolean connected, int metalPixels, int bladeLength, int edgePixels,
                        int tipWidth, double edgeRatio, double damage, double attackSpeed,
                        int durability, int enchantability, String quality, String reason) {}

    private ForgeMath() {}

    /** A flat billet with an integral tang. The right end becomes the point; the upper edge becomes the cutting edge. */
    public static boolean[] billet() {
        boolean[] cells = new boolean[PIXELS];
        for (int x = 4; x <= 14; x++) {
            for (int y = 5; y <= 11; y++) cells[index(x, y)] = true;
        }
        for (int x = 1; x <= 4; x++) {
            for (int y = 7; y <= 9; y++) cells[index(x, y)] = true;
        }
        return cells;
    }

    public static int index(int x, int y) { return y * SIDE + x; }
    public static boolean occupied(boolean[] cells, int x, int y) {
        return x >= 0 && y >= 0 && x < SIDE && y < SIDE && cells[index(x, y)];
    }

    public static Stats evaluate(String metal, boolean[] cells, int handle) {
        int count = 0;
        int maxX = -1;
        int tang = 0;
        for (int y = 0; y < SIDE; y++) for (int x = 0; x < SIDE; x++) {
            if (!occupied(cells, x, y)) continue;
            count++;
            maxX = Math.max(maxX, x);
            if (x <= 3) tang++;
        }
        int bladeLength = 0;
        int edge = 0;
        int tipWidth = 0;
        if (maxX >= 4) {
            bladeLength = maxX - 3;
            for (int x = 4; x <= maxX; x++) {
                if (!occupied(cells, x, 5) && occupied(cells, x, 6)) edge++;
            }
            for (int y = 0; y < SIDE; y++) if (occupied(cells, maxX, y)) tipWidth++;
        }

        boolean connected = isConnected(cells, count);
        double edgeRatio = bladeLength == 0 ? 0 : edge / (double) bladeLength;
        String reason = "";
        if (!connected) reason = "Une la hoja al espigo; no dejes píxeles aislados.";
        else if (tang < 5) reason = "Conserva metal suficiente en el espigo para fijar la empuñadura.";
        else if (bladeLength < 8) reason = "Alarga la hoja: necesita al menos ocho columnas de filo.";
        else if (tipWidth > 2) reason = "Afina la punta hasta dejar uno o dos píxeles en el extremo.";
        else if (edgeRatio < 0.55) reason = "Forma el filo: retira píxeles de la cara superior de la hoja.";
        else if (count < 24) reason = "Has quitado demasiado metal; la hoja no soportará los golpes.";

        boolean valid = reason.isEmpty();
        double copper = metal.equals("copper") ? 1.0 : 0.0;
        double materialDamage = metal.equals("copper") ? 2.5 + copper : 3.5 + 2.0;
        double shapeDamage = Math.min(0.85, Math.max(0, bladeLength - 8) * 0.06) + edgeRatio * 0.75;
        double speed = (metal.equals("copper") ? -2.8 : -2.6) + switch (handle) {
            case BONE -> 0.35;
            case LEATHER -> 0.18;
            case IRON -> -0.25;
            default -> 0.0;
        };
        int baseDurability = metal.equals("copper") ? 160 : 250;
        double handleDurability = switch (handle) {
            case BONE -> 0.86;
            case LEATHER -> 0.94;
            case IRON -> 1.15;
            default -> 1.0;
        };
        double retainedMetal = count / 86.0;
        int durability = (int) Math.round(baseDurability * Math.max(0.48, retainedMetal) * handleDurability);
        int enchantability = (metal.equals("copper") ? 11 : 14) + (int) Math.round(edgeRatio * 5)
                + (tipWidth == 1 ? 2 : 0) + switch (handle) {
                    case BONE -> 1;
                    case LEATHER -> 2;
                    case IRON -> -1;
                    default -> 0;
                };
        String quality = edgeRatio >= 0.88 && tipWidth == 1 && count >= 34 ? "Filo maestro"
                : edgeRatio >= 0.68 && tipWidth <= 2 ? "Filo limpio" : "Filo irregular";
        return new Stats(valid, connected, count, bladeLength, edge, tipWidth, edgeRatio,
                materialDamage + shapeDamage, speed, Math.max(36, durability), Math.max(1, enchantability),
                quality, reason);
    }

    private static boolean isConnected(boolean[] cells, int count) {
        if (count == 0) return false;
        boolean[] seen = new boolean[PIXELS];
        Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < PIXELS; i++) if (cells[i]) { queue.add(i); seen[i] = true; break; }
        int visited = 0;
        while (!queue.isEmpty()) {
            int i = queue.remove();
            visited++;
            int x = i % SIDE, y = i / SIDE;
            int[] neighbors = {index(x - 1, y), index(x + 1, y), index(x, y - 1), index(x, y + 1)};
            for (int n : neighbors) {
                if (n < 0 || n >= PIXELS) continue;
                int nx = n % SIDE, ny = n / SIDE;
                if (Math.abs(nx - x) + Math.abs(ny - y) != 1 || !cells[n] || seen[n]) continue;
                seen[n] = true;
                queue.add(n);
            }
        }
        return visited == count;
    }
}
