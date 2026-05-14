package com.smartledger;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.api.AuthRepository;
import com.smartledger.api.BiometricTokenManager;
import com.smartledger.api.SessionManager;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private BiometricTokenManager biometricTokenManager;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);
        biometricTokenManager = new BiometricTokenManager(this);

        // If already logged in via normal session, skip login screen
        if (authRepository.isLoggedIn()) {
            openMain();
            return;
        }

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);

        findViewById(R.id.btn_google_login).setOnClickListener(v -> authenticate(false));
        findViewById(R.id.btn_register).setOnClickListener(v -> authenticate(true));

        // Show biometric button only if:
        // 1. The device supports biometrics AND
        // 2. The user has previously enrolled (saved tokens exist)
        android.view.View btnBiometric = findViewById(R.id.btn_biometric);
        BiometricManager biometricManager = BiometricManager.from(this);
        boolean biometricsAvailable = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;

        if (biometricsAvailable && biometricTokenManager.hasSavedSession()) {
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

        AuthRepository.AuthCallback successCallback = new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String email) {
                // If a different account logs in, clear stale biometric tokens
                String savedEmail = biometricTokenManager.getEmail();
                if (savedEmail != null && !savedEmail.equals(email)) {
                    biometricTokenManager.clear();
                }
                // After successful password login, offer biometric enrollment
                offerBiometricEnrollment();
            }

            @Override
            public void onError(String message) {
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            }
        };

        if (register) {
            authRepository.register(email, password, successCallback);
        } else {
            // For login attempts, wrap with a dialog that offers registration on failure
            authRepository.login(email, password, new AuthRepository.AuthCallback() {
                @Override
                public void onSuccess(String email) {
                    successCallback.onSuccess(email);
                }

                @Override
                public void onError(String message) {
                    new MaterialAlertDialogBuilder(LoginActivity.this)
                            .setTitle("Account not found")
                            .setMessage("No account exists for this email. Would you like to create one?")
                            .setPositiveButton("Create account", (dialog, which) -> authenticate(true))
                            .setNegativeButton("Cancel", (dialog, which) ->
                                    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show())
                            .show();
                }
            });
        }
    }

    /**
     * After a successful email/password login, check if the device supports
     * biometrics and offer the user to enable fingerprint login for next time.
     */
    private void offerBiometricEnrollment() {
        BiometricManager biometricManager = BiometricManager.from(this);
        boolean biometricsAvailable = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;

        // If device doesn't support biometrics or user already enrolled (same account), skip
        if (!biometricsAvailable || biometricTokenManager.hasSavedSession()) {
            openMain();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Fingerprint Login")
                .setMessage("Would you like to use your fingerprint to log in next time? " +
                        "Your session will be stored securely using AES-256 encryption.")
                .setIcon(R.drawable.ic_fingerprint)
                .setPositiveButton("Enable", (dialog, which) -> {
                    // Save current session tokens to encrypted biometric store
                    biometricTokenManager.saveTokens(
                            sessionManager.getToken(),
                            sessionManager.getRefreshToken(),
                            sessionManager.getUserId(),
                            sessionManager.getEmail()
                    );
                    Snackbar.make(findViewById(android.R.id.content),
                            "Fingerprint login enabled!", Snackbar.LENGTH_SHORT).show();
                    openMain();
                })
                .setNegativeButton("Not now", (dialog, which) -> openMain())
                .setCancelable(false)
                .show();
    }

    private void openMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    /**
     * Show the system biometric prompt. On success, restore the encrypted
     * tokens into the active SessionManager and navigate to the main screen.
     */
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

                // Restore the encrypted session into the active SessionManager
                sessionManager.saveSession(
                        biometricTokenManager.getToken(),
                        biometricTokenManager.getRefreshToken(),
                        biometricTokenManager.getUserId(),
                        biometricTokenManager.getEmail()
                );

                Snackbar.make(findViewById(android.R.id.content),
                        "Welcome back, " + biometricTokenManager.getEmail() + "!",
                        Snackbar.LENGTH_SHORT).show();
                openMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Snackbar.make(findViewById(android.R.id.content),
                        "Fingerprint not recognized. Try again.", Snackbar.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Authenticate to access " + biometricTokenManager.getEmail())
                .setNegativeButtonText("Use password instead")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
