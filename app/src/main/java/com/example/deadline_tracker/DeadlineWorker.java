package com.example.deadline_tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DeadlineWorker extends Worker {
    private static final String CHANNEL_ID = "deadline_channel";

    public DeadlineWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        String title  = getInputData().getString("title");
        String date   = getInputData().getString("date");
        String time   = getInputData().getString("time");
        String when   = getInputData().getString("when");
        String taskId = getInputData().getString("taskId");

        Context context = getApplicationContext();
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Deadline Reminders", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            manager.createNotificationChannel(ch);
        }

        Intent intent = new Intent(context, StudentDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String body = title + "\n📅 " + date + " at " + time + "\n(" + when + ")";
        manager.notify(taskId.hashCode(),
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("⏰ Upcoming Deadline")
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true).setContentIntent(pi)
                        .setVibrate(new long[]{0, 500, 200, 500}).build());

        return Result.success();
    }
}