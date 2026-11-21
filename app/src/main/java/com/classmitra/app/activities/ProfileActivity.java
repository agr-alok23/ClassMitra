package com.classmitra.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.classmitra.app.R;
import com.classmitra.app.auth.LoginActivity;
import com.classmitra.app.databinding.ActivityProfileBinding;
import com.classmitra.app.utils.UIUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * ProfileActivity handles user profile view and app settings.
 */
public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private static final String PREFS_NAME = "settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PROFILE_PIC = "profile_pic_uri";

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    saveProfilePicture(uri);
                    binding.ivProfilePic.setImageURI(uri);
                    binding.ivProfilePic.setImageTintList(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupProfileData();
        setupProfileItems();
        setupThemeSwitch();
        setupClickListeners();
        loadProfilePicture();

        if (getIntent().getBooleanExtra("OPEN_NOTIFICATIONS", false)) {
            startActivity(new Intent(this, NotificationsActivity.class));
        }
    }

    private void loadProfilePicture() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(KEY_PROFILE_PIC, null);
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            binding.ivProfilePic.setImageURI(uri);
            binding.ivProfilePic.setImageTintList(null);
        }
    }

    private void saveProfilePicture(Uri uri) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_PROFILE_PIC, uri.toString()).apply();
    }

    private void setupThemeSwitch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        binding.switchDarkMode.setChecked(isDarkMode);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void setupProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            binding.tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            binding.tvUserEmail.setText(user.getEmail());
        }
        
        String role = getIntent().getStringExtra("ROLE");
        if (role != null) {
            binding.tvUserRole.setText(role);
            if ("TEACHER".equalsIgnoreCase(role)) {
                binding.itemStudyStreak.getRoot().setVisibility(View.GONE);
                binding.itemQuizPerformance.tvSettingTitle.setText("Quizzes Created");
                binding.itemFlashcardsCreated.tvSettingTitle.setText("Flashcard Sets Created");
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.layoutToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.layoutToolbar.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupProfileItems() {
        binding.itemEditProfile.tvSettingTitle.setText("Edit Profile");
        binding.itemChangePassword.tvSettingTitle.setText("Change Password");
        binding.itemNotifications.tvSettingTitle.setText("Notifications");
        
        binding.itemEditProfile.ivSettingIcon.setImageResource(android.R.drawable.ic_menu_edit);
        binding.itemChangePassword.ivSettingIcon.setImageResource(android.R.drawable.ic_lock_idle_lock);
        binding.itemNotifications.ivSettingIcon.setImageResource(android.R.drawable.ic_popup_reminder);

        binding.itemStudyStreak.tvSettingTitle.setText("Study Streak");
        binding.itemQuizPerformance.tvSettingTitle.setText("Quiz Performance");
        binding.itemFlashcardsCreated.tvSettingTitle.setText("Flashcards Created");

        binding.itemStudyStreak.ivSettingIcon.setImageResource(android.R.drawable.btn_star_big_on);
        binding.itemQuizPerformance.ivSettingIcon.setImageResource(android.R.drawable.ic_menu_help);
        binding.itemFlashcardsCreated.ivSettingIcon.setImageResource(android.R.drawable.ic_menu_view);
        
        binding.itemPrivacy.tvSettingTitle.setText("Privacy Policy");
        binding.itemAbout.tvSettingTitle.setText("About ClassMitra");
        binding.itemRate.tvSettingTitle.setText("Rate App");
        binding.itemLogout.tvSettingTitle.setText("Logout");

        binding.itemPrivacy.ivSettingIcon.setImageResource(android.R.drawable.ic_lock_lock);
        binding.itemAbout.ivSettingIcon.setImageResource(android.R.drawable.ic_dialog_info);
        binding.itemRate.ivSettingIcon.setImageResource(android.R.drawable.btn_star);
        binding.itemLogout.ivSettingIcon.setImageResource(android.R.drawable.ic_lock_power_off);
    }

    private void setupClickListeners() {
        binding.itemLogout.getRoot().setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());

        binding.itemEditProfile.getRoot().setOnClickListener(v -> showEditProfileDialog());
        binding.itemNotifications.getRoot().setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
        binding.profileCard.setOnClickListener(v -> showProfilePictureOptions());
        binding.itemChangePassword.getRoot().setOnClickListener(v -> showChangePasswordDialog());
        
        binding.itemStudyStreak.getRoot().setOnClickListener(v -> UIUtils.showComingSoon(this, "Study Streak"));
        binding.itemQuizPerformance.getRoot().setOnClickListener(v -> {
            String role = getIntent().getStringExtra("ROLE");
            if ("TEACHER".equalsIgnoreCase(role)) {
                UIUtils.showComingSoon(this, "Quizzes Created");
            } else {
                UIUtils.showComingSoon(this, "Quiz Performance");
            }
        });
        binding.itemFlashcardsCreated.getRoot().setOnClickListener(v -> UIUtils.showComingSoon(this, "Flashcards"));
        binding.itemPrivacy.getRoot().setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));
        binding.itemAbout.getRoot().setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        binding.itemRate.getRoot().setOnClickListener(v -> openPlayStore());
    }

    private void openPlayStore() {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            new AlertDialog.Builder(this)
                    .setTitle("Rate App")
                    .setMessage("ClassMitra is not yet published on Google Play. Thank you for your support!")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void showProfilePictureOptions() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void showEditProfileDialog() {
        EditText etFullName = new EditText(this);
        etFullName.setHint("Full Name");
        EditText etEmail = new EditText(this);
        etEmail.setHint("Update Email");
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);
        layout.addView(etFullName);
        layout.addView(etEmail);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            etFullName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
            etEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Profile")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String name = etFullName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    
                    if (user != null) {
                        if (!name.isEmpty()) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    binding.tvUserName.setText(name);
                                }
                            });
                        }
                        
                        if (!email.isEmpty() && !email.equals(user.getEmail())) {
                            user.updateEmail(email).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    binding.tvUserEmail.setText(email);
                                    Toast.makeText(this, "Email updated", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Email update failed. " + (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText etNewPassword = view.findViewById(R.id.etFullName);
        etNewPassword.setHint("Enter New Password");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(view)
                .create();

        btnSave.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            if (newPassword.length() >= 6) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.updatePassword(newPassword).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
