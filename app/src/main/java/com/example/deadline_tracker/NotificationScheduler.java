package com.example.deadline_tracker;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    public static void scheduleTaskNotification(
            Context context, String taskId, String title, String date, String time) {

        long taskTimestamp = parseDateTime(date, time);
        long now = System.currentTimeMillis();

        long delay24h = taskTimestamp - now - (24 * 60 * 60 * 1000L);
        if (delay24h > 0) scheduleWork(context, taskId, title, date, time, delay24h, "24 hours before deadline");

        long delay2h = taskTimestamp - now - (2 * 60 * 60 * 1000L);
        if (delay2h > 0)  scheduleWork(context, taskId, title, date, time, delay2h, "2 hours before deadline");
    }

    private static void scheduleWork(Context context, String taskId, String title,
                                     String date, String time, long delayMs, String when) {
        Data data = new Data.Builder()
                .putString("taskId", taskId).putString("title", title)
                .putString("date", date).putString("time", time).putString("when", when).build();

        WorkManager.getInstance(context).enqueue(
                new OneTimeWorkRequest.Builder(DeadlineWorker.class)
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .setInputData(data).addTag(taskId).build());
    }

    public static void cancelTaskNotification(Context context, String taskId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(taskId);
    }

    private static long parseDateTime(String date, String time) {
        try {
            String[] d = date.split("/");
            int day = Integer.parseInt(d[0].trim());
            int month = Integer.parseInt(d[1].trim()) - 1;
            int year  = Integer.parseInt(d[2].trim());
            String[] t = time.replace(" AM","").replace(" PM","").split(":");
            int hour   = Integer.parseInt(t[0].trim());
            int minute = Integer.parseInt(t[1].trim());
            if (time.contains("PM") && hour != 12) hour += 12;
            if (time.contains("AM") && hour == 12) hour = 0;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(year, month, day, hour, minute, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis() + (24 * 60 * 60 * 1000L);
        }
    }
}