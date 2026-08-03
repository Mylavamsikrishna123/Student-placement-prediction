CREATE DATABASE IF NOT EXISTS JAVAPROJECT;
USE JAVAPROJECT;

-- Drop existing tables if they exist (in reverse order of dependencies)
DROP TABLE IF EXISTS eligibility_results;
DROP TABLE IF EXISTS company_skills;
DROP TABLE IF EXISTS student_skills;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS admin;

-- Students table --
CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    student_id_number VARCHAR(50) UNIQUE, -- Nullable until student completes profile
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    branch VARCHAR(100),
    degree VARCHAR(20), -- Nullable until student completes profile
    cgpa DECIMAL(4,2),
    college_name VARCHAR(150),
    phone_number VARCHAR(15),
    certifications TEXT,
    backlogs INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Skills table (master list of all possible skills)
CREATE TABLE skills (
    skill_id INT AUTO_INCREMENT PRIMARY KEY,
    skill_name VARCHAR(50) NOT NULL UNIQUE
);

-- Student_Skills table (many-to-many: students and their skill levels)
CREATE TABLE student_skills (
    student_skill_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    skill_id INT NOT NULL,
    skill_level INT NOT NULL, -- 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_skill (student_id, skill_id)
);

-- Companies table
CREATE TABLE companies (
    company_id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL UNIQUE,
    application_link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    required_cgpa DECIMAL(4,2) DEFAULT NULL
);

-- Company_Skills table (many-to-many: companies and their required skills with levels)
CREATE TABLE company_skills (
    company_skill_id INT AUTO_INCREMENT PRIMARY KEY,
    company_id INT NOT NULL,
    skill_id INT NOT NULL,
    required_level INT NOT NULL, -- 1=Beginner⭐, 2=Medium⭐⭐, 3=Advanced⭐⭐⭐
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE CASCADE,
    UNIQUE KEY unique_company_skill (company_id, skill_id)
);

-- Eligibility Results table (stores historical eligibility checks)
CREATE TABLE eligibility_results (
    result_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    company_id INT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    is_eligible BOOLEAN DEFAULT FALSE,
    reason_if_not_eligible VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(company_id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_company (company_id),
    INDEX idx_eligible (is_eligible),
    INDEX idx_timestamp (timestamp)
);

-- Admin table
CREATE TABLE admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin credentials (admin@placement.com / admin123)
INSERT IGNORE INTO admin (email, password) VALUES ('admin@placement.com', 'admin123');

-- Insert common skills
INSERT IGNORE INTO skills (skill_name) VALUES 
('Java'), ('Python'), ('C'), ('C++'), ('HTML'), ('CSS'), ('JavaScript'), 
('ReactJS'), ('SQL'), ('Node.js'), ('Spring'), ('Angular'), ('Vue.js');

