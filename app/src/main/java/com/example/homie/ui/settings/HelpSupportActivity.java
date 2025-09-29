
package com.example.homie.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.homie.R;

public class HelpSupportActivity extends AppCompatActivity {

    private CardView cardFaq;
    private CardView cardContactSupport;
    private CardView cardUserGuide;
    private CardView cardCommunity;
    private TextView textVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Help & Support");
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        cardFaq = findViewById(R.id.card_faq);
        cardContactSupport = findViewById(R.id.card_contact_support);
        cardUserGuide = findViewById(R.id.card_user_guide);
        cardCommunity = findViewById(R.id.card_community);
        textVersion = findViewById(R.id.text_version);

        if (textVersion != null) {
            textVersion.setText("Version 1.0.0 (Build 2023.10.15)");
        }
    }

    private void setupClickListeners() {
        if (cardFaq != null) {
            cardFaq.setOnClickListener(v -> openFaq());
        }

        if (cardContactSupport != null) {
            cardContactSupport.setOnClickListener(v -> contactSupport());
        }

        if (cardUserGuide != null) {
            cardUserGuide.setOnClickListener(v -> openUserGuide());
        }

        if (cardCommunity != null) {
            cardCommunity.setOnClickListener(v -> openCommunity());
        }
    }

    private void openFaq() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/faq"));
        startActivity(intent);
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@smarthome.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Smart Home App Support");
        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    private void openUserGuide() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/user-guide"));
        startActivity(intent);
    }

    private void openCommunity() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://example.com/community"));
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
