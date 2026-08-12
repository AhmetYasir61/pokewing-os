package com.pokewing.pokeface.client;

import com.pokewing.pokeface.face.FaceProfile;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;

/**
 * Pixel editor for the parts of the face a player draws by hand.
 *
 * <p>Two modes share the screen. The <b>face</b> canvas is the 8x8 patch that is
 * baked into the skin itself (see {@link SkinOverride}) — painting skin tone over
 * the eyes a skin already has is what stops them colliding with the animated
 * ones. The <b>eye</b> canvas is the sprite the animated iris uses, and it is
 * <em>two</em> canvases side by side: the left and right eye are drawn
 * separately, because a pair of eyes is rarely the same drawing twice. Until the
 * right one is touched it simply mirrors the left.
 *
 * <p>The layout is computed from a single running Y so the controls, the palette
 * and the preview cannot land on top of each other at any GUI scale.
 */
public final class FacePixelEditorScreen extends Screen {

    private static final int FACE_GRID = FaceProfile.FACE_SIZE;
    private static final int PANEL_WIDTH = 200;
    private static final int ROW = 22;
    private static final int SWATCH = 13;
    private static final int[] PRESETS = {
            0xFFF9DFC8, 0xFFEDC5A4, 0xFFD2A278, 0xFFB07B54, 0xFF8D5524,
            0xFF6B4423, 0xFF3D2314, 0xFFFFFFFF, 0xFF9A9A9A, 0xFF101010,
            0xFF3A6BA5, 0xFF2E8B57, 0xFFB03060, 0xFFE0A030, 0xFF7A3FB0,
    };

    private final Screen parent;
    private final FaceProfile profile;
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();

    private int cell = 20;
    private int color = 0xFFEDC5A4;
    private Tool tool = Tool.BRUSH;
    private Canvas canvas = Canvas.FACE;
    private boolean paintingLeft = true;
    private boolean painting;

    private int panelX;
    private int paletteY;
    private int previewY;

    private enum Tool {
        BRUSH, ERASER, FILL
    }

    private enum Canvas {
        FACE, EYE
    }

    private record Snapshot(boolean left, int[] pixels) {
    }

    public FacePixelEditorScreen(Screen parent, FaceProfile profile) {
        super(Component.translatable("pokeface.editor.title"));
        this.parent = parent;
        this.profile = profile;
    }

    private boolean eyeMode() {
        return this.canvas == Canvas.EYE;
    }

    private int gridSize() {
        return eyeMode() ? Math.max(1, this.profile.eyeArtSize) : FACE_GRID;
    }

    private int[] pixels(boolean left) {
        if (!eyeMode()) {
            return this.profile.normalisedPixels();
        }
        if (left) {
            return this.profile.eyeArt;
        }
        if (this.profile.eyeArtRight == null || this.profile.eyeArtRight.length != this.profile.eyeArt.length) {
            this.profile.eyeArtRight = new int[this.profile.eyeArt.length];
        }
        return this.profile.eyeArtRight;
    }

    /** Left edge of a canvas; the eye mode lays two of them out side by side. */
    private int gridLeft(boolean left) {
        int span = gridSize() * this.cell;
        int area = this.width - PANEL_WIDTH;
        if (!eyeMode()) {
            return area / 2 - span / 2;
        }
        int gap = 24;
        int total = span * 2 + gap;
        int start = area / 2 - total / 2;
        return left ? start : start + span + gap;
    }

    private int gridTop() {
        return this.height / 2 - (gridSize() * this.cell) / 2 - 10;
    }

    /** Keeps the canvases on screen at any resolution or GUI scale. */
    private void fitCell() {
        int columns = eyeMode() ? 2 : 1;
        int widthBudget = (this.width - PANEL_WIDTH - 60) / columns;
        int heightBudget = this.height - 120;
        this.cell = Mth.clamp(Math.min(widthBudget, heightBudget) / Math.max(1, gridSize()), 4, 40);
    }

