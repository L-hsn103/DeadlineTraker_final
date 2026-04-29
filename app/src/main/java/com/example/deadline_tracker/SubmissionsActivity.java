package com.example.deadline_tracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmissionsActivity extends AppCompatActivity {

    private TextView     tvBack, tvTaskTitle, tvSubmissionCount, tvEmptyState;
    private RecyclerView recyclerView;
    private ProgressBar  progressBar;

    private FirebaseFirestore db;
    private String taskId    = "";
    private String taskTitle = "";

    private final List<SubmissionItem> submissionList = new ArrayList<>();
    private SubmissionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submissions);

        db        = FirebaseFirestore.getInstance();
        taskId    = getIntent().getStringExtra("taskId");
        taskTitle = getIntent().getStringExtra("taskTitle");

        tvBack            = findViewById(R.id.tvBack);
        tvTaskTitle       = findViewById(R.id.tvTaskTitle);
        tvSubmissionCount = findViewById(R.id.tvSubmissionCount);
        tvEmptyState      = findViewById(R.id.tvEmptyState);
        recyclerView      = findViewById(R.id.recyclerSubmissions);
        progressBar       = findViewById(R.id.progressBar);

        tvBack.setOnClickListener(v -> finish());
        tvTaskTitle.setText(taskTitle != null ? taskTitle : "Submissions");

        adapter = new SubmissionAdapter(submissionList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadSubmissions();
    }

    private void loadSubmissions() {
        if (taskId == null || taskId.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        db.collection("submissions")
                .document(taskId)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    progressBar.setVisibility(View.GONE);
                    submissionList.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        SubmissionItem item = new SubmissionItem();
                        item.studentName = doc.getString("studentName");
                        item.studentId   = doc.getString("studentId");   // ✅ fetch ID
                        item.fileName    = doc.getString("fileName");
                        item.fileUrl     = doc.getString("fileUrl");
                        item.submittedAt = doc.getLong("submittedAt") != null
                                ? doc.getLong("submittedAt") : 0L;
                        submissionList.add(item);
                    }

                    // Sort latest first
                    submissionList.sort((a, b) -> Long.compare(b.submittedAt, a.submittedAt));
                    adapter.notifyDataSetChanged();

                    int count = submissionList.size();
                    tvSubmissionCount.setText(count + " student" +
                            (count != 1 ? "s" : "") + " submitted");

                    if (count == 0) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Failed to load submissions: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Get last 2 digits of student ID ─────────────────────
    // e.g. "CSE2023045" → "**45"
    // e.g. "20210034"   → "**34"
    private String maskStudentId(String studentId) {
        if (studentId == null || studentId.isEmpty()) return "ID: ****";
        String trimmed = studentId.trim();
        if (trimmed.length() <= 2) return "ID: " + trimmed;
        String last2 = trimmed.substring(trimmed.length() - 2);
        return "ID: **" + last2;
    }

    // ── Data model ───────────────────────────────────────────
    static class SubmissionItem {
        String studentName;
        String studentId;     // ✅ NEW
        String fileName;
        String fileUrl;
        long   submittedAt;
    }

    // ── Adapter ──────────────────────────────────────────────
    class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

        private final List<SubmissionItem> list;

        SubmissionAdapter(List<SubmissionItem> list) {
            this.list = list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_submission_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SubmissionItem item = list.get(position);

            // ✅ Show name + masked ID e.g. "Rahul Ahmed  •  ID: **45"
            String name    = item.studentName != null ? item.studentName : "Unknown Student";
            String maskedId = maskStudentId(item.studentId);
            holder.tvStudentName.setText(name + "  •  " + maskedId);

            holder.tvFileName.setText(
                    item.fileName != null ? "📎 " + item.fileName : "📎 file");

            if (item.submittedAt > 0) {
                String date = new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(item.submittedAt));
                holder.tvSubmittedAt.setText("Submitted: " + date);
            } else {
                holder.tvSubmittedAt.setText("");
            }

            holder.btnDownload.setOnClickListener(v -> {
                if (item.fileUrl != null && !item.fileUrl.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(item.fileUrl));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(SubmissionsActivity.this,
                            "File not available", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvFileName, tvSubmittedAt, btnDownload;

            ViewHolder(View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.tvStudentName);
                tvFileName    = itemView.findViewById(R.id.tvFileName);
                tvSubmittedAt = itemView.findViewById(R.id.tvSubmittedAt);
                btnDownload   = itemView.findViewById(R.id.btnDownloadSubmission);
            }
        }
    }
}