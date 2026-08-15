package com.placement;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Thin JDBC helper used by the API layer. Each call opens a connection,
 * does the work, and closes it. This keeps the code easy to follow without
 * introducing a connection pool for this project size.
 */
public class Database {
    private String dbUrl;
    private String dbUser;
    private String dbPass;
    private Connection testConnection; // For test injection

    /**
     * Read DB settings from environment with sensible defaults for local dev.
     * DB_URL, DB_USER, DB_PASS can override the defaults.
     */
    public Database() {
        this.dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/JAVAPROJECT");
        this.dbUser = System.getenv().getOrDefault("DB_USER", "root");
        this.dbPass = System.getenv().getOrDefault("DB_PASS", "root");
        this.testConnection = null;
    }

    /**
     * Probe the configured database once at startup so misconfiguration is reported
     * before the server begins serving requests. Non-fatal: the server still starts
     * and surfaces connection errors on the first real request.
     */
    public void validateConnection() {
        try {
            System.out.println("Validating database connection...");
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            System.out.println("✓ Database connection successful");
            System.out.println("  URL: " + dbUrl);
            System.out.println("  User: " + dbUser);
            conn.close();
        } catch (SQLException e) {
            System.err.println("✗ DATABASE CONNECTION FAILED!");
            System.err.println("  Error: " + e.getMessage());
            System.err.println("  URL: " + dbUrl);
            System.err.println("  User: " + dbUser);
            System.err.println("\nPlease check:");
            System.err.println("  1. MySQL is running (net start MySQL80)");
            System.err.println("  2. Database 'JAVAPROJECT' exists");
            System.err.println("  3. User '" + dbUser + "' has correct password");
            System.err.println("  4. Run: mysql -u root -p < database_schema.sql");
        }
    }
    
    /**
     * Constructor for testing with injected connection.
     */
    public Database(Connection testConnection) {
        this.testConnection = testConnection;
    }