    @Override
    protected void init() {
        if (eyeMode() && !this.profile.hasEyeArt()) {
            this.profile.resizeEyeArt(8);
        }
        fitCell();

        this.panelX = this.width - PANEL_WIDTH + 10;
        int x = this.panelX;
        int w = PANEL_WIDTH - 20;
        int h = 20;
        int y = 34;

        addRenderableWidget(Button.builder(
                Component.translatable(eyeMode() ? "pokeface.editor.canvas_eye" : "pokeface.editor.canvas_face"),
                b -> {
                    this.canvas = eyeMode() ? Canvas.FACE : Canvas.EYE;
                    this.undoStack.clear();
                    rebuildWidgets();
                }).bounds(x, y, w, h).build());
        y += ROW + 4;

        if (eyeMode()) {
            int bw = (w - 9) / 4;
            for (int i = 0; i < FaceProfile.EYE_ART_SIZES.length; i++) {
                int size = FaceProfile.EYE_ART_SIZES[i];
                addRenderableWidget(Button.builder(Component.literal(String.valueOf(size)),
                        b -> {
                            this.profile.resizeEyeArt(size);
                            this.undoStack.clear();
                            rebuildWidgets();
                        }).bounds(x + i * (bw + 3), y, bw, h).build());
            }
            y += ROW + 4;
        }

        int tw = (w - 6) / 3;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.brush"),
                b -> this.tool = Tool.BRUSH).bounds(x, y, tw, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.eraser"),
                b -> this.tool = Tool.ERASER).bounds(x + tw + 3, y, tw, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.fill"),
                b -> this.tool = Tool.FILL).bounds(x + (tw + 3) * 2, y, tw, h).build());
        y += ROW + 4;

        y = addChannel(x, y, w, h, "pokeface.editor.red", 16);
        y = addChannel(x, y, w, h, "pokeface.editor.green", 8);
        y = addChannel(x, y, w, h, "pokeface.editor.blue", 0);
        y += 4;

        // Palette and the colour preview are drawn, not widgets, so their rows
        // are reserved here and the running Y walks past them.
        this.paletteY = y;
        int paletteRows = (PRESETS.length + 4) / 5;
        y += paletteRows * SWATCH + 6;
        this.previewY = y;
        y += 16 + 6;

        int half = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.undo"),
                b -> undo()).bounds(x, y, half, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.clear"),
                b -> {
                    pushUndo(this.paintingLeft);
                    Arrays.fill(pixels(this.paintingLeft), 0);
                }).bounds(x + half + 4, y, half, h).build());
        y += ROW + 2;

        if (eyeMode()) {
            addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.mirror_to_right"),
                    b -> {
                        pushUndo(false);
                        this.profile.mirrorEyeArtToRight();
                    }).bounds(x, y, w, h).build());
            y += ROW + 2;
            addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.use_builtin"),
                    b -> {
                        this.profile.eyeArtSize = 0;
                        this.profile.eyeArt = new int[0];
                        this.profile.eyeArtRight = new int[0];
                        this.canvas = Canvas.FACE;
                        rebuildWidgets();
                    }).bounds(x, y, w, h).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.cover_eyes"),
                    b -> coverSkinEyes()).bounds(x, y, w, h).build());
        }
        y += ROW + 6;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(x, y, w, h).build());
    }

    private int addChannel(int x, int y, int w, int h, String key, int shift) {
        addRenderableWidget(new AbstractSliderButton(x, y, w, h, Component.empty(),
                (this.color >>> shift & 0xFF) / 255.0D) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable(key).append(Component.literal(
                        ": " + Math.round(this.value * 255.0D))));
            }

            @Override
            protected void applyValue() {
                int v = (int) Math.round(this.value * 255.0D) & 0xFF;
                FacePixelEditorScreen.this.color =
                        (FacePixelEditorScreen.this.color & ~(0xFF << shift)) | (v << shift) | 0xFF000000;
            }
        });
        return y + ROW;
    }

    /**
     * One-click version of the whole point of the face canvas: fills the two rows
     * a vanilla-proportioned skin puts its eyes on with the current colour.
     */
    private void coverSkinEyes() {
        pushUndo(true);
        for (int y = 3; y <= 4; y++) {
            for (int x = 0; x < FACE_GRID; x++) {
                this.profile.setPixel(x, y, this.color);
            }
        }
    }

    private void pushUndo(boolean left) {
        this.undoStack.push(new Snapshot(left, pixels(left).clone()));
        if (this.undoStack.size() > 32) {
            this.undoStack.removeLast();
        }
    }

    private void undo() {
        Snapshot previous = this.undoStack.poll();
        if (previous == null) {
            return;
        }
        int[] target = pixels(previous.left());
        if (previous.pixels().length == target.length) {
            System.arraycopy(previous.pixels(), 0, target, 0, target.length);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, (this.width - PANEL_WIDTH) / 2, 14, 0xFFFFFF);

        if (eyeMode()) {
            drawGrid(graphics, true, Component.translatable("pokeface.editor.left_eye"));
            drawGrid(graphics, false, Component.translatable("pokeface.editor.right_eye"));
        } else {
            drawGrid(graphics, true, Component.translatable("pokeface.editor.canvas_face"));
        }

        drawPalette(graphics);

        graphics.fill(this.panelX, this.previewY, this.panelX + PANEL_WIDTH - 20, this.previewY + 16, 0xFF000000);
        graphics.fill(this.panelX + 1, this.previewY + 1, this.panelX + PANEL_WIDTH - 21,
                this.previewY + 15, this.color);
        graphics.drawString(this.font, Component.translatable("pokeface.editor.tool",
                        Component.translatable("pokeface.editor." + this.tool.name().toLowerCase(Locale.ROOT))),
                this.panelX, this.previewY + 19, 0xC0C0C0, false);

        drawPreview(graphics);
    }

    private void drawGrid(GuiGraphics graphics, boolean left, Component label) {
        int size = gridSize();
        int gx = gridLeft(left);
        int gy = gridTop();
        int span = size * this.cell;
        int[] data = pixels(left);

        graphics.drawCenteredString(this.font, label, gx + span / 2, gy - 14, 0xFFFFFF);
        graphics.fill(gx - 2, gy - 2, gx + span + 2, gy + span + 2, 0xFF000000);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int px = gx + x * this.cell;
                int py = gy + y * this.cell;
                // Transparent pixels show a checkerboard: "nothing drawn here".
                graphics.fill(px, py, px + this.cell, py + this.cell,
                        (x + y) % 2 == 0 ? 0xFF3C3C3C : 0xFF303030);
                int argb = data[y * size + x];
                if ((argb >>> 24) != 0) {
                    graphics.fill(px, py, px + this.cell, py + this.cell, argb);
                }
            }
        }

        if (!eyeMode()) {
            // Guides at the rows a vanilla skin puts its eyes and mouth on.
            graphics.renderOutline(gx, gy + 3 * this.cell, span, this.cell * 2, 0x66FFDD55);
            graphics.renderOutline(gx, gy + 5 * this.cell, span, this.cell, 0x6655DDFF);
        } else if (!left && !this.profile.hasSeparateRightEye()) {
            graphics.drawCenteredString(this.font, Component.translatable("pokeface.editor.mirroring"),
                    gx + span / 2, gy + span + 6, 0x909090);
        }
    }

    private void drawPalette(GuiGraphics graphics) {
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = this.panelX + (i % 5) * SWATCH;
            int sy = this.paletteY + (i / 5) * SWATCH;
            graphics.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, 0xFF000000);
            graphics.fill(sx + 1, sy + 1, sx + SWATCH - 2, sy + SWATCH - 2, PRESETS[i]);
        }
    }

    private void drawPreview(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int scale = 40;
        int px = this.panelX + (PANEL_WIDTH - 20) / 2;
        int feetY = this.height - 16;
        graphics.fill(px - 42, feetY - Math.round(scale * 1.9F) - 8, px + 42, feetY + 6, 0x60000000);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, px, feetY, scale, 0.0F, -20.0F,
                this.minecraft.player);
    }

    private boolean paletteClicked(double mouseX, double mouseY) {
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = this.panelX + (i % 5) * SWATCH;
            int sy = this.paletteY + (i / 5) * SWATCH;
            if (mouseX >= sx && mouseX < sx + SWATCH && mouseY >= sy && mouseY < sy + SWATCH) {
                this.color = PRESETS[i];
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    private boolean paintAt(double mouseX, double mouseY, boolean startStroke) {
        int size = gridSize();
        for (int side = 0; side < (eyeMode() ? 2 : 1); side++) {
            boolean left = side == 0;
            int gx = gridLeft(left);
            int gy = gridTop();
            int cx = (int) ((mouseX - gx) / this.cell);
            int cy = (int) ((mouseY - gy) / this.cell);
            if (mouseX < gx || mouseY < gy || cx < 0 || cy < 0 || cx >= size || cy >= size) {
                continue;
            }
            if (startStroke) {
                pushUndo(left);
            }
            this.paintingLeft = left;
            int[] data = pixels(left);
            switch (this.tool) {
                case ERASER -> data[cy * size + cx] = 0;
                case FILL -> Arrays.fill(data, this.color);
                default -> data[cy * size + cx] = this.color;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (paletteClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == 1) {
            // Right click always erases, whichever tool is selected.
            Tool previous = this.tool;
            this.tool = Tool.ERASER;
            boolean painted = paintAt(mouseX, mouseY, true);
            this.tool = previous;
            if (painted) {
                this.painting = true;
                return true;
            }
        } else if (paintAt(mouseX, mouseY, true)) {
            this.painting = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.painting && this.tool != Tool.FILL) {
            Tool previous = this.tool;
            if (button == 1) {
                this.tool = Tool.ERASER;
            }
            paintAt(mouseX, mouseY, false);
            this.tool = previous;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.painting = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.cell = Mth.clamp(this.cell + (int) Math.signum(delta) * 2, 4, 40);
        return true;
    }

    @Override
    public void onClose() {
        PokeFaceClient.saveLocalProfile();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
