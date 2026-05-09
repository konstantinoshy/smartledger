package com.smartledger.models;

/**
 * Simulated crypto price data.
 * Αντί για live API (CoinGecko), χρησιμοποιούμε τοπικές
 * simulated τιμές με random jitter για demo.
 */
public class CryptoPrice {
    private final String symbol;
    private final String name;
    private final double currentPrice;
    private final double change24h;

    public CryptoPrice(String symbol, String name, double currentPrice, double change24h) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.change24h = change24h;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getChange24h() { return change24h; }
}
