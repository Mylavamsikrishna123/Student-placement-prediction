package com.placement;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.placement.models.Company;
import com.placement.models.Student;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// Simple HTTP server using built-in HttpServer
public class App {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try { port = Integer.parseInt(envPort); } catch (Exception e) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Database db = new Database();

        server.createContext("/api/health", new SimpleHandler((ex) -> sendText(ex, 200, "{\"status\":\"ok\"}")));

        server.createContext("/api/register", new PostHandler((ex, body) -> {
            Map<String, String> data = parseJson(body);
            String email = data.getOrDefault("email", "");
            String password = data.getOrDefault("password", "");
            if (email.isEmpty() || password.isEmpty()) {
                return sendText(ex, 400, "{\"error\":\"Email and password are required\"}");
            }
            String name = email.contains("@") ? email.split("@")[0] : email;
            boolean ok = db.register(email, password, name);
            if (ok) return sendText(ex, 200, "{\"success\":true}");
            return sendText(ex, 400, "{\"error\":\"Registration failed\"}");
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
            if (ok) return sendText(ex, 200, "{\"success\":true}");
            return sendText(ex, 401, "{\"error\":\"Invalid credentials\"}");
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
                        String body = readBody(ex);
                        Map<String, String> data = parseJson(body);
                        String email = data.getOrDefault("email", "");
                        String name = data.getOrDefault("name", "");
                        String idNumber = data.getOrDefault("idNumber", "");
                        String department = data.getOrDefault("department", "");
                        String degree = data.getOrDefault("degree", "");
                        String collegeName = data.getOrDefault("collegeName", "");
                        String phone = data.getOrDefault("phone", "");
                        Double cgpa = parseDouble(data.get("cgpa"));
                        Map<String, Integer> skills = parseSkills(data.get("skills_raw"));
                        boolean ok = db.saveProfile(email, name, idNumber, department, degree, collegeName, phone, cgpa, skills);
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
                    if (method.equalsIgnoreCase("GET")) {
                        List<Map<String,Object>> companies = db.getCompanies();
                        String out = listToJson(companies);
                        sendText(ex, 200, out);
                        return;
                    }

                    if (method.equalsIgnoreCase("POST")) {
                        String body = readBody(ex);
                        Map<String,String> data = parseJson(body);
                        String name = data.getOrDefault("name", "");
                        String link = data.getOrDefault("link", "");
                        Double requiredCgpa = parseDouble(data.get("requiredCgpa"));
                        Map<String,Integer> skills = parseSkills(data.get("skills_raw"));
                        boolean ok = db.addCompany(name, link, requiredCgpa != null ? requiredCgpa : -1, skills);
                        if (ok) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to save company\"}");
                        return;
                    }

                    // PUT and DELETE are expected to include id in path like /api/companies/123
                    URI uri = ex.getRequestURI();
                    String path = uri.getPath();
                    String[] parts = path.split("/");
                    if (parts.length >= 4) {
                        String idStr = parts[3];
                        int companyId = Integer.parseInt(idStr);
                        if (method.equalsIgnoreCase("PUT")) {
                            String body = readBody(ex);
                            Map<String,String> data = parseJson(body);
                            String name = data.getOrDefault("name", "");
                            String link = data.getOrDefault("link", "");
                            Double requiredCgpa = parseDouble(data.get("requiredCgpa"));
                            Map<String,Integer> skills = parseSkills(data.get("skills_raw"));
                            boolean ok = db.updateCompany(companyId, name, link, requiredCgpa != null ? requiredCgpa : -1, skills);
                            if (ok) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to update company\"}");
                            return;
                        } else if (method.equalsIgnoreCase("DELETE")) {
                            boolean ok = db.deleteCompany(companyId);
                            if (ok) sendText(ex, 200, "{\"success\":true}"); else sendText(ex, 400, "{\"error\":\"Failed to delete company\"}");
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

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + port);
    }

    // --- Helpers and small functional interfaces ---
    interface BodyHandler { int handle(HttpExchange ex, String body) throws IOException; }

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

    static class SimpleHandler implements HttpHandler {
        private HttpHandler inner;
        SimpleHandler(HttpHandler h) { this.inner = h; }
        public void handle(HttpExchange ex) throws IOException { inner.handle(ex); }
    }

    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        byte[] data = is.readAllBytes();
        String body = new String(data, StandardCharsets.UTF_8);
        return body;
    }

    static int sendText(HttpExchange ex, int status, String body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "application/json; charset=utf-8");
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        return status;
    }

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

    static Double parseDouble(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

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

    static String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
