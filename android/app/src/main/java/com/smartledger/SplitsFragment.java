package com.smartledger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.smartledger.adapters.SplitGroupAdapter;
import com.smartledger.data.SplitRepository;
import com.smartledger.models.SplitExpense;
import com.smartledger.models.SplitGroup;
import com.smartledger.models.SplitMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SplitsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SplitGroupAdapter adapter;
    private SplitRepository repository;
    private View emptyState;
    private android.widget.ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splits, container, false);

        recyclerView = view.findViewById(R.id.recycler_groups);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        repository = SplitRepository.getInstance(requireContext());

        adapter = new SplitGroupAdapter();
        adapter.setOnGroupClickListener(new SplitGroupAdapter.OnGroupClickListener() {
            @Override
            public void onGroupClick(SplitGroup group) {
                showGroupDetailDialog(group);
            }
            @Override
            public void onGroupLongClick(SplitGroup group) {
                showDeleteGroupDialog(group);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_add_group).setOnClickListener(v -> showAddGroupDialog());
        swipeRefresh.setOnRefreshListener(this::loadGroups);

        loadGroups();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) loadGroups();
    }

    private void loadGroups() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        repository.getGroups(new SplitRepository.GroupListCallback() {
            @Override
            public void onSuccess(List<SplitGroup> groups) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                // For each group, load members + expenses to get counts and balance
                for (SplitGroup group : groups) {
                    enrichGroupData(group, () -> adapter.submitGroups(groups));
                }
                if (groups.isEmpty()) {
                    adapter.submitGroups(groups);
                }
                updateEmptyState(groups.isEmpty());
            }
            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (getView() != null) Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                updateEmptyState(true);
            }
        });
    }

    private void enrichGroupData(SplitGroup group, Runnable onDone) {
        repository.getMembers(group.getId(), new SplitRepository.MemberListCallback() {
            @Override
            public void onSuccess(List<SplitMember> members) {
                group.setMemberCount(members.size());
                // Now get expenses for balance calculation
                repository.getGroupExpenses(group.getId(), new SplitRepository.ExpenseListCallback() {
                    @Override
                    public void onSuccess(List<SplitExpense> expenses) {
                        int pending = 0;
                        for (SplitExpense e : expenses) if (!e.isSettled()) pending++;
                        group.setPendingCount(pending);

                        // Calculate user balance (first member = "You")
                        List<String> names = new ArrayList<>();
                        names.add("You");
                        for (SplitMember m : members) names.add(m.getDisplayName());
                        Map<String, Double> balances = SplitRepository.calculateBalances(expenses, names);
                        Double myBalance = balances.get("You");
                        group.setBalanceAmount(myBalance != null ? myBalance : 0);
                        onDone.run();
                    }
                    @Override
                    public void onError(String msg) {
                        group.setPendingCount(0);
                        group.setBalanceAmount(0);
                        onDone.run();
                    }
                });
            }
            @Override
            public void onError(String msg) {
                group.setMemberCount(0);
                onDone.run();
            }
        });
    }

    private void updateEmptyState(boolean empty) {
        if (emptyState != null) emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─── Add Group Dialog ───────────────────────────

    private void showAddGroupDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Group name (e.g. Road Trip)");
        input.setSingleLine(true);

        LinearLayout container = new LinearLayout(requireContext());
        container.setPadding(64, 32, 64, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Group")
                .setView(container)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Untitled Group";
                    createGroup(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createGroup(String name) {
        repository.createGroup(name, new SplitRepository.GroupCallback() {
            @Override
            public void onSuccess(SplitGroup group) {
                if (getView() != null)
                    Snackbar.make(getView(), "\"" + group.getName() + "\" created!", Snackbar.LENGTH_SHORT).show();
                // Prompt to add members
                showAddMemberDialog(group);
                loadGroups();
            }
            @Override
            public void onError(String message) {
                if (getView() != null) Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ─── Group Detail Dialog ────────────────────────

    private void showGroupDetailDialog(SplitGroup group) {
        // First load members and expenses
        repository.getMembers(group.getId(), new SplitRepository.MemberListCallback() {
            @Override
            public void onSuccess(List<SplitMember> members) {
                repository.getGroupExpenses(group.getId(), new SplitRepository.ExpenseListCallback() {
                    @Override
                    public void onSuccess(List<SplitExpense> expenses) {
                        buildGroupDetailDialog(group, members, expenses);
                    }
                    @Override
                    public void onError(String msg) {
                        buildGroupDetailDialog(group, members, new ArrayList<>());
                    }
                });
            }
            @Override
            public void onError(String msg) {
                if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void buildGroupDetailDialog(SplitGroup group, List<SplitMember> members,
                                         List<SplitExpense> expenses) {
        List<String> allNames = new ArrayList<>();
        allNames.add("You");
        for (SplitMember m : members) allNames.add(m.getDisplayName());

        Map<String, Double> balances = SplitRepository.calculateBalances(expenses, allNames);

        StringBuilder sb = new StringBuilder();
        sb.append("Members: ").append(allNames.size()).append("\n\n");

        // Balances
        sb.append("─── Balances ───\n");
        for (String name : allNames) {
            Double bal = balances.get(name);
            double b = bal != null ? bal : 0;
            String sign = b >= 0 ? "+" : "";
            sb.append(String.format(Locale.US, "• %s: %s$%.2f\n", name, sign, Math.abs(b)));
        }

        // Pending expenses
        int pending = 0;
        double totalPending = 0;
        for (SplitExpense e : expenses) {
            if (!e.isSettled()) { pending++; totalPending += e.getAmount(); }
        }
        sb.append(String.format(Locale.US, "\n%d pending expenses ($%.2f total)\n", pending, totalPending));

        // Recent expenses
        sb.append("\n─── Recent Expenses ───\n");
        int shown = 0;
        for (SplitExpense e : expenses) {
            if (shown >= 5) break;
            String status = e.isSettled() ? "✓" : "⏳";
            sb.append(String.format(Locale.US, "%s %s — $%.2f (by %s)\n",
                    status, e.getDescription(), e.getAmount(), e.getPaidBy()));
            shown++;
        }
        if (expenses.isEmpty()) sb.append("No expenses yet.\n");

        final int pendingCount = pending;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(group.getName())
                .setMessage(sb.toString())
                .setPositiveButton("Add Expense", (d, w) -> showAddExpenseDialog(group, allNames))
                .setNeutralButton("Add Member", (d, w) -> showAddMemberDialog(group))
                .setNegativeButton(pendingCount > 0 ? "Settle All" : "Close", (d, w) -> {
                    if (pendingCount > 0) settleGroup(group);
                })
                .show();
    }

    // ─── Add Member Dialog ──────────────────────────

    private void showAddMemberDialog(SplitGroup group) {
        EditText input = new EditText(requireContext());
        input.setHint("Member name (e.g. Alex)");
        input.setSingleLine(true);

        LinearLayout container = new LinearLayout(requireContext());
        container.setPadding(64, 32, 64, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Member to " + group.getName())
                .setView(container)
                .setPositiveButton("Add", (dialog, which) -> {
                    String memberName = input.getText().toString().trim();
                    if (memberName.isEmpty()) return;
                    repository.addMember(group.getId(), memberName, new SplitRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (getView() != null)
                                Snackbar.make(getView(), memberName + " added!", Snackbar.LENGTH_SHORT).show();
                            loadGroups();
                        }
                        @Override
                        public void onError(String msg) {
                            if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Add Expense Dialog ─────────────────────────

    private void showAddExpenseDialog(SplitGroup group, List<String> memberNames) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_split_expense, null);

        AutoCompleteTextView editPaidBy = dialogView.findViewById(R.id.edit_paid_by);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.til_split_amount);
        TextInputLayout tilDesc = dialogView.findViewById(R.id.til_split_description);

        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, memberNames);
        editPaidBy.setAdapter(nameAdapter);
        if (!memberNames.isEmpty()) editPaidBy.setText(memberNames.get(0), false);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Shared Expense")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String paidBy = editPaidBy.getText().toString().trim();
            String amtStr = tilAmount.getEditText() != null ? tilAmount.getEditText().getText().toString().trim() : "";
            String desc = tilDesc.getEditText() != null ? tilDesc.getEditText().getText().toString().trim() : "";

            boolean valid = true;
            tilAmount.setError(null);
            tilDesc.setError(null);

            if (amtStr.isEmpty()) { tilAmount.setError("Required"); valid = false; }
            else {
                try {
                    double amt = Double.parseDouble(amtStr);
                    if (amt <= 0) { tilAmount.setError("Must be > 0"); valid = false; }
                } catch (NumberFormatException e) { tilAmount.setError("Invalid"); valid = false; }
            }
            if (desc.isEmpty()) { tilDesc.setError("Required"); valid = false; }
            if (paidBy.isEmpty()) paidBy = "You";
            if (!valid) return;

            double amount = Double.parseDouble(amtStr);
            repository.addSplitExpense(group.getId(), paidBy, amount, desc,
                    new SplitRepository.ExpenseCallback() {
                @Override
                public void onSuccess(SplitExpense expense) {
                    dialog.dismiss();
                    if (getView() != null)
                        Snackbar.make(getView(), "Expense added!", Snackbar.LENGTH_SHORT).show();
                    loadGroups();
                }
                @Override
                public void onError(String msg) {
                    if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    // ─── Settle / Delete ────────────────────────────

    private void settleGroup(SplitGroup group) {
        repository.settleAll(group.getId(), new SplitRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getView() != null)
                    Snackbar.make(getView(), "All settled! ✓", Snackbar.LENGTH_SHORT).show();
                loadGroups();
            }
            @Override
            public void onError(String msg) {
                if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showDeleteGroupDialog(SplitGroup group) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Group?")
                .setMessage("This will permanently delete \"" + group.getName() + "\" and all its expenses.")
                .setPositiveButton("Delete", (d, w) -> {
                    repository.deleteGroup(group.getId(), new SplitRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            if (getView() != null)
                                Snackbar.make(getView(), "Group deleted", Snackbar.LENGTH_SHORT).show();
                            loadGroups();
                        }
                        @Override
                        public void onError(String msg) {
                            if (getView() != null) Snackbar.make(getView(), msg, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
