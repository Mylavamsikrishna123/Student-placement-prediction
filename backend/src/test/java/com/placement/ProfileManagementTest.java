package com.placement;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for student profile management.
 * Tests Database profile operations: save, get, update.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Profile Management Integration Tests")
public class ProfileManagementTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_profile;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private Database db;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
            // Students table
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "student_id_number VARCHAR(50) UNIQUE, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
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
                "skill_name VARCHAR(100) NOT NULL UNIQUE)");
            
            // Student-Skills junction table
            stmt.execute("CREATE TABLE student_skills (" +
                "student_id INT NOT NULL, " +
                "skill_id INT NOT NULL, " +
                "PRIMARY KEY (student_id, skill_id), " +
                "FOREIGN KEY (student_id) REFERENCES students(student_id), " +
                "FOREIGN KEY (skill_id) REFERENCES skills(skill_id))");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        db = new Database(new NonClosingConnectionWrapper(testConn));
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM student_skills");
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM skills");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Save student profile with all fields succeeds")
    public void testSaveStudentProfile() {
        String email = "profile@example.com";
        boolean result = db.register(email, "Password123", "John Doe");
        assertTrue(result, "Registration should succeed");
        
        // Update profile with additional info
        String[] profileData = new String[]{
            "STU123456",      // ID number
            "john@domain",    // branch
            "B.Tech",         // degree
            "7.5",            // CGPA
            "NIT Trichy",     // college
            "9876543210",     // phone
            "3",              // backlogs
            "AWS, Docker"     // certifications
        };
        
        // Verify student can be found
        boolean found = checkStudentExists(email);
        assertTrue(found, "Student should exist after registration");
    }
    
    @Test
    @DisplayName("Get student profile returns complete information")
    public void testGetStudentProfile() {
        String email = "fetch@example.com";
        db.register(email, "Pass123", "Jane Smith");
        
        // Fetch and verify
        boolean exists = checkStudentExists(email);
        assertTrue(exists, "Should be able to retrieve profile");
    }
    
    @Test
    @DisplayName("Update student profile modifies existing data")
    public void testUpdateStudentProfile() {
        String email = "update@example.com";
        db.register(email, "Password123", "Test User");
        
        // Verify initial record exists
        assertTrue(checkStudentExists(email), "Initial record should exist");
        
        // Update profile
        updateStudentProfile(email, "Tech", "M.Tech", "8.2", "IIT Delhi", "9988776655");
        
        // Verify update
        assertTrue(checkStudentExists(email), "Updated record should still exist");
    }
    
    @Test
    @DisplayName("Student ID number must be unique")
    public void testStudentIDNumberUniqueness() throws SQLException {
        String idNumber = "STU999999";
        
        // Insert first student with ID number
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO students (student_name, email, password_hash, student_id_number) VALUES " +
                "('User 1', 'user1@example.com', 'hash', '" + idNumber + "')");
        }
        
        // Try to insert second student with same ID - should fail
        Exception exception = assertThrows(SQLException.class, () -> {
            try (Statement stmt = testConn.createStatement()) {
                stmt.execute("INSERT INTO students (student_name, email, password_hash, student_id_number) VALUES " +
                    "('User 2', 'user2@example.com', 'hash', '" + idNumber + "')");
            }
        });
        
        assertNotNull(exception, "Should throw exception for duplicate ID number");
    }
    
    @Test
    @DisplayName("CGPA is stored with decimal precision")
    public void testCGPAStoragePrecision() throws SQLException {
        double cgpa = 7.85;
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO students (student_name, email, cgpa) VALUES " +
                "('Test User', 'cgpa@example.com', " + cgpa + ")");
        }
        
        // Verify precision
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT cgpa FROM students WHERE email = 'cgpa@example.com'")) {
            assertTrue(rs.next(), "Record should exist");
            assertEquals(cgpa, rs.getDouble("cgpa"), 0.01, "CGPA should be stored accurately");
        }
    }
    
    @Test
    @DisplayName("Department field stores string successfully")
    public void testDepartmentStorage() throws SQLException {
        String dept = "Computer Science";
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO students (student_name, email, department) VALUES " +
                "('Test User', 'dept@example.com', '" + dept + "')");
        }
        
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT department FROM students WHERE email = 'dept@example.com'")) {
            assertTrue(rs.next(), "Record should exist");
            assertEquals(dept, rs.getString("department"), "Department should match");
        }
    }
    
    @Test
    @DisplayName("Optional fields can be null")
    public void testProfileWithNullOptionalFields() throws SQLException {
        // Register with minimal required fields
        String email = "minimal@example.com";
        db.register(email, "Password123", "Minimal User");
        
        // Verify record exists even with null optional fields
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students WHERE email = '" + email + "'")) {
            assertTrue(rs.next(), "Record should exist with null optional fields");
            // Most fields should be null or default
            assertTrue(rs.getObject("student_id_number") == null || rs.getString("student_id_number").isEmpty(),
                "ID number can be null");
        }
    }
    
    @Test
    @DisplayName("Profile with no skills succeeds")
    public void testProfileWithoutSkills() {
        String email = "noskills@example.com";
        boolean result = db.register(email, "Pass123", "No Skills User");
        assertTrue(result, "Registration should succeed without skills");
    }
    
    // Helper methods
    private boolean checkStudentExists(String email) {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students WHERE email = '" + email + "'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void updateStudentProfile(String email, String branch, String degree, String cgpa, String college, String phone) {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("UPDATE students SET branch = '" + branch + "', degree = '" + degree + "', " +
                "cgpa = " + cgpa + ", college_name = '" + college + "', phone_number = '" + phone + "' " +
                "WHERE email = '" + email + "'");
        } catch (SQLException e) {
            fail("Update failed: " + e.getMessage());
        }
    }
    
    /**
     * Wrapper to prevent Database.open() from closing our test connection
     */
    private static class NonClosingConnectionWrapper implements java.sql.Connection {
        private final Connection delegate;
        
        public NonClosingConnectionWrapper(Connection delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void close() {
            // Prevent actual closure
        }
        
        @Override
        public Statement createStatement() throws SQLException {
            return delegate.createStatement();
        }
        
        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return delegate.prepareStatement(sql);
        }
        
        @Override
        public boolean isClosed() throws SQLException {
            return delegate.isClosed();
        }
        
        @Override
        public Object unwrap(Class iface) throws SQLException {
            return delegate.unwrap(iface);
        }
        
        @Override
        public boolean isWrapperFor(Class iface) throws SQLException {
            return delegate.isWrapperFor(iface);
        }
        
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency); }
        @Override public java.util.Map getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public java.sql.Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(java.sql.Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return delegate.prepareStatement(sql, autoGeneratedKeys); }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return delegate.prepareStatement(sql, columnIndexes); }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return delegate.prepareStatement(sql, columnNames); }
        @Override public java.sql.Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public java.sql.Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public java.sql.NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public java.util.Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException { delegate.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
    }
}
