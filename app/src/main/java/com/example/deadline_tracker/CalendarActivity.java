package com.example.deadline_tracker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deadline_tracker.model.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {

    private GridLayout   calendarGrid;
    private TextView     tvCurrentMonth, tvDateLabel;
    private LinearLayout calTaskContainer;

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;

    private Calendar currentCal = Calendar.getInstance();
    private String   userRole   = "";
    private String   userDept   = "";
    private String   userBatch  = "";
    private String   userUid    = "";

    // taskDateMap: date key → list of tasks
    private final Map<String, List<Task>> dateTaskMap = new HashMap<>();
    // submissionDateMap: date key → list of submission info (teacher only)
    private final Map<String, List<String>> dateSubmissionMap = new HashMap<>();

    private String selectedDateKey = ""; // currently selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        db      = FirebaseFirestore.getInstance();
        mAuth   = FirebaseAuth.getInstance();
        userUid = mAuth.getCurrentUser().getUid();

        calendarGrid     = findViewById(R.id.calendarGrid);
        tvCurrentMonth   = findViewById(R.id.tvCurrentMonth);
        tvDateLabel      = findViewById(R.id.tvDateLabel);
        calTaskContainer = findViewById(R.id.calTaskContainer);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, -1);
            refreshCalendar();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentCal.add(Calendar.MONTH, 1);
            refreshCalendar();
        });

        findViewById(R.id.calNavHome).setOnClickListener(v -> finish());
        findViewById(R.id.calNavNotif).setOnClickListener(v -> navigateToNotif());
        findViewById(R.id.calNavSettings).setOnClickListener(v -> navigateToSettings());

        loadUserProfile();
    }

    private void loadUserProfile() {
        db.collection("users").document(userUid).get()
                .addOnSuccessListener(doc -> {
                    userRole  = doc.getString("role");
                    userDept  = doc.getString("department");
                    userBatch = doc.getString("batch") != null
                            ? doc.getString("batch") : "";
                    loadTasks();

                    // ✅ If teacher/CR, also load submissions
                    if ("teacher".equals(userRole) || "cr".equals(userRole)) {
                        loadSubmissionsForTeacher();
                    }
                });
    }

    private void loadTasks() {
        db.collection("tasks")
                .whereEqualTo("department", userDept)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    dateTaskMap.clear();

                    for (var doc : snapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());

                        if ("student".equals(userRole)) {
                            if (task.getBatches() == null ||
                                    !task.getBatches().contains(userBatch))
                                continue;
                        }

                        String date = task.getDate() != null
                                ? task.getDate().trim() : "";
                        if (date.isEmpty()) continue;

                        if (!dateTaskMap.containsKey(date))
                            dateTaskMap.put(date, new ArrayList<>());
                        dateTaskMap.get(date).add(task);
                    }

                    refreshCalendar();
                });
    }

    // ✅ Load all submissions grouped by submission date (teacher only)
    private void loadSubmissionsForTeacher() {
        db.collection("tasks")
                .whereEqualTo("teacherUid", userUid)
                .get()
                .addOnSuccessListener(taskSnaps -> {
                    for (QueryDocumentSnapshot taskDoc : taskSnaps) {
                        String taskId    = taskDoc.getId();
                        String taskTitle = taskDoc.getString("title");

                        db.collection("submissions")
                                .document(taskId)
                                .collection("students")
                                .get()
                                .addOnSuccessListener(subSnaps -> {
                                    for (QueryDocumentSnapshot subDoc : subSnaps) {
                                        Long submittedAt = subDoc.getLong("submittedAt");
                                        String studentName = subDoc.getString("studentName");
                                        String studentId   = subDoc.getString("studentId");
                                        if (submittedAt == null) continue;

                                        // Convert timestamp to date key
                                        Calendar cal = Calendar.getInstance();
                                        cal.setTimeInMillis(submittedAt);
                                        String dateKey = cal.get(Calendar.DAY_OF_MONTH) + "/" +
                                                (cal.get(Calendar.MONTH) + 1) + "/" +
                                                cal.get(Calendar.YEAR);

                                        String info = (studentName != null ? studentName : "Student") +
                                                (studentId != null && studentId.length() >= 2
                                                        ? " (**" + studentId.substring(studentId.length() - 2) + ")"
                                                        : "") +
                                                " → " + (taskTitle != null ? taskTitle : "Task");

                                        if (!dateSubmissionMap.containsKey(dateKey))
                                            dateSubmissionMap.put(dateKey, new ArrayList<>());
                                        dateSubmissionMap.get(dateKey).add(info);
                                    }
                                    // refresh if this is the selected date
                                    if (!selectedDateKey.isEmpty()) {
                                        showTasksForDate(selectedDateKey,
                                                dateTaskMap.get(selectedDateKey));
                                    }
                                });
                    }
                });
    }

    private void refreshCalendar() {
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        tvCurrentMonth.setText(fmt.format(currentCal.getTime()));

        calendarGrid.removeAllViews();

        Calendar temp = (Calendar) currentCal.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int startDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int totalDays      = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today    = Calendar.getInstance();
        int todayDay      = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth    = today.get(Calendar.MONTH);
        int todayYear     = today.get(Calendar.YEAR);
        int viewMonth     = currentCal.get(Calendar.MONTH);
        int viewYear      = currentCal.get(Calendar.YEAR);

        for (int i = 0; i < startDayOfWeek; i++)
            calendarGrid.addView(makeEmptyCell());

        for (int day = 1; day <= totalDays; day++) {
            boolean isToday = (day == todayDay
                    && viewMonth == todayMonth
                    && viewYear  == todayYear);

            String dateKey       = day + "/" + (viewMonth + 1) + "/" + viewYear;
            List<Task> tasksOnDay = dateTaskMap.get(dateKey);
            boolean hasTask      = tasksOnDay != null && !tasksOnDay.isEmpty();
            boolean hasSub       = dateSubmissionMap.containsKey(dateKey)
                    && !dateSubmissionMap.get(dateKey).isEmpty();

            calendarGrid.addView(
                    makeDayCell(day, isToday, hasTask, hasSub, tasksOnDay, dateKey));
        }
    }

    private View makeEmptyCell() {
        View v = new View(this);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width      = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.height     = dpToPx(48);
        v.setLayoutParams(p);
        return v;
    }

    private View makeDayCell(int day, boolean isToday, boolean hasTask,
                             boolean hasSub, List<Task> tasks, String dateKey) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width      = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.height     = GridLayout.LayoutParams.WRAP_CONTENT;
        p.setMargins(2, 2, 2, 2);
        cell.setLayoutParams(p);

        TextView tvDay = new TextView(this);
        tvDay.setText(String.valueOf(day));
        tvDay.setTextSize(13);
        tvDay.setGravity(Gravity.CENTER);
        tvDay.setPadding(4, 10, 4, 4);

        if (isToday) {
            tvDay.setTextColor(0xFFC4B5FD);
            tvDay.setTypeface(null, Typeface.BOLD);
            cell.setBackgroundResource(R.drawable.bg_cal_today);
        } else {
            tvDay.setTextColor(0x99FFFFFF);
        }
        cell.addView(tvDay);

        // ✅ Task dot
        if (hasTask) {
            View dot = new View(this);
            LinearLayout.LayoutParams dotP =
                    new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
            dotP.gravity      = Gravity.CENTER_HORIZONTAL;
            dotP.bottomMargin = dpToPx(2);
            dot.setLayoutParams(dotP);
            String firstType = tasks.get(0).getType();
            if ("CT".equals(firstType))
                dot.setBackgroundResource(R.drawable.bg_dot_red);
            else if ("Quiz".equals(firstType))
                dot.setBackgroundResource(R.drawable.bg_dot_yellow);
            else
                dot.setBackgroundResource(R.drawable.bg_dot_purple);
            cell.addView(dot);
        }

        // ✅ Submission dot (green) for teacher
        if (hasSub) {
            View subDot = new View(this);
            LinearLayout.LayoutParams subDotP =
                    new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
            subDotP.gravity      = Gravity.CENTER_HORIZONTAL;
            subDotP.bottomMargin = dpToPx(4);
            subDot.setLayoutParams(subDotP);
            subDot.setBackgroundResource(R.drawable.bg_dot_blue);
            cell.addView(subDot);
        }

        cell.setOnClickListener(v -> {
            selectedDateKey = dateKey;
            showTasksForDate(dateKey, tasks);
        });

        return cell;
    }

    private void showTasksForDate(String dateKey, List<Task> tasks) {
        calTaskContainer.removeAllViews();

        boolean hasTasks = tasks != null && !tasks.isEmpty();
        List<String> subs = dateSubmissionMap.get(dateKey);
        boolean hasSubs   = subs != null && !subs.isEmpty();

        if (!hasTasks && !hasSubs) {
            tvDateLabel.setText("No tasks on " + dateKey);

            // ✅ Teacher can add task from empty date
            if ("teacher".equals(userRole) || "cr".equals(userRole)) {
                tvDateLabel.setText("No tasks on " + dateKey + "  —  tap + to add");
                calTaskContainer.addView(makeAddTaskButton(dateKey));
            } else {
                TextView empty = new TextView(this);
                empty.setText("No tasks on this day");
                empty.setTextSize(12);
                empty.setTextColor(0x40FFFFFF);
                empty.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dpToPx(20);
                empty.setLayoutParams(lp);
                calTaskContainer.addView(empty);
            }
            return;
        }

        int totalItems = (hasTasks ? tasks.size() : 0) + (hasSubs ? subs.size() : 0);
        tvDateLabel.setText(dateKey + "  —  " + totalItems + " item(s)");

        // ✅ Show tasks
        if (hasTasks) {
            if (tasks.size() > 1)
                calTaskContainer.addView(makeConflictBanner(tasks.size()));

            for (Task task : tasks)
                calTaskContainer.addView(makeTaskCard(task));
        }

        // ✅ Show submissions section (teacher only)
        if (hasSubs && ("teacher".equals(userRole) || "cr".equals(userRole))) {
            calTaskContainer.addView(makeSubmissionsSection(subs));
        }

        // ✅ Add task button always visible for teacher
        if ("teacher".equals(userRole) || "cr".equals(userRole)) {
            calTaskContainer.addView(makeAddTaskButton(dateKey));
        }
    }

    // ✅ "Add Task" button for teacher — pre-fills the date
    private View makeAddTaskButton(String dateKey) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(R.drawable.bg_button_purple);
        btn.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(12);
        btn.setLayoutParams(lp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("＋  Add Task on " + dateKey);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(Color.WHITE);
        tvLabel.setTypeface(null, Typeface.BOLD);
        btn.addView(tvLabel);

        btn.setOnClickListener(v -> {
            // Open AddTaskActivity and pass the selected date
            Intent intent = new Intent(this, AddTaskActivity.class);
            intent.putExtra("prefillDate", dateKey);
            startActivity(intent);
        });

        return btn;
    }

    // ✅ Submissions section card for teacher
    private View makeSubmissionsSection(List<String> subs) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundResource(R.drawable.bg_task_card);
        section.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin    = dpToPx(8);
        lp.bottomMargin = dpToPx(8);
        section.setLayoutParams(lp);

        // Header
        TextView header = new TextView(this);
        header.setText("📥 SUBMISSIONS ON THIS DAY");
        header.setTextSize(10);
        header.setTextColor(0xFF4ADE80);
        header.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dpToPx(8);
        header.setLayoutParams(hlp);
        section.addView(header);

        // Each submission
        for (String subInfo : subs) {
            TextView tv = new TextView(this);
            tv.setText("• " + subInfo);
            tv.setTextSize(12);
            tv.setTextColor(0xCCFFFFFF);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.bottomMargin = dpToPx(4);
            tv.setLayoutParams(tlp);
            section.addView(tv);
        }

        return section;
    }

    private View makeConflictBanner(int count) {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setBackgroundResource(R.drawable.bg_conflict_banner);
        banner.setPadding(dpToPx(13), dpToPx(10), dpToPx(13), dpToPx(10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dpToPx(10);
        banner.setLayoutParams(lp);

        TextView warn = new TextView(this);
        warn.setText("⚠️ Date conflict — " + count +
                " tasks on this day. Consider rescheduling.");
        warn.setTextSize(11);
        warn.setTextColor(0xFFFCA5A5);
        warn.setLineSpacing(dpToPx(2), 1);
        banner.addView(warn);

        return banner;
    }

    private View makeTaskCard(Task task) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_task_card);
        card.setPadding(dpToPx(13), dpToPx(13), dpToPx(13), dpToPx(13));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cardLp);

        View dot = new View(this);
        LinearLayout.LayoutParams dotLp =
                new LinearLayout.LayoutParams(dpToPx(10), dpToPx(10));
        dotLp.rightMargin = dpToPx(11);
        dot.setLayoutParams(dotLp);

        switch (task.getType() != null ? task.getType() : "") {
            case "CT":         dot.setBackgroundResource(R.drawable.bg_dot_red);    break;
            case "Quiz":       dot.setBackgroundResource(R.drawable.bg_dot_yellow); break;
            case "Assignment": dot.setBackgroundResource(R.drawable.bg_dot_purple); break;
            default:           dot.setBackgroundResource(R.drawable.bg_dot_blue);   break;
        }
        card.addView(dot);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(task.getTitle());
        tvTitle.setTextSize(13);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTypeface(null, Typeface.BOLD);
        info.addView(tvTitle);

        if (task.getTeacherName() != null && !task.getTeacherName().isEmpty()) {
            TextView tvTeacher = new TextView(this);
            tvTeacher.setText("👤 " + task.getTeacherName());
            tvTeacher.setTextSize(11);
            tvTeacher.setTextColor(0x59FFFFFF);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dpToPx(3);
            tvTeacher.setLayoutParams(tlp);
            info.addView(tvTeacher);
        }

        if (task.getTime() != null && !task.getTime().isEmpty()) {
            TextView tvTime = new TextView(this);
            tvTime.setText("🕐 " + task.getTime());
            tvTime.setTextSize(11);
            tvTime.setTextColor(0x59FFFFFF);
            info.addView(tvTime);
        }

        if (task.getBatches() != null && !task.getBatches().isEmpty()) {
            TextView tvBatch = new TextView(this);
            tvBatch.setText("Batch " + String.join(", ", task.getBatches()));
            tvBatch.setTextSize(10);
            tvBatch.setTextColor(0x66FFFFFF);
            tvBatch.setBackgroundResource(R.drawable.bg_chip_default);
            tvBatch.setPadding(dpToPx(10), dpToPx(3), dpToPx(10), dpToPx(3));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = dpToPx(5);
            tvBatch.setLayoutParams(blp);
            info.addView(tvBatch);
        }

        card.addView(info);

        TextView tvTag = new TextView(this);
        tvTag.setText(task.getType() != null ? task.getType() : "");
        tvTag.setTextSize(10);
        tvTag.setTypeface(null, Typeface.BOLD);
        tvTag.setPadding(dpToPx(9), dpToPx(4), dpToPx(9), dpToPx(4));

        switch (task.getType() != null ? task.getType() : "") {
            case "CT":
                tvTag.setTextColor(0xFF93C5FD);
                tvTag.setBackgroundResource(R.drawable.bg_tag_ct);
                break;
            case "Assignment":
                tvTag.setTextColor(0xFFC4B5FD);
                tvTag.setBackgroundResource(R.drawable.bg_tag_asgn);
                break;
            case "Quiz":
                tvTag.setTextColor(0xFFFCD34D);
                tvTag.setBackgroundResource(R.drawable.bg_tag_quiz);
                break;
            default:
                tvTag.setTextColor(0xFF93C5FD);
                tvTag.setBackgroundResource(R.drawable.bg_tag_ct);
                break;
        }
        card.addView(tvTag);

        return card;
    }

    private void navigateToNotif() {
        if ("teacher".equals(userRole) || "cr".equals(userRole))
            startActivity(new Intent(this, TeacherNotificationActivity.class));
        else
            startActivity(new Intent(this, NotificationListActivity.class));
    }

    private void navigateToSettings() {
        if ("teacher".equals(userRole) || "cr".equals(userRole))
            startActivity(new Intent(this, TeacherSettingsActivity.class));
        else
            startActivity(new Intent(this, SettingsActivity.class));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}