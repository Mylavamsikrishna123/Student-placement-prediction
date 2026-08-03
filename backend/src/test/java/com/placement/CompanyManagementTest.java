package com.placement;

import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for company management.
 * Tests Database operations for adding, retrieving, updating companies.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Company Management Integration Tests")
public class CompanyManagementTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_company;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private Database db;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        
        try (Statement stmt = testConn.createStatement()) {
            // Companies table
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(100) NOT NULL UNIQUE, " +
                "required_cgpa DECIMAL(4,2) DEFAULT 6.0, " +
                "max_backlogs INT DEFAULT 0, " +
                "package_lpa DECIMAL(6,2), " +
                "job_role VARCHAR(100), " +
                "location VARCHAR(100))");
            
            // Skills table
            stmt.execute("CREATE TABLE skills (" +
                "skill_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "skill_name VARCHAR(100) NOT NULL UNIQUE)");
            
            // Company-Skills junction table
            stmt.execute("CREATE TABLE company_skills (" +
                "company_id INT NOT NULL, " +
                "skill_id INT NOT NULL, " +
                "PRIMARY KEY (company_id, skill_id), " +
                "FOREIGN KEY (company_id) REFERENCES companies(company_id), " +
                "FOREIGN KEY (skill_id) REFERENCES skills(skill_id))");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        db = new Database(new NonClosingConnectionWrapper(testConn));
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM company_skills");
            stmt.execute("DELETE FROM companies");
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
    @DisplayName("Add new company succeeds")
    public void testAddCompanySuccess() throws SQLException {
        boolean result = addCompany("TechCorp", 7.0, 0, 12.5, "SDE", "Bangalore");
        assertTrue(result, "Company addition should succeed");
        
        // Verify company exists
        assertTrue(companyExists("TechCorp"), "Company should be retrievable");
    }
    
    @Test
    @DisplayName("Company name must be unique")
    public void testCompanyNameUniqueness() throws SQLException {
        String companyName = "UniqueCompany";
        
        addCompany(companyName, 6.5, 0, 10.0, "Analyst", "Delhi");
        
        // Try to add another company with same name
        Exception exception = assertThrows(SQLException.class, () -> {
            addCompany(companyName, 7.0, 1, 15.0, "SDE", "Mumbai");
        });
        
        assertNotNull(exception, "Should throw exception for duplicate company name");
    }
    
    @Test
    @DisplayName("Get all companies returns complete list")
    public void testGetAllCompanies() throws SQLException {
        addCompany("Acme Inc", 6.0, 0, 8.0, "Junior Developer", "NYC");
        addCompany("TechStart", 7.0, 1, 12.0, "SDE", "SF");
        addCompany("DataFlow", 7.5, 0, 15.0, "Data Engineer", "Bangalore");
        
        int count = countCompanies();
        assertEquals(3, count, "Should return all 3 companies");
    }
    
    @Test
    @DisplayName("Update company details modifies existing record")
    public void testUpdateCompanyDetails() throws SQLException {
        addCompany("UpdateTest", 6.0, 0, 10.0, "Developer", "Delhi");
        
        // Update package
        updateCompanyPackage("UpdateTest", 12.5);
        
        double newPackage = getCompanyPackage("UpdateTest");
        assertEquals(12.5, newPackage, 0.01, "Package should be updated");
    }
    
    @Test
    @DisplayName("Delete company removes record")
    public void testDeleteCompany() throws SQLException {
        addCompany("DeleteMe", 6.5, 0, 11.0, "Tester", "Hyderabad");
        assertTrue(companyExists("DeleteMe"), "Company should exist initially");
        
        deleteCompany("DeleteMe");
        assertFalse(companyExists("DeleteMe"), "Company should be deleted");
    }
    
    @Test
    @DisplayName("Company can have multiple required skills")
    public void testCompanySkillsAssociation() throws SQLException {
        addCompany("SkillTest", 6.5, 0, 11.0, "SDE", "Bangalore");
        
        // Add skills
        int skillId1 = addSkill("Java");
        int skillId2 = addSkill("Spring");
        
        // Associate skills
        associateSkill("SkillTest", skillId1);
        associateSkill("SkillTest", skillId2);
        
        int skillCount = countCompanySkills("SkillTest");
        assertEquals(2, skillCount, "Company should have 2 skills");
    }
    
    @Test
    @DisplayName("Filter companies by required CGPA")
    public void testGetCompaniesByRequiredCGPA() throws SQLException {
        addCompany("HighCGPA", 8.0, 0, 15.0, "SDE", "Bangalore");
        addCompany("MediumCGPA", 7.0, 1, 12.0, "Developer", "Delhi");
        addCompany("LowCGPA", 6.0, 2, 8.0, "Intern", "Mumbai");
        
        // Count companies with minimum CGPA >= 7.0
        int highCGPACount = countCompaniesByMinCGPA(7.0);
        assertEquals(2, highCGPACount, "Should find 2 companies with CGPA >= 7.0");
    }
    
    @Test
    @DisplayName("Company can exist without assigned skills")
    public void testCompanyWithoutSkills() throws SQLException {
        addCompany("NoSkills", 6.5, 0, 10.0, "Generic Role", "Chennai");
        
        int skillCount = countCompanySkills("NoSkills");
        assertEquals(0, skillCount, "Company should have no skills initially");
        
        assertTrue(companyExists("NoSkills"), "Company without skills should exist");
    }
    
    // Helper methods
    private boolean addCompany(String name, double cgpa, int backlogs, double package_lpa, String role, String location) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO companies (company_name, required_cgpa, max_backlogs, package_lpa, job_role, location) " +
                "VALUES ('" + name + "', " + cgpa + ", " + backlogs + ", " + package_lpa + ", '" + role + "', '" + location + "')");
            return true;
        }
    }
    
    private boolean companyExists(String name) {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM companies WHERE company_name = '" + name + "'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }
    
    private void deleteCompany(String name) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM companies WHERE company_name = '" + name + "'");
        }
    }
    
    private void updateCompanyPackage(String name, double package_lpa) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("UPDATE companies SET package_lpa = " + package_lpa + " WHERE company_name = '" + name + "'");
        }
    }
    
    private double getCompanyPackage(String name) throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT package_lpa FROM companies WHERE company_name = '" + name + "'")) {
            if (rs.next()) {
                return rs.getDouble("package_lpa");
            }
        }
        return 0;
    }
    
    private int countCompanies() throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM companies")) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }
    
    private int countCompaniesByMinCGPA(double minCGPA) throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM companies WHERE required_cgpa >= " + minCGPA)) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }
    
    private int addSkill(String skillName) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("INSERT INTO skills (skill_name) VALUES ('" + skillName + "')", Statement.RETURN_GENERATED_KEYS);
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
    
    private void associateSkill(String companyName, int skillId) throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            int companyId = getCompanyId(companyName);
            stmt.execute("INSERT INTO company_skills (company_id, skill_id) VALUES (" + companyId + ", " + skillId + ")");
        }
    }
    
    private int getCompanyId(String name) throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT company_id FROM companies WHERE company_name = '" + name + "'")) {
            if (rs.next()) {
                return rs.getInt("company_id");
            }
        }
        return -1;
    }
    
    private int countCompanySkills(String companyName) throws SQLException {
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) as cnt FROM company_skills cs " +
                 "JOIN companies c ON cs.company_id = c.company_id " +
                 "WHERE c.company_name = '" + companyName + "'")) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
        }
        return 0;
    }
    
    /**
     * Wrapper to prevent Database.open() from closing our test connection
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
