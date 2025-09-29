package com.example.homie.ui.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.homie.R;

public class AccountSettingsActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editPhone;
    private Button btnSave;
    private Button btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Account Settings");
        }

        initViews();
        loadUserData();
        setupClickListeners();
    }

    private void initViews() {
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        btnSave = findViewById(R.id.btn_save);
        btnChangePassword = findViewById(R.id.btn_change_password);
    }

    private void loadUserData() {
        // Load user data from preferences or database
        editName.setText("John Doe");
        editEmail.setText("john.doe@example.com");
        editPhone.setText("+1 (555) 123-4567");
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveAccountInfo());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void saveAccountInfo() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to preferences or send to server
        Toast.makeText(this, "Account information saved", Toast.LENGTH_SHORT).show();
    }

    private void changePassword() {
        // Open password change dialog or activity
        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
