
package com.example.homie.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homie.R;

public class PrivacySecuritySettingsActivity extends AppCompatActivity {

    private Switch switchTwoFactor;
    private Switch switchDataCollection;
    private Switch switchLocationTracking;
    private Switch switchActivityLog;
    private Button btnSave;
    private Button btnChangePassword;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security_settings);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Privacy & Security");
        }

        preferences = getSharedPreferences("security_prefs", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        switchTwoFactor = findViewById(R.id.switch_two_factor);
        switchDataCollection = findViewById(R.id.switch_data_collection);
        switchLocationTracking = findViewById(R.id.switch_location_tracking);
        switchActivityLog = findViewById(R.id.switch_activity_log);
        btnSave = findViewById(R.id.btn_save);
        btnChangePassword = findViewById(R.id.btn_change_password);
    }

    private void loadSettings() {
        switchTwoFactor.setChecked(preferences.getBoolean("two_factor_auth", false));
        switchDataCollection.setChecked(preferences.getBoolean("data_collection", true));
        switchLocationTracking.setChecked(preferences.getBoolean("location_tracking", false));
        switchActivityLog.setChecked(preferences.getBoolean("activity_log", true));
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveSettings());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("two_factor_auth", switchTwoFactor.isChecked());
        editor.putBoolean("data_collection", switchDataCollection.isChecked());
        editor.putBoolean("location_tracking", switchLocationTracking.isChecked());
        editor.putBoolean("activity_log", switchActivityLog.isChecked());
        editor.apply();

        Toast.makeText(this, "Privacy & security settings saved", Toast.LENGTH_SHORT).show();
    }

    private void changePassword() {
        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
