package com.example.deadline_tracker.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String department;
    private String batch;
    private String studentId;
    private String teacherId;

    public User() {}

    public User(String uid, String name, String email, String role,
                String department, String batch, String studentId, String teacherId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
        this.batch = batch;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    public String getUid()          { return uid; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getRole()         { return role; }
    public String getDepartment()   { return department; }
    public String getBatch()        { return batch; }
    public String getStudentId()    { return studentId; }
    public String getTeacherId()    { return teacherId; }

    public void setUid(String uid)          { this.uid = uid; }
    public void setName(String name)        { this.name = name; }
    public void setEmail(String email)      { this.email = email; }
    public void setRole(String role)        { this.role = role; }
    public void setDepartment(String d)     { this.department = d; }
    public void setBatch(String b)          { this.batch = b; }
    public void setStudentId(String id)     { this.studentId = id; }
    public void setTeacherId(String id)     { this.teacherId = id; }
}