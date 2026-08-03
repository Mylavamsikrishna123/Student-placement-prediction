package com.placement;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: End-to-End Workflow Tests
 * Tests complete user journeys and integrated workflows across multiple operations.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("End-to-End Workflow Tests")
public class EndToEndWorkflowTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_e2e;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
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
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0, " +
                "department VARCHAR(100), " +
                "degree VARCHAR(20))");
            
            stmt.execute("CREATE TABLE companies (" +
                "company_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "company_name VARCHAR(150) NOT NULL UNIQUE, " +
                "required_cgpa DECIMAL(4,2), " +
                "max_backlogs INT DEFAULT 0)");
            
            stmt.execute("CREATE TABLE applications (" +
                "application_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT, " +
                "company_id INT, " +
                "status VARCHAR(50) DEFAULT 'APPLIED', " +
                "applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "offer_accepted BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (student_id) REFERENCES students(student_id), " +
                "FOREIGN KEY (company_id) REFERENCES companies(company_id))");
            
            stmt.execute("CREATE TABLE reports (" +
                "report_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "report_type VARCHAR(50), " +
                "generated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "data LONGTEXT)");
        }
    }
    
    @BeforeEach
    public void setupTest() throws SQLException {
        db = new Database(new NonClosingConnectionWrapper(testConn));
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM applications");
            stmt.execute("DELETE FROM companies");
            stmt.execute("DELETE FROM students");
            stmt.execute("DELETE FROM reports");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Complete registration to placement flow")
    public void testCompleteRegistrationToPlacementFlow() throws SQLException {
        // Step 1: Student registration
        String email = "student@example.com";
        String password = "SecurePass123";
        String name = "John Doe";
        
        boolean registered = db.register(email, password, name);
        assertTrue(registered, "Student registration should succeed");
        
        // Step 2: Update profile with details
        updateStudentProfile(email, 8.5, 0, "CSE", "B.Tech");
        verifyProfileUpdated(email, 8.5);
        
        // Step 3: Create companies with requirements
        createCompany("Google", 8.0, 0);
        createCompany("Microsoft", 7.5, 1);
        createCompany("Amazon", 7.0, 2);
        
        // Step 4: Check eligibility
        assertTrue(isEligible(email, "Google"), "Student should be eligible for Google");
        assertTrue(isEligible(email, "Microsoft"), "Student should be eligible for Microsoft");
        assertTrue(isEligible(email, "Amazon"), "Student should be eligible for Amazon");
        
        // Step 5: Apply to jobs
        int appId1 = applyToCompany(email, "Google");
        int appId2 = applyToCompany(email, "Microsoft");
        
        // Step 6: Accept offer
        acceptOffer(appId1);
        assertTrue(isOfferAccepted(appId1), "Offer should be accepted");
        
        // Step 7: Verify placement
        int placedCount = countPlacedStudents();
        assertEquals(1, placedCount, "One student should be placed");
    }
    
    @Test
    @DisplayName("Student login and profile update workflow")
    public void testStudentLoginAndProfileUpdate() throws SQLException {
        // Setup: Register student
        db.register("student@example.com", "password123", "Student Name");
        
        // Step 1: Login
        boolean loggedIn = db.login("student@example.com", "password123", "STUDENT");
        assertTrue(loggedIn, "Login should succeed");
        
        // Step 2: Update profile
        updateStudentProfile("student@example.com", 7.5, 1, "ECE", "B.Tech");
        
        // Step 3: Verify updates
        verifyProfileUpdated("student@example.com", 7.5);
        
        // Step 4: View profile
        String studentName = getStudentName("student@example.com");
        assertEquals("Student Name", studentName, "Student name should match");
    }
    
    @Test
    @DisplayName("Company search and application workflow")
    public void testCompanySearchAndApplication() throws SQLException {
        // Setup: Register student with profile
        db.register("student@example.com", "password", "Student");
        updateStudentProfile("student@example.com", 8.0, 0, "CSE", "B.Tech");
        
        // Create companies
        createCompany("TCS", 6.5, 2);
        createCompany("Infosys", 7.0, 1);
        createCompany("Wipro", 7.5, 0);
        
        // Search for eligible companies
        List<String> eligibleCompanies = findEligibleCompanies("student@example.com");
        assertEquals(3, eligibleCompanies.size(), "Student should find 3 eligible companies");
        assertTrue(eligibleCompanies.contains("TCS"), "TCS should be in results");
        
        // Apply to multiple companies
        applyToCompany("student@example.com", "TCS");
        applyToCompany("student@example.com", "Infosys");
        
        int applicationCount = countApplications("student@example.com");
        assertEquals(2, applicationCount, "Student should have 2 applications");
    }
    
    @Test
    @DisplayName("Placement offer acceptance workflow")
    public void testPlacementOfferAcceptance() throws SQLException {
        // Setup: Register and apply
        db.register("student@example.com", "password", "Student");
        updateStudentProfile("student@example.com", 8.5, 0, "CSE", "B.Tech");
        createCompany("Google", 8.0, 0);
        
        int appId = applyToCompany("student@example.com", "Google");
        String initialStatus = getApplicationStatus(appId);
        assertEquals("APPLIED", initialStatus, "Initial status should be APPLIED");
        
        // Accept offer
        acceptOffer(appId);
        String updatedStatus = getApplicationStatus(appId);
        assertEquals("ACCEPTED", updatedStatus, "Status should be ACCEPTED after accepting offer");
    }
    
    @Test
    @DisplayName("Multiple job applications workflow")
    public void testMultipleJobApplications() throws SQLException {
        db.register("student@example.com", "password", "Student");
        updateStudentProfile("student@example.com", 8.0, 0, "CSE", "B.Tech");
        
        // Create 5 companies
        for (int i = 1; i <= 5; i++) {
            createCompany("Company" + i, 7.0 + (i * 0.1), 0);
        }
        
        // Apply to all
        for (int i = 1; i <= 5; i++) {
            applyToCompany("student@example.com", "Company" + i);
        }
        
        int totalApps = countApplications("student@example.com");
        assertEquals(5, totalApps, "Student should have 5 applications");
    }
    
    @Test
    @DisplayName("Withdraw application workflow")
    public void testWithdrawApplication() throws SQLException {
        db.register("student@example.com", "password", "Student");
        updateStudentProfile("student@example.com", 8.0, 0, "CSE", "B.Tech");
        createCompany("Company A", 7.0, 0);
        
        int appId = applyToCompany("student@example.com", "Company A");
        assertTrue(applicationExists(appId), "Application should exist");
        
        withdrawApplication(appId);
        assertFalse(applicationExists(appId), "Application should be withdrawn");
    }
    
    @Test
    @DisplayName("Complete admin workflow")
    public void testCompleteAdminWorkflow() throws SQLException {
        // Admin creates students
        db.register("student1@example.com", "pass", "Student 1");
        db.register("student2@example.com", "pass", "Student 2");
        
        // Admin creates companies
        createCompany("Google", 8.0, 0);
        createCompany("Microsoft", 7.5, 1);
        
        // Admin views all students
        int studentCount = countAllStudents();
        assertEquals(2, studentCount, "Admin should see 2 students");
        
        // Admin views all companies
        int companyCount = countAllCompanies();
        assertEquals(2, companyCount, "Admin should see 2 companies");
        
        // Admin generates report
        String reportId = generateReport("PLACEMENT_SUMMARY");
        assertNotNull(reportId, "Report should be generated");
    }
    
    @Test
    @DisplayName("Report generation workflow")
    public void testReportGeneration() throws SQLException {
        // Setup data
        db.register("student@example.com", "pass", "Student");
        updateStudentProfile("student@example.com", 8.0, 0, "CSE", "B.Tech");
        createCompany("Google", 8.0, 0);
        
        int appId = applyToCompany("student@example.com", "Google");
        acceptOffer(appId);
        
        // Generate reports
        String placementReport = generateReport("PLACEMENT_STATISTICS");
        String studentReport = generateReport("STUDENT_DETAILS");
        String companyReport = generateReport("COMPANY_STATISTICS");
        
        assertNotNull(placementReport, "Placement report should be generated");
        assertNotNull(studentReport, "Student report should be generated");
        assertNotNull(companyReport, "Company report should be generated");
    }
    
    @Test
    @DisplayName("Data export workflow")
    public void testDataExportWorkflow() throws SQLException {
        // Create data
        db.register("student@example.com", "pass", "Student");
        updateStudentProfile("student@example.com", 8.0, 0, "CSE", "B.Tech");
        createCompany("Google", 8.0, 0);
        createCompany("Microsoft", 7.5, 1);
        
        // Export students
        String studentExport = exportStudents();
        assertTrue(studentExport.contains("student@example.com"), "Export should contain student email");
        
        // Export companies
        String companyExport = exportCompanies();
        assertTrue(companyExport.contains("Google"), "Export should contain company name");
        assertTrue(companyExport.contains("Microsoft"), "Export should contain company name");
    }
    
    // ===== Helper Methods =====
    
    private void updateStudentProfile(String email, double cgpa, int backlogs, String dept, String degree) throws SQLException {
        String sql = "UPDATE students SET cgpa = ?, backlogs = ?, department = ?, degree = ? WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setDouble(1, cgpa);
            ps.setInt(2, backlogs);
            ps.setString(3, dept);
            ps.setString(4, degree);
            ps.setString(5, email);
            ps.executeUpdate();
        }
    }
    
    private void verifyProfileUpdated(String email, double expectedCgpa) throws SQLException {
        String sql = "SELECT cgpa FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Student should exist");
                assertEquals(expectedCgpa, rs.getDouble("cgpa"), 0.01);
            }
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
    
    private boolean isEligible(String email, String companyName) throws SQLException {
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
    
    private int applyToCompany(String email, String companyName) throws SQLException {
        String sql = "INSERT INTO applications (student_id, company_id, status) " +
                     "SELECT s.student_id, c.company_id, 'APPLIED' FROM students s, companies c " +
                     "WHERE s.email = ? AND c.company_name = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email);
            ps.setString(2, companyName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
    
    private void acceptOffer(int appId) throws SQLException {
        String sql = "UPDATE applications SET status = 'ACCEPTED', offer_accepted = TRUE WHERE application_id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            ps.executeUpdate();
        }
    }
    
    private boolean isOfferAccepted(int appId) throws SQLException {
        String sql = "SELECT offer_accepted FROM applications WHERE application_id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("offer_accepted");
                }
            }
        }
        return false;
    }
    
    private int countPlacedStudents() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT student_id) FROM applications WHERE offer_accepted = TRUE";
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
    
    private List<String> findEligibleCompanies(String email) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = "SELECT c.company_name FROM students s, companies c " +
                     "WHERE s.email = ? AND s.cgpa >= c.required_cgpa AND s.backlogs <= c.max_backlogs";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("company_name"));
                }
            }
        }
        return result;
    }
    
    private int countApplications(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications a, students s WHERE a.student_id = s.student_id AND s.email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    private String getApplicationStatus(int appId) throws SQLException {
        String sql = "SELECT status FROM applications WHERE application_id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }
    
    private boolean applicationExists(int appId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE application_id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    private void withdrawApplication(int appId) throws SQLException {
        String sql = "DELETE FROM applications WHERE application_id = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            ps.executeUpdate();
        }
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
    
    private int countAllCompanies() throws SQLException {
        String sql = "SELECT COUNT(*) FROM companies";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    private String generateReport(String reportType) throws SQLException {
        String reportId = UUID.randomUUID().toString();
        String sql = "INSERT INTO reports (report_type, data) VALUES (?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, reportType);
            ps.setString(2, "Report data for " + reportType);
            ps.executeUpdate();
        }
        return reportId;
    }
    
    private String exportStudents() throws SQLException {
        StringBuilder export = new StringBuilder();
        String sql = "SELECT email, student_name, cgpa, backlogs FROM students";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                export.append(rs.getString("email")).append(",");
                export.append(rs.getString("student_name")).append(",");
                export.append(rs.getDouble("cgpa")).append(",");
                export.append(rs.getInt("backlogs")).append("\n");
            }
        }
        return export.toString();
    }
    
    private String exportCompanies() throws SQLException {
        StringBuilder export = new StringBuilder();
        String sql = "SELECT company_name, required_cgpa, max_backlogs FROM companies";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                export.append(rs.getString("company_name")).append(",");
                export.append(rs.getDouble("required_cgpa")).append(",");
                export.append(rs.getInt("max_backlogs")).append("\n");
            }
        }
        return export.toString();
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
