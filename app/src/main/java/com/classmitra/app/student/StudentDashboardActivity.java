package com.classmitra.app.student;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.classmitra.app.R;
import com.classmitra.app.activities.ProfileActivity;
import com.classmitra.app.adapters.PdfUploadAdapter;
import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.chat.ChatActivity;
import com.classmitra.app.databinding.ActivityStudentDashboardBinding;
import com.classmitra.app.flashcards.FlashcardActivity;
import com.classmitra.app.models.PdfUpload;
import com.classmitra.app.quiz.QuizActivity;
import com.classmitra.app.teacher.ClassroomActivity;
import com.classmitra.app.teacher.UploadActivity;
import com.classmitra.app.utils.UIUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentDashboardActivity provides a premium hub for student learning.
 */
public class StudentDashboardActivity extends AppCompatActivity {

    private ActivityStudentDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updateUserGreeting();
        setupRecentActivity();
        setupLearningProgress();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void setupLearningProgress() {
        // Since no real data exists yet, we show the empty state as per audit
        binding.progressIndicator.setProgress(0);
        binding.tvProgressPercent.setText("0%");
        binding.tvProgressMessage.setText("No learning progress available.");
    }

    private void updateUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                binding.tvGreeting.setText("Hello, " + name + "!");
            }
        }
    }

    private void setupRecentActivity() {
        List<PdfUpload> activityList = new ArrayList<>();
        String latestText = DocumentDataManager.getInstance().getExtractedText();
        if (!latestText.isEmpty()) {
            activityList.add(new PdfUpload("Latest Processed Material", "Active Session"));
        } else {
            activityList.add(new PdfUpload("No recent study material found", "-"));
        }

        PdfUploadAdapter adapter = new PdfUploadAdapter(activityList);
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentActivity.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Quick Actions
        binding.btnContinue.setOnClickListener(v -> {
            String docText = DocumentDataManager.getInstance().getExtractedText();
            if (!docText.isEmpty()) {
                startActivity(new Intent(this, ChatActivity.class));
            } else {
                Toast.makeText(this, "No recent study material found. Please upload a document.", Toast.LENGTH_LONG).show();
            }
        });

        binding.cardMyNotes.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadActivity.class);
            intent.putExtra("ROLE", "STUDENT");
            startActivity(intent);
        });
        binding.cardAskMitra.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.cardPracticeQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("ROLE", "STUDENT");
            startActivity(intent);
        });
        binding.cardFlashcardsGrid.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra("ROLE", "STUDENT");
            startActivity(intent);
        });
        
        binding.cardSubjects.setOnClickListener(v -> startActivity(new Intent(this, ClassroomActivity.class)));
        binding.cardPerformance.setOnClickListener(v -> startActivity(new Intent(this, ProgressActivity.class)));

        binding.ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("OPEN_NOTIFICATIONS", true);
            startActivity(intent);
        });

        binding.btnViewAllTools.setOnClickListener(v -> UIUtils.showComingSoon(this, "Study Toolkit"));
        binding.fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadActivity.class);
            intent.putExtra("ROLE", "STUDENT");
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                return true;
            } else if (itemId == R.id.nav_uploads) {
                Intent intent = new Intent(this, UploadActivity.class);
                intent.putExtra("ROLE", "STUDENT");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_ask_mitra) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("ROLE", "STUDENT");
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
