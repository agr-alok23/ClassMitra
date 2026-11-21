package com.classmitra.app.chat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.classmitra.app.R;
import com.classmitra.app.adapters.ChatAdapter;
import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.ai.GeminiService;
import com.classmitra.app.databinding.ActivityChatBinding;
import com.classmitra.app.models.ChatMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern ChatActivity for "Ask Mitra" with AI multimodal support and Markdown rendering.
 */
public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private GeminiService geminiService;
    private Bitmap selectedBitmap;

    private final ActivityResultLauncher<Intent> speechResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> resultList = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (resultList != null && !resultList.isEmpty()) {
                        String spokenText = resultList.get(0);
                        binding.etMessage.setText(spokenText);
                    }
                }
            }
    );

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getExtras() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        selectedBitmap = photo;
                        showImagePreview(photo);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        geminiService = new GeminiService();
        setupToolbar();
        setupChat();
        setupKeyboardHandling();
        setupClickListeners();

        if (messageList.isEmpty()) {
            addMitraMessage("Hi! I'm Mitra 🤖 Your personal AI learning companion.\n\n" +
                    "I can help you **summarize notes**, **generate MCQs**, or **explain complex topics**.\n\n" +
                    "Feel free to upload an image or ask anything!");
        }
    }

    private void setupKeyboardHandling() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutInputContainer, (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(0, 0, 0, imeInsets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        
        // Handle regeneration
        adapter.setOnMessageActionListener(position -> {
            if (position > 0 && messageList.get(position - 1).getType() == ChatMessage.TYPE_USER) {
                String lastUserPrompt = messageList.get(position - 1).getMessage();
                Bitmap lastImage = messageList.get(position - 1).getImage();
                
                // Remove the last AI response
                messageList.remove(position);
                adapter.notifyItemRemoved(position);
                
                // Call AI again
                callGemini(lastUserPrompt, lastImage);
            }
        });

        binding.rvChat.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChat.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnVoice.setOnClickListener(v -> startVoiceInput());
        binding.btnAttach.setOnClickListener(v -> showAttachmentOptions());
        binding.btnRemoveImage.setOnClickListener(v -> removeImage());

        // Suggestions
        binding.chipExplain.setOnClickListener(v -> handleSuggestion("Explain the key concepts of this document."));
        binding.chipMCQs.setOnClickListener(v -> handleSuggestion("Generate 3 multiple choice questions from these notes."));
        binding.chipFlashcards.setOnClickListener(v -> handleSuggestion("Create a few study flashcards from this content."));
        binding.chipSimplify.setOnClickListener(v -> handleSuggestion("Simplify the most complex parts of these notes."));
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (!text.isEmpty() || selectedBitmap != null) {
            String prompt = text.isEmpty() ? "Analyze this image" : text;
            addUserMessage(prompt, selectedBitmap);
            binding.etMessage.setText("");
            
            Bitmap imageToProcess = selectedBitmap;
            removeImage(); // UI cleanup
            
            callGemini(prompt, imageToProcess);
        }
    }

    private void handleSuggestion(String suggestion) {
        addUserMessage(suggestion, null);
        callGemini(suggestion, null);
    }

    private void callGemini(String prompt, Bitmap image) {
        String docText = DocumentDataManager.getInstance().getExtractedText();
        String fullPrompt = prompt;

        if (docText != null && !docText.isEmpty()) {
            fullPrompt = "CONTEXT FROM USER NOTES: " + docText + "\n\nUSER QUESTION: " + prompt;
        }

        // Add a temporary typing message
        addTypingIndicator();

        geminiService.generateContentWithImage(fullPrompt, image, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    addMitraMessage(response);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> {
                    removeTypingIndicator();
                    addMitraMessage("Error: " + throwable.getMessage());
                });
            }
        });
    }

    private void addTypingIndicator() {
        ChatMessage typingMsg = new ChatMessage("...", ChatMessage.TYPE_MITRA);
        typingMsg.setTyping(true);
        messageList.add(typingMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void removeTypingIndicator() {
        if (!messageList.isEmpty() && messageList.get(messageList.size() - 1).isTyping()) {
            int pos = messageList.size() - 1;
            messageList.remove(pos);
            adapter.notifyItemRemoved(pos);
        }
    }

    private void addUserMessage(String message, Bitmap image) {
        messageList.add(new ChatMessage(message, ChatMessage.TYPE_USER, image));
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void addMitraMessage(String message) {
        messageList.add(new ChatMessage(message, ChatMessage.TYPE_MITRA));
        adapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        binding.rvChat.postDelayed(() -> 
            binding.rvChat.smoothScrollToPosition(messageList.size() - 1), 100);
    }

    private void showAttachmentOptions() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(intent);
                    } else {
                        galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build());
                    }
                })
                .show();
    }

    private void handleImageSelected(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                selectedBitmap = bitmap;
                showImagePreview(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePreview(Bitmap bitmap) {
        binding.ivPreview.setImageBitmap(bitmap);
        binding.cardImagePreview.setVisibility(View.VISIBLE);
    }

    private void removeImage() {
        selectedBitmap = null;
        binding.cardImagePreview.setVisibility(View.GONE);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question...");
        try {
            speechResultLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }
}