package com.pokewing.pokeface.client;

import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.compat.EpicFightCompat;
import com.pokewing.pokeface.compat.VoiceChatCompat;
import com.pokewing.pokeface.face.FaceDirector;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceStyle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

/**
 * The in-game face menu: pick a style, nudge the eyes and mouth onto whatever
 * head texture the player wears, choose the mouth/teeth/line colours, and switch
 * the camera and voice-chat drivers on or off. Everything writes straight into
 * the local {@link FaceProfile} so the preview on the left updates live.
 */
public final class FaceCustomizeScreen extends Screen {

    private static final int[] SWATCHES = {
            0xFF101010, 0xFF7A1F28, 0xFFF2EAD8, 0xFFFFFFFF, 0xFF3A3A3A,
            0xFF2E5FA3, 0xFF3E8E41, 0xFF9B59B6, 0xFFD35400, 0xFFC0392B,
    };

    private final Screen parent;
    private FaceProfile working;
    private int colorTarget;   // 0 line, 1 eye, 2 mouth inner, 3 teeth
    private int scroll;

    /** Preview camera. Zoom is scroll-wheel driven so any GUI scale is usable. */
    private int previewScale = 60;
    private float previewYaw;
    private float previewPitch;
    private boolean draggingPreview;

    public FaceCustomizeScreen(Screen parent) {
        super(Component.translatable("pokeface.menu.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.working = PokeFaceClient.localProfile();
        int x = this.width / 2 - 20;
        int y = 40;
        int w = 150;
        int h = 20;

        addRenderableWidget(CycleButton.<FaceStyle>builder(s -> Component.translatable(s.translationKey()))
                .withValues(List.of(FaceStyle.values()))
                .withInitialValue(this.working.styleEnum())
                .create(x, y, w, h, Component.translatable("pokeface.menu.style"),
                        (button, value) -> this.working.style = value.key()));
        y += 24;

        y = addSlider(x, y, w, h, "pokeface.menu.eye_x", this.working.eyeOffsetX, -3.0F, 3.0F,
                v -> this.working.eyeOffsetX = v);
        y = addSlider(x, y, w, h, "pokeface.menu.eye_y", this.working.eyeOffsetY, -3.0F, 3.0F,
                v -> this.working.eyeOffsetY = v);
        y = addSlider(x, y, w, h, "pokeface.menu.eye_spacing", this.working.eyeSpacing, 1.0F, 5.0F,
                v -> this.working.eyeSpacing = v);
        y = addSlider(x, y, w, h, "pokeface.menu.eye_scale", this.working.eyeScale, 0.4F, 2.0F,
                v -> this.working.eyeScale = v);
        y = addSlider(x, y, w, h, "pokeface.menu.mouth_x", this.working.mouthOffsetX, -3.0F, 3.0F,
                v -> this.working.mouthOffsetX = v);
        y = addSlider(x, y, w, h, "pokeface.menu.mouth_y", this.working.mouthOffsetY, -3.0F, 3.0F,
                v -> this.working.mouthOffsetY = v);
        y = addSlider(x, y, w, h, "pokeface.menu.mouth_scale", this.working.mouthScale, 0.4F, 2.0F,
                v -> this.working.mouthScale = v);

        addRenderableWidget(CycleButton.<Integer>builder(this::colorTargetLabel)
                .withValues(List.of(0, 1, 2, 3))
                .withInitialValue(this.colorTarget)
                .create(x, y, w, h, Component.translatable("pokeface.menu.color_target"),
                        (button, value) -> this.colorTarget = value));
        y += 24;
        this.scroll = y + 22;   // swatch row is drawn manually below the widgets

        y = this.scroll + 4;
        addRenderableWidget(CycleButton.onOffBuilder(this.working.useSkinHead)
                .create(x, y, w, h, Component.translatable("pokeface.menu.use_skin_head"),
                        (b, v) -> this.working.useSkinHead = v));
        y += 24;
        addRenderableWidget(CycleButton.onOffBuilder(this.working.useTracker && PokeFaceConfig.trackerEnabled())
                .create(x, y, w, h, Component.translatable("pokeface.menu.use_camera"),
                        (b, v) -> {
                            this.working.useTracker = v;
                            PokeFaceConfig.setTrackerEnabled(v);
                            PokeFaceClient.restartTracker();
                        }));
        y += 24;
        addRenderableWidget(CycleButton.onOffBuilder(this.working.voiceChatMouth)
                .create(x, y, w, h, Component.translatable("pokeface.menu.voice_mouth"),
                        (b, v) -> this.working.voiceChatMouth = v));
        y += 28;

        addRenderableWidget(Button.builder(Component.translatable("pokeface.menu.pixel_editor"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new FacePixelEditorScreen(this, this.working));
                    }
                }).bounds(x, y, 150, h).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("pokeface.menu.reset"),
                b -> {
                    FaceProfile fresh = new FaceProfile();
                    fresh.style = this.working.style;
                    copyInto(fresh, this.working);
                    rebuildWidgets();
                }).bounds(x, y, 72, h).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(x + 78, y, 72, h).build());
    }

    private static void copyInto(FaceProfile from, FaceProfile to) {
        to.eyeOffsetX = from.eyeOffsetX;
        to.eyeOffsetY = from.eyeOffsetY;
        to.eyeSpacing = from.eyeSpacing;
        to.eyeScale = from.eyeScale;
        to.mouthOffsetX = from.mouthOffsetX;
        to.mouthOffsetY = from.mouthOffsetY;
        to.mouthScale = from.mouthScale;
        to.lineColor = from.lineColor;
        to.eyeColor = from.eyeColor;
        to.mouthInnerColor = from.mouthInnerColor;
        to.teethColor = from.teethColor;
    }

    private Component colorTargetLabel(int target) {
        return Component.translatable(switch (target) {
            case 1 -> "pokeface.menu.color_eye";
            case 2 -> "pokeface.menu.color_mouth";
            case 3 -> "pokeface.menu.color_teeth";
            default -> "pokeface.menu.color_line";
        });
    }

    private int addSlider(int x, int y, int w, int h, String key, float initial,
                          float min, float max, java.util.function.Consumer<Float> setter) {
        addRenderableWidget(new AbstractSliderButton(x, y, w, h, Component.empty(),
                (initial - min) / (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                float v = min + (float) this.value * (max - min);
                setMessage(Component.translatable(key)
                        .append(Component.literal(": " + String.format(Locale.ROOT, "%.2f", v))));
            }

            @Override
            protected void applyValue() {
                setter.accept(min + (float) this.value * (max - min));
            }
        });
        return y + 22;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);

        renderPreview(graphics);

        drawSwatches(graphics, mouseX, mouseY);
        drawStatus(graphics);
    }

    /**
     * Live preview of your own character. Scroll to zoom and drag to spin, so the
     * face stays readable at any GUI scale instead of being a few pixels tall.
     */
    private void renderPreview(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        // renderEntityInInventory scales by entity height, so a player ends up
        // about 1.9 * scale pixels tall. The panel and the feet position are
        // derived from that instead of from the scale directly, otherwise the
        // model grows out of its box and drifts off centre as you zoom.
        int modelHeight = Math.round(this.previewScale * 1.9F);
        int panelWidth = Math.max(90, Math.round(this.previewScale * 1.6F));
        int centerX = Math.max(panelWidth / 2 + 8, this.width / 2 - 170);
        int centerY = this.height / 2 - 10;
        int top = centerY - modelHeight / 2;
        int feetY = top + modelHeight;

        graphics.fill(centerX - panelWidth / 2, top - 8,
                centerX + panelWidth / 2, feetY + 8, 0x60000000);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, centerX, feetY,
                this.previewScale, this.previewYaw, this.previewPitch, this.minecraft.player);
        graphics.drawCenteredString(this.font, Component.translatable("pokeface.menu.preview_hint"),
                centerX, feetY + 14, 0x808080);
    }

    private void drawSwatches(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.width / 2 - 20;
        int y = this.scroll - 18;
        for (int i = 0; i < SWATCHES.length; i++) {
            int sx = x + i * 15;
            graphics.fill(sx, y, sx + 13, y + 13, 0xFF000000);
            graphics.fill(sx + 1, y + 1, sx + 12, y + 12, SWATCHES[i]);
            if (SWATCHES[i] == currentColor()) {
                graphics.renderOutline(sx - 1, y - 1, 15, 15, 0xFFFFFFFF);
            }
        }
    }

    private int currentColor() {
        return switch (this.colorTarget) {
            case 1 -> this.working.eyeColor;
            case 2 -> this.working.mouthInnerColor;
            case 3 -> this.working.teethColor;
            default -> this.working.lineColor;
        };
    }

    private void setCurrentColor(int argb) {
        switch (this.colorTarget) {
            case 1 -> this.working.eyeColor = argb;
            case 2 -> this.working.mouthInnerColor = argb;
            case 3 -> this.working.teethColor = argb;
            default -> this.working.lineColor = argb;
        }
    }

    private void drawStatus(GuiGraphics graphics) {
        FaceDirector.Source source = PokeFaceClient.lastSource();
        String sourceKey = switch (source) {
            case TRACKER -> "pokeface.status.camera";
            case REACTION -> "pokeface.status.reaction";
            default -> "pokeface.status.idle";
        };
        int y = this.height - 42;
        graphics.drawString(this.font, Component.translatable(sourceKey), 10, y, 0xA0FFA0, false);
        graphics.drawString(this.font, Component.translatable("pokeface.status.epicfight",
                EpicFightCompat.isLoaded() ? "OK" : "-"), 10, y + 11, 0xAAAAAA, false);
        graphics.drawString(this.font, Component.translatable("pokeface.status.voicechat",
                VoiceChatCompat.isLoaded() ? "OK" : "-"), 10, y + 22, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < this.width / 2.0 - 40) {
            this.previewScale = Mth.clamp(this.previewScale + (int) Math.signum(delta) * 6, 20, 160);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingPreview) {
            // These are the cursor-offset components the vanilla helper takes, so
            // they move one-for-one with the drag rather than in tiny fractions.
            this.previewYaw = Mth.clamp(this.previewYaw - (float) dragX, -120.0F, 120.0F);
            this.previewPitch = Mth.clamp(this.previewPitch - (float) dragY, -60.0F, 60.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingPreview = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < this.width / 2.0 - 40) {
            this.draggingPreview = true;
            return true;
        }
        int x = this.width / 2 - 20;
        int y = this.scroll - 18;
        if (mouseY >= y && mouseY <= y + 13) {
            int index = (int) ((mouseX - x) / 15);
            if (index >= 0 && index < SWATCHES.length && mouseX >= x) {
                setCurrentColor(SWATCHES[Mth.clamp(index, 0, SWATCHES.length - 1)]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
