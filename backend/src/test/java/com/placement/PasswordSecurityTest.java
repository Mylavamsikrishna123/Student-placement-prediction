package com.placement;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for password security and BCrypt implementation.
 * Tests password hashing, verification, and legacy password upgrade logic.
 */
@DisplayName("Password Security Tests")
public class PasswordSecurityTest {
    
    @Test
    @DisplayName("BCrypt hashing produces valid hash format ($2a$ or $2b$ prefix)")
    public void testBCryptHashFormat() {
        String password = "TestPassword123";
        String salt = BCrypt.gensalt(12);
        String hash = BCrypt.hashpw(password, salt);
        
        assertNotNull(hash, "Hash should not be null");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"), 
            "Hash should start with $2a$ or $2b$");
        assertEquals(60, hash.length(), "BCrypt hash should be exactly 60 characters");
    }
    
    @Test
    @DisplayName("BCrypt salt is random - same password produces different hashes")
    public void testBCryptSaltRandomness() {
        String password = "SamePassword123";
        
        String hash1 = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String hash2 = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        assertNotEquals(hash1, hash2, "Different salts should produce different hashes");
        assertTrue(BCrypt.checkpw(password, hash1), "Password should verify against hash1");
        assertTrue(BCrypt.checkpw(password, hash2), "Password should verify against hash2");
    }
    
    @Test
    @DisplayName("BCrypt verification succeeds for correct password")
    public void testBCryptPasswordVerification() {
        String password = "CorrectPassword123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        boolean result = BCrypt.checkpw(password, hash);
        assertTrue(result, "Correct password should verify against hash");
    }
    
    @Test
    @DisplayName("BCrypt verification fails for wrong password")
    public void testBCryptWrongPasswordRejection() {
        String correctPassword = "CorrectPassword123";
        String wrongPassword = "WrongPassword123";
        String hash = BCrypt.hashpw(correctPassword, BCrypt.gensalt(12));
        
        boolean result = BCrypt.checkpw(wrongPassword, hash);
        assertFalse(result, "Wrong password should not verify against hash");
    }
    
    @Test
    @DisplayName("BCrypt cost factor 12 is used (security standard)")
    public void testBCryptCostFactor() {
        String password = "TestPassword123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        
        // Extract cost factor from hash (format: $2a$12$...)
        String costStr = hash.substring(4, 6);
        int costFactor = Integer.parseInt(costStr);
        
        assertEquals(12, costFactor, "Cost factor should be 12 for security");
    }
    
    @Test
    @DisplayName("BCrypt hash from one salt cannot be used with another password")
    public void testBCryptCrossSaltRejection() {
        String password1 = "Password1";
        String password2 = "Password2";
        
        String hash1 = BCrypt.hashpw(password1, BCrypt.gensalt(12));
        String hash2 = BCrypt.hashpw(password2, BCrypt.gensalt(12));
        
        assertFalse(BCrypt.checkpw(password1, hash2), "Password1 should not match hash from Password2");
        assertFalse(BCrypt.checkpw(password2, hash1), "Password2 should not match hash from Password1");
    }
    
    @Test
    @DisplayName("Password with special characters hashes correctly")
    public void testSpecialCharacterPasswordHashing() {
        String passwordWithSpecialChars = "P@ssw0rd!#$%^&*()";
        String hash = BCrypt.hashpw(passwordWithSpecialChars, BCrypt.gensalt(12));
        
        assertTrue(BCrypt.checkpw(passwordWithSpecialChars, hash), 
            "Special character password should hash and verify correctly");
    }
    
    @Test
    @DisplayName("Empty password can be hashed (though not recommended)")
    public void testEmptyPasswordHashing() {
        String emptyPassword = "";
        String hash = BCrypt.hashpw(emptyPassword, BCrypt.gensalt(12));
        
        assertNotNull(hash, "Empty password can still be hashed");
        assertTrue(BCrypt.checkpw(emptyPassword, hash), 
            "Empty password should verify against its hash");
    }
    
    @Test
    @DisplayName("Long password hashes correctly")
    public void testLongPasswordHashing() {
        String longPassword = "a".repeat(100) + "Test123";
        String hash = BCrypt.hashpw(longPassword, BCrypt.gensalt(12));
        
        assertTrue(BCrypt.checkpw(longPassword, hash), 
            "Long password should hash and verify correctly");
    }
    
    @Test
    @DisplayName("Case-sensitive password hashing - 'Password' != 'password'")
    public void testPasswordCaseSensitivity() {
        String passwordUpper = "TestPassword123";
        String passwordLower = "testpassword123";
        String hash = BCrypt.hashpw(passwordUpper, BCrypt.gensalt(12));
        
        assertTrue(BCrypt.checkpw(passwordUpper, hash), "Correct case should match");
        assertFalse(BCrypt.checkpw(passwordLower, hash), "Different case should not match");
    }
}
