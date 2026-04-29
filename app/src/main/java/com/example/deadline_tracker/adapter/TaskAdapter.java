package com.example.deadline_tracker.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deadline_tracker.R;
import com.example.deadline_tracker.TaskDetailActivity;
import com.example.deadline_tracker.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> taskList,
                       OnTaskClickListener listener) {
        this.context  = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder,
                                 int position) {
        holder.bind(taskList.get(position));
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    public void updateList(List<Task> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvTaskMeta, tvTaskTag;
        View     dotView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvTaskMeta = itemView.findViewById(R.id.tvTaskMeta);
            tvTaskTag  = itemView.findViewById(R.id.tvTaskTag);
            dotView    = itemView.findViewById(R.id.taskDot);
        }

        public void bind(Task task) {
            tvTaskName.setText(task.getTitle() != null
                    ? task.getTitle() : "");

            // Meta line
            String meta = "📅 " + task.getDate();
            if (task.getTime() != null &&
                    !task.getTime().isEmpty())
                meta += " — " + task.getTime();
            if (task.getTeacherName() != null &&
                    !task.getTeacherName().isEmpty())
                meta += "\n👤 " + task.getTeacherName();
            tvTaskMeta.setText(meta);

            // Type tag + dot
            String type = task.getType() != null
                    ? task.getType() : "";
            tvTaskTag.setText(type);
            switch (type) {
                case "CT":
                    tvTaskTag.setTextColor(0xFF93C5FD);
                    tvTaskTag.setBackgroundResource(
                            R.drawable.bg_tag_ct);
                    dotView.setBackgroundResource(
                            R.drawable.bg_dot_red);
                    break;
                case "Assignment":
                    tvTaskTag.setTextColor(0xFFC4B5FD);
                    tvTaskTag.setBackgroundResource(
                            R.drawable.bg_tag_asgn);
                    dotView.setBackgroundResource(
                            R.drawable.bg_dot_purple);
                    break;
                default:
                    tvTaskTag.setTextColor(0xFFFCD34D);
                    tvTaskTag.setBackgroundResource(
                            R.drawable.bg_tag_quiz);
                    dotView.setBackgroundResource(
                            R.drawable.bg_dot_yellow);
                    break;
            }

            // Tap → open TaskDetailActivity
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context,
                        TaskDetailActivity.class);

                intent.putExtra("taskId",
                        task.getTaskId());
                intent.putExtra("title",
                        task.getTitle());
                intent.putExtra("type",
                        task.getType());
                intent.putExtra("date",
                        task.getDate());
                intent.putExtra("time",
                        task.getTime());
                intent.putExtra("description",
                        task.getDescription());
                intent.putExtra("department",
                        task.getDepartment());
                intent.putExtra("teacherName",
                        task.getTeacherName());
                intent.putExtra("teacherId",
                        task.getTeacherId());
                intent.putExtra("timestamp",
                        task.getTimestamp());

                // ← THESE 2 were missing before
                intent.putExtra("attachmentUrl",
                        task.getAttachmentUrl() != null
                                ? task.getAttachmentUrl() : "");
                intent.putExtra("attachmentName",
                        task.getAttachmentName() != null
                                ? task.getAttachmentName() : "");

                // Batches as comma string
                if (task.getBatches() != null) {
                    intent.putExtra("batches",
                            String.join(", ",
                                    task.getBatches()));
                }

                context.startActivity(intent);
                if (listener != null)
                    listener.onTaskClick(task);
            });
        }
    }
}