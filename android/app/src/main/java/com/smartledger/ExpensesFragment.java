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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

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
    private android.widget.ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        recyclerView = view.findViewById(R.id.recycler_expenses);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        tvMonthlySpend = view.findViewById(R.id.tv_monthly_spend);
        progressBar = view.findViewById(R.id.progress_bar);

        expenseRepository = ExpenseRepository.getInstance(requireContext());
        allExpenses = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new ExpenseAdapter(displayList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        setupSwipeToDelete();
        
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadExpenses();
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#EF4444"));
            private Drawable deleteIcon;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                View itemView = viewHolder.itemView;

                if (deleteIcon == null && getContext() != null) {
                    deleteIcon = ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_delete);
                    if (deleteIcon != null) {
                        deleteIcon.setTint(Color.WHITE);
                    }
                }

                if (dX < 0 && deleteIcon != null) { 
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    background.setBounds(itemView.getRight() + ((int) dX) - 20, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Expense expenseToDelete = displayList.get(position);

                displayList.remove(position);
                adapter.notifyItemRemoved(position);

                expenseRepository.deleteExpense(expenseToDelete, new ExpenseRepository.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        loadExpenses();
                        if (getView() != null) {
                            Snackbar.make(getView(), "Expense deleted", Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        displayList.add(position, expenseToDelete);
                        adapter.notifyItemInserted(position);
                        if (getView() != null) {
                            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        };

        new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerView);
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
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        expenseRepository.getExpenses(new ExpenseRepository.ExpenseListCallback() {
            @Override
            public void onSuccess(List<Expense> expenses) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                allExpenses = expenses;
                refreshList();
            }

            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
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
        double total = com.smartledger.utils.FinancialUtils.calculateTotalSpent(allExpenses);
        tvMonthlySpend.setText(String.format(java.util.Locale.getDefault(), "$%,.2f", total));
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
            String desc = tilDesc.getEditText() != null ? tilDesc.getEditText().getText().toString().trim() : "";
            String cat = editCat.getText().toString().trim();
            String amtStr = tilAmount.getEditText() != null ? tilAmount.getEditText().getText().toString().trim() : "";

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

            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            expenseRepository.addExpense(amount, cat, desc, new ExpenseRepository.ExpenseCallback() {
                @Override
                public void onSuccess(Expense expense) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    loadExpenses();
                    recyclerView.scrollToPosition(0);
                    dialog.dismiss();

                    if (getView() != null) {
                        Snackbar.make(getView(), "Expense added", Snackbar.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (getView() != null) {
                        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }
}
