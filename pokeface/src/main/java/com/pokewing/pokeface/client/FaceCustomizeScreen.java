package com.pokewing.pokeface.client;

import com.pokewing.pokeface.PokeFaceConfig;
import com.pokewing.pokeface.compat.EpicFightCompat;
import com.pokewing.pokeface.compat.VoiceChatCompat;
import com.pokewing.pokeface.face.CharacterLibrary;
import com.pokewing.pokeface.face.FaceDirector;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.face.FaceStyle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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

    /** Face preview size, in screen pixels per skin pixel. Scroll to zoom. */
    private int previewScale = 12;

    // Character library panel, laid out down the left edge.
    private static final int LIST_X = 10;
    private static final int LIST_WIDTH = 230;
    private static final int ROW_HEIGHT = 34;
    private EditBox searchBox;
    private EditBox renameBox;
    private CharacterLibrary.Character renaming;
    /** The saved character the right-hand controls are editing, if any. */
    private CharacterLibrary.Character editing;
    private int listScroll;

    public FaceCustomizeScreen(Screen parent) {
        super(Component.translatable("pokeface.menu.title"));
        this.parent = parent;
    }

    private int listTop() {
        return 62;
    }

    private int listBottom() {
        return this.height - 40;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
    }

    private java.util.List<CharacterLibrary.Character> filtered() {
        return CharacterLibrary.search(this.searchBox == null ? "" : this.searchBox.getValue());
    }

    private void clampScroll() {
        int max = Math.max(0, filtered().size() * ROW_HEIGHT - (listBottom() - listTop()));
        this.listScroll = Mth.clamp(this.listScroll, 0, max);
    }

    @Override
    protected void init() {
        this.working = PokeFaceClient.localProfile();

        // Character library: search at the top, the list below, both down the
        // left edge so the editor controls keep the right half to themselves.
        String previousQuery = this.searchBox == null ? "" : this.searchBox.getValue();
        this.searchBox = new EditBox(this.font, LIST_X, 24, LIST_WIDTH - 74, 18,
                Component.translatable("pokeface.chars.search"));
        this.searchBox.setHint(Component.translatable("pokeface.chars.search"));
        this.searchBox.setValue(previousQuery);
        this.searchBox.setResponder(v -> {
            this.listScroll = 0;
            clampScroll();
        });
        addRenderableWidget(this.searchBox);

        addRenderableWidget(Button.builder(Component.translatable("pokeface.chars.save_new"),
                b -> {
                    CharacterLibrary.Character created =
                            CharacterLibrary.create(defaultName(), this.working);
                    this.renaming = created;
                    rebuildWidgets();
                }).bounds(LIST_X + LIST_WIDTH - 70, 24, 70, 18).build());

        if (this.renaming != null) {
            String startingName = this.renaming.name;
            this.renameBox = new EditBox(this.font, LIST_X, listTop() - 20, LIST_WIDTH - 74, 18,
                    Component.translatable("pokeface.chars.rename"));
            this.renameBox.setValue(startingName);
            this.renameBox.setMaxLength(48);
            addRenderableWidget(this.renameBox);
            addRenderableWidget(Button.builder(Component.translatable("pokeface.chars.rename_ok"),
                    b -> {
                        this.renaming.name = this.renameBox.getValue().isBlank()
                                ? startingName : this.renameBox.getValue();
                        CharacterLibrary.save(this.renaming);
                        this.renaming = null;
                        rebuildWidgets();
                    }).bounds(LIST_X + LIST_WIDTH - 70, listTop() - 20, 70, 18).build());
        }

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

        // Second column: everything that is per eye rather than per face.
        int ex = Math.min(this.width - 155, x + 160);
        int ey = 40;
        ey = addSlider(ex, ey, w, h, "pokeface.menu.converge", this.working.eyeConverge, -2.0F, 2.0F,
                v -> this.working.eyeConverge = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.left_eye_x", this.working.eyeLeftOffsetX, -3.0F, 3.0F,
                v -> this.working.eyeLeftOffsetX = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.left_eye_y", this.working.eyeLeftOffsetY, -3.0F, 3.0F,
                v -> this.working.eyeLeftOffsetY = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.right_eye_x", this.working.eyeRightOffsetX, -3.0F, 3.0F,
                v -> this.working.eyeRightOffsetX = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.right_eye_y", this.working.eyeRightOffsetY, -3.0F, 3.0F,
                v -> this.working.eyeRightOffsetY = v);
        addRenderableWidget(CycleButton.onOffBuilder(this.working.mirrorRightEye)
                .create(ex, ey, w, h, Component.translatable("pokeface.menu.mirror_right"),
                        (b, v) -> this.working.mirrorRightEye = v));
        ey += 24;
        ey = addSlider(ex, ey, w, h, "pokeface.menu.iris_size", this.working.irisScale, 0.2F, 1.0F,
                v -> this.working.irisScale = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.pupil_size", this.working.pupilScale, 0.0F, 1.0F,
                v -> this.working.pupilScale = v);
        addRenderableWidget(CycleButton.onOffBuilder(this.working.drawSclera)
                .create(ex, ey, w, h, Component.translatable("pokeface.menu.sclera"),
                        (b, v) -> this.working.drawSclera = v));
        ey += 24;
        addRenderableWidget(CycleButton.onOffBuilder(this.working.drawBrows)
                .create(ex, ey, w, h, Component.translatable("pokeface.menu.brows"),
                        (b, v) -> this.working.drawBrows = v));
        ey += 24;
        ey = addSlider(ex, ey, w, h, "pokeface.menu.brow_tilt", this.working.browTilt, 0.0F, 3.0F,
                v -> this.working.browTilt = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.brow_length", this.working.browLength, 1.0F, 4.0F,
                v -> this.working.browLength = v);
        ey = addSlider(ex, ey, w, h, "pokeface.menu.brow_thickness", this.working.browThickness, 0.2F, 1.5F,
                v -> this.working.browThickness = v);
        addSlider(ex, ey, w, h, "pokeface.menu.brow_y", this.working.browOffsetY, -3.0F, 3.0F,
                v -> this.working.browOffsetY = v);

        addRenderableWidget(CycleButton.<Integer>builder(this::colorTargetLabel)
                .withValues(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8))
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

        addRenderableWidget(CycleButton.onOffBuilder(this.working.eyeGlow)
                .create(x, y, 150, h, Component.translatable("pokeface.menu.eye_glow"),
                        (b, v) -> this.working.eyeGlow = v));
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.menu.market"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new MarketScreen(this));
                    }
                }).bounds(x, y, 150, h).build());
        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.menu.attachments"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new AttachmentScreen(this, this.working));
                    }
                }).bounds(x, y, 150, h).build());
        y += 24;

        // Matching both eyes by hand is the common case, so it gets a button.
        addRenderableWidget(Button.builder(Component.translatable("pokeface.menu.match_eyes"),
                b -> this.working.mirrorEyeColors()).bounds(x, y, 150, h).build());
        y += 24;

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

    private String defaultName() {
        return Component.translatable("pokeface.chars.new_name",
                CharacterLibrary.all().size() + 1).getString();
    }

    /**
     * The character list: a face thumbnail, the name, and edit / rename / delete
     * on the right. Only the rows on screen are drawn and hit-tested, so the list
     * costs the same whether it holds five characters or a thousand.
     */
    private void renderCharacterList(GuiGraphics graphics, int mouseX, int mouseY) {
        java.util.List<CharacterLibrary.Character> characters = filtered();
        int top = listTop();
        int bottom = listBottom();
        graphics.fill(LIST_X - 4, top - 4, LIST_X + LIST_WIDTH + 4, bottom + 4, 0x70000000);

        if (characters.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("pokeface.chars.empty"),
                    LIST_X + 6, top + 6, 0x909090, false);
            return;
        }

        int first = this.listScroll / ROW_HEIGHT;
        int last = Math.min(characters.size(), first + visibleRows() + 1);
        for (int i = first; i < last; i++) {
            CharacterLibrary.Character character = characters.get(i);
            int y = top + i * ROW_HEIGHT - this.listScroll;
            if (y + ROW_HEIGHT < top || y > bottom) {
                continue;
            }
            boolean hovered = mouseX >= LIST_X && mouseX <= LIST_X + LIST_WIDTH
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(LIST_X, y, LIST_X + LIST_WIDTH, y + ROW_HEIGHT - 2,
                    hovered ? 0x50FFFFFF : 0x40000000);

            FacePreview.draw(graphics, LIST_X + 3, y + 3, 3, character.profile,
                    PokeFaceClient.director().current());

            graphics.drawString(this.font, character.name, LIST_X + 34, y + 5, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.translatable(
                            character.profile.styleEnum().translationKey()),
                    LIST_X + 34, y + 17, 0x909090, false);

            drawRowButton(graphics, rowButtonX(0), y + 6, "E", mouseX, mouseY);
            drawRowButton(graphics, rowButtonX(1), y + 6, "R", mouseX, mouseY);
            drawRowButton(graphics, rowButtonX(2), y + 6, "X", mouseX, mouseY);
        }
    }

    private int rowButtonX(int index) {
        return LIST_X + LIST_WIDTH - 20 - (2 - index) * 22;
    }

    private void drawRowButton(GuiGraphics graphics, int x, int y, String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        graphics.fill(x, y, x + 18, y + 18, hovered ? 0xFF5A5A5A : 0xFF303030);
        graphics.renderOutline(x, y, 18, 18, 0xFF101010);
        graphics.drawCenteredString(this.font, label, x + 9, y + 5, 0xFFFFFF);
    }

    /** @return true when the click landed on a row control. */
    private boolean clickCharacterList(double mouseX, double mouseY) {
        if (mouseX < LIST_X || mouseX > LIST_X + LIST_WIDTH
                || mouseY < listTop() || mouseY > listBottom()) {
            return false;
        }
        java.util.List<CharacterLibrary.Character> characters = filtered();
        int index = (int) ((mouseY - listTop() + this.listScroll) / ROW_HEIGHT);
        if (index < 0 || index >= characters.size()) {
            return false;
        }
        CharacterLibrary.Character character = characters.get(index);
        int y = listTop() + index * ROW_HEIGHT - this.listScroll + 6;
        for (int i = 0; i < 3; i++) {
            int bx = rowButtonX(i);
            if (mouseX >= bx && mouseX < bx + 18 && mouseY >= y && mouseY < y + 18) {
                switch (i) {
                    case 0 -> {
                        // Edit: wear it, so every control on the right edits it.
                        PokeFaceClient.wearProfile(character.profile);
                        this.working = PokeFaceClient.localProfile();
                        this.editing = character;
                    }
                    case 1 -> this.renaming = character;
                    default -> {
                        CharacterLibrary.delete(character);
                        if (this.editing == character) {
                            this.editing = null;
                        }
                    }
                }
                rebuildWidgets();
                return true;
            }
        }
        // Clicking the row itself also wears the character.
        PokeFaceClient.wearProfile(character.profile);
        this.working = PokeFaceClient.localProfile();
        this.editing = character;
        rebuildWidgets();
        return true;
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
        to.eyeColorRight = from.eyeColorRight;
        to.eyeLeftOffsetX = from.eyeLeftOffsetX;
        to.eyeLeftOffsetY = from.eyeLeftOffsetY;
        to.eyeRightOffsetX = from.eyeRightOffsetX;
        to.eyeRightOffsetY = from.eyeRightOffsetY;
        to.eyeConverge = from.eyeConverge;
        to.scleraColor = from.scleraColor;
        to.scleraColorRight = from.scleraColorRight;
        to.pupilColor = from.pupilColor;
        to.pupilColorRight = from.pupilColorRight;
        to.irisScale = from.irisScale;
        to.pupilScale = from.pupilScale;
        to.browTilt = from.browTilt;
        to.browLength = from.browLength;
        to.browThickness = from.browThickness;
        to.browOffsetY = from.browOffsetY;
        to.eyeGlow = from.eyeGlow;
        to.eyeGlowSpread = from.eyeGlowSpread;
        to.mouthInnerColor = from.mouthInnerColor;
        to.teethColor = from.teethColor;
    }

    private Component colorTargetLabel(int target) {
        return Component.translatable(switch (target) {
            case 1 -> "pokeface.menu.color_eye_left";
            case 2 -> "pokeface.menu.color_eye_right";
            case 3 -> "pokeface.menu.color_sclera_left";
            case 4 -> "pokeface.menu.color_sclera_right";
            case 5 -> "pokeface.menu.color_pupil_left";
            case 6 -> "pokeface.menu.color_pupil_right";
            case 7 -> "pokeface.menu.color_mouth";
            case 8 -> "pokeface.menu.color_teeth";
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
        renderCharacterList(graphics, mouseX, mouseY);

        drawSwatches(graphics, mouseX, mouseY);
        drawStatus(graphics);
    }

    /**
     * Live preview of the face being edited.
     *
     * <p>Drawn flat by {@link FacePreview} rather than by rendering the player
     * entity: under Epic Fight the entity goes through a patched renderer whose
     * armature is not posed for a menu, which is what made the preview come out
     * stretched and broken.
     */
    private void renderPreview(GuiGraphics graphics) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int size = 8 * this.previewScale;
        int px = this.width / 2 - 150 - size / 2;
        int py = this.height / 2 - size / 2;
        FacePreview.draw(graphics, px, py, this.previewScale, this.working,
                PokeFaceClient.director().current());
        graphics.drawCenteredString(this.font, Component.translatable("pokeface.menu.preview_hint"),
                px + size / 2, py + size + 6, 0x808080);
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
            case 2 -> this.working.eyeColorRight;
            case 3 -> this.working.scleraColor;
            case 4 -> this.working.scleraColorRight;
            case 5 -> this.working.pupilColor;
            case 6 -> this.working.pupilColorRight;
            case 7 -> this.working.mouthInnerColor;
            case 8 -> this.working.teethColor;
            default -> this.working.lineColor;
        };
    }

    private void setCurrentColor(int argb) {
        switch (this.colorTarget) {
            case 1 -> this.working.eyeColor = argb;
            case 2 -> this.working.eyeColorRight = argb;
            case 3 -> this.working.scleraColor = argb;
            case 4 -> this.working.scleraColorRight = argb;
            case 5 -> this.working.pupilColor = argb;
            case 6 -> this.working.pupilColorRight = argb;
            case 7 -> this.working.mouthInnerColor = argb;
            case 8 -> this.working.teethColor = argb;
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
        // Spells out "bound but nothing is arriving", which is otherwise
        // indistinguishable from a frozen face.
        graphics.drawString(this.font, Component.translatable("pokeface.status.tracker",
                PokeFaceClient.trackerStatus()), 10, y + 33, 0xAAAAAA, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < LIST_X + LIST_WIDTH) {
            // Over the character list: scroll it. Elsewhere on the left: zoom.
            this.listScroll = Math.max(0, this.listScroll - (int) Math.signum(delta) * ROW_HEIGHT);
            clampScroll();
            return true;
        }
        if (mouseX < this.width / 2.0 - 40) {
            this.previewScale = Mth.clamp(this.previewScale + (int) Math.signum(delta) * 2, 4, 32);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickCharacterList(mouseX, mouseY)) {
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
        if (this.editing != null) {
            // Edits made while a saved character is worn belong to that character.
            this.editing.profile = this.working.copy();
            CharacterLibrary.save(this.editing);
        }
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
