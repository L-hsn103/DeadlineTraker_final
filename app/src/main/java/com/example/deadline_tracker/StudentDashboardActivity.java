package com.example.deadline_tracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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

public class StudentDashboardActivity extends AppCompatActivity {

    private RecyclerView    recyclerTasks;
    private TaskAdapter     taskAdapter;
    private List<Task>      taskList = new ArrayList<>();

    private TextView        tvUserName, tvUserRole;
    private TextView        tvUrgentCount, tvTotalCount;
    private CircleImageView ivAvatar;

    private FirebaseFirestore    db;
    private FirebaseAuth         mAuth;

    // ✅ Store listener so we can remove it and avoid stacking
    private ListenerRegistration taskListener;

    private String currentDept  = "";
    private String currentBatch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvUserName    = findViewById(R.id.tvUserName);
        tvUserRole    = findViewById(R.id.tvUserRole);
        tvUrgentCount = findViewById(R.id.tvUrgentCount);
        tvTotalCount  = findViewById(R.id.tvWeekCount);
        ivAvatar      = findViewById(R.id.ivAvatar);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, taskList, this::openTaskDetail);
        recyclerTasks.setAdapter(taskAdapter);

        setupNavigation();
        requestNotifPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile(); // reload profile + photo on every resume
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ✅ Remove listener when screen is not visible — prevents stacking
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }
    }

    private void openTaskDetail(Task task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("taskId",         task.getTaskId()         != null ? task.getTaskId()         : "");
        intent.putExtra("title",          task.getTitle()          != null ? task.getTitle()          : "");
        intent.putExtra("type",           task.getType()           != null ? task.getType()           : "");
        intent.putExtra("date",           task.getDate()           != null ? task.getDate()           : "");
        intent.putExtra("time",           task.getTime()           != null ? task.getTime()           : "");
        intent.putExtra("description",    task.getDescription()    != null ? task.getDescription()    : "");
        intent.putExtra("department",     task.getDepartment()     != null ? task.getDepartment()     : "");
        intent.putExtra("teacherName",    task.getTeacherName()    != null ? task.getTeacherName()    : "");
        intent.putExtra("teacherId",      task.getTeacherId()      != null ? task.getTeacherId()      : "");
        intent.putExtra("attachmentUrl",  task.getAttachmentUrl()  != null ? task.getAttachmentUrl()  : "");
        intent.putExtra("attachmentName", task.getAttachmentName() != null ? task.getAttachmentName() : "");
        intent.putExtra("timestamp",      task.getTimestamp());
        if (task.getBatches() != null)
            intent.putExtra("batches", String.join(", ", task.getBatches()));
        startActivity(intent);
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v ->
                recyclerTasks.smoothScrollToPosition(0));
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navNotif).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationListActivity.class)));
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    String name  = doc.getString("name");
                    String dept  = doc.getString("department");
                    String batch = doc.getString("batch");
                    String photo = doc.getString("photoUrl");

                    tvUserName.setText(name != null ? name : "Student");
                    tvUserRole.setText(
                            (dept  != null ? dept  : "") + " • Batch " +
                                    (batch != null ? batch : ""));

                    // ✅ Skip memory cache for instant photo update
                    if (photo != null && !photo.isEmpty()) {
                        Glide.with(this)
                                .load(photo)
                                .circleCrop()
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.bg_avatar_green)
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.bg_avatar_green);
                    }

                    // ✅ Only attach task listener once per dept+batch
                    if (!dept.equals(currentDept) || !batch.equals(currentBatch)
                            || taskListener == null) {
                        currentDept  = dept != null ? dept : "";
                        currentBatch = batch != null ? batch : "";
                        loadTasks(currentDept, currentBatch);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Profile Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void loadTasks(String dept, String batch) {
        if (dept == null || dept.isEmpty()) return;

        // ✅ Remove old listener before attaching a new one
        if (taskListener != null) {
            taskListener.remove();
            taskListener = null;
        }

        taskListener = db.collection("tasks")
                .whereEqualTo("department", dept)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    taskList.clear();
                    int  urgentCount  = 0;
                    long currentTime  = System.currentTimeMillis();
                    long oneDayMillis = 24 * 60 * 60 * 1000L;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task == null) continue;
                        task.setTaskId(doc.getId());

                        if (task.getBatches() != null &&
                                task.getBatches().contains(batch)) {
                            taskList.add(task);
                            if (task.getTimestamp() > currentTime &&
                                    (task.getTimestamp() - currentTime) < oneDayMillis)
                                urgentCount++;
                        }
                    }

                    if (tvUrgentCount != null)
                        tvUrgentCount.setText(String.valueOf(urgentCount));
                    if (tvTotalCount != null)
                        tvTotalCount.setText(String.valueOf(taskList.size()));

                    taskAdapter.updateList(new ArrayList<>(taskList));
                });
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
}