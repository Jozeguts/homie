
package com.example.homie.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homie.R;

public class NotificationsSettingsActivity extends AppCompatActivity {

    private Switch switchDeviceAlerts;
    private Switch switchSecurityAlerts;
    private Switch switchSmartSuggestions;
    private Switch switchWeeklyReports;
    private Switch switchAppUpdates;
    private Button btnSave;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications_settings);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notification Settings");
        }

        preferences = getSharedPreferences("notification_prefs", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupClickListeners();
    }

    private void initViews() {
        switchDeviceAlerts = findViewById(R.id.switch_device_alerts);
        switchSecurityAlerts = findViewById(R.id.switch_security_alerts);
        switchSmartSuggestions = findViewById(R.id.switch_smart_suggestions);
        switchWeeklyReports = findViewById(R.id.switch_weekly_reports);
        switchAppUpdates = findViewById(R.id.switch_app_updates);
        btnSave = findViewById(R.id.btn_save);
    }

    private void loadSettings() {
        switchDeviceAlerts.setChecked(preferences.getBoolean("device_alerts", true));
        switchSecurityAlerts.setChecked(preferences.getBoolean("security_alerts", true));
        switchSmartSuggestions.setChecked(preferences.getBoolean("smart_suggestions", false));
        switchWeeklyReports.setChecked(preferences.getBoolean("weekly_reports", true));
        switchAppUpdates.setChecked(preferences.getBoolean("app_updates", true));
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("device_alerts", switchDeviceAlerts.isChecked());
        editor.putBoolean("security_alerts", switchSecurityAlerts.isChecked());
        editor.putBoolean("smart_suggestions", switchSmartSuggestions.isChecked());
        editor.putBoolean("weekly_reports", switchWeeklyReports.isChecked());
        editor.putBoolean("app_updates", switchAppUpdates.isChecked());
        editor.apply();

        Toast.makeText(this, "Notification preferences saved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
