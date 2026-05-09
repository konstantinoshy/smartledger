package com.smartledger;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.api.AuthRepository;

import java.util.concurrent.Executor;

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

        android.view.View btnBiometric = findViewById(R.id.btn_biometric);
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            btnBiometric.setVisibility(android.view.View.VISIBLE);
            btnBiometric.setOnClickListener(v -> showBiometricPrompt());
        } else {
            btnBiometric.setVisibility(android.view.View.GONE);
        }
    }

    private void authenticate(boolean register) {
        String email = tilEmail.getEditText() != null ? tilEmail.getEditText().getText().toString().trim() : "";
        String password = tilPassword.getEditText() != null ? tilPassword.getEditText().getText().toString() : "";

        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
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

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Snackbar.make(findViewById(android.R.id.content),
                        "Authentication error: " + errString, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Snackbar.make(findViewById(android.R.id.content),
                        "Authentication succeeded!", Snackbar.LENGTH_SHORT).show();
                openMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Snackbar.make(findViewById(android.R.id.content),
                        "Authentication failed", Snackbar.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for SmartLedger")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
