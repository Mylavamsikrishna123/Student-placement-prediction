package com.placement.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Student Model Tests")
public class StudentTest {
    private Student student;

    @BeforeEach
    public void setUp() {
        student = new Student();
    }

    @Test
    @DisplayName("Test Default Constructor Initialization")
    public void testDefaultConstructor() {
        assertNotNull(student);
        assertEquals(0, student.getId());
        assertNull(student.getName());
        assertNull(student.getEmail());
    }

    @Test
    @DisplayName("Test Parameterized Constructor with All Fields")
    public void testParameterizedConstructor() {
        Student s = new Student(1, "John Doe", "john@email.com", "ID123", "CSE", 
                                "CSE-A", "B.Tech", 3.8, "ABC College", "9876543210", 
                                "AWS,Docker", 0);
        assertEquals(1, s.getId());
        assertEquals("John Doe", s.getName());
        assertEquals("john@email.com", s.getEmail());
        assertEquals("ID123", s.getIdNumber());
        assertEquals("CSE", s.getDepartment());
        assertEquals("CSE-A", s.getBranch());
        assertEquals("B.Tech", s.getDegree());
        assertEquals(3.8, s.getCgpa());
        assertEquals("ABC College", s.getCollegeName());
        assertEquals("9876543210", s.getPhone());
        assertEquals("AWS,Docker", s.getCertifications());
        assertEquals(0, s.getBacklogs());
    }

    @Test
    @DisplayName("Test ID Setter and Getter")
    public void testIdSetterGetter() {
        student.setId(5);
        assertEquals(5, student.getId());
    }

    @Test
    @DisplayName("Test Name Setter and Getter")
    public void testNameSetterGetter() {
        student.setName("Jane Smith");
        assertEquals("Jane Smith", student.getName());
    }

    @Test
    @DisplayName("Test Email Setter and Getter")
    public void testEmailSetterGetter() {
        student.setEmail("jane@email.com");
        assertEquals("jane@email.com", student.getEmail());
    }

    @Test
    @DisplayName("Test ID Number Setter and Getter")
    public void testIdNumberSetterGetter() {
        student.setIdNumber("ID456");
        assertEquals("ID456", student.getIdNumber());
    }

    @Test
    @DisplayName("Test Department Setter and Getter")
    public void testDepartmentSetterGetter() {
        student.setDepartment("IT");
        assertEquals("IT", student.getDepartment());
    }

    @Test
    @DisplayName("Test Branch Setter and Getter")
    public void testBranchSetterGetter() {
        student.setBranch("IT-B");
        assertEquals("IT-B", student.getBranch());
    }

    @Test
    @DisplayName("Test Degree Setter and Getter")
    public void testDegreeSetterGetter() {
        student.setDegree("B.Tech");
        assertEquals("B.Tech", student.getDegree());
    }

    @Test
    @DisplayName("Test CGPA Setter and Getter")
    public void testCgpaSetterGetter() {
        student.setCgpa(3.9);
        assertEquals(3.9, student.getCgpa());
    }

    @Test
    @DisplayName("Test College Name Setter and Getter")
    public void testCollegeNameSetterGetter() {
        student.setCollegeName("XYZ College");
        assertEquals("XYZ College", student.getCollegeName());
    }

    @Test
    @DisplayName("Test Phone Setter and Getter")
    public void testPhoneSetterGetter() {
        student.setPhone("9123456789");
        assertEquals("9123456789", student.getPhone());
    }

    @Test
    @DisplayName("Test Certifications Setter and Getter")
    public void testCertificationsSetterGetter() {
        student.setCertifications("Java,Python,AWS");
        assertEquals("Java,Python,AWS", student.getCertifications());
    }

    @Test
    @DisplayName("Test Backlogs Setter and Getter")
    public void testBacklogsSetterGetter() {
        student.setBacklogs(2);
        assertEquals(2, student.getBacklogs());
    }

    @Test
    @DisplayName("Test CGPA Max Value (4.0)")
    public void testCgpaMaxValue() {
        student.setCgpa(4.0);
        assertEquals(4.0, student.getCgpa());
    }

    @Test
    @DisplayName("Test CGPA Min Value (0.0)")
    public void testCgpaMinValue() {
        student.setCgpa(0.0);
        assertEquals(0.0, student.getCgpa());
    }

    @Test
    @DisplayName("Test CGPA Decimal Value")
    public void testCgpaDecimalValue() {
        student.setCgpa(3.75);
        assertEquals(3.75, student.getCgpa());
    }

    @Test
    @DisplayName("Test Backlogs Zero")
    public void testBacklogsZero() {
        student.setBacklogs(0);
        assertEquals(0, student.getBacklogs());
    }

    @Test
    @DisplayName("Test Backlogs Positive Value")
    public void testBacklogsPositiveValue() {
        student.setBacklogs(3);
        assertEquals(3, student.getBacklogs());
    }

    @Test
    @DisplayName("Test All Fields Update")
    public void testAllFieldsUpdate() {
        student.setId(10);
        student.setName("Alice Johnson");
        student.setEmail("alice@example.com");
        student.setIdNumber("ID789");
        student.setDepartment("ECE");
        student.setBranch("ECE-A");
        student.setDegree("B.Tech");
        student.setCgpa(3.6);
        student.setCollegeName("DEF College");
        student.setPhone("9988776655");
        student.setCertifications("C++,VLSI");
        student.setBacklogs(1);

        assertEquals(10, student.getId());
        assertEquals("Alice Johnson", student.getName());
        assertEquals("alice@example.com", student.getEmail());
        assertEquals("ID789", student.getIdNumber());
        assertEquals("ECE", student.getDepartment());
        assertEquals("ECE-A", student.getBranch());
        assertEquals("B.Tech", student.getDegree());
        assertEquals(3.6, student.getCgpa());
        assertEquals("DEF College", student.getCollegeName());
        assertEquals("9988776655", student.getPhone());
        assertEquals("C++,VLSI", student.getCertifications());
        assertEquals(1, student.getBacklogs());
    }

    @Test
    @DisplayName("Test Null Values Handling")
    public void testNullValuesHandling() {
        student.setName(null);
        student.setEmail(null);
        student.setCertifications(null);
        
        assertNull(student.getName());
        assertNull(student.getEmail());
        assertNull(student.getCertifications());
    }

    @Test
    @DisplayName("Test Email Format Validation")
    public void testEmailFormat() {
        student.setEmail("test.user@university.edu");
        assertEquals("test.user@university.edu", student.getEmail());
    }

    @Test
    @DisplayName("Test Phone Number Storage")
    public void testPhoneNumberStorage() {
        student.setPhone("+91-9876543210");
        assertEquals("+91-9876543210", student.getPhone());
    }

    @Test
    @DisplayName("Test Multiple Certifications")
    public void testMultipleCertifications() {
        String certs = "AWS Certified,Google Cloud,Azure Certified";
        student.setCertifications(certs);
        assertEquals(certs, student.getCertifications());
    }
}
