package com.classmitra.app.teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.classmitra.app.R;
import com.classmitra.app.activities.ProfileActivity;
import com.classmitra.app.adapters.PdfUploadAdapter;
import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.chat.ChatActivity;
import com.classmitra.app.databinding.ActivityTeacherDashboardBinding;
import com.classmitra.app.flashcards.FlashcardActivity;
import com.classmitra.app.models.PdfUpload;
import com.classmitra.app.quiz.QuizActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * TeacherDashboardActivity provides a central hub for teacher-related AI tools.
 */
public class TeacherDashboardActivity extends AppCompatActivity {

    private ActivityTeacherDashboardBinding binding;
    private static final String PREFS_NAME = "teacher_stats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeacherDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.layoutToolbar.toolbar);
        updateUserGreeting();
        setupRecentUploads();
        setupClickListeners();
        updateTeacherStats();
    }

    private void updateTeacherStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int quizCount = prefs.getInt("quiz_count", 0);
        int flashcardCount = prefs.getInt("flashcard_count", 0);
        
        binding.tvQuizCount.setText(String.valueOf(quizCount));
        binding.tvFlashcardCount.setText(String.valueOf(flashcardCount));
    }

    private void updateUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                binding.tvGreetingTeacher.setText("Good Morning, " + name + " \uD83D\uDC4B");
            }
        }
    }

    private void setupRecentUploads() {
        List<PdfUpload> uploads = new ArrayList<>();
        String latestDoc = DocumentDataManager.getInstance().getExtractedText();
        if (!latestDoc.isEmpty()) {
            uploads.add(new PdfUpload("Latest Processed Material", "Session Active"));
        } else {
            uploads.add(new PdfUpload("No materials uploaded yet", "-"));
        }

        PdfUploadAdapter adapter = new PdfUploadAdapter(uploads);
        binding.rvRecentUploads.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentUploads.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.cardUpload.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadActivity.class);
            intent.putExtra("ROLE", "TEACHER");
            startActivity(intent);
        });
        binding.cardSummary.setOnClickListener(v -> {
            if (!DocumentDataManager.getInstance().getExtractedText().isEmpty()) {
                Intent intent = new Intent(this, SummaryActivity.class);
                intent.putExtra("ROLE", "TEACHER");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please upload a document first to generate AI summaries.", Toast.LENGTH_LONG).show();
            }
        });
        binding.cardQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("ROLE", "TEACHER");
            startActivity(intent);
        });
        binding.cardFlashcards.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra("ROLE", "TEACHER");
            startActivity(intent);
        });
        
        binding.cardChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        binding.cardClassroom.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Classroom")
                .setMessage("Classroom feature coming soon. You will be able to manage students and assignments here.")
                .setPositiveButton("OK", null)
                .show());
        
        // Teacher Specific Actions
        binding.cardPerformance.setOnClickListener(v -> {
            int count = Integer.parseInt(binding.tvQuizCount.getText().toString());
            if (count > 0) {
                startActivity(new Intent(this, QuizActivity.class));
            } else {
                Toast.makeText(this, "No quizzes created yet.", Toast.LENGTH_SHORT).show();
            }
        });
        binding.cardStreak.setOnClickListener(v -> {
            int count = Integer.parseInt(binding.tvFlashcardCount.getText().toString());
            if (count > 0) {
                startActivity(new Intent(this, FlashcardActivity.class));
            } else {
                Toast.makeText(this, "No flashcard sets created yet.", Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.fabAskMitra.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                return true;
            } else if (itemId == R.id.nav_uploads) {
                Intent intent = new Intent(this, UploadActivity.class);
                intent.putExtra("ROLE", "TEACHER");
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("ROLE", "TEACHER");
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
