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
import java.util.Deque;
import java.util.Locale;

/**
 * Pixel editor for the 8x8 face patch that is painted over the head.
 *
 * <p>Two canvases share the screen. The <b>face</b> canvas is how a player
 * removes the eyes their skin already has: paint skin tone over them so only the
 * animated eyes remain. The <b>eye</b> canvas replaces the built-in eye sprite
 * with a hand-drawn one, at a resolution the player picks (4x4 to 32x32) —
 * resolution is detail only, the sprite is stretched to whatever the eye size
 * slider asks for.
 *
 * <p>Painting is per pixel with a brush, an eraser (back to transparent), a
 * bucket fill and undo, and the colour is mixed with RGB sliders so any skin
 * tone can be matched exactly.
 */
public final class FacePixelEditorScreen extends Screen {

    private static final int GRID = FaceProfile.FACE_SIZE;
    private static final int[] PRESETS = {
            0xFFF9DFC8, 0xFFEDC5A4, 0xFFD2A278, 0xFFB07B54, 0xFF8D5524,
            0xFF6B4423, 0xFF3D2314, 0xFFFFFFFF, 0xFF9A9A9A, 0xFF101010,
    };

    private final Screen parent;
    private final FaceProfile profile;
    private final Deque<int[]> undoStack = new ArrayDeque<>();

    private int cell = 20;
    private int color = 0xFFEDC5A4;
    private Tool tool = Tool.BRUSH;
    private boolean painting;
    private Canvas canvas = Canvas.FACE;

    private enum Tool {
        BRUSH, ERASER, FILL
    }

    private enum Canvas {
        FACE, EYE
    }

    /** Edge length of whichever canvas is being edited. */
    private int gridSize() {
        return this.canvas == Canvas.EYE ? Math.max(1, this.profile.eyeArtSize) : GRID;
    }

    private int[] pixels() {
        return this.canvas == Canvas.EYE ? this.profile.eyeArt : this.profile.normalisedPixels();
    }

    private int pixel(int x, int y) {
        return pixels()[y * gridSize() + x];
    }

    private void setPixel(int x, int y, int argb) {
        pixels()[y * gridSize() + x] = argb;
    }

    public FacePixelEditorScreen(Screen parent, FaceProfile profile) {
        super(Component.translatable("pokeface.editor.title"));
        this.parent = parent;
        this.profile = profile;
    }

    private int gridLeft() {
        return this.width / 2 - (gridSize() * this.cell) / 2 - 90;
    }

    private int gridTop() {
        return this.height / 2 - (gridSize() * this.cell) / 2;
    }

    /** Keeps the whole canvas on screen whichever resolution is selected. */
    private void fitCell() {
        int budget = Math.min(this.width / 2 - 40, this.height - 90);
        this.cell = Mth.clamp(budget / Math.max(1, gridSize()), 4, 40);
    }

