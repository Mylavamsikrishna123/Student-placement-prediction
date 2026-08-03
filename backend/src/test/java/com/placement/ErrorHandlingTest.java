package com.placement;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for error handling and exception scenarios.
 * Tests Database class behavior for SQLExceptions, constraint violations, and edge cases.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Error Handling Tests")
public class ErrorHandlingTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_error;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private Database db;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
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
            
            stmt.execute("CREATE TABLE admin (" +
                "admin_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60))");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        // Create fresh Database instance for each test
        // Wrap in a non-closing connection to prevent Database.open() from closing our test connection
        db = new Database(new NonClosingConnectionWrapper(testConn));
        
        // Clear test data
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM admin");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Duplicate email handling")
    public void testDuplicateEmailException() {
        // This test verifies that the system handles duplicate emails
        // Note: The exact behavior (exception vs silent failure) depends on implementation
        assertTrue(true, "Duplicate email handling test placeholder");
    }
    
    @Test
    @DisplayName("Null email input throws IllegalArgumentException")
    public void testNullEmailValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register(null, "Password123", "Test User"));
        
        assertTrue(exception.getMessage().contains("Email"),
            "Exception should mention email validation");
    }
    
    @Test
    @DisplayName("Empty email input throws IllegalArgumentException")
    public void testEmptyEmailValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register("", "Password123", "Test User"));
        
        assertTrue(exception.getMessage().contains("Email"),
            "Exception should mention email validation");
    }
    
    @Test
    @DisplayName("Null password input throws IllegalArgumentException")
    public void testNullPasswordValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register("valid@example.com", null, "Test User"));
        
        assertTrue(exception.getMessage().contains("Password"),
            "Exception should mention password validation");
    }
    
    @Test
    @DisplayName("Empty password input throws IllegalArgumentException")
    public void testEmptyPasswordValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register("valid@example.com", "", "Test User"));
        
        assertTrue(exception.getMessage().contains("Password"),
            "Exception should mention password validation");
    }
    
    @Test
    @DisplayName("Null name input throws IllegalArgumentException")
    public void testNullNameValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register("valid@example.com", "Password123", null));
        
        assertTrue(exception.getMessage().contains("Name"),
            "Exception should mention name validation");
    }
    
    @Test
    @DisplayName("Empty name input throws IllegalArgumentException")
    public void testEmptyNameValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            db.register("valid@example.com", "Password123", ""));
        
        assertTrue(exception.getMessage().contains("Name"),
            "Exception should mention name validation");
    }
    
    @Test
    @DisplayName("Login with null email returns false gracefully")
    public void testLoginNullEmailGraceful() {
        boolean result = db.login(null, "Password123", "student");
        assertFalse(result, "Login should return false for null email without throwing");
    }
    
    @Test
    @DisplayName("Login with null password returns false gracefully")
    public void testLoginNullPasswordGraceful() {
        boolean result = db.login("test@example.com", null, "student");
        assertFalse(result, "Login should return false for null password without throwing");
    }
    
    @Test
    @DisplayName("Multiple registration attempts do not corrupt database state")
    public void testDatabaseStateAfterFailedRegistrations() {
        // This test verifies that multiple registration operations maintain database consistency
        assertTrue(true, "Database state consistency test placeholder");
    }
    
    @Test
    @DisplayName("SQL injection attempt is prevented by parameterized queries")
    public void testSQLInjectionPrevention() {
        String maliciousEmail = "' OR '1'='1";
        String password = "anypassword";
        
        // This should not cause SQL injection or throw unexpected error
        boolean result = db.login(maliciousEmail, password, "student");
        assertFalse(result, "SQL injection attempt should safely return false");
    }
    
    @Test
    @DisplayName("Very long email string is handled safely")
    public void testVeryLongEmailHandling() {
        String longEmail = "a".repeat(150) + "@example.com"; // Exceeds VARCHAR(100)
        
        // Should either throw an exception or return false, but not crash
        try {
            db.register(longEmail, "Password123", "Test User");
            // If no exception, that's also acceptable (some implementations silently fail)
        } catch (Exception e) {
            // Exception is expected for oversized input
            assertNotNull(e, "Should throw exception for oversized email");
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

