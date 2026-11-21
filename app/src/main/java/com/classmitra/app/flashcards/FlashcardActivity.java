package com.classmitra.app.flashcards;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.classmitra.app.R;
import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.ai.GeminiService;
import com.classmitra.app.chat.ChatActivity;
import com.classmitra.app.databinding.ActivityFlashcardsBinding;
import com.classmitra.app.quiz.QuizActivity;
import com.classmitra.app.student.StudentDashboardActivity;
import com.classmitra.app.teacher.TeacherDashboardActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * FlashcardActivity handles the revision experience using interactive cards.
 */
public class FlashcardActivity extends AppCompatActivity {

    private ActivityFlashcardsBinding binding;
    private List<Flashcard> flashcardList;
    private int currentIndex = 0;
    private boolean isShowingFront = true;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFlashcardsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiService = new GeminiService();
        flashcardList = new ArrayList<>();
        
        setupToolbar();
        setupClickListeners();
    }

    private void fetchAiFlashcards(String topic) {
        if (geminiService.isApiKeyMissing()) return;

        String sourceText;
        if (topic != null && !topic.isEmpty()) {
            sourceText = topic;
            binding.tvFlashcardSource.setText(getString(R.string.flashcard_topic_label, topic));
        } else {
            sourceText = DocumentDataManager.getInstance().getExtractedText();
            if (sourceText.isEmpty()) {
                Toast.makeText(this, R.string.error_no_notes, Toast.LENGTH_LONG).show();
                return;
            }
            binding.tvFlashcardSource.setText(R.string.flashcard_source_notes);
        }

        binding.layoutLoading.loadingView.setVisibility(View.VISIBLE);
        geminiService.generateFlashcards(sourceText, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        String jsonString = extractJsonArray(response);
                        JSONArray array = new JSONArray(jsonString);
                        flashcardList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            flashcardList.add(new Flashcard(obj.getString("question"), obj.getString("answer")));
                        }
                        binding.layoutLoading.loadingView.setVisibility(View.GONE);
                        if (!flashcardList.isEmpty()) {
                            currentIndex = 0;
                            updateUI();
                            binding.mainViewFlipper.setDisplayedChild(1); // Show Flashcards
                        }
                        Toast.makeText(FlashcardActivity.this, R.string.ai_flashcards_loaded, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        binding.layoutLoading.loadingView.setVisibility(View.GONE);
                        Toast.makeText(FlashcardActivity.this, R.string.error_parse_ai_response, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    binding.layoutLoading.loadingView.setVisibility(View.GONE);
                    Toast.makeText(FlashcardActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String extractJsonArray(String raw) {
        if (raw == null) return "[]";
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private void setupToolbar() {
        setSupportActionBar(binding.layoutToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.layoutToolbar.toolbar.setNavigationOnClickListener(v -> handleBackAction());
    }

    private void handleBackAction() {
        if (binding.mainViewFlipper.getDisplayedChild() > 0) {
            binding.mainViewFlipper.setDisplayedChild(0);
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        handleBackAction();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackAction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupClickListeners() {
        binding.btnGenerateNew.setOnClickListener(v -> {
            Editable topicEditable = binding.etFlashcardTopic.getText();
            String topic = (topicEditable != null) ? topicEditable.toString().trim() : "";
            fetchAiFlashcards(topic);
        });

        binding.cardView.setOnClickListener(v -> flipCard());

        binding.btnNext.setOnClickListener(v -> {
            if (currentIndex < flashcardList.size() - 1) {
                currentIndex++;
                isShowingFront = true;
                updateUI();
            } else {
                binding.mainViewFlipper.setDisplayedChild(2); // Completion
            }
        });

        binding.btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                isShowingFront = true;
                updateUI();
            }
        });

        binding.btnPracticeQuiz.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        binding.btnAskMitra.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));

        binding.btnBackToDashboard.setOnClickListener(v -> {
            String role = getIntent().getStringExtra("ROLE");
            Intent intent;
            if ("TEACHER".equals(role)) {
                intent = new Intent(this, TeacherDashboardActivity.class);
            } else {
                intent = new Intent(this, StudentDashboardActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        binding.btnCompletionAskMitra.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
    }

    private void flipCard() {
        if (flashcardList.isEmpty()) return;

        AnimatorSet outAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
        AnimatorSet inAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);

        outAnim.setTarget(binding.cardView);
        inAnim.setTarget(binding.cardView);

        outAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isShowingFront = !isShowingFront;
                Flashcard card = flashcardList.get(currentIndex);
                binding.tvCardLabel.setText(isShowingFront ? R.string.flashcard_label_question : R.string.flashcard_label_answer);
                binding.tvCardContent.setText(isShowingFront ? card.question : card.answer);
                inAnim.start();
            }
        });

        outAnim.start();
    }

    private void updateUI() {
        if (flashcardList.isEmpty()) return;
        
        Flashcard card = flashcardList.get(currentIndex);
        binding.tvCardLabel.setText(R.string.flashcard_label_question);
        binding.tvCardContent.setText(card.question);
        binding.tvCardCount.setText(getString(R.string.flashcard_count, currentIndex + 1, flashcardList.size()));
        binding.flashcardProgress.setProgress(((currentIndex + 1) * 100) / flashcardList.size());
        binding.btnPrevious.setVisibility(currentIndex == 0 ? View.GONE : View.VISIBLE);
        binding.btnNext.setText(currentIndex == flashcardList.size() - 1 ? R.string.btn_finish : R.string.btn_next_simple);
    }

    private static class Flashcard {
        String question;
        String answer;

        Flashcard(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }
}
