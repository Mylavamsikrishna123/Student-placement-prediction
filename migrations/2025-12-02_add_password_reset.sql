-- Password Reset Tokens Table
-- Note: A fresh run of database_schema.sql already includes this table; this migration
--       is only needed for databases created from an older version of the schema.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    reset_token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_token (reset_token),
    INDEX idx_expires (expires_at)
);
