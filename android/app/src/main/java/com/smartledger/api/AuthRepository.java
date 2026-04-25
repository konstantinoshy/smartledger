package com.smartledger.api;

import android.content.Context;

import com.smartledger.api.dto.AuthRequest;
import com.smartledger.api.dto.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(String email);
        void onError(String message);
    }

    private final SessionManager sessionManager;
    private final SmartLedgerApi api;

    public AuthRepository(Context context) {
        sessionManager = new SessionManager(context);
        api = ApiClient.getApi(sessionManager);
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void login(String email, String password, AuthCallback callback) {
        authenticate(api.login("password", new AuthRequest(email, password)), callback);
    }

    public void register(String email, String password, AuthCallback callback) {
        authenticate(api.register(new AuthRequest(email, password)), callback);
    }

    private void authenticate(Call<AuthResponse> call, AuthCallback callback) {
        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Authentication failed. Check the API and credentials.");
                    return;
                }

                AuthResponse auth = response.body();
                if (auth.accessToken == null) {
                    callback.onError("Authentication requires email confirmation or returned no session token.");
                    return;
                }

                String email = auth.user != null ? auth.user.email : "";
                String userId = auth.user != null ? auth.user.id : "";
                sessionManager.saveSession(auth.accessToken, auth.refreshToken, userId, email);
                callback.onSuccess(email);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Backend unavailable. Start the API and try again.");
            }
        });
    }
}