    /** Open a new JDBC connection. Caller is responsible for closing. */
    private Connection open() throws SQLException {
        if (testConnection != null) {
            // Wrap the injected connection so try-with-resources in callers does NOT
            // close the shared test connection. Only the wrapper is closed.
            return new NonClosingConnection(testConnection);
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    /**
     * Connection wrapper whose close() is a no-op. Used when a test injects a shared
     * connection so that try-with-resources in individual Database methods does not
     * close the connection other tests rely on.
     */
    private static final class NonClosingConnection implements Connection {
        private final Connection delegate;
        NonClosingConnection(Connection delegate) { this.delegate = delegate; }
        @Override public void close() throws SQLException { /* no-op: keep test connection alive */ }
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
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
        @Override public java.util.Map<String,Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String,Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
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
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }

    /**
     * Register a new student account. Password is hashed with BCrypt before storage.
     * Name is derived from email for initial creation; full profile completed later.
     *
     * @return true on insert success; throws EMAIL_DUPLICATE for unique email violation.
     */
    public boolean register(String email, String password, String name) {
        // Validate inputs
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        
        // Hash password before storage
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        String sql = "INSERT INTO students (student_name, student_id_number, email, password, department, degree) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            // Do not auto-generate student_id_number; leave NULL to be set later via profile save
            ps.setNull(2, java.sql.Types.VARCHAR);
            ps.setString(3, email);
            ps.setString(4, hashedPassword);
            ps.setNull(5, java.sql.Types.VARCHAR); // department can be NULL
            // Do not prefill degree; leave NULL until student sets it in profile
            ps.setNull(6, java.sql.Types.VARCHAR);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            // Check for duplicate email error
            String msg = e.getMessage() != null ? e.getMessage() : "";
            
            // MySQL error code 1062
            if (e.getErrorCode() == 1062 && msg.toLowerCase().contains("email")) {
                throw new RuntimeException("EMAIL_DUPLICATE", e);
            }
            
            // H2 error code 23505 (integrity constraint violation)
            if (e.getErrorCode() == 23505) {
                throw new RuntimeException("EMAIL_DUPLICATE", e);
            }
            
            // H2 and other databases: check error message for UNIQUE constraint
            if (msg.toUpperCase().contains("UNIQUE") && msg.toUpperCase().contains("EMAIL")) {
                throw new RuntimeException("EMAIL_DUPLICATE", e);
            }
            
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validate credentials for either student or admin tables using BCrypt.
     * Passwords are always BCrypt hashes; there is no plaintext fallback.
     */
    public boolean login(String email, String password, String role) {
        String table = "students";
        if ("admin".equalsIgnoreCase(role)) table = "admin";

        String sql = "SELECT password FROM " + table + " WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedPassword = rs.getString("password");
                if (storedPassword == null || !storedPassword.startsWith("$2")) {
                    // Not a BCrypt hash — reject (no plaintext fallback).
                    return false;
                }
                try {
                    return BCrypt.checkpw(password, storedPassword);
                } catch (Exception e) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Save or update a student's profile and skills in a single transaction.
     * Skills are replaced wholesale based on the provided map.
     */
    public boolean saveProfile(String email, String name, String idNumber, String department,
                               String degree, String collegeName, String phone, Double cgpa,
                               Map<String,Integer> skills, String branch, String certifications, int backlogs) {
        Connection c = null;
        try {
            c = open();
            c.setAutoCommit(false);
            // Ensure ID number uniqueness (allow keeping same value for this email)
            if (!isIdNumberAvailable(idNumber, email)) {
                throw new SQLException("ID_NUMBER_DUPLICATE");
            }
            String updateSql = "UPDATE students SET student_name=?, student_id_number=?, department=?, degree=?, college_name=?, phone_number=?, cgpa=?, certifications=?, backlogs=? WHERE LOWER(email)=LOWER(?)";
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                ps.setString(1, name);
                ps.setString(2, idNumber);
                ps.setString(3, department);
                ps.setString(4, degree);
                ps.setString(5, collegeName);
                ps.setString(6, phone);
                ps.setDouble(7, cgpa != null ? cgpa : 0.0);
                ps.setString(8, certifications != null ? certifications : "");
                ps.setInt(9, backlogs);
                ps.setString(10, email);
                ps.executeUpdate();
            }

            String deleteSkills = "DELETE FROM student_skills WHERE student_id = (SELECT student_id FROM students WHERE LOWER(email)=LOWER(?))";
            try (PreparedStatement ps = c.prepareStatement(deleteSkills)) {
                ps.setString(1, email);
                ps.executeUpdate();
            }

            if (skills != null && !skills.isEmpty()) {
                for (Map.Entry<String,Integer> e : skills.entrySet()) {
                    int skillId = getOrCreateSkillId(c, e.getKey());
                    if (skillId > 0) {
                        String insert = "INSERT INTO student_skills (student_id, skill_id, skill_level) VALUES ((SELECT student_id FROM students WHERE LOWER(email)=LOWER(?)), ?, ?)";
                        try (PreparedStatement ps = c.prepareStatement(insert)) {
                            ps.setString(1, email);
                            ps.setInt(2, skillId);
                            ps.setInt(3, e.getValue());
                            ps.executeUpdate();
                        }
                    }
                }
            }

            c.commit();
            c.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try { if (c != null) { c.rollback(); c.setAutoCommit(true); } } catch (SQLException ex) {}
            return false;
        } finally {
            try { if (c != null && !c.isClosed()) c.close(); } catch (SQLException ex) {}
        }
    }
    
    /** Overload kept for callers that don’t pass new fields yet. */
    public boolean saveProfile(String email, String name, String idNumber, String department,
                               String degree, String collegeName, String phone, Double cgpa,
                               Map<String,Integer> skills) {
        return saveProfile(email, name, idNumber, department, degree, collegeName, phone, cgpa, skills, "", "", 0);
    }

    /** Load a student's profile with a flat map plus a nested skills map. */
    public Map<String,Object> getStudentProfile(String email) {
        Map<String,Object> profile = new HashMap<>();
        String sql = "SELECT student_id, student_name, student_id_number, email, department, degree, cgpa, college_name, phone_number, certifications, backlogs FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile.put("id", rs.getInt("student_id"));
                    profile.put("name", rs.getString("student_name"));
                    profile.put("idNumber", rs.getString("student_id_number"));
                    profile.put("email", rs.getString("email"));
                    profile.put("department", rs.getString("department"));
                    profile.put("degree", rs.getString("degree"));
                    Object cgpaObj = rs.getObject("cgpa");
                    profile.put("cgpa", cgpaObj != null ? ((Number)cgpaObj).doubleValue() : null);
                    profile.put("collegeName", rs.getString("college_name"));
                    profile.put("phone", rs.getString("phone_number"));
                    profile.put("certifications", rs.getString("certifications"));
                    Object backlogsObj = rs.getObject("backlogs");
                    profile.put("backlogs", backlogsObj != null ? ((Number)backlogsObj).intValue() : 0);

                    int studentId = rs.getInt("student_id");
                    Map<String,Integer> skills = new HashMap<>();
                    String skillsSql = "SELECT s.skill_name, ss.skill_level FROM student_skills ss JOIN skills s ON ss.skill_id = s.skill_id WHERE ss.student_id = ?";
                    try (PreparedStatement sp = c.prepareStatement(skillsSql)) {
                        sp.setInt(1, studentId);
                        try (ResultSet rs2 = sp.executeQuery()) {
                            while (rs2.next()) skills.put(rs2.getString("skill_name"), rs2.getInt("skill_level"));
                        }
                    }
                    profile.put("skills", skills);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return profile;
    }

    /** List companies with a simple, human-readable skills summary. */
    public List<Map<String,Object>> getCompanies() {
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT c.company_id, c.company_name, c.application_link, c.required_cgpa, "
                + "GROUP_CONCAT(CONCAT(s.skill_name, '(', cs.required_level, '★)') SEPARATOR ', ') as required_skills "
                + "FROM companies c LEFT JOIN company_skills cs ON c.company_id = cs.company_id LEFT JOIN skills s ON cs.skill_id = s.skill_id "
                + "GROUP BY c.company_id, c.company_name, c.application_link, c.required_cgpa ORDER BY c.company_name";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> comp = new HashMap<>();
                comp.put("id", rs.getInt("company_id"));
                comp.put("name", rs.getString("company_name"));
                comp.put("link", rs.getString("application_link"));
                comp.put("skills", rs.getString("required_skills"));
                Object cgpaObj = rs.getObject("required_cgpa");
                if (cgpaObj != null) comp.put("requiredCgpa", cgpaObj instanceof Number ? ((Number)cgpaObj).doubleValue() : Double.parseDouble(cgpaObj.toString()));
                else comp.put("requiredCgpa", null);
                list.add(comp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Insert a company and its required skills in one go. */
    public boolean addCompany(String name, String link, double requiredCgpa, Map<String,Integer> requiredSkills) {
        Connection c = null;
        try {
            c = open();
            c.setAutoCommit(false);
            String insert = "INSERT INTO companies (company_name, application_link, required_cgpa) VALUES (?, ?, ?)";
            int companyId = -1;
            try (PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, link);
                if (requiredCgpa >= 0) ps.setDouble(3, requiredCgpa); else ps.setNull(3, java.sql.Types.DECIMAL);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) companyId = rs.getInt(1); }
            }

            if (companyId > 0 && requiredSkills != null) {
                for (Map.Entry<String,Integer> e : requiredSkills.entrySet()) {
                    int skillId = getOrCreateSkillId(c, e.getKey());
                    if (skillId > 0) {
                        String ins = "INSERT INTO company_skills (company_id, skill_id, required_level) VALUES (?, ?, ?)";
                        try (PreparedStatement ps = c.prepareStatement(ins)) {
                            ps.setInt(1, companyId); ps.setInt(2, skillId); ps.setInt(3, e.getValue()); ps.executeUpdate();
                        }
                    }
                }
            }
            c.commit(); c.setAutoCommit(true); return true;
        } catch (SQLException e) { e.printStackTrace(); try { if (c != null) { c.rollback(); c.setAutoCommit(true); } } catch (SQLException ex) {} return false; }
        finally { try { if (c!=null) c.close(); } catch (SQLException ex) {} }
    }

    /** Update a company and replace its required skills. */
    public boolean updateCompany(int companyId, String name, String link, double requiredCgpa, Map<String,Integer> requiredSkills) {
        Connection c = null;
        try {
            c = open(); c.setAutoCommit(false);
            String upd = "UPDATE companies SET company_name=?, application_link=?, required_cgpa=? WHERE company_id=?";
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                ps.setString(1, name); ps.setString(2, link);
                if (requiredCgpa >= 0) ps.setDouble(3, requiredCgpa); else ps.setNull(3, java.sql.Types.DECIMAL);
                ps.setInt(4, companyId); ps.executeUpdate();
            }
            String del = "DELETE FROM company_skills WHERE company_id=?";
            try (PreparedStatement ps = c.prepareStatement(del)) { ps.setInt(1, companyId); ps.executeUpdate(); }
            if (requiredSkills != null) {
                for (Map.Entry<String,Integer> e : requiredSkills.entrySet()) {
                    int skillId = getOrCreateSkillId(c, e.getKey());
                    if (skillId > 0) {
                        String ins = "INSERT INTO company_skills (company_id, skill_id, required_level) VALUES (?, ?, ?)";
                        try (PreparedStatement ps = c.prepareStatement(ins)) { ps.setInt(1, companyId); ps.setInt(2, skillId); ps.setInt(3, e.getValue()); ps.executeUpdate(); }
                    }
                }
            }
            c.commit(); c.setAutoCommit(true); return true;
        } catch (SQLException e) { e.printStackTrace(); try { if (c != null) { c.rollback(); c.setAutoCommit(true); } } catch (SQLException ex) {} return false; }
        finally { try { if (c!=null) c.close(); } catch (SQLException ex) {} }
    }

    /** Delete a company by id. */
    public boolean deleteCompany(int companyId) {
        String sql = "DELETE FROM companies WHERE company_id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, companyId); int rows = ps.executeUpdate(); return rows > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Determine eligible companies for a student based on CGPA and skills.
     * A small optimization: if CGPA ≥ 9.0, show all companies at once.
     */
    public List<Map<String,Object>> getEligibleCompanies(double cgpa, Map<String,Integer> studentSkills) {
        List<Map<String,Object>> eligible = new ArrayList<>();
        if (cgpa >= 9.0) {
            String sql = "SELECT DISTINCT c.company_id, c.company_name, c.application_link, "
                    + "GROUP_CONCAT(CONCAT(s.skill_name, '(', cs.required_level, '★)') SEPARATOR ', ') as required_skills "
                    + "FROM companies c LEFT JOIN company_skills cs ON c.company_id = cs.company_id LEFT JOIN skills s ON cs.skill_id = s.skill_id "
                    + "GROUP BY c.company_id, c.company_name, c.application_link ORDER BY c.company_name";
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> comp = new HashMap<>(); comp.put("id", rs.getInt("company_id")); comp.put("name", rs.getString("company_name")); comp.put("link", rs.getString("application_link")); comp.put("skills", rs.getString("required_skills")); eligible.add(comp);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            return eligible;
        }

        if (cgpa >= 7.0) {
            // Only companies whose required CGPA the student meets, then filter by skills in Java.
            String sql = "SELECT c.company_id, c.company_name, c.application_link, c.required_cgpa, s.skill_name, cs.required_level "
                    + "FROM companies c INNER JOIN company_skills cs ON c.company_id = cs.company_id INNER JOIN skills s ON cs.skill_id = s.skill_id "
                    + "WHERE c.required_cgpa IS NULL OR c.required_cgpa <= ? "
                    + "ORDER BY c.company_id";
            Map<Integer,Map<String,Object>> cmap = new HashMap<>();
            Map<Integer,List<Map<String,String>>> cmapSkills = new HashMap<>();
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDouble(1, cgpa);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int cid = rs.getInt("company_id");
                        if (!cmap.containsKey(cid)) { Map<String,Object> comp = new HashMap<>(); comp.put("id", cid); comp.put("name", rs.getString("company_name")); comp.put("link", rs.getString("application_link")); comp.put("skills", ""); cmap.put(cid, comp); cmapSkills.put(cid, new ArrayList<>()); }
                        Map<String,String> req = new HashMap<>(); req.put("skill", rs.getString("skill_name")); req.put("level", String.valueOf(rs.getInt("required_level"))); cmapSkills.get(cid).add(req);
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); return eligible; }

            for (Map.Entry<Integer,Map<String,Object>> entry : cmap.entrySet()) {
                int cid = entry.getKey(); boolean isEligible = true; StringBuilder sbs = new StringBuilder();
                for (Map<String,String> req : cmapSkills.get(cid)) {
                    String skillName = req.get("skill"); int requiredLevel = Integer.parseInt(req.get("level")); int studentLevel = studentSkills.getOrDefault(skillName, 0);
                    if (sbs.length()>0) sbs.append(", "); sbs.append(skillName).append("(").append(requiredLevel).append("★)");
                    if (studentLevel < requiredLevel) { isEligible = false; break; }
                }
                if (isEligible) { Map<String,Object> comp = entry.getValue(); comp.put("skills", sbs.toString()); eligible.add(comp); }
            }
        }
        return eligible;
    }

