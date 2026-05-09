package com.smartledger.api;

import com.smartledger.api.dto.AuthRequest;
import com.smartledger.api.dto.AuthResponse;
import com.smartledger.api.dto.CreateExpenseRequest;
import com.smartledger.api.dto.ExpenseDto;
import com.smartledger.api.dto.PortfolioAssetDto;
import com.smartledger.api.dto.PortfolioTransactionDto;
import com.smartledger.api.dto.SplitExpenseDto;
import com.smartledger.api.dto.SplitGroupDto;
import com.smartledger.api.dto.SplitMemberDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SmartLedgerApi {

    // ─── Auth ───────────────────────────────────────
    @POST("auth/v1/signup")
    Call<AuthResponse> register(@Body AuthRequest request);

    @POST("auth/v1/token")
    Call<AuthResponse> login(@Query("grant_type") String grantType, @Body AuthRequest request);

    // ─── Expenses ───────────────────────────────────
    @GET("rest/v1/expenses")
    Call<List<ExpenseDto>> getExpenses(
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/expenses")
    Call<List<ExpenseDto>> createExpense(@Body CreateExpenseRequest request);

    // ─── Split Groups ───────────────────────────────
    @GET("rest/v1/split_groups")
    Call<List<SplitGroupDto>> getGroups(
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/split_groups")
    Call<List<SplitGroupDto>> createGroup(@Body Map<String, Object> body);

    @DELETE("rest/v1/split_groups")
    Call<Void> deleteGroup(@Query("id") String idFilter);

    // ─── Split Members ──────────────────────────────
    @GET("rest/v1/split_members")
    Call<List<SplitMemberDto>> getMembers(
            @Query("group_id") String groupIdFilter,
            @Query("select") String select
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/split_members")
    Call<List<SplitMemberDto>> addMember(@Body Map<String, Object> body);

    // ─── Split Expenses ─────────────────────────────
    @GET("rest/v1/split_expenses")
    Call<List<SplitExpenseDto>> getSplitExpenses(
            @Query("group_id") String groupIdFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/split_expenses")
    Call<List<SplitExpenseDto>> createSplitExpense(@Body Map<String, Object> body);

    @PATCH("rest/v1/split_expenses")
    Call<Void> settleExpenses(
            @Query("group_id") String groupIdFilter,
            @Body Map<String, Object> body
    );

    // ─── Portfolio Assets ───────────────────────────
    @GET("rest/v1/portfolio_assets")
    Call<List<PortfolioAssetDto>> getPortfolioAssets(
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation,resolution=merge-duplicates")
    @POST("rest/v1/portfolio_assets")
    Call<List<PortfolioAssetDto>> upsertPortfolioAsset(@Body Map<String, Object> body);

    @DELETE("rest/v1/portfolio_assets")
    Call<Void> deletePortfolioAsset(@Query("id") String idFilter);

    // ─── Portfolio Transactions ─────────────────────
    @GET("rest/v1/portfolio_transactions")
    Call<List<PortfolioTransactionDto>> getPortfolioTransactions(
            @Query("select") String select,
            @Query("order") String order
    );

    @Headers("Prefer: return=representation")
    @POST("rest/v1/portfolio_transactions")
    Call<List<PortfolioTransactionDto>> createPortfolioTransaction(@Body Map<String, Object> body);
}
