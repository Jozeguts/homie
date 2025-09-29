package com.example.homie.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homie.R;
import com.example.homie.adapter.SettingsAdapter;
import com.example.homie.model.SettingsItem;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements SettingsAdapter.OnSettingClickListener {

    private RecyclerView settingsRecyclerView;
    private SettingsAdapter settingsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        initViews();
        setupSettingsList();
    }

    private void initViews() {
        settingsRecyclerView = findViewById(R.id.settings_recycler_view);
    }

    private void setupSettingsList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        settingsRecyclerView.setLayoutManager(layoutManager);

        List<SettingsItem> settingsItems = createSettingsItems();
        settingsAdapter = new SettingsAdapter(settingsItems, this);
        settingsRecyclerView.setAdapter(settingsAdapter);
    }

    private List<SettingsItem> createSettingsItems() {
        List<SettingsItem> items = new ArrayList<>();

        items.add(new SettingsItem(
                R.drawable.ic_account,
                "Account",
                "Manage your account details",
                AccountSettingsActivity.class));

        items.add(new SettingsItem(
                R.drawable.ic_notifications,
                "Notifications",
                "Configure notification preferences",
                NotificationsSettingsActivity.class));

        items.add(new SettingsItem(
                R.drawable.ic_security,
                "Privacy & Security",
                "Control your privacy settings",
                PrivacySecuritySettingsActivity.class));

        items.add(new SettingsItem(
                R.drawable.ic_help,
                "Help & Support",
                "Get help with your smart home",
                HelpSupportActivity.class));

        items.add(new SettingsItem(
                R.drawable.ic_info,
                "About",
                "Version information and legal details",
                AboutActivity.class));

        return items;
    }

    @Override
    public void onSettingClick(SettingsItem item) {
        Intent intent = new Intent(this, item.getTargetActivity());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
