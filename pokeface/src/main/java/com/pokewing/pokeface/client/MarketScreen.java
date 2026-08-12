package com.pokewing.pokeface.client;

import com.pokewing.pokeface.market.Checkout;
import com.pokewing.pokeface.market.MarketAuth;
import com.pokewing.pokeface.market.MarketClient;
import com.pokewing.pokeface.market.MarketConfig;
import com.pokewing.pokeface.market.MarketItem;
import com.pokewing.pokeface.model.ModelLibrary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The cosmetic market: browse what a store sells, buy it on the store's page,
 * and install what you own. Creators publish their own models from the same
 * screen.
 *
 * <p>No payment field appears anywhere here by design. Buying opens the store's
 * checkout, which is the only place a card should ever be typed.
 */
public final class MarketScreen extends Screen {

    private static final int ROW_HEIGHT = 30;

    private final Screen parent;
    private final List<MarketItem> items = new ArrayList<>();
    private EditBox searchBox;
    private EditBox catalogBox;
    private String status = "";
    private int scroll;
    private boolean loading;

    public MarketScreen(Screen parent) {
        super(Component.translatable("pokeface.market.title"));
        this.parent = parent;
    }

    private int listTop() {
        return 78;
    }

    private int listBottom() {
        return this.height - 86;
    }

