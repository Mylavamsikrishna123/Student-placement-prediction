package com.placement.models;

/**
 * Plain model for a student. This mirrors the main columns in the database
 * and is intentionally minimal so it stays easy to pass around.
 */
public class Student {
    private int id;
    private String name;
    private String email;
    private String idNumber;
    private String department;
    private String branch;
    private String degree;
    private double cgpa;
    private String collegeName;
    private String phone;
    private String certifications;
    private int backlogs;

    public Student() {}

    /**
     * Convenience constructor for full initialization.
     */
    public Student(int id, String name, String email, String idNumber, String department,
                   String branch, String degree, double cgpa, String collegeName, String phone,
                   String certifications, int backlogs) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.idNumber = idNumber;
        this.department = department;
        this.branch = branch;
        this.degree = degree;
        this.cgpa = cgpa;
        this.collegeName = collegeName;
        this.phone = phone;
        this.certifications = certifications;
        this.backlogs = backlogs;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    public int getBacklogs() { return backlogs; }
    public void setBacklogs(int backlogs) { this.backlogs = backlogs; }
}
