package com.classmitra.app.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.ai.GeminiService;
import com.classmitra.app.chat.ChatActivity;
import com.classmitra.app.databinding.ActivitySummaryBinding;
import com.classmitra.app.flashcards.FlashcardActivity;
import com.classmitra.app.quiz.QuizActivity;

/**
 * SummaryActivity displays the premium AI-generated summary results.
 */
public class SummaryActivity extends AppCompatActivity {

    private ActivitySummaryBinding binding;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiService = new GeminiService();

        // Setup Toolbar
        setSupportActionBar(binding.layoutToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.layoutToolbar.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupClickListeners();
        fetchAiSummary();
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

    private void fetchAiSummary() {
        binding.layoutLoading.loadingView.setVisibility(View.VISIBLE);
        
        String docText = DocumentDataManager.getInstance().getExtractedText();

        if (docText.isEmpty()) {
            binding.layoutLoading.loadingView.setVisibility(View.GONE);
            binding.tvSummaryContent.setText("No content available to summarize. Please upload a PDF first.");
            return;
        }

        geminiService.getSummary(docText, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    binding.layoutLoading.loadingView.setVisibility(View.GONE);
                    binding.tvSummaryContent.setText(response);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    binding.layoutLoading.loadingView.setVisibility(View.GONE);
                    Toast.makeText(SummaryActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupClickListeners() {
        String role = getIntent().getStringExtra("ROLE");
        
        binding.btnGenerateQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra("ROLE", role);
            startActivity(intent);
        });

        binding.btnGenerateFlashcards.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra("ROLE", role);
            startActivity(intent);
        });

        binding.btnAskMitra.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
        });
    }
}
