package com.smartledger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.adapters.CryptoAssetAdapter;
import com.smartledger.api.CryptoPriceSimulator;
import com.smartledger.data.CryptoRepository;
import com.smartledger.models.CryptoPrice;
import com.smartledger.models.PortfolioAsset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CryptoFragment extends Fragment {

    private RecyclerView recyclerView;
    private CryptoAssetAdapter adapter;
    private CryptoRepository repository;
    private TextView tvTotalValue;
    private TextView tvPortfolioChange;
    private View emptyState;
    private android.widget.ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    private List<PortfolioAsset> currentAssets = new ArrayList<>();
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshPrices;
    private static final long PRICE_REFRESH_INTERVAL = 30_000; // 30 sec

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crypto, container, false);

        recyclerView = view.findViewById(R.id.recycler_assets);
        tvTotalValue = view.findViewById(R.id.tv_total_value);
        tvPortfolioChange = view.findViewById(R.id.tv_portfolio_change);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        repository = CryptoRepository.getInstance(requireContext());

        adapter = new CryptoAssetAdapter();
        adapter.setOnAssetClickListener(this::showAssetDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_buy).setOnClickListener(v -> showTradeDialog("Buy"));
        view.findViewById(R.id.btn_sell).setOnClickListener(v -> showTradeDialog("Sell"));
        view.findViewById(R.id.btn_optimize).setOnClickListener(v -> showOptimizeDialog());
        swipeRefresh.setOnRefreshListener(this::loadPortfolio);

        loadPortfolio();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPortfolio();
        startPriceRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPriceRefresh();
    }

    private void startPriceRefresh() {
        refreshHandler.postDelayed(refreshRunnable, PRICE_REFRESH_INTERVAL);
    }

    private void stopPriceRefresh() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void refreshPrices() {
        if (!isAdded()) return;
        // Re-enrich with new simulated prices
        CryptoPriceSimulator sim = CryptoPriceSimulator.getInstance();
        for (PortfolioAsset asset : currentAssets) {
            CryptoPrice cp = sim.getPrice(asset.getSymbol());
            asset.setCurrentPrice(cp.getCurrentPrice());
            asset.setChange24h(cp.getChange24h());
        }
        adapter.submitAssets(currentAssets);
        updatePortfolioHeader();
        startPriceRefresh();
    }

    private void loadPortfolio() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repository.getPortfolio(new CryptoRepository.AssetListCallback() {
            @Override
            public void onSuccess(List<PortfolioAsset> assets) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                currentAssets = assets;
                adapter.submitAssets(assets);
                updatePortfolioHeader();
                updateEmptyState(assets.isEmpty());
            }
            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (getView() != null) Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                updateEmptyState(currentAssets.isEmpty());
            }
        });
    }

    private void updatePortfolioHeader() {
        double totalValue = 0;
        double weightedChange = 0;
        for (PortfolioAsset a : currentAssets) {
            totalValue += a.getCurrentValue();
            weightedChange += a.getChange24h() * (a.getCurrentValue());
        }

        if (tvTotalValue != null) {
            tvTotalValue.setText(String.format(Locale.US, "$%,.2f", totalValue));
        }
        if (tvPortfolioChange != null && totalValue > 0) {
            double avgChange = weightedChange / totalValue;
            String sign = avgChange >= 0 ? "+" : "";
            tvPortfolioChange.setText(String.format(Locale.US, "%s%.1f%%", sign, avgChange));
        }
    }

    private void updateEmptyState(boolean empty) {
        if (emptyState != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─── Trade Dialog ───────────────────────────────

    private void showTradeDialog(String action) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_trade_crypto, null);

        AutoCompleteTextView editSymbol = dialogView.findViewById(R.id.edit_crypto_symbol);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.til_trade_amount);
        TextView tvPreview = dialogView.findViewById(R.id.tv_trade_preview);

        CryptoPriceSimulator sim = CryptoPriceSimulator.getInstance();
        String[] symbols = sim.getSupportedSymbols();
        String[] labels = new String[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            labels[i] = symbols[i] + " — " + sim.getNameForSymbol(symbols[i]);
        }

        ArrayAdapter<String> symAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, labels);
        editSymbol.setAdapter(symAdapter);

        // Update preview when symbol changes
        editSymbol.setOnItemClickListener((parent, v, pos, id) -> {
            CryptoPrice cp = sim.getPrice(symbols[pos]);
            String amtStr = tilAmount.getEditText() != null ?
                    tilAmount.getEditText().getText().toString().trim() : "";
            updateTradePreview(tvPreview, cp, amtStr, action);
        });

        // Update preview when amount changes
        if (tilAmount.getEditText() != null) {
            tilAmount.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    String selected = editSymbol.getText().toString();
                    for (int i = 0; i < labels.length; i++) {
                        if (labels[i].equals(selected)) {
                            CryptoPrice cp = sim.getPrice(symbols[i]);
                            updateTradePreview(tvPreview, cp, s.toString().trim(), action);
                            break;
                        }
                    }
                }
            });
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(action + " Crypto")
                .setView(dialogView)
                .setPositiveButton("Confirm " + action, null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String selectedLabel = editSymbol.getText().toString().trim();
            String amtStr = tilAmount.getEditText() != null ?
                    tilAmount.getEditText().getText().toString().trim() : "";

            // Find symbol
            String symbol = null;
            for (int i = 0; i < labels.length; i++) {
                if (labels[i].equals(selectedLabel)) { symbol = symbols[i]; break; }
            }
            if (symbol == null) {
                Snackbar.make(getView(), "Select a cryptocurrency", Snackbar.LENGTH_SHORT).show();
                return;
            }

            tilAmount.setError(null);
            if (amtStr.isEmpty()) { tilAmount.setError("Required"); return; }
            double amountUsd;
            try {
                amountUsd = Double.parseDouble(amtStr);
                if (amountUsd <= 0) { tilAmount.setError("Must be > 0"); return; }
            } catch (NumberFormatException e) { tilAmount.setError("Invalid"); return; }

            CryptoPrice cp = sim.getPrice(symbol);
            double quantity = amountUsd / cp.getCurrentPrice();

            CryptoRepository.TransactionCallback cb = new CryptoRepository.TransactionCallback() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    if (getView() != null)
                        Snackbar.make(getView(), action + " order executed!", Snackbar.LENGTH_SHORT).show();
                    loadPortfolio();
                }
                @Override
                public void onError(String msg) {
                    if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
                }
            };

            if ("Buy".equals(action)) {
                repository.buyAsset(symbol, quantity, cp.getCurrentPrice(), cb);
            } else {
                repository.sellAsset(symbol, quantity, cp.getCurrentPrice(), cb);
            }
        });
    }

    private void updateTradePreview(TextView tvPreview, CryptoPrice cp, String amtStr, String action) {
        if (cp == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%s price: $%,.2f\n", cp.getSymbol(), cp.getCurrentPrice()));
        sb.append(String.format(Locale.US, "24h: %s%.1f%%\n",
                cp.getChange24h() >= 0 ? "+" : "", cp.getChange24h()));

        if (!amtStr.isEmpty()) {
            try {
                double amt = Double.parseDouble(amtStr);
                double qty = amt / cp.getCurrentPrice();
                sb.append(String.format(Locale.US, "\nYou will %s: %.6f %s",
                        action.toLowerCase(), qty, cp.getSymbol()));
            } catch (NumberFormatException ignored) {}
        }
        tvPreview.setText(sb.toString());
    }

    // ─── Asset Detail ───────────────────────────────

    private void showAssetDetail(PortfolioAsset asset) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "Symbol: %s\n", asset.getSymbol()));
        sb.append(String.format(Locale.US, "Current Price: $%,.2f\n", asset.getCurrentPrice()));
        sb.append(String.format(Locale.US, "24h Change: %s%.1f%%\n",
                asset.getChange24h() >= 0 ? "+" : "", asset.getChange24h()));
        sb.append(String.format(Locale.US, "\nYour Holdings: %.4f %s\n",
                asset.getQuantity(), asset.getSymbol()));
        sb.append(String.format(Locale.US, "Current Value: $%,.2f\n", asset.getCurrentValue()));
        sb.append(String.format(Locale.US, "Avg Buy Price: $%,.2f\n", asset.getAveragePrice()));

        double pnl = asset.getProfitLoss();
        sb.append(String.format(Locale.US, "P&L: %s$%,.2f\n",
                pnl >= 0 ? "+" : "-", Math.abs(pnl)));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(asset.getName())
                .setMessage(sb.toString())
                .setPositiveButton("Buy More", (d, w) -> showTradeDialog("Buy"))
                .setNeutralButton("Sell", (d, w) -> showTradeDialog("Sell"))
                .setNegativeButton("Close", null)
                .show();
    }

    // ─── Optimize Dialog ────────────────────────────

    private void showOptimizeDialog() {
        String suggestion = repository.getOptimizationSuggestion(currentAssets);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Portfolio Optimization")
                .setMessage(suggestion)
                .setPositiveButton("Got it", null)
                .show();
    }
}
