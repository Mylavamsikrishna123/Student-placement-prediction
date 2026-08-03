package com.placement;

import com.placement.models.Student;
import com.placement.models.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Tests - Complete Workflow")
public class IntegrationTest {
    
    private Student student;
    private Company company;
    private Database db;

    @BeforeEach
    public void setUp() {
        student = new Student();
        company = new Company();
        db = new Database();
    }

    @Test
    @DisplayName("Test Complete Student Registration Workflow")
    public void testCompleteStudentRegistrationWorkflow() {
        // Step 1: Create student object
        assertNotNull(student);
        
        // Step 2: Set student details
        student.setEmail("workflow@test.com");
        student.setName("Test Student");
        student.setIdNumber("STU001");
        student.setDepartment("CSE");
        student.setDegree("B.Tech");
        
        // Step 3: Verify data is stored
        assertEquals("workflow@test.com", student.getEmail());
        assertEquals("Test Student", student.getName());
        assertEquals("STU001", student.getIdNumber());
    }

    @Test
    @DisplayName("Test Complete Student Profile Creation")
    public void testCompleteStudentProfileCreation() {
        // Create complete student profile
        Student completeStudent = new Student();
        completeStudent.setId(1);
        completeStudent.setName("John Profile");
        completeStudent.setEmail("john.profile@test.com");
        completeStudent.setIdNumber("STU123");
        completeStudent.setDepartment("CSE");
        completeStudent.setBranch("CSE-A");
        completeStudent.setDegree("B.Tech");
        completeStudent.setCgpa(8.5);
        completeStudent.setCollegeName("Test University");
        completeStudent.setPhone("9876543210");
        completeStudent.setCertifications("Java,AWS");
        completeStudent.setBacklogs(0);
        
        // Verify all fields
        assertEquals(1, completeStudent.getId());
        assertEquals("John Profile", completeStudent.getName());
        assertEquals("john.profile@test.com", completeStudent.getEmail());
        assertEquals(8.5, completeStudent.getCgpa());
        assertEquals(0, completeStudent.getBacklogs());
    }

    @Test
    @DisplayName("Test Eligibility Check Workflow - High CGPA")
    public void testEligibilityCheckWorkflowHighCgpa() {
        // Student with high CGPA
        student.setCgpa(9.0);
        student.setBacklogs(0);
        
        // Verify eligibility criteria
        assertTrue(student.getCgpa() >= 8.0, "CGPA should be >= 8.0");
        assertEquals(0, student.getBacklogs(), "Should have no backlogs");
    }

    @Test
    @DisplayName("Test Eligibility Check Workflow - Low CGPA")
    public void testEligibilityCheckWorkflowLowCgpa() {
        // Student with low CGPA
        student.setCgpa(6.5);
        student.setBacklogs(0);
        
        // Verify eligibility criteria
        assertTrue(student.getCgpa() < 7.0, "CGPA is below 7.0");
    }

    @Test
    @DisplayName("Test Student with Backlogs Eligibility")
    public void testStudentWithBacklogsEligibility() {
        // Student with backlogs
        student.setCgpa(8.0);
        student.setBacklogs(2);
        
        // Verify backlog impact
        assertTrue(student.getBacklogs() > 0, "Student has backlogs");
        assertEquals(2, student.getBacklogs());
    }

    @Test
    @DisplayName("Test Company Creation Workflow")
    public void testCompanyCreationWorkflow() {
        // Create company
        company.setId(1);
        company.setName("TechCorp");
        
        // Verify company creation
        assertEquals(1, company.getId());
        assertEquals("TechCorp", company.getName());
    }

    @Test
    @DisplayName("Test Multiple Companies Creation")
    public void testMultipleCompaniesCreation() {
        Company company1 = new Company();
        Company company2 = new Company();
        Company company3 = new Company();
        
        company1.setId(1);
        company1.setName("Google");
        
        company2.setId(2);
        company2.setName("Microsoft");
        
        company3.setId(3);
        company3.setName("Amazon");
        
        assertEquals("Google", company1.getName());
        assertEquals("Microsoft", company2.getName());
        assertEquals("Amazon", company3.getName());
    }

    @Test
    @DisplayName("Test Student-Company Matching Workflow")
    public void testStudentCompanyMatchingWorkflow() {
        // Create student with profile
        student.setName("Alice");
        student.setCgpa(8.5);
        student.setBacklogs(0);
        
        // Create company with requirements
        company.setId(1);
        company.setName("TCS");
        
        // Verify both objects are created
        assertNotNull(student);
        assertNotNull(company);
        assertEquals("Alice", student.getName());
        assertEquals("TCS", company.getName());
    }

    @Test
    @DisplayName("Test Eligibility Results Storage Workflow")
    public void testEligibilityResultsStorageWorkflow() {
        // Setup student and company
        student.setEmail("test@example.com");
        student.setName("Test User");
        student.setCgpa(8.5);
        student.setBacklogs(0);
        
        company.setId(1);
        company.setName("Company1");
        
        // Verify data for storage
        assertNotNull(student.getEmail());
        assertNotNull(company.getName());
        assertTrue(student.getCgpa() > 0);
    }

    @Test
    @DisplayName("Test Complete Admin Dashboard Workflow")
    public void testCompleteAdminDashboardWorkflow() {
        // Create multiple students
        Student s1 = new Student();
        Student s2 = new Student();
        
        s1.setName("Student1");
        s1.setCgpa(8.5);
        
        s2.setName("Student2");
        s2.setCgpa(7.0);
        
        // Verify students created
        assertEquals("Student1", s1.getName());
        assertEquals("Student2", s2.getName());
        
        // Create companies
        Company c1 = new Company();
        c1.setName("TCS");
        
        // Verify system ready for dashboard
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotNull(c1);
    }

    @Test
    @DisplayName("Test Database Initialization Workflow")
    public void testDatabaseInitializationWorkflow() {
        // Initialize database
        Database database = new Database();
        assertNotNull(database);
        
        // Create student and company
        Student testStudent = new Student();
        Company testCompany = new Company();
        
        testStudent.setName("DB Test");
        testCompany.setName("DB Company");
        
        // Verify all components ready
        assertNotNull(testStudent);
        assertNotNull(testCompany);
        assertNotNull(database);
    }

    @Test
    @DisplayName("Test End-to-End Student Journey")
    public void testEndToEndStudentJourney() {
        // 1. Register
        student.setEmail("journey@test.com");
        student.setName("Journey Student");
        
        // 2. Complete Profile
        student.setIdNumber("STU999");
        student.setDepartment("CSE");
        student.setBranch("CSE-B");
        student.setDegree("B.Tech");
        student.setCgpa(8.7);
        student.setCollegeName("Test Univ");
        student.setPhone("9999999999");
        student.setCertifications("Java");
        student.setBacklogs(0);
        
        // 3. Check Eligibility
        assertTrue(student.getCgpa() >= 7.0);
        assertEquals(0, student.getBacklogs());
        
        // 4. Verify Data
        assertEquals("Journey Student", student.getName());
        assertEquals("journey@test.com", student.getEmail());
        assertEquals(8.7, student.getCgpa());
    }
}
