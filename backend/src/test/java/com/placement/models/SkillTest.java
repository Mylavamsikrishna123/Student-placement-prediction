package com.placement.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Skill Model Tests")
public class SkillTest {
    private Skill skill;

    @BeforeEach
    public void setUp() {
        skill = new Skill();
    }

    @Test
    @DisplayName("Test Default Constructor")
    public void testDefaultConstructor() {
        assertNotNull(skill);
    }

    @Test
    @DisplayName("Test Skill Name Setter and Getter")
    public void testSkillNameSetterGetter() {
        skill.setName("Java");
        assertEquals("Java", skill.getName());
    }

    @Test
    @DisplayName("Test Skill ID Setter and Getter")
    public void testSkillIdSetterGetter() {
        skill.setId(1);
        assertEquals(1, skill.getId());
    }

    @Test
    @DisplayName("Test Java Skill")
    public void testJavaSkill() {
        skill.setId(1);
        skill.setName("Java");
        
        assertEquals(1, skill.getId());
        assertEquals("Java", skill.getName());
    }

    @Test
    @DisplayName("Test Python Skill")
    public void testPythonSkill() {
        skill.setId(2);
        skill.setName("Python");
        
        assertEquals(2, skill.getId());
        assertEquals("Python", skill.getName());
    }

    @Test
    @DisplayName("Test SQL Skill")
    public void testSqlSkill() {
        skill.setId(3);
        skill.setName("SQL");
        
        assertEquals(3, skill.getId());
        assertEquals("SQL", skill.getName());
    }

    @Test
    @DisplayName("Test Skill Name Update")
    public void testSkillNameUpdate() {
        skill.setName("C++");
        assertEquals("C++", skill.getName());
        
        skill.setName("JavaScript");
        assertEquals("JavaScript", skill.getName());
    }

    @Test
    @DisplayName("Test Multiple Skills")
    public void testMultipleSkills() {
        Skill skill1 = new Skill();
        Skill skill2 = new Skill();
        Skill skill3 = new Skill();
        
        skill1.setName("Java");
        skill2.setName("Python");
        skill3.setName("Docker");
        
        assertEquals("Java", skill1.getName());
        assertEquals("Python", skill2.getName());
        assertEquals("Docker", skill3.getName());
    }

    @Test
    @DisplayName("Test Skill ID Positive")
    public void testSkillIdPositive() {
        skill.setId(50);
        assertTrue(skill.getId() > 0);
    }

    @Test
    @DisplayName("Test Skill Name Not Empty")
    public void testSkillNameNotEmpty() {
        skill.setName("AWS");
        assertNotNull(skill.getName());
        assertNotEquals("", skill.getName());
    }

    @Test
    @DisplayName("Test AWS Skill")
    public void testAwsSkill() {
        skill.setId(4);
        skill.setName("AWS");
        assertEquals("AWS", skill.getName());
    }

    @Test
    @DisplayName("Test Docker Skill")
    public void testDockerSkill() {
        skill.setId(5);
        skill.setName("Docker");
        assertEquals("Docker", skill.getName());
    }

    @Test
    @DisplayName("Test Kubernetes Skill")
    public void testKubernetesSkill() {
        skill.setId(6);
        skill.setName("Kubernetes");
        assertEquals("Kubernetes", skill.getName());
    }
}
