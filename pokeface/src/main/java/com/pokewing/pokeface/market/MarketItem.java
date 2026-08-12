package com.pokewing.pokeface.market;

/**
 * One cosmetic listing as the storefront describes it.
 *
 * <p>Prices are carried as the store's own formatted string as well as the raw
 * amount, because a store knows how to write its own currency and this mod has
 * no business guessing at symbols, decimals or tax lines.
 */
public final class MarketItem {

    public String id = "";
    public String name = "";
    public String author = "";
    public String description = "";
    /** Raw amount, for sorting only. */
    public double price;
    /** What the store says the price is, e.g. "₺49,90" or "$4.99". */
    public String priceLabel = "";
    /**
     * Where buying happens: the store's own checkout page. The mod opens it and
     * stays out of the transaction entirely.
     */
    public String checkoutUrl = "";
    /** Where the pack is fetched from once it is owned. */
    public String downloadUrl = "";
    /** File name the download is saved as, e.g. {@code foxears.zip}. */
    public String fileName = "";
    /** Set by the client after checking what is already installed. */
    public transient boolean installed;

    public String label() {
        return name + (author.isEmpty() ? "" : " §8· " + author);
    }
}
