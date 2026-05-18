package com.smartledger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.smartledger.api.ApiClient;
import com.smartledger.api.BiometricTokenManager;
import com.smartledger.api.SessionManager;
import com.smartledger.api.SmartLedgerApi;
import com.smartledger.api.dto.AuthResponse;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "smartledger_preferences";
    private static final String KEY_DARK_MODE = "dark_mode_override";
    private static final String KEY_MONTHLY_BUDGET_PREFIX = "monthly_budget_";
    private static final float DEFAULT_BUDGET = 5000f;

    private SessionManager sessionManager;
    private BiometricTokenManager biometricTokenManager;
    private SharedPreferences preferences;
    private SmartLedgerApi api;

    private TextView tvEmail;
    private TextView tvBudgetValue;
    private TextView tvVersionFooter;
    private MaterialSwitch switchFingerprint;
    private MaterialSwitch switchDarkMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sessionManager = new SessionManager(requireContext());
        biometricTokenManager = new BiometricTokenManager(requireContext());
        preferences = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        api = ApiClient.getApi(sessionManager);

        // Bind views
        tvEmail = view.findViewById(R.id.tv_email);
        tvBudgetValue = view.findViewById(R.id.tv_budget_value);
        tvVersionFooter = view.findViewById(R.id.tv_version_footer);
        switchFingerprint = view.findViewById(R.id.switch_fingerprint);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        setupAccountSection();
        setupSecuritySection(view);
        setupPreferencesSection(view);
        setupMoreSection(view);
        setupVersionFooter();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh budget display in case it was changed elsewhere
        refreshBudgetDisplay();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshBudgetDisplay();
        }
    }

    // ─── ACCOUNT ────────────────────────────────────────────────────────

    private void setupAccountSection() {
        String email = sessionManager.getEmail();
        tvEmail.setText(email != null ? email : "—");
    }

    // ─── SECURITY ───────────────────────────────────────────────────────

    private void setupSecuritySection(View view) {
        // Fingerprint toggle
        boolean biometricAvailable = BiometricManager.from(requireContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;

        boolean fingerprintEnabled = biometricAvailable
                && biometricTokenManager.hasSavedSession();

        switchFingerprint.setChecked(fingerprintEnabled);

        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return; // ignore programmatic changes

            if (isChecked) {
                showEnableFingerprintDialog();
            } else {
                biometricTokenManager.clear();
                if (getView() != null) {
                    Snackbar.make(getView(), "Fingerprint login disabled",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        // Dark-mode toggle
        boolean darkModeOn = AppCompatDelegate.getDefaultNightMode()
                == AppCompatDelegate.MODE_NIGHT_YES;
        switchDarkMode.setChecked(darkModeOn);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;

            int mode = isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

            preferences.edit().putInt(KEY_DARK_MODE, mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
            requireActivity().recreate();
        });

        // Change password row
        view.findViewById(R.id.row_change_password)
                .setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showEnableFingerprintDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enable fingerprint login?")
                .setMessage("You will be asked to verify your fingerprint next time you log in.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    // Copy current session tokens into biometric-encrypted storage
                    biometricTokenManager.saveTokens(
                            sessionManager.getToken(),
                            sessionManager.getRefreshToken(),
                            sessionManager.getUserId(),
                            sessionManager.getEmail()
                    );
                    if (getView() != null) {
                        Snackbar.make(getView(), "Fingerprint login enabled",
                                Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        switchFingerprint.setChecked(false))
                .setOnCancelListener(dialog -> switchFingerprint.setChecked(false))
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.til_new_password);
        TextInputEditText editNewPassword = dialogView.findViewById(R.id.edit_new_password);
        TextInputLayout tilConfirmPassword = dialogView.findViewById(R.id.til_confirm_password);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.edit_confirm_password);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String newPass = editNewPassword.getText() != null
                            ? editNewPassword.getText().toString() : "";
                    String confirmPass = editConfirmPassword.getText() != null
                            ? editConfirmPassword.getText().toString() : "";

                    tilNewPassword.setError(null);
                    tilConfirmPassword.setError(null);

                    if (newPass.length() < 8) {
                        tilNewPassword.setError("Password must be at least 8 characters");
                        return;
                    }
                    if (!newPass.equals(confirmPass)) {
                        tilConfirmPassword.setError("Passwords do not match");
                        return;
                    }

                    // Call Supabase PUT /auth/v1/user
                    Map<String, Object> body = new HashMap<>();
                    body.put("password", newPass);

                    api.updateUser(body).enqueue(new Callback<AuthResponse>() {
                        @Override
                        public void onResponse(Call<AuthResponse> call,
                                               Response<AuthResponse> response) {
                            if (!response.isSuccessful()) {
                                if (getView() != null) {
                                    Snackbar.make(getView(),
                                            "Failed to update password. Try again.",
                                            Snackbar.LENGTH_LONG).show();
                                }
                                return;
                            }
                            dialog.dismiss();
                            if (getView() != null) {
                                Snackbar.make(getView(),
                                        "Password updated successfully",
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<AuthResponse> call, Throwable t) {
                            if (getView() != null) {
                                Snackbar.make(getView(),
                                        "Network error. Check your connection.",
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                });
    }

    // ─── PREFERENCES ────────────────────────────────────────────────────

    private void setupPreferencesSection(View root) {
        refreshBudgetDisplay();
        // Φόρτωσε budget από cloud (fallback σε τοπικό cache)
        loadBudgetFromCloud();

        root.findViewById(R.id.row_budget).setOnClickListener(v -> showBudgetDialog());
    }

    private String getBudgetKey() {
        String userId = sessionManager.getUserId();
        return KEY_MONTHLY_BUDGET_PREFIX + (userId != null ? userId : "default");
    }

    private void refreshBudgetDisplay() {
        float budget = preferences.getFloat(getBudgetKey(), DEFAULT_BUDGET);
        tvBudgetValue.setText(String.format(Locale.getDefault(), "$%,.2f", budget));
    }

    /**
     * Φορτώνει το budget από το Supabase cloud.
     * Αν πετύχει, ενημερώνει το τοπικό cache.
     * Αν αποτύχει (offline), δείχνει το cached budget.
     */
    private void loadBudgetFromCloud() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        api.getUserSettings("eq." + userId, "*").enqueue(
                new Callback<java.util.List<com.smartledger.api.dto.UserSettingsDto>>() {
                    @Override
                    public void onResponse(Call<java.util.List<com.smartledger.api.dto.UserSettingsDto>> call,
                                           Response<java.util.List<com.smartledger.api.dto.UserSettingsDto>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            float cloudBudget = response.body().get(0).monthlyBudgetMinor / 100f;
                            // Αποθήκευση τοπικά ως cache
                            preferences.edit()
                                    .putFloat(getBudgetKey(), cloudBudget)
                                    .apply();
                            refreshBudgetDisplay();
                        }
                        // Αν δεν υπάρχει εγγραφή στο cloud, κρατάμε το τοπικό default
                    }

                    @Override
                    public void onFailure(Call<java.util.List<com.smartledger.api.dto.UserSettingsDto>> call,
                                          Throwable t) {
                        // Offline — χρησιμοποιούμε το τοπικό cache (ήδη εμφανίζεται)
                    }
                });
    }

    private void showBudgetDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_budget, null);

        TextInputLayout tilBudget = dialogView.findViewById(R.id.til_budget);
        TextInputEditText editBudget = dialogView.findViewById(R.id.edit_budget);

        float currentBudget = preferences.getFloat(getBudgetKey(), DEFAULT_BUDGET);
        editBudget.setText(String.valueOf(currentBudget));
        editBudget.selectAll();

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Monthly Budget")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String input = editBudget.getText() != null
                            ? editBudget.getText().toString().trim() : "";

                    if (input.isEmpty()) {
                        tilBudget.setError("Enter a budget amount");
                        return;
                    }

                    try {
                        float value = Float.parseFloat(input);
                        if (value <= 0) {
                            tilBudget.setError("Budget must be greater than 0");
                            return;
                        }

                        // Αποθήκευση τοπικά (cache)
                        preferences.edit()
                                .putFloat(getBudgetKey(), value)
                                .apply();
                        refreshBudgetDisplay();
                        dialog.dismiss();

                        // Αποθήκευση στο cloud (Supabase upsert)
                        saveBudgetToCloud(value);

                        if (getView() != null) {
                            Snackbar.make(getView(), "Budget updated",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        tilBudget.setError("Invalid number");
                    }
                });
    }

    /**
     * Αποθηκεύει το budget στο Supabase (upsert).
     * Αν αποτύχει, το budget παραμένει αποθηκευμένο τοπικά.
     */
    private void saveBudgetToCloud(float budgetValue) {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        int budgetMinor = Math.round(budgetValue * 100);

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("monthly_budget_minor", budgetMinor);
        body.put("currency", "USD");

        api.upsertUserSettings(body).enqueue(
                new Callback<java.util.List<com.smartledger.api.dto.UserSettingsDto>>() {
                    @Override
                    public void onResponse(Call<java.util.List<com.smartledger.api.dto.UserSettingsDto>> call,
                                           Response<java.util.List<com.smartledger.api.dto.UserSettingsDto>> response) {
                        // Αποθηκεύτηκε στο cloud — τίποτα επιπλέον
                    }

                    @Override
                    public void onFailure(Call<java.util.List<com.smartledger.api.dto.UserSettingsDto>> call,
                                          Throwable t) {
                        // Offline — το budget αποθηκεύτηκε μόνο τοπικά
                        // Θα συγχρονιστεί στο cloud την επόμενη φορά
                    }
                });
    }

    // ─── MORE ───────────────────────────────────────────────────────────

    private void setupMoreSection(View root) {
        root.findViewById(R.id.row_about).setOnClickListener(v -> showAboutDialog());
        root.findViewById(R.id.row_logout).setOnClickListener(v -> showLogoutDialog());
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("SmartLedger")
                .setMessage("Version " + BuildConfig.VERSION_NAME
                        + "\n\nYour personal finance companion.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage("You will need to sign in again.")
                .setPositiveButton("Log Out", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        // Only clear the active session — keep biometric enrollment intact
        // so the fingerprint button appears on the next login screen
        sessionManager.clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setupVersionFooter() {
        tvVersionFooter.setText(
                String.format("SmartLedger v%s", BuildConfig.VERSION_NAME));
    }
}
