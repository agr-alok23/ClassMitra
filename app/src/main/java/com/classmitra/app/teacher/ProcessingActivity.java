package com.classmitra.app.teacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.classmitra.app.ai.DocumentDataManager;
import com.classmitra.app.ai.PdfTextExtractor;
import com.classmitra.app.databinding.ActivityProcessingBinding;

/**
 * ProcessingActivity simulates the AI analysis of the uploaded document.
 */
public class ProcessingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityProcessingBinding binding = ActivityProcessingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String uriString = getIntent().getStringExtra("FILE_URI");
        if (uriString != null) {
            Uri fileUri = Uri.parse(uriString);
            extractPdf(fileUri);
        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void extractPdf(Uri fileUri) {
        PdfTextExtractor.extractText(this, fileUri, new PdfTextExtractor.Callback() {
            @Override
            public void onTextExtracted(String text) {
                DocumentDataManager.getInstance().setExtractedText(text);
                runOnUiThread(() -> navigateToSummary());
            }

            @Override
            public void onError(Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(ProcessingActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void navigateToSummary() {
        Intent intent = new Intent(this, SummaryActivity.class);
        intent.putExtra("ROLE", getIntent().getStringExtra("ROLE"));
        startActivity(intent);
        finish(); // Remove processing screen from the back stack
    }
}