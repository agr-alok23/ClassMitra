package com.classmitra.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.classmitra.app.auth.LoginActivity;
import com.classmitra.app.databinding.ActivityRoleSelectionBinding;
import com.classmitra.app.student.StudentDashboardActivity;
import com.classmitra.app.teacher.TeacherDashboardActivity;

/**
 * RoleSelectionActivity allows users to choose between Teacher and Student roles.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    private ActivityRoleSelectionBinding binding;
    private String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cardTeacher.setOnClickListener(v -> selectRole("TEACHER"));
        binding.cardStudent.setOnClickListener(v -> selectRole("STUDENT"));

        binding.btnContinue.setOnClickListener(v -> {
            if (!selectedRole.isEmpty()) {
                Intent intent;
                if (selectedRole.equals("TEACHER")) {
                    intent = new Intent(this, TeacherDashboardActivity.class);
                } else {
                    intent = new Intent(this, StudentDashboardActivity.class);
                }
                intent.putExtra("ROLE", selectedRole);
                startActivity(intent);
                finish();
            }
        });
    }

    private void selectRole(String role) {
        selectedRole = role;
        binding.btnContinue.setEnabled(true);

        if (role.equals("TEACHER")) {
            binding.cardTeacher.setStrokeWidth(4);
            binding.cardTeacher.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
            
            binding.cardStudent.setStrokeWidth(0);
            binding.cardStudent.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        } else {
            binding.cardStudent.setStrokeWidth(4);
            binding.cardStudent.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
            
            binding.cardTeacher.setStrokeWidth(0);
            binding.cardTeacher.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }
}