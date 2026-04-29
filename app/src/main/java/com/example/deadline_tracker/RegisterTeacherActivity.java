package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deadline_tracker.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterTeacherActivity extends AppCompatActivity {

    private boolean isTeacher = true;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private Button btnRegister;

    // Teacher fields
    private EditText etTeacherName, etTeacherId, etTeacherDept, etTeacherEmail, etTeacherPassword;
    // CR fields
    private EditText etCRName, etCRId, etCRDept, etCRBatch, etCREmail, etCRPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_teacher);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Teacher fields
        etTeacherName     = findViewById(R.id.etTeacherName);
        etTeacherId       = findViewById(R.id.etTeacherId);
        etTeacherDept     = findViewById(R.id.etTeacherDept);
        etTeacherEmail    = findViewById(R.id.etTeacherEmail);
        etTeacherPassword = findViewById(R.id.etTeacherPassword);

        // CR fields
        etCRName     = findViewById(R.id.etCRName);
        etCRId       = findViewById(R.id.etCRId);
        etCRDept     = findViewById(R.id.etCRDept);
        etCRBatch    = findViewById(R.id.etCRBatch);
        etCREmail    = findViewById(R.id.etCREmail);
        etCRPassword = findViewById(R.id.etCRPassword);

        btnRegister  = findViewById(R.id.btnRegister);
        progressBar  = findViewById(R.id.progressBar);

        Button tabTeacher          = findViewById(R.id.tabTeacher);
        Button tabCR               = findViewById(R.id.tabCR);
        LinearLayout layoutTeacher = findViewById(R.id.layoutTeacher);
        LinearLayout layoutCR      = findViewById(R.id.layoutCR);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        tabTeacher.setOnClickListener(v -> {
            isTeacher = true;
            layoutTeacher.setVisibility(View.VISIBLE);
            layoutCR.setVisibility(View.GONE);
            tabTeacher.setTextColor(0xFFC084FC);
            tabTeacher.setBackgroundResource(R.drawable.bg_subtab_active);
            tabCR.setTextColor(0x66FFFFFF);
            tabCR.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnRegister.setText("Register as Teacher");
        });

        tabCR.setOnClickListener(v -> {
            isTeacher = false;
            layoutTeacher.setVisibility(View.GONE);
            layoutCR.setVisibility(View.VISIBLE);
            tabCR.setTextColor(0xFFC084FC);
            tabCR.setBackgroundResource(R.drawable.bg_subtab_active);
            tabTeacher.setTextColor(0x66FFFFFF);
            tabTeacher.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnRegister.setText("Register as CR");
        });

        btnRegister.setOnClickListener(v -> {
            if (isTeacher) registerTeacher();
            else registerCR();
        });
    }

    private void registerTeacher() {
        String name     = etTeacherName.getText().toString().trim();
        String id       = etTeacherId.getText().toString().trim();
        String dept     = etTeacherDept.getText().toString().trim();
        String email    = etTeacherEmail.getText().toString().trim();
        String password = etTeacherPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name))     { etTeacherName.setError("Required");     return; }
        if (TextUtils.isEmpty(id))       { etTeacherId.setError("Required");       return; }
        if (TextUtils.isEmpty(dept))     { etTeacherDept.setError("Required");     return; }
        if (TextUtils.isEmpty(email))    { etTeacherEmail.setError("Required");    return; }
        if (password.length() < 6)       { etTeacherPassword.setError("Min 6 chars"); return; }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    User user = new User(uid, name, email, "teacher", dept, "", "", id);

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(unused -> {
                                showLoading(false);
                                Toast.makeText(this, "Welcome, " + name + "!",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, TeacherDashboardActivity.class));
                                finishAffinity();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void registerCR() {
        String name     = etCRName.getText().toString().trim();
        String id       = etCRId.getText().toString().trim();
        String dept     = etCRDept.getText().toString().trim();
        String batch    = etCRBatch.getText().toString().trim();
        String email    = etCREmail.getText().toString().trim();
        String password = etCRPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name))  { etCRName.setError("Required");  return; }
        if (TextUtils.isEmpty(id))    { etCRId.setError("Required");    return; }
        if (TextUtils.isEmpty(dept))  { etCRDept.setError("Required");  return; }
        if (TextUtils.isEmpty(batch)) { etCRBatch.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { etCREmail.setError("Required"); return; }
        if (password.length() < 6)    { etCRPassword.setError("Min 6 chars"); return; }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    User user = new User(uid, name, email, "cr", dept, batch, id, "");

                    db.collection("users").document(uid).set(user)
                            .addOnSuccessListener(unused -> {
                                showLoading(false);
                                Toast.makeText(this, "Welcome CR " + name + "!",
                                        Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, TeacherDashboardActivity.class));
                                finishAffinity();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null)
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
    }
}