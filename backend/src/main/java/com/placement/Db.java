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

public class Db {
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/JAVAPROJECT");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASS = System.getenv().getOrDefault("DB_PASS", "root");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Student Registration
    public static boolean registerStudent(String name, String idNumber, String email, String password, 
                                         String department, String degree) {
        String sql = "INSERT INTO students (student_name, student_id_number, email, password, department, degree) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, idNumber);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setString(5, department);
            ps.setString(6, degree);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Student Login
    public static boolean validateStudentLogin(String email, String password) {
        String sql = "SELECT 1 FROM students WHERE email = ? AND password = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // Admin Login
    public static boolean validateAdminLogin(String email, String password) {
        String sql = "SELECT 1 FROM admin WHERE email = ? AND password = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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

    // Get or create skill ID
    public static int getOrCreateSkillId(String skillName) {
        String select = "SELECT skill_id FROM skills WHERE skill_name = ?";
        String insert = "INSERT INTO skills (skill_name) VALUES (?)";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, skillName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("skill_id");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, skillName);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Save Student Profile with Skills
    public static boolean saveStudentProfile(String email, String name, String idNumber, String department, 
                                            String degree, String collegeName, String phone, Double cgpa,
                                            Map<String, Integer> skills) {
        String updateStudent = "UPDATE students SET student_name=?, student_id_number=?, department=?, degree=?, college_name=?, phone_number=?, cgpa=? WHERE email=?";
        String deleteSkills = "DELETE FROM student_skills WHERE student_id = (SELECT student_id FROM students WHERE email = ?)";
        String insertSkill = "INSERT INTO student_skills (student_id, skill_id, skill_level) VALUES ((SELECT student_id FROM students WHERE email = ?), ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update student info
                try (PreparedStatement ps = conn.prepareStatement(updateStudent)) {
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
                
                // Delete old skills
                try (PreparedStatement ps = conn.prepareStatement(deleteSkills)) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                
                // Insert new skills
                for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                    int skillId = getOrCreateSkillId(entry.getKey());
                    if (skillId > 0) {
                        try (PreparedStatement ps = conn.prepareStatement(insertSkill)) {
                            ps.setString(1, email);
                            ps.setInt(2, skillId);
                            ps.setInt(3, entry.getValue());
                            ps.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get All Companies
    public static List<Map<String, Object>> getCompanies() {
        String sql = "SELECT c.company_id, c.company_name, c.application_link, c.required_cgpa, " +
                     "GROUP_CONCAT(CONCAT(s.skill_name, '(' , cs.required_level, '★)') SEPARATOR ', ') as required_skills " +
                     "FROM companies c " +
                     "LEFT JOIN company_skills cs ON c.company_id = cs.company_id " +
                     "LEFT JOIN skills s ON cs.skill_id = s.skill_id " +
                     "GROUP BY c.company_id, c.company_name, c.application_link, c.required_cgpa " +
                     "ORDER BY c.company_name";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("company_id"));
                m.put("name", rs.getString("company_name"));
                m.put("link", rs.getString("application_link"));
                m.put("skills", rs.getString("required_skills"));
                Object cgpaObj = rs.getObject("required_cgpa");
                if (cgpaObj != null) {
                    if (cgpaObj instanceof Number) {
                        m.put("requiredCgpa", ((Number) cgpaObj).doubleValue());
                    } else {
                        try {
                            m.put("requiredCgpa", Double.parseDouble(cgpaObj.toString()));
                        } catch (Exception ignored) {
                            m.put("requiredCgpa", null);
                        }
                    }
                } else {
                    m.put("requiredCgpa", null);
                }
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Add Company with Skills
    public static boolean addCompany(String name, String link, double requiredCgpa, Map<String, Integer> requiredSkills) {
        String insertCompany = "INSERT INTO companies (company_name, application_link, required_cgpa) VALUES (?, ?, ?)";
        String insertSkill = "INSERT INTO company_skills (company_id, skill_id, required_level) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int companyId = -1;
                try (PreparedStatement ps = conn.prepareStatement(insertCompany, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, link);
                    // Allow NULL for requiredCgpa if negative sentinel passed
                    if (Double.isFinite(requiredCgpa) && requiredCgpa >= 0) {
                        ps.setDouble(3, requiredCgpa);
                    } else {
                        ps.setNull(3, java.sql.Types.DECIMAL);
                    }
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            companyId = rs.getInt(1);
                        }
                    }
                }
                
                if (companyId > 0 && requiredSkills != null) {
                    for (Map.Entry<String, Integer> entry : requiredSkills.entrySet()) {
                        int skillId = getOrCreateSkillId(entry.getKey());
                        if (skillId > 0) {
                            try (PreparedStatement ps = conn.prepareStatement(insertSkill)) {
                                ps.setInt(1, companyId);
                                ps.setInt(2, skillId);
                                ps.setInt(3, entry.getValue());
                                ps.executeUpdate();
                            }
                        }
                    }
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get Eligible Companies based on matching rules
    public static List<Map<String, Object>> getEligibleCompanies(double cgpa, Map<String, Integer> studentSkills) {
        List<Map<String, Object>> eligible = new ArrayList<>();
        
        // Rule 1: If CGPA >= 9.0, show all companies
        if (cgpa >= 9.0) {
            String sql = "SELECT DISTINCT c.company_id, c.company_name, c.application_link, " +
                         "GROUP_CONCAT(CONCAT(s.skill_name, '(', cs.required_level, '★)') SEPARATOR ', ') as required_skills " +
                         "FROM companies c " +
                         "LEFT JOIN company_skills cs ON c.company_id = cs.company_id " +
                         "LEFT JOIN skills s ON cs.skill_id = s.skill_id " +
                         "GROUP BY c.company_id, c.company_name, c.application_link " +
                         "ORDER BY c.company_name";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("company_id"));
                    m.put("name", rs.getString("company_name"));
                    m.put("link", rs.getString("application_link"));
                    m.put("skills", rs.getString("required_skills"));
                    eligible.add(m);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return eligible;
        }
        
        // Rule 2: If CGPA >= 7.0, show companies with matching skills
        if (cgpa >= 7.0) {
            // Get all companies and filter in Java
            String allCompaniesSql = "SELECT c.company_id, c.company_name, c.application_link, " +
                                     "s.skill_name, cs.required_level " +
                                     "FROM companies c " +
                                     "INNER JOIN company_skills cs ON c.company_id = cs.company_id " +
                                     "INNER JOIN skills s ON cs.skill_id = s.skill_id " +
                                     "ORDER BY c.company_id";
            
            Map<Integer, Map<String, Object>> companyMap = new HashMap<>();
            Map<Integer, List<Map<String, String>>> companySkillsMap = new HashMap<>();
            
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(allCompaniesSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int companyId = rs.getInt("company_id");
                    if (!companyMap.containsKey(companyId)) {
                        Map<String, Object> comp = new HashMap<>();
                        comp.put("id", companyId);
                        comp.put("name", rs.getString("company_name"));
                        comp.put("link", rs.getString("application_link"));
                        comp.put("skills", "");
                        companyMap.put(companyId, comp);
                        companySkillsMap.put(companyId, new ArrayList<>());
                    }
                    Map<String, String> skillReq = new HashMap<>();
                    skillReq.put("skill", rs.getString("skill_name"));
                    skillReq.put("level", String.valueOf(rs.getInt("required_level")));
                    companySkillsMap.get(companyId).add(skillReq);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Filter companies where student meets all skill requirements
            for (Map.Entry<Integer, Map<String, Object>> entry : companyMap.entrySet()) {
                int companyId = entry.getKey();
                List<Map<String, String>> requiredSkills = companySkillsMap.get(companyId);
                boolean isEligible = true;
                StringBuilder skillsStr = new StringBuilder();
                
                for (Map<String, String> req : requiredSkills) {
                    String skillName = req.get("skill");
                    int requiredLevel = Integer.parseInt(req.get("level"));
                    int studentLevel = studentSkills.getOrDefault(skillName, 0);
                    
                    if (skillsStr.length() > 0) skillsStr.append(", ");
                    skillsStr.append(skillName).append("(").append(requiredLevel).append("★)");
                    
                    if (studentLevel < requiredLevel) {
                        isEligible = false;
                        break;
                    }
                }
                
                if (isEligible) {
                    Map<String, Object> comp = entry.getValue();
                    comp.put("skills", skillsStr.toString());
                    eligible.add(comp);
                }
            }
        }
        
        return eligible;
    }

    // Update Company with Skills
    public static boolean updateCompany(int companyId, String name, String link, double requiredCgpa, Map<String, Integer> requiredSkills) {
        String updateCompany = "UPDATE companies SET company_name=?, application_link=?, required_cgpa=? WHERE company_id=?";
        String deleteSkills = "DELETE FROM company_skills WHERE company_id=?";
        String insertSkill = "INSERT INTO company_skills (company_id, skill_id, required_level) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update company info
                try (PreparedStatement ps = conn.prepareStatement(updateCompany)) {
                    ps.setString(1, name);
                    ps.setString(2, link);
                    if (Double.isFinite(requiredCgpa) && requiredCgpa >= 0) {
                        ps.setDouble(3, requiredCgpa);
                    } else {
                        ps.setNull(3, java.sql.Types.DECIMAL);
                    }
                    ps.setInt(4, companyId);
                    ps.executeUpdate();
                }
                
                // Delete old skills
                try (PreparedStatement ps = conn.prepareStatement(deleteSkills)) {
                    ps.setInt(1, companyId);
                    ps.executeUpdate();
                }
                
                // Insert new skills
                if (requiredSkills != null) {
                    for (Map.Entry<String, Integer> entry : requiredSkills.entrySet()) {
                        int skillId = getOrCreateSkillId(entry.getKey());
                        if (skillId > 0) {
                            try (PreparedStatement ps = conn.prepareStatement(insertSkill)) {
                                ps.setInt(1, companyId);
                                ps.setInt(2, skillId);
                                ps.setInt(3, entry.getValue());
                                ps.executeUpdate();
                            }
                        }
                    }
                }
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Delete Company
    public static boolean deleteCompany(int companyId) {
        String sql = "DELETE FROM companies WHERE company_id=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
