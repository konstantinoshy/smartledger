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

public class SplitsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_splits, container, false);

        view.findViewById(R.id.btn_add_group).setOnClickListener(v -> showAddGroupDialog());
        view.findViewById(R.id.btn_settle_up).setOnClickListener(v -> showSettleUpDialog());

        return view;
    }

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
                    if (getView() != null) {
                        Snackbar.make(getView(), "\"" + name + "\" group created", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettleUpDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Settle Up")
                .setMessage("Mark all pending expenses in this group as settled?")
                .setPositiveButton("Settle All", (dialog, which) -> {
                    if (getView() != null) {
                        Snackbar.make(getView(), "All settled!", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
