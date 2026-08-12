package com.pokewing.pokeface.market;

import com.pokewing.pokeface.PokeFace;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Sends the player to the store's checkout page.
 *
 * <p>This is the whole of the mod's involvement in a payment. The card is typed
 * into the store's own page — Tebex or whatever the catalog points at — under
 * that provider's PCI compliance, and the mod never sees, transports or stores a
 * single payment field. There is deliberately no in-mod card form to add later.
 *
 * <p>The system browser is the default because the player can see the address
 * bar and the certificate there, which is exactly what you want them checking
 * before they pay. The in-game option renders the same page through MCEF when it
 * is installed, for players who would rather not alt-tab; it is opt-in, and it
 * falls back to the browser when MCEF is absent.
 */
public final class Checkout {

    private Checkout() {
    }

    public static void open(Screen parent, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        MarketConfig config = MarketConfig.get();
        if (!config.useSystemBrowser && openInGame(url)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Vanilla's confirm screen shows the full URL before anything opens, so
        // the player always sees where they are being sent to pay.
        mc.setScreen(new ConfirmLinkScreen(ok -> {
            if (ok) {
                Util.getPlatform().openUri(url);
            }
            mc.setScreen(parent);
        }, url, true));
    }

    /** @return true when MCEF took the page; false to fall back to the browser */
    private static boolean openInGame(String url) {
        if (ModList.get() == null || !ModList.get().isLoaded("mcef")) {
            PokeFace.LOGGER.info("PokeFace: MCEF not installed - checkout opens in the browser.");
            return false;
        }
        try {
            Class<?> screenClass = Class.forName("com.pokewing.pokeface.market.McefCheckoutScreen");
            Method open = screenClass.getMethod("open", String.class);
            open.invoke(null, url);
            return true;
        } catch (Throwable t) {
            PokeFace.LOGGER.warn("PokeFace: in-game checkout unavailable ({}), using the browser.",
                    t.toString());
            return false;
        }
    }
}
