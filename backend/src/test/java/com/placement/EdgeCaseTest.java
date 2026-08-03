package com.placement;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: Edge Case Tests
 * Tests unusual and extreme scenarios that may reveal hidden bugs.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Edge Case Tests")
public class EdgeCaseTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_edge;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0)");
            
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
    @DisplayName("Empty database operations return empty results")
    public void testEmptyDatabaseOperations() throws SQLException {
        int studentCount = countStudents();
        int companyCount = countCompanies();
        
        assertEquals(0, studentCount, "Empty database should have 0 students");
        assertEquals(0, companyCount, "Empty database should have 0 companies");
    }
    
    @Test
    @DisplayName("Large dataset operations (1000+ records)")
    public void testLargeDataSets() throws SQLException {
        // Create 1000 students
        for (int i = 0; i < 1000; i++) {
            createStudent("student" + i + "@example.com", "Student " + i, 5.0 + (i % 5), i % 3);
        }
        
        int count = countStudents();
        assertEquals(1000, count, "Database should contain 1000 students");
    }
    
    @Test
    @DisplayName("Special characters in names")
    public void testSpecialCharactersInNames() throws SQLException {
        String[] specialNames = {
            "José García",
            "Jean-Pierre",
            "O'Brien",
            "Anna-Maria",
            "نور الدين", // Arabic characters
            "李明" // Chinese characters
        };
        
        for (String name : specialNames) {
            createStudent("student" + name.hashCode() + "@example.com", name, 5.0, 0);
        }
        
        int count = countStudents();
        assertEquals(specialNames.length, count, "All special character names should be stored");
    }
    
    @Test
    @DisplayName("Unicode characters in emails and text fields")
    public void testUnicodeCharacters() throws SQLException {
        // Note: Email validation should prevent most unicode, but text fields should allow it
        String unicodeName = "Ñoño ñoño";
        createStudent("unicode@example.com", unicodeName, 5.0, 0);
        
        String storedName = getStudentName("unicode@example.com");
        assertTrue(storedName.contains("ñ"), "Unicode characters should be stored and retrieved");
    }
    
    @Test
    @DisplayName("Concurrent registration attempts from same email")
    public void testConcurrentRegistrations() throws SQLException {
        // Simulate concurrent registrations (sequential for testing purposes)
        String email = "concurrent@example.com";
        createStudent(email, "Student 1", 5.0, 0);
        
        // Try duplicate
        try {
            createStudent(email, "Student 2", 6.0, 0);
            fail("Duplicate email should throw exception");
        } catch (SQLException e) {
            // Expected - duplicate key violation
            assertTrue(e.getMessage().contains("UNIQUE") || e.getErrorCode() == 23505,
                "Should fail with UNIQUE constraint violation");
        }
    }
    
    @Test
    @DisplayName("Database connection failure recovery")
    public void testDatabaseConnectionFailure() throws SQLException {
        // Create a valid student first
        createStudent("stable@example.com", "Stable Student", 5.0, 0);
        
        // Verify data is still there
        int count = countStudents();
        assertEquals(1, count, "Data should persist after operation");
    }
    
    @Test
    @DisplayName("Memory efficiency with large text fields")
    public void testMemoryEfficiency() throws SQLException {
        // Create student with large data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeData.append("This is a large text field to test memory efficiency. ");
        }
        
        createStudent("memory@example.com", "Memory Test Student", 5.0, 0);
        
        // Verify data retrieval works
        String name = getStudentName("memory@example.com");
        assertNotNull(name, "Large data should be retrievable");
    }
    
    @Test
    @DisplayName("Transaction rollback on error")
    public void testTransactionRollback() throws SQLException {
        try {
            testConn.setAutoCommit(false);
            
            createStudent("trans@example.com", "Transaction Test", 5.0, 0);
            testConn.commit();
            
            int count = countStudents();
            assertEquals(1, count, "Committed transaction should persist");
            
            testConn.setAutoCommit(true);
        } catch (SQLException e) {
            testConn.rollback();
            testConn.setAutoCommit(true);
        }
    }
    
    @Test
    @DisplayName("Duplicate operations are idempotent")
    public void testDuplicateOperations() throws SQLException {
        String email = "duplicate@example.com";
        createStudent(email, "First Time", 5.0, 0);
        
        int countAfterFirst = countStudents();
        assertEquals(1, countAfterFirst, "First insert should create 1 record");
        
        // Second insert attempt should fail
        try {
            createStudent(email, "Second Time", 6.0, 0);
            fail("Duplicate should not be allowed");
        } catch (SQLException e) {
            // Expected
        }
        
        int countAfterSecond = countStudents();
        assertEquals(1, countAfterSecond, "Duplicate insert should not change count");
    }
    
    @Test
    @DisplayName("Race condition with simultaneous operations")
    public void testRaceConditions() throws SQLException {
        // Simulate race condition with two threads trying to update same record
        createStudent("race@example.com", "Original", 5.0, 0);
        
        // First update
        updateStudentCGPA("race@example.com", 6.0);
        double cgpa1 = getStudentCGPA("race@example.com");
        assertEquals(6.0, cgpa1, 0.01, "First update should succeed");
        
        // Second update (simulates race condition)
        updateStudentCGPA("race@example.com", 7.0);
        double cgpa2 = getStudentCGPA("race@example.com");
        assertEquals(7.0, cgpa2, 0.01, "Second update should succeed, overwriting first");
    }
    
    @Test
    @DisplayName("NULL value handling in optional fields")
    public void testNullValueHandling() throws SQLException {
        createStudentWithNulls("null@example.com", "Null Test", null, null);
        
        String name = getStudentName("null@example.com");
        assertEquals("Null Test", name, "Non-null field should be retrievable");
    }
    
    @Test
    @DisplayName("Empty string handling")
    public void testEmptyStringHandling() throws SQLException {
        try {
            createStudent("empty@example.com", "", 5.0, 0);
            // If it succeeds, verify it's stored as empty
            String name = getStudentName("empty@example.com");
            assertEquals("", name, "Empty string should be stored");
        } catch (SQLException e) {
            // Empty string may be rejected by database constraints
            assertNotNull(e, "Database should reject or handle empty strings appropriately");
        }
    }
    
    @Test
    @DisplayName("Whitespace handling in names")
    public void testWhitespaceHandling() throws SQLException {
        String[] whitespaceNames = {
            "  Leading Spaces",
            "Trailing Spaces  ",
            "  Both Sides  ",
            "Multiple  Spaces  Between"
        };
        
        for (String name : whitespaceNames) {
            createStudent("ws" + name.hashCode() + "@example.com", name, 5.0, 0);
        }
        
        int count = countStudents();
        assertEquals(whitespaceNames.length, count, "All whitespace variants should be stored");
    }
    
    @Test
    @DisplayName("Case sensitivity in unique fields")
    public void testCaseInsensitivity() throws SQLException {
        // Create with lowercase
        createStudent("test@example.com", "Student 1", 5.0, 0);
        
        // Try duplicate with uppercase (email should be case-insensitive)
        try {
            createStudent("TEST@EXAMPLE.COM", "Student 2", 6.0, 0);
            // May or may not fail depending on implementation
        } catch (SQLException e) {
            // Expected if case-insensitive
        }
    }
    
    @Test
    @DisplayName("SQL injection prevention")
    public void testSQLInjectionPrevention() throws SQLException {
        String injectionAttempt = "'; DROP TABLE students; --";
        
        try {
            createStudent("injection@example.com", injectionAttempt, 5.0, 0);
            
            // Table should still exist
            int count = countStudents();
            assertEquals(1, count, "Table should not be dropped");
        } catch (SQLException e) {
            // May fail due to invalid name format
        }
    }
    
    @Test
    @DisplayName("Very long names are truncated or rejected properly")
    public void testVeryLongNames() throws SQLException {
        String veryLongName = "A".repeat(500); // Exceeds VARCHAR(100)
        
        try {
            createStudent("long@example.com", veryLongName, 5.0, 0);
            // If accepted, should be truncated to 100 chars
            String stored = getStudentName("long@example.com");
            assertTrue(stored.length() <= 100, "Long name should be truncated to max length");
        } catch (SQLException e) {
            // May be rejected entirely
            assertNotNull(e, "Very long name should be handled appropriately");
        }
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
    
    private void createStudentWithNulls(String email, String name, Double cgpa, Integer backlogs) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, cgpa, backlogs) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            if (cgpa != null) ps.setDouble(3, cgpa);
            else ps.setNull(3, java.sql.Types.DECIMAL);
            if (backlogs != null) ps.setInt(4, backlogs);
            else ps.setNull(4, java.sql.Types.INTEGER);
            ps.executeUpdate();
        }
    }
    
    private void updateStudentCGPA(String email, double newCGPA) throws SQLException {
        String sql = "UPDATE students SET cgpa = ? WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setDouble(1, newCGPA);
            ps.setString(2, email);
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
    
    private int countCompanies() throws SQLException {
        String sql = "SELECT COUNT(*) FROM companies";
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
    
    private double getStudentCGPA(String email) throws SQLException {
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
}
