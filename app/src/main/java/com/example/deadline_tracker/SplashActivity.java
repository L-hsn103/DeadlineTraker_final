package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // ✅ No delay — check login immediately
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // User is logged in — fetch role and navigate
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            if ("teacher".equalsIgnoreCase(role) ||
                                    "cr".equalsIgnoreCase(role)) {
                                startActivity(new Intent(this,
                                        TeacherDashboardActivity.class));
                            } else {
                                startActivity(new Intent(this,
                                        StudentDashboardActivity.class));
                            }
                        } else {
                            startActivity(new Intent(this, LoginActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
        } else {
            // Not logged in — go to login immediately
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}