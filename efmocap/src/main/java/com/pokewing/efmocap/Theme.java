package com.pokewing.efmocap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/** Flat dark studio palette and the small drawing helpers the editor needs. */
public final class Theme {
    private Theme() {}

    public static final int HEADER   = 0xFF16161A;
    public static final int PANEL    = 0xEF1E1E23;
    public static final int PANEL_IN = 0xFF17171B;
    public static final int RAIL     = 0xFF121215;
    public static final int LINE     = 0xFF34343E;
    public static final int ACCENT   = 0xFFC8453F;
    public static final int ACCENT_D = 0xFF8E2F2B;
    public static final int TEXT     = 0xFFE2E2E6;
    public static final int TEXT_DIM = 0xFF8B8B95;
    public static final int BTN      = 0xFF2A2A32;
    public static final int BTN_HOV  = 0xFF3A3A46;
    public static final int TRACK    = 0xFF232329;
    public static final int CLIP     = 0xFF3C6E8F;
    public static final int CLIP_SEL = 0xFF4E8FB8;
    public static final int CAM_CLIP = 0xFF7A5AA8;
    public static final int OK       = 0xFF5BA85B;

    /** Panel with a 1px border. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int bg) {
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, LINE);
        g.fill(x, y + h - 1, x + w, y + h, LINE);
        g.fill(x, y, x + 1, y + h, LINE);
        g.fill(x + w - 1, y, x + w, y + h, LINE);
    }

    /** Button face; {@code on} paints it in the accent colour. */
    public static void button(GuiGraphics g, Font font, int x, int y, int w, int h,
                              String label, boolean hovered, boolean on, boolean danger) {
        int bg = on ? (hovered ? ACCENT : ACCENT_D)
                    : danger ? (hovered ? 0xFF7A2F2F : 0xFF44262A)
                    : (hovered ? BTN_HOV : BTN);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, on ? ACCENT : LINE);
        String text = trim(font, label, w - 8);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2,
                on ? 0xFFFFFFFF : TEXT, false);
    }

    /** Left-aligned list row. */
    public static void row(GuiGraphics g, Font font, int x, int y, int w, int h,
                           String label, boolean hovered, boolean selected) {
        g.fill(x, y, x + w, y + h, selected ? 0xFF33333E : (hovered ? 0xFF2A2A33 : 0xFF212128));
        if (selected) g.fill(x, y, x + 2, y + h, ACCENT);
        String text = trim(font, label, w - 12);
        g.drawString(font, text, x + 8, y + (h - 8) / 2, selected ? TEXT : 0xFFC2C2CA, false);
    }

    public static String trim(Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && font.width(s + "…") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /** Ticks -> 0:03.4 */
    public static String time(int ticks) {
        double sec = ticks / 20.0;
        int m = (int) (sec / 60);
        double s = sec - m * 60;
        return String.format(java.util.Locale.ROOT, "%d:%04.1f", m, s);
    }
}
