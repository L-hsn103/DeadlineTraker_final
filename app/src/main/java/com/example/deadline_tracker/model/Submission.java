package com.example.deadline_tracker.model;

public class Submission {
    private String studentUid;
    private String studentName;
    private String fileUrl;
    private String fileName;
    private long submittedAt;
    private String status = "Received";

    // Required empty constructor for Firebase
    public Submission() {}

    // Getters
    public String getStudentUid() { return studentUid; }
    public String getStudentName() { return studentName; }
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }
    public long getSubmittedAt() { return submittedAt; }
    public String getStatus() { return status; }

    // Setters (Firebase needs these to populate the object)
    public void setStudentUid(String studentUid) { this.studentUid = studentUid; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }
    public void setStatus(String status) { this.status = status; }
}