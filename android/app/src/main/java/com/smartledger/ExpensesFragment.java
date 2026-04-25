package com.smartledger;

import android.os.Bundle;
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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.adapters.ExpenseAdapter;
import com.smartledger.data.ExpenseRepository;
import com.smartledger.models.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpensesFragment extends Fragment {

    private static final String[] CATEGORIES = {"Food", "Transport", "Entertainment", "Rent", "Travel", "General"};

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ExpenseRepository expenseRepository;
    private List<Expense> allExpenses;
    private List<Expense> displayList;
    private TextView tvEmptyState;
    private TextView tvMonthlySpend;
    private String activeFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        recyclerView = view.findViewById(R.id.recycler_expenses);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvMonthlySpend = view.findViewById(R.id.tv_monthly_spend);

        expenseRepository = ExpenseRepository.getInstance(requireContext());
        allExpenses = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new ExpenseAdapter(displayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        loadExpenses();

        view.findViewById(R.id.btn_add_expense).setOnClickListener(v -> showAddExpenseDialog());

        setupChips(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void setupChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                activeFilter = null;
            } else {
                Chip selected = group.findViewById(checkedIds.get(0));
                if (selected != null) {
                    String text = selected.getText().toString();
                    activeFilter = text.equals("All") ? null : text;
                }
            }
            refreshList();
        });
    }

    private void refreshList() {
        List<Expense> nextDisplayList = new ArrayList<>();
        displayList.clear();
        if (activeFilter == null) {
            nextDisplayList.addAll(allExpenses);
        } else {
            for (Expense e : allExpenses) {
                if (activeFilter.equalsIgnoreCase(e.getCategory())) {
                    nextDisplayList.add(e);
                }
            }
        }
        displayList.addAll(nextDisplayList);
        adapter.submitExpenses(displayList);
        updateEmptyState();
        updateMonthlySpend();
    }

    private void loadExpenses() {
        expenseRepository.getExpenses(new ExpenseRepository.ExpenseListCallback() {
            @Override
            public void onSuccess(List<Expense> expenses) {
                allExpenses = expenses;
                refreshList();
            }

            @Override
            public void onError(String message) {
                if (getView() != null) {
                    Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                }
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (displayList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateMonthlySpend() {
        if (tvMonthlySpend == null) return;
        double total = 0;
        for (Expense e : allExpenses) {
            total += e.getAmount();
        }
        tvMonthlySpend.setText(String.format("$%,.2f", total));
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
}
