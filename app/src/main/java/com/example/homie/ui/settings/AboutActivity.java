
package com.example.homie.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.homie.R;

public class AboutActivity extends AppCompatActivity {

    private TextView textAppName;
    private TextView textVersion;
    private TextView textBuildDate;
    private TextView textDeveloper;
    private CardView cardTerms;
    private CardView cardPrivacy;
    private CardView cardLicenses;
    private Button btnGithub;
    private Button btnSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("About");
        }

        initViews();
        setupInfo();
        setupClickListeners();
    }

    private void initViews() {
        textAppName = findViewById(R.id.text_app_name);
        textVersion = findViewById(R.id.text_version);
        textBuildDate = findViewById(R.id.text_build_date);
        textDeveloper = findViewById(R.id.text_developer);
        cardTerms = findViewById(R.id.card_terms);
        cardPrivacy = findViewById(R.id.card_privacy);
        cardLicenses = findViewById(R.id.card_licenses);
        btnGithub = findViewById(R.id.btn_github);
        btnSupport = findViewById(R.id.btn_support);
    }

    private void setupInfo() {
        if (textAppName != null) {
            textAppName.setText("Smart Home App");
        }
        if (textVersion != null) {
            textVersion.setText("Version 1.0.0 (Build 2023.10.15)");
        }
        if (textBuildDate != null) {
            textBuildDate.setText("Released: October 15, 2023");
        }
        if (textDeveloper != null) {
            textDeveloper.setText("Developer: Lovable Smart Home Team");
        }
    }

    private void setupClickListeners() {
        if (cardTerms != null) {
            cardTerms.setOnClickListener(v -> openTerms());
        }

        if (cardPrivacy != null) {
            cardPrivacy.setOnClickListener(v -> openPrivacy());
        }

        if (cardLicenses != null) {
            cardLicenses.setOnClickListener(v -> openLicenses());
        }

        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> openGithub());
        }

        if (btnSupport != null) {
            btnSupport.setOnClickListener(v -> openSupport());
        }
    }

    private void openTerms() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/terms"));
        startActivity(intent);
    }

    private void openPrivacy() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/privacy"));
        startActivity(intent);
    }

    private void openLicenses() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/licenses"));
        startActivity(intent);
    }

    private void openGithub() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/github"));
        startActivity(intent);
    }

    private void openSupport() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/support"));
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
