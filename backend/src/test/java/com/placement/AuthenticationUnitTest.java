package com.placement;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for login/authentication functionality.
 * Tests Database.login() method with various scenarios.
 * Uses H2 in-memory database with test data pre-populated.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Authentication Unit Tests")
public class AuthenticationUnitTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
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
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "password VARCHAR(100), " +
                "department VARCHAR(100), " +
                "branch VARCHAR(100), " +
                "degree VARCHAR(20), " +
                "cgpa DECIMAL(4,2), " +
                "college_name VARCHAR(150), " +
                "phone_number VARCHAR(15), " +
                "certifications TEXT, " +
                "backlogs INT DEFAULT 0)");
            
            // Admin table
            stmt.execute("CREATE TABLE admin (" +
                "admin_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "password VARCHAR(100))");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        // Create fresh Database instance for each test
        // Wrap in a non-closing connection to prevent Database.open() from closing our test connection
        db = new Database(new NonClosingConnectionWrapper(testConn));
        
        // Clear and repopulate test data
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM admin");
            
            // Insert test student with BCrypt hashed password
            String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt(12));
            stmt.execute("INSERT INTO students (student_name, email, password_hash) VALUES ('Test Student', 'student@test.com', '" + hashedPassword + "')");
            
            // Insert test admin with hashed password
            String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(12));
            stmt.execute("INSERT INTO admin (email, password_hash) VALUES ('admin@test.com', '" + adminHash + "')");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Login succeeds with correct student credentials")
    public void testLoginCorrectPassword() {
        boolean result = db.login("student@test.com", "password123", "student");
        assertTrue(result, "Login should succeed with correct password");
    }
    
    @Test
    @DisplayName("Login fails with wrong student password")
    public void testLoginWrongPassword() {
        boolean result = db.login("student@test.com", "wrongpassword", "student");
        assertFalse(result, "Login should fail with wrong password");
    }
    
    @Test
    @DisplayName("Login fails with non-existent email")
    public void testLoginNonExistentUser() {
        boolean result = db.login("nonexistent@test.com", "password123", "student");
        assertFalse(result, "Login should fail for non-existent user");
    }
    
    @Test
    @DisplayName("Login is case-insensitive for email")
    public void testLoginEmailCaseInsensitive() {
        // Test with uppercase
        boolean result1 = db.login("STUDENT@TEST.COM", "password123", "student");
        assertTrue(result1, "Login should work with uppercase email");
        
        // Test with mixed case
        boolean result2 = db.login("StUdEnT@TeSt.CoM", "password123", "student");
        assertTrue(result2, "Login should work with mixed case email");
    }
    
    @Test
    @DisplayName("Admin login succeeds with correct credentials")
    public void testAdminLoginCorrectPassword() {
        boolean result = db.login("admin@test.com", "admin123", "admin");
        assertTrue(result, "Admin login should succeed with correct credentials");
    }
    
    @Test
    @DisplayName("Admin login fails with wrong password")
    public void testAdminLoginWrongPassword() {
        boolean result = db.login("admin@test.com", "wrongpass", "admin");
        assertFalse(result, "Admin login should fail with wrong password");
    }
    
    @Test
    @DisplayName("Student credentials do not work for admin login")
    public void testStudentCannotLoginAsAdmin() {
        boolean result = db.login("student@test.com", "password123", "admin");
        assertFalse(result, "Student credentials should not work for admin role");
    }
    
    @Test
    @DisplayName("Login with null email returns false")
    public void testLoginNullEmail() {
        boolean result = db.login(null, "password123", "student");
        assertFalse(result, "Login should return false for null email");
    }
    
    @Test
    @DisplayName("Login with empty email returns false")
    public void testLoginEmptyEmail() {
        boolean result = db.login("", "password123", "student");
        assertFalse(result, "Login should return false for empty email");
    }
    
    @Test
    @DisplayName("Login with null password returns false")
    public void testLoginNullPassword() {
        boolean result = db.login("student@test.com", null, "student");
        assertFalse(result, "Login should return false for null password");
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
        
        // Delegated methods (not exhaustive - add as needed)
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

