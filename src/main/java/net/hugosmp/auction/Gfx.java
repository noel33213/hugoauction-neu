package net.hugosmp.auction;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;

/** Kleine Zeichenhelfer. Alle Farben sind ARGB. */
final class Gfx {
    private Gfx() {}

    static int argb(int alpha, int rgb) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    static double ease(double t) {
        t = clamp01(t);
        return 1 - Math.pow(1 - t, 3);
    }

    static int lerpColor(int c1, int c2, double t) {
        t = clamp01(t);
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (r << 16) | (g << 8) | b;
    }

    /** Rechteck mit leicht abgerundeten Ecken. */
    static void rect(DrawContext c, int x1, int y1, int x2, int y2, int color) {
        c.fill(x1 + 1, y1, x2 - 1, y2, color);
        c.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    static Text styled(String s, boolean bold) {
        return bold ? Text.literal(s).formatted(Formatting.BOLD) : Text.literal(s);
    }

    static int width(TextRenderer tr, String s, boolean bold) {
        return tr.getWidth(styled(s, bold));
    }

    private static int fixAlpha(int color) {
        int a = color >>> 24;
        if (a == 0) return color | 0xFF000000;
        return color;
    }

    static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int color, boolean bold) {
        if ((color >>> 24) != 0 && (color >>> 24) < 6) return;
        c.drawTextWithShadow(tr, styled(s, bold), x, y, fixAlpha(color));
    }

    static void centered(DrawContext c, TextRenderer tr, String s, int cx, int y, int color, boolean bold) {
        text(c, tr, s, cx - width(tr, s, bold) / 2, y, color, bold);
    }

    static void right(DrawContext c, TextRenderer tr, String s, int rx, int y, int color, boolean bold) {
        text(c, tr, s, rx - width(tr, s, bold), y, color, bold);
    }

    static void scaledText(DrawContext c, TextRenderer tr, String s, float x, float y, float scale, int color, boolean bold) {
        if ((color >>> 24) != 0 && (color >>> 24) < 6) return;
        Matrix3x2fStack st = c.getMatrices();
        st.pushMatrix();
        st.translate(x, y);
        st.scale(scale, scale);
        c.drawTextWithShadow(tr, styled(s, bold), 0, 0, fixAlpha(color));
        st.popMatrix();
    }

    static void scaledItem(DrawContext c, ItemStack stack, float x, float y, float scale) {
        if (stack == null || stack.isEmpty()) return;
        Matrix3x2fStack st = c.getMatrices();
        st.pushMatrix();
        st.translate(x, y);
        st.scale(scale, scale);
        c.drawItem(stack, 0, 0);
        st.popMatrix();
    }

    static String trunc(TextRenderer tr, String s, int maxW, boolean bold) {
        if (width(tr, s, bold) <= maxW) return s;
        while (s.length() > 1 && width(tr, s + "..", bold) > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }
}
