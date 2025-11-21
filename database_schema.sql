CREATE DATABASE IF NOT EXISTS JAVAPROJECT;
USE JAVAPROJECT;

-- Drop existing tables if they exist (in reverse order of dependencies)
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
    student_id_number VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    degree VARCHAR(20) NOT NULL, -- B.Tech or M.Tech
    cgpa DECIMAL(4,2),
    college_name VARCHAR(150),
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

