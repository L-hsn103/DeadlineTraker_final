package com.example.deadline_tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deadline_tracker.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherNotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerTeacherNotif;
    private TeacherNotifAdapter adapter;
    private List<Task> allTasks = new ArrayList<>();
    private List<Task> filteredTasks = new ArrayList<>();
    private TextView tabAll, tabPostedByMe, tabConflict;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String teacherUid = "";
    private String department = "";
    private Map<String, List<Task>> dateTaskMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_notification);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        teacherUid = user.getUid();

        recyclerTeacherNotif = findViewById(R.id.recyclerTeacherNotif);
        recyclerTeacherNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherNotifAdapter(filteredTasks);
        recyclerTeacherNotif.setAdapter(adapter);

        tabAll = findViewById(R.id.tabAll);
        tabPostedByMe = findViewById(R.id.tabPostedByMe);
        tabConflict = findViewById(R.id.tabConflict);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvMarkAll).setOnClickListener(v -> adapter.markAllRead());

        tabAll.setOnClickListener(v -> filterTasks("all"));
        tabPostedByMe.setOnClickListener(v -> filterTasks("mine"));
        tabConflict.setOnClickListener(v -> filterTasks("conflict"));

        loadDepartmentThenTasks();
    }

    private void loadDepartmentThenTasks() {
        db.collection("users").document(teacherUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        department = doc.getString("department");
                        loadAllDeptTasks();
                    }
                });
    }

    private void loadAllDeptTasks() {
        db.collection("tasks")
                .whereEqualTo("department", department)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    allTasks.clear();
                    dateTaskMap.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setTaskId(doc.getId());
                            allTasks.add(task);

                            String date = task.getDate();
                            if (!dateTaskMap.containsKey(date))
                                dateTaskMap.put(date, new ArrayList<>());
                            dateTaskMap.get(date).add(task);
                        }
                    }
                    filterTasks("all");
                });
    }

    private void filterTasks(String filter) {
        resetTabStyles();
        filteredTasks.clear();

        switch (filter) {
            case "mine":
                highlightTab(tabPostedByMe);
                for (Task t : allTasks)
                    if (teacherUid.equals(t.getTeacherUid()))
                        filteredTasks.add(t);
                break;
            case "conflict":
                highlightTab(tabConflict);
                for (Task t : allTasks) {
                    List<Task> onSameDate = dateTaskMap.get(t.getDate());
                    if (onSameDate != null && onSameDate.size() > 1)
                        filteredTasks.add(t);
                }
                break;
            default:
                highlightTab(tabAll);
                filteredTasks.addAll(allTasks);
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private void resetTabStyles() {
        tabAll.setBackgroundResource(R.drawable.bg_chip_default);
        tabAll.setTextColor(0x66FFFFFF);
        tabPostedByMe.setBackgroundResource(R.drawable.bg_chip_default);
        tabPostedByMe.setTextColor(0x66FFFFFF);
        tabConflict.setBackgroundResource(R.drawable.bg_chip_default);
        tabConflict.setTextColor(0x66FFFFFF);
    }

    private void highlightTab(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_batch_sel);
        tab.setTextColor(0xFFC4B5FD);
    }

    class TeacherNotifAdapter extends RecyclerView.Adapter<TeacherNotifAdapter.VH> {
        List<Task> list;
        List<Boolean> readStatus = new ArrayList<>();

        TeacherNotifAdapter(List<Task> list) { this.list = list; }

        public void markAllRead() {
            readStatus.clear();
            for (int i = 0; i < list.size(); i++) readStatus.add(true);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Task task = list.get(pos);
            long now = System.currentTimeMillis();
            long diff = task.getTimestamp() - now;
            long day = 24 * 60 * 60 * 1000L;

            h.title.setText(task.getTitle());
            h.meta.setText("📅 " + task.getDate() + (task.getTime().isEmpty() ? "" : " — " + task.getTime()));

            if (task.getBatches() != null) {
                h.batches.setText("Batch " + task.getBatches());
            }

            // Type and Icon logic
            String type = task.getType() != null ? task.getType() : "CT";
            h.type.setText(type);
            setupTypeStyle(h, type);

            // Countdown logic
            if (diff <= 0) {
                h.countdown.setText("Deadline passed");
                h.countdown.setTextColor(0xFF9CA3AF);
            } else if (diff <= day) {
                h.countdown.setText("Due in < 24h!");
                h.countdown.setTextColor(0xFFF87171);
            } else {
                long days = diff / day;
                h.countdown.setText(days + " days remaining");
                h.countdown.setTextColor(0xFF4ADE80);
            }

            // Conflict Banner
            List<Task> onSameDate = dateTaskMap.get(task.getDate());
            if (onSameDate != null && onSameDate.size() > 1) {
                h.conflictBanner.setVisibility(View.VISIBLE);
                int others = onSameDate.size() - 1;
                h.conflictText.setText("⚠️ " + others + " other task(s) on this date.");
            } else {
                h.conflictBanner.setVisibility(View.GONE);
            }

            boolean isRead = pos < readStatus.size() && readStatus.get(pos);
            h.unreadDot.setVisibility(isRead ? View.GONE : View.VISIBLE);

            h.itemView.setOnClickListener(v -> {
                while (readStatus.size() <= pos) readStatus.add(false);
                readStatus.set(pos, true);
                notifyItemChanged(pos);
            });
        }

        private void setupTypeStyle(VH h, String type) {
            switch (type) {
                case "CT":
                    h.type.setTextColor(0xFF93C5FD);
                    h.type.setBackgroundResource(R.drawable.bg_tag_ct);
                    h.icon.setText("📝");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_ct);
                    break;
                case "Quiz":
                    h.type.setTextColor(0xFFFCD34D);
                    h.type.setBackgroundResource(R.drawable.bg_tag_quiz);
                    h.icon.setText("❓");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_quiz);
                    break;
                default:
                    h.type.setTextColor(0xFFC4B5FD);
                    h.type.setBackgroundResource(R.drawable.bg_tag_asgn);
                    h.icon.setText("📋");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_asgn);
                    break;
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, meta, batches, type, countdown, icon, conflictText;
            FrameLayout iconBg;
            LinearLayout conflictBanner;
            View unreadDot;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.notifTitle);
                meta = v.findViewById(R.id.notifMeta);
                batches = v.findViewById(R.id.notifBatches);
                type = v.findViewById(R.id.notifType);
                countdown = v.findViewById(R.id.notifCountdown);
                icon = v.findViewById(R.id.notifIcon);
                iconBg = v.findViewById(R.id.notifIconBg);
                conflictBanner = v.findViewById(R.id.conflictBanner);
                conflictText = v.findViewById(R.id.conflictText);
                unreadDot = v.findViewById(R.id.unreadDot);
            }
        }
    }
}