package com.smartledger.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Manages biometric-linked session tokens using AES-256 encrypted storage.
 * Tokens saved here are only accessible after successful biometric authentication.
 * This is separate from {@link SessionManager} which uses plain SharedPreferences.
 */
public class BiometricTokenManager {

    private static final String PREFS_NAME = "smartledger_biometric_session";
    private static final String KEY_TOKEN = "bio_token";
    private static final String KEY_REFRESH_TOKEN = "bio_refresh_token";
    private static final String KEY_EMAIL = "bio_email";
    private static final String KEY_USER_ID = "bio_user_id";

    private final SharedPreferences encryptedPrefs;

    public BiometricTokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create encrypted preferences for biometric session", e);
        }
    }

    /**
     * Save session tokens after the user opts in to biometric login.
     */
    public void saveTokens(String token, String refreshToken, String userId, String email) {
        encryptedPrefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public String getToken() {
        return encryptedPrefs.getString(KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return encryptedPrefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return encryptedPrefs.getString(KEY_EMAIL, null);
    }

    /**
     * Returns true if the user has previously enrolled biometric login
     * and tokens are stored in the encrypted store.
     */
    public boolean hasSavedSession() {
        return getToken() != null;
    }

    /**
     * Clears all encrypted tokens. Called on logout.
     */
    public void clear() {
        encryptedPrefs.edit().clear().apply();
    }
}
