package com.smartledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.smartledger.R;
import com.smartledger.models.Expense;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenseList = new ArrayList<>();

    public ExpenseAdapter(List<Expense> expenseList) {
        this.expenseList.addAll(expenseList);
    }

    public void submitExpenses(List<Expense> newExpenses) {
        List<Expense> oldExpenses = new ArrayList<>(expenseList);
        List<Expense> nextExpenses = new ArrayList<>(newExpenses);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ExpenseDiffCallback(oldExpenses, nextExpenses)
        );

        expenseList.clear();
        expenseList.addAll(nextExpenses);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        final TextView description;
        final TextView category;
        final TextView amount;
        private Expense currentExpense;
        private String currentAmount;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.text_description);
            category = itemView.findViewById(R.id.text_category);
            amount = itemView.findViewById(R.id.text_amount);

            itemView.setOnClickListener(v -> showExpenseDetails());
        }

        void bind(Expense expense) {
            currentExpense = expense;
            currentAmount = itemView.getContext()
                    .getString(R.string.expense_amount_format, expense.getAmount());

            description.setText(expense.getDescription());
            category.setText(expense.getCategory());
            amount.setText(currentAmount);
        }

        private void showExpenseDetails() {
            if (currentExpense == null) {
                return;
            }

            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(itemView.getContext());
            builder.setTitle("Expense Details");

            String dateString = "N/A";
            if (currentExpense.getDate() != null) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
                try {
                    dateString = sdf.format(currentExpense.getDate());
                } catch (Exception e) {
                    dateString = "Invalid Date";
                }
            }

            String message = "Date: " + dateString + "\n\n" +
                    "Description: " + currentExpense.getDescription() + "\n\n" +
                    "Category: " + currentExpense.getCategory() + "\n\n" +
                    "Amount: " + currentAmount;

            builder.setMessage(message);
            builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    private static class ExpenseDiffCallback extends DiffUtil.Callback {
        private final List<Expense> oldExpenses;
        private final List<Expense> newExpenses;

        ExpenseDiffCallback(List<Expense> oldExpenses, List<Expense> newExpenses) {
            this.oldExpenses = oldExpenses;
            this.newExpenses = newExpenses;
        }

        @Override
        public int getOldListSize() {
            return oldExpenses.size();
        }

        @Override
        public int getNewListSize() {
            return newExpenses.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Expense oldExpense = oldExpenses.get(oldItemPosition);
            Expense newExpense = newExpenses.get(newItemPosition);

            if (oldExpense.getId() != null && newExpense.getId() != null) {
                return oldExpense.getId().equals(newExpense.getId());
            }

            return sameExpense(oldExpense, newExpense);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return sameExpense(oldExpenses.get(oldItemPosition), newExpenses.get(newItemPosition));
        }

        private static boolean sameExpense(Expense left, Expense right) {
            return Double.compare(left.getAmount(), right.getAmount()) == 0
                    && Objects.equals(left.getId(), right.getId())
                    && Objects.equals(left.getUserId(), right.getUserId())
                    && Objects.equals(left.getCategory(), right.getCategory())
                    && Objects.equals(left.getDescription(), right.getDescription())
                    && sameDate(left.getDate(), right.getDate())
                    && sameDate(left.getCreatedAt(), right.getCreatedAt());
        }

        private static boolean sameDate(Date left, Date right) {
            if (left == null || right == null) {
                return left == right;
            }
            return left.getTime() == right.getTime();
        }
    }
}
