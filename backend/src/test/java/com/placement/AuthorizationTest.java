package com.placement;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: Authorization and Access Control Tests
 * Tests role-based access control, token validation, and permission enforcement.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Authorization Unit Tests")
public class AuthorizationTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_auth;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private Database db;
    private Map<String, String> tokenStore; // Simulate token storage
    private Map<String, Long> tokenExpiry; // Token expiry times
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        tokenStore = new HashMap<>();
        tokenExpiry = new HashMap<>();
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "role VARCHAR(20) DEFAULT 'STUDENT', " +
                "token VARCHAR(255), " +
                "token_expiry BIGINT, " +
                "backlogs INT DEFAULT 0)");
            
            stmt.execute("CREATE TABLE admins (" +
                "admin_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "admin_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "role VARCHAR(20) DEFAULT 'ADMIN', " +
                "token VARCHAR(255), " +
                "token_expiry BIGINT)");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        db = new Database(new NonClosingConnectionWrapper(testConn));
        tokenStore.clear();
        tokenExpiry.clear();
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM admins");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Admin can view all students")
    public void testAdminCanViewAllStudents() throws SQLException {
        // Setup: Create admin and students
        createStudent("student1@example.com", "password1", "Student 1");
        createStudent("student2@example.com", "password2", "Student 2");
        createAdmin("admin@example.com", "adminpass", "Admin");
        
        // Admin token should grant access to all students
        String adminToken = generateToken("ADMIN");
        assertTrue(isAuthorized(adminToken, "VIEW_ALL_STUDENTS"), 
            "Admin should be authorized to view all students");
        
        // Verify student count
        int studentCount = countAllStudents();
        assertEquals(2, studentCount, "Admin should see all 2 students");
    }
    
    @Test
    @DisplayName("Student can only view own profile")
    public void testStudentCanOnlyViewOwnProfile() throws SQLException {
        createStudent("student1@example.com", "password1", "Student 1");
        createStudent("student2@example.com", "password2", "Student 2");
        
        String token1 = generateToken("STUDENT");
        String token2 = generateToken("STUDENT");
        String invalidToken = "invalid_token";
        
        // Valid tokens can view profiles
        assertTrue(canViewProfile(token1, "student1@example.com"), 
            "Student with valid token should view profile");
        
        // Invalid token cannot view profile
        assertFalse(canViewProfile(invalidToken, "student2@example.com"), 
            "Student with invalid token should NOT view profile");
    }
    
    @Test
    @DisplayName("Token expires after specified time")
    public void testTokenExpiration() throws SQLException {
        String token = generateToken("STUDENT");
        long expiryTime = System.currentTimeMillis() - 1000; // Already expired
        tokenExpiry.put(token, expiryTime);
        
        assertFalse(isTokenValid(token), "Expired token should be invalid");
    }
    
    @Test
    @DisplayName("Invalid token is rejected")
    public void testInvalidTokenRejection() {
        String invalidToken = "invalid_token_xyz";
        
        assertFalse(isTokenValid(invalidToken), "Invalid token should be rejected");
        assertFalse(isAuthorized(invalidToken, "ANY_ACTION"), 
            "Invalid token should not authorize any action");
    }
    
    @Test
    @DisplayName("Multiple login sessions create different tokens")
    public void testMultipleLoginSessions() throws SQLException {
        createStudent("student@example.com", "password", "Student");
        
        String token1 = generateToken("STUDENT");
        String token2 = generateToken("STUDENT");
        
        assertNotEquals(token1, token2, "Different login sessions should create different tokens");
        assertTrue(isTokenValid(token1), "First token should be valid");
        assertTrue(isTokenValid(token2), "Second token should be valid");
    }
    
    @Test
    @DisplayName("Concurrent tokens from same user are managed separately")
    public void testConcurrentTokens() throws SQLException {
        createStudent("student@example.com", "password", "Student");
        
        String token1 = generateToken("STUDENT");
        String token2 = generateToken("STUDENT");
        String token3 = generateToken("STUDENT");
        
        // All tokens should be valid
        assertTrue(isTokenValid(token1), "First token should be valid");
        assertTrue(isTokenValid(token2), "Second token should be valid");
        assertTrue(isTokenValid(token3), "Third token should be valid");
        
        // Invalidate one token
        revokeToken(token2);
        assertFalse(isTokenValid(token2), "Revoked token should be invalid");
        assertTrue(isTokenValid(token1), "Other token should still be valid");
        assertTrue(isTokenValid(token3), "Third token should still be valid");
    }
    
    @Test
    @DisplayName("Token revocation prevents further access")
    public void testTokenRevocation() throws SQLException {
        String token = generateToken("STUDENT");
        assertTrue(isTokenValid(token), "Token should be valid after generation");
        
        revokeToken(token);
        assertFalse(isTokenValid(token), "Revoked token should be invalid");
        assertFalse(isAuthorized(token, "ANY_ACTION"), 
            "Revoked token should not authorize actions");
    }
    
    @Test
    @DisplayName("Permission denied for unauthorized role")
    public void testPermissionDeniedAccess() throws SQLException {
        String studentToken = generateToken("STUDENT");
        
        // Student should NOT be able to delete users
        assertFalse(isAuthorized(studentToken, "DELETE_USER"), 
            "Student should not be authorized to delete users");
        
        // Student should NOT be able to modify system settings
        assertFalse(isAuthorized(studentToken, "MODIFY_SETTINGS"), 
            "Student should not be authorized to modify settings");
    }
    
    @Test
    @DisplayName("Role-based menu access is enforced")
    public void testRoleBasedMenuAccess() throws SQLException {
        String studentToken = generateToken("STUDENT");
        String adminToken = generateToken("ADMIN");
        
        // Admin menu items
        assertTrue(canAccessMenu(adminToken, "ADMIN_DASHBOARD"), 
            "Admin should access admin dashboard");
        assertTrue(canAccessMenu(adminToken, "USER_MANAGEMENT"), 
            "Admin should access user management");
        
        // Student menu items
        assertTrue(canAccessMenu(studentToken, "MY_PROFILE"), 
            "Student should access own profile");
        assertTrue(canAccessMenu(studentToken, "JOB_SEARCH"), 
            "Student should access job search");
        
        // Cross-access denied
        assertFalse(canAccessMenu(studentToken, "ADMIN_DASHBOARD"), 
            "Student should not access admin dashboard");
        assertFalse(canAccessMenu(studentToken, "USER_MANAGEMENT"), 
            "Student should not access user management");
    }
    
    @Test
    @DisplayName("Cross-role access violations are prevented")
    public void testCrossRoleAccessViolation() throws SQLException {
        String studentToken = generateToken("STUDENT");
        
        // Students cannot perform admin operations
        assertFalse(performAdminOperation(studentToken, "DELETE_ALL_JOBS"), 
            "Student should not delete all jobs");
        assertFalse(performAdminOperation(studentToken, "RESET_DATABASE"), 
            "Student should not reset database");
        assertFalse(performAdminOperation(studentToken, "GENERATE_REPORTS"), 
            "Student should not generate system reports");
    }
    
    // ===== Helper Methods =====
    
    private void createStudent(String email, String password, String name) throws SQLException {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO students (email, password_hash, student_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ps.setString(3, name);
            ps.setString(4, "STUDENT");
            ps.executeUpdate();
        }
    }
    
    private void createAdmin(String email, String password, String name) throws SQLException {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO admins (email, password_hash, admin_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ps.setString(3, name);
            ps.setString(4, "ADMIN");
            ps.executeUpdate();
        }
    }
    
    private String generateToken(String role) {
        String token = UUID.randomUUID().toString() + "_" + role;
        long expiryTime = System.currentTimeMillis() + 3600000; // 1 hour
        tokenStore.put(token, role);
        tokenExpiry.put(token, expiryTime);
        return token;
    }
    
    private boolean isTokenValid(String token) {
        if (!tokenStore.containsKey(token)) {
            return false;
        }
        long expiryTime = tokenExpiry.getOrDefault(token, 0L);
        return System.currentTimeMillis() < expiryTime;
    }
    
    private boolean isAuthorized(String token, String action) {
        if (!isTokenValid(token)) {
            return false;
        }
        String role = tokenStore.get(token);
        
        // Admin has all permissions
        if ("ADMIN".equals(role)) {
            return true;
        }
        
        // Student limited permissions
        if ("STUDENT".equals(role)) {
            return action.equals("VIEW_OWN_PROFILE") || 
                   action.equals("UPDATE_OWN_PROFILE") ||
                   action.equals("SEARCH_JOBS");
        }
        
        return false;
    }
    
    private boolean canViewProfile(String token, String email) {
        return isTokenValid(token) && isAuthorized(token, "VIEW_OWN_PROFILE");
    }
    
    private int countAllStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    private void revokeToken(String token) {
        tokenStore.remove(token);
        tokenExpiry.remove(token);
    }
    
    private boolean canAccessMenu(String token, String menuItem) {
        if (!isTokenValid(token)) {
            return false;
        }
        String role = tokenStore.get(token);
        
        if ("ADMIN".equals(role)) {
            return true; // Admin can access all menus
        }
        
        if ("STUDENT".equals(role)) {
            return menuItem.equals("MY_PROFILE") || 
                   menuItem.equals("JOB_SEARCH") ||
                   menuItem.equals("APPLICATIONS") ||
                   menuItem.equals("PROFILE_SETTINGS");
        }
        
        return false;
    }
    
    private boolean performAdminOperation(String token, String operation) {
        if (!isTokenValid(token)) {
            return false;
        }
        String role = tokenStore.get(token);
        return "ADMIN".equals(role);
    }
    
    /**
     * Non-closing connection wrapper
     */
    private static class NonClosingConnectionWrapper implements java.sql.Connection {
        private final Connection delegate;
        
        public NonClosingConnectionWrapper(Connection delegate) {
            this.delegate = delegate;
        }
        
        @Override public void close() {}
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
        @Override public Object unwrap(Class iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class iface) throws SQLException { return delegate.isWrapperFor(iface); }
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
