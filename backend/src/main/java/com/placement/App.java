package com.placement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.placement.models.Company;
import com.placement.models.Student;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Minimal HTTP API for the placement system using the built-in {@code HttpServer}.
 *
 * Routes are under the {@code /api} prefix and return JSON. This class keeps
 * things straightforward on purpose: no frameworks, only small helpers for
 * reading bodies and writing responses.
 */
public class App {
    // Simple opaque token store: token -> TokenData mapping
    private static final ConcurrentHashMap<String, TokenData> tokenStore = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final long TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    // Token data holder
    static class TokenData {
        String email;
        String role;
        long expiresAt;
        
        TokenData(String email, String role, long expiresAt) {
            this.email = email;
            this.role = role;
            this.expiresAt = expiresAt;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    /**
     * Generate a secure random token.
     */
    private static String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Extract and validate Bearer token from Authorization header.
     * Returns TokenData if valid, null otherwise.
     */
    private static TokenData requireAuth(HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        
        String token = auth.substring(7);
        TokenData data = tokenStore.get(token);
        
        if (data == null || data.isExpired()) {
            if (data != null) tokenStore.remove(token); // Clean up expired
            return null;
        }
        
        return data;
    }
    
    /**
     * Validate email format (basic).
     */
    static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Validate password strength.
     */
    static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================");
        System.out.println("  STUDENT PLACEMENT SYSTEM - BACKEND SERVER");
        System.out.println("================================================================");
        System.out.println();
        
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try { port = Integer.parseInt(envPort); } catch (Exception e) {}
        }

        System.out.println("Initializing HTTP Server on port " + port + "...");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        System.out.println("Initializing Database connection...");
        Database db = new Database();
        db.validateConnection();

        System.out.println("Registering API endpoints...");
        // Health endpoint for quick readiness checks
        server.createContext("/api/health", new SimpleHandler((ex) -> sendText(ex, 200, "{\"status\":\"ok\"}")));

