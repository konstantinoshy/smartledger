package com.smartledger.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.smartledger.api.ApiClient;
import com.smartledger.api.SessionManager;
import com.smartledger.api.SmartLedgerApi;
import com.smartledger.api.dto.SplitExpenseDto;
import com.smartledger.api.dto.SplitGroupDto;
import com.smartledger.api.dto.SplitMemberDto;
import com.smartledger.data.local.AppDatabase;
import com.smartledger.data.local.SplitExpenseDao;
import com.smartledger.data.local.SplitGroupDao;
import com.smartledger.models.SplitExpense;
import com.smartledger.models.SplitGroup;
import com.smartledger.models.SplitMember;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplitRepository {

    public interface GroupListCallback {
        void onSuccess(List<SplitGroup> groups);
        void onError(String message);
    }

    public interface GroupCallback {
        void onSuccess(SplitGroup group);
        void onError(String message);
    }

    public interface ExpenseListCallback {
        void onSuccess(List<SplitExpense> expenses);
        void onError(String message);
    }

    public interface ExpenseCallback {
        void onSuccess(SplitExpense expense);
        void onError(String message);
    }

    public interface MemberListCallback {
        void onSuccess(List<SplitMember> members);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    private static SplitRepository instance;
    private final SessionManager sessionManager;
    private final SmartLedgerApi api;
    private final SplitGroupDao groupDao;
    private final SplitExpenseDao splitExpenseDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private SplitRepository(Context context) {
        sessionManager = new SessionManager(context);
        api = ApiClient.getApi(sessionManager);
        AppDatabase db = AppDatabase.getDatabase(context);
        groupDao = db.splitGroupDao();
        splitExpenseDao = db.splitExpenseDao();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static SplitRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SplitRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getGroups(GroupListCallback callback) {
        api.getGroups("*", "created_at.desc").enqueue(new Callback<List<SplitGroupDto>>() {
            @Override
            public void onResponse(Call<List<SplitGroupDto>> call, Response<List<SplitGroupDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    loadGroupsLocal(callback);
                    return;
                }
                List<SplitGroup> groups = new ArrayList<>();
                for (SplitGroupDto dto : response.body()) {
                    groups.add(toGroup(dto));
                }
                executor.execute(() -> { groupDao.clearAll(); groupDao.insertAll(groups); });
                callback.onSuccess(groups);
            }

            @Override
            public void onFailure(Call<List<SplitGroupDto>> call, Throwable t) {
                loadGroupsLocal(callback);
            }
        });
    }

    private void loadGroupsLocal(GroupListCallback callback) {
        executor.execute(() -> {
            List<SplitGroup> local = groupDao.getAllGroups();
            mainHandler.post(() -> {
                if (!local.isEmpty()) {
                    callback.onError("Offline. Showing cached groups.");
                    callback.onSuccess(local);
                } else {
                    callback.onError("No groups found.");
                }
            });
        });
    }

    public void createGroup(String name, GroupCallback callback) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) { callback.onError("Not authenticated."); return; }
        Map<String, Object> body = new HashMap<>();
        body.put("owner_id", userId);
        body.put("name", name);
        api.createGroup(body).enqueue(new Callback<List<SplitGroupDto>>() {
            @Override
            public void onResponse(Call<List<SplitGroupDto>> call, Response<List<SplitGroupDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    callback.onError("Could not create group."); return;
                }
                SplitGroup group = toGroup(response.body().get(0));
                executor.execute(() -> groupDao.insert(group));
                callback.onSuccess(group);
            }
            @Override
            public void onFailure(Call<List<SplitGroupDto>> call, Throwable t) {
                callback.onError("Cannot create group while offline.");
            }
        });
    }

    public void deleteGroup(String groupId, SimpleCallback callback) {
        api.deleteGroup("eq." + groupId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                executor.execute(() -> groupDao.deleteById(groupId));
                callback.onSuccess();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Cannot delete while offline.");
            }
        });
    }

    public void getMembers(String groupId, MemberListCallback callback) {
        api.getMembers("eq." + groupId, "*").enqueue(new Callback<List<SplitMemberDto>>() {
            @Override
            public void onResponse(Call<List<SplitMemberDto>> call, Response<List<SplitMemberDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Could not load members."); return;
                }
                List<SplitMember> members = new ArrayList<>();
                for (SplitMemberDto dto : response.body()) {
                    members.add(new SplitMember(dto.id, dto.groupId, dto.displayName));
                }
                callback.onSuccess(members);
            }
            @Override
            public void onFailure(Call<List<SplitMemberDto>> call, Throwable t) {
                callback.onError("Offline.");
            }
        });
    }

    public void addMember(String groupId, String displayName, SimpleCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("group_id", groupId);
        body.put("display_name", displayName);
        api.addMember(body).enqueue(new Callback<List<SplitMemberDto>>() {
            @Override
            public void onResponse(Call<List<SplitMemberDto>> call, Response<List<SplitMemberDto>> response) {
                if (!response.isSuccessful()) { callback.onError("Could not add member."); return; }
                callback.onSuccess();
            }
            @Override
            public void onFailure(Call<List<SplitMemberDto>> call, Throwable t) {
                callback.onError("Offline.");
            }
        });
    }

    public void getGroupExpenses(String groupId, ExpenseListCallback callback) {
        api.getSplitExpenses("eq." + groupId, "*", "created_at.desc").enqueue(new Callback<List<SplitExpenseDto>>() {
            @Override
            public void onResponse(Call<List<SplitExpenseDto>> call, Response<List<SplitExpenseDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    loadExpensesLocal(groupId, callback); return;
                }
                List<SplitExpense> expenses = new ArrayList<>();
                for (SplitExpenseDto dto : response.body()) { expenses.add(toExpense(dto)); }
                executor.execute(() -> { splitExpenseDao.clearByGroup(groupId); splitExpenseDao.insertAll(expenses); });
                callback.onSuccess(expenses);
            }
            @Override
            public void onFailure(Call<List<SplitExpenseDto>> call, Throwable t) {
                loadExpensesLocal(groupId, callback);
            }
        });
    }

    private void loadExpensesLocal(String groupId, ExpenseListCallback callback) {
        executor.execute(() -> {
            List<SplitExpense> local = splitExpenseDao.getExpensesByGroup(groupId);
            mainHandler.post(() -> {
                callback.onError("Offline. Showing cached data.");
                callback.onSuccess(local);
            });
        });
    }

    public void addSplitExpense(String groupId, String paidBy, double amount,
                                String description, ExpenseCallback callback) {
        int amountMinor = (int) Math.round(amount * 100);
        Map<String, Object> body = new HashMap<>();
        body.put("group_id", groupId);
        body.put("paid_by", paidBy);
        body.put("amount_minor", amountMinor);
        body.put("description", description);
        body.put("is_settled", false);
        api.createSplitExpense(body).enqueue(new Callback<List<SplitExpenseDto>>() {
            @Override
            public void onResponse(Call<List<SplitExpenseDto>> call, Response<List<SplitExpenseDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    callback.onError("Could not add expense."); return;
                }
                SplitExpense expense = toExpense(response.body().get(0));
                executor.execute(() -> splitExpenseDao.insert(expense));
                callback.onSuccess(expense);
            }
            @Override
            public void onFailure(Call<List<SplitExpenseDto>> call, Throwable t) {
                callback.onError("Cannot add expense while offline.");
            }
        });
    }

    public void settleAll(String groupId, SimpleCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("is_settled", true);
        api.settleExpenses("eq." + groupId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                executor.execute(() -> splitExpenseDao.settleAllByGroup(groupId));
                callback.onSuccess();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Cannot settle while offline.");
            }
        });
    }

    /** Υπολογισμός balances: θετικό = τους χρωστάνε, αρνητικό = χρωστάνε */
    public static Map<String, Double> calculateBalances(List<SplitExpense> expenses, List<String> memberNames) {
        Map<String, Double> balances = new HashMap<>();
        for (String name : memberNames) balances.put(name, 0.0);
        if (memberNames.isEmpty()) return balances;
        int count = memberNames.size();
        for (SplitExpense exp : expenses) {
            if (exp.isSettled()) continue;
            double share = exp.getAmount() / count;
            String payer = exp.getPaidBy();
            Double pb = balances.get(payer);
            if (pb != null) balances.put(payer, pb + exp.getAmount() - share);
            for (String name : memberNames) {
                if (!name.equals(payer)) {
                    Double b = balances.get(name);
                    if (b != null) balances.put(name, b - share);
                }
            }
        }
        return balances;
    }

    private static SplitGroup toGroup(SplitGroupDto dto) {
        return new SplitGroup(dto.id, dto.ownerId, dto.name, parseDate(dto.createdAt));
    }

    private static SplitExpense toExpense(SplitExpenseDto dto) {
        return new SplitExpense(dto.id, dto.groupId, dto.paidBy,
                dto.amountMinor / 100.0, dto.description, dto.isSettled, parseDate(dto.createdAt));
    }

    private static Date parseDate(String value) {
        if (value == null) return new Date();
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            return fmt.parse(value);
        } catch (Exception e) { return new Date(); }
    }
}
