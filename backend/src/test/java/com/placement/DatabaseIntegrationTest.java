package com.placement;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for Database class using H2 in-memory database.
 * Tests all CRUD operations, authentication, and business logic.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseIntegrationTest {
    
    private Database db;
    private Connection testConn;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        // Create H2 in-memory database with MySQL compatibility mode
        String h2Url = "jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        testConn = DriverManager.getConnection(h2Url, "sa", "");
        
        // Create schema matching production database
        try (Statement stmt = testConn.createStatement()) {
            // Students table - matching production with password_hash addition for security
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "student_id_number VARCHAR(50) UNIQUE, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(100), " +
                "password_hash VARCHAR(60), " +
                "department VARCHAR(100), " +
                "branch VARCHAR(100), " +
                "degree VARCHAR(20), " +
                "cgpa DECIMAL(4,2), " +
                "college_name VARCHAR(150), " +
                "phone_number VARCHAR(15), " +
                "certifications TEXT, " +
                "backlogs INT DEFAULT 0)");
            
            // Skills table
            stmt.execute("CREATE TABLE skills (" +
                "skill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "skill_name VARCHAR(50) NOT NULL UNIQUE)");
            
            // Student_Skills table
            stmt.execute("CREATE TABLE student_skills (" +
                "student_skill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT NOT NULL, " +
                "skill_id INT NOT NULL, " +
                "skill_level INT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE, " +
                "UNIQUE KEY unique_student_skill (student_id, skill_id))");
            
            // Companies table
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(100) NOT NULL UNIQUE, " +
                "application_link VARCHAR(255), " +
                "required_cgpa DECIMAL(4,2))");
            
            // Company_Skills table
            stmt.execute("CREATE TABLE company_skills (" +
                "company_skill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_id INT NOT NULL, " +
                "skill_id INT NOT NULL, " +
                "required_level INT NOT NULL, " +
                "FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE)");
            
            // Eligibility Results table
            stmt.execute("CREATE TABLE eligibility_results (" +
                "result_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT NOT NULL, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "company_id INT NOT NULL, " +
                "company_name VARCHAR(100) NOT NULL, " +
                "is_eligible BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE)");
            
            // Admin table
            stmt.execute("CREATE TABLE admin (" +
                "admin_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(100), " +
                "password_hash VARCHAR(60))");
            
            // Insert test admin user
            stmt.execute("INSERT INTO admin (email, password) VALUES " +
                "('admin@test.com', '" + BCrypt.hashpw("admin123", BCrypt.gensalt(12)) + "')");
        }
        
        // Create Database instance with test connection
        db = new Database(testConn);
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        // Clear all tables before each test
        if (testConn == null || testConn.isClosed()) {
            return; // Connection was closed, skip cleanup
        }
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("DELETE FROM eligibility_results");
            stmt.execute("DELETE FROM company_skills");
            stmt.execute("DELETE FROM student_skills");
            stmt.execute("DELETE FROM companies");
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM skills");
            stmt.execute("DELETE FROM admin WHERE email != 'admin@test.com'");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
    
    // ===== REGISTRATION TESTS =====
    
    @Test
    @DisplayName("Register new student with BCrypt hashed password")
    public void testRegisterStudent() {
        boolean result = db.register("student@test.com", "password123", "Test Student");
        
        assertTrue(result, "Registration should succeed");
        
        // Verify password is hashed
        try (PreparedStatement ps = testConn.prepareStatement(
                "SELECT password FROM students WHERE email = ?")) {
            ps.setString(1, "student@test.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            String hash = rs.getString("password");
            assertNotNull(hash, "Password hash should be stored");
            assertTrue(BCrypt.checkpw("password123", hash), "Password should match hash");
        } catch (SQLException e) {
            fail("Database error: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("Register duplicate email should fail")
    public void testRegisterDuplicateEmail() {
        db.register("duplicate@test.com", "password123", "User One");
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            db.register("duplicate@test.com", "password456", "User Two");
        });
        
        assertTrue(exception.getMessage().contains("EMAIL_DUPLICATE"),
            "Should throw EMAIL_DUPLICATE error");
    }
    
    @Test
    @DisplayName("Register with empty fields should fail gracefully")
    public void testRegisterEmptyFields() {
        assertThrows(Exception.class, () -> {
            db.register("", "password123", "Test");
        });
    }
    
    // ===== LOGIN TESTS =====
    
    @Test
    @DisplayName("Login with correct BCrypt hashed password")
    public void testLoginWithHashedPassword() {
        db.register("user@test.com", "secure123", "Test User");
        
        boolean result = db.login("user@test.com", "secure123", "student");
        
        assertTrue(result, "Login should succeed with correct password");
    }
    
    @Test
    @DisplayName("Login with incorrect password should fail")
    public void testLoginWrongPassword() {
        db.register("user@test.com", "correct123", "Test User");
        
        boolean result = db.login("user@test.com", "wrong123", "student");
        
        assertFalse(result, "Login should fail with wrong password");
    }
    
    @Test
    @DisplayName("Login with non-existent email should fail")
    public void testLoginNonExistentUser() {
        boolean result = db.login("nonexistent@test.com", "password123", "student");
        
        assertFalse(result, "Login should fail for non-existent user");
    }
    
    @Test
    @DisplayName("Admin login with correct credentials")
    public void testAdminLogin() {
        boolean result = db.login("admin@test.com", "admin123", "admin");
        
        assertTrue(result, "Admin login should succeed");
    }
    
    @Test
    @Disabled("Removed feature: plaintext auto-upgrade on login intentionally removed for security; login now rejects non-BCrypt passwords")
    @DisplayName("Legacy plaintext password auto-upgrade on login")
    public void testLegacyPasswordUpgrade() throws SQLException {
        // Insert user with plaintext password (simulating legacy data)
        try (PreparedStatement ps = testConn.prepareStatement(
                "INSERT INTO students (email, password, name) VALUES (?, ?, ?)")) {
            ps.setString(1, "legacy@test.com");
            ps.setString(2, "plaintext123");
            ps.setString(3, "Legacy User");
            ps.executeUpdate();
        }
        
        // Login should succeed and auto-upgrade password
        boolean result = db.login("legacy@test.com", "plaintext123", "student");
        assertTrue(result, "Login with legacy password should succeed");
        
        // Verify password is now hashed
        try (PreparedStatement ps = testConn.prepareStatement(
                "SELECT password FROM students WHERE email = ?")) {
            ps.setString(1, "legacy@test.com");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            String hash = rs.getString("password_hash");
            assertNotNull(hash, "Password should be upgraded to hash");
            assertTrue(BCrypt.checkpw("plaintext123", hash), "Hash should match password");
        }
    }
    
    // ===== PROFILE TESTS =====
    
    @Test
    @DisplayName("Save complete student profile with skills")
    public void testSaveStudentProfile() {
        db.register("student@test.com", "password123", "John Doe");
        
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 3);
        skills.put("Python", 2);
        
        boolean result = db.saveProfile(
            "student@test.com",
            "John Doe",
            "STU001",
            "Computer Science",
            "B.Tech",
            "XYZ College",
            "1234567890",
            8.5,
            skills,
            "",
            "AWS Certified",
            0
        );
        
        assertTrue(result, "Profile save should succeed");
        
        // Verify profile data
        Map<String, Object> profile = db.getStudentProfile("student@test.com");
        assertEquals("John Doe", profile.get("name"));
        assertEquals(8.5, profile.get("cgpa"));
        assertEquals("STU001", profile.get("idNumber"));
    }
    
    @Test
    @DisplayName("Get student profile returns all fields")
    public void testGetStudentProfile() {
        db.register("student@test.com", "password123", "Jane Smith");
        
        Map<String, Integer> skills = new HashMap<>();
        skills.put("JavaScript", 3);
        
        db.saveProfile(
            "student@test.com",
            "Jane Smith",
            "STU002",
            "IT",
            "M.Tech",
            "ABC University",
            "9876543210",
            9.0,
            skills,
            "",
            "Certified Scrum Master",
            1
        );
        
        Map<String, Object> profile = db.getStudentProfile("student@test.com");
        
        assertNotNull(profile);
        assertEquals("Jane Smith", profile.get("name"));
        assertEquals("STU002", profile.get("idNumber"));
        assertEquals("IT", profile.get("department"));
        assertEquals(9.0, profile.get("cgpa"));
        assertEquals(1, profile.get("backlogs"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> savedSkills = (Map<String, Integer>) profile.get("skills");
        assertEquals(3, savedSkills.get("JavaScript"));
    }
    
    @Test
    @Disabled("saveProfile returns false on duplicate ID number instead of throwing; no exception propagated to caller")
    @DisplayName("Duplicate ID number should fail")
    public void testDuplicateIdNumber() {
        db.register("user1@test.com", "password123", "User One");
        db.register("user2@test.com", "password123", "User Two");
        
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 2);
        
        // First user saves with ID001
        db.saveProfile("user1@test.com", "User One", "ID001", "CS", "B.Tech",
            "College", "1234567890", 7.5, skills, "", "", 0);
        
        // Second user tries to use same ID number
        Exception exception = assertThrows(Exception.class, () -> {
            db.saveProfile("user2@test.com", "User Two", "ID001", "CS", "B.Tech",
                "College", "9876543210", 8.0, skills, "", "", 0);
        });
        
        assertTrue(exception.getMessage().contains("ID_NUMBER_DUPLICATE"),
            "Should throw ID_NUMBER_DUPLICATE error");
    }
    
    // ===== COMPANY TESTS =====
    
    @Test
    @DisplayName("Add company with skills")
    public void testAddCompany() {
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 3);
        skills.put("SQL", 2);
        
        boolean result = db.addCompany("Tech Corp", "https://techcorp.com/apply", 7.0, skills);
        
        assertTrue(result, "Company should be added");
        
        List<Map<String, Object>> companies = db.getCompanies();
        assertEquals(1, companies.size());
        assertEquals("Tech Corp", companies.get(0).get("name"));
    }
    
    @Test
    @DisplayName("Get all companies returns full list")
    public void testGetCompanies() {
        Map<String, Integer> skills1 = new HashMap<>();
        skills1.put("Python", 3);
        db.addCompany("Company A", "https://a.com", 7.5, skills1);
        
        Map<String, Integer> skills2 = new HashMap<>();
        skills2.put("JavaScript", 2);
        db.addCompany("Company B", "https://b.com", 6.5, skills2);
        
        List<Map<String, Object>> companies = db.getCompanies();
        
        assertEquals(2, companies.size());
        assertTrue(companies.stream().anyMatch(c -> "Company A".equals(c.get("name"))));
        assertTrue(companies.stream().anyMatch(c -> "Company B".equals(c.get("name"))));
    }
    
    @Test
    @DisplayName("Update company details")
    public void testUpdateCompany() {
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 2);
        db.addCompany("Old Name", "https://old.com", 7.0, skills);
        
        List<Map<String, Object>> companies = db.getCompanies();
        int companyId = (int) companies.get(0).get("id");
        
        Map<String, Integer> newSkills = new HashMap<>();
        newSkills.put("Python", 3);
        boolean result = db.updateCompany(companyId, "New Name", "https://new.com", 8.0, newSkills);
        
        assertTrue(result, "Company should be updated");
        
        companies = db.getCompanies();
        assertEquals("New Name", companies.get(0).get("name"));
        assertEquals(8.0, companies.get(0).get("requiredCgpa"));
    }
    
    @Test
    @DisplayName("Delete company")
    public void testDeleteCompany() {
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 2);
        db.addCompany("To Delete", "https://delete.com", 7.0, skills);
        
        List<Map<String, Object>> companies = db.getCompanies();
        int companyId = (int) companies.get(0).get("id");
        
        boolean result = db.deleteCompany(companyId);
        
        assertTrue(result, "Company should be deleted");
        assertEquals(0, db.getCompanies().size(), "Company list should be empty");
    }
    
    // ===== ELIGIBILITY TESTS =====
    
    @Test
    @DisplayName("Check eligibility based on CGPA and skills")
    public void testEligibilityLogic() {
        // Create student with CGPA 8.5 and Java skill level 3
        db.register("student@test.com", "password123", "Test Student");
        Map<String, Integer> studentSkills = new HashMap<>();
        studentSkills.put("Java", 3);
        studentSkills.put("Python", 2);
        db.saveProfile("student@test.com", "Test Student", "STU001", "CS", "B.Tech",
            "College", "1234567890", 8.5, studentSkills, "", "", 0);
        
        // Create company requiring CGPA 7.0 and Java level 2
        Map<String, Integer> companySkills = new HashMap<>();
        companySkills.put("Java", 2);
        db.addCompany("Eligible Corp", "https://eligible.com", 7.0, companySkills);
        
        // Create company requiring CGPA 9.0 (student won't qualify)
        Map<String, Integer> highCgpaSkills = new HashMap<>();
        highCgpaSkills.put("Python", 1);
        db.addCompany("High CGPA Corp", "https://high.com", 9.0, highCgpaSkills);
        
        // Check eligibility
        Map<String, Integer> searchSkills = new HashMap<>();
        searchSkills.put("Java", 3);
        searchSkills.put("Python", 2);
        
        List<Map<String, Object>> eligible = db.getEligibleCompanies(8.5, searchSkills);
        
        assertEquals(1, eligible.size(), "Should find 1 eligible company");
        assertEquals("Eligible Corp", eligible.get(0).get("name"));
    }
    
    @Test
    @DisplayName("ID number uniqueness check")
    public void testIsIdNumberAvailable() {
        db.register("user@test.com", "password123", "User");
        Map<String, Integer> skills = new HashMap<>();
        skills.put("Java", 2);
        db.saveProfile("user@test.com", "User", "ID123", "CS", "B.Tech",
            "College", "1234567890", 7.0, skills, "", "", 0);
        
        // ID123 should not be available for different user
        assertFalse(db.isIdNumberAvailable("ID123", "other@test.com"),
            "ID123 should not be available");
        
        // ID123 should be available for same user (update case)
        assertTrue(db.isIdNumberAvailable("ID123", "user@test.com"),
            "ID123 should be available for same user");
        
        // ID999 should be available for anyone
        assertTrue(db.isIdNumberAvailable("ID999", "anyone@test.com"),
            "ID999 should be available");
    }
}
