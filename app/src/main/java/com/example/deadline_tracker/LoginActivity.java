package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText etUserId, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        etUserId    = findViewById(R.id.etUserId);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> loginUser());

        findViewById(R.id.tvRegisterNow).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterRoleActivity.class)));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    private void loginUser() {
        String userId   = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(userId)) {
            etUserId.setError("ID required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // We check BOTH studentId and teacherId fields in one go or sequentially
        // This search works because of the 'allow list: if true' rule
        db.collection("users")
                .whereEqualTo("studentId", userId)
                .get()
                .addOnSuccessListener(studentSnap -> {
                    if (!studentSnap.isEmpty()) {
                        String email = studentSnap.getDocuments().get(0).getString("email");
                        signInWithEmail(email, password);
                    } else {
                        // Not a student, check if it's a teacher/CR
                        db.collection("users")
                                .whereEqualTo("teacherId", userId)
                                .get()
                                .addOnSuccessListener(teacherSnap -> {
                                    if (!teacherSnap.isEmpty()) {
                                        String email = teacherSnap.getDocuments().get(0).getString("email");
                                        signInWithEmail(email, password);
                                    } else {
                                        showError("ID not found. Check your ID or Register.");
                                    }
                                }).addOnFailureListener(e -> showError("Search Error: " + e.getMessage()));
                    }
                }).addOnFailureListener(e -> showError("Search Error: " + e.getMessage()));
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // Now that we are logged in, we fetch the full profile
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                progressBar.setVisibility(View.GONE);
                                if (doc.exists()) {
                                    String role = doc.getString("role");
                                    Class<?> destination = ("teacher".equals(role) || "cr".equals(role))
                                            ? TeacherDashboardActivity.class
                                            : StudentDashboardActivity.class;

                                    Intent intent = new Intent(this, destination);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    showError("Profile data missing.");
                                }
                            })
                            .addOnFailureListener(e -> showError("Profile Fetch Error: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showError("Authentication failed. Check password."));
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}