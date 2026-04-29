package com.example.deadline_tracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.deadline_tracker.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeacherSettingsActivity extends AppCompatActivity {

    private CircleImageView profilePhoto;
    private TextView        tvProfileName, tvProfileRole, tvTeacherId;
    private EditText        etEditName, etEditDept;
    private Button          btnSaveProfile;
    private RecyclerView    recyclerMyTasks;
    private MyTaskAdapter   myTaskAdapter;
    private List<Task>      myTasks = new ArrayList<>();

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private String            currentUid;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    profilePhoto.setImageURI(uri);
                    uploadPhotoToCloudinary(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_settings);

        CloudinaryManager.init(this);

        mAuth      = FirebaseAuth.getInstance();
        db         = FirebaseFirestore.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        profilePhoto   = findViewById(R.id.profilePhoto);
        tvProfileName  = findViewById(R.id.tvProfileName);
        tvProfileRole  = findViewById(R.id.tvProfileRole);
        tvTeacherId    = findViewById(R.id.tvTeacherId);
        etEditName     = findViewById(R.id.etEditName);
        etEditDept     = findViewById(R.id.etEditDept);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        recyclerMyTasks = findViewById(R.id.recyclerMyTasks);
        recyclerMyTasks.setLayoutManager(new LinearLayoutManager(this));
        myTaskAdapter = new MyTaskAdapter(myTasks);
        recyclerMyTasks.setAdapter(myTaskAdapter);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangePhoto).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
        findViewById(R.id.btnChangePassword).setOnClickListener(v ->
                showChangePasswordDialog());

        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navNotif).setOnClickListener(v ->
                startActivity(new Intent(this, TeacherNotificationActivity.class)));

        loadProfile();
        loadMyTasks();
    }

    private void loadProfile() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    String name    = doc.getString("name");
                    String dept    = doc.getString("department");
                    String teachId = doc.getString("teacherId");
                    String photo   = doc.getString("photoUrl");

                    tvProfileName.setText(name != null ? name : "");
                    tvProfileRole.setText("Teacher — " +
                            (dept != null ? dept : "") + " Dept.");
                    tvTeacherId.setText(teachId != null ? teachId : "");
                    etEditName.setText(name);
                    etEditDept.setText(dept);

                    // ✅ Load profile photo
                    if (photo != null && !photo.isEmpty()) {
                        Glide.with(this).load(photo).circleCrop()
                                .placeholder(R.drawable.bg_avatar_purple)
                                .into(profilePhoto);
                    }
                });
    }

    private void loadMyTasks() {
        db.collection("tasks")
                .whereEqualTo("teacherUid", currentUid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    myTasks.clear();
                    for (var doc : snapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());
                        myTasks.add(task);
                    }
                    myTaskAdapter.notifyDataSetChanged();
                });
    }

    // ✅ Upload to Cloudinary instead of Firebase Storage
    private void uploadPhotoToCloudinary(Uri uri) {
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show();

        CloudinaryManager.uploadFile(this, uri,
                "deadline_tracker/profile_photos",
                new CloudinaryManager.UploadListener() {
                    @Override
                    public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String fileUrl, String publicId) {
                        db.collection("users").document(currentUid)
                                .update("photoUrl", fileUrl)
                                .addOnSuccessListener(unused ->
                                        runOnUiThread(() -> {
                                            Toast.makeText(TeacherSettingsActivity.this,
                                                    "✅ Photo updated!",
                                                    Toast.LENGTH_SHORT).show();
                                            Glide.with(TeacherSettingsActivity.this)
                                                    .load(fileUrl).circleCrop()
                                                    .into(profilePhoto);
                                        }));
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(TeacherSettingsActivity.this,
                                        "Upload failed: " + error,
                                        Toast.LENGTH_LONG).show());
                    }
                });
    }

    private void saveProfile() {
        String name = etEditName.getText().toString().trim();
        String dept = etEditDept.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etEditName.setError("Required"); return; }
        if (TextUtils.isEmpty(dept)) { etEditDept.setError("Required"); return; }

        btnSaveProfile.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",       name);
        updates.put("department", dept);

        db.collection("users").document(currentUid).update(updates)
                .addOnSuccessListener(unused -> {
                    btnSaveProfile.setEnabled(true);
                    tvProfileName.setText(name);
                    tvProfileRole.setText("Teacher — " + dept + " Dept.");
                    Toast.makeText(this, "✅ Profile updated!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showChangePasswordDialog() {
        EditText etNewPassword = new EditText(this);
        etNewPassword.setHint("New password (min 6 chars)");
        etNewPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNewPassword.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(etNewPassword)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = etNewPassword.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(this,
                                "Password must be at least 6 characters",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.getCurrentUser().updatePassword(newPass)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this,
                                            "✅ Password updated!",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed (please re-login and try again): "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Delete \"" + task.getTitle() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("tasks").document(task.getTaskId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                NotificationScheduler.cancelTaskNotification(
                                        this, task.getTaskId());
                                Toast.makeText(this, "Task deleted",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Inner Adapter ────────────────────────────────────────
    class MyTaskAdapter extends RecyclerView.Adapter<MyTaskAdapter.VH> {
        List<Task> list;

        MyTaskAdapter(List<Task> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_task, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Task task = list.get(pos);
            h.title.setText(task.getTitle());
            String batchStr = task.getBatches() != null
                    ? "Batch " + String.join(", ", task.getBatches()) : "";
            h.date.setText("📅 " + task.getDate() + "  " + batchStr);

            if (task.getType() != null) {
                h.type.setText(task.getType());
                switch (task.getType()) {
                    case "CT":
                        h.type.setTextColor(0xFF93C5FD);
                        h.type.setBackgroundResource(R.drawable.bg_tag_ct);
                        h.dot.setBackgroundResource(R.drawable.bg_dot_red);
                        break;
                    case "Quiz":
                        h.type.setTextColor(0xFFFCD34D);
                        h.type.setBackgroundResource(R.drawable.bg_tag_quiz);
                        h.dot.setBackgroundResource(R.drawable.bg_dot_yellow);
                        break;
                    default:
                        h.type.setTextColor(0xFFC4B5FD);
                        h.type.setBackgroundResource(R.drawable.bg_tag_asgn);
                        h.dot.setBackgroundResource(R.drawable.bg_dot_purple);
                        break;
                }
            }
            h.btnDelete.setOnClickListener(v -> deleteTask(task));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView    title, date, type;
            View        dot;
            FrameLayout btnDelete;

            VH(View v) {
                super(v);
                title     = v.findViewById(R.id.tvTaskTitle);
                date      = v.findViewById(R.id.tvTaskDate);
                type      = v.findViewById(R.id.tvTaskType);
                dot       = v.findViewById(R.id.taskDot);
                btnDelete = v.findViewById(R.id.btnDelete);
            }
        }
    }
}