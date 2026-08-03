package com.placement;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for student eligibility checking.
 * Tests eligibility logic based on CGPA, backlogs, and skills.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Eligibility Check Integration Tests")
public class EligibilityCheckTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_eligibility;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
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
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0)");
            
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(100) NOT NULL UNIQUE, " +
                "required_cgpa DECIMAL(4,2) DEFAULT 6.0, " +
                "max_backlogs INT DEFAULT 0)");
            
            stmt.execute("CREATE TABLE eligibility (" +
                "student_id INT NOT NULL, " +
                "company_id INT NOT NULL, " +
                "eligible BOOLEAN DEFAULT FALSE, " +
                "PRIMARY KEY (student_id, company_id))");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        db = new Database(new NonClosingConnectionWrapper(testConn));
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM eligibility");
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM companies");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Student eligible when CGPA meets minimum threshold")
    public void testStudentEligibleByCGPA() throws SQLException {
        addStudent("eligibleCGPA@test.com", "Eligible Student", 7.5, 0);
        addCompany("CompanyA", 7.0, 0);
        
        boolean eligible = isEligible("eligibleCGPA@test.com", "CompanyA");
        assertTrue(eligible, "Student with sufficient CGPA should be eligible");
    }
    
    @Test
    @DisplayName("Student ineligible when backlogs exceed maximum")
    public void testStudentIneligibleByBacklogs() throws SQLException {
        addStudent("tooManyBacklogs@test.com", "Backlog Student", 8.0, 3);
        addCompany("CompanyB", 7.0, 1);
        
        boolean eligible = isEligible("tooManyBacklogs@test.com", "CompanyB");
        assertFalse(eligible, "Student with excess backlogs should be ineligible");
    }
    
    @Test
    @DisplayName("Eligibility respects exact CGPA threshold")
    public void testEligibilityExactCGPAThreshold() throws SQLException {
        addStudent("exactCGPA@test.com", "Exact CGPA Student", 7.0, 0);
        addCompany("CompanyC", 7.0, 0);
        
        boolean eligible = isEligible("exactCGPA@test.com", "CompanyC");
        assertTrue(eligible, "Student with exact CGPA threshold should be eligible");
    }
    
    @Test
    @DisplayName("Eligibility allows companies accepting backlogs")
    public void testEligibilityBacklogsAllowed() throws SQLException {
        addStudent("hasBacklogs@test.com", "Student With Backlogs", 6.5, 2);
        addCompany("CompanyD", 6.0, 3);
        
        boolean eligible = isEligible("hasBacklogs@test.com", "CompanyD");
        assertTrue(eligible, "Student should be eligible for companies allowing backlogs");
    }
    
    @Test
    @DisplayName("Multiple company eligibility checking works")
    public void testMultipleCompanyEligibility() throws SQLException {
        addStudent("multiCompany@test.com", "Multi Company Student", 7.5, 1);
        addCompany("CompanyE", 8.0, 0);
        addCompany("CompanyF", 7.0, 1);
        addCompany("CompanyG", 6.5, 2);
        
        assertFalse(isEligible("multiCompany@test.com", "CompanyE"), "Should be ineligible for high-CGPA company");
        assertTrue(isEligible("multiCompany@test.com", "CompanyF"), "Should be eligible for medium company");
        assertTrue(isEligible("multiCompany@test.com", "CompanyG"), "Should be eligible for low-CGPA company");
    }
    
    @Test
    @DisplayName("No backlogs required for all companies")
    public void testEligibilityNoBacklogsRequired() throws SQLException {
        addStudent("zeroBacklogs@test.com", "No Backlogs Student", 6.5, 0);
        addCompany("CompanyH", 6.0, 0);
        
        boolean eligible = isEligible("zeroBacklogs@test.com", "CompanyH");
        assertTrue(eligible, "Student with no backlogs should be eligible");
    }
    
    @Test
    @DisplayName("Complex eligibility scenarios combine constraints")
    public void testComplexEligibilityScenarios() throws SQLException {
        // Scenario 1: Low CGPA, high backlogs - definitely ineligible
        addStudent("lowPerformer@test.com", "Low Performer", 5.5, 5);
        addCompany("StrictCompany", 7.0, 0);
        assertFalse(isEligible("lowPerformer@test.com", "StrictCompany"), "Low performer should be ineligible");
        
        // Scenario 2: Medium CGPA, medium backlogs - depends on requirements
        addStudent("averageStudent@test.com", "Average Student", 6.8, 2);
        addCompany("ModerateCompany", 6.5, 2);
        assertTrue(isEligible("averageStudent@test.com", "ModerateCompany"), "Average student should be eligible for moderate company");
    }
    
    // Helper methods
    private void addStudent(String email, String name, double cgpa, int backlogs) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO students (email, student_name, cgpa, backlogs) VALUES " +
                "('" + email + "', '" + name + "', " + cgpa + ", " + backlogs + ")");
        }
    }
    
    private void addCompany(String name, double requiredCGPA, int maxBacklogs) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO companies (company_name, required_cgpa, max_backlogs) VALUES " +
                "('" + name + "', " + requiredCGPA + ", " + maxBacklogs + ")");
        }
    }
    
    private boolean isEligible(String email, String companyName) throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT s.cgpa, s.backlogs, c.required_cgpa, c.max_backlogs " +
                 "FROM students s, companies c " +
                 "WHERE s.email = '" + email + "' AND c.company_name = '" + companyName + "'")) {
            if (rs.next()) {
                double studentCGPA = rs.getDouble("cgpa");
                int studentBacklogs = rs.getInt("backlogs");
                double requiredCGPA = rs.getDouble("required_cgpa");
                int maxBacklogs = rs.getInt("max_backlogs");
                
                return studentCGPA >= requiredCGPA && studentBacklogs <= maxBacklogs;
            }
        }
        return false;
    }
    
    /**
     * Wrapper to prevent Database.open() from closing test connection
     */
    private static class NonClosingConnectionWrapper implements java.sql.Connection {
        private final Connection delegate;
        public NonClosingConnectionWrapper(Connection delegate) { this.delegate = delegate; }
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
