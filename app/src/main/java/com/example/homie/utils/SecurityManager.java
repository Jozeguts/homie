package com.example.homie.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.biometric.BiometricManager;

public class SecurityManager {
    private static final String PREFS_NAME = "security_prefs";
    private static final String KEY_AUTH = "is_authenticated";
    private static final String KEY_LAST_AUTH_TIME = "last_auth_time";

    private static final long TIMEOUT_DURATION_MS = 20 * 60 * 1000; // 20 minutes

    private final SharedPreferences prefs;

    public SecurityManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isUserAuthenticated() {
        boolean isAuthenticated = prefs.getBoolean(KEY_AUTH, false);
        long lastAuthTime = prefs.getLong(KEY_LAST_AUTH_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if (!isAuthenticated) {
            return false;
        }
        if (currentTime - lastAuthTime > TIMEOUT_DURATION_MS) {
            clearAuthentication();
            return false;
        }
        return true;
    }

    public void setUserAuthenticated(boolean isAuthenticated) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_AUTH, isAuthenticated);
        if (isAuthenticated) {
            editor.putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis());
        } else {
            editor.remove(KEY_LAST_AUTH_TIME);
        }
        editor.apply();
    }

    public void clearAuthentication() {
        prefs.edit()
                .putBoolean(KEY_AUTH, false)
                .remove(KEY_LAST_AUTH_TIME)
                .apply();
    }

    public boolean canAuthenticateWithBiometrics(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public boolean canAuthenticateWithDeviceCredential(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }
}