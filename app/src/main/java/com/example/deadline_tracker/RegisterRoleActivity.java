package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterRoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_role);

        TextView tvBack = findViewById(R.id.tvBack);
        LinearLayout cardStudent = findViewById(R.id.cardStudent);
        LinearLayout cardTeacher = findViewById(R.id.cardTeacher);

        tvBack.setOnClickListener(v -> finish());

        cardStudent.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterStudentActivity.class)));

        cardTeacher.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterTeacherActivity.class)));
    }
}