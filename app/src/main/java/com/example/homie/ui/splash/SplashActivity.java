package com.example.homie.ui.splash;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.homie.MainActivity;
import com.example.homie.R;
import com.example.homie.ui.auth.AuthActivity;
import com.example.homie.utils.SecurityManager;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000; // 3 seconds

    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView versionTextView;
    private ProgressBar loadingProgressBar;
    private SecurityManager securityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        securityManager = new SecurityManager(this);

        initViews();
        startAnimations();
        initializeApp();
    }

    private void initViews() {
        logoImageView = findViewById(R.id.logo_image_view);
        appNameTextView = findViewById(R.id.app_name_text_view);
        versionTextView = findViewById(R.id.version_text_view);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);

        // Set version text
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionTextView.setText("Version " + versionName);
        } catch (Exception e) {
            versionTextView.setText("Version 1.0.0");
        }
    }

    private void startAnimations() {
        // Logo animations
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        logoFadeIn.setDuration(1000);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoImageView, "scaleX", 0.5f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 0.5f, 1f);
        logoScaleX.setDuration(1000);
        logoScaleY.setDuration(1000);

        // App name slide-up and fade
        ObjectAnimator nameSlideUp = ObjectAnimator.ofFloat(appNameTextView, "translationY", 100f, 0f);
        ObjectAnimator nameFadeIn = ObjectAnimator.ofFloat(appNameTextView, "alpha", 0f, 1f);
        nameSlideUp.setDuration(800);
        nameFadeIn.setDuration(800);
        nameSlideUp.setStartDelay(500);
        nameFadeIn.setStartDelay(500);

        // Version fade-in
        ObjectAnimator versionFadeIn = ObjectAnimator.ofFloat(versionTextView, "alpha", 0f, 1f);
        versionFadeIn.setDuration(600);
        versionFadeIn.setStartDelay(1000);

        // Progress bar fade-in and spin
        ObjectAnimator progressFadeIn = ObjectAnimator.ofFloat(loadingProgressBar, "alpha", 0f, 1f);
        progressFadeIn.setDuration(500);
        progressFadeIn.setStartDelay(1500);

        ObjectAnimator progressSpin = ObjectAnimator.ofFloat(loadingProgressBar, "rotation", 0f, 360f);
        progressSpin.setDuration(2000);
        progressSpin.setRepeatCount(ObjectAnimator.INFINITE);
        progressSpin.setStartDelay(1500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(logoFadeIn, logoScaleX, logoScaleY, nameSlideUp, nameFadeIn, versionFadeIn,
                progressFadeIn);
        animatorSet.start();

        progressSpin.start();
    }

    private void initializeApp() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (securityManager.isUserAuthenticated()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, AuthActivity.class);
            }

            startActivity(intent);

            // Use correct transition method based on API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                        android.R.anim.fade_in, android.R.anim.fade_out);
            }

            finish();
        }, SPLASH_DURATION);
    }

    @SuppressWarnings("deprecation")
    private void voidCallOverridePendingTransition() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
