package com.example.deadline_tracker;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TaskDetailActivity extends AppCompatActivity {

    // ── TextViews ────────────────────────────────────────────
    private TextView tvDetailTitle, tvDetailType, tvDetailDate, tvDetailTime;
    private TextView tvDetailDept, tvDetailBatch, tvDetailTeacher;
    private TextView tvDetailDescription, tvDetailTimeLeft, tvDetailStatus;
    private TextView tvDetailCountdown, tvAttachmentName, btnDownload;
    private TextView tvUploadLabel, tvSubmissionFileName, tvSubmissionStatus;
    private TextView labelAttachment;
    private TextView labelSubmission;

    // ── LinearLayouts ────────────────────────────────────────
    private LinearLayout cardAttachment;
    private LinearLayout cardSubmissionStatus;
    private LinearLayout btnUploadSubmission;

    // ── Buttons ──────────────────────────────────────────────
    private Button      btnSubmit;
    private Button      btnResubmit;        // ✅ NEW resubmit button
    private Button      btnViewSubmissions;
    private ProgressBar progressBar;

    private String taskId             = "";
    private String attachmentUrl      = "";
    private String userRole           = "";
    private String studentName        = "";
    private String studentId          = "";
    private Uri    submissionUri      = null;
    private String submissionFileName = "";
    private boolean isResubmitting    = false; // ✅ track resubmit mode

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    submissionUri      = uri;
                    submissionFileName = getFileName(uri);
                    tvUploadLabel.setText(submissionFileName);
                    tvUploadLabel.setTextColor(0xFF4ADE80);
                    tvSubmissionFileName.setText("📎 " + submissionFileName);
                    tvSubmissionFileName.setVisibility(View.VISIBLE);
                    btnSubmit.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        CloudinaryManager.init(this);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ── Bind views ───────────────────────────────────────
        tvDetailTitle        = findViewById(R.id.tvDetailTitle);
        tvDetailType         = findViewById(R.id.tvDetailType);
        tvDetailDate         = findViewById(R.id.tvDetailDate);
        tvDetailTime         = findViewById(R.id.tvDetailTime);
        tvDetailDept         = findViewById(R.id.tvDetailDept);
        tvDetailBatch        = findViewById(R.id.tvDetailBatch);
        tvDetailTeacher      = findViewById(R.id.tvDetailTeacher);
        tvDetailDescription  = findViewById(R.id.tvDetailDescription);
        tvDetailTimeLeft     = findViewById(R.id.tvDetailTimeLeft);
        tvDetailStatus       = findViewById(R.id.tvDetailStatus);
        tvDetailCountdown    = findViewById(R.id.tvDetailCountdown);
        tvAttachmentName     = findViewById(R.id.tvAttachmentName);
        btnDownload          = findViewById(R.id.btnDownload);
        cardAttachment       = findViewById(R.id.cardAttachment);
        labelAttachment      = findViewById(R.id.labelAttachment);
        labelSubmission      = findViewById(R.id.labelSubmission);
        cardSubmissionStatus = findViewById(R.id.cardSubmissionStatus);
        tvSubmissionStatus   = findViewById(R.id.tvSubmissionStatus);
        btnUploadSubmission  = findViewById(R.id.btnUploadSubmission);
        tvUploadLabel        = findViewById(R.id.tvUploadLabel);
        tvSubmissionFileName = findViewById(R.id.tvSubmissionFileName);
        btnSubmit            = findViewById(R.id.btnSubmit);
        btnResubmit          = findViewById(R.id.btnResubmit);
        btnViewSubmissions   = findViewById(R.id.btnViewSubmissions);
        progressBar          = findViewById(R.id.progressBar);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        // ── Intent extras ────────────────────────────────────
        taskId                   = getIntent().getStringExtra("taskId");
        final String title       = getIntent().getStringExtra("title");
        final String type        = getIntent().getStringExtra("type");
        final String date        = getIntent().getStringExtra("date");
        final String time        = getIntent().getStringExtra("time");
        final String description = getIntent().getStringExtra("description");
        final String department  = getIntent().getStringExtra("department");
        final String batches     = getIntent().getStringExtra("batches");
        final String teacherName = getIntent().getStringExtra("teacherName");
        final long   timestamp   = getIntent().getLongExtra("timestamp", 0);
        attachmentUrl            = getIntent().getStringExtra("attachmentUrl");
        final String attachName  = getIntent().getStringExtra("attachmentName");

        // ── Fill UI ──────────────────────────────────────────
        tvDetailTitle.setText(title != null ? title : "");
        tvDetailDate.setText(date != null ? date : "—");
        tvDetailTime.setText(time != null && !time.isEmpty() ? time : "—");
        tvDetailDept.setText(department != null ? department : "—");
        tvDetailBatch.setText(batches != null ? batches : "—");
        tvDetailTeacher.setText(teacherName != null ? teacherName : "—");
        tvDetailDescription.setText(
                description != null && !description.isEmpty()
                        ? description : "No description provided.");

        // ── Type tag ─────────────────────────────────────────
        if (type != null) {
            tvDetailType.setText(type);
            switch (type) {
                case "CT":
                    tvDetailType.setTextColor(0xFF93C5FD);
                    tvDetailType.setBackgroundResource(R.drawable.bg_tag_ct);
                    break;
                case "Assignment":
                    tvDetailType.setTextColor(0xFFC4B5FD);
                    tvDetailType.setBackgroundResource(R.drawable.bg_tag_asgn);
                    break;
                case "Quiz":
                    tvDetailType.setTextColor(0xFFFCD34D);
                    tvDetailType.setBackgroundResource(R.drawable.bg_tag_quiz);
                    break;
            }
        }

        setTimeRemaining(timestamp);

        // ── Teacher attachment ────────────────────────────────
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            labelAttachment.setVisibility(View.VISIBLE);
            cardAttachment.setVisibility(View.VISIBLE);
            tvAttachmentName.setText(
                    attachName != null && !attachName.isEmpty()
                            ? attachName : "Attached file");
            btnDownload.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(attachmentUrl));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }

        loadUserRole(type);

        btnUploadSubmission.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{
                        "application/pdf", "image/jpeg", "image/png"}));

        btnSubmit.setOnClickListener(v -> submitAssignment());

        // ✅ Resubmit — confirm then open file picker
        btnResubmit.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Resubmit Assignment")
                    .setMessage("This will replace your previous submission. Continue?")
                    .setPositiveButton("Yes, Resubmit", (dialog, which) -> {
                        isResubmitting = true;
                        // reset UI for new upload
                        cardSubmissionStatus.setVisibility(View.GONE);
                        btnResubmit.setVisibility(View.GONE);
                        btnUploadSubmission.setVisibility(View.VISIBLE);
                        tvUploadLabel.setText("Tap to upload your new solution (PDF/image)");
                        tvUploadLabel.setTextColor(0x99FFFFFF);
                        tvSubmissionFileName.setVisibility(View.GONE);
                        btnSubmit.setVisibility(View.GONE);
                        submissionUri = null;
                        filePickerLauncher.launch(new String[]{
                                "application/pdf", "image/jpeg", "image/png"});
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnViewSubmissions.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubmissionsActivity.class);
            intent.putExtra("taskId",    taskId);
            intent.putExtra("taskTitle", title);
            startActivity(intent);
        });
    }

    private void loadUserRole(String taskType) {
        final String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    userRole    = doc.getString("role");
                    studentName = doc.getString("name");
                    studentId   = doc.getString("studentId") != null
                            ? doc.getString("studentId") : "";

                    if ("teacher".equals(userRole) || "cr".equals(userRole)) {
                        btnViewSubmissions.setVisibility(View.VISIBLE);
                        loadSubmissionCount();

                    } else if ("student".equals(userRole)
                            && "Assignment".equals(taskType)
                            && attachmentUrl != null
                            && !attachmentUrl.isEmpty()) {

                        labelSubmission.setVisibility(View.VISIBLE);
                        btnUploadSubmission.setVisibility(View.VISIBLE);
                        checkExistingSubmission();
                    }
                });
    }

    private void loadSubmissionCount() {
        if (taskId == null || taskId.isEmpty()) return;
        db.collection("submissions")
                .document(taskId)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();
                    btnViewSubmissions.setText(count == 0
                            ? "View Submissions (none yet)"
                            : "View Submissions (" + count + ")");
                });
    }

    private void checkExistingSubmission() {
        if (taskId == null || taskId.isEmpty()) return;
        final String uid = mAuth.getCurrentUser().getUid();
        db.collection("submissions")
                .document(taskId)
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fn = doc.getString("fileName");
                        // ✅ Show submitted status + resubmit button
                        cardSubmissionStatus.setVisibility(View.VISIBLE);
                        tvSubmissionStatus.setText("✅ Submitted: " +
                                (fn != null ? fn : "file"));
                        btnUploadSubmission.setVisibility(View.GONE);
                        btnSubmit.setVisibility(View.GONE);
                        btnResubmit.setVisibility(View.VISIBLE); // ✅ show resubmit
                    }
                });
    }

    private void submitAssignment() {
        if (submissionUri == null) {
            Toast.makeText(this, "Please select a file first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // File size check — max 10 MB
        try {
            AssetFileDescriptor fd =
                    getContentResolver().openAssetFileDescriptor(submissionUri, "r");
            if (fd != null) {
                long size = fd.getLength();
                fd.close();
                if (size > 10 * 1024 * 1024) {
                    Toast.makeText(this,
                            "File too large! Maximum allowed size is 10 MB.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (Exception ignored) {}

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading... 0%");

        final String uid = mAuth.getCurrentUser().getUid();

        CloudinaryManager.uploadFile(this, submissionUri,
                "deadline_tracker/submissions/" + taskId,
                new CloudinaryManager.UploadListener() {
                    @Override
                    public void onProgress(int percent) {
                        runOnUiThread(() ->
                                btnSubmit.setText("Uploading... " + percent + "%"));
                    }

                    @Override
                    public void onSuccess(String fileUrl, String publicId) {
                        runOnUiThread(() -> saveSubmissionToFirestore(uid, fileUrl));
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText(isResubmitting
                                    ? "Resubmit Assignment" : "Submit Assignment");
                            Toast.makeText(TaskDetailActivity.this,
                                    "Upload failed: " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void saveSubmissionToFirestore(String uid, String fileUrl) {
        java.util.Map<String, Object> submission = new java.util.HashMap<>();
        submission.put("studentUid",  uid);
        submission.put("studentName", studentName);
        submission.put("studentId",   studentId);
        submission.put("fileUrl",     fileUrl);
        submission.put("fileName",    submissionFileName);
        submission.put("submittedAt", System.currentTimeMillis());
        submission.put("taskId",      taskId);
        // ✅ track how many times submitted
        submission.put("resubmitted", isResubmitting);

        db.collection("submissions")
                .document(taskId)
                .collection("students")
                .document(uid)
                .set(submission)  // set() overwrites previous submission
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Assignment");
                    cardSubmissionStatus.setVisibility(View.VISIBLE);
                    tvSubmissionStatus.setText("✅ " +
                            (isResubmitting ? "Resubmitted: " : "Submitted: ")
                            + submissionFileName);
                    btnUploadSubmission.setVisibility(View.GONE);
                    btnSubmit.setVisibility(View.GONE);
                    tvSubmissionFileName.setVisibility(View.GONE);
                    btnResubmit.setVisibility(View.VISIBLE); // ✅ allow resubmit again
                    isResubmitting = false;
                    Toast.makeText(this,
                            "Assignment submitted successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Assignment");
                    Toast.makeText(this,
                            "Failed to save submission: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setTimeRemaining(long timestamp) {
        long now  = System.currentTimeMillis();
        long diff = timestamp - now;

        if (diff <= 0) {
            tvDetailTimeLeft.setText("Deadline has passed");
            tvDetailTimeLeft.setTextColor(0xFF9CA3AF);
            tvDetailStatus.setText("Expired");
            tvDetailStatus.setTextColor(0xFF9CA3AF);
            tvDetailCountdown.setText("⌛ Deadline passed");
            tvDetailCountdown.setTextColor(0xFF9CA3AF);
        } else {
            long days    = diff / (24 * 60 * 60 * 1000L);
            long hours   = (diff % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
            long minutes = (diff % (60 * 60 * 1000L)) / (60 * 1000L);

            String timeLeftStr;
            if (days > 0)
                timeLeftStr = days + " day" + (days > 1 ? "s" : "") +
                        " " + hours + " hr remaining";
            else if (hours > 0)
                timeLeftStr = hours + " hour" + (hours > 1 ? "s" : "") +
                        " " + minutes + " min remaining";
            else
                timeLeftStr = minutes + " minute" + (minutes != 1 ? "s" : "") +
                        " remaining";

            tvDetailTimeLeft.setText(timeLeftStr);
            tvDetailCountdown.setText("⏰ " + timeLeftStr);

            if (diff < 24 * 60 * 60 * 1000L) {
                tvDetailTimeLeft.setTextColor(0xFFF87171);
                tvDetailCountdown.setTextColor(0xFFF87171);
                tvDetailStatus.setText("Urgent!");
                tvDetailStatus.setTextColor(0xFFF87171);
            } else if (diff < 3 * 24 * 60 * 60 * 1000L) {
                tvDetailTimeLeft.setTextColor(0xFFFBBF24);
                tvDetailCountdown.setTextColor(0xFFFBBF24);
                tvDetailStatus.setText("Coming soon");
                tvDetailStatus.setTextColor(0xFFFBBF24);
            } else {
                tvDetailTimeLeft.setTextColor(0xFF4ADE80);
                tvDetailCountdown.setTextColor(0xFF4ADE80);
                tvDetailStatus.setText("Upcoming");
                tvDetailStatus.setTextColor(0xFF4ADE80);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = "submitted_file";
        try (android.database.Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) result = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return result;
    }
}