    /** Get or create the id for a skill name. */
    private int getOrCreateSkillId(Connection conn, String skillName) throws SQLException {
        String sel = "SELECT skill_id FROM skills WHERE skill_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, skillName);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt("skill_id"); }
        }
        String ins = "INSERT INTO skills (skill_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, skillName); ps.executeUpdate(); try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        }
        return -1;
    }

    /** Fetch the student_id for an email, or -1 if not found. */
    public int getStudentIdByEmail(String email) {
        String sql = "SELECT student_id FROM students WHERE LOWER(email) = LOWER(?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("student_id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // Eligibility check logic: a student is eligible for a company when their CGPA
    // meets the required CGPA AND they meet every required skill level. This mirrors
    // the /api/eligible branch so /api/eligibility/check and /api/eligible agree.
    public boolean checkStudentEligibility(int studentId, int companyId) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "SELECT s.cgpa, c.required_cgpa FROM students s, companies c WHERE s.student_id = ? AND c.company_id = ?")) {
            ps.setInt(1, studentId);
            ps.setInt(2, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object cgpaObj = rs.getObject(1);
                    Double studentCgpa = cgpaObj != null ? ((Number) cgpaObj).doubleValue() : null;
                    Object reqCgpaObj = rs.getObject(2);
                    Double requiredCgpa = reqCgpaObj != null ? ((Number) reqCgpaObj).doubleValue() : null;
                    if (requiredCgpa != null && (studentCgpa == null || studentCgpa < requiredCgpa)) return false;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }

        // Verify every required skill is met by the student.
        String skillSql = "SELECT cs.required_level, ss.skill_level FROM company_skills cs "
                + "LEFT JOIN student_skills ss ON cs.skill_id = ss.skill_id AND ss.student_id = ? "
                + "WHERE cs.company_id = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(skillSql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int requiredLevel = rs.getInt("required_level");
                    int studentLevel = rs.getObject("skill_level") != null ? rs.getInt("skill_level") : 0;
                    if (studentLevel < requiredLevel) return false;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
        return true;
    }

    /**
     * Short reason why a student isn’t eligible, or null if eligible.
     */
    public String getEligibilityReason(int studentId, int companyId) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(
                "SELECT s.cgpa, s.backlogs, c.required_cgpa FROM students s, companies c WHERE s.student_id = ? AND c.company_id = ?")) {
            ps.setInt(1, studentId);
            ps.setInt(2, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Object cgpaObj = rs.getObject(1);
                    Double studentCgpa = cgpaObj != null ? ((Number) cgpaObj).doubleValue() : null;
                    int backlogs = rs.getInt(2);
                    Object reqCgpaObj = rs.getObject(3);
                    Double requiredCgpa = reqCgpaObj != null ? ((Number) reqCgpaObj).doubleValue() : null;
                    
                    if (studentCgpa == null) return "Profile incomplete: CGPA not set";
                    if (backlogs > 0) return "Student has " + backlogs + " backlogs";
                    if (requiredCgpa != null && studentCgpa < requiredCgpa) 
                        return "CGPA " + studentCgpa + " is below required " + requiredCgpa;
                    // CGPA/backlogs fine — check for missing required skills.
                    String skillSql = "SELECT s.skill_name, cs.required_level, ss.skill_level FROM company_skills cs "
                            + "JOIN skills s ON cs.skill_id = s.skill_id "
                            + "LEFT JOIN student_skills ss ON cs.skill_id = ss.skill_id AND ss.student_id = ? "
                            + "WHERE cs.company_id = ?";
                    try (PreparedStatement sp = c.prepareStatement(skillSql)) {
                        sp.setInt(1, studentId);
                        sp.setInt(2, companyId);
                        try (ResultSet sr = sp.executeQuery()) {
                            java.util.List<String> missing = new ArrayList<>();
                            while (sr.next()) {
                                int requiredLevel = sr.getInt("required_level");
                                int studentLevel = sr.getObject("skill_level") != null ? sr.getInt("skill_level") : 0;
                                if (studentLevel < requiredLevel) {
                                    missing.add(sr.getString("skill_name") + " (" + studentLevel + "/" + requiredLevel + ")");
                                }
                            }
                            if (!missing.isEmpty()) return "Missing required skills: " + String.join(", ", missing);
                        }
                    }
                    return null;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown error";
    }

    /** Replace any previous result for (student, company) with the latest one. */
    public void saveEligibilityResult(int studentId, String studentName, int companyId, String companyName, boolean isEligible, String reason) {
        try (Connection c = open()) {
            // First delete existing result for this student-company pair
            String deleteSql = "DELETE FROM eligibility_results WHERE student_id = ? AND company_id = ?";
            try (PreparedStatement deletePs = c.prepareStatement(deleteSql)) {
                deletePs.setInt(1, studentId);
                deletePs.setInt(2, companyId);
                deletePs.executeUpdate();
            }
            
            // Then insert the new result
            String insertSql = "INSERT INTO eligibility_results (student_id, student_name, company_id, company_name, is_eligible, reason_if_not_eligible) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertPs = c.prepareStatement(insertSql)) {
                insertPs.setInt(1, studentId);
                insertPs.setString(2, studentName);
                insertPs.setInt(3, companyId);
                insertPs.setString(4, companyName);
                insertPs.setBoolean(5, isEligible);
                insertPs.setString(6, reason);
                insertPs.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Query eligibility results with optional filters. */
    public List<Map<String,Object>> getEligibilityResults(Integer studentId, Integer companyId, Boolean isEligible) {
        List<Map<String,Object>> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT er.*, s.student_id_number, s.department, c.application_link FROM eligibility_results er " +
            "LEFT JOIN students s ON er.student_id = s.student_id " +
            "LEFT JOIN companies c ON er.company_id = c.company_id WHERE 1=1"
        );
        if (studentId != null) sql.append(" AND er.student_id = ?");
        if (companyId != null) sql.append(" AND er.company_id = ?");
        if (isEligible != null) sql.append(" AND er.is_eligible = ?");
        sql.append(" ORDER BY er.timestamp DESC");
        
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (studentId != null) ps.setInt(idx++, studentId);
            if (companyId != null) ps.setInt(idx++, companyId);
            if (isEligible != null) ps.setBoolean(idx++, isEligible);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String,Object> row = new HashMap<>();
                    row.put("result_id", rs.getInt("result_id"));
                    row.put("student_id", rs.getInt("student_id"));
                    row.put("student_name", rs.getString("student_name"));
                    row.put("student_idnumber", rs.getString("student_id_number"));
                    row.put("student_department", rs.getString("department"));
                    row.put("company_id", rs.getInt("company_id"));
                    row.put("company_name", rs.getString("company_name"));
                    row.put("company_link", rs.getString("application_link"));
                    row.put("is_eligible", rs.getBoolean("is_eligible"));
                    row.put("reason", rs.getString("reason_if_not_eligible"));
                    row.put("timestamp", rs.getTimestamp("timestamp").toString());
                    results.add(row);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return results;
    }

    /** Compute and persist eligibility for all companies for a student. */
    public void checkAndSaveEligibilityForStudent(int studentId, String email) {
        // Get student info
        Map<String,Object> profile = getStudentProfile(email);
        String studentName = (String) profile.getOrDefault("name", "Unknown");
        
        // Get all companies
        List<Map<String,Object>> companies = getCompanies();
        
        // Check eligibility for each company
        for (Map<String,Object> company : companies) {
            int companyId = ((Number) company.get("id")).intValue();
            String companyName = (String) company.get("name");
            
            boolean isEligible = checkStudentEligibility(studentId, companyId);
            String reason = isEligible ? null : getEligibilityReason(studentId, companyId);
            
            saveEligibilityResult(studentId, studentName, companyId, companyName, isEligible, reason);
        }
    }

    // --- New helper: check ID number availability ---
    public boolean isIdNumberAvailable(String idNumber, String email) {
        if (idNumber == null || idNumber.isEmpty()) return false; // treated as required upstream
        String sql = "SELECT email FROM students WHERE student_id_number = ? LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, idNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return true; // not used
                String existingEmail = rs.getString("email");
                return existingEmail.equalsIgnoreCase(email); // same owner is fine
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============== PASSWORD RESET FUNCTIONALITY ==============
    
    /**
     * Generate and store a password reset token for the given email.
     * Returns the generated token string, or null if email doesn't exist.
     * Token expires in 1 hour.
     */
    public String generatePasswordResetToken(String email) {
        // First verify email exists in students table
        if (!emailExists(email)) {
            return null;
        }
        
        // Generate secure random token (32 bytes)
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        // Calculate expiry time (1 hour from now)
        long expiryMillis = System.currentTimeMillis() + (60 * 60 * 1000);
        java.sql.Timestamp expiresAt = new java.sql.Timestamp(expiryMillis);
        
        // Store token in database
        String sql = "INSERT INTO password_reset_tokens (email, reset_token, expires_at) VALUES (?, ?, ?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
            return token;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Verify if a reset token is valid (exists, not expired, not used).
     * Returns the associated email if valid, null otherwise.
     */
    public String verifyResetToken(String token) {
        String sql = "SELECT email, expires_at, used FROM password_reset_tokens WHERE reset_token = ? LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // Token doesn't exist
                }
                
                String email = rs.getString("email");
                java.sql.Timestamp expiresAt = rs.getTimestamp("expires_at");
                boolean used = rs.getBoolean("used");
                
                // Check if token is expired
                if (System.currentTimeMillis() > expiresAt.getTime()) {
                    return null; // Token expired
                }
                
                // Check if token was already used
                if (used) {
                    return null; // Token already used
                }
                
                return email;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Reset password using a valid token.
     * Returns true if successful, false if token invalid or password update failed.
     */
    public boolean resetPasswordWithToken(String token, String newPassword) {
        // Validate inputs
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        
        // Verify token and get email
        String email = verifyResetToken(token);
        if (email == null) {
            return false; // Invalid or expired token
        }
        
        // Hash new password
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        
        try (Connection c = open()) {
            // Update password
            String updateSql = "UPDATE students SET password = ? WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                ps.setString(1, hashedPassword);
                ps.setString(2, email);
                int rows = ps.executeUpdate();
                
                if (rows == 0) {
                    return false; // No student found with that email
                }
            }
            
            // Mark token as used
            String markUsedSql = "UPDATE password_reset_tokens SET used = TRUE WHERE reset_token = ?";
            try (PreparedStatement ps = c.prepareStatement(markUsedSql)) {
                ps.setString(1, token);
                ps.executeUpdate();
            }
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if an email exists in the students table.
     */
    private boolean emailExists(String email) {
        String sql = "SELECT email FROM students WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cleanup expired and used reset tokens (maintenance method).
     * Should be called periodically to keep database clean.
     */
    public int cleanupExpiredResetTokens() {
        String sql = "DELETE FROM password_reset_tokens WHERE expires_at < NOW() OR used = TRUE";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
