package com.example.deadline_tracker.model;

import java.util.List;

public class Task {

    private String       taskId;
    private String       title;
    private String       description;
    private String       type;
    private String       department;
    private String       teacherName;
    private String       teacherUid;
    private String       teacherId;
    private String       attachmentUrl;
    private String       attachmentName;
    private long         timestamp;

    // Separate date and time fields
    private String       date;
    private String       time;

    // batches MUST be List<String> — this was the crash cause
    private List<String> batches;

    // Required empty constructor for Firestore
    public Task() {}

    // ── Getters ──────────────────────────────────────────────
    public String       getTaskId()        { return taskId; }
    public String       getTitle()         { return title; }
    public String       getDescription()   { return description; }
    public String       getType()          { return type; }
    public String       getDepartment()    { return department; }
    public String       getTeacherName()   { return teacherName; }
    public String       getTeacherUid()    { return teacherUid; }
    public String       getTeacherId()     { return teacherId; }
    public String       getAttachmentUrl() { return attachmentUrl; }
    public String       getAttachmentName(){ return attachmentName; }
    public long         getTimestamp()     { return timestamp; }
    public List<String> getBatches()       { return batches; }

    // getDate and getTime work with BOTH old and new data format
    public String getDate() {
        // New format: separate date field
        if (date != null && !date.isEmpty()) return date;
        // Old format: dateTime field with | separator
        return "";
    }

    public String getTime() {
        // New format: separate time field
        if (time != null && !time.isEmpty()) return time;
        return "";
    }

    // ── Setters ──────────────────────────────────────────────
    public void setTaskId(String taskId)               { this.taskId = taskId; }
    public void setTitle(String title)                 { this.title = title; }
    public void setDescription(String desc)            { this.description = desc; }
    public void setType(String type)                   { this.type = type; }
    public void setDepartment(String department)       { this.department = department; }
    public void setTeacherName(String teacherName)     { this.teacherName = teacherName; }
    public void setTeacherUid(String teacherUid)       { this.teacherUid = teacherUid; }
    public void setTeacherId(String teacherId)         { this.teacherId = teacherId; }
    public void setAttachmentUrl(String url)           { this.attachmentUrl = url; }
    public void setAttachmentName(String name)         { this.attachmentName = name; }
    public void setTimestamp(long timestamp)           { this.timestamp = timestamp; }
    public void setBatches(List<String> batches)       { this.batches = batches; }
    public void setDate(String date)                   { this.date = date; }
    public void setTime(String time)                   { this.time = time; }
}