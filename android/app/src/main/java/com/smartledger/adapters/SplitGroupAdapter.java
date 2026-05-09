package com.smartledger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartledger.R;
import com.smartledger.models.SplitGroup;

import java.util.ArrayList;
import java.util.List;

public class SplitGroupAdapter extends RecyclerView.Adapter<SplitGroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(SplitGroup group);
        void onGroupLongClick(SplitGroup group);
    }

    private final List<SplitGroup> groups = new ArrayList<>();
    private OnGroupClickListener listener;

    public SplitGroupAdapter() {}

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public void submitGroups(List<SplitGroup> newGroups) {
        groups.clear();
        groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_split_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        SplitGroup group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() { return groups.size(); }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvInfo;
        final TextView tvBalance;
        final TextView tvBalanceLabel;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_group_name);
            tvInfo = itemView.findViewById(R.id.tv_group_info);
            tvBalance = itemView.findViewById(R.id.tv_group_balance);
            tvBalanceLabel = itemView.findViewById(R.id.tv_balance_label);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroupClick(groups.get(pos));
                }
            });
            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onGroupLongClick(groups.get(pos));
                }
                return true;
            });
        }

        void bind(SplitGroup group) {
            tvName.setText(group.getName());

            int members = group.getMemberCount();
            int pending = group.getPendingCount();
            String info = members + " Members • " + pending + " Pending";
            tvInfo.setText(info);

            double balance = group.getBalanceAmount();
            String balanceStr = String.format(java.util.Locale.getDefault(), "$%,.2f", Math.abs(balance));
            if (balance >= 0) {
                tvBalance.setText("+" + balanceStr);
                tvBalance.setTextColor(itemView.getContext().getResources().getColor(R.color.sl_positive));
                tvBalanceLabel.setText("You are owed");
            } else {
                tvBalance.setText("-" + balanceStr);
                tvBalance.setTextColor(itemView.getContext().getResources().getColor(R.color.sl_warning));
                tvBalanceLabel.setText("You owe");
            }
        }
    }
}
