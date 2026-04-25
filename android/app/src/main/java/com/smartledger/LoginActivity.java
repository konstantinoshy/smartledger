package com.smartledger;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.api.AuthRepository;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);
        if (authRepository.isLoggedIn()) {
            openMain();
            return;
        }

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);

        findViewById(R.id.btn_google_login).setOnClickListener(v -> authenticate(false));
        findViewById(R.id.btn_register).setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean register) {
        String email = tilEmail.getEditText().getText().toString().trim();
        String password = tilPassword.getEditText().getText().toString();

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            return;
        }

        AuthRepository.AuthCallback callback = new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String email) {
                openMain();
            }

            @Override
            public void onError(String message) {
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            }
        };

        if (register) {
            authRepository.register(email, password, callback);
        } else {
            authRepository.login(email, password, callback);
        }
    }

    private void openMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
