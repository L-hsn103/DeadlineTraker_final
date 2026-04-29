package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deadline_tracker.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterStudentActivity extends AppCompatActivity {

    private EditText etFullName, etStudentId, etDepartment, etBatch, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_student);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etFullName   = findViewById(R.id.etFullName);
        etStudentId  = findViewById(R.id.etStudentId);
        etDepartment = findViewById(R.id.etDepartment);
        etBatch      = findViewById(R.id.etBatch);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        btnRegister  = findViewById(R.id.btnRegister);
        progressBar  = findViewById(R.id.progressBar);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> registerStudent());
    }

    private void registerStudent() {
        String name       = etFullName.getText().toString().trim();
        String studentId  = etStudentId.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String batch      = etBatch.getText().toString().trim();
        String email      = etEmail.getText().toString().trim();
        String password   = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name))       { etFullName.setError("Required");   return; }
        if (TextUtils.isEmpty(studentId))  { etStudentId.setError("Required");  return; }
        if (TextUtils.isEmpty(department)) { etDepartment.setError("Required"); return; }
        if (TextUtils.isEmpty(batch))      { etBatch.setError("Required");      return; }
        if (TextUtils.isEmpty(email))      { etEmail.setError("Required");      return; }
        if (password.length() < 6)         { etPassword.setError("Min 6 chars");return; }

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    User user = new User(
                            uid, name, email,
                            "student",
                            department,
                            batch,
                            studentId,
                            ""
                    );

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this,
                                        "Welcome " + name + "!",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, StudentDashboardActivity.class));
                                finishAffinity();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(this,
                                        "Failed to save profile: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}