package com.placement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Simple OOP Database helper. Each method opens its own Connection and closes it.
public class Database {
    private String dbUrl;
    private String dbUser;
    private String dbPass;

    public Database() {
        this.dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/JAVAPROJECT");
        this.dbUser = System.getenv().getOrDefault("DB_USER", "root");
        this.dbPass = System.getenv().getOrDefault("DB_PASS", "root");
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    public boolean register(String email, String password, String name) {
        String sql = "INSERT INTO students (student_name, student_id_number, email, password, department, degree) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            String studentId = "STU" + (System.currentTimeMillis() % 100000);
            ps.setString(1, name);
            ps.setString(2, studentId);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setString(5, "");
            ps.setString(6, "B.Tech");
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String email, String password, String role) {
        String table = "students";
        if ("admin".equalsIgnoreCase(role)) table = "admin";
        String sql = "SELECT 1 FROM " + table + " WHERE email = ? AND password = ? LIMIT 1";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveProfile(String email, String name, String idNumber, String department,
                               String degree, String collegeName, String phone, Double cgpa,
                               Map<String,Integer> skills) {
        Connection c = null;
        try {
            c = open();
            c.setAutoCommit(false);
            String updateSql = "UPDATE students SET student_name=?, student_id_number=?, department=?, degree=?, college_name=?, phone_number=?, cgpa=? WHERE email=?";
            try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                ps.setString(1, name);
                ps.setString(2, idNumber);
                ps.setString(3, department);
                ps.setString(4, degree);
                ps.setString(5, collegeName);
                ps.setString(6, phone);
                ps.setDouble(7, cgpa != null ? cgpa : 0.0);
                ps.setString(8, email);
                ps.executeUpdate();
            }

            String deleteSkills = "DELETE FROM student_skills WHERE student_id = (SELECT student_id FROM students WHERE email = ?)";
            try (PreparedStatement ps = c.prepareStatement(deleteSkills)) {
                ps.setString(1, email);
                ps.executeUpdate();
            }

            if (skills != null && !skills.isEmpty()) {
                for (Map.Entry<String,Integer> e : skills.entrySet()) {
                    int skillId = getOrCreateSkillId(c, e.getKey());
                    if (skillId > 0) {
                        String insert = "INSERT INTO student_skills (student_id, skill_id, skill_level) VALUES ((SELECT student_id FROM students WHERE email = ?), ?, ?)";
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
            try { if (c != null) c.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { if (c != null && !c.isClosed()) c.close(); } catch (SQLException ex) {}
        }
    }

    public Map<String,Object> getStudentProfile(String email) {
        Map<String,Object> profile = new HashMap<>();
        String sql = "SELECT student_id, student_name, student_id_number, email, department, degree, cgpa, college_name, phone_number FROM students WHERE email = ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile.put("name", rs.getString("student_name"));
                    profile.put("idNumber", rs.getString("student_id_number"));
                    profile.put("email", rs.getString("email"));
                    profile.put("department", rs.getString("department"));
                    profile.put("degree", rs.getString("degree"));
                    Object cgpaObj = rs.getObject("cgpa");
                    profile.put("cgpa", cgpaObj != null ? ((Number)cgpaObj).doubleValue() : null);
                    profile.put("collegeName", rs.getString("college_name"));
                    profile.put("phone", rs.getString("phone_number"));

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
        } catch (SQLException e) { e.printStackTrace(); try { if (c!=null) c.rollback(); } catch (SQLException ex) {} return false; }
        finally { try { if (c!=null) c.close(); } catch (SQLException ex) {} }
    }

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
        } catch (SQLException e) { e.printStackTrace(); try { if (c!=null) c.rollback(); } catch (SQLException ex) {} return false; }
        finally { try { if (c!=null) c.close(); } catch (SQLException ex) {} }
    }

    public boolean deleteCompany(int companyId) {
        String sql = "DELETE FROM companies WHERE company_id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, companyId); int rows = ps.executeUpdate(); return rows > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

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
            String sql = "SELECT c.company_id, c.company_name, c.application_link, s.skill_name, cs.required_level "
                    + "FROM companies c INNER JOIN company_skills cs ON c.company_id = cs.company_id INNER JOIN skills s ON cs.skill_id = s.skill_id ORDER BY c.company_id";
            Map<Integer,Map<String,Object>> cmap = new HashMap<>();
            Map<Integer,List<Map<String,String>>> cmapSkills = new HashMap<>();
            try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int cid = rs.getInt("company_id");
                    if (!cmap.containsKey(cid)) { Map<String,Object> comp = new HashMap<>(); comp.put("id", cid); comp.put("name", rs.getString("company_name")); comp.put("link", rs.getString("application_link")); comp.put("skills", ""); cmap.put(cid, comp); cmapSkills.put(cid, new ArrayList<>()); }
                    Map<String,String> req = new HashMap<>(); req.put("skill", rs.getString("skill_name")); req.put("level", String.valueOf(rs.getInt("required_level"))); cmapSkills.get(cid).add(req);
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
}
