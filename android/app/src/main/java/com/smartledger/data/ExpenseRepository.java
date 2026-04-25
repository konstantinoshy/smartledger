package com.smartledger.data;

import android.content.Context;

import com.smartledger.api.ApiClient;
import com.smartledger.api.SessionManager;
import com.smartledger.api.SmartLedgerApi;
import com.smartledger.api.dto.CreateExpenseRequest;
import com.smartledger.api.dto.ExpenseDto;
import com.smartledger.models.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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

    private static ExpenseRepository instance;

    private final SessionManager sessionManager;
    private final SmartLedgerApi api;

    private ExpenseRepository(Context context) {
        sessionManager = new SessionManager(context);
        api = ApiClient.getApi(sessionManager);
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
                    callback.onError("Could not load expenses.");
                    return;
                }

                List<Expense> expenses = new ArrayList<>();
                for (ExpenseDto dto : response.body()) {
                    expenses.add(toExpense(dto));
                }
                callback.onSuccess(expenses);
            }

            @Override
            public void onFailure(Call<List<ExpenseDto>> call, Throwable t) {
                callback.onError("Supabase unavailable. Check project URL and network.");
            }
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
                callback.onSuccess(toExpense(response.body().get(0)));
            }

            @Override
            public void onFailure(Call<List<ExpenseDto>> call, Throwable t) {
                callback.onError("Supabase unavailable. Check project URL and network.");
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
