package com.smartledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class CryptoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crypto, container, false);

        view.findViewById(R.id.btn_buy).setOnClickListener(v -> showTradeDialog("Buy"));
        view.findViewById(R.id.btn_sell).setOnClickListener(v -> showTradeDialog("Sell"));
        view.findViewById(R.id.btn_optimize).setOnClickListener(v -> showOptimizeDialog());
        view.findViewById(R.id.tv_view_all_assets).setOnClickListener(v -> {
            if (getView() != null) {
                Snackbar.make(getView(), "More assets coming in Phase 2", Snackbar.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showTradeDialog(String action) {
        EditText input = new EditText(requireContext());
        input.setHint("Amount in USD");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setSingleLine(true);

        LinearLayout container = new LinearLayout(requireContext());
        container.setPadding(64, 32, 64, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(action + " Bitcoin")
                .setMessage("Enter the amount you'd like to " + action.toLowerCase() + ".")
                .setView(container)
                .setPositiveButton("Confirm " + action, (dialog, which) -> {
                    String amtStr = input.getText().toString().trim();
                    String display = amtStr.isEmpty() ? "" : " for $" + amtStr;
                    if (getView() != null) {
                        Snackbar.make(getView(), action + " order placed" + display, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOptimizeDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Portfolio Optimization")
                .setMessage("Simulated rebalance complete.\n\n"
                        + "• BTC allocation: 55% → 50%\n"
                        + "• Stablecoin reserve: 10% → 15%\n"
                        + "• Estimated annual yield: +3.2%\n\n"
                        + "This is a Phase 1 simulation. Real trading will be available in a future update.")
                .setPositiveButton("Got it", null)
                .show();
    }
}
