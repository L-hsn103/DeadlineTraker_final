package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.deadline_tracker.adapter.TaskAdapter;
import com.example.deadline_tracker.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeacherDashboardActivity extends AppCompatActivity {

    private RecyclerView    recyclerTasks;
    private TaskAdapter     taskAdapter;
    private List<Task>      taskList = new ArrayList<>();

    private TextView        tvUserName, tvUserRole;
    private TextView        tvCtCount, tvAsgnCount, tvQuizCount;
    private CircleImageView ivAvatar;

    private FirebaseFirestore    db;
    private FirebaseAuth         mAuth;

    // ✅ Store listener so we can remove it and avoid stacking
    private ListenerRegistration taskListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvUserName  = findViewById(R.id.tvUserName);
        tvUserRole  = findViewById(R.id.tvUserRole);
        tvCtCount   = findViewById(R.id.tvCtCount);
        tvAsgnCount = findViewById(R.id.tvAsgnCount);
        tvQuizCount = findViewById(R.id.tvQuizCount);
        ivAvatar    = findViewById(R.id.ivAvatar);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, taskList, task -> { });
        recyclerTasks.setAdapter(taskAdapter);

        findViewById(R.id.fabAddTask).setOnClickListener(v ->
                startActivity(new Intent(this, AddTaskActivity.class)));

        setupNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeacherProfile(); // reload profile + photo on every resume
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ✅ Remove listener when screen not visible — prevents stacking
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }

    private void setupNavigation() {
        findViewById(R.id.tNavHome).setOnClickListener(v ->
                recyclerTasks.smoothScrollToPosition(0));
        findViewById(R.id.tNavCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.tNavNotif).setOnClickListener(v ->
                startActivity(new Intent(this, TeacherNotificationActivity.class)));
        findViewById(R.id.tNavSettings).setOnClickListener(v ->
                startActivity(new Intent(this, TeacherSettingsActivity.class)));
    }

    private void loadTeacherProfile() {
        if (mAuth.getCurrentUser() == null) return;
        final String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name  = doc.getString("name");
                    String dept  = doc.getString("department");
                    String role  = doc.getString("role");
                    String photo = doc.getString("photoUrl");

                    tvUserName.setText(name != null ? name : "Teacher");
                    String displayRole = "cr".equalsIgnoreCase(role) ? "CR" : "Teacher";
                    tvUserRole.setText(displayRole + " — " +
                            (dept != null ? dept : "") + " Dept.");

                    // ✅ Skip memory cache for instant photo update
                    if (photo != null && !photo.isEmpty()) {
                        Glide.with(TeacherDashboardActivity.this)
                                .load(photo)
                                .circleCrop()
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.bg_avatar_purple)
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.bg_avatar_purple);
                    }

                    // ✅ Only attach task listener if not already running
                    if (taskListener == null) {
                        loadMyTasks(uid);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Profile error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void loadMyTasks(final String teacherUid) {
        // ✅ Remove old listener before attaching new one
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        taskListener = db.collection("tasks")
                .whereEqualTo("teacherUid", teacherUid)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    taskList.clear();
                    int ct = 0, asgn = 0, quiz = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task == null) continue;
                        task.setTaskId(doc.getId());
                        taskList.add(task);

                        String type = task.getType();
                        if ("CT".equals(type))              ct++;
                        else if ("Assignment".equals(type)) asgn++;
                        else if ("Quiz".equals(type))       quiz++;
                    }

                    tvCtCount.setText(String.valueOf(ct));
                    tvAsgnCount.setText(String.valueOf(asgn));
                    tvQuizCount.setText(String.valueOf(quiz));
                    taskAdapter.updateList(new ArrayList<>(taskList));
                });
    }
}