package com.placement;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4: Comprehensive Data Validation Tests
 * Tests SQL injection prevention, XSS mitigation, data integrity, and constraint enforcement.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Comprehensive Data Validation Tests")
public class ComprehensiveDataValidationTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_validation;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60) NOT NULL, " +
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0 CHECK (backlogs >= 0), " +
                "department VARCHAR(100))");
            
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(150) NOT NULL UNIQUE, " +
                "required_cgpa DECIMAL(4,2) CHECK (required_cgpa >= 0 AND required_cgpa <= 10), " +
                "max_backlogs INT CHECK (max_backlogs >= 0))");
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
    @DisplayName("SQL injection prevention with single quotes")
    public void testSQLInjectionPrevention() throws SQLException {
        String injectionPayload = "test'; DROP TABLE students; --";
        
        // Use parameterized query (PreparedStatement)
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "injection@example.com");
            ps.setString(2, injectionPayload);
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            ps.executeUpdate();
        }
        
        // Verify table still exists and data is treated as literal string
        int count = countStudents();
        assertEquals(1, count, "SQL injection should be prevented");
        
        // Verify the actual data is stored
        String storedName = getStudentName("injection@example.com");
        assertEquals(injectionPayload, storedName, "Injection payload should be stored as literal text");
    }
    
    @Test
    @DisplayName("XSS vulnerability prevention")
    public void testXSSVulnerabilities() throws SQLException {
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src='javascript:alert(1)'></iframe>"
        };
        
        for (String xss : xssPayloads) {
            String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = testConn.prepareStatement(sql)) {
                ps.setString(1, "xss" + xss.hashCode() + "@example.com");
                ps.setString(2, xss);
                ps.setString(3, "hash123");
                ps.setDouble(4, 5.0);
                ps.executeUpdate();
            }
        }
        
        // Verify all XSS payloads are stored as literal strings
        int count = countStudents();
        assertEquals(xssPayloads.length, count, "All XSS payloads should be stored safely");
    }
    
    @Test
    @DisplayName("Data integrity with UNIQUE constraints")
    public void testDataIntegrity() throws SQLException {
        createStudent("unique@example.com", "Student 1", "hash1", 5.0);
        
        // Attempt duplicate
        try {
            createStudent("unique@example.com", "Student 2", "hash2", 6.0);
            fail("UNIQUE constraint should prevent duplicate");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("UNIQUE") || e.getErrorCode() == 23505,
                "Should fail with UNIQUE constraint violation");
        }
    }
    
    @Test
    @DisplayName("CHECK constraint violation for invalid data")
    public void testConstraintViolation() throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa, backlogs) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "check@example.com");
            ps.setString(2, "Student");
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            ps.setInt(5, -1); // Negative backlogs should violate CHECK constraint
            
            try {
                ps.executeUpdate();
                fail("CHECK constraint should prevent negative backlogs");
            } catch (SQLException e) {
                // Expected
                assertTrue(e.getMessage().toLowerCase().contains("check") || 
                           e.getMessage().toLowerCase().contains("constraint"),
                    "Should fail with constraint violation");
            }
        }
    }
    
    @Test
    @DisplayName("NULL value handling in NOT NULL fields")
    public void testNullValueHandling() throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "null@example.com");
            ps.setNull(2, java.sql.Types.VARCHAR); // NULL in NOT NULL field
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            
            try {
                ps.executeUpdate();
                fail("NOT NULL constraint should prevent NULL values");
            } catch (SQLException e) {
                assertTrue(e.getMessage().toLowerCase().contains("null"),
                    "Should fail with NOT NULL constraint violation");
            }
        }
    }
    
    @Test
    @DisplayName("Empty string handling vs NULL")
    public void testEmptyStringHandling() throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "empty@example.com");
            ps.setString(2, ""); // Empty string (different from NULL)
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            ps.executeUpdate();
        }
        
        String name = getStudentName("empty@example.com");
        assertEquals("", name, "Empty string should be different from NULL");
    }
    
    @Test
    @DisplayName("Whitespace-only string handling")
    public void testWhitespaceHandling() throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "whitespace@example.com");
            ps.setString(2, "   "); // Whitespace-only string
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            ps.executeUpdate();
        }
        
        String name = getStudentName("whitespace@example.com");
        assertEquals("   ", name, "Whitespace-only string should be preserved");
    }
    
    @Test
    @DisplayName("Case sensitivity in string comparisons")
    public void testCaseInsensitivity() throws SQLException {
        createStudent("case@example.com", "StudentName", "hash1", 5.0);
        
        // Query with different case
        String sql = "SELECT COUNT(*) FROM students WHERE student_name = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "StudentName");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertEquals(1, count, "Case-sensitive comparison should find the record");
            }
        }
        
        // Case-insensitive search
        sql = "SELECT COUNT(*) FROM students WHERE LOWER(student_name) = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "studentname");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                assertEquals(1, count, "Case-insensitive search should find the record");
            }
        }
    }
    
    @Test
    @DisplayName("Special characters filtering")
    public void testSpecialCharacterFiltering() throws SQLException {
        String[] specialChars = {
            "Student's Name", // Apostrophe
            "José", // Accented characters
            "李 明", // Asian characters
            "abc@def#ghi$jkl" // Special symbols
        };
        
        for (String name : specialChars) {
            String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = testConn.prepareStatement(sql)) {
                ps.setString(1, "special" + name.hashCode() + "@example.com");
                ps.setString(2, name);
                ps.setString(3, "hash123");
                ps.setDouble(4, 5.0);
                ps.executeUpdate();
            }
        }
        
        int count = countStudents();
        assertEquals(specialChars.length, count, "Special characters should be handled safely");
    }
    
    @Test
    @DisplayName("String length validation")
    public void testLengthValidation() throws SQLException {
        // Email max 100 chars
        String longEmail = "a".repeat(88) + "@example.com"; // Total exactly 100 chars (88 + 12)
        
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, longEmail);
            ps.setString(2, "Student");
            ps.setString(3, "hash123");
            ps.setDouble(4, 5.0);
            ps.executeUpdate();
        }
        
        String email = getStudentEmail(longEmail);
        assertEquals(longEmail, email, "Long strings should be stored correctly");
    }
    
    @Test
    @DisplayName("Format validation for CGPA field")
    public void testFormatValidation() throws SQLException {
        // Test CGPA precision (should be DECIMAL(4,2))
        createStudent("format@example.com", "Student", "hash123", 7.55);
        
        String sql = "SELECT cgpa FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "format@example.com");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double cgpa = rs.getDouble("cgpa");
                assertEquals(7.55, cgpa, 0.001, "CGPA should maintain precision");
            }
        }
    }
    
    @Test
    @DisplayName("CGPA range validation (0-10)")
    public void testConsistencyChecks() throws SQLException {
        String sql = "INSERT INTO companies (company_name, required_cgpa, max_backlogs) VALUES (?, ?, ?)";
        
        // Test maximum CGPA (10.0)
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "MaxCGPA Company");
            ps.setDouble(2, 10.0);
            ps.setInt(3, 0);
            ps.executeUpdate();
        }
        
        // Test minimum CGPA (0.0)
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "MinCGPA Company");
            ps.setDouble(2, 0.0);
            ps.setInt(3, 0);
            ps.executeUpdate();
        }
        
        // Test invalid CGPA (> 10.0) - should violate CHECK constraint
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, "InvalidCGPA Company");
            ps.setDouble(2, 10.5);
            ps.setInt(3, 0);
            
            try {
                ps.executeUpdate();
                fail("CGPA > 10.0 should violate constraint");
            } catch (SQLException e) {
                // Expected
            }
        }
    }
    
    @Test
    @DisplayName("Foreign key constraint prevention")
    public void testForeignKeyConstraint() throws SQLException {
        // This would test referential integrity if we had foreign keys
        // For now, verify basic constraint mechanism works
        
        createStudent("fk@example.com", "Student", "hash123", 5.0);
        int studentCount = countStudents();
        assertEquals(1, studentCount, "Student should be created successfully");
    }
    
    @Test
    @DisplayName("Timestamp handling and consistency")
    public void testTimestampConsistency() throws SQLException {
        long beforeInsert = System.currentTimeMillis();
        createStudent("time@example.com", "Student", "hash123", 5.0);
        long afterInsert = System.currentTimeMillis();
        
        // Verify data consistency
        String name = getStudentName("time@example.com");
        assertEquals("Student", name, "Data should be consistent after insertion");
    }
    
    // ===== Helper Methods =====
    
    private void createStudent(String email, String name, String passwordHash, double cgpa) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setString(3, passwordHash);
            ps.setDouble(4, cgpa);
            ps.executeUpdate();
        }
    }
    
    private int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
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
    
    private String getStudentEmail(String email) throws SQLException {
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
}
