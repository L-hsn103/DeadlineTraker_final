package com.example.deadline_tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deadline_tracker.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationListActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private List<Task>   allTasks      = new ArrayList<>();
    private List<Task>   filteredTasks = new ArrayList<>();
    private NotifAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;
    private String studentDepartment = "";
    private String studentBatch      = "";
    private TextView tabAll, tabUrgent, tabToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotifAdapter(filteredTasks);
        recyclerNotifications.setAdapter(adapter);

        tabAll    = findViewById(R.id.tabAll);
        tabUrgent = findViewById(R.id.tabUrgent);
        tabToday  = findViewById(R.id.tabToday);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvMarkAll).setOnClickListener(v -> adapter.markAllRead());
        tabAll.setOnClickListener(v    -> filterTasks("all"));
        tabUrgent.setOnClickListener(v -> filterTasks("urgent"));
        tabToday.setOnClickListener(v  -> filterTasks("today"));

        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.navSettings).setOnClickListener(v ->
                startActivity(new Intent(this,
                        com.example.deadline_tracker.SettingsActivity.class)));

        loadUserThenTasks();
    }

    private void loadUserThenTasks() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    studentDepartment = doc.getString("department");
                    studentBatch      = doc.getString("batch");
                    loadTasks();
                });
    }

    private void loadTasks() {
        db.collection("tasks")
                .whereEqualTo("department", studentDepartment)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    allTasks.clear();
                    for (var doc : snapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());
                        if (task.getBatches() != null &&
                                task.getBatches().contains(studentBatch)) {
                            allTasks.add(task);
                        }
                    }
                    filterTasks("all");
                });
    }

    private void filterTasks(String filter) {
        tabAll.setBackgroundResource(R.drawable.bg_chip_default);
        tabAll.setTextColor(0x66FFFFFF);
        tabUrgent.setBackgroundResource(R.drawable.bg_chip_default);
        tabUrgent.setTextColor(0x66FFFFFF);
        tabToday.setBackgroundResource(R.drawable.bg_chip_default);
        tabToday.setTextColor(0x66FFFFFF);

        long now = System.currentTimeMillis();
        long day = 24 * 60 * 60 * 1000L;
        filteredTasks.clear();

        switch (filter) {
            case "urgent":
                tabUrgent.setBackgroundResource(R.drawable.bg_chip_batch_sel);
                tabUrgent.setTextColor(0xFFC4B5FD);
                for (Task t : allTasks)
                    if (t.getTimestamp() > now && t.getTimestamp() - now <= day)
                        filteredTasks.add(t);
                break;
            case "today":
                tabToday.setBackgroundResource(R.drawable.bg_chip_batch_sel);
                tabToday.setTextColor(0xFFC4B5FD);
                for (Task t : allTasks) {
                    long diff = t.getTimestamp() - now;
                    if (diff > 0 && diff <= day) filteredTasks.add(t);
                }
                break;
            default:
                tabAll.setBackgroundResource(R.drawable.bg_chip_batch_sel);
                tabAll.setTextColor(0xFF4ADE80);
                filteredTasks.addAll(allTasks);
                break;
        }
        adapter.notifyDataSetChanged();
    }

    class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        List<Task>    list;
        List<Boolean> readStatus = new ArrayList<>();

        NotifAdapter(List<Task> list) { this.list = list; }

        public void markAllRead() {
            readStatus.clear();
            for (int i = 0; i < list.size(); i++) readStatus.add(true);
            notifyDataSetChanged();
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Task task = list.get(pos);
            long now  = System.currentTimeMillis();
            long diff = task.getTimestamp() - now;
            long day  = 24 * 60 * 60 * 1000L;

            h.title.setText(task.getTitle());
            h.desc.setText("📅 " + task.getDate() +
                    (task.getTime() != null && !task.getTime().isEmpty()
                            ? " — " + task.getTime() : ""));

            if (diff <= 0) {
                h.time.setText("Deadline passed");
                h.time.setTextColor(0xFF9CA3AF);
            } else if (diff <= day) {
                h.time.setText("Due in less than 24 hours!");
                h.time.setTextColor(0xFFF87171);
            } else {
                long days = diff / day;
                h.time.setText(days + " day" + (days > 1 ? "s" : "") + " remaining");
                h.time.setTextColor(0xFF4ADE80);
            }

            switch (task.getType() != null ? task.getType() : "") {
                case "CT":
                    h.icon.setText("📝");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_ct);
                    break;
                case "Quiz":
                    h.icon.setText("❓");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_quiz);
                    break;
                default:
                    h.icon.setText("📋");
                    h.iconBg.setBackgroundResource(R.drawable.bg_stat_asgn);
                    break;
            }

            boolean isRead = pos < readStatus.size() && readStatus.get(pos);
            h.unreadDot.setVisibility(isRead ? View.GONE : View.VISIBLE);

            h.itemView.setOnClickListener(v -> {
                while (readStatus.size() <= pos) readStatus.add(false);
                readStatus.set(pos, true);
                notifyItemChanged(pos);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView    title, desc, time, icon;
            FrameLayout iconBg;
            View        unreadDot;

            VH(View v) {
                super(v);
                title     = v.findViewById(R.id.notifTitle);
                desc      = v.findViewById(R.id.notifDesc);
                time      = v.findViewById(R.id.notifTime);
                icon      = v.findViewById(R.id.notifIcon);
                iconBg    = v.findViewById(R.id.notifIconBg);
                unreadDot = v.findViewById(R.id.unreadDot);
            }
        }
    }
}