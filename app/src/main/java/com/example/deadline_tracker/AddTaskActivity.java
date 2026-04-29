package com.example.deadline_tracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    private EditText     etTaskTitle, etDateTime, etDepartment, etBatches, etDescription;
    private TextView     tvBack, chipCT, chipAssignment, chipQuiz, tvSelectedFile;
    private LinearLayout attachFileBtn;
    private Button       btnPostTask;
    private ProgressBar  progressBar;

    private String selectedType = "CT";
    private Uri    fileUri      = null;
    private String fileName     = "";

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    fileUri  = uri;
                    fileName = getFileName(uri);
                    tvSelectedFile.setText("📎 " + fileName);
                    tvSelectedFile.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        CloudinaryManager.init(this);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
        autoFillTeacherInfo();

        // ✅ If launched from calendar, pre-fill the date
        String prefillDate = getIntent().getStringExtra("prefillDate");
        if (prefillDate != null && !prefillDate.isEmpty()) {
            // prefillDate is like "29/4/2026" — convert to "2026-04-29"
            try {
                String[] parts = prefillDate.split("/");
                int day   = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year  = Integer.parseInt(parts[2]);
                String formatted = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d", year, month, day);
                etDateTime.setText(formatted + " | ");
                etDateTime.setHint("Tap to change time");
            } catch (Exception ignored) {}
        }
    }

    private void initViews() {
        etTaskTitle    = findViewById(R.id.etTaskTitle);
        etDateTime     = findViewById(R.id.etDateTime);
        etDepartment   = findViewById(R.id.etDepartment);
        etBatches      = findViewById(R.id.etBatches);
        etDescription  = findViewById(R.id.etDescription);
        tvBack         = findViewById(R.id.tvBack);
        chipCT         = findViewById(R.id.chipCT);
        chipAssignment = findViewById(R.id.chipAssignment);
        chipQuiz       = findViewById(R.id.chipQuiz);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        attachFileBtn  = findViewById(R.id.attachFileBtn);
        btnPostTask    = findViewById(R.id.btnPostTask);
        progressBar    = findViewById(R.id.progressBar);
    }

    private void autoFillTeacherInfo() {
        String uid = mAuth.getUid();
        if (uid == null) return;
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String dept = doc.getString("department");
                        if (dept != null && !dept.isEmpty()) {
                            etDepartment.setText(dept);
                            etDepartment.setEnabled(false);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        tvBack.setOnClickListener(v -> finish());
        etDateTime.setOnClickListener(v -> showDateTimePicker());
        chipCT.setOnClickListener(v -> selectType("CT"));
        chipAssignment.setOnClickListener(v -> selectType("Assignment"));
        chipQuiz.setOnClickListener(v -> selectType("Quiz"));
        attachFileBtn.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"application/pdf", "image/*"}));
        btnPostTask.setOnClickListener(v -> validateAndUpload());
    }

    private void showDateTimePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d", year, month + 1, day);
            new TimePickerDialog(this, (view1, hour, min) -> {
                String amPm     = hour >= 12 ? "PM" : "AM";
                int    dispHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                String time     = String.format(Locale.getDefault(),
                        "%02d:%02d %s", dispHour, min, amPm);
                etDateTime.setText(date + " | " + time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void selectType(String type) {
        selectedType = type;
        chipCT.setBackgroundResource(R.drawable.bg_chip_default);
        chipAssignment.setBackgroundResource(R.drawable.bg_chip_default);
        chipQuiz.setBackgroundResource(R.drawable.bg_chip_default);
        chipCT.setTextColor(0x66FFFFFF);
        chipAssignment.setTextColor(0x66FFFFFF);
        chipQuiz.setTextColor(0x66FFFFFF);

        if ("CT".equals(type)) {
            chipCT.setBackgroundResource(R.drawable.bg_chip_ct_sel);
            chipCT.setTextColor(0xFF93C5FD);
        } else if ("Assignment".equals(type)) {
            chipAssignment.setBackgroundResource(R.drawable.bg_chip_asgn_sel);
            chipAssignment.setTextColor(0xFFC4B5FD);
        } else {
            chipQuiz.setBackgroundResource(R.drawable.bg_chip_quiz_sel);
            chipQuiz.setTextColor(0xFFFCD34D);
        }
    }

    private void validateAndUpload() {
        String title = etTaskTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTaskTitle.setError("Title required");
            return;
        }
        String dateTime = etDateTime.getText().toString().trim();
        if (dateTime.isEmpty() || dateTime.equals(" | ")) {
            Toast.makeText(this, "Please pick a date and time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // File size check — max 20 MB
        if (fileUri != null) {
            try {
                AssetFileDescriptor fd =
                        getContentResolver().openAssetFileDescriptor(fileUri, "r");
                if (fd != null) {
                    long size = fd.getLength();
                    fd.close();
                    if (size > 20 * 1024 * 1024) {
                        Toast.makeText(this,
                                "File too large! Maximum allowed size is 20 MB.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        progressBar.setVisibility(View.VISIBLE);
        btnPostTask.setEnabled(false);

        if (fileUri != null) {
            uploadToCloudinary(title);
        } else {
            saveToFirestore(title, "", "");
        }
    }

    private void uploadToCloudinary(String title) {
        btnPostTask.setText("Uploading... 0%");

        CloudinaryManager.uploadFile(this, fileUri, "deadline_tracker/tasks",
                new CloudinaryManager.UploadListener() {
                    @Override
                    public void onProgress(int percent) {
                        runOnUiThread(() ->
                                btnPostTask.setText("Uploading... " + percent + "%"));
                    }

                    @Override
                    public void onSuccess(String fileUrl, String publicId) {
                        runOnUiThread(() -> {
                            btnPostTask.setText("Post Task");
                            saveToFirestore(title, fileUrl, fileName);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnPostTask.setEnabled(true);
                            btnPostTask.setText("Post Task");
                            Toast.makeText(AddTaskActivity.this,
                                    "Upload failed: " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void saveToFirestore(String title, String fileUrl, String attachName) {
        final String uid      = mAuth.getUid();
        final String dateTime = etDateTime.getText().toString().trim();

        final String[] parts = dateTime.contains("|")
                ? dateTime.split("\\|")
                : new String[]{dateTime};
        final String date = parts[0].trim();
        final String time = parts.length > 1 ? parts[1].trim() : "";

        final long         timestamp   = parseTimestamp(date, time);
        final List<String> batchList   = Arrays.asList(
                etBatches.getText().toString().trim().split("\\s*,\\s*"));
        final String dept              = etDepartment.getText().toString().trim();
        final String description       = etDescription.getText().toString().trim();
        final String type              = selectedType;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String teacherName = doc.getString("name");
                    String teacherId   = doc.getString("teacherId");

                    Map<String, Object> task = new HashMap<>();
                    task.put("title",          title);
                    task.put("date",           date);
                    task.put("time",           time);
                    task.put("dateTime",       dateTime);
                    task.put("type",           type);
                    task.put("department",     dept);
                    task.put("batches",        batchList);
                    task.put("description",    description);
                    task.put("attachmentUrl",  fileUrl);
                    task.put("attachmentName", attachName);
                    task.put("teacherUid",     uid);
                    task.put("teacherName",    teacherName != null ? teacherName : "");
                    task.put("teacherId",      teacherId   != null ? teacherId   : "");
                    task.put("timestamp",      timestamp);

                    db.collection("tasks").add(task)
                            .addOnSuccessListener(ref -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AddTaskActivity.this,
                                        "Task posted successfully!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnPostTask.setEnabled(true);
                                Toast.makeText(AddTaskActivity.this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnPostTask.setEnabled(true);
                    Toast.makeText(AddTaskActivity.this,
                            "Failed to fetch profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private long parseTimestamp(String date, String time) {
        try {
            String[] dp  = date.split("-");
            int year     = Integer.parseInt(dp[0].trim());
            int month    = Integer.parseInt(dp[1].trim()) - 1;
            int day      = Integer.parseInt(dp[2].trim());
            int hour = 0, minute = 0;
            if (!time.isEmpty()) {
                String[] tp  = time.split(":");
                hour         = Integer.parseInt(tp[0].trim());
                String[] map = tp[1].trim().split(" ");
                minute       = Integer.parseInt(map[0]);
                if (map.length > 1) {
                    boolean pm = map[1].equalsIgnoreCase("PM");
                    if (pm && hour != 12) hour += 12;
                    if (!pm && hour == 12) hour = 0;
                }
            }
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, hour, minute, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000);
        }
    }

    private String getFileName(Uri uri) {
        String result = "file";
        try (android.database.Cursor c =
                     getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = c.getString(idx);
            }
        } catch (Exception ignored) {}
        return result;
    }
}