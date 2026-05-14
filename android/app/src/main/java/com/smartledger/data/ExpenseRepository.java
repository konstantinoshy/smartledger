package com.smartledger.data;

import android.content.Context;

import android.os.Handler;
import android.os.Looper;

import com.smartledger.api.ApiClient;
import com.smartledger.api.SessionManager;
import com.smartledger.api.SmartLedgerApi;
import com.smartledger.api.dto.CreateExpenseRequest;
import com.smartledger.api.dto.ExpenseDto;
import com.smartledger.data.local.AppDatabase;
import com.smartledger.data.local.ExpenseDao;
import com.smartledger.models.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseRepository {

    public interface ExpenseListCallback {
        void onSuccess(List<Expense> expenses);
        void onError(String message);
    }

    public interface ExpenseCallback {
        void onSuccess(Expense expense);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    private static ExpenseRepository instance;

    private final SessionManager sessionManager;
    private final SmartLedgerApi api;
    private final ExpenseDao expenseDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private ExpenseRepository(Context context) {
        sessionManager = new SessionManager(context);
        api = ApiClient.getApi(sessionManager);
        expenseDao = AppDatabase.getDatabase(context).expenseDao();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ExpenseRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ExpenseRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void getExpenses(ExpenseListCallback callback) {
        api.getExpenses("*", "spent_at.desc").enqueue(new Callback<List<ExpenseDto>>() {
            @Override
            public void onResponse(Call<List<ExpenseDto>> call, Response<List<ExpenseDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    loadFromLocalDatabase(callback, "Server returned error. Showing offline data.");
                    return;
                }

                List<Expense> expenses = new ArrayList<>();
                for (ExpenseDto dto : response.body()) {
                    expenses.add(toExpense(dto));
                }

                // Αποθήκευση στην τοπική βάση Room (Cache)
                executor.execute(() -> {
                    expenseDao.clearAll();
                    expenseDao.insertAll(expenses);
                });

                callback.onSuccess(expenses);
            }

            @Override
            public void onFailure(Call<List<ExpenseDto>> call, Throwable t) {
                loadFromLocalDatabase(callback, "You are offline. Showing cached expenses.");
            }
        });
    }

    private void loadFromLocalDatabase(ExpenseListCallback callback, String errorMessage) {
        executor.execute(() -> {
            List<Expense> localExpenses = expenseDao.getAllExpenses();
            mainHandler.post(() -> {
                if (!localExpenses.isEmpty()) {
                    // Δείχνουμε τα cached δεδομένα αλλά ειδοποιούμε τον χρήστη
                    callback.onError(errorMessage);
                    callback.onSuccess(localExpenses);
                } else {
                    callback.onError("Network unavailable and no local data found.");
                }
            });
        });
    }

    public void addExpense(double amount, String category, String description, ExpenseCallback callback) {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onError("Missing authenticated user. Log in again.");
            return;
        }

        int amountMinor = (int) Math.round(amount * 100);
        CreateExpenseRequest request = new CreateExpenseRequest(
                userId,
                amountMinor,
                "USD",
                category,
                description,
                formatDate(new Date())
        );

        api.createExpense(request).enqueue(new Callback<List<ExpenseDto>>() {
            @Override
            public void onResponse(Call<List<ExpenseDto>> call, Response<List<ExpenseDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    callback.onError("Could not save expense.");
                    return;
                }
                Expense newExpense = toExpense(response.body().get(0));

                // Προσθήκη και στην τοπική βάση κατευθείαν για γρήγορη απόκριση
                executor.execute(() -> expenseDao.insert(newExpense));

                callback.onSuccess(newExpense);
            }

            @Override
            public void onFailure(Call<List<ExpenseDto>> call, Throwable t) {
                callback.onError("Cannot add expenses while offline.");
            }
        });
    }

    public void deleteExpense(Expense expense, DeleteCallback callback) {
        api.deleteExpense("eq." + expense.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Could not delete expense from server.");
                    return;
                }
                executor.execute(() -> expenseDao.delete(expense));
                callback.onSuccess();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Cannot delete expenses while offline.");
            }
        });
    }

    private static Expense toExpense(ExpenseDto dto) {
        Date spentAt = parseDate(dto.spentAt);
        Date createdAt = parseDate(dto.createdAt);
        return new Expense(
                dto.id,
                dto.userId,
                dto.amountMinor / 100.0,
                dto.category,
                dto.description,
                spentAt,
                createdAt
        );
    }

    private static Date parseDate(String value) {
        if (value == null) {
            return new Date();
        }
        try {
            return newIsoFormat().parse(value);
        } catch (Exception e) {
            return new Date();
        }
    }

    private static String formatDate(Date date) {
        return newIsoFormat().format(date);
    }

    private static SimpleDateFormat newIsoFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}