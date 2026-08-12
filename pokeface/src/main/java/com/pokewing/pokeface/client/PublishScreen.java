package com.pokewing.pokeface.client;

import com.pokewing.pokeface.market.MarketClient;
import com.pokewing.pokeface.market.MarketConfig;
import com.pokewing.pokeface.model.ModelLibrary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Puts one of your own models up for sale: pick the model, name the price, send
 * it to the store.
 *
 * <p>The mod hands over the files and the asking price; listing, review and
 * payouts are the store's business, which is also why no money ever moves
 * through here.
 */
public final class PublishScreen extends Screen {

    private final Screen parent;
    private EditBox endpointBox;
    private EditBox currencyBox;
    private double price = 5.0;
    private int selected;
    private String status = "";

    public PublishScreen(Screen parent) {
        super(Component.translatable("pokeface.publish.title"));
        this.parent = parent;
    }

    private List<String> models() {
        return ModelLibrary.names();
    }

    @Override
    protected void init() {
        MarketConfig config = MarketConfig.get();
        int x = this.width / 2 - 150;
        int y = 50;

        this.endpointBox = new EditBox(this.font, x, y, 300, 18,
                Component.translatable("pokeface.publish.endpoint"));
        this.endpointBox.setMaxLength(512);
        this.endpointBox.setValue(config.publishUrl);
        this.endpointBox.setHint(Component.translatable("pokeface.publish.endpoint"));
        addRenderableWidget(this.endpointBox);
        y += 26;

        List<String> models = models();
        addRenderableWidget(Button.builder(Component.literal(models.isEmpty()
                        ? Component.translatable("pokeface.publish.no_models").getString()
                        : models.get(Math.min(this.selected, models.size() - 1))),
                b -> {
                    if (!models.isEmpty()) {
                        this.selected = (this.selected + 1) % models.size();
                        rebuildWidgets();
                    }
                }).bounds(x, y, 300, 20).build());
        y += 26;

        addRenderableWidget(new AbstractSliderButton(x, y, 200, 20, Component.empty(),
                Math.min(1.0, this.price / 100.0)) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable("pokeface.publish.price").append(
                        Component.literal(": " + String.format(Locale.ROOT, "%.2f",
                                this.value * 100.0))));
            }

            @Override
            protected void applyValue() {
                PublishScreen.this.price = this.value * 100.0;
            }
        });
        this.currencyBox = new EditBox(this.font, x + 206, y + 1, 94, 18,
                Component.translatable("pokeface.publish.currency"));
        this.currencyBox.setValue("TRY");
        this.currencyBox.setMaxLength(8);
        addRenderableWidget(this.currencyBox);
        y += 30;

        addRenderableWidget(Button.builder(Component.translatable("pokeface.publish.submit"),
                b -> submit()).bounds(x, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(x + 156, y, 144, 20).build());
    }

    private void submit() {
        List<String> models = models();
        if (models.isEmpty()) {
            this.status = Component.translatable("pokeface.publish.no_models").getString();
            return;
        }
        MarketConfig config = MarketConfig.get();
        config.publishUrl = this.endpointBox.getValue().trim();
        config.save();

        String model = models.get(Math.min(this.selected, models.size() - 1));
        this.status = Component.translatable("pokeface.publish.sending", model).getString();
        MarketClient.publish(model, this.price, this.currencyBox.getValue().trim())
                .thenAccept(result -> {
                    if (this.minecraft != null) {
                        this.minecraft.execute(() -> this.status = result);
                    }
                });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (!this.status.isEmpty()) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2,
                    this.height - 40, 0xC0C0C0);
        }
        graphics.drawCenteredString(this.font, Component.translatable("pokeface.publish.note"),
                this.width / 2, this.height - 26, 0x707070);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
