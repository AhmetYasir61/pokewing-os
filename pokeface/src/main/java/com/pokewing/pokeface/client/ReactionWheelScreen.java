package com.pokewing.pokeface.client;

import com.pokewing.pokeface.face.Expression;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Radial reaction picker (default key R). Hold the key, point at a wedge, release
 * or click to play that reaction on your character for a few seconds.
 */
public final class ReactionWheelScreen extends Screen {

    private static final float RADIUS = 70.0F;
    private static final float INNER = 26.0F;

    private int hovered = -1;

    public ReactionWheelScreen() {
        super(Component.translatable("pokeface.wheel.title"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float cx = this.width / 2.0F;
        float cy = this.height / 2.0F;
        Expression[] values = Expression.values();

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        this.hovered = -1;
        if (distance > INNER && distance < RADIUS * 1.6D) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
            if (angle < 0.0D) {
                angle += 360.0D;
            }
            this.hovered = (int) (angle / (360.0D / values.length)) % values.length;
        }

        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        for (int i = 0; i < values.length; i++) {
            double angle = Math.toRadians(i * (360.0 / values.length) - 90.0);
            int lx = (int) (cx + Math.cos(angle) * RADIUS);
            int ly = (int) (cy + Math.sin(angle) * RADIUS);
            boolean active = i == this.hovered;
            int box = active ? 0xCC3A7BD5 : 0x99202020;
            graphics.fill(lx - 30, ly - 10, lx + 30, ly + 10, box);
            graphics.drawCenteredString(this.font,
                    Component.translatable(values[i].translationKey()), lx, ly - 4,
                    active ? 0xFFFFFF : 0xC0C0C0);
        }

        graphics.drawCenteredString(this.font, this.title, (int) cx, (int) (cy - 4), 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void confirm() {
        if (this.hovered >= 0) {
            PokeFaceClient.pushManualReaction(Expression.values()[
                    Mth.clamp(this.hovered, 0, Expression.values().length - 1)]);
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        confirm();
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (PokeFaceClient.KEY_WHEEL.matches(keyCode, scanCode)) {
            confirm();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
