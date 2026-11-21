package com.classmitra.app.teacher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.classmitra.app.adapters.PdfUploadAdapter;
import com.classmitra.app.databinding.ActivityUploadBinding;
import com.classmitra.app.models.PdfUpload;

import java.util.ArrayList;
import java.util.List;

/**
 * UploadActivity handles the study material selection and upload process.
 */
public class UploadActivity extends AppCompatActivity {

    private ActivityUploadBinding binding;

    // File picker launcher using the modern Activity Result API
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        navigateToProcessing(fileUri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Toolbar
        setSupportActionBar(binding.layoutToolbar.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.layoutToolbar.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        setupRecentUploads();
        setupClickListeners();
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

    private void setupRecentUploads() {
        List<PdfUpload> uploads = new ArrayList<>();
        uploads.add(new PdfUpload("Operating Systems.pdf", "Oct 25, 2023"));
        uploads.add(new PdfUpload("DBMS Unit 3.pdf", "Oct 24, 2023"));
        uploads.add(new PdfUpload("Java Basics.pdf", "Oct 23, 2023"));

        PdfUploadAdapter adapter = new PdfUploadAdapter(uploads);
        binding.rvRecentUploads.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentUploads.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.cardUploadArea.setOnClickListener(v -> openFilePicker());
        binding.btnUploadFile.setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf"); // Strictly following PDF extraction task
        filePickerLauncher.launch(intent);
    }

    private void navigateToProcessing(Uri fileUri) {
        Intent intent = new Intent(this, ProcessingActivity.class);
        intent.putExtra("FILE_URI", fileUri.toString());
        intent.putExtra("ROLE", getIntent().getStringExtra("ROLE"));
        startActivity(intent);
    }
}
