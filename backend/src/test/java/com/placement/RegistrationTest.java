package com.placement;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for student registration functionality.
 * Tests Database.register() method with various input scenarios.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Registration Unit Tests")
public class RegistrationTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_reg;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private Database db;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        // Create H2 in-memory database - persistent for all tests in this class
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        // Create students table
        try (Statement stmt = testConn.createStatement()) {
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
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        // Create fresh Database instance for each test
        // Wrap in a non-closing connection to prevent Database.open() from closing our test connection
        db = new Database(new NonClosingConnectionWrapper(testConn));
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        // Clear students table before each test
        try (Statement stmt = testConn.createStatement()) {
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
    @DisplayName("Register new student with valid email and password")
    public void testRegisterValidStudent() {
        boolean result = db.register("student@example.com", "SecurePass123", "John Doe");
        assertTrue(result, "Registration should succeed with valid inputs");
    }
    
    @Test
    @DisplayName("Registered password is hashed with BCrypt")
    public void testPasswordHashedWithBCrypt() throws SQLException {
        String email = "hash@example.com";
        String password = "TestPassword123";
        String name = "Hash Test";
        
        db.register(email, password, name);
        
        // Verify BCrypt hash is stored
        String sql = "SELECT password FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Student should be found");
                String storedHash = rs.getString("password");
                assertNotNull(storedHash, "password (BCrypt hash) should not be null");
                assertTrue(BCrypt.checkpw(password, storedHash), "Password should match BCrypt hash");
            }
        }
    }
    
    @Test
    @DisplayName("Register duplicate email throws EMAIL_DUPLICATE exception")
    public void testRegisterDuplicateEmail() {
        String email = "duplicate@example.com";
        db.register(email, "Password123", "First User");
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            db.register(email, "DifferentPass123", "Second User"));
        
        assertTrue(exception.getMessage().contains("EMAIL_DUPLICATE"), 
            "Should throw EMAIL_DUPLICATE for duplicate email");
    }
    
    @Test
    @DisplayName("Register with null email throws IllegalArgumentException")
    public void testRegisterNullEmail() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register(null, "Password123", "Test User"));
        
        assertTrue(exception.getMessage().contains("Email cannot be empty"),
            "Should throw error for null email");
    }
    
    @Test
    @DisplayName("Register with empty email throws IllegalArgumentException")
    public void testRegisterEmptyEmail() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register("", "Password123", "Test User"));
        
        assertTrue(exception.getMessage().contains("Email cannot be empty"),
            "Should throw error for empty email");
    }
    
    @Test
    @DisplayName("Register with null password throws IllegalArgumentException")
    public void testRegisterNullPassword() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register("valid@example.com", null, "Test User"));
        
        assertTrue(exception.getMessage().contains("Password cannot be empty"),
            "Should throw error for null password");
    }
    
    @Test
    @DisplayName("Register with empty password throws IllegalArgumentException")
    public void testRegisterEmptyPassword() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register("valid@example.com", "", "Test User"));
        
        assertTrue(exception.getMessage().contains("Password cannot be empty"),
            "Should throw error for empty password");
    }
    
    @Test
    @DisplayName("Register with null name throws IllegalArgumentException")
    public void testRegisterNullName() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register("valid@example.com", "Password123", null));
        
        assertTrue(exception.getMessage().contains("Name cannot be empty"),
            "Should throw error for null name");
    }
    
    @Test
    @DisplayName("Register with empty name throws IllegalArgumentException")
    public void testRegisterEmptyName() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            db.register("valid@example.com", "Password123", ""));
        
        assertTrue(exception.getMessage().contains("Name cannot be empty"),
            "Should throw error for empty name");
    }
    
    @Test
    @DisplayName("BCrypt salt is random - different registrations produce different hashes")
    public void testBCryptSaltRandomness() throws SQLException {
        String email1 = "salt1@example.com";
        String email2 = "salt2@example.com";
        String samePassword = "IdenticalPassword123";
        
        db.register(email1, samePassword, "User 1");
        db.register(email2, samePassword, "User 2");
        
        // Retrieve both hashes
        String hash1 = getPasswordHash(email1);
        String hash2 = getPasswordHash(email2);
        
        assertNotNull(hash1, "First hash should exist");
        assertNotNull(hash2, "Second hash should exist");
        assertNotEquals(hash1, hash2, "Different hashes for same password (random salt)");
        assertTrue(BCrypt.checkpw(samePassword, hash1), "Password should match first hash");
        assertTrue(BCrypt.checkpw(samePassword, hash2), "Password should match second hash");
    }
    
    @Test
    @DisplayName("BCrypt hash format is valid ($2a$ or $2b$ prefix with cost factor)")
    public void testBCryptHashFormat() throws SQLException {
        String email = "format@example.com";
        db.register(email, "TestPassword123", "Format Test");
        
        String hash = getPasswordHash(email);
        assertNotNull(hash, "Hash should be stored");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"), 
            "BCrypt hash should start with $2a$ or $2b$");
        assertTrue(hash.length() == 60, "BCrypt hash should be 60 characters");
    }
    
    /**
     * Helper method to retrieve password hash from database
     */
    private String getPasswordHash(String email) throws SQLException {
        String sql = "SELECT password FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        }
        return null;
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

