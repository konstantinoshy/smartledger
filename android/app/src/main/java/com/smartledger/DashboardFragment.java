package com.smartledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.adapters.ExpenseAdapter;
import com.smartledger.api.SessionManager;
import com.smartledger.data.ExpenseRepository;
import com.smartledger.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private static final String[] CATEGORIES = {"Food", "Transport", "Entertainment", "Rent", "Travel", "General"};

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ExpenseRepository expenseRepository;
    private List<Expense> expenseList;
    private TextView totalExpensesText;
    private TextView monthlySpendText;
    private TextView spendIndicatorText;
    private ImageView spendIndicatorIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        recyclerView = view.findViewById(R.id.recycler_expenses);
        totalExpensesText = view.findViewById(R.id.text_total_expenses);
        monthlySpendText = view.findViewById(R.id.text_monthly_spend);
        spendIndicatorText = view.findViewById(R.id.text_spend_indicator);
        spendIndicatorIcon = view.findViewById(R.id.icon_spend_indicator);

        expenseRepository = ExpenseRepository.getInstance(requireContext());
        expenseList = new ArrayList<>();
        adapter = new ExpenseAdapter(expenseList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        loadExpenses();

        view.findViewById(R.id.card_add_expense).setOnClickListener(v -> showAddExpenseDialog());
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());

        TextView tvViewAll = view.findViewById(R.id.tv_view_all_activity);
        tvViewAll.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_expenses);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            loadExpenses();
        }
    }

    private void loadExpenses() {
        expenseRepository.getExpenses(new ExpenseRepository.ExpenseListCallback() {
            @Override
            public void onSuccess(List<Expense> expenses) {
                expenseList = expenses;
                adapter.submitExpenses(expenseList);
                updateTotal();
            }

            @Override
            public void onError(String message) {
                if (getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateTotal() {
        double totalSpent = 0;
        for (Expense e : expenseList) {
            totalSpent += e.getAmount();
        }
        double liquidity = 5000.00 - totalSpent;
        totalExpensesText.setText(String.format("$%,.2f", liquidity));
        if (monthlySpendText != null) {
            monthlySpendText.setText(String.format("$%,.2f", totalSpent));
        }
        updateSpendIndicator(totalSpent);
    }

    private void updateSpendIndicator(double totalSpent) {
        if (spendIndicatorText == null || spendIndicatorIcon == null || getContext() == null) return;

        double percentSpent = (totalSpent / 5000.00) * 100;
        boolean highSpend = percentSpent >= 50;

        int colorRes = highSpend ? R.color.sl_warning : R.color.sl_positive;
        int iconRes = highSpend ? R.drawable.ic_trending_down : R.drawable.ic_trending_up;
        int color = ContextCompat.getColor(getContext(), colorRes);

        spendIndicatorText.setText(String.format("%.1f%% of budget spent this month", percentSpent));
        spendIndicatorText.setTextColor(color);
        spendIndicatorIcon.setImageResource(iconRes);
        spendIndicatorIcon.setColorFilter(color);
    }

    private void showAddExpenseDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

        TextInputLayout tilDesc = dialogView.findViewById(R.id.til_description);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.til_amount);
        AutoCompleteTextView editCat = dialogView.findViewById(R.id.edit_category);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        editCat.setAdapter(catAdapter);
        editCat.setText(CATEGORIES[0], false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Expense")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String desc = tilDesc.getEditText().getText().toString().trim();
            String cat = editCat.getText().toString().trim();
            String amtStr = tilAmount.getEditText().getText().toString().trim();

            boolean valid = true;
            tilDesc.setError(null);
            tilAmount.setError(null);

            if (desc.isEmpty()) {
                tilDesc.setError("Description is required");
                valid = false;
            }
            if (amtStr.isEmpty()) {
                tilAmount.setError("Amount is required");
                valid = false;
            } else {
                try {
                    double amt = Double.parseDouble(amtStr);
                    if (amt <= 0) {
                        tilAmount.setError("Amount must be greater than 0");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    tilAmount.setError("Invalid amount");
                    valid = false;
                }
            }

            if (!valid) return;

            double amount = Double.parseDouble(amtStr);
            if (cat.isEmpty()) cat = "General";

            expenseRepository.addExpense(amount, cat, desc, new ExpenseRepository.ExpenseCallback() {
                @Override
                public void onSuccess(Expense expense) {
                    loadExpenses();
                    recyclerView.scrollToPosition(0);
                    dialog.dismiss();

                    if (getView() != null) {
                        Snackbar.make(getView(), "Expense added", Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    if (getView() != null) {
                        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("You will need to sign in again.")
                .setPositiveButton("Log Out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        new SessionManager(requireContext()).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
