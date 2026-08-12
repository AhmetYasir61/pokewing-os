package com.pokewing.pokeface.client;

import com.pokewing.pokeface.face.Attachment;
import com.pokewing.pokeface.face.FaceProfile;
import com.pokewing.pokeface.model.ModelLibrary;

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
 * Places OBJ attachments — ears, tails, horns — on the head or the body.
 *
 * <p>Models come from {@code config/pokeface/models/} as an {@code .obj} plus a
 * matching {@code .png}; the reload button rescans that folder so a model can be
 * dropped in without restarting. Placement is per attachment and is what gets
 * synced to other players — the files themselves stay local, like a resource
 * pack.
 */
public final class AttachmentScreen extends Screen {

    private static final int PANEL = 210;
    private static final int ROW = 22;

    private final Screen parent;
    private final FaceProfile profile;
    private int selected;

    public AttachmentScreen(Screen parent, FaceProfile profile) {
        super(Component.translatable("pokeface.attach.title"));
        this.parent = parent;
        this.profile = profile;
    }

    private List<Attachment> list() {
        return this.profile.normalisedAttachments();
    }

    private Attachment current() {
        List<Attachment> list = list();
        if (list.isEmpty()) {
            return null;
        }
        this.selected = Mth.clamp(this.selected, 0, list.size() - 1);
        return list.get(this.selected);
    }

    @Override
    protected void init() {
        int x = this.width - PANEL + 10;
        int w = PANEL - 20;
        int h = 20;
        int y = 34;

        addRenderableWidget(Button.builder(Component.translatable("pokeface.attach.reload"),
                b -> {
                    ModelLibrary.reload();
                    rebuildWidgets();
                }).bounds(x, y, w, h).build());
        y += ROW + 4;

        List<String> models = ModelLibrary.names();
        int half = (w - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.attach.add"),
                b -> {
                    if (!models.isEmpty() && list().size() < FaceProfile.MAX_ATTACHMENTS) {
                        list().add(new Attachment(models.get(0), Attachment.Anchor.HEAD));
                        this.selected = list().size() - 1;
                        rebuildWidgets();
                    }
                }).bounds(x, y, half, h).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.attach.remove"),
                b -> {
                    if (current() != null) {
                        list().remove(this.selected);
                        rebuildWidgets();
                    }
                }).bounds(x + half + 4, y, half, h).build());
        y += ROW + 4;

        Attachment attachment = current();
        if (attachment == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                    b -> onClose()).bounds(x, y, w, h).build());
            return;
        }

        // Which entry is being edited, and which model it uses.
        addRenderableWidget(Button.builder(
                Component.translatable("pokeface.attach.entry", this.selected + 1, list().size()),
                b -> {
                    this.selected = (this.selected + 1) % list().size();
                    rebuildWidgets();
                }).bounds(x, y, w, h).build());
        y += ROW + 2;

        if (!models.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal(attachment.model),
                    b -> {
                        int index = Math.max(0, models.indexOf(attachment.model));
                        attachment.model = models.get((index + 1) % models.size());
                        rebuildWidgets();
                    }).bounds(x, y, w, h).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("pokeface.attach.no_models"),
                    b -> ModelLibrary.reload()).bounds(x, y, w, h).build());
        }
        y += ROW + 2;

        addRenderableWidget(CycleButton.<Attachment.Anchor>builder(
                        a -> Component.translatable(a.translationKey()))
                .withValues(List.of(Attachment.Anchor.values()))
                .withInitialValue(attachment.anchor)
                .create(x, y, w, h, Component.translatable("pokeface.attach.anchor"),
                        (b, v) -> attachment.anchor = v));
        y += ROW + 2;

        y = slider(x, y, w, h, "pokeface.attach.x", attachment.offsetX, -16.0F, 16.0F,
                v -> attachment.offsetX = v);
        y = slider(x, y, w, h, "pokeface.attach.y", attachment.offsetY, -16.0F, 16.0F,
                v -> attachment.offsetY = v);
        y = slider(x, y, w, h, "pokeface.attach.z", attachment.offsetZ, -16.0F, 16.0F,
                v -> attachment.offsetZ = v);
        y = slider(x, y, w, h, "pokeface.attach.rx", attachment.rotateX, -180.0F, 180.0F,
                v -> attachment.rotateX = v);
        y = slider(x, y, w, h, "pokeface.attach.ry", attachment.rotateY, -180.0F, 180.0F,
                v -> attachment.rotateY = v);
        y = slider(x, y, w, h, "pokeface.attach.rz", attachment.rotateZ, -180.0F, 180.0F,
                v -> attachment.rotateZ = v);
        y = slider(x, y, w, h, "pokeface.attach.scale", attachment.scale, 0.1F, 4.0F,
                v -> attachment.scale = v);

        addRenderableWidget(CycleButton.onOffBuilder(attachment.glow)
                .create(x, y, w, h, Component.translatable("pokeface.attach.glow"),
                        (b, v) -> attachment.glow = v));
        y += ROW + 2;
        addRenderableWidget(CycleButton.onOffBuilder(attachment.visible)
                .create(x, y, w, h, Component.translatable("pokeface.attach.visible"),
                        (b, v) -> attachment.visible = v));
        y += ROW + 4;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(x, y, w, h).build());
    }

    private int slider(int x, int y, int w, int h, String key, float initial,
                       float min, float max, java.util.function.Consumer<Float> setter) {
        addRenderableWidget(new AbstractSliderButton(x, y, w, h, Component.empty(),
                (Mth.clamp(initial, min, max) - min) / (max - min)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                float v = min + (float) this.value * (max - min);
                setMessage(Component.translatable(key).append(Component.literal(
                        ": " + String.format(Locale.ROOT, "%.2f", v))));
            }

            @Override
            protected void applyValue() {
                setter.accept(min + (float) this.value * (max - min));
            }
        });
        return y + ROW;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, (this.width - PANEL) / 2, 14, 0xFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            int scale = 90;
            int cx = (this.width - PANEL) / 2;
            int feetY = this.height / 2 + Math.round(scale * 1.9F) / 2;
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, cx, feetY, scale,
                    cx - mouseX, feetY - Math.round(scale * 1.2F) - mouseY, this.minecraft.player);
        }

        graphics.drawString(this.font, Component.translatable("pokeface.attach.folder",
                ModelLibrary.directory().toString()), 10, this.height - 20, 0x909090, false);
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