    @Override
    protected void init() {
        MarketConfig config = MarketConfig.get();

        this.catalogBox = new EditBox(this.font, 20, 26, this.width - 150, 18,
                Component.translatable("pokeface.market.catalog_url"));
        this.catalogBox.setMaxLength(512);
        this.catalogBox.setValue(config.catalogUrl);
        this.catalogBox.setHint(Component.translatable("pokeface.market.catalog_hint"));
        addRenderableWidget(this.catalogBox);

        addRenderableWidget(Button.builder(Component.translatable("pokeface.market.refresh"),
                b -> {
                    config.catalogUrl = this.catalogBox.getValue().trim();
                    config.save();
                    reload();
                }).bounds(this.width - 124, 26, 104, 18).build());

        this.searchBox = new EditBox(this.font, 20, 52, 200, 18,
                Component.translatable("pokeface.market.search"));
        this.searchBox.setHint(Component.translatable("pokeface.market.search"));
        this.searchBox.setResponder(v -> this.scroll = 0);
        addRenderableWidget(this.searchBox);

        // Checkout preference. The browser is the default because the player can
        // see the address bar and certificate before they pay.
        addRenderableWidget(CycleButton.onOffBuilder(config.useSystemBrowser)
                .create(230, 52, 190, 18, Component.translatable("pokeface.market.browser"),
                        (b, v) -> {
                            config.useSystemBrowser = v;
                            config.save();
                        }));
        addRenderableWidget(CycleButton.onOffBuilder(config.rememberSession)
                .create(428, 52, 190, 18, Component.translatable("pokeface.market.remember"),
                        (b, v) -> {
                            config.rememberSession = v;
                            if (!v) {
                                config.forgetSession();
                            } else {
                                config.save();
                            }
                        }));

        int bottom = this.height - 58;
        addRenderableWidget(Button.builder(Component.translatable("pokeface.market.publish"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new PublishScreen(this));
                    }
                }).bounds(20, bottom, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("pokeface.market.folder"),
                b -> net.minecraft.Util.getPlatform().openFile(ModelLibrary.directory().toFile()))
                .bounds(176, bottom, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                b -> onClose()).bounds(this.width - 170, bottom, 150, 20).build());

        if (this.items.isEmpty() && !config.catalogUrl.isBlank()) {
            reload();
        }
    }

    private void reload() {
        this.loading = true;
        this.status = "";
        // Sign in first, so anything already owned comes back as "Install" even
        // on a fresh config folder; the catalog itself does not need it.
        MarketAuth.signIn().thenCompose(ok -> MarketClient.catalog()).thenAccept(list -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    this.items.clear();
                    this.items.addAll(list);
                    this.loading = false;
                    this.status = list.isEmpty()
                            ? Component.translatable("pokeface.market.empty").getString()
                            : "";
                });
            }
        });
    }

    private List<MarketItem> filtered() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        if (query.isEmpty()) {
            return this.items;
        }
        List<MarketItem> out = new ArrayList<>();
        for (MarketItem item : this.items) {
            if (item.name.toLowerCase(Locale.ROOT).contains(query)
                    || item.author.toLowerCase(Locale.ROOT).contains(query)) {
                out.add(item);
            }
        }
        return out;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int top = listTop();
        int bottom = listBottom();
        graphics.fill(16, top - 4, this.width - 16, bottom + 4, 0x70000000);

        List<MarketItem> list = filtered();
        if (this.loading) {
            graphics.drawString(this.font, Component.translatable("pokeface.market.loading"),
                    24, top + 6, 0xC0C0C0, false);
        } else if (list.isEmpty()) {
            graphics.drawString(this.font, this.status.isEmpty()
                            ? Component.translatable("pokeface.market.empty").getString() : this.status,
                    24, top + 6, 0x909090, false);
        }

        for (int i = 0; i < list.size(); i++) {
            int y = top + i * ROW_HEIGHT - this.scroll;
            if (y + ROW_HEIGHT < top || y > bottom) {
                continue;
            }
            MarketItem item = list.get(i);
            boolean hovered = mouseY >= y && mouseY < y + ROW_HEIGHT - 2
                    && mouseX >= 20 && mouseX <= this.width - 20;
            graphics.fill(20, y, this.width - 20, y + ROW_HEIGHT - 2, hovered ? 0x50FFFFFF : 0x40000000);
            graphics.drawString(this.font, item.label(), 28, y + 5, 0xFFFFFF, false);
            graphics.drawString(this.font, item.description, 28, y + 16, 0x909090, false);

            String price = item.priceLabel.isEmpty()
                    ? String.format(Locale.ROOT, "%.2f", item.price) : item.priceLabel;
            graphics.drawString(this.font, price, this.width - 210, y + 10, 0xFFD860, false);

            String action = item.installed
                    ? Component.translatable("pokeface.market.installed").getString()
                    : (item.downloadUrl.isEmpty()
                        ? Component.translatable("pokeface.market.buy").getString()
                        : Component.translatable("pokeface.market.install").getString());
            int bx = this.width - 130;
            graphics.fill(bx, y + 5, bx + 100, y + 23, item.installed ? 0xFF303030 : 0xFF2E6B3A);
            graphics.drawCenteredString(this.font, action, bx + 50, y + 10, 0xFFFFFF);
        }

        if (!this.status.isEmpty() && !this.loading) {
            graphics.drawString(this.font, this.status, 20, this.height - 32, 0xC0C0C0, false);
        }
        graphics.drawString(this.font, Component.translatable("pokeface.market.no_card"),
                20, this.height - 20, 0x707070, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<MarketItem> list = filtered();
        int top = listTop();
        for (int i = 0; i < list.size(); i++) {
            int y = top + i * ROW_HEIGHT - this.scroll;
            int bx = this.width - 130;
            if (mouseX >= bx && mouseX <= bx + 100 && mouseY >= y + 5 && mouseY <= y + 23) {
                act(list.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void act(MarketItem item) {
        if (item.installed) {
            return;
        }
        if (item.downloadUrl.isEmpty()) {
            // Not owned yet: the store's page takes it from here.
            Checkout.open(this, item.checkoutUrl);
            return;
        }
        this.status = Component.translatable("pokeface.market.installing", item.name).getString();
        MarketClient.install(item).thenAccept(ok -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    if (ok) {
                        ModelLibrary.reload();
                        item.installed = true;
                        this.status = Component.translatable("pokeface.market.installed_ok",
                                item.name).getString();
                    } else {
                        this.status = Component.translatable("pokeface.market.install_failed").getString();
                    }
                });
            }
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, filtered().size() * ROW_HEIGHT - (listBottom() - listTop()));
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(delta) * ROW_HEIGHT, 0, max);
        return true;
    }

    @Override
    public void onClose() {
        MarketConfig.get().save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
