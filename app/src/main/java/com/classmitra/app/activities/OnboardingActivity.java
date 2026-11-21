package com.classmitra.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.classmitra.app.R;
import com.classmitra.app.activities.SplashActivity;
import com.classmitra.app.adapters.OnboardingAdapter;
import com.classmitra.app.auth.LoginActivity;
import com.classmitra.app.databinding.ActivityOnboardingBinding;
import com.classmitra.app.models.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

/**
 * OnboardingActivity handles the introduction of the app's features to the user.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private OnboardingAdapter onboardingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupOnboardingItems();
        setupIndicators();
        setCurrentIndicator(0);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                updateButtons(position);
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (binding.viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
            } else {
                navigateToLogin();
            }
        });

        binding.btnSkip.setOnClickListener(v -> navigateToLogin());
        binding.btnGetStarted.setOnClickListener(v -> navigateToLogin());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_subtitle_1),
                getString(R.string.onboarding_desc_1),
                android.R.drawable.ic_dialog_info
        ));

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_2),
                null,
                getString(R.string.onboarding_desc_2),
                android.R.drawable.ic_menu_upload
        ));

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_3),
                null,
                getString(R.string.onboarding_desc_3),
                android.R.drawable.ic_menu_help
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        binding.viewPager.setAdapter(onboardingAdapter);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.dot_unselected
            ));
            indicators[i].setLayoutParams(layoutParams);
            binding.layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = binding.layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) binding.layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.dot_selected
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.dot_unselected
                ));
            }
        }
    }

    private void updateButtons(int position) {
        if (position == onboardingAdapter.getItemCount() - 1) {
            binding.btnNext.setVisibility(View.GONE);
            binding.btnSkip.setVisibility(View.GONE);
            binding.btnGetStarted.setVisibility(View.VISIBLE);
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.btnSkip.setVisibility(View.VISIBLE);
            binding.btnGetStarted.setVisibility(View.GONE);
        }
    }

    private void navigateToLogin() {
        startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
        finish();
    }
}