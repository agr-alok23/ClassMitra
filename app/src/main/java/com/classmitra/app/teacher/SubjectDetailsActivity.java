package com.classmitra.app.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.classmitra.app.chat.ChatActivity;
import com.classmitra.app.databinding.ActivitySubjectDetailsBinding;
import com.classmitra.app.flashcards.FlashcardActivity;
import com.classmitra.app.quiz.QuizActivity;

/**
 * SubjectDetailsActivity provides access to all resources for a specific subject.
 */
public class SubjectDetailsActivity extends AppCompatActivity {

    private ActivitySubjectDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubjectDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String subjectName = getIntent().getStringExtra("SUBJECT_NAME");
        if (subjectName != null) {
            binding.tvSubjectTitle.setText(subjectName);
        }

        setupToolbar();
        setupClickListeners();
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupClickListeners() {
        binding.cardNotes.setOnClickListener(v -> startActivity(new Intent(this, UploadActivity.class)));
        binding.cardSummary.setOnClickListener(v -> startActivity(new Intent(this, SummaryActivity.class)));
        binding.cardQuiz.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        binding.cardFlashcards.setOnClickListener(v -> startActivity(new Intent(this, FlashcardActivity.class)));
        binding.cardAskMitra.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
    }
}