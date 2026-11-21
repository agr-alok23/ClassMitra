package com.classmitra.app.quiz;

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
import com.classmitra.app.databinding.ActivityQuizBinding;
import com.classmitra.app.flashcards.FlashcardActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * QuizActivity handles the AI-generated quiz experience.
 */
public class QuizActivity extends AppCompatActivity {

    private ActivityQuizBinding binding;
    private List<Question> questionList;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int[] userAnswers; 
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiService = new GeminiService();
        questionList = new ArrayList<>();

        setupToolbar();
        setupClickListeners();
    }

    private void fetchAiQuiz(String topic) {
        if (geminiService.isApiKeyMissing()) {
            Toast.makeText(this, "API Key Missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceText;
        if (topic != null && !topic.isEmpty()) {
            sourceText = topic;
            binding.tvQuizSource.setText("Topic: " + topic);
        } else {
            sourceText = DocumentDataManager.getInstance().getExtractedText();
            if (sourceText.isEmpty()) {
                Toast.makeText(this, "No notes available. Please enter a topic.", Toast.LENGTH_LONG).show();
                return;
            }
            binding.tvQuizSource.setText("Source: Uploaded Notes");
        }

        binding.layoutLoading.loadingView.setVisibility(View.VISIBLE);
        geminiService.generateQuiz(sourceText, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        String jsonString = extractJsonArray(response);
                        JSONArray array = new JSONArray(jsonString);
                        questionList.clear();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            JSONArray optArray = obj.getJSONArray("options");
                            String[] options = new String[4];
                            for (int j = 0; j < 4; j++) options[j] = optArray.getString(j);
                            questionList.add(new Question(obj.getString("question"), options, obj.getInt("correctIndex")));
                        }
                        
                        userAnswers = new int[questionList.size()];
                        for (int i = 0; i < userAnswers.length; i++) userAnswers[i] = -1;
                        
                        binding.layoutLoading.loadingView.setVisibility(View.GONE);
                        binding.viewFlipper.setDisplayedChild(1); // Show Question screen
                        showQuestion(0);
                        Toast.makeText(QuizActivity.this, "AI Quiz Loaded", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        binding.layoutLoading.loadingView.setVisibility(View.GONE);
                        Toast.makeText(QuizActivity.this, "Failed to parse AI response", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    binding.layoutLoading.loadingView.setVisibility(View.GONE);
                    Toast.makeText(QuizActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
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
        if (binding.viewFlipper.getDisplayedChild() == 1) {
            binding.viewFlipper.setDisplayedChild(0);
        } else if (binding.viewFlipper.getDisplayedChild() == 2) {
            binding.viewFlipper.setDisplayedChild(0);
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
        binding.btnStartQuiz.setOnClickListener(v -> {
            String topic = binding.etQuizTopic.getText().toString().trim();
            fetchAiQuiz(topic);
        });

        binding.btnNext.setOnClickListener(v -> {
            saveAnswer();
            if (currentQuestionIndex < questionList.size() - 1) {
                currentQuestionIndex++;
                showQuestion(currentQuestionIndex);
            } else {
                calculateScore();
                showResult();
            }
        });

        binding.btnPrevious.setOnClickListener(v -> {
            if (currentQuestionIndex > 0) {
                saveAnswer();
                currentQuestionIndex--;
                showQuestion(currentQuestionIndex);
            }
        });

        binding.btnReview.setOnClickListener(v -> Toast.makeText(this, "Review coming soon", Toast.LENGTH_SHORT).show());
        
        binding.btnResultFlashcards.setOnClickListener(v -> {
            Intent intent = new Intent(this, FlashcardActivity.class);
            intent.putExtra("ROLE", getIntent().getStringExtra("ROLE"));
            startActivity(intent);
        });
        
        binding.btnResultChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
    }

    private void showQuestion(int index) {
        Question q = questionList.get(index);
        binding.tvQuestionCount.setText("Question " + (index + 1) + " of " + questionList.size());
        binding.tvQuestionText.setText(q.text);
        binding.rbOption1.setText(q.options[0]);
        binding.rbOption2.setText(q.options[1]);
        binding.rbOption3.setText(q.options[2]);
        binding.rbOption4.setText(q.options[3]);

        binding.rgOptions.clearCheck();
        if (userAnswers[index] != -1) {
            switch (userAnswers[index]) {
                case 0: binding.rbOption1.setChecked(true); break;
                case 1: binding.rbOption2.setChecked(true); break;
                case 2: binding.rbOption3.setChecked(true); break;
                case 3: binding.rbOption4.setChecked(true); break;
            }
        }

        binding.quizProgress.setProgress(((index + 1) * 100) / questionList.size());
        binding.btnPrevious.setVisibility(index == 0 ? View.GONE : View.VISIBLE);
        binding.btnNext.setText(index == questionList.size() - 1 ? "Finish" : "Next");
    }

    private void saveAnswer() {
        int checkedId = binding.rgOptions.getCheckedRadioButtonId();
        if (checkedId == binding.rbOption1.getId()) userAnswers[currentQuestionIndex] = 0;
        else if (checkedId == binding.rbOption2.getId()) userAnswers[currentQuestionIndex] = 1;
        else if (checkedId == binding.rbOption3.getId()) userAnswers[currentQuestionIndex] = 2;
        else if (checkedId == binding.rbOption4.getId()) userAnswers[currentQuestionIndex] = 3;
    }

    private void calculateScore() {
        score = 0;
        for (int i = 0; i < questionList.size(); i++) {
            if (userAnswers[i] == questionList.get(i).correctIndex) {
                score++;
            }
        }
    }

    private void showResult() {
        binding.viewFlipper.setDisplayedChild(2);
        binding.tvFinalScore.setText(score + " / " + questionList.size());
        binding.resultCircularProgress.setProgress((score * 100) / questionList.size());
    }

    private static class Question {
        String text;
        String[] options;
        int correctIndex;

        Question(String text, String[] options, int correctIndex) {
            this.text = text;
            this.options = options;
            this.correctIndex = correctIndex;
        }
    }
}
