package com.placement;

import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class App {
    private static final Gson gson = new Gson();
    
    // Error Messages
    private static final String ERROR_DB_FAIL = "Database operation failed";
    private static final String ERROR_INVALID_INPUT = "Invalid input provided";
    private static final String ERROR_SERVER = "Server error";
    private static final String ERROR_UNAUTHORIZED = "Invalid credentials";

    public static void main(String[] args) {
        port(getPort());
        enableCORS();

        // Health check
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });

        // Student Registration
        post("/api/register", (req, res) -> {
            res.type("application/json");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                String email = body.containsKey("email") ? (String) body.get("email") : "";
                String password = body.containsKey("password") ? (String) body.get("password") : "";
                
                if (email.isEmpty() || password.isEmpty()) {
                    res.status(400);
                    return "{\"error\":\"Email and password are required\"}";
                }
                
                // Use email as default name and generate a default ID
                String name = email.split("@")[0]; // Use part before @ as default name
                String idNumber = "STU" + System.currentTimeMillis() % 100000; // Generate a simple ID
                String department = "";
                String degree = "B.Tech"; // Default degree
                
                boolean ok = Db.registerStudent(name, idNumber, email, password, department, degree);
                if (!ok) {
                    res.status(400);
                    return "{\"error\":\"Registration failed. Email may already exist.\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"Server error\"}";
            }
        });

        // Student Login
        post("/api/login", (req, res) -> {
            res.type("application/json");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                String email = body.containsKey("email") ? (String) body.get("email") : "";
                String password = body.containsKey("password") ? (String) body.get("password") : "";
                String role = body.containsKey("role") ? (String) body.get("role") : "student";
                
                boolean ok = false;
                if ("admin".equals(role)) {
                    ok = Db.validateAdminLogin(email, password);
                } else {
                    ok = Db.validateStudentLogin(email, password);
                }
                
                if (!ok) {
                    res.status(401);
                    return "{\"error\":\"Invalid credentials\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"Server error\"}";
            }
        });

        // Save Student Profile
        post("/api/student/profile", (req, res) -> {
            res.type("application/json");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                String email = body.containsKey("email") ? (String) body.get("email") : "";
                String name = body.containsKey("name") ? (String) body.get("name") : "";
                String idNumber = body.containsKey("idNumber") ? (String) body.get("idNumber") : "";
                String department = body.containsKey("department") ? (String) body.get("department") : "";
                String degree = body.containsKey("degree") ? (String) body.get("degree") : "";
                String collegeName = body.containsKey("collegeName") ? (String) body.get("collegeName") : "";
                String phone = body.containsKey("phone") ? (String) body.get("phone") : "";
                Double cgpa = null;
                if (body.get("cgpa") != null) {
                    if (body.get("cgpa") instanceof Number) {
                        cgpa = ((Number) body.get("cgpa")).doubleValue();
                    } else {
                        cgpa = Double.parseDouble(body.get("cgpa").toString());
                    }
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> skillsObj = (Map<String, Object>) body.get("skills");
                Map<String, Integer> skills = new HashMap<>();
                if (skillsObj != null) {
                    for (Map.Entry<String, Object> entry : skillsObj.entrySet()) {
                        Object val = entry.getValue();
                        int level = 0;
                        if (val instanceof Number) {
                            level = ((Number) val).intValue();
                        } else {
                            level = Integer.parseInt(val.toString());
                        }
                        skills.put(entry.getKey(), level);
                    }
                }
                
                boolean ok = Db.saveStudentProfile(email, name, idNumber, department, degree, collegeName, phone, cgpa, skills);
                if (!ok) {
                    res.status(400);
                    return "{\"error\":\"Could not save profile\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"Server error\"}";
            }
        });

        // Get Companies (for admin dashboard)
        get("/api/companies", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Db.getCompanies());
        });

        // Add Company (admin)
        post("/api/companies", (req, res) -> {
            res.type("application/json");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                String name = body.containsKey("name") ? (String) body.get("name") : "";
                String link = body.containsKey("link") ? (String) body.get("link") : "";
                double requiredCgpa = -1;
                if (body.containsKey("requiredCgpa")) {
                    Object cgObj = body.get("requiredCgpa");
                    try {
                        if (cgObj instanceof Number) requiredCgpa = ((Number) cgObj).doubleValue();
                        else requiredCgpa = Double.parseDouble(cgObj.toString());
                    } catch (Exception ignored) { requiredCgpa = -1; }
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> skillsObj = (Map<String, Object>) body.get("skills");
                Map<String, Integer> requiredSkills = new HashMap<>();
                if (skillsObj != null) {
                    for (Map.Entry<String, Object> entry : skillsObj.entrySet()) {
                        Object val = entry.getValue();
                        int level = 0;
                        if (val instanceof Number) {
                            level = ((Number) val).intValue();
                        } else {
                            level = Integer.parseInt(val.toString());
                        }
                        requiredSkills.put(entry.getKey(), level);
                    }
                }
                
                boolean ok = Db.addCompany(name, link, requiredCgpa, requiredSkills);
                if (!ok) {
                    res.status(400);
                    return "{\"error\":\"Could not save company - database operation failed\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                System.err.println("Error saving company: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // Get Eligible Companies (for student)
        get("/api/eligible", (req, res) -> {
            res.type("application/json");
            try {
                double cgpa = Double.parseDouble(req.queryParams("cgpa"));
                String skillsParam = Optional.ofNullable(req.queryParams("skills")).orElse("");
                Map<String, Integer> skillRatings = parseSkills(skillsParam);
                return gson.toJson(Db.getEligibleCompanies(cgpa, skillRatings));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(400);
                return "{\"error\":\"Invalid parameters\"}";
            }
        });

        // Update Company (admin)
        put("/api/companies/:id", (req, res) -> {
            res.type("application/json");
            try {
                int companyId = Integer.parseInt(req.params(":id"));
                @SuppressWarnings("unchecked")
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);
                String name = body.containsKey("name") ? (String) body.get("name") : "";
                String link = body.containsKey("link") ? (String) body.get("link") : "";
                double requiredCgpa = -1;
                if (body.containsKey("requiredCgpa")) {
                    Object cgObj = body.get("requiredCgpa");
                    try {
                        if (cgObj instanceof Number) requiredCgpa = ((Number) cgObj).doubleValue();
                        else requiredCgpa = Double.parseDouble(cgObj.toString());
                    } catch (Exception ignored) { requiredCgpa = -1; }
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> skillsObj = (Map<String, Object>) body.get("skills");
                Map<String, Integer> requiredSkills = new HashMap<>();
                if (skillsObj != null) {
                    for (Map.Entry<String, Object> entry : skillsObj.entrySet()) {
                        Object val = entry.getValue();
                        int level = 0;
                        if (val instanceof Number) {
                            level = ((Number) val).intValue();
                        } else {
                            level = Integer.parseInt(val.toString());
                        }
                        requiredSkills.put(entry.getKey(), level);
                    }
                }
                
                boolean ok = Db.updateCompany(companyId, name, link, requiredCgpa, requiredSkills);
                if (!ok) {
                    res.status(400);
                    return "{\"error\":\"Could not update company\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"Server error\"}";
            }
        });

        // Delete Company (admin)
        delete("/api/companies/:id", (req, res) -> {
            res.type("application/json");
            try {
                int companyId = Integer.parseInt(req.params(":id"));
                boolean ok = Db.deleteCompany(companyId);
                if (!ok) {
                    res.status(400);
                    return "{\"error\":\"Could not delete company\"}";
                }
                return "{\"success\":true}";
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "{\"error\":\"Server error\"}";
            }
        });
    }

    private static int getPort() {
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) {
            return Integer.parseInt(env);
        }
        return 8080;
    }

    private static void enableCORS() {
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });
        options("/*", (req, res) -> {
            res.status(200);
            return "OK";
        });
    }

    // Parse skills format: Java:3,Python:2
    private static Map<String, Integer> parseSkills(String param) {
        Map<String, Integer> result = new HashMap<>();
        if (param == null || param.isBlank()) return result;
        String[] pairs = param.split(",");
        for (String p : pairs) {
            String[] kv = p.split(":");
            if (kv.length == 2) {
                try {
                    result.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}
