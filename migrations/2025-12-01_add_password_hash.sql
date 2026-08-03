-- Migration: Add password_hash column to students and admin tables
-- Purpose: Support BCrypt hashed passwords for security upgrade
-- Migration strategy: Keeps existing password column for backward compatibility during transition

-- Students table - add password_hash column
ALTER TABLE students
ADD COLUMN password_hash VARCHAR(60) COMMENT 'BCrypt hashed password';

-- Admin table - add password_hash column
ALTER TABLE admin
ADD COLUMN password_hash VARCHAR(60) COMMENT 'BCrypt hashed password';

-- Add indexes for performance (optional but recommended)
CREATE INDEX idx_students_email ON students(email);
CREATE INDEX idx_admin_email ON admin(email);

-- Note: After migration, the application will:
-- 1. On registration: Store BCrypt hash in password_hash column
-- 2. On login: Check password_hash first, fall back to plaintext for legacy users
-- 3. Auto-upgrade: Update legacy passwords to BCrypt on successful login
-- 
-- Once all users have been upgraded, you can safely:
-- ALTER TABLE students DROP COLUMN password;
-- ALTER TABLE admin DROP COLUMN password;