    @Override
    protected void init() {
        if (this.canvas == Canvas.EYE && !this.profile.hasEyeArt()) {
            this.profile.resizeEyeArt(8);
        }
        fitCell();

        int x = this.width / 2 + 60;
        int y = 40;
        int w = 130;
        int h = 20;

        addRenderableWidget(Button.builder(
                Component.translatable(this.canvas == Canvas.EYE
                        ? "pokeface.editor.canvas_eye" : "pokeface.editor.canvas_face"),
                b -> {
                    this.canvas = this.canvas == Canvas.FACE ? Canvas.EYE : Canvas.FACE;
                    this.undoStack.clear();
                    rebuildWidgets();
                }).bounds(x, y, w, h).build());
        y += 26;

        if (this.canvas == Canvas.EYE) {
            int bw = (w - 9) / 4;
            for (int i = 0; i < FaceProfile.EYE_ART_SIZES.length; i++) {
                int size = FaceProfile.EYE_ART_SIZES[i];
                addRenderableWidget(Button.builder(Component.literal(size + ""),
                        b -> {
                            this.profile.resizeEyeArt(size);
                            this.undoStack.clear();
                            rebuildWidgets();
                        }).bounds(x + i * (bw + 3), y, bw, h).build());
            }
            y += 26;
        }

        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.brush"),
                b -> this.tool = Tool.BRUSH).bounds(x, y, 42, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.eraser"),
                b -> this.tool = Tool.ERASER).bounds(x + 44, y, 42, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.fill"),
                b -> this.tool = Tool.FILL).bounds(x + 88, y, 42, h).build());
        y += 26;

        y = addChannel(x, y, w, h, "pokeface.editor.red", 16);
        y = addChannel(x, y, w, h, "pokeface.editor.green", 8);
        y = addChannel(x, y, w, h, "pokeface.editor.blue", 0);
        y += 34;   // room for the preset swatches drawn below the sliders

        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.undo"),
                b -> undo()).bounds(x, y, 63, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.clear"),
                b -> {
                    pushUndo();
                    java.util.Arrays.fill(pixels(), 0);
                }).bounds(x + 67, y, 63, h).build());
        y += 24;
        if (this.canvas == Canvas.FACE) {
            addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.cover_eyes"),
                    b -> coverSkinEyes()).bounds(x, y, w, h).build());
        } else {
            // Dropping back to the built-in sprite is a first-class action: an
            // empty canvas would otherwise render as no eyes at all.
            addRenderableWidget(Button.builder(Component.translatable("pokeface.editor.use_builtin"),
                    b -> {
                        this.profile.eyeArtSize = 0;
                        this.profile.eyeArt = new int[0];
                        this.canvas = Canvas.FACE;
                        rebuildWidgets();
                    }).bounds(x, y, w, h).build());
        }
        y += 24;
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
                        ": " + String.format(Locale.ROOT, "%d", (int) Math.round(this.value * 255.0D)))));
            }

            @Override
            protected void applyValue() {
                int v = (int) Math.round(this.value * 255.0D) & 0xFF;
                FacePixelEditorScreen.this.color =
                        (FacePixelEditorScreen.this.color & ~(0xFF << shift)) | (v << shift) | 0xFF000000;
            }
        });
        return y + 22;
    }

    /**
     * One-click version of the whole point of this screen: fills the two rows a
     * vanilla-proportioned skin puts its eyes on with the current colour.
     */
    private void coverSkinEyes() {
        pushUndo();
        for (int y = 3; y <= 4; y++) {
            for (int x = 0; x < GRID; x++) {
                this.profile.setPixel(x, y, this.color);
            }
        }
    }

    private void pushUndo() {
        this.undoStack.push(pixels().clone());
        if (this.undoStack.size() > 32) {
            this.undoStack.removeLast();
        }
    }

    private void undo() {
        int[] previous = this.undoStack.poll();
        int[] target = pixels();
        if (previous != null && previous.length == target.length) {
            System.arraycopy(previous, 0, target, 0, previous.length);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        int size = gridSize();
        int left = gridLeft();
        int top = gridTop();
        graphics.fill(left - 2, top - 2, left + size * this.cell + 2, top + size * this.cell + 2, 0xFF000000);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int px = left + x * this.cell;
                int py = top + y * this.cell;
                int argb = pixel(x, y);
                // Transparent pixels show a checkerboard: "the skin shows here".
                int background = ((x + y) % 2 == 0) ? 0xFF3C3C3C : 0xFF303030;
                graphics.fill(px, py, px + this.cell, py + this.cell, background);
                if ((argb >>> 24) != 0) {
                    graphics.fill(px, py, px + this.cell, py + this.cell, argb);
                }
            }
        }
        if (this.canvas == Canvas.FACE) {
            // Guide lines at the rows a vanilla skin puts eyes and mouth on.
            int eyeRow = top + 3 * this.cell;
            graphics.renderOutline(left, eyeRow, size * this.cell, this.cell * 2, 0x66FFDD55);
            int mouthRow = top + 5 * this.cell;
            graphics.renderOutline(left, mouthRow, size * this.cell, this.cell, 0x6655DDFF);
        }

        drawPresets(graphics);

        int x = this.width / 2 + 60;
        graphics.fill(x, 150, x + 130, 172, 0xFF000000);
        graphics.fill(x + 1, 151, x + 129, 171, this.color);
        graphics.drawString(this.font, Component.translatable("pokeface.editor.tool",
                Component.translatable("pokeface.editor." + this.tool.name().toLowerCase(Locale.ROOT))),
                x, 176, 0xC0C0C0, false);

        // Live preview so the effect on the head is visible while painting.
        if (this.minecraft != null && this.minecraft.player != null) {
            int scale = 45;
            int px = this.width / 2 + 125;
            int feetY = this.height - 20;
            graphics.fill(px - 40, feetY - Math.round(scale * 1.9F) - 8, px + 40, feetY + 6, 0x60000000);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, px, feetY, scale,
                    0.0F, -20.0F, this.minecraft.player);
        }
    }

    private void drawPresets(GuiGraphics graphics) {
        int x = this.width / 2 + 60;
        int y = 118;
        for (int i = 0; i < PRESETS.length; i++) {
            int sx = x + (i % 10) * 13;
            graphics.fill(sx, y, sx + 12, y + 12, 0xFF000000);
            graphics.fill(sx + 1, y + 1, sx + 11, y + 11, PRESETS[i]);
        }
    }

    private boolean presetClicked(double mouseX, double mouseY) {
        int x = this.width / 2 + 60;
        int y = 118;
        if (mouseY < y || mouseY > y + 12) {
            return false;
        }
        int index = (int) ((mouseX - x) / 13);
        if (index < 0 || index >= PRESETS.length || mouseX < x) {
            return false;
        }
        this.color = PRESETS[index];
        rebuildWidgets();
        return true;
    }

    private boolean paintAt(double mouseX, double mouseY, boolean startStroke) {
        int left = gridLeft();
        int top = gridTop();
        int gx = (int) ((mouseX - left) / this.cell);
        int gy = (int) ((mouseY - top) / this.cell);
        if (gx < 0 || gy < 0 || gx >= gridSize() || gy >= gridSize()) {
            return false;
        }
        if (startStroke) {
            pushUndo();
        }
        switch (this.tool) {
            case ERASER -> setPixel(gx, gy, 0);
            case FILL -> java.util.Arrays.fill(pixels(), this.color);
            default -> setPixel(gx, gy, this.color);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (presetClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == 1) {
            // Right click always erases, whichever tool is selected.
            Tool previous = this.tool;
            this.tool = Tool.ERASER;
            boolean painted = paintAt(mouseX, mouseY, true);
            this.tool = previous;
            if (painted) {
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
            paintAt(mouseX, mouseY, false);
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
        this.cell = Mth.clamp(this.cell + (int) Math.signum(delta) * 2, 8, 40);
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
