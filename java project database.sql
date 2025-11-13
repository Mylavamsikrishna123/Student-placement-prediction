CREATE  DATABASE JAVAPROJECT;
USE JAVAPROJECT;
CREATE TABLE register (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    confirm_password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO register (email, password, confirm_password)
VALUES
('rizvyshaik@gmail.com', 'Rizvy123', 'Rizvy123'),
('jayakrishna@gmail.com', 'Jaykrishna123', 'Jaykrishna123'),
('vamsikrishna@gmail.com', 'VamsiKrishna123', 'VamsiKrishna123'),
('divya12@gmail.com', 'Divya123', 'Divya123'),
('ammysyed@gmail.com', 'Ammy123', 'Ammy123'),
('tonnystark@gmail.com', 'Tonny123', 'Tonny123'),
('bunny@gmail.com', 'Bunny123', 'Bunny123');

CREATE TABLE student_login (
    login_id INT AUTO_INCREMENT PRIMARY KEY,
    register_id INT NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (register_id) REFERENCES register(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
INSERT INTO student_login (register_id, email, password)
VALUES
(1, 'rizvyshaik@gmail.com', 'Rizvy123'),
(2, 'jayakrishna@gmail.com', 'Jaykrishna123'),
(3, 'vamsikrishna@gmail.com', 'VamsiKrishna123'),
(4, 'divya12@gmail.com', 'Divya123'),
(5, 'ammysyed@gmail.com', 'Ammy123'),
(6, 'tonnystark@gmail.com', 'Tonny123'),
(7, 'bunny@gmail.com', 'Bunny123');
CREATE TABLE student_dashboard (
    dashboard_id INT AUTO_INCREMENT PRIMARY KEY,
    register_id INT NOT NULL,
    login_id INT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    student_id_number VARCHAR(50) NOT NULL UNIQUE,
    college_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(15),
    gpa DECIMAL(3,2),
    skills VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (register_id) REFERENCES register(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (login_id) REFERENCES student_login(login_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
INSERT INTO student_dashboard 
(register_id, login_id, student_name, student_id_number, college_name, phone_number, gpa, skills)
VALUES
(1, 1, 'Shaik Murtaj Rizvy', 'KL12345', 'K L University', '9876543210', 8.76, 'Java, Python, HTML, C++'),
(2, 2, 'Jaya Krishna', 'KL12346', 'K L University', '9876500010', 8.65, 'Java, HTML, Python'),
(3, 3, 'Vamsi Krishna', 'KL12347', 'K L University', '9876500022', 9.10, 'C++, Java, Python'),
(4, 4, 'Divya', 'KL12348', 'K L University', '9876500033', 8.90, 'HTML, CSS, Java'),
(5, 5, 'Ammy Syed', 'KL12349', 'K L University', '9876500044', 9.20, 'Python, JavaScript, C++'),
(6, 6, 'Tony Stark', 'KL12350', 'SRM University', '9876500055', 9.80, 'C++, Java, Python, HTML'),
(7, 7, 'Bunny', 'KL12351', 'K L University', '9876500066', 8.45, 'HTML, Java, C');

CREATE TABLE admin_dashboard (
    company_id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    min_cgpa DECIMAL(4,2) NOT NULL,
    max_cgpa DECIMAL(4,2),
    required_skills VARCHAR(255),
    application_link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO admin_dashboard 
(company_name, min_cgpa, max_cgpa, required_skills, application_link) 
VALUES 
('Infosys', 7.0, 10.0, 'Java(4★), Python(3★), SQL(3★)', 'https://infosys.apply.com'),
('TCS', 7.0, 10.0, 'C(3★), Python(3★)', 'https://tcs.apply.com'),
('Amazon', 0.0, 9.0, 'Java(5★), Python(5★), SQL(5★)', 'https://amazon.apply.com'),
('Google', 9.0, 10.0, 'Java(5★), Python(5★), SQL(5★), C(5★)', 'https://google.apply.com'),
('Cursor', 8.5, 10.0, 'Java(5★), Python(5★), SQL(5★), HTML(5★), ReactJS(5★)', 'https://cursor.apply.com');
Select * from register;
select * from student_login;
select * from  student_dashboard;
SELECT company_name, required_skills, application_link
FROM admin_dashboard
WHERE min_cgpa <= 8.76 AND (max_cgpa IS NULL OR max_cgpa >= 8.76);

