package com.placement.models;

public class Company {
    private int id;
    private String name;
    private String industry;
    private String location;
    private double minCgpa;
    private int salary;

    public Company() {}

    public Company(int id, String name, String industry, String location, double minCgpa, int salary) {
        this.id = id;
        this.name = name;
        this.industry = industry;
        this.location = location;
        this.minCgpa = minCgpa;
        this.salary = salary;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getMinCgpa() { return minCgpa; }
    public void setMinCgpa(double minCgpa) { this.minCgpa = minCgpa; }

    public int getSalary() { return salary; }
    public void setSalary(int salary) { this.salary = salary; }
}
