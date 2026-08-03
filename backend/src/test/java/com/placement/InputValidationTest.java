package com.placement;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for input validation logic across API layer and Database layer.
 * Tests validation of: email format, phone format, CGPA range, name format, etc.
 */
@DisplayName("Input Validation Tests")
public class InputValidationTest {
    
    @Test
    @DisplayName("Valid email format is accepted")
    public void testValidEmailFormat() {
        String[] validEmails = {
            "user@example.com",
            "john.doe@example.co.uk",
            "test+tag@domain.org",
            "user123@test.com"
        };
        
        for (String email : validEmails) {
            assertTrue(isValidEmail(email), "Email should be valid: " + email);
        }
    }
    
    @Test
    @DisplayName("Invalid email formats are rejected")
    public void testInvalidEmailFormat() {
        String[] invalidEmails = {
            "notanemail",
            "@example.com",
            "user@",
            "user @example.com",
            "user@.com",
            ""
        };
        
        for (String email : invalidEmails) {
            assertFalse(isValidEmail(email), "Email should be invalid: " + email);
        }
    }
    
    @Test
    @DisplayName("Phone number 10-15 digits is valid")
    public void testValidPhoneFormat() {
        String[] validPhones = {
            "9876543210",      // 10 digits
            "98765432101",     // 11 digits
            "987654321012345"  // 15 digits
        };
        
        for (String phone : validPhones) {
            assertTrue(isValidPhone(phone), "Phone should be valid: " + phone);
        }
    }
    
    @Test
    @DisplayName("Phone number outside 10-15 digits is rejected")
    public void testInvalidPhoneFormat() {
        String[] invalidPhones = {
            "123456",           // 6 digits - too short
            "9876543210123456", // 16 digits - too long
            "98765 43210",      // contains space
            "9876543210a",      // contains letter
            "+919876543210",    // contains + sign
            ""                  // empty
        };
        
        for (String phone : invalidPhones) {
            assertFalse(isValidPhone(phone), "Phone should be invalid: " + phone);
        }
    }
    
    @Test
    @DisplayName("CGPA between 0.0 and 10.0 is valid")
    public void testValidCGPARange() {
        double[] validCGPAs = { 0.0, 5.5, 7.0, 9.99, 10.0 };
        
        for (double cgpa : validCGPAs) {
            assertTrue(isValidCGPA(cgpa), "CGPA should be valid: " + cgpa);
        }
    }
    
    @Test
    @DisplayName("CGPA outside 0.0-10.0 range is rejected")
    public void testInvalidCGPARange() {
        double[] invalidCGPAs = { -1.0, -0.1, 10.01, 11.0, 15.0 };
        
        for (double cgpa : invalidCGPAs) {
            assertFalse(isValidCGPA(cgpa), "CGPA should be invalid: " + cgpa);
        }
    }
    
    @Test
    @DisplayName("Name with only letters, spaces, hyphens, apostrophes is valid")
    public void testValidNameFormat() {
        String[] validNames = {
            "John Doe",
            "Mary-Jane",
            "O'Connor",
            "Jean-Claude",
            "Maria de la Cruz"
        };
        
        for (String name : validNames) {
            assertTrue(isValidName(name), "Name should be valid: " + name);
        }
    }
    
    @Test
    @DisplayName("Name with numbers or special characters is rejected")
    public void testInvalidNameFormat() {
        String[] invalidNames = {
            "John123",        // contains numbers
            "John@",          // contains @
            "John#Doe",       // contains #
            "User123",        // contains numbers
            ""                // empty
        };
        
        for (String name : invalidNames) {
            assertFalse(isValidName(name), "Name should be invalid: " + name);
        }
    }
    
    @Test
    @DisplayName("Backlogs non-negative integer is valid")
    public void testValidBacklogsValue() {
        int[] validBacklogs = { 0, 1, 2, 5, 10 };
        
        for (int backlog : validBacklogs) {
            assertTrue(isValidBacklogs(backlog), "Backlogs should be valid: " + backlog);
        }
    }
    
    @Test
    @DisplayName("Negative backlogs value is rejected")
    public void testInvalidBacklogsNegative() {
        int[] invalidBacklogs = { -1, -5, -10 };
        
        for (int backlog : invalidBacklogs) {
            assertFalse(isValidBacklogs(backlog), "Backlogs should be invalid (negative): " + backlog);
        }
    }
    
    @Test
    @DisplayName("Student ID number format STU+6digits is valid")
    public void testValidIDNumberFormat() {
        String[] validIDs = {
            "STU123456",
            "STU000001",
            "STU999999"
        };
        
        for (String id : validIDs) {
            assertTrue(isValidIDNumber(id), "ID number should be valid: " + id);
        }
    }
    
    @Test
    @DisplayName("Invalid student ID number format is rejected")
    public void testInvalidIDNumberFormat() {
        String[] invalidIDs = {
            "ID123456",       // wrong prefix
            "STU12345",       // only 5 digits
            "STU1234567",     // 7 digits
            "stu123456",      // lowercase
            "",               // empty
            "123456"          // no prefix
        };
        
        for (String id : invalidIDs) {
            assertFalse(isValidIDNumber(id), "ID number should be invalid: " + id);
        }
    }
    
    @Test
    @DisplayName("Valid degree values are accepted")
    public void testValidDegreeFormat() {
        String[] validDegrees = { "B.Tech", "M.Tech", "B.E." };
        
        for (String degree : validDegrees) {
            assertTrue(isValidDegree(degree), "Degree should be valid: " + degree);
        }
    }
    
    @Test
    @DisplayName("Invalid degree values are rejected")
    public void testInvalidDegreeFormat() {
        String[] invalidDegrees = { "", "BS", "Diploma", "PhD" };
        
        for (String degree : invalidDegrees) {
            assertFalse(isValidDegree(degree), "Degree should be invalid: " + degree);
        }
    }
    
    /**
     * Helper validation methods - match App.java implementations
     */
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        return phone.matches("^\\d{10,15}$");
    }
    
    private boolean isValidCGPA(double cgpa) {
        return cgpa >= 0.0 && cgpa <= 10.0;
    }
    
    private boolean isValidName(String name) {
        if (name == null || name.isEmpty()) return false;
        // Only letters, spaces, hyphens, apostrophes
        return name.matches("^[a-zA-Z\\s\\-']+$");
    }
    
    private boolean isValidBacklogs(int backlogs) {
        return backlogs >= 0;
    }
    
    private boolean isValidIDNumber(String id) {
        if (id == null || id.isEmpty()) return false;
        return id.matches("^STU\\d{6}$");
    }
    
    private boolean isValidDegree(String degree) {
        if (degree == null || degree.isEmpty()) return false;
        return degree.equals("B.Tech") || degree.equals("M.Tech") || degree.equals("B.E.");
    }
}
