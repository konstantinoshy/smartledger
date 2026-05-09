package com.smartledger.api;

import com.smartledger.models.CryptoPrice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simulated crypto price provider.
 * Αντί για live CoinGecko API, παρέχει ρεαλιστικές τιμές
 * με μικρό random jitter κάθε φορά που καλείται.
 * Ιδανικό για ακαδημαϊκό demo χωρίς internet dependency.
 */
public class CryptoPriceSimulator {

    private static CryptoPriceSimulator instance;
    private final Random random = new Random();

    // Base prices (ρεαλιστικές τιμές αναφοράς)
    private final Map<String, Double> basePrices = new HashMap<>();
    private final Map<String, String> coinNames = new HashMap<>();

    private CryptoPriceSimulator() {
        basePrices.put("BTC", 64210.00);
        basePrices.put("ETH", 3180.00);
        basePrices.put("SOL", 145.50);
        basePrices.put("ADA", 0.45);
        basePrices.put("DOT", 7.20);

        coinNames.put("BTC", "Bitcoin");
        coinNames.put("ETH", "Ethereum");
        coinNames.put("SOL", "Solana");
        coinNames.put("ADA", "Cardano");
        coinNames.put("DOT", "Polkadot");
    }

    public static CryptoPriceSimulator getInstance() {
        if (instance == null) {
            instance = new CryptoPriceSimulator();
        }
        return instance;
    }

    /**
     * Επιστρέφει simulated τιμή για ένα symbol.
     * Προσθέτει ±3% random jitter στο base price.
     */
    public CryptoPrice getPrice(String symbol) {
        Double base = basePrices.get(symbol);
        String name = coinNames.get(symbol);
        if (base == null || name == null) {
            return new CryptoPrice(symbol, symbol, 0, 0);
        }

        double jitter = 1.0 + (random.nextDouble() * 0.06 - 0.03); // ±3%
        double price = base * jitter;
        double change = (random.nextDouble() * 10.0 - 4.0); // -4% to +6% bias positive

        return new CryptoPrice(symbol, name, Math.round(price * 100.0) / 100.0, Math.round(change * 10.0) / 10.0);
    }

    /**
     * Επιστρέφει simulated τιμές για όλα τα supported coins.
     */
    public List<CryptoPrice> getAllPrices() {
        List<CryptoPrice> prices = new ArrayList<>();
        for (String symbol : new String[]{"BTC", "ETH", "SOL", "ADA", "DOT"}) {
            prices.add(getPrice(symbol));
        }
        return prices;
    }

    /**
     * Επιστρέφει τα supported symbols.
     */
    public String[] getSupportedSymbols() {
        return new String[]{"BTC", "ETH", "SOL", "ADA", "DOT"};
    }

    /**
     * Επιστρέφει τα ονόματα coins.
     */
    public String getNameForSymbol(String symbol) {
        String name = coinNames.get(symbol);
        return name != null ? name : symbol;
    }
}