        // Create a student account. Expects JSON: { email, password }
        server.createContext("/api/register", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String email = data.getOrDefault("email", "");
            String password = data.getOrDefault("password", "");
            
            // Input validation
            if (email.isEmpty() || password.isEmpty()) {
                return sendText(ex, 400, "{\"error\":\"Email and password are required\"}");
            }
            if (!isValidEmail(email)) {
                return sendText(ex, 400, "{\"error\":\"Invalid email format\"}");
            }
            if (!isValidPassword(password)) {
                return sendText(ex, 400, "{\"error\":\"Password must be at least 8 characters\"}");
            }
            
            String name = email.contains("@") ? email.split("@")[0] : email;
            try {
                boolean ok = db.register(email, password, name);
                if (ok) return sendText(ex, 200, "{\"success\":true}");
                return sendText(ex, 400, "{\"error\":\"Registration failed\"}");
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("EMAIL_DUPLICATE")) {
                    return sendText(ex, 409, "{\"error\":\"Email already in use\"}");
                }
                // Generic error message - don't leak internal details
                return sendText(ex, 500, "{\"error\":\"Registration failed\"}");
            }
        }));

        server.createContext("/api/login", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String email = data.getOrDefault("email", "");
            String password = data.getOrDefault("password", "");
            String role = data.getOrDefault("role", "student");

            if (email.isEmpty() || password.isEmpty()) {
                return sendText(ex, 400, "{\"error\":\"Email and password are required\"}");
            }

            boolean ok = db.login(email, password, role);
            if (!ok) {
                return sendText(ex, 401, "{\"error\":\"Invalid credentials\"}");
            }

            // Generate token and store
            String token = generateToken();
            long expiresAt = System.currentTimeMillis() + TOKEN_VALIDITY_MS;
            tokenStore.put(token, new TokenData(email, role, expiresAt));

            return sendText(ex, 200, "{\"success\":true,\"token\":\"" + token + "\",\"email\":\"" + escape(email) + "\",\"role\":\"" + escape(role) + "\"}");
        }));

        // ========== PASSWORD RESET ENDPOINTS ==========
        
        // Request password reset token
        server.createContext("/api/forgot-password", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String email = data.getOrDefault("email", "").trim();

            // Validate email format
            if (!isValidEmail(email)) {
                return sendText(ex, 400, "{\"error\":\"Invalid email format\"}");
            }
            
            // Generate reset token
            String token = db.generatePasswordResetToken(email);

            if (token == null) {
                // Don't reveal if email exists or not (security best practice).
                return sendText(ex, 200, "{\"success\":true,\"message\":\"If email exists, reset code has been generated\"}");
            }

            // For local development only: log the token server-side. In production this
            // would be sent via email and never returned in the response body.
            System.out.println("[FORGOT PASSWORD] Reset code generated for " + email + " (dev-only log): " + token);
            return sendText(ex, 200,
                "{\"success\":true,\"message\":\"If email exists, reset code has been generated\",\"expiresIn\":\"1 hour\"}");
        }));

        // Verify reset token validity
        server.createContext("/api/verify-reset-token", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String token = data.getOrDefault("token", "").trim();

            if (token.isEmpty()) {
                return sendText(ex, 400, "{\"error\":\"Token is required\"}");
            }

            String email = db.verifyResetToken(token);

            if (email == null) {
                return sendText(ex, 400, "{\"error\":\"Invalid or expired reset code\"}");
            }

            return sendText(ex, 200, "{\"success\":true,\"email\":\"" + escape(email) + "\"}");
        }));

        // Reset password with token
        server.createContext("/api/reset-password", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String token = data.getOrDefault("token", "").trim();
            String newPassword = data.getOrDefault("newPassword", "");

            // Validate inputs
            if (token.isEmpty()) {
                return sendText(ex, 400, "{\"error\":\"Reset code is required\"}");
            }

            if (!isValidPassword(newPassword)) {
                return sendText(ex, 400, "{\"error\":\"Password must be at least 8 characters\"}");
            }

            // Reset password
            boolean success = db.resetPasswordWithToken(token, newPassword);

            if (!success) {
                return sendText(ex, 400, "{\"error\":\"Invalid or expired reset code\"}");
            }
            
            // Cleanup old tokens
            int cleaned = db.cleanupExpiredResetTokens();
            System.out.println("[RESET PASSWORD SUCCESS] Password reset complete. Cleaned " + cleaned + " expired tokens.");
            
            return sendText(ex, 200, "{\"success\":true,\"message\":\"Password reset successful\"}");
        }));

        server.createContext("/api/student/profile", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    if (ex.getRequestMethod().equalsIgnoreCase("GET")) {
                        Map<String, String> q = queryToMap(ex.getRequestURI().getRawQuery());
                        String email = q.getOrDefault("email", "");
                        if (email.isEmpty()) { sendText(ex, 400, "{\"error\":\"Email is required\"}"); return; }
                        Map<String, Object> profile = db.getStudentProfile(email);
                        String out = mapToJson(profile);
                        sendText(ex, 200, out);
                    } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {
                        // Require authentication for profile updates
                        TokenData auth = requireAuth(ex);
                        if (auth == null) {
                            sendText(ex, 401, "{\"error\":\"Unauthorized\"}");
                            return;
                        }
                        
                        String body = readBody(ex);
                        Map<String, String> data = parseJson(body);
                        String email = data.getOrDefault("email", "");
                        
                        // Ensure user can only update their own profile
                        if (!email.equals(auth.email)) {
                            sendText(ex, 403, "{\"error\":\"Forbidden\"}");
                            return;
                        }
                        String name = data.getOrDefault("name", "");
                        String idNumber = data.getOrDefault("idNumber", "");
                        String department = data.getOrDefault("department", "");
                        String degree = data.getOrDefault("degree", "");
                        String collegeName = data.getOrDefault("collegeName", "");
                        String phone = data.getOrDefault("phone", "");
                        String certifications = data.getOrDefault("certifications", "");
                        String backlogsStr = data.getOrDefault("backlogs", "0");
                        int backlogs = 0;
                        try { backlogs = Integer.parseInt(backlogsStr); } catch (Exception e) {}
                        Double cgpa = parseDouble(data.get("cgpa"));
                        
                        // Server-side validation for required fields
                        if (email.isEmpty()) { sendText(ex, 400, "{\"error\":\"Email is required\"}"); return; }
                        if (name.isEmpty()) { sendText(ex, 400, "{\"error\":\"Name is required\"}"); return; }
                        if (!name.matches("^[a-zA-Z\\s'-]*$")) { sendText(ex, 400, "{\"error\":\"Name can only contain letters, spaces, hyphens, and apostrophes\"}"); return; }
                        if (idNumber.isEmpty()) { sendText(ex, 400, "{\"error\":\"ID Number is required\"}"); return; }
                        // ID number uniqueness check
                        if (!db.isIdNumberAvailable(idNumber, email)) { sendText(ex, 400, "{\"error\":\"ID Number already exists\"}"); return; }
                        if (phone.isEmpty()) { sendText(ex, 400, "{\"error\":\"Phone number is required\"}"); return; }
                        if (!phone.matches("^\\d{10,15}$")) { sendText(ex, 400, "{\"error\":\"Phone number must be 10-15 digits\"}"); return; }
                        if (cgpa == null || cgpa < 0 || cgpa > 10) { sendText(ex, 400, "{\"error\":\"CGPA must be between 0 and 10\"}"); return; }
                        if (department.isEmpty()) { sendText(ex, 400, "{\"error\":\"Department is required\"}"); return; }
                        if (degree.isEmpty()) { sendText(ex, 400, "{\"error\":\"Degree is required\"}"); return; }
                        if (collegeName.isEmpty()) { sendText(ex, 400, "{\"error\":\"College Name is required\"}"); return; }
                        
                        Map<String, Integer> skills = parseSkills(data.get("skills_raw"));
                        boolean ok = false;
                        try {
                            ok = db.saveProfile(email, name, idNumber, department, degree, collegeName, phone, cgpa, skills, "", certifications, backlogs);
                        } catch (Exception e) {
                            if (e.getMessage() != null && e.getMessage().contains("ID_NUMBER_DUPLICATE")) {
                                sendText(ex, 400, "{\"error\":\"ID Number already exists\"}");
                                return;
                            }
                            e.printStackTrace();
                        }
                        if (ok) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to save profile\"}");
                    } else if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                        sendText(ex, 200, "OK");
                    } else {
                        sendText(ex, 405, "Method Not Allowed");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(ex, 500, "{\"error\":\"Server error\"}");
                }
            }
        });

        server.createContext("/api/companies", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    String method = ex.getRequestMethod();
                    // Handle CORS preflight
                    if (method.equalsIgnoreCase("OPTIONS")) {
                        sendText(ex, 200, "OK");
                        return;
                    }
                    if (method.equalsIgnoreCase("GET")) {
                        List<Map<String,Object>> companies = db.getCompanies();
                        String out = listToJson(companies);
                        sendText(ex, 200, out);
                        return;
                    }

                    if (method.equalsIgnoreCase("POST")) {
                        // Require admin authentication for creating companies
                        TokenData auth = requireAuth(ex);
                        if (auth == null) {
                            sendText(ex, 401, "{\"error\":\"Unauthorized\"}");
                            return;
                        }
                        if (!"admin".equals(auth.role)) {
                            sendText(ex, 403, "{\"error\":\"Forbidden - Admin access required\"}");
                            return;
                        }
                        
                        // Create a new company
                        String body = readBody(ex);
                        Map<String,String> data = parseJson(body);
                        String name = data.getOrDefault("name", "");
                        String link = data.getOrDefault("link", "");
                        Double requiredCgpa = parseDouble(data.get("requiredCgpa"));
                        Map<String,Integer> skills = parseSkills(data.get("skills_raw"));
                        boolean created = db.addCompany(name, link, requiredCgpa != null ? requiredCgpa : -1, skills);
                        if (created) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to save company\"}");
                        return;
                    }

                    // PUT and DELETE are expected to include id in path like /api/companies/123
                    URI uri = ex.getRequestURI();
                    String path = uri.getPath();
                    String[] parts = path.split("/");
                    if (parts.length >= 4) {
                        String idStr = parts[3];
                        int companyId = Integer.parseInt(idStr);
                        if (method.equalsIgnoreCase("OPTIONS")) {
                            sendText(ex, 200, "OK");
                            return;
                        }
                        if (method.equalsIgnoreCase("PUT")) {
                            // Require admin authentication for updating companies
                            TokenData auth = requireAuth(ex);
                            if (auth == null) {
                                sendText(ex, 401, "{\"error\":\"Unauthorized\"}");
                                return;
                            }
                            if (!"admin".equals(auth.role)) {
                                sendText(ex, 403, "{\"error\":\"Forbidden - Admin access required\"}");
                                return;
                            }
                            
                            // Update existing company
                            String body = readBody(ex);
                            Map<String,String> data = parseJson(body);
                            String name = data.getOrDefault("name", "");
                            String link = data.getOrDefault("link", "");
                            Double requiredCgpa = parseDouble(data.get("requiredCgpa"));
                            Map<String,Integer> skills = parseSkills(data.get("skills_raw"));
                            boolean updated = db.updateCompany(companyId, name, link, requiredCgpa != null ? requiredCgpa : -1, skills);
                            if (updated) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to update company\"}");
                            return;
                        } else if (method.equalsIgnoreCase("DELETE")) {
                            // Require admin authentication for deleting companies
                            TokenData auth = requireAuth(ex);
                            if (auth == null) {
                                sendText(ex, 401, "{\"error\":\"Unauthorized\"}");
                                return;
                            }
                            if (!"admin".equals(auth.role)) {
                                sendText(ex, 403, "{\"error\":\"Forbidden - Admin access required\"}");
                                return;
                            }
                            
                            boolean deleted = db.deleteCompany(companyId);
                            if (deleted)  sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to delete company\"}");
                            return;
                        }
                    }

                    sendText(ex, 405, "Method Not Allowed");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(ex, 500, "{\"error\":\"Server error\"}");
                }
            }
        });

        server.createContext("/api/eligible", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { sendText(ex, 405, "Method Not Allowed"); return; }
                    Map<String,String> q = queryToMap(ex.getRequestURI().getRawQuery());
                    double cgpa = 0.0;
                    try { cgpa = Double.parseDouble(q.getOrDefault("cgpa","0")); } catch (Exception e) {}
                    Map<String,Integer> studentSkills = new HashMap<>();
                    String skillsParam = q.get("skills");
                    if (skillsParam != null && !skillsParam.isEmpty()) {
                        String[] pairs = skillsParam.split(",");
                        for (String pair : pairs) {
                            String[] parts = pair.split(":");
                            if (parts.length==2) {
                                try { studentSkills.put(parts[0].trim(), Integer.parseInt(parts[1].trim())); } catch (Exception e) {}
                            }
                        }
                    }
                    List<Map<String,Object>> eligible = db.getEligibleCompanies(cgpa, studentSkills);
                    sendText(ex, 200, listToJson(eligible));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(ex, 400, "{\"error\":\"Invalid parameters\"}");
                }
            }
        });

        // Eligibility Check Endpoint
        server.createContext("/api/eligibility/check", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    // Handle CORS preflight
                    if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendText(ex, 200, "OK"); return; }
                    if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { sendText(ex, 405, "Method Not Allowed"); return; }
                    String body = readBody(ex);
                    Map<String,String> data = parseJson(body);
                    String studentEmail = data.getOrDefault("email", "");
                    
                    int studentId = db.getStudentIdByEmail(studentEmail);
                    if (studentId <= 0) {
                        sendText(ex, 400, "{\"error\":\"Student not found\"}");
                        return;
                    }
                    
                    db.checkAndSaveEligibilityForStudent(studentId, studentEmail);
                    sendText(ex, 200, "{\"success\":true,\"message\":\"Eligibility checked and saved\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(ex, 400, "{\"error\":\"Failed to check eligibility\"}");
                }
            }
        });

        // Get Eligibility Results Endpoint
        server.createContext("/api/eligibility/results", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    // Handle CORS preflight
                    if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendText(ex, 200, "OK"); return; }
                    if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { sendText(ex, 405, "Method Not Allowed"); return; }
                    Map<String,String> q = queryToMap(ex.getRequestURI().getRawQuery());
                    
                    Integer studentId = null;
                    Integer companyId = null;
                    Boolean isEligible = null;
                    
                    if (q.containsKey("studentId")) try { studentId = Integer.parseInt(q.get("studentId")); } catch (Exception e) {}
                    if (q.containsKey("email")) {
                        String email = q.get("email");
                        int id = db.getStudentIdByEmail(email);
                        if (id > 0) studentId = id;
                    }
                    if (q.containsKey("companyId")) try { companyId = Integer.parseInt(q.get("companyId")); } catch (Exception e) {}
                    if (q.containsKey("eligible")) isEligible = q.get("eligible").equalsIgnoreCase("true");
                    
                    List<Map<String,Object>> results = db.getEligibilityResults(studentId, companyId, isEligible);
                    sendText(ex, 200, listToJson(results));
                } catch (Exception e) {
                    e.printStackTrace();
                    sendText(ex, 400, "{\"error\":\"Failed to get results\"}");
                }
            }
        });

        server.setExecutor(null);
        server.start();
        
        System.out.println();
        System.out.println("================================================================");
        System.out.println("  ✓ SERVER STARTED SUCCESSFULLY");
        System.out.println("================================================================");
        System.out.println();
        System.out.println("  Port:        " + port);
        System.out.println("  Health:      http://localhost:" + port + "/api/health");
        System.out.println("  API Base:    http://localhost:" + port + "/api/");
        System.out.println();
        System.out.println("  Endpoints:");
        System.out.println("    GET  /api/health");
        System.out.println("    POST /api/register");
        System.out.println("    POST /api/login");
        System.out.println("    POST /api/forgot-password");
        System.out.println("    POST /api/verify-reset-token");
        System.out.println("    POST /api/reset-password");
        System.out.println("    GET  /api/student/profile");
        System.out.println("    POST /api/student/profile");
        System.out.println("    GET  /api/companies");
        System.out.println("    POST /api/companies");
        System.out.println("    PUT  /api/companies/{id}");
        System.out.println("    DELETE /api/companies/{id}");
        System.out.println("    GET  /api/eligible");
        System.out.println("    POST /api/eligibility/check");
        System.out.println("    GET  /api/eligibility/results");
        System.out.println();
        System.out.println("  Server is ready to accept requests.");
        System.out.println("  Press Ctrl+C to stop.");
        System.out.println("================================================================");
        System.out.println();
        
        // Keep JVM alive so the process doesn't exit immediately
        new CountDownLatch(1).await();
    }

    // --- Helpers and small functional interfaces ---
    /**
     * Callback contract for handlers that need the parsed request body.
     */
    interface BodyHandler { int handle(HttpExchange ex, String body) throws IOException; }

    /**
     * Small adapter that only allows POST (and CORS preflight) and provides the
     * raw request body to the delegate.
     */
    static class PostHandler implements HttpHandler {
        private BodyHandler handler;
        PostHandler(BodyHandler h) { this.handler = h; }
        @Override public void handle(HttpExchange ex) throws IOException {
            if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                sendText(ex, 200, "OK");
                return;
            }
            if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { sendText(ex, 405, "Method Not Allowed"); return; }
            String body = readBody(ex);
            handler.handle(ex, body);
        }
    }

    /**
     * Pass-through handler used where only a simple lambda is needed.
     */
    static class SimpleHandler implements HttpHandler {
        private HttpHandler inner;
        SimpleHandler(HttpHandler h) { this.inner = h; }
        public void handle(HttpExchange ex) throws IOException { inner.handle(ex); }
    }

    /**
     * Read the full request body as UTF-8 text.
     */
    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        byte[] data = is.readAllBytes();
        String body = new String(data, StandardCharsets.UTF_8);
        return body;
    }

    /**
     * Write a JSON response with common CORS headers.
     *
     * @param ex     exchange
     * @param status HTTP status code
     * @param body   JSON string to write
     */
    static int sendText(HttpExchange ex, int status, String body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        
        // CORS allow-list - read from environment or default to localhost:5500.
        // Exact-match only (split on comma, trim) to prevent substring bypass.
        String allowedOriginsEnv = System.getenv("ALLOWED_ORIGINS");
        if (allowedOriginsEnv == null || allowedOriginsEnv.isEmpty()) {
            allowedOriginsEnv = "http://localhost:5500";
        }
        java.util.Set<String> allowed = new java.util.HashSet<>();
        for (String o : allowedOriginsEnv.split(",")) {
            String t = o.trim();
            if (!t.isEmpty()) allowed.add(t);
        }

        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (origin != null && allowed.contains(origin)) {
            h.add("Access-Control-Allow-Origin", origin);
        } else if (origin == null) {
            // For non-browser/same-origin requests, allow the first listed origin.
            String firstOrigin = allowed.iterator().hasNext() ? allowed.iterator().next() : "http://localhost:5500";
            h.add("Access-Control-Allow-Origin", firstOrigin);
        }
        
        h.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Accept, Origin, Authorization");
        h.add("Access-Control-Max-Age", "86400");
        h.add("Vary", "Origin");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        return status;
    }

    /**
     * Very small JSON parser for flat key/value pairs.
     *
     * This handles strings, numbers, and a raw nested object captured as a
     * string (used for skills). It avoids external dependencies.
     */
    static Map<String,String> parseJson(String body) {
        Map<String,String> map = new HashMap<>();
        if (body == null) return map;
        String s = body.trim();
        if (s.startsWith("{" ) && s.endsWith("}")) s = s.substring(1, s.length()-1);
        int idx = 0;
        while (idx < s.length()) {
            // find key
            int k1 = s.indexOf('"', idx);
            if (k1 < 0) break;
            int k2 = s.indexOf('"', k1+1);
            if (k2 < 0) break;
            String key = s.substring(k1+1, k2);
            int colon = s.indexOf(':', k2);
            if (colon < 0) break;
            int valStart = colon+1;
            while (valStart < s.length() && Character.isWhitespace(s.charAt(valStart))) valStart++;
            String value = "";
            if (valStart < s.length() && s.charAt(valStart) == '"') {
                int v1 = valStart+1;
                int v2 = s.indexOf('"', v1);
                while (v2 > v1 && s.charAt(v2-1) == '\\') { v2 = s.indexOf('"', v2+1); }
                if (v2 < 0) v2 = s.length();
                value = s.substring(v1, v2);
                idx = v2+1;
            } else if (valStart < s.length() && s.charAt(valStart) == '{') {
                // capture raw object for skills
                int brace = valStart;
                int depth = 0;
                int i = brace;
                for (; i < s.length(); i++) {
                    if (s.charAt(i) == '{') depth++;
                    else if (s.charAt(i) == '}') { depth--; if (depth==0) { i++; break; } }
                }
                value = s.substring(valStart, Math.min(i, s.length()));
                // store raw object as skills_raw
                map.put(key, value);
                idx = i;
                continue;
            } else {
                // number, null, or bare
                int v2 = valStart;
                while (v2 < s.length() && s.charAt(v2) != ',' ) v2++;
                value = s.substring(valStart, v2).trim();
                idx = v2+1;
            }
            map.put(key, value);
            // skip comma/whitespace
            while (idx < s.length() && (s.charAt(idx)==',' || Character.isWhitespace(s.charAt(idx)))) idx++;
        }
        // If skills was stored as raw JSON in map under "skills" key, also put a friendly key "skills_raw"
        if (map.containsKey("skills") && map.get("skills").startsWith("{")) {
            map.put("skills_raw", map.get("skills"));
        }
        return map;
    }

    /**
     * Parse a simple JSON object of skills like {"Java":3,"SQL":2} into a map.
     */
    static Map<String,Integer> parseSkills(String raw) {
        Map<String,Integer> skills = new HashMap<>();
        if (raw == null) return skills;
        String s = raw.trim();
        if (s.startsWith("{" ) && s.endsWith("}")) s = s.substring(1, s.length()-1);
        // split by commas at top level
        String[] parts = s.split(",");
        for (String p : parts) {
            String[] kv = p.split(":");
            if (kv.length>=2) {
                String k = kv[0].trim(); if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1,k.length()-1);
                String v = kv[1].trim();
                try { skills.put(k, Integer.parseInt(v)); } catch (Exception e) {}
            }
        }
        return skills;
    }

    /** Parse a Double, returning null on any error. */
    static Double parseDouble(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    /** Turn a query string like a=b&c=d into a map. */
    static Map<String,String> queryToMap(String query) {
        Map<String,String> result = new HashMap<>();
        if (query == null) return result;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx+1), "UTF-8");
                    result.put(key, value);
                }
            } catch (Exception e) {}
        }
        return result;
    }

    /** Serialize a map to a compact JSON string (strings, maps, numbers). */
    static String mapToJson(Map<String,Object> map) {
        if (map == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String,Object> e : map.entrySet()) {
            if (!first) sb.append(','); first=false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof String) sb.append('"').append(escape((String)v)).append('"');
            else if (v instanceof Map) sb.append(mapToJson((Map)v));
            else sb.append(String.valueOf(v));
        }
        sb.append('}');
        return sb.toString();
    }

    /** Serialize a list of maps to a JSON array string. */
    static String listToJson(List<Map<String,Object>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Map<String,Object> m : list) {
            if (!first) sb.append(','); first=false;
            sb.append(mapToJson(m));
        }
        sb.append(']');
        return sb.toString();
    }

    /** Minimal string escape for JSON content. */
    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
