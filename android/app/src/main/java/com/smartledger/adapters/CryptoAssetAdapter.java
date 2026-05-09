package com.smartledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartledger.R;
import com.smartledger.models.PortfolioAsset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CryptoAssetAdapter extends RecyclerView.Adapter<CryptoAssetAdapter.AssetViewHolder> {

    public interface OnAssetClickListener {
        void onAssetClick(PortfolioAsset asset);
    }

    private final List<PortfolioAsset> assets = new ArrayList<>();
    private OnAssetClickListener listener;

    public void setOnAssetClickListener(OnAssetClickListener listener) {
        this.listener = listener;
    }

    public void submitAssets(List<PortfolioAsset> newAssets) {
        assets.clear();
        assets.addAll(newAssets);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crypto_asset, parent, false);
        return new AssetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        holder.bind(assets.get(position));
    }

    @Override
    public int getItemCount() { return assets.size(); }

    class AssetViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvSymbol, tvPrice, tvChange, tvHoldings, tvValue;
        final ImageView ivTrend;

        AssetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_asset_name);
            tvSymbol = itemView.findViewById(R.id.tv_asset_symbol);
            tvPrice = itemView.findViewById(R.id.tv_asset_price);
            tvChange = itemView.findViewById(R.id.tv_asset_change);
            tvHoldings = itemView.findViewById(R.id.tv_asset_holdings);
            tvValue = itemView.findViewById(R.id.tv_asset_value);
            ivTrend = itemView.findViewById(R.id.iv_trend);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAssetClick(assets.get(pos));
                }
            });
        }

        void bind(PortfolioAsset asset) {
            tvName.setText(asset.getName());
            tvSymbol.setText(asset.getSymbol());
            tvPrice.setText(String.format(Locale.US, "$%,.2f", asset.getCurrentPrice()));
            tvHoldings.setText(String.format(Locale.US, "%.4f %s", asset.getQuantity(), asset.getSymbol()));
            tvValue.setText(String.format(Locale.US, "$%,.2f", asset.getCurrentValue()));

            double change = asset.getChange24h();
            boolean positive = change >= 0;
            String changeStr = String.format(Locale.US, "%s%.1f%%", positive ? "+" : "", change);
            tvChange.setText(changeStr);

            int color = positive ? R.color.sl_positive : R.color.sl_warning;
            tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), color));

            int trendIcon = positive ? R.drawable.ic_trending_up : R.drawable.ic_trending_down;
            ivTrend.setImageResource(trendIcon);
            ivTrend.setColorFilter(ContextCompat.getColor(itemView.getContext(), color));
        }
    }
}
