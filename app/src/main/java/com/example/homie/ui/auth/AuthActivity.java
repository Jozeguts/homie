package com.example.homie.ui.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.homie.MainActivity;
import com.example.homie.R;
import com.example.homie.utils.SecurityManager;

import java.util.concurrent.Executor;

public class AuthActivity extends AppCompatActivity {

    private SecurityManager securityManager;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        securityManager = new SecurityManager(this);

        // Check if authentication is already valid
        if (securityManager.isUserAuthenticated()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        Button authButton = findViewById(R.id.auth_button);
        authButton.setOnClickListener(v -> authenticateUser());

        // Set up BiometricPrompt
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(AuthActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                securityManager.setUserAuthenticated(true);
                startActivity(new Intent(AuthActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(AuthActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        // Configure PromptInfo
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("App Authentication")
                .setSubtitle("Authenticate using your device credentials");

        // Handle API level differences
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.BIOMETRIC_WEAK |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29
            builder.setDeviceCredentialAllowed(true);
        } else { // Below API 29
            builder.setNegativeButtonText("Cancel");
        }

        promptInfo = builder.build();
    }

    private void authenticateUser() {
        // Check if biometrics or device credentials are available
        if (securityManager.canAuthenticateWithBiometrics(this) ||
                securityManager.canAuthenticateWithDeviceCredential(this)) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this,
                    "No authentication methods available. Please set up a PIN, pattern, or biometric on your device.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
