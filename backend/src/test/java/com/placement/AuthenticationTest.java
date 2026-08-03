package com.placement;

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for API endpoints with authentication and authorization.
 * Tests token generation, validation, protected routes, and CORS.
 * 
 * NOTE: These tests require a running backend server on port 8081.
 * To run these tests:
 * 1. Start the backend server: mvn spring-boot:run
 * 2. In another terminal, run: mvn test -Dtest=AuthenticationTest
 * 
 * Tests are currently disabled to prevent failures when server is not running.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthenticationTest {
    
    private HttpClient client;
    private static final String BASE_URL = "http://localhost:8081";
    private String studentToken;
    private String adminToken;
    
    @BeforeAll
    public void setup() throws Exception {
        client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }
    
    @AfterAll
    public void teardown() {
        // Cleanup
    }
    
    // ===== REGISTRATION TESTS =====
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Register with valid email and strong password")
    public void testRegisterValidUser() throws Exception {
        String requestBody = "{\"email\":\"newuser@test.com\",\"password\":\"strongpass123\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Should succeed or fail with duplicate (if already exists from previous test)
        assertTrue(response.statusCode() == 200 || response.statusCode() == 409,
            "Registration should succeed or fail with duplicate");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Register with invalid email format should fail")
    public void testRegisterInvalidEmail() throws Exception {
        String requestBody = "{\"email\":\"notanemail\",\"password\":\"password123\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(400, response.statusCode(), "Should reject invalid email");
        assertTrue(response.body().contains("error"), "Should return error message");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Register with weak password should fail")
    public void testRegisterWeakPassword() throws Exception {
        String requestBody = "{\"email\":\"user@test.com\",\"password\":\"weak\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(400, response.statusCode(), "Should reject weak password");
        assertTrue(response.body().contains("8 characters"), "Should mention password length requirement");
    }
    
    // ===== LOGIN AND TOKEN TESTS =====
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Login returns token in response")
    public void testLoginReturnsToken() throws Exception {
        // First register a user
        String registerBody = "{\"email\":\"tokenuser@test.com\",\"password\":\"password123\"}";
        HttpRequest registerReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody))
            .build();
        client.send(registerReq, HttpResponse.BodyHandlers.ofString());
        
        // Then login
        String loginBody = "{\"email\":\"tokenuser@test.com\",\"password\":\"password123\",\"role\":\"student\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        
        HttpResponse<String> response = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Login should succeed");
        assertTrue(response.body().contains("token"), "Response should contain token");
        assertTrue(response.body().contains("email"), "Response should contain email");
        assertTrue(response.body().contains("role"), "Response should contain role");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Login with wrong password fails")
    public void testLoginWrongPassword() throws Exception {
        String loginBody = "{\"email\":\"tokenuser@test.com\",\"password\":\"wrongpassword\",\"role\":\"student\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Should reject wrong password");
        assertTrue(response.body().contains("Invalid credentials"), "Should return error message");
    }
    
    // ===== PROTECTED ROUTE TESTS =====
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Profile update without token returns 401")
    public void testProfileUpdateWithoutToken() throws Exception {
        String requestBody = "{\"email\":\"user@test.com\",\"name\":\"Test\",\"cgpa\":8.0}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/student/profile"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Should reject request without token");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Profile update with invalid token returns 401")
    public void testProfileUpdateWithInvalidToken() throws Exception {
        String requestBody = "{\"email\":\"user@test.com\",\"name\":\"Test\",\"cgpa\":8.0}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/student/profile"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer invalid_token_12345")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(401, response.statusCode(), "Should reject invalid token");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Profile update with valid token succeeds")
    public void testProfileUpdateWithValidToken() throws Exception {
        // Register and login to get token
        String registerBody = "{\"email\":\"authuser@test.com\",\"password\":\"password123\"}";
        HttpRequest registerReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody))
            .build();
        client.send(registerReq, HttpResponse.BodyHandlers.ofString());
        
        String loginBody = "{\"email\":\"authuser@test.com\",\"password\":\"password123\",\"role\":\"student\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        HttpResponse<String> loginResp = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        
        // Extract token from response (simple parsing)
        String token = extractToken(loginResp.body());
        assertNotNull(token, "Should receive token from login");
        
        // Update profile with token
        String profileBody = "{\"email\":\"authuser@test.com\",\"name\":\"Auth User\",\"idNumber\":\"AUTH001\"," +
            "\"department\":\"CS\",\"degree\":\"B.Tech\",\"collegeName\":\"Test College\"," +
            "\"phone\":\"1234567890\",\"cgpa\":8.5,\"backlogs\":0}";
        
        HttpRequest profileReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/student/profile"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(profileBody))
            .build();
        
        HttpResponse<String> response = client.send(profileReq, HttpResponse.BodyHandlers.ofString());
        
        // Should succeed (200) or fail with validation error (400) depending on data state
        assertTrue(response.statusCode() == 200 || response.statusCode() == 400,
            "Should process authenticated request");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("User cannot update another user's profile")
    public void testProfileUpdateCrossUser() throws Exception {
        // Login as user A
        String registerA = "{\"email\":\"userA@test.com\",\"password\":\"password123\"}";
        HttpRequest registerReqA = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerA))
            .build();
        client.send(registerReqA, HttpResponse.BodyHandlers.ofString());
        
        String loginA = "{\"email\":\"userA@test.com\",\"password\":\"password123\",\"role\":\"student\"}";
        HttpRequest loginReqA = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginA))
            .build();
        HttpResponse<String> loginRespA = client.send(loginReqA, HttpResponse.BodyHandlers.ofString());
        String tokenA = extractToken(loginRespA.body());
        
        // Try to update user B's profile with user A's token
        String profileB = "{\"email\":\"userB@test.com\",\"name\":\"User B\",\"idNumber\":\"B001\"," +
            "\"department\":\"CS\",\"degree\":\"B.Tech\",\"collegeName\":\"Test College\"," +
            "\"phone\":\"1234567890\",\"cgpa\":8.0,\"backlogs\":0}";
        
        HttpRequest profileReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/student/profile"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + tokenA)
            .POST(HttpRequest.BodyPublishers.ofString(profileB))
            .build();
        
        HttpResponse<String> response = client.send(profileReq, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(403, response.statusCode(), "Should reject cross-user update");
    }
    
    // ===== ADMIN AUTHORIZATION TESTS =====
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("Student cannot create companies (admin only)")
    public void testStudentCannotCreateCompany() throws Exception {
        // Login as student
        String registerBody = "{\"email\":\"student@test.com\",\"password\":\"password123\"}";
        HttpRequest registerReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerBody))
            .build();
        client.send(registerReq, HttpResponse.BodyHandlers.ofString());
        
        String loginBody = "{\"email\":\"student@test.com\",\"password\":\"password123\",\"role\":\"student\"}";
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        HttpResponse<String> loginResp = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        String token = extractToken(loginResp.body());
        
        // Try to create company
        String companyBody = "{\"name\":\"Test Corp\",\"link\":\"https://test.com\",\"requiredCgpa\":7.0," +
            "\"skills\":{\"Java\":2}}";
        
        HttpRequest companyReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/companies"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(companyBody))
            .build();
        
        HttpResponse<String> response = client.send(companyReq, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(403, response.statusCode(), "Student should be forbidden from creating companies");
    }
    
    // ===== CORS TESTS =====
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("CORS headers include Authorization")
    public void testCorsHeadersIncludeAuthorization() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/health"))
            .header("Origin", "http://localhost:5500")
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        String allowHeaders = response.headers()
            .firstValue("Access-Control-Allow-Headers")
            .orElse("");
        
        assertTrue(allowHeaders.contains("Authorization"),
            "CORS should allow Authorization header");
    }
    
    @Test
    @Disabled("Requires running server on port 8081 - integration test")
    @DisplayName("OPTIONS preflight succeeds")
    public void testOptionsPreflight() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/student/profile"))
            .header("Origin", "http://localhost:5500")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "OPTIONS should return 200");
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Extract token from JSON response (simple string parsing).
     */
    private String extractToken(String jsonResponse) {
        String search = "\"token\":\"";
        int start = jsonResponse.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = jsonResponse.indexOf("\"", start);
        if (end < 0) return null;
        return jsonResponse.substring(start, end);
    }
}
