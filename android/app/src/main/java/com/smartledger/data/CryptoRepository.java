package com.smartledger.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.smartledger.api.ApiClient;
import com.smartledger.api.CryptoPriceSimulator;
import com.smartledger.api.SessionManager;
import com.smartledger.api.SmartLedgerApi;
import com.smartledger.api.dto.PortfolioAssetDto;
import com.smartledger.api.dto.PortfolioTransactionDto;
import com.smartledger.data.local.AppDatabase;
import com.smartledger.data.local.PortfolioAssetDao;
import com.smartledger.models.CryptoPrice;
import com.smartledger.models.PortfolioAsset;
import com.smartledger.models.PortfolioTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CryptoRepository {

    public interface AssetListCallback {
        void onSuccess(List<PortfolioAsset> assets);
        void onError(String message);
    }

    public interface TransactionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface TransactionListCallback {
        void onSuccess(List<PortfolioTransaction> transactions);
        void onError(String message);
    }

    private static CryptoRepository instance;
    private final SessionManager sessionManager;
    private final SmartLedgerApi api;
    private final PortfolioAssetDao assetDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private CryptoRepository(Context context) {
        sessionManager = new SessionManager(context);
        api = ApiClient.getApi(sessionManager);
        assetDao = AppDatabase.getDatabase(context).portfolioAssetDao();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static CryptoRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CryptoRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getPortfolio(AssetListCallback callback) {
        api.getPortfolioAssets("*", "symbol.asc").enqueue(new Callback<List<PortfolioAssetDto>>() {
            @Override
            public void onResponse(Call<List<PortfolioAssetDto>> call, Response<List<PortfolioAssetDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    loadAssetsLocal(callback); return;
                }
                List<PortfolioAsset> assets = new ArrayList<>();
                for (PortfolioAssetDto dto : resp.body()) assets.add(toAsset(dto));
                enrichWithPrices(assets);
                executor.execute(() -> { assetDao.clearAll(); assetDao.insertAll(assets); });
                callback.onSuccess(assets);
            }
            @Override
            public void onFailure(Call<List<PortfolioAssetDto>> call, Throwable t) {
                loadAssetsLocal(callback);
            }
        });
    }

    private void loadAssetsLocal(AssetListCallback callback) {
        executor.execute(() -> {
            List<PortfolioAsset> local = assetDao.getAllAssets();
            enrichWithPrices(local);
            mainHandler.post(() -> {
                if (!local.isEmpty()) {
                    callback.onError("Offline. Showing cached portfolio.");
                    callback.onSuccess(local);
                } else {
                    callback.onError("No portfolio data found.");
                }
            });
        });
    }

    /** Buy: upsert asset, log transaction */
    public void buyAsset(String symbol, double quantity, double price, TransactionCallback callback) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) { callback.onError("Not authenticated."); return; }
        String name = CryptoPriceSimulator.getInstance().getNameForSymbol(symbol);

        // 1. Log transaction
        Map<String, Object> txBody = new HashMap<>();
        txBody.put("user_id", userId);
        txBody.put("symbol", symbol);
        txBody.put("action", "BUY");
        txBody.put("quantity", quantity);
        txBody.put("price", price);

        api.createPortfolioTransaction(txBody).enqueue(new Callback<List<PortfolioTransactionDto>>() {
            @Override
            public void onResponse(Call<List<PortfolioTransactionDto>> call, Response<List<PortfolioTransactionDto>> resp) {
                // 2. Upsert asset (simplified: just add to holdings)
                upsertAssetOnServer(userId, symbol, name, quantity, price, callback);
            }
            @Override
            public void onFailure(Call<List<PortfolioTransactionDto>> call, Throwable t) {
                callback.onError("Cannot buy while offline.");
            }
        });
    }

    /** Sell: update asset (reduce quantity), log transaction */
    public void sellAsset(String symbol, double quantity, double price, TransactionCallback callback) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) { callback.onError("Not authenticated."); return; }

        Map<String, Object> txBody = new HashMap<>();
        txBody.put("user_id", userId);
        txBody.put("symbol", symbol);
        txBody.put("action", "SELL");
        txBody.put("quantity", quantity);
        txBody.put("price", price);

        api.createPortfolioTransaction(txBody).enqueue(new Callback<List<PortfolioTransactionDto>>() {
            @Override
            public void onResponse(Call<List<PortfolioTransactionDto>> call, Response<List<PortfolioTransactionDto>> resp) {
                String name = CryptoPriceSimulator.getInstance().getNameForSymbol(symbol);
                upsertAssetOnServer(userId, symbol, name, -quantity, price, callback);
            }
            @Override
            public void onFailure(Call<List<PortfolioTransactionDto>> call, Throwable t) {
                callback.onError("Cannot sell while offline.");
            }
        });
    }

    private void upsertAssetOnServer(String userId, String symbol, String name,
                                      double quantityDelta, double price, TransactionCallback callback) {
        // First get current holdings
        api.getPortfolioAssets("*", "symbol.asc").enqueue(new Callback<List<PortfolioAssetDto>>() {
            @Override
            public void onResponse(Call<List<PortfolioAssetDto>> call, Response<List<PortfolioAssetDto>> resp) {
                double currentQty = 0;
                double currentAvg = price;
                String existingId = null;

                if (resp.isSuccessful() && resp.body() != null) {
                    for (PortfolioAssetDto dto : resp.body()) {
                        if (dto.symbol.equals(symbol)) {
                            currentQty = dto.quantity;
                            currentAvg = dto.averagePrice;
                            existingId = dto.id;
                            break;
                        }
                    }
                }

                double newQty = currentQty + quantityDelta;
                if (newQty < 0) { callback.onError("Insufficient holdings."); return; }

                // Calculate new average price (only on buy)
                double newAvg = currentAvg;
                if (quantityDelta > 0 && (currentQty + quantityDelta) > 0) {
                    newAvg = ((currentAvg * currentQty) + (price * quantityDelta)) / (currentQty + quantityDelta);
                }

                if (newQty <= 0 && existingId != null) {
                    // Delete asset if sold everything
                    api.deletePortfolioAsset("eq." + existingId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> c, Response<Void> r) {
                            executor.execute(() -> assetDao.deleteBySymbol(symbol));
                            callback.onSuccess();
                        }
                        @Override
                        public void onFailure(Call<Void> c, Throwable t) { callback.onError("Error."); }
                    });
                } else {
                    Map<String, Object> body = new HashMap<>();
                    body.put("user_id", userId);
                    body.put("symbol", symbol);
                    body.put("name", name);
                    body.put("quantity", newQty);
                    body.put("average_price", Math.round(newAvg * 100.0) / 100.0);
                    body.put("asset_type", "crypto");

                    api.upsertPortfolioAsset(body).enqueue(new Callback<List<PortfolioAssetDto>>() {
                        @Override
                        public void onResponse(Call<List<PortfolioAssetDto>> c, Response<List<PortfolioAssetDto>> r) {
                            if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                                PortfolioAsset a = toAsset(r.body().get(0));
                                executor.execute(() -> assetDao.insert(a));
                            }
                            callback.onSuccess();
                        }
                        @Override
                        public void onFailure(Call<List<PortfolioAssetDto>> c, Throwable t) {
                            callback.onError("Error saving asset.");
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call<List<PortfolioAssetDto>> call, Throwable t) {
                callback.onError("Offline.");
            }
        });
    }

    public void getTransactions(TransactionListCallback callback) {
        api.getPortfolioTransactions("*", "created_at.desc").enqueue(new Callback<List<PortfolioTransactionDto>>() {
            @Override
            public void onResponse(Call<List<PortfolioTransactionDto>> call, Response<List<PortfolioTransactionDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) { callback.onError("Error."); return; }
                List<PortfolioTransaction> txs = new ArrayList<>();
                for (PortfolioTransactionDto dto : resp.body()) {
                    txs.add(new PortfolioTransaction(dto.id, dto.userId, dto.symbol,
                            dto.action, dto.quantity, dto.price, parseDate(dto.createdAt)));
                }
                callback.onSuccess(txs);
            }
            @Override
            public void onFailure(Call<List<PortfolioTransactionDto>> call, Throwable t) {
                callback.onError("Offline.");
            }
        });
    }

    /** Enriches assets with simulated current prices */
    private void enrichWithPrices(List<PortfolioAsset> assets) {
        CryptoPriceSimulator sim = CryptoPriceSimulator.getInstance();
        for (PortfolioAsset asset : assets) {
            CryptoPrice cp = sim.getPrice(asset.getSymbol());
            asset.setCurrentPrice(cp.getCurrentPrice());
            asset.setChange24h(cp.getChange24h());
        }
    }

    /** Portfolio optimization suggestions */
    public String getOptimizationSuggestion(List<PortfolioAsset> assets) {
        if (assets.isEmpty()) return "Buy your first crypto asset to get optimization suggestions.";
        double totalValue = 0;
        for (PortfolioAsset a : assets) totalValue += a.getCurrentValue();
        if (totalValue <= 0) return "No portfolio value to optimize.";

        StringBuilder sb = new StringBuilder();
        sb.append("Portfolio Rebalance Suggestions:\n\n");
        for (PortfolioAsset a : assets) {
            double pct = (a.getCurrentValue() / totalValue) * 100;
            sb.append(String.format(Locale.US, "• %s: %.1f%% of portfolio", a.getSymbol(), pct));
            if (pct > 60) sb.append(" ⚠️ Over-concentrated! Consider reducing.");
            else if (pct < 5) sb.append(" — Small position.");
            sb.append("\n");
        }
        sb.append(String.format(Locale.US, "\nTotal Value: $%,.2f", totalValue));
        return sb.toString();
    }

    private static PortfolioAsset toAsset(PortfolioAssetDto dto) {
        return new PortfolioAsset(dto.id, dto.userId, dto.symbol, dto.name,
                dto.quantity, dto.averagePrice, dto.assetType, parseDate(dto.updatedAt));
    }

    private static Date parseDate(String value) {
        if (value == null) return new Date();
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            return fmt.parse(value);
        } catch (Exception e) { return new Date(); }
    }
}
