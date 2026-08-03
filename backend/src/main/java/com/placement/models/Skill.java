package com.placement.models;

/**
 * Minimal skill record. Names are unique in the database and map to an id.
 */
public class Skill {
    private int id;
    private String name;

    public Skill() {}

    public Skill(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
