package com.smartledger.api;

import com.smartledger.api.dto.AuthRequest;
import com.smartledger.api.dto.AuthResponse;
import com.smartledger.api.dto.CreateExpenseRequest;
import com.smartledger.api.dto.ExpenseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SmartLedgerApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> register(@Body AuthRequest request);

    @POST("auth/v1/token")
    Call<AuthResponse> login(@Query("grant_type") String grantType, @Body AuthRequest request);

    @GET("rest/v1/expenses")
    Call<List<ExpenseDto>> getExpenses(
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/expenses")
    Call<List<ExpenseDto>> createExpense(@Body CreateExpenseRequest request);
}
