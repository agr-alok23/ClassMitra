package com.classmitra.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.classmitra.app.R;
import com.classmitra.app.auth.RoleSelectionActivity;
import com.classmitra.app.databinding.ActivitySplashBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity serves as the entry point of the application,
 * providing a brand introduction with animations.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme preference before super.onCreate
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        // Using ViewBinding to access layout components safely
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Load predefined animations from resources
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation floating = AnimationUtils.loadAnimation(this, R.anim.float_anim);

        // Start animations for the UI elements
        // Logo and text will fade in smoothly
        binding.ivLogo.startAnimation(fadeIn);
        binding.tvAppName.startAnimation(fadeIn);
        binding.tvTagline.startAnimation(fadeIn);
        
        // The mascot placeholder will have a gentle floating effect
        binding.ivMascot.startAnimation(floating);

        // Delay navigation by 2500ms (2.5 seconds) using a Handler
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Intent intent;
            if (currentUser != null) {
                // User is signed in, skip onboarding and login
                intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
            } else {
                // No user is signed in, show onboarding
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            startActivity(intent);
            // Finish splash so the user cannot return to it via the back button
            finish();
        }, 2500);
    }
}