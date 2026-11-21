package com.classmitra.app.teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.classmitra.app.R;
import com.classmitra.app.adapters.SubjectAdapter;
import com.classmitra.app.databinding.ActivityClassroomBinding;
import com.classmitra.app.models.Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ClassroomActivity displays a list of subjects the user is managing or enrolled in.
 */
public class ClassroomActivity extends AppCompatActivity implements SubjectAdapter.OnSubjectClickListener {

    private ActivityClassroomBinding binding;
    private List<Subject> subjects;
    private SubjectAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClassroomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupSubjectsList();
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

    private void setupSubjectsList() {
        subjects = new ArrayList<>();
        subjects.add(new Subject("Operating Systems", 12, 4, Color.parseColor("#2196F3")));
        subjects.add(new Subject("DBMS", 18, 6, Color.parseColor("#4CAF50")));
        subjects.add(new Subject("OOP using Java", 10, 5, Color.parseColor("#FF9800")));
        subjects.add(new Subject("Computer Networks", 9, 3, Color.parseColor("#F44336")));
        subjects.add(new Subject("Software Engineering", 7, 2, Color.parseColor("#9C27B0")));

        adapter = new SubjectAdapter(subjects, this);
        binding.rvSubjects.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvSubjects.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.fabAddSubject.setOnClickListener(v -> showAddSubjectDialog());
    }

    private void showAddSubjectDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_subject, null);
        EditText etSubjectName = view.findViewById(R.id.etSubjectName);
        Button btnAdd = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnAdd.setOnClickListener(v -> {
            String name = etSubjectName.getText().toString().trim();
            if (!name.isEmpty()) {
                addNewSubject(name);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter subject name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void addNewSubject(String name) {
        int[] colors = {
            Color.parseColor("#2196F3"), Color.parseColor("#4CAF50"), 
            Color.parseColor("#FF9800"), Color.parseColor("#F44336"), 
            Color.parseColor("#9C27B0")
        };
        int randomColor = colors[new Random().nextInt(colors.length)];
        
        Subject newSubject = new Subject(name, 0, 0, randomColor);
        subjects.add(0, newSubject);
        adapter.notifyItemInserted(0);
        binding.rvSubjects.scrollToPosition(0);
        Toast.makeText(this, "Subject added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubjectClick(Subject subject) {
        Intent intent = new Intent(this, SubjectDetailsActivity.class);
        intent.putExtra("SUBJECT_NAME", subject.getName());
        startActivity(intent);
    }
}
