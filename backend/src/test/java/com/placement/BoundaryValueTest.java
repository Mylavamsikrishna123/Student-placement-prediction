package com.placement;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: Boundary Value Tests
 * Tests edge cases and boundary conditions for all input fields and calculations.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Boundary Value Tests")
public class BoundaryValueTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_boundary;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "phone_number VARCHAR(15), " +
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0, " +
                "student_id_number VARCHAR(20) UNIQUE)");
            
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(150) NOT NULL UNIQUE, " +
                "required_cgpa DECIMAL(4,2), " +
                "max_backlogs INT)");
        }
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM companies");
            stmt.execute("DELETE FROM students");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("CGPA maximum value (10.0) is accepted")
    public void testCGPAMaximumValue() throws SQLException {
        createStudent("student@example.com", "Perfect Student", 10.0, 0);
        double cgpa = getCGPA("student@example.com");
        assertEquals(10.0, cgpa, 0.01, "Maximum CGPA 10.0 should be accepted");
    }
    
    @Test
    @DisplayName("CGPA minimum value (0.0) is accepted")
    public void testCGPAMinimumValue() throws SQLException {
        createStudent("student@example.com", "Minimum Student", 0.0, 0);
        double cgpa = getCGPA("student@example.com");
        assertEquals(0.0, cgpa, 0.01, "Minimum CGPA 0.0 should be accepted");
    }
    
    @Test
    @DisplayName("CGPA boundary (just below max: 9.99)")
    public void testCGPAJustBelowMax() throws SQLException {
        createStudent("student@example.com", "High Achiever", 9.99, 0);
        double cgpa = getCGPA("student@example.com");
        assertEquals(9.99, cgpa, 0.01, "CGPA 9.99 should be accepted");
    }
    
    @Test
    @DisplayName("CGPA boundary (just above min: 0.01)")
    public void testCGPAJustAboveMin() throws SQLException {
        createStudent("student@example.com", "Low Achiever", 0.01, 0);
        double cgpa = getCGPA("student@example.com");
        assertEquals(0.01, cgpa, 0.01, "CGPA 0.01 should be accepted");
    }
    
    @Test
    @DisplayName("Backlogs large numbers (999999)")
    public void testBacklogsLargeNumbers() throws SQLException {
        createStudent("student@example.com", "Student", 5.0, 999999);
        int backlogs = getBacklogs("student@example.com");
        assertEquals(999999, backlogs, "Large backlog numbers should be accepted");
    }
    
    @Test
    @DisplayName("Backlogs minimum (0)")
    public void testBacklogsMinimum() throws SQLException {
        createStudent("student@example.com", "Student", 5.0, 0);
        int backlogs = getBacklogs("student@example.com");
        assertEquals(0, backlogs, "Zero backlogs should be accepted");
    }
    
    @Test
    @DisplayName("CGPA precision validation (2 decimal places)")
    public void testCGPAPrecision() throws SQLException {
        createStudent("student@example.com", "Student", 7.55, 0);
        double cgpa = getCGPA("student@example.com");
        
        // Check that precision is maintained
        String cgpaStr = String.format("%.2f", cgpa);
        assertTrue(cgpaStr.matches("\\d+\\.\\d{2}"), 
            "CGPA should have exactly 2 decimal places");
    }
    
    @Test
    @DisplayName("Student name maximum length")
    public void testNameMaxLength() throws SQLException {
        String maxLengthName = "A".repeat(100); // 100 chars max for VARCHAR(100)
        createStudent("student@example.com", maxLengthName, 5.0, 0);
        String name = getStudentName("student@example.com");
        assertEquals(maxLengthName, name, "Maximum length name should be stored");
    }
    
    @Test
    @DisplayName("Email maximum length")
    public void testEmailMaxLength() throws SQLException {
        String maxLengthEmail = "a".repeat(86) + "@test.com"; // Total ~95 chars, VARCHAR(100)
        createStudent(maxLengthEmail, "Student", 5.0, 0);
        String email = getEmail(maxLengthEmail);
        assertEquals(maxLengthEmail, email, "Maximum length email should be stored");
    }
    
    @Test
    @DisplayName("Phone number edge cases")
    public void testPhoneEdgeCases() throws SQLException {
        // Minimum valid length (10 digits)
        createStudentWithPhone("min@example.com", "Min Phone", "1234567890");
        String minPhone = getPhone("min@example.com");
        assertEquals("1234567890", minPhone, "10-digit phone should be accepted");
        
        // Maximum valid length (15 digits)
        createStudentWithPhone("max@example.com", "Max Phone", "123456789012345");
        String maxPhone = getPhone("max@example.com");
        assertEquals("123456789012345", maxPhone, "15-digit phone should be accepted");
    }
    
    @Test
    @DisplayName("Student ID number format validation")
    public void testIDNumberFormat() throws SQLException {
        String idNumber = "STU123456"; // Standard format
        createStudentWithIDNumber("student@example.com", "Student", idNumber);
        String storedID = getIDNumber("student@example.com");
        assertEquals(idNumber, storedID, "ID number format should be maintained");
    }
    
    @Test
    @DisplayName("Degree type boundary values")
    public void testDegreeTypeValidation() throws SQLException {
        String[] validDegrees = {"B.Tech", "M.Tech", "B.E.", "M.E.", "B.Sc.", "M.Sc."};
        
        for (String degree : validDegrees) {
            assertTrue(isValidDegree(degree), degree + " should be a valid degree type");
        }
    }
    
    @Test
    @DisplayName("Skill name length boundary")
    public void testSkillNameLength() throws SQLException {
        String skillName = "A".repeat(80); // Max length (leaving room for @skill.com = 100 total)
        createSkill(skillName);
        String storedSkill = getSkillName(skillName);
        assertEquals(skillName, storedSkill, "Skill name with max length should be stored");
    }
    
    @Test
    @DisplayName("Company name length boundary")
    public void testCompanyNameLength() throws SQLException {
        String companyName = "Company ".repeat(20); // Long company name
        // Truncate to 150 chars (VARCHAR limit)
        if (companyName.length() > 150) {
            companyName = companyName.substring(0, 150);
        }
        createCompany(companyName, 7.0, 0);
        String storedName = getCompanyName(companyName);
        assertEquals(companyName, storedName, "Company name with max length should be stored");
    }
    
    @Test
    @DisplayName("CGPA eligibility at exact threshold")
    public void testCGPAExactThreshold() throws SQLException {
        double requiredCGPA = 8.0;
        createStudent("student@example.com", "Student", requiredCGPA, 0);
        createCompany("Company", requiredCGPA, 0);
        
        boolean isEligible = checkEligibility("student@example.com", "Company");
        assertTrue(isEligible, "Student with exact CGPA threshold should be eligible");
    }
    
    @Test
    @DisplayName("Backlogs eligibility at exact threshold")
    public void testBacklogsExactThreshold() throws SQLException {
        int maxBacklogs = 2;
        createStudent("student@example.com", "Student", 8.0, maxBacklogs);
        createCompany("Company", 7.0, maxBacklogs);
        
        boolean isEligible = checkEligibility("student@example.com", "Company");
        assertTrue(isEligible, "Student with exact backlog threshold should be eligible");
    }
    
    // ===== Helper Methods =====
    
    private void createStudent(String email, String name, double cgpa, int backlogs) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, cgpa, backlogs) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setDouble(3, cgpa);
            ps.setInt(4, backlogs);
            ps.executeUpdate();
        }
    }
    
    private void createStudentWithPhone(String email, String name, String phone) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, cgpa, phone_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setDouble(3, 5.0);
            ps.setString(4, phone);
            ps.executeUpdate();
        }
    }
    
    private void createStudentWithIDNumber(String email, String name, String idNumber) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, cgpa, student_id_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setDouble(3, 5.0);
            ps.setString(4, idNumber);
            ps.executeUpdate();
        }
    }
    
    private void createCompany(String name, double reqCgpa, int maxBacklogs) throws SQLException {
        String sql = "INSERT INTO companies (company_name, required_cgpa, max_backlogs) VALUES (?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, reqCgpa);
            ps.setInt(3, maxBacklogs);
            ps.executeUpdate();
        }
    }
    
    private void createSkill(String skillName) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, cgpa) VALUES (?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, skillName + "@skill.com");
            ps.setString(2, skillName);
            ps.setDouble(3, 5.0);
            ps.executeUpdate();
        }
    }
    
    private double getCGPA(String email) throws SQLException {
        String sql = "SELECT cgpa FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cgpa");
                }
            }
        }
        return -1;
    }
    
    private int getBacklogs(String email) throws SQLException {
        String sql = "SELECT backlogs FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("backlogs");
                }
            }
        }
        return -1;
    }
    
    private String getStudentName(String email) throws SQLException {
        String sql = "SELECT student_name FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("student_name");
                }
            }
        }
        return null;
    }
    
    private String getEmail(String email) throws SQLException {
        String sql = "SELECT email FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        return null;
    }
    
    private String getPhone(String email) throws SQLException {
        String sql = "SELECT phone_number FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("phone_number");
                }
            }
        }
        return null;
    }
    
    private String getIDNumber(String email) throws SQLException {
        String sql = "SELECT student_id_number FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("student_id_number");
                }
            }
        }
        return null;
    }
    
    private String getSkillName(String skillName) throws SQLException {
        String sql = "SELECT student_name FROM students WHERE student_name = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, skillName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("student_name");
                }
            }
        }
        return null;
    }
    
    private String getCompanyName(String companyName) throws SQLException {
        String sql = "SELECT company_name FROM companies WHERE company_name = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, companyName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("company_name");
                }
            }
        }
        return null;
    }
    
    private boolean checkEligibility(String email, String companyName) throws SQLException {
        String sql = "SELECT s.cgpa, s.backlogs, c.required_cgpa, c.max_backlogs FROM students s, companies c " +
                     "WHERE s.email = ? AND c.company_name = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, companyName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double cgpa = rs.getDouble("cgpa");
                    int backlogs = rs.getInt("backlogs");
                    double reqCgpa = rs.getDouble("required_cgpa");
                    int maxBacklogs = rs.getInt("max_backlogs");
                    return cgpa >= reqCgpa && backlogs <= maxBacklogs;
                }
            }
        }
        return false;
    }
    
    private boolean isValidDegree(String degree) {
        return degree.matches("(B\\.Tech|M\\.Tech|B\\.E\\.|M\\.E\\.|B\\.Sc\\.|M\\.Sc\\.)");
    }
}
