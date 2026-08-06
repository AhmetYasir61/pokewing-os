package com.pokewing.efmocap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared chrome for every studio menu: the header, the icon rail that moves
 * between menus, the transport and timeline along the bottom, and the
 * hand-drawn widget system. Each workspace is its own screen and only has to
 * fill in the middle.
 *
 * <p>The world is never dimmed — the scene stays watchable and scrubbable while
 * it's being directed.</p>
 */
public abstract class StudioScreen extends Screen {

    /** A clickable region we draw ourselves; vanilla buttons don't fit the look. */
    protected record Zone(int x, int y, int w, int h, String label, String tip,
                          Runnable action, int kind, boolean on) {
        public static final int BUTTON = 0, RAIL = 1, ROW = 2, DANGER = 3;
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    protected final List<Zone> zones = new ArrayList<>();
    protected String status = "";
    private boolean draggingHead;

    protected int railW, listX, listW, propX, propW, contentY, contentH, tlY, tlH;

    protected StudioScreen(String title) {
        super(Component.literal(title));
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Label shown above the left-hand list. */
    protected abstract String listTitle();

    /** Build this menu's zones and widgets. */
    protected abstract void buildContent();

    /** Which rail entry is lit. */
    protected abstract int railIndex();

    @Override
    protected void init() {
        railW = 34;
        tlH = Math.max(96, Math.min(150, height / 4));
        contentY = 26;
        tlY = height - tlH;
        contentH = tlY - contentY - 4;
        listX = railW + 4;
        listW = Math.max(150, Math.min(230, width / 4));
        propX = listX + listW + 4;
        propW = width - propX - 4;
        rebuild();
    }

    protected void rebuild() {
        zones.clear();
        clearWidgets();
        buildRail();
        buildContent();
    }

    private void buildRail() {
        railBtn("Ç", "Çekimler & sahne", 0, contentY, () -> go(new TakesScreen()));
        railBtn("K", "Karakter oluşturucu", 1, contentY + 38, () -> go(new CharacterScreen()));
        railBtn("M", "Kamera", 2, contentY + 76, () -> go(new CameraScreen()));
        railBtn("R", "Render & ayarlar", 3, contentY + 114, () -> go(new RenderScreen()));
    }

    private void railBtn(String glyph, String tip, int index, int y, Runnable r) {
        zones.add(new Zone(2, y, railW - 4, 34, glyph, tip, r, Zone.RAIL, railIndex() == index));
    }

    protected void go(Screen s) {
        if (minecraft != null) minecraft.setScreen(s);
    }

    // --- widget helpers --------------------------------------------------

    protected void btn(int x, int y, int w, String label, String tip, Runnable r) {
        zones.add(new Zone(x, y, w, 18, label, tip, r, Zone.BUTTON, false));
    }

    protected void toggle(int x, int y, int w, String label, boolean on, Runnable r) {
        zones.add(new Zone(x, y, w, 18, label, null, r, Zone.BUTTON, on));
    }

    protected void danger(int x, int y, int w, String label, Runnable r) {
        zones.add(new Zone(x, y, w, 18, label, null, r, Zone.DANGER, false));
    }

    protected void row(int x, int y, int w, String label, String tip,
                       boolean selected, Runnable r) {
        zones.add(new Zone(x, y, w, 18, label, tip, r, Zone.ROW, selected));
    }

    // --- rendering -------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        Theme.panel(g, 0, 0, width, 26, Theme.HEADER);
        g.drawString(font, "EFMocap", 10, 9, Theme.TEXT, false);
        g.drawString(font, "§8" + title.getString(), 70, 9, Theme.TEXT_DIM, false);

        String live = (MocapRecorder.INSTANCE.isRecording() ? "§c● KAYIT  " : "")
                + (VideoRecorder.INSTANCE.isActive() ? "§c● VİDEO  " : "")
                + "§8" + TakeLibrary.INSTANCE.count() + " çekim · "
                + CharacterLibrary.INSTANCE.count() + " karakter · "
                + CameraDirector.INSTANCE.keyCount() + " kamera";
        g.drawString(font, live, width - font.width(live) - 10, 9, Theme.TEXT_DIM, false);

        g.fill(0, 26, railW, tlY, Theme.RAIL);
        if (usesList()) {
            Theme.panel(g, listX, contentY, listW, contentH, Theme.PANEL);
            g.drawString(font, "§8" + listTitle(), listX + 8, contentY + 7, Theme.TEXT_DIM, false);
            Theme.panel(g, propX, contentY, propW, contentH, Theme.PANEL);
        } else {
            Theme.panel(g, listX, contentY, width - listX - 4, contentH, Theme.PANEL);
        }

        for (Zone z : zones) {
            boolean hov = z.hit(mouseX, mouseY);
            switch (z.kind()) {
                case Zone.RAIL -> {
                    g.fill(z.x(), z.y(), z.x() + z.w(), z.y() + z.h(),
                            z.on() ? Theme.ACCENT_D : (hov ? Theme.BTN_HOV : 0xFF1B1B20));
                    if (z.on()) g.fill(z.x(), z.y(), z.x() + 2, z.y() + z.h(), Theme.ACCENT);
                    g.drawString(font, z.label(),
                            z.x() + (z.w() - font.width(z.label())) / 2, z.y() + 13,
                            z.on() ? 0xFFFFFFFF : Theme.TEXT, false);
                }
                case Zone.ROW -> Theme.row(g, font, z.x(), z.y(), z.w(), z.h(),
                        z.label(), hov, z.on());
                case Zone.DANGER -> Theme.button(g, font, z.x(), z.y(), z.w(), z.h(),
                        z.label(), hov, false, true);
                default -> Theme.button(g, font, z.x(), z.y(), z.w(), z.h(),
                        z.label(), hov, z.on(), false);
            }
        }

        super.render(g, mouseX, mouseY, partial);
        renderTimeline(g, mouseX, mouseY);

        if (!status.isEmpty()) {
            g.drawString(font, status, listX + 8, tlY - 14, Theme.TEXT, false);
        }
        for (Zone z : zones) {
            if (z.tip() != null && z.hit(mouseX, mouseY)) {
                g.renderTooltip(font, Component.literal(z.tip()), mouseX, mouseY);
                break;
            }
        }
    }

    /** Render menus use the full width instead of a list + properties split. */
    protected boolean usesList() { return true; }

    // --- timeline --------------------------------------------------------

    protected int trackX() { return 108; }
    protected int trackW() { return width - trackX() - 12; }

    /** Take highlighted on the timeline, if this menu tracks one. */
    protected String highlightedTake() { return ""; }

    private void renderTimeline(GuiGraphics g, int mouseX, int mouseY) {
        Theme.panel(g, 0, tlY, width, tlH, Theme.PANEL);

        ReplayDirector rd = ReplayDirector.INSTANCE;
        int len = Math.max(1, rd.sceneLength());
        int head = rd.sceneTick();

        int by = tlY + 6;
        drawMini(g, 8, by, 20, rd.isPaused() ? "▶" : "❚❚", mouseX, mouseY);
        drawMini(g, 32, by, 20, "■", mouseX, mouseY);
        drawMini(g, 56, by, 20, "|◀", mouseX, mouseY);
        g.drawString(font, Theme.time(head) + " §8/ " + Theme.time(len),
                8, by + 26, Theme.TEXT, false);
        g.drawString(font, "§8" + rd.activeCount() + " oyuncu sahnede",
                8, by + 38, Theme.TEXT_DIM, false);

        int tx = trackX(), tw = trackW(), ty = tlY + 6;

        g.fill(tx, ty, tx + tw, ty + 10, Theme.PANEL_IN);
        for (int s = 0; s <= len / 20; s++) {
            int px = tx + (int) ((s * 20.0 / len) * tw);
            g.fill(px, ty + 4, px + 1, ty + 10, Theme.LINE);
            if (s % 5 == 0) g.drawString(font, "§8" + s, px + 2, ty + 1, Theme.TEXT_DIM, false);
        }

        int cy = ty + 14;
        String hl = highlightedTake();
        for (String name : TakeLibrary.INSTANCE.names()) {
            if (cy > tlY + tlH - 16) break;
            MocapRecording r = TakeLibrary.INSTANCE.get(name);
            if (r == null) continue;
            g.fill(tx, cy, tx + tw, cy + 12, Theme.TRACK);
            int cw = Math.max(2, (int) ((r.length() / (double) len) * tw));
            boolean sel = name.equals(hl);
            g.fill(tx, cy, tx + cw, cy + 12, sel ? Theme.CLIP_SEL : Theme.CLIP);
            g.drawString(font, Theme.trim(font, name, 92), 8, cy + 2,
                    sel ? Theme.TEXT : Theme.TEXT_DIM, false);
            if (r.deathTick >= 0) {
                int dx = tx + (int) ((r.deathTick / (double) len) * tw);
                g.fill(dx, cy - 1, dx + 2, cy + 13, Theme.ACCENT);
            }
            cy += 14;
        }

        int camLen = CameraDirector.INSTANCE.path().durationTicks();
        if (camLen > 0 && cy <= tlY + tlH - 16) {
            g.fill(tx, cy, tx + tw, cy + 12, Theme.TRACK);
            int cw = Math.max(2, (int) ((camLen / (double) len) * tw));
            g.fill(tx, cy, tx + cw, cy + 12, Theme.CAM_CLIP);
            g.drawString(font, "§8kamera", 8, cy + 2, Theme.TEXT_DIM, false);
        }

        int hx = tx + (int) ((Math.min(head, len) / (double) len) * tw);
        g.fill(hx, ty, hx + 1, tlY + tlH - 4, Theme.ACCENT);
        g.fill(hx - 3, ty, hx + 4, ty + 4, Theme.ACCENT);
    }

    private void drawMini(GuiGraphics g, int x, int y, int w, String label, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 16;
        g.fill(x, y, x + w, y + 16, hov ? Theme.BTN_HOV : Theme.BTN);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 4, Theme.TEXT, false);
    }

    // --- input -----------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;

        ReplayDirector rd = ReplayDirector.INSTANCE;
        int by = tlY + 6;
        if (my >= by && my < by + 16) {
            if (mx >= 8 && mx < 28) {
                if (rd.activeCount() == 0) rd.playScene(true);
                rd.setPaused(!rd.isPaused());
                return true;
            }
            if (mx >= 32 && mx < 52) { rd.clearAll(); return true; }
            if (mx >= 56 && mx < 76) { rd.setPaused(false); rd.restart(); return true; }
        }

        if (my >= tlY && mx >= trackX()) {
            draggingHead = true;
            scrubTo(mx);
            return true;
        }

        for (Zone z : zones) {
            if (z.hit(mx, my)) { z.action().run(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingHead) { scrubTo(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingHead = false;
        return super.mouseReleased(mx, my, button);
    }

    private void scrubTo(double mx) {
        ReplayDirector rd = ReplayDirector.INSTANCE;
        if (rd.activeCount() == 0) rd.playScene(true);
        int len = Math.max(1, rd.sceneLength());
        double f = (mx - trackX()) / (double) trackW();
        rd.setPaused(true);
        rd.seek((int) Math.round(Math.max(0, Math.min(1, f)) * len));
    }
}
