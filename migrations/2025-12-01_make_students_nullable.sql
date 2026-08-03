-- Migration: Make students.student_id_number and students.degree nullable to match backend registration flow
-- Reason: Backend defers ID Number and Degree until profile completion; registration currently inserts NULLs for these fields.

USE JAVAPROJECT;

ALTER TABLE students 
    MODIFY COLUMN student_id_number VARCHAR(50) NULL UNIQUE;

ALTER TABLE students 
    MODIFY COLUMN degree VARCHAR(20) NULL